package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "announcements")
data class AnnouncementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titleEn: String = "",
    val titleBn: String = "",
    val descriptionEn: String = "",
    val descriptionBn: String = "",
    val categoryEn: String = "", // "Urgent Notice", "General News", "Upcoming Event", "Maintenance"
    val categoryBn: String = "",
    val date: String = "",
    val priority: String = "Medium", // "High", "Medium", "Low"
    val author: String = "Club Management Committee"
)
