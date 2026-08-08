package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.AnnouncementEntity
import com.example.data.repository.ClubRepository
import com.example.data.repository.FirestoreRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NoticesViewModel(application: Application) : AndroidViewModel(application) {
    private val firestoreRepository = FirestoreRepository()
    private val clubRepository = ClubRepository(AppDatabase.getDatabase(application))

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _isOfflineCachedMode = MutableStateFlow(false)
    val isOfflineCachedMode: StateFlow<Boolean> = _isOfflineCachedMode.asStateFlow()

    // Combined Flow: Listen to Firestore stream, cache into Room, fallback to Room database when offline
    val allNotices: StateFlow<List<AnnouncementEntity>> = combine(
        firestoreRepository.getAnnouncementsStream().catch { emit(emptyList()) },
        clubRepository.allAnnouncements
    ) { firestoreNotices, roomNotices ->
        _isLoading.value = false
        if (firestoreNotices.isNotEmpty()) {
            _isOfflineCachedMode.value = false
            viewModelScope.launch(Dispatchers.IO) {
                clubRepository.cacheAnnouncements(firestoreNotices)
            }
            firestoreNotices
        } else {
            if (roomNotices.isNotEmpty()) {
                _isOfflineCachedMode.value = true
            }
            roomNotices
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val filteredNotices: StateFlow<List<AnnouncementEntity>> = combine(
        allNotices,
        _selectedCategory
    ) { notices, category ->
        if (category == "All") {
            notices
        } else {
            notices.filter { it.categoryEn.equals(category, ignoreCase = true) || it.categoryBn.equals(category, ignoreCase = true) }
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun setCategoryFilter(category: String) {
        _selectedCategory.value = category
    }

    fun publishNoticeToFirestore(
        titleEn: String,
        titleBn: String,
        descriptionEn: String,
        descriptionBn: String,
        categoryEn: String,
        categoryBn: String,
        priority: String = "Medium",
        author: String = "Club Management Committee"
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val newNotice = AnnouncementEntity(
                titleEn = titleEn,
                titleBn = titleBn,
                descriptionEn = descriptionEn,
                descriptionBn = descriptionBn,
                categoryEn = categoryEn,
                categoryBn = categoryBn,
                date = java.time.LocalDate.now().toString(),
                priority = priority,
                author = author
            )
            firestoreRepository.addAnnouncement(newNotice)
            clubRepository.insertAnnouncement(newNotice)

            val log = com.example.data.model.ActivityLogEntity(
                id = "LOG-${System.currentTimeMillis()}",
                actionType = "NOTICE_CREATION",
                adminId = "ADM-001",
                adminName = author,
                titleEn = "Published Notice: $titleEn",
                titleBn = "বিজ্ঞপ্তি প্রকাশ: $titleBn",
                detailsEn = "Category: $categoryEn • Priority: $priority • Content: $descriptionEn",
                detailsBn = "ক্যাটাগরি: $categoryBn • অগ্রাধিকার: $priority • বিবরণ: $descriptionBn",
                timestamp = java.time.LocalDateTime.now().toString(),
                targetId = "NOTICE-${System.currentTimeMillis().toString().takeLast(4)}"
            )
            firestoreRepository.addActivityLog(log)
            clubRepository.insertActivityLog(log)

            // Trigger FCM Push Notification
            com.example.util.NotificationHelper.showNotification(
                context = getApplication(),
                title = "📢 New Club Notice: $titleEn",
                body = "Category: $categoryEn • $descriptionEn",
                channelId = com.example.util.NotificationHelper.CHANNEL_NOTICES
            )

            _isLoading.value = false
        }
    }

    fun sendTestNoticeNotification() {
        com.example.util.NotificationHelper.showNotification(
            context = getApplication(),
            title = "📢 FCM Test Notice: General Assembly Meeting",
            body = "Test FCM push notification: Annual General Meeting will be held on August 15 at 6 PM in the Resident Club House.",
            channelId = com.example.util.NotificationHelper.CHANNEL_NOTICES
        )
    }
}
