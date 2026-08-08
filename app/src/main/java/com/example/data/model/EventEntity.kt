package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String = "",
    val titleEn: String = "",
    val titleBn: String = "",
    val descriptionEn: String = "",
    val descriptionBn: String = "",
    val eventType: String = "EVENT", // EVENT, MEETING, PAYMENT_DEADLINE
    val date: String = "", // YYYY-MM-DD e.g. "2026-07-30"
    val time: String = "", // HH:MM e.g. "18:00"
    val locationEn: String = "",
    val locationBn: String = "",
    val amount: Double = 0.0,
    val isReminderSet: Boolean = false,
    val createdBy: String = "ADM-001"
)
