package com.example.data.repository

import com.example.data.db.*
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class ClubRepository(private val db: AppDatabase) {
    private val userDao = db.userDao()
    private val financialDao = db.financialDao()
    private val announcementDao = db.announcementDao()
    private val complaintDao = db.complaintDao()
    private val activityDao = db.activityDao()
    private val activityLogDao = db.activityLogDao()
    private val eventDao = db.eventDao()

    val allUsers: Flow<List<UserEntity>> = userDao.getAllUsers()
    val allAnnouncements: Flow<List<AnnouncementEntity>> = announcementDao.getAllAnnouncements()
    val allActivities: Flow<List<ActivityEntity>> = activityDao.getAllActivities()
    val allComplaints: Flow<List<ComplaintEntity>> = complaintDao.getAllComplaints()
    val allFinancials: Flow<List<FinancialRecordEntity>> = financialDao.getAllFinancials()
    val allActivityLogs: Flow<List<ActivityLogEntity>> = activityLogDao.getAllActivityLogs()
    val allEvents: Flow<List<EventEntity>> = eventDao.getAllEvents()

    fun getUserById(userId: String): Flow<UserEntity?> = userDao.getUserById(userId)
    fun getComplaintsByUserId(userId: String): Flow<List<ComplaintEntity>> = complaintDao.getComplaintsByUserId(userId)
    fun getFinancialsByUserId(userId: String): Flow<List<FinancialRecordEntity>> = financialDao.getFinancialsByUserId(userId)

    suspend fun getUserByPhone(phone: String): UserEntity? = userDao.getUserByPhone(phone)

    suspend fun insertUser(user: UserEntity) = userDao.insertUser(user)
    suspend fun cacheUsers(users: List<UserEntity>) = userDao.insertUsers(users)
    suspend fun updateMembershipStatus(userId: String, status: String) = userDao.updateMembershipStatus(userId, status)
    suspend fun updateUser(user: UserEntity) = userDao.updateUser(user)
    suspend fun deleteUser(userId: String) = userDao.deleteUserById(userId)
    suspend fun deleteAllUsersExcept(preserveKey: String) = userDao.deleteAllUsersExcept(preserveKey)

    suspend fun insertFinancialRecord(record: FinancialRecordEntity) = financialDao.insertFinancialRecord(record)
    suspend fun updatePaymentStatus(id: Long, status: String, txId: String, gateway: String) =
        financialDao.updatePaymentStatus(id, status, txId, gateway)

    suspend fun insertAnnouncement(announcement: AnnouncementEntity) = announcementDao.insertAnnouncement(announcement)
    suspend fun cacheAnnouncements(announcements: List<AnnouncementEntity>) = announcementDao.insertAnnouncements(announcements)
    suspend fun deleteAnnouncement(id: Long) = announcementDao.deleteAnnouncement(id)

    suspend fun insertComplaint(complaint: ComplaintEntity) = complaintDao.insertComplaint(complaint)
    suspend fun updateComplaintStatus(id: Long, status: String, noteEn: String, noteBn: String, updatedAt: String) =
        complaintDao.updateComplaintStatus(id, status, noteEn, noteBn, updatedAt)

    suspend fun insertActivity(activity: ActivityEntity) = activityDao.insertActivity(activity)

    suspend fun insertActivityLog(log: ActivityLogEntity) = activityLogDao.insert(log)

    suspend fun insertEvent(event: EventEntity) = eventDao.insert(event)
    suspend fun toggleEventReminder(id: String, isSet: Boolean) = eventDao.updateReminderStatus(id, isSet)
}
