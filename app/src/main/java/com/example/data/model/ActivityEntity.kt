package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activities")
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titleEn: String,
    val titleBn: String,
    val date: String,
    val locationEn: String,
    val locationBn: String,
    val summaryEn: String,
    val summaryBn: String,
    val imageUrl: String = "",
    val participantsCount: Int
)
