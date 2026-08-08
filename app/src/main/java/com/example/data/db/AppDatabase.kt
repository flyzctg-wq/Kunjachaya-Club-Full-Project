package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.*
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [
        UserEntity::class,
        FinancialRecordEntity::class,
        AnnouncementEntity::class,
        ComplaintEntity::class,
        ActivityEntity::class,
        ActivityLogEntity::class,
        EventEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun financialDao(): FinancialDao
    abstract fun announcementDao(): AnnouncementDao
    abstract fun complaintDao(): ComplaintDao
    abstract fun activityDao(): ActivityDao
    abstract fun activityLogDao(): ActivityLogDao
    abstract fun eventDao(): EventDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Secure encryption key for SQLCipher to protect resident NIDs, contacts & sensitive profile details
        private const val DB_PASSPHRASE = "Kunjachaya_Club_Encrypted_Resident_DB_Passphrase_2026!"

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // Initialize SQLCipher native libraries
                SQLiteDatabase.loadLibs(context.applicationContext)
                val passphraseBytes = SQLiteDatabase.getBytes(DB_PASSPHRASE.toCharArray())
                val factory = SupportFactory(passphraseBytes)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kunjachaya_club_db"
                )
                    .openHelperFactory(factory)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
