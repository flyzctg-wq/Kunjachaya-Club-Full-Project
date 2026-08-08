package com.example.data.db

import androidx.room.*
import com.example.data.model.ComplaintEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ComplaintDao {
    @Query("SELECT * FROM complaints ORDER BY id DESC")
    fun getAllComplaints(): Flow<List<ComplaintEntity>>

    @Query("SELECT * FROM complaints WHERE userId = :userId ORDER BY id DESC")
    fun getComplaintsByUserId(userId: String): Flow<List<ComplaintEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComplaint(complaint: ComplaintEntity): Long

    @Query("UPDATE complaints SET status = :status, adminNoteEn = :noteEn, adminNoteBn = :noteBn, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateComplaintStatus(id: Long, status: String, noteEn: String, noteBn: String, updatedAt: String)
}
