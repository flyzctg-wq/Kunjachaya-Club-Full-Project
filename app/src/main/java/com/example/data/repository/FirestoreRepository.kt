package com.example.data.repository

import com.example.data.model.ActivityLogEntity
import com.example.data.model.AnnouncementEntity
import com.example.data.model.ComplaintEntity
import com.example.data.model.EventEntity
import com.example.data.model.FinancialRecordEntity
import com.example.data.model.UserEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.tasks.await

/**
 * Boilerplate repository for initializing and managing Firebase Firestore data
 * for users and financial records in Kunjachaya Club.
 */
class FirestoreRepository {
    private val firestore: FirebaseFirestore?
        get() = com.example.util.FirebaseManager.firestore ?: run {
            try {
                FirebaseFirestore.getInstance()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    private val usersCollection get() = firestore?.collection("users")
    private val financialsCollection get() = firestore?.collection("financials")
    private val announcementsCollection get() = firestore?.collection("announcements")
    private val complaintsCollection get() = firestore?.collection("complaints")
    private val activityLogsCollection get() = firestore?.collection("ActivityLogs")
    private val eventsCollection get() = firestore?.collection("Events")

    // --- AUDIT LOGS / ACTIVITY LOGS MANAGEMENT ---

    /**
     * Real-time stream of all administrator activity logs from 'ActivityLogs' Firestore collection.
     */
    fun getActivityLogsStream(): Flow<List<ActivityLogEntity>> {
        val collection = activityLogsCollection ?: return emptyFlow()
        return callbackFlow {
            val listener = collection.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val logs = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ActivityLogEntity::class.java)
                } ?: emptyList()
                trySend(logs)
            }
            awaitClose { listener.remove() }
        }
    }

    /**
     * Add an administrative activity audit log to 'ActivityLogs' Firestore collection.
     */
    suspend fun addActivityLog(log: ActivityLogEntity): Result<String> = runCatching {
        val collection = activityLogsCollection ?: throw IllegalStateException("Firestore not initialized")
        val docRef = collection.document()
        val logToSave = if (log.id.isEmpty()) log.copy(id = docRef.id) else log
        docRef.set(logToSave).await()
        docRef.id
    }

    // --- COMPLAINTS MANAGEMENT ---

    /**
     * Real-time stream of all complaints from Firestore.
     */
    fun getComplaintsStream(): Flow<List<ComplaintEntity>> {
        val collection = complaintsCollection ?: return emptyFlow()
        return callbackFlow {
            val listener = collection.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val complaints = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ComplaintEntity::class.java)
                } ?: emptyList()
                trySend(complaints)
            }
            awaitClose { listener.remove() }
        }
    }

    /**
     * Real-time stream of complaints submitted by a specific user.
     */
    fun getUserComplaintsStream(userId: String): Flow<List<ComplaintEntity>> {
        val collection = complaintsCollection ?: return emptyFlow()
        return callbackFlow {
            val query = collection.whereEqualTo("userId", userId)
            val listener = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val complaints = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ComplaintEntity::class.java)
                } ?: emptyList()
                trySend(complaints)
            }
            awaitClose { listener.remove() }
        }
    }

    /**
     * Submit a new complaint to the 'complaints' collection in Firestore with an initial status of 'Pending'.
     */
    suspend fun addComplaint(complaint: ComplaintEntity): Result<String> = runCatching {
        val collection = complaintsCollection ?: throw IllegalStateException("Firestore not initialized")
        val docRef = collection.document()
        val complaintToSave = complaint.copy(
            id = docRef.id.hashCode().toLong(),
            status = "Pending"
        )
        docRef.set(complaintToSave).await()
        docRef.id
    }

    /**
     * Update the status and admin notes for a complaint in Firestore.
     */
    suspend fun updateComplaintStatus(
        complaintId: Long,
        status: String,
        adminNoteEn: String,
        adminNoteBn: String,
        updatedAt: String
    ): Result<Unit> = runCatching {
        val collection = complaintsCollection ?: throw IllegalStateException("Firestore not initialized")
        collection.document(complaintId.toString()).update(
            mapOf(
                "status" to status,
                "adminNoteEn" to adminNoteEn,
                "adminNoteBn" to adminNoteBn,
                "updatedAt" to updatedAt
            )
        ).await()
    }

    // --- ANNOUNCEMENTS & NOTICES MANAGEMENT ---

    /**
     * Real-time stream of all announcements and notices from Firestore.
     */
    fun getAnnouncementsStream(): Flow<List<AnnouncementEntity>> {
        val collection = announcementsCollection ?: return emptyFlow()
        return callbackFlow {
            val listener = collection.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val announcements = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(AnnouncementEntity::class.java)
                } ?: emptyList()
                trySend(announcements)
            }
            awaitClose { listener.remove() }
        }
    }

    /**
     * Add a new announcement/notice to Firestore.
     */
    suspend fun addAnnouncement(announcement: AnnouncementEntity): Result<String> = runCatching {
        val collection = announcementsCollection ?: throw IllegalStateException("Firestore not initialized")
        val docRef = collection.document()
        val announcementWithId = announcement.copy(id = docRef.id.hashCode().toLong())
        docRef.set(announcementWithId).await()
        docRef.id
    }

    // --- USER MANAGEMENT ---

    /**
     * Save or update a user document in Firestore.
     */
    suspend fun saveUser(user: UserEntity): Result<Unit> = runCatching {
        val collection = usersCollection ?: throw IllegalStateException("Firestore not initialized")
        collection.document(user.id).set(user).await()
    }

    /**
     * Fetch a single user by User ID.
     */
    suspend fun getUserById(userId: String): UserEntity? {
        val collection = usersCollection ?: return null
        val snapshot = collection.document(userId).get().await()
        return if (snapshot.exists()) snapshot.toObject(UserEntity::class.java) else null
    }

    /**
     * Real-time stream of all users from Firestore.
     */
    fun getUsersStream(): Flow<List<UserEntity>> {
        val collection = usersCollection ?: return emptyFlow()
        return callbackFlow {
            val listener = collection.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val users = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(UserEntity::class.java)
                } ?: emptyList()
                trySend(users)
            }
            awaitClose { listener.remove() }
        }
    }

    /**
     * Update membership status of a user (e.g. "Active", "Pending").
     */
    suspend fun updateMembershipStatus(userId: String, status: String): Result<Unit> = runCatching {
        val collection = usersCollection ?: throw IllegalStateException("Firestore not initialized")
        collection.document(userId).update("membershipStatus", status).await()
    }

    /**
     * Update user role and moderation privilege flags in Firestore.
     */
    suspend fun updateUserRoleAndPrivileges(
        userId: String,
        memberClass: String,
        committeePost: String,
        canManageNotices: Boolean,
        canManageComplaints: Boolean,
        canManageMembers: Boolean,
        canManageFinancials: Boolean,
        canDeleteItems: Boolean
    ): Result<Unit> = runCatching {
        val collection = usersCollection ?: throw IllegalStateException("Firestore not initialized")
        collection.document(userId).update(
            mapOf(
                "memberClass" to memberClass,
                "committeePost" to committeePost,
                "canManageNotices" to canManageNotices,
                "canManageComplaints" to canManageComplaints,
                "canManageMembers" to canManageMembers,
                "canManageFinancials" to canManageFinancials,
                "canDeleteItems" to canDeleteItems
            )
        ).await()
    }

    /**
     * Delete a user document from Firestore.
     */
    suspend fun deleteUser(userId: String): Result<Unit> = runCatching {
        val collection = usersCollection ?: throw IllegalStateException("Firestore not initialized")
        collection.document(userId).delete().await()
    }

    // --- FINANCIAL RECORDS MANAGEMENT ---

    /**
     * Save a new financial record (Due, Paid, Donation) to Firestore.
     * Returns the real Firestore document id — callers that need to
     * reference this record later (e.g. a payment gateway recordId) must
     * use this, not the numeric `id` field.
     */
    suspend fun addFinancialRecord(record: FinancialRecordEntity): Result<String> = runCatching {
        val collection = financialsCollection ?: throw IllegalStateException("Firestore not initialized")
        val docRef = collection.document()
        val recordWithId = record.copy(id = docRef.id.hashCode().toLong(), docId = docRef.id)
        docRef.set(recordWithId).await()
        docRef.id
    }

    /**
     * Stream user-specific financial records in real-time.
     */
    fun getUserFinancialsStream(userId: String): Flow<List<FinancialRecordEntity>> {
        val collection = financialsCollection ?: return emptyFlow()
        return callbackFlow {
            val query = collection.whereEqualTo("userId", userId)
                .orderBy("date", Query.Direction.DESCENDING)

            val listener = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val records = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(FinancialRecordEntity::class.java)?.copy(docId = doc.id)
                } ?: emptyList()
                trySend(records)
            }
            awaitClose { listener.remove() }
        }
    }

    /**
     * Stream all financial records in real-time (for Admin overview).
     */
    fun getAllFinancialsStream(): Flow<List<FinancialRecordEntity>> {
        val collection = financialsCollection ?: return emptyFlow()
        return callbackFlow {
            val listener = collection.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val records = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(FinancialRecordEntity::class.java)?.copy(docId = doc.id)
                } ?: emptyList()
                trySend(records)
            }
            awaitClose { listener.remove() }
        }
    }

    /**
     * Update the payment status and transaction details of a financial record.
     */
    suspend fun updatePaymentStatus(
        recordId: Long,
        status: String,
        transactionId: String,
        paymentGateway: String
    ): Result<Unit> = runCatching {
        val collection = financialsCollection ?: return@runCatching
        // Try updating by matching numeric 'id' field
        val querySnapshot = collection.whereEqualTo("id", recordId).get().await()
        if (!querySnapshot.isEmpty) {
            for (doc in querySnapshot.documents) {
                doc.reference.update(
                    mapOf(
                        "status" to status,
                        "transactionId" to transactionId,
                        "paymentGateway" to paymentGateway
                    )
                ).await()
            }
        } else {
            // Fallback: update document by string key directly
            collection.document(recordId.toString()).set(
                mapOf(
                    "status" to status,
                    "transactionId" to transactionId,
                    "paymentGateway" to paymentGateway
                ),
                com.google.firebase.firestore.SetOptions.merge()
            ).await()
        }
    }

    // --- EVENTS & CALENDAR MANAGEMENT ---

    /**
     * Real-time stream of all scheduled calendar events from 'Events' Firestore collection.
     */
    fun getEventsStream(): Flow<List<EventEntity>> {
        val collection = eventsCollection ?: return emptyFlow()
        return callbackFlow {
            val listener = collection.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val events = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(EventEntity::class.java)
                } ?: emptyList()
                trySend(events)
            }
            awaitClose { listener.remove() }
        }
    }

    /**
     * Add a calendar event to 'Events' Firestore collection.
     */
    suspend fun addEvent(event: EventEntity): Result<String> = runCatching {
        val collection = eventsCollection ?: throw IllegalStateException("Firestore not initialized")
        val docRef = collection.document()
        val eventToSave = if (event.id.isEmpty()) event.copy(id = docRef.id) else event
        docRef.set(eventToSave).await()
        docRef.id
    }

    /**
     * Toggle reminder status for an event in Firestore.
     */
    suspend fun updateEventReminder(eventId: String, isReminderSet: Boolean): Result<Unit> = runCatching {
        val collection = eventsCollection ?: throw IllegalStateException("Firestore not initialized")
        collection.document(eventId).update("isReminderSet", isReminderSet).await()
    }
}
