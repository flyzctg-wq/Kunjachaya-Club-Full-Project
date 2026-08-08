package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.*
import com.example.data.repository.ClubRepository
import com.example.data.repository.FirestoreRepository
import com.example.ui.language.Language
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.storage.FirebaseStorage
import com.example.data.preferences.LanguagePreferences
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.tasks.await

class ClubViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ClubRepository(AppDatabase.getDatabase(application))
    private val firestoreRepository = FirestoreRepository()
    private val paymentRepository = com.example.data.repository.PaymentRepository()
    private val languagePreferences = LanguagePreferences(application)

    val language = MutableStateFlow(languagePreferences.getLanguage())
    val isDarkTheme = MutableStateFlow(languagePreferences.isDarkTheme())
    val currentUser = MutableStateFlow<UserEntity?>(null)

    fun toggleTheme() {
        val newTheme = !isDarkTheme.value
        isDarkTheme.value = newTheme
        languagePreferences.setDarkTheme(newTheme)
    }

    private val _isUsersOfflineCached = MutableStateFlow(false)
    val isUsersOfflineCached: StateFlow<Boolean> = _isUsersOfflineCached.asStateFlow()
    
    val allUsers: StateFlow<List<UserEntity>> = combine(
        firestoreRepository.getUsersStream().catch { emit(emptyList()) },
        repository.allUsers
    ) { firestoreUsers, roomUsers ->
        if (firestoreUsers.isNotEmpty()) {
            _isUsersOfflineCached.value = false
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                repository.cacheUsers(firestoreUsers)
            }
            firestoreUsers
        } else {
            if (roomUsers.isNotEmpty()) {
                _isUsersOfflineCached.value = true
            }
            roomUsers
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAnnouncements: StateFlow<List<AnnouncementEntity>> = repository.allAnnouncements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allActivities: StateFlow<List<ActivityEntity>> = repository.allActivities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allComplaints: StateFlow<List<ComplaintEntity>> = repository.allComplaints
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFinancials: StateFlow<List<FinancialRecordEntity>> = combine(
        firestoreRepository.getAllFinancialsStream().catch { emit(emptyList()) },
        repository.allFinancials
    ) { firestoreFinancials, roomFinancials ->
        val map = LinkedHashMap<Long, FinancialRecordEntity>()
        firestoreFinancials.forEach { map[it.id] = it }
        roomFinancials.forEach { roomRec ->
            val existing = map[roomRec.id]
            if (existing == null || roomRec.status == "Completed" || roomRec.transactionId.isNotBlank()) {
                map[roomRec.id] = roomRec
            }
        }
        if (map.isEmpty()) roomFinancials else map.values.toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allActivityLogs: StateFlow<List<ActivityLogEntity>> = combine(
        firestoreRepository.getActivityLogsStream().catch { emit(emptyList()) },
        repository.allActivityLogs
    ) { firestoreLogs, roomLogs ->
        if (firestoreLogs.isNotEmpty()) firestoreLogs else roomLogs
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Financials for Current User
    val userFinancials: StateFlow<List<FinancialRecordEntity>> = combine(allFinancials, currentUser) { list, user ->
        if (user == null) emptyList()
        else list.filter { it.userId == user.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Complaints for Current User
    val userComplaints: StateFlow<List<ComplaintEntity>> = combine(allComplaints, currentUser) { list, user ->
        if (user == null) emptyList()
        else if (user.hasComplaintPermission()) list
        else list.filter { it.userId == user.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Restore the real Firebase Auth session, if any — never auto-select
        // a fake or hardcoded local account. If no one is signed in, the app
        // stays on the sign-in screen.
        viewModelScope.launch {
            val firebaseUser = FirebaseAuth.getInstance().currentUser
            if (firebaseUser != null) {
                val identity = firebaseUser.email ?: firebaseUser.phoneNumber
                val matched = identity?.let { id ->
                    allUsers.value.firstOrNull {
                        it.phone.equals(id, ignoreCase = true) || it.primaryContact.equals(id, ignoreCase = true)
                    } ?: repository.getUserByPhone(id)
                }
                currentUser.value = matched
            }
        }
    }

    fun toggleLanguage() {
        val newLang = if (language.value == Language.EN) Language.BN else Language.EN
        setLanguage(newLang)
    }

    fun setLanguage(lang: Language) {
        language.value = lang
        languagePreferences.setLanguage(lang)
    }

    fun selectUser(user: UserEntity) {
        currentUser.value = user
    }

    fun deleteUser(user: UserEntity) {
        viewModelScope.launch {
            repository.deleteUser(user.id)
            firestoreRepository.deleteUser(user.id)
            // Log deletion
            val log = ActivityLogEntity(
                id = "LOG-${System.currentTimeMillis()}",
                actionType = "MEMBER_DELETION",
                adminId = currentUser.value?.id ?: "",
                adminName = currentUser.value?.nameEn ?: "",
                titleEn = "Deleted User Record: ${user.nameEn} (${user.id})",
                titleBn = "সদস্য রেকর্ড অপসারণ: ${user.nameBn} (${user.id})",
                detailsEn = "User account (${user.phone}) was removed by ${currentUser.value?.nameEn ?: "an administrator"}.",
                detailsBn = "সদস্য অ্যাকাউন্টটি (${user.phone}) অপসারণ করা হয়েছে।",
                timestamp = java.time.LocalDateTime.now().toString(),
                targetId = user.id
            )
            repository.insertActivityLog(log)
            firestoreRepository.addActivityLog(log)
        }
    }

    fun createNewUserByAdmin(
        name: String,
        emailOrPhone: String,
        holding: String,
        road: String,
        block: String,
        memberClass: String,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val newId = "USR-${System.currentTimeMillis().toString().takeLast(6)}"
            val newUser = UserEntity(
                id = newId,
                phone = emailOrPhone,
                nameEn = name,
                nameBn = name,
                road = road,
                block = block,
                holding = holding,
                primaryContact = emailOrPhone,
                membershipStatus = "Active",
                memberClass = memberClass,
                joinedDate = java.time.LocalDate.now().toString()
            )
            repository.insertUser(newUser)
            firestoreRepository.saveUser(newUser)

            val log = ActivityLogEntity(
                id = "LOG-${System.currentTimeMillis()}",
                actionType = "USER_CREATION",
                adminId = currentUser.value?.id ?: "",
                adminName = currentUser.value?.nameEn ?: "",
                titleEn = "Created New User: $name ($memberClass)",
                titleBn = "নতুন সদস্য তৈরি: $name ($memberClass)",
                detailsEn = "Contact: $emailOrPhone • Address: $holding, $road",
                detailsBn = "যোগাযোগ: $emailOrPhone • ঠিকানা: $holding, $road",
                timestamp = java.time.LocalDateTime.now().toString(),
                targetId = newId
            )
            repository.insertActivityLog(log)
            firestoreRepository.addActivityLog(log)
            onComplete()
        }
    }

    // --- Real Firebase Phone Auth (SMS OTP) ---
    // No phone number is ever treated as verified without Firebase actually
    // confirming an SMS code with Google's servers — there is no local
    // shortcut or fixed/demo OTP.
    private var pendingVerificationId: String? = null
    private var pendingResendToken: com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken? = null
    val phoneAuthError = MutableStateFlow<String?>(null)

    fun sendPhoneOtp(
        activity: android.app.Activity,
        phoneNumber: String,
        onCodeSent: () -> Unit
    ) {
        phoneAuthError.value = null
        val options = com.google.firebase.auth.PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: com.google.firebase.auth.PhoneAuthCredential) {
                    // Android auto-retrieved the SMS itself — still a real, Firebase-verified credential.
                    signInWithPhoneCredential(credential) { _, _ -> }
                }

                override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                    phoneAuthError.value = e.localizedMessage ?: "Phone verification failed"
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken
                ) {
                    pendingVerificationId = verificationId
                    pendingResendToken = token
                    onCodeSent()
                }
            })
            .build()
        com.google.firebase.auth.PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyPhoneOtp(code: String, onResult: (Boolean, String?) -> Unit) {
        val verificationId = pendingVerificationId
        if (verificationId.isNullOrBlank()) {
            onResult(false, "No OTP request in progress — please request a new code")
            return
        }
        val credential = com.google.firebase.auth.PhoneAuthProvider.getCredential(verificationId, code)
        signInWithPhoneCredential(credential, onResult)
    }

    private fun signInWithPhoneCredential(
        credential: com.google.firebase.auth.PhoneAuthCredential,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val authResult = FirebaseAuth.getInstance().signInWithCredential(credential).await()
                val firebaseUser = authResult.user
                if (firebaseUser?.phoneNumber == null) {
                    onResult(false, "Phone verification did not return a confirmed number")
                    return@launch
                }
                val phone = firebaseUser.phoneNumber!!
                val existing = repository.getUserByPhone(phone)
                    ?: allUsers.value.firstOrNull { it.phone.equals(phone, ignoreCase = true) }
                val userToSet = existing ?: UserEntity(
                    id = firebaseUser.uid,
                    phone = phone,
                    primaryContact = phone,
                    membershipStatus = "Pending",
                    memberClass = MemberClass.NEW.name,
                    joinedDate = java.time.LocalDate.now().toString()
                ).also {
                    repository.insertUser(it)
                    firestoreRepository.saveUser(it)
                }
                currentUser.value = userToSet
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "Phone sign-in failed")
            }
        }
    }

    fun loginWithEmail(
        email: String,
        pass: String,
        selectedRole: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val authResult = FirebaseAuth.getInstance()
                    .signInWithEmailAndPassword(email, pass)
                    .await()
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    val uid = firebaseUser.uid
                    val userEmail = firebaseUser.email ?: email

                    // Match strictly by exact phone, primary contact, or uid
                    val existingUser = allUsers.value.firstOrNull { 
                        it.phone.equals(userEmail, ignoreCase = true) || 
                        it.primaryContact.equals(userEmail, ignoreCase = true) ||
                        it.id == uid
                    } ?: repository.getUserByPhone(userEmail)

                    val userToSet = existingUser ?: UserEntity(
                        id = uid,
                        phone = userEmail,
                        nameEn = firebaseUser.displayName.takeIf { !it.isNullOrBlank() } ?: userEmail.substringBefore("@"),
                        nameBn = firebaseUser.displayName.takeIf { !it.isNullOrBlank() } ?: userEmail.substringBefore("@"),
                        primaryContact = userEmail,
                        membershipStatus = "Pending",
                        memberClass = MemberClass.NEW.name,
                        joinedDate = java.time.LocalDate.now().toString()
                    )

                    // 1. Save locally to Room DB immediately (fast)
                    repository.insertUser(userToSet)

                    // 2. Set current user state & trigger UI callback IMMEDIATELY
                    currentUser.value = userToSet
                    onResult(true, null)

                    // 3. Sync to Firestore in background without blocking UI
                    viewModelScope.launch {
                        try {
                            withTimeoutOrNull(5000L) {
                                firestoreRepository.saveUser(userToSet)
                            }
                        } catch (_: Exception) {}
                    }
                } else {
                    onResult(false, "Authentication returned empty user profile")
                }
            } catch (e: Exception) {
                val rawMsg = e.localizedMessage ?: ""
                val friendlyMsg = if (rawMsg.contains("credential", ignoreCase = true) ||
                    rawMsg.contains("password", ignoreCase = true) ||
                    rawMsg.contains("user-not-found", ignoreCase = true) ||
                    rawMsg.contains("invalid", ignoreCase = true)) {
                    "ইমেইল/পাসওয়ার্ড সঠিক নয়, অথবা একাউন্টটি এখনো তৈরি হয়নি। নতুন একাউন্ট খুলতে নিচে 'নতুন সদস্য? নতুন একাউন্ট খুলুন' বাটনে চাপ দিন।"
                } else {
                    rawMsg.ifBlank { "Firebase Authentication failed" }
                }
                onResult(false, friendlyMsg)
            }
        }
    }

    fun registerWithEmail(
        email: String,
        pass: String,
        name: String,
        phone: String,
        selectedRole: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val authResult = FirebaseAuth.getInstance()
                    .createUserWithEmailAndPassword(email, pass)
                    .await()
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    runCatching {
                        val profileChange = UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build()
                        firebaseUser.updateProfile(profileChange).await()
                    }

                    val newId = firebaseUser.uid
                    val newUser = UserEntity(
                        id = newId,
                        phone = if (phone.isNotBlank()) phone else email,
                        nameEn = name,
                        nameBn = name,
                        primaryContact = if (phone.isNotBlank()) phone else email,
                        emergencyContact = phone,
                        membershipStatus = "Pending",
                        memberClass = MemberClass.NEW.name,
                        joinedDate = java.time.LocalDate.now().toString()
                    )

                    // 1. Save locally to Room DB immediately
                    repository.insertUser(newUser)

                    // 2. Set current user & trigger UI callback IMMEDIATELY
                    currentUser.value = newUser
                    onResult(true, null)

                    // 3. Sync to Firestore in background without blocking UI
                    viewModelScope.launch {
                        try {
                            withTimeoutOrNull(5000L) {
                                firestoreRepository.saveUser(newUser)
                            }
                        } catch (_: Exception) {}
                    }
                } else {
                    onResult(false, "Failed to create Firebase user account")
                }
            } catch (e: Exception) {
                val rawMsg = e.localizedMessage ?: ""
                val friendlyMsg = if (rawMsg.contains("already in use", ignoreCase = true)) {
                    "এই ইমেইল দিয়ে ইতোমধ্যে একাউন্ট খোলা আছে। অনুগ্রহ করে লগইন করুন।"
                } else if (rawMsg.contains("weak", ignoreCase = true) || rawMsg.contains("6 characters", ignoreCase = true)) {
                    "পাসওয়ার্ড অন্তত ৬ অক্ষরের হতে হবে।"
                } else {
                    rawMsg.ifBlank { "Registration failed" }
                }
                onResult(false, friendlyMsg)
            }
        }
    }

    fun sendPasswordResetEmail(email: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            if (email.isBlank()) {
                onResult(false, "ইমেইল এড্রেস লিখুন")
                return@launch
            }
            try {
                FirebaseAuth.getInstance().sendPasswordResetEmail(email).await()
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "Failed to send password reset email")
            }
        }
    }

    fun logout() {
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        currentUser.value = null
    }

    fun updateUser(user: UserEntity) {
        viewModelScope.launch {
            repository.updateUser(user)
            firestoreRepository.saveUser(user)
        }
    }

    /** Upload a profile photo to Firebase Storage and save the URL to Firestore/Room. */
    fun uploadProfilePhoto(uri: Uri, context: Context, onResult: (Boolean) -> Unit) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            try {
                val storageRef = FirebaseStorage.getInstance()
                    .reference
                    .child("profile_pics/$uid")
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@launch
                val bytes = inputStream.readBytes()
                inputStream.close()
                storageRef.putBytes(bytes).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()
                // Update Room + Firestore
                val updated = currentUser.value!!.copy(profilePicUrl = downloadUrl)
                repository.updateUser(updated)
                firestoreRepository.saveUser(updated)
                currentUser.value = updated
                onResult(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false)
            }
        }
    }

    fun updateUserRoleAndPrivileges(
        userId: String,
        memberClass: String,
        committeePost: String,
        canManageNotices: Boolean,
        canManageComplaints: Boolean,
        canManageMembers: Boolean,
        canManageFinancials: Boolean,
        canDeleteItems: Boolean,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                // Update Firebase Firestore
                val result = firestoreRepository.updateUserRoleAndPrivileges(
                    userId = userId,
                    memberClass = memberClass,
                    committeePost = committeePost,
                    canManageNotices = canManageNotices,
                    canManageComplaints = canManageComplaints,
                    canManageMembers = canManageMembers,
                    canManageFinancials = canManageFinancials,
                    canDeleteItems = canDeleteItems
                )
                // Update local database record
                val existingList = repository.allUsers.firstOrNull() ?: emptyList()
                val existing = existingList.find { it.id == userId }
                if (existing != null) {
                    val updated = existing.copy(
                        memberClass = memberClass,
                        committeePost = committeePost,
                        canManageNotices = canManageNotices,
                        canManageComplaints = canManageComplaints,
                        canManageMembers = canManageMembers,
                        canManageFinancials = canManageFinancials,
                        canDeleteItems = canDeleteItems
                    )
                    repository.updateUser(updated)
                }
                onComplete?.invoke(result.isSuccess)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete?.invoke(false)
            }
        }
    }

    fun processPayment(
        userId: String,
        amount: Double,
        titleEn: String,
        titleBn: String,
        gateway: String,
        type: String, // "Paid" or "Donation"
        onComplete: (txId: String) -> Unit
    ) {
        viewModelScope.launch {
            val txId = "${gateway.take(2).uppercase()}${System.currentTimeMillis().toString().takeLast(8)}"
            val record = FinancialRecordEntity(
                userId = userId,
                titleEn = titleEn,
                titleBn = titleBn,
                amount = amount,
                type = type,
                monthYear = java.time.LocalDate.now().let { it.month.name.lowercase().replaceFirstChar { c -> c.uppercase() } + " " + it.year },
                date = java.time.LocalDate.now().toString(),
                paymentGateway = gateway,
                transactionId = txId,
                status = "Completed"
            )
            repository.insertFinancialRecord(record)
            firestoreRepository.addFinancialRecord(record)
            onComplete(txId)
        }
    }

    fun launchPipraPayCheckout(
        context: android.content.Context,
        record: FinancialRecordEntity,
        userPhone: String,
        userEmail: String,
        onOrderIdCreated: (orderId: String) -> Unit
    ) {
        viewModelScope.launch {
            // A record must already be a real Firestore document before
            // checkout starts — the webhook needs a real docId to update
            // when payment actually completes. A brand-new donation with no
            // docId yet gets persisted (status Pending) first.
            val persistedRecord = if (record.docId.isBlank()) {
                val docId = firestoreRepository.addFinancialRecord(record).getOrNull()
                if (docId != null) {
                    val withDocId = record.copy(docId = docId)
                    repository.insertFinancialRecord(withDocId)
                    withDocId
                } else record
            } else record

            paymentRepository.launchPipraPayCheckout(
                context = context,
                amount = persistedRecord.amount,
                userId = persistedRecord.userId,
                recordId = persistedRecord.docId,
                title = persistedRecord.titleEn,
                customerPhone = userPhone,
                customerEmail = userEmail,
                onOrderIdCreated = { orderId ->
                    onOrderIdCreated(orderId)
                    // Only watch for the webhook's real completion — nothing
                    // here writes "Completed" to financials itself. That
                    // write happens exclusively server-side, with the Admin
                    // SDK, using the real PipraPay transaction id.
                }
            )
        }
    }

    fun submitComplaint(
        titleEn: String,
        titleBn: String,
        categoryEn: String,
        categoryBn: String,
        descEn: String,
        descBn: String,
        imageUrl: String
    ) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val complaint = ComplaintEntity(
                userId = user.id,
                userNameEn = user.nameEn,
                userNameBn = user.nameBn,
                holdingNo = "${user.holding}, ${user.road}",
                titleEn = titleEn,
                titleBn = titleBn,
                categoryEn = categoryEn,
                categoryBn = categoryBn,
                descriptionEn = descEn,
                descriptionBn = descBn,
                imageUrl = imageUrl,
                status = "Pending",
                adminNoteEn = "",
                adminNoteBn = "",
                createdAt = java.time.LocalDateTime.now().toString(),
                updatedAt = java.time.LocalDateTime.now().toString()
            )
            repository.insertComplaint(complaint)
        }
    }

    fun approveUserMembership(userId: String) {
        viewModelScope.launch {
            repository.updateMembershipStatus(userId, "Active")
            firestoreRepository.updateMembershipStatus(userId, "Active")

            val log = ActivityLogEntity(
                id = "LOG-${System.currentTimeMillis()}",
                actionType = "MEMBER_APPROVAL",
                adminId = currentUser.value?.id ?: "",
                adminName = currentUser.value?.nameEn ?: "Membership Committee",
                titleEn = "Approved Resident Member ($userId)",
                titleBn = "সদস্য আবেদন অনুমোদন প্রদান ($userId)",
                detailsEn = "Verified membership documents and activated full resident privileges.",
                detailsBn = "সদস্যপদের নথিপত্র যাচাইকরণ শেষে সক্রিয় নাগরিক সুবিধা চালু করা হয়েছে।",
                timestamp = java.time.LocalDateTime.now().toString(),
                targetId = userId
            )
            repository.insertActivityLog(log)
            firestoreRepository.addActivityLog(log)
        }
    }

    fun rejectUserMembership(userId: String) {
        viewModelScope.launch {
            repository.updateMembershipStatus(userId, "Rejected")
            firestoreRepository.updateMembershipStatus(userId, "Rejected")

            val log = ActivityLogEntity(
                id = "LOG-${System.currentTimeMillis()}",
                actionType = "MEMBER_APPROVAL",
                adminId = currentUser.value?.id ?: "",
                adminName = currentUser.value?.nameEn ?: "Membership Committee",
                titleEn = "Rejected Resident Member ($userId)",
                titleBn = "সদস্য আবেদন প্রত্যাখ্যান ($userId)",
                detailsEn = "Application rejected due to unverified documentation or policy mismatch.",
                detailsBn = "অসম্পূর্ণ বা অযাচিত নথিপত্রের কারণে আবেদনটি প্রত্যাখ্যান করা হয়েছে।",
                timestamp = java.time.LocalDateTime.now().toString(),
                targetId = userId
            )
            repository.insertActivityLog(log)
            firestoreRepository.addActivityLog(log)
        }
    }

    fun deleteAnnouncement(noticeId: Long) {
        viewModelScope.launch {
            repository.deleteAnnouncement(noticeId)
        }
    }

    fun updateComplaintStatus(id: Long, status: String, noteEn: String, noteBn: String) {
        viewModelScope.launch {
            val nowDateTime = java.time.LocalDateTime.now()
            val now = nowDateTime.toString()
            repository.updateComplaintStatus(id, status, noteEn, noteBn, now)
            firestoreRepository.updateComplaintStatus(id, status, noteEn, noteBn, now)

            val log = ActivityLogEntity(
                id = "LOG-${System.currentTimeMillis()}",
                actionType = "COMPLAINT_UPDATE",
                adminId = currentUser.value?.id ?: "",
                adminName = currentUser.value?.nameEn ?: "Maintenance Admin",
                titleEn = "Updated Complaint #$id to $status",
                titleBn = "অভিযোগ #$id এর স্ট্যাটাস $status করা হয়েছে",
                detailsEn = "Admin Remark: ${if (noteEn.isBlank()) "Status changed to $status" else noteEn}",
                detailsBn = "অ্যাডমিন নোট: ${if (noteBn.isBlank()) "স্ট্যাটাস পরিবর্তন: $status" else noteBn}",
                timestamp = now,
                targetId = "CMP-$id"
            )
            repository.insertActivityLog(log)
            firestoreRepository.addActivityLog(log)
        }
    }

    fun publishNotice(
        titleEn: String,
        titleBn: String,
        descEn: String,
        descBn: String,
        categoryEn: String,
        categoryBn: String,
        priority: String
    ) {
        viewModelScope.launch {
            val nowDateTime = java.time.LocalDateTime.now()
            val now = nowDateTime.toString()
            val notice = AnnouncementEntity(
                titleEn = titleEn,
                titleBn = titleBn,
                descriptionEn = descEn,
                descriptionBn = descBn,
                categoryEn = categoryEn,
                categoryBn = categoryBn,
                date = nowDateTime.toLocalDate().toString(),
                priority = priority
            )
            repository.insertAnnouncement(notice)
            firestoreRepository.addAnnouncement(notice)

            val log = ActivityLogEntity(
                id = "LOG-${System.currentTimeMillis()}",
                actionType = "NOTICE_CREATION",
                adminId = currentUser.value?.id ?: "",
                adminName = currentUser.value?.nameEn ?: "Club Executive Committee",
                titleEn = "Published Notice: $titleEn",
                titleBn = "বিজ্ঞপ্তি প্রকাশ: $titleBn",
                detailsEn = "Category: $categoryEn • Priority: $priority • Content: $descEn",
                detailsBn = "ক্যাটাগরি: $categoryBn • অগ্রাধিকার: $priority • বিষয়বস্তু: $descBn",
                timestamp = now,
                targetId = "NOTICE-${System.currentTimeMillis().toString().takeLast(4)}"
            )
            repository.insertActivityLog(log)
            firestoreRepository.addActivityLog(log)
        }
    }

    fun recordFinancialAdjustment(
        targetUserId: String,
        titleEn: String,
        titleBn: String,
        amount: Double,
        adjustmentType: String,
        noteEn: String,
        noteBn: String
    ) {
        viewModelScope.launch {
            val nowDateTime = java.time.LocalDateTime.now()
            val now = nowDateTime.toString()
            val record = FinancialRecordEntity(
                userId = targetUserId,
                titleEn = titleEn,
                titleBn = titleBn,
                amount = amount,
                type = "Adjustment",
                monthYear = nowDateTime.month.name.lowercase().replaceFirstChar { it.uppercase() } + " " + nowDateTime.year,
                date = nowDateTime.toLocalDate().toString(),
                paymentGateway = "Admin Adjustment",
                transactionId = "ADJ${System.currentTimeMillis().toString().takeLast(6)}",
                status = "Completed"
            )
            repository.insertFinancialRecord(record)
            firestoreRepository.addFinancialRecord(record)

            val log = ActivityLogEntity(
                id = "LOG-${System.currentTimeMillis()}",
                actionType = "FINANCIAL_ADJUSTMENT",
                adminId = currentUser.value?.id ?: "",
                adminName = currentUser.value?.nameEn ?: "Club Treasurer",
                titleEn = "Financial Adjustment: $titleEn (৳ ${amount.toInt()})",
                titleBn = "আর্থিক সমন্বয়: $titleBn (৳ ${amount.toInt()})",
                detailsEn = "Adjustment Type: $adjustmentType • Admin Note: $noteEn",
                detailsBn = "সমন্বয়ের ধরন: $adjustmentType • বিবরণ: $noteBn",
                timestamp = now,
                targetId = targetUserId
            )
            repository.insertActivityLog(log)
            firestoreRepository.addActivityLog(log)
        }
    }
}
