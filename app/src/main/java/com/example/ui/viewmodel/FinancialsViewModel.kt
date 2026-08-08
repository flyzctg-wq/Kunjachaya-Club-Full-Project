package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.FinancialRecordEntity
import com.example.data.repository.ClubRepository
import com.example.data.repository.FirestoreRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class FinancialsViewModel(application: Application) : AndroidViewModel(application) {
    private val firestoreRepository = FirestoreRepository()
    private val clubRepository = ClubRepository(AppDatabase.getDatabase(application))

    private val _currentUserId = MutableStateFlow("USR-101")
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Real-time stream of user financial records from Firestore with Room database fallback
    val userFinancials: StateFlow<List<FinancialRecordEntity>> = _currentUserId
        .flatMapLatest { userId ->
            combine(
                firestoreRepository.getUserFinancialsStream(userId).catch { emit(emptyList()) },
                clubRepository.getFinancialsByUserId(userId)
            ) { firestoreRecords, roomRecords ->
                _isLoading.value = false
                if (firestoreRecords.isNotEmpty()) {
                    firestoreRecords
                } else {
                    roomRecords
                }
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    // All financial records (for Admin / General overview)
    val allFinancials: StateFlow<List<FinancialRecordEntity>> = combine(
        firestoreRepository.getAllFinancialsStream().catch { emit(emptyList()) },
        clubRepository.allFinancials
    ) { firestoreRecords, roomRecords ->
        if (firestoreRecords.isNotEmpty()) firestoreRecords else roomRecords
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Flow for tracking current unpaid Dues amount sum
    val totalDuesAmount: StateFlow<Double> = userFinancials.map { records ->
        records.filter { it.type.equals("Due", ignoreCase = true) && !it.status.equals("Completed", ignoreCase = true) }
            .sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Flow for tracking total Paid amounts sum
    val totalPaidAmount: StateFlow<Double> = userFinancials.map { records ->
        records.filter { (it.type.equals("Paid", ignoreCase = true) || it.type.equals("Due", ignoreCase = true)) && it.status.equals("Completed", ignoreCase = true) }
            .sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Flow for tracking total Donations amount sum
    val totalDonationAmount: StateFlow<Double> = userFinancials.map { records ->
        records.filter { it.type.equals("Donation", ignoreCase = true) && it.status.equals("Completed", ignoreCase = true) }
            .sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Unpaid Dues list
    val pendingDuesList: StateFlow<List<FinancialRecordEntity>> = userFinancials.map { records ->
        records.filter { it.type.equals("Due", ignoreCase = true) && !it.status.equals("Completed", ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Completed Paid transactions history
    val paidHistoryList: StateFlow<List<FinancialRecordEntity>> = userFinancials.map { records ->
        records.filter { it.status.equals("Completed", ignoreCase = true) && !it.type.equals("Donation", ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Donation History
    val donationHistoryList: StateFlow<List<FinancialRecordEntity>> = userFinancials.map { records ->
        records.filter { it.type.equals("Donation", ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setCurrentUserId(userId: String) {
        _currentUserId.value = userId
    }

    /**
     * Submit a payment for an existing due or financial record.
     */
    fun processPayment(
        record: FinancialRecordEntity,
        paymentGateway: String,
        transactionId: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            // Update in Firestore
            firestoreRepository.updatePaymentStatus(
                recordId = record.id,
                status = "Completed",
                transactionId = transactionId,
                paymentGateway = paymentGateway
            )
            // Update in local Room DB
            clubRepository.updatePaymentStatus(
                id = record.id,
                status = "Completed",
                txId = transactionId,
                gateway = paymentGateway
            )
            _isLoading.value = false
        }
    }

    /**
     * Create and record a new voluntary donation in Firestore and local Room database.
     */
    fun recordDonation(
        userId: String,
        amount: Double,
        titleEn: String,
        titleBn: String,
        paymentGateway: String,
        transactionId: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val donationRecord = FinancialRecordEntity(
                userId = userId,
                titleEn = titleEn,
                titleBn = titleBn,
                amount = amount,
                type = "Donation",
                monthYear = java.time.LocalDate.now().let { it.month.name.lowercase().replaceFirstChar { c -> c.uppercase() } + " " + it.year },
                date = java.time.LocalDate.now().toString(),
                paymentGateway = paymentGateway,
                transactionId = transactionId,
                status = "Completed"
            )
            firestoreRepository.addFinancialRecord(donationRecord)
            clubRepository.insertFinancialRecord(donationRecord)
            _isLoading.value = false
        }
    }
}
