package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.ActivityLogEntity
import com.example.data.model.EventEntity
import com.example.data.repository.ClubRepository
import com.example.data.repository.FirestoreRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CalendarViewModel(application: Application) : AndroidViewModel(application) {
    private val clubRepository = ClubRepository(AppDatabase.getDatabase(application))
    private val firestoreRepository = FirestoreRepository()

    val selectedDate = MutableStateFlow<String?>(java.time.LocalDate.now().toString())
    val selectedCategory = MutableStateFlow("ALL") // ALL, EVENT, MEETING, PAYMENT_DEADLINE

    // Real-time stream combining Firestore with Room database
    val allEvents: StateFlow<List<EventEntity>> = combine(
        firestoreRepository.getEventsStream().catch { emit(emptyList()) },
        clubRepository.allEvents
    ) { firestoreEvents, roomEvents ->
        if (firestoreEvents.isNotEmpty()) firestoreEvents else roomEvents
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered events based on selected date & category
    val filteredEvents: StateFlow<List<EventEntity>> = combine(
        allEvents,
        selectedDate,
        selectedCategory
    ) { events, date, category ->
        events.filter { event ->
            val matchesCategory = (category == "ALL" || event.eventType == category)
            val matchesDate = (date == null || event.date == date)
            matchesCategory && matchesDate
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dates with active events for calendar dot indicators
    val eventDatesMap: StateFlow<Map<String, List<String>>> = allEvents.map { events ->
        events.groupBy { it.date }.mapValues { entry -> entry.value.map { it.eventType } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun selectDate(date: String?) {
        selectedDate.value = date
    }

    fun selectCategory(category: String) {
        selectedCategory.value = category
    }

    fun toggleReminder(event: EventEntity, newStatus: Boolean) {
        viewModelScope.launch {
            clubRepository.toggleEventReminder(event.id, newStatus)
            firestoreRepository.updateEventReminder(event.id, newStatus)
        }
    }

    fun addNewEvent(event: EventEntity) {
        viewModelScope.launch {
            val generatedId = "EVT-${System.currentTimeMillis()}"
            val finalEvent = event.copy(id = generatedId)
            clubRepository.insertEvent(finalEvent)
            firestoreRepository.addEvent(finalEvent)

            // Log activity
            val log = ActivityLogEntity(
                id = "LOG-${System.currentTimeMillis()}",
                actionType = "EVENT_SCHEDULED",
                adminId = "ADM-001",
                adminName = "Club Management",
                titleEn = "Scheduled New ${event.eventType}: ${event.titleEn}",
                titleBn = "নতুন ${event.eventType} নির্ধারণ করা হয়েছে: ${event.titleBn}",
                detailsEn = "Scheduled for ${event.date} at ${event.time} (${event.locationEn})",
                detailsBn = "তারিখ: ${event.date}, সময়: ${event.time} (${event.locationBn})",
                timestamp = java.time.LocalDateTime.now().toString(),
                targetId = generatedId
            )
            clubRepository.insertActivityLog(log)
            firestoreRepository.addActivityLog(log)
        }
    }
}
