package com.example.data.db

import androidx.room.*
import com.example.data.model.FinancialRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialDao {
    @Query("SELECT * FROM financials WHERE userId = :userId ORDER BY id DESC")
    fun getFinancialsByUserId(userId: String): Flow<List<FinancialRecordEntity>>

    @Query("SELECT * FROM financials ORDER BY id DESC")
    fun getAllFinancials(): Flow<List<FinancialRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFinancialRecord(record: FinancialRecordEntity): Long

    @Query("UPDATE financials SET status = :status, transactionId = :txId, paymentGateway = :gateway WHERE id = :id")
    suspend fun updatePaymentStatus(id: Long, status: String, txId: String, gateway: String)
}
