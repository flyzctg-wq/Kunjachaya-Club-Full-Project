package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.ComplaintEntity
import com.example.data.repository.ClubRepository
import com.example.data.repository.FirestoreRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class ComplaintsViewModel(application: Application) : AndroidViewModel(application) {
    private val firestoreRepository = FirestoreRepository()
    private val clubRepository = ClubRepository(AppDatabase.getDatabase(application))

    private val _currentUserId = MutableStateFlow("USR-101")
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _submitResult = MutableSharedFlow<Result<String>>()
    val submitResult: SharedFlow<Result<String>> = _submitResult.asSharedFlow()

    // Stream user-specific complaints from Firestore with Room fallback
    val userComplaints: StateFlow<List<ComplaintEntity>> = _currentUserId
        .flatMapLatest { userId ->
            combine(
                firestoreRepository.getUserComplaintsStream(userId).catch { emit(emptyList()) },
                clubRepository.getComplaintsByUserId(userId)
            ) { firestoreList, roomList ->
                _isLoading.value = false
                if (firestoreList.isNotEmpty()) firestoreList else roomList
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    // Stream all complaints (for Admin / Board view)
    val allComplaints: StateFlow<List<ComplaintEntity>> = combine(
        firestoreRepository.getComplaintsStream().catch { emit(emptyList()) },
        clubRepository.allComplaints
    ) { firestoreList, roomList ->
        if (firestoreList.isNotEmpty()) firestoreList else roomList
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun setCurrentUserId(userId: String) {
        _currentUserId.value = userId
    }

    /**
     * Submit a new complaint to Firestore with initial status = 'Pending' and store locally in Room.
     */
    fun submitComplaint(
        titleEn: String,
        titleBn: String = titleEn,
        categoryEn: String = "Maintenance",
        categoryBn: String = "রক্ষণাবেক্ষণ",
        descEn: String,
        descBn: String = descEn,
        imageUrl: String = "",
        userNameEn: String = "Md. Rafiqul Islam",
        userNameBn: String = "মোঃ রফিকুল ইসলাম",
        holdingNo: String = "Holding 42/A, Road 04"
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            val complaint = ComplaintEntity(
                userId = _currentUserId.value,
                userNameEn = userNameEn,
                userNameBn = userNameBn,
                holdingNo = holdingNo,
                titleEn = titleEn,
                titleBn = titleBn,
                categoryEn = categoryEn,
                categoryBn = categoryBn,
                descriptionEn = descEn,
                descriptionBn = descBn,
                imageUrl = imageUrl,
                status = "Pending", // Initial status required by user prompt
                adminNoteEn = "",
                adminNoteBn = "",
                createdAt = now,
                updatedAt = now
            )

            // 1. Push to Firestore 'complaints' collection
            val firestoreResult = firestoreRepository.addComplaint(complaint)

            // 2. Push to local Room Database
            clubRepository.insertComplaint(complaint)

            _isSubmitting.value = false
            _submitResult.emit(firestoreResult)
        }
    }

    /**
     * Update status of a complaint and dispatch FCM Push Notification to member.
     */
    fun updateComplaintStatus(
        complaint: ComplaintEntity,
        newStatus: String,
        adminNoteEn: String = "",
        adminNoteBn: String = ""
    ) {
        viewModelScope.launch {
            val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

            // Update in Firestore
            firestoreRepository.updateComplaintStatus(
                complaintId = complaint.id,
                status = newStatus,
                adminNoteEn = adminNoteEn,
                adminNoteBn = adminNoteBn,
                updatedAt = now
            )

            // Update in local Room DB
            clubRepository.updateComplaintStatus(
                id = complaint.id,
                status = newStatus,
                noteEn = adminNoteEn,
                noteBn = adminNoteBn,
                updatedAt = now
            )

            // Trigger FCM Push Notification
            com.example.util.NotificationHelper.showNotification(
                context = getApplication(),
                title = "🔔 Complaint Status Update: $newStatus",
                body = "Complaint: '${complaint.titleEn}' is now $newStatus. ${if (adminNoteEn.isNotBlank()) "Note: $adminNoteEn" else ""}",
                channelId = com.example.util.NotificationHelper.CHANNEL_COMPLAINTS
            )
        }
    }

    fun sendTestComplaintNotification() {
        com.example.util.NotificationHelper.showNotification(
            context = getApplication(),
            title = "🔔 FCM Test Complaint Update: Under Review",
            body = "Ticket #1042 status changed to Under Review. Maintenance engineer assigned.",
            channelId = com.example.util.NotificationHelper.CHANNEL_COMPLAINTS
        )
    }
}
