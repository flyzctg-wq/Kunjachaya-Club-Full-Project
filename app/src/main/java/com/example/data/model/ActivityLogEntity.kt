package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey val id: String = "",
    val actionType: String = "", // NOTICE_CREATION, COMPLAINT_UPDATE, FINANCIAL_ADJUSTMENT, MEMBER_APPROVAL
    val adminId: String = "ADM-001",
    val adminName: String = "Club Admin Committee",
    val titleEn: String = "",
    val titleBn: String = "",
    val detailsEn: String = "",
    val detailsBn: String = "",
    val timestamp: String = "",
    val targetId: String = ""
)
