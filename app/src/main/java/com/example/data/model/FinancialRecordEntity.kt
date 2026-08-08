package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "financials")
data class FinancialRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String = "",
    val titleEn: String = "",
    val titleBn: String = "",
    val amount: Double = 0.0,
    val type: String = "Due", // "Due", "Paid", "Donation"
    val monthYear: String = "", // e.g., "July 2026"
    val date: String = "",
    val paymentGateway: String = "", // "bKash", "Nagad", "Rocket", "Bank Transfer"
    val transactionId: String = "",
    val status: String = "Pending", // "Pending", "Completed", "Failed"
    // The REAL Firestore document id for this record. `id` above is a local
    // Room primary key / one-way hash and can't be used to look this record
    // back up in Firestore — anything that needs to reference this record
    // server-side (e.g. a payment webhook) must use docId, not id.
    val docId: String = ""
)
