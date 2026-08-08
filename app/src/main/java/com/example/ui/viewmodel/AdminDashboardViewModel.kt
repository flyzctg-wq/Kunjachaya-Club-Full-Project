package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.ActivityLogEntity
import com.example.data.model.ComplaintEntity
import com.example.data.model.FinancialRecordEntity
import com.example.data.repository.ClubRepository
import com.example.data.repository.FirestoreRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AdminDashboardMetrics(
    val totalLogsCount: Int = 0,
    val noticesCreatedCount: Int = 0,
    val complaintUpdatesCount: Int = 0,
    val financialAdjustmentsCount: Int = 0,
    val memberApprovalsCount: Int = 0,
    val pendingComplaintsCount: Int = 0,
    val totalFinancialTransactionsCount: Int = 0,
    val totalCollectedAmount: Double = 0.0
)

class AdminDashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val clubRepository = ClubRepository(AppDatabase.getDatabase(application))
    private val firestoreRepository = FirestoreRepository()

    // 1. Activity Logs stream from Firestore with Room fallback
    val activityLogs: StateFlow<List<ActivityLogEntity>> = combine(
        firestoreRepository.getActivityLogsStream().catch { emit(emptyList()) },
        clubRepository.allActivityLogs
    ) { firestoreLogs, roomLogs ->
        if (firestoreLogs.isNotEmpty()) firestoreLogs else roomLogs
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 2. All Complaints stream from Firestore with Room fallback
    val allComplaints: StateFlow<List<ComplaintEntity>> = combine(
        firestoreRepository.getComplaintsStream().catch { emit(emptyList()) },
        clubRepository.allComplaints
    ) { firestoreComplaints, roomComplaints ->
        if (firestoreComplaints.isNotEmpty()) firestoreComplaints else roomComplaints
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filter pending complaints
    val pendingComplaints: StateFlow<List<ComplaintEntity>> = allComplaints
        .map { list -> list.filter { it.status == "Pending" || it.status == "Under Review" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 3. All Financial Records stream from Firestore with Room fallback
    val allFinancials: StateFlow<List<FinancialRecordEntity>> = combine(
        firestoreRepository.getAllFinancialsStream().catch { emit(emptyList()) },
        clubRepository.allFinancials
    ) { firestoreFinancials, roomFinancials ->
        if (firestoreFinancials.isNotEmpty()) firestoreFinancials else roomFinancials
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Recent financial transactions (latest 5)
    val recentFinancials: StateFlow<List<FinancialRecordEntity>> = allFinancials
        .map { list -> list.take(5) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 4. Aggregated metrics derived from 'ActivityLogs' collection and system data
    val metrics: StateFlow<AdminDashboardMetrics> = combine(
        activityLogs,
        allComplaints,
        allFinancials
    ) { logs, complaints, financials ->
        AdminDashboardMetrics(
            totalLogsCount = logs.size,
            noticesCreatedCount = logs.count { it.actionType == "NOTICE_CREATION" },
            complaintUpdatesCount = logs.count { it.actionType == "COMPLAINT_UPDATE" },
            financialAdjustmentsCount = logs.count { it.actionType == "FINANCIAL_ADJUSTMENT" },
            memberApprovalsCount = logs.count { it.actionType == "MEMBER_APPROVAL" },
            pendingComplaintsCount = complaints.count { it.status == "Pending" || it.status == "Under Review" },
            totalFinancialTransactionsCount = financials.size,
            totalCollectedAmount = financials.filter { it.status == "Completed" || it.status == "Paid" }.sumOf { it.amount }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AdminDashboardMetrics())

    fun updateComplaintStatus(id: Long, status: String, adminNoteEn: String, adminNoteBn: String) {
        viewModelScope.launch {
            val now = java.time.LocalDateTime.now().toString()
            clubRepository.updateComplaintStatus(id, status, adminNoteEn, adminNoteBn, now)

            val log = ActivityLogEntity(
                id = "LOG-${System.currentTimeMillis()}",
                actionType = "COMPLAINT_UPDATE",
                adminId = "ADM-001",
                adminName = "Maintenance Admin",
                titleEn = "Updated Complaint #$id to $status",
                titleBn = "অভিযোগ #$id এর স্ট্যাটাস $status করা হয়েছে",
                detailsEn = "Admin Remark: ${if (adminNoteEn.isBlank()) "Status set to $status" else adminNoteEn}",
                detailsBn = "অ্যাডমিন নোট: ${if (adminNoteBn.isBlank()) "স্ট্যাটাস পরিবর্তন: $status" else adminNoteBn}",
                timestamp = now,
                targetId = id.toString()
            )
            clubRepository.insertActivityLog(log)
            firestoreRepository.addActivityLog(log)
        }
    }
}
