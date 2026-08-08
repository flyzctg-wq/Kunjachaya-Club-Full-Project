package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.EventEntity
import com.example.ui.language.Language
import com.example.ui.viewmodel.CalendarViewModel
import com.example.ui.viewmodel.ClubViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsCalendarScreen(
    clubViewModel: ClubViewModel,
    calendarViewModel: CalendarViewModel = viewModel()
) {
    val lang by clubViewModel.language.collectAsState()
    val allEvents by calendarViewModel.allEvents.collectAsState()
    val filteredEvents by calendarViewModel.filteredEvents.collectAsState()
    val selectedDate by calendarViewModel.selectedDate.collectAsState()
    val selectedCategory by calendarViewModel.selectedCategory.collectAsState()
    val eventDatesMap by calendarViewModel.eventDatesMap.collectAsState()

    val context = LocalContext.current
    var showAddEventDialog by remember { mutableStateOf(false) }
    var currentMonthYear by remember { mutableStateOf("2026-07") } // "2026-07" or "2026-08"

    val eventsCount = allEvents.count { it.eventType == "EVENT" }
    val meetingsCount = allEvents.count { it.eventType == "MEETING" }
    val deadlinesCount = allEvents.count { it.eventType == "PAYMENT_DEADLINE" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (lang == Language.BN) "ক্লাব ক্যালেন্ডার ও সময়সূচী" else "Events Calendar & Deadlines",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (lang == Language.BN) "ইভেন্ট, মিটিং ও বিল পরিশোধের সময়সীমা" else "Upcoming Events, Meetings & Reminders",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            calendarViewModel.selectDate(null)
                            calendarViewModel.selectCategory("ALL")
                        },
                        modifier = Modifier.testTag("reset_calendar_filter_btn")
                    ) {
                        Icon(Icons.Default.FilterListOff, contentDescription = "Reset Filters")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddEventDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(if (lang == Language.BN) "নতুন সূচি যোগ" else "Add Event") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_event_fab")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // --- 1. OVERVIEW SUMMARY CARDS ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EventSummaryBox(
                        title = if (lang == Language.BN) "ক্লাব ইভেন্ট" else "Club Events",
                        count = "$eventsCount",
                        icon = Icons.Default.Festival,
                        color = Color(0xFF0288D1),
                        modifier = Modifier.weight(1f)
                    )
                    EventSummaryBox(
                        title = if (lang == Language.BN) "বোর্ড মিটিং" else "Meetings",
                        count = "$meetingsCount",
                        icon = Icons.Default.Groups,
                        color = Color(0xFF6A1B9A),
                        modifier = Modifier.weight(1f)
                    )
                    EventSummaryBox(
                        title = if (lang == Language.BN) "পেমেন্ট ডেডলাইন" else "Deadlines",
                        count = "$deadlinesCount",
                        icon = Icons.Default.EventRepeat,
                        color = Color(0xFFD32F2F),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // --- 2. MONTH CALENDAR GRID PICKER ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Month Header Navigation
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { currentMonthYear = "2026-07" }) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                            }

                            Text(
                                text = if (currentMonthYear == "2026-07") {
                                    if (lang == Language.BN) "জুলাই ২০২৬" else "July 2026"
                                } else {
                                    if (lang == Language.BN) "আগস্ট ২০২৬" else "August 2026"
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            IconButton(onClick = { currentMonthYear = "2026-08" }) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Days of week header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            val daysOfWeek = if (lang == Language.BN) {
                                listOf("রবি", "সোম", "মঙ্গল", "বুধ", "বৃহঃ", "শুক্র", "শনি")
                            } else {
                                listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                            }
                            daysOfWeek.forEach { day ->
                                Text(
                                    text = day,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(36.dp),
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Date Grid (July = 31 days, August = 31 days)
                        val totalDays = 31
                        val daysList = (1..totalDays).map { day ->
                            val formattedDay = if (day < 10) "0$day" else "$day"
                            "$currentMonthYear-$formattedDay"
                        }

                        // Grid displaying dates
                        Column {
                            daysList.chunked(7).forEach { week ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    week.forEach { dateStr ->
                                        val dayNum = dateStr.takeLast(2)
                                        val isSelected = (selectedDate == dateStr)
                                        val hasEvents = eventDatesMap.containsKey(dateStr)
                                        val eventTypes = eventDatesMap[dateStr] ?: emptyList()

                                        CalendarDayCell(
                                            dayNumber = dayNum,
                                            isSelected = isSelected,
                                            hasEvents = hasEvents,
                                            eventTypes = eventTypes,
                                            onClick = {
                                                if (selectedDate == dateStr) {
                                                    calendarViewModel.selectDate(null)
                                                } else {
                                                    calendarViewModel.selectDate(dateStr)
                                                }
                                            }
                                        )
                                    }
                                    // Pad empty spaces if last week has less than 7 days
                                    repeat(7 - week.size) {
                                        Spacer(modifier = Modifier.width(36.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Selected Date Indicator / Reset Chip
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (selectedDate != null) {
                                    if (lang == Language.BN) "নির্বাচিত তারিখ: $selectedDate" else "Selected Date: $selectedDate"
                                } else {
                                    if (lang == Language.BN) "সব তারিখের ইভেন্ট দেখানো হচ্ছে" else "Showing All Calendar Dates"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (selectedDate != null) {
                                SuggestionChip(
                                    onClick = { calendarViewModel.selectDate(null) },
                                    label = { Text(if (lang == Language.BN) "সব দেখুন" else "Clear Date", fontSize = 10.sp) },
                                    icon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(12.dp)) },
                                    modifier = Modifier.testTag("clear_date_chip")
                                )
                            }
                        }
                    }
                }
            }

            // --- 3. CATEGORY FILTER ROW ---
            item {
                Column {
                    Text(
                        text = if (lang == Language.BN) "ক্যাটাগরি অনুযায়ী ফিল্টার" else "Filter by Event Category",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedCategory == "ALL",
                                onClick = { calendarViewModel.selectCategory("ALL") },
                                label = { Text(if (lang == Language.BN) "সবগুলো (${allEvents.size})" else "All (${allEvents.size})") },
                                modifier = Modifier.testTag("filter_all_events")
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedCategory == "EVENT",
                                onClick = { calendarViewModel.selectCategory("EVENT") },
                                label = { Text(if (lang == Language.BN) "ক্লাব ইভেন্ট ($eventsCount)" else "Club Events ($eventsCount)") },
                                leadingIcon = { Icon(Icons.Default.Festival, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.testTag("filter_club_events")
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedCategory == "MEETING",
                                onClick = { calendarViewModel.selectCategory("MEETING") },
                                label = { Text(if (lang == Language.BN) "বোর্ড মিটিং ($meetingsCount)" else "Meetings ($meetingsCount)") },
                                leadingIcon = { Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.testTag("filter_meetings")
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedCategory == "PAYMENT_DEADLINE",
                                onClick = { calendarViewModel.selectCategory("PAYMENT_DEADLINE") },
                                label = { Text(if (lang == Language.BN) "পেমেন্ট ডেডলাইন ($deadlinesCount)" else "Payment Deadlines ($deadlinesCount)") },
                                leadingIcon = { Icon(Icons.Default.EventRepeat, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.testTag("filter_payment_deadlines")
                            )
                        }
                    }
                }
            }

            // --- 4. SCHEDULED EVENTS LIST ---
            if (filteredEvents.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.EventBusy, contentDescription = null, modifier = Modifier.size(40.dp), tint = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (lang == Language.BN) "এই তারিখে বা ফিল্টারে কোনো নির্ধারিত সূচি নেই" else "No events scheduled for selected date or filter",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                items(filteredEvents) { event ->
                    EventCalendarCard(
                        event = event,
                        lang = lang,
                        onToggleReminder = { newStatus ->
                            calendarViewModel.toggleReminder(event, newStatus)
                            val title = if (lang == Language.BN) event.titleBn else event.titleEn
                            val msg = if (newStatus) {
                                if (lang == Language.BN) "⏰ '$title' এর জন্য রিমাইন্ডার সেট করা হয়েছে!" else "⏰ Reminder active for '$title'!"
                            } else {
                                if (lang == Language.BN) "রিমাইন্ডার বন্ধ করা হয়েছে" else "Reminder turned off"
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    // Add New Event Dialog
    if (showAddEventDialog) {
        AddEventDialog(
            lang = lang,
            onDismiss = { showAddEventDialog = false },
            onAddEvent = { newEvent ->
                calendarViewModel.addNewEvent(newEvent)
                showAddEventDialog = false
                Toast.makeText(
                    context,
                    if (lang == Language.BN) "নতুন সূচি সফলভাবে যোগ করা হয়েছে!" else "New event scheduled successfully!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }
}

@Composable
fun EventSummaryBox(
    title: String,
    count: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(count, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
            Text(title, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun CalendarDayCell(
    dayNumber: String,
    isSelected: Boolean,
    hasEvents: Boolean,
    eventTypes: List<String>,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(vertical = 4.dp)
            .testTag("calendar_day_$dayNumber")
    ) {
        Text(
            text = dayNumber,
            fontSize = 12.sp,
            fontWeight = if (isSelected || hasEvents) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Indicator dots for events
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (eventTypes.contains("EVENT")) {
                Box(modifier = Modifier.size(4.dp).background(if (isSelected) Color.White else Color(0xFF0288D1), CircleShape))
            }
            if (eventTypes.contains("MEETING")) {
                Box(modifier = Modifier.size(4.dp).background(if (isSelected) Color.White else Color(0xFF6A1B9A), CircleShape))
            }
            if (eventTypes.contains("PAYMENT_DEADLINE")) {
                Box(modifier = Modifier.size(4.dp).background(if (isSelected) Color.White else Color(0xFFD32F2F), CircleShape))
            }
        }
    }
}

@Composable
fun EventCalendarCard(
    event: EventEntity,
    lang: Language,
    onToggleReminder: (Boolean) -> Unit
) {
    val categoryColor = when (event.eventType) {
        "MEETING" -> Color(0xFF6A1B9A)
        "PAYMENT_DEADLINE" -> Color(0xFFD32F2F)
        else -> Color(0xFF0288D1)
    }

    val categoryIcon = when (event.eventType) {
        "MEETING" -> Icons.Default.Groups
        "PAYMENT_DEADLINE" -> Icons.Default.EventRepeat
        else -> Icons.Default.Festival
    }

    val categoryLabel = when (event.eventType) {
        "MEETING" -> if (lang == Language.BN) "বোর্ড মিটিং" else "Meeting"
        "PAYMENT_DEADLINE" -> if (lang == Language.BN) "পেমেন্ট ডেডলাইন" else "Payment Deadline"
        else -> if (lang == Language.BN) "ক্লাব ইভেন্ট" else "Club Event"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("event_card_${event.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Tag & Date Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = categoryColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(categoryIcon, contentDescription = null, tint = categoryColor, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(categoryLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = categoryColor)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${event.date} • ${event.time}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title & Amount
            Text(
                text = if (lang == Language.BN) event.titleBn else event.titleEn,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (event.amount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (lang == Language.BN) "প্রদেয় পরিমাণ: ৳ ${event.amount.toInt()}" else "Amount Due: ৳ ${event.amount.toInt()}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD32F2F)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Description
            Text(
                text = if (lang == Language.BN) event.descriptionBn else event.descriptionEn,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Location & Reminder Toggle Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (lang == Language.BN) event.locationBn else event.locationEn,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Interactive Reminder Toggle Button
                OutlinedButton(
                    onClick = { onToggleReminder(!event.isReminderSet) },
                    shape = RoundedCornerShape(20.dp),
                    colors = if (event.isReminderSet) {
                        ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF2E7D32).copy(alpha = 0.12f))
                    } else {
                        ButtonDefaults.outlinedButtonColors()
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("reminder_toggle_btn_${event.id}")
                ) {
                    Icon(
                        imageVector = if (event.isReminderSet) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                        contentDescription = null,
                        tint = if (event.isReminderSet) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (event.isReminderSet) {
                            if (lang == Language.BN) "রিমাইন্ডার চালু ⏰" else "Reminder Active ⏰"
                        } else {
                            if (lang == Language.BN) "রিমাইন্ডার সেট করুন 🔔" else "Set Reminder 🔔"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (event.isReminderSet) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun AddEventDialog(
    lang: Language,
    onDismiss: () -> Unit,
    onAddEvent: (EventEntity) -> Unit
) {
    var titleEn by remember { mutableStateOf("") }
    var titleBn by remember { mutableStateOf("") }
    var descEn by remember { mutableStateOf("") }
    var descBn by remember { mutableStateOf("") }
    var eventType by remember { mutableStateOf("EVENT") } // EVENT, MEETING, PAYMENT_DEADLINE
    var date by remember { mutableStateOf(java.time.LocalDate.now().toString()) }
    var time by remember { mutableStateOf("18:00") }
    var locationEn by remember { mutableStateOf("Kunjachaya Club Hall") }
    var locationBn by remember { mutableStateOf("কুঞ্জছায়া ক্লাব হল") }
    var amountText by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (lang == Language.BN) "নতুন ক্যালেন্ডার সূচি তৈরি করুন" else "Schedule New Calendar Event") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(if (lang == Language.BN) "সূচির ধরণ (Category):" else "Event Category:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = eventType == "EVENT",
                            onClick = { eventType = "EVENT" },
                            label = { Text("Event", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f).testTag("dialog_chip_event")
                        )
                        FilterChip(
                            selected = eventType == "MEETING",
                            onClick = { eventType = "MEETING" },
                            label = { Text("Meeting", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f).testTag("dialog_chip_meeting")
                        )
                        FilterChip(
                            selected = eventType == "PAYMENT_DEADLINE",
                            onClick = { eventType = "PAYMENT_DEADLINE" },
                            label = { Text("Deadline", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f).testTag("dialog_chip_deadline")
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = titleEn,
                        onValueChange = { titleEn = it },
                        label = { Text("Title (English)") },
                        modifier = Modifier.fillMaxWidth().testTag("add_event_title_en")
                    )
                }

                item {
                    OutlinedTextField(
                        value = titleBn,
                        onValueChange = { titleBn = it },
                        label = { Text("Title (বাংলা)") },
                        modifier = Modifier.fillMaxWidth().testTag("add_event_title_bn")
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = date,
                            onValueChange = { date = it },
                            label = { Text("Date (YYYY-MM-DD)") },
                            modifier = Modifier.weight(1f).testTag("add_event_date")
                        )
                        OutlinedTextField(
                            value = time,
                            onValueChange = { time = it },
                            label = { Text("Time (HH:MM)") },
                            modifier = Modifier.weight(1f).testTag("add_event_time")
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = locationEn,
                        onValueChange = { locationEn = it },
                        label = { Text("Location (English)") },
                        modifier = Modifier.fillMaxWidth().testTag("add_event_location_en")
                    )
                }

                item {
                    OutlinedTextField(
                        value = locationBn,
                        onValueChange = { locationBn = it },
                        label = { Text("Location (বাংলা)") },
                        modifier = Modifier.fillMaxWidth().testTag("add_event_location_bn")
                    )
                }

                if (eventType == "PAYMENT_DEADLINE") {
                    item {
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            label = { Text("Amount Due (৳)") },
                            modifier = Modifier.fillMaxWidth().testTag("add_event_amount")
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = descEn,
                        onValueChange = { descEn = it },
                        label = { Text("Description (English)") },
                        modifier = Modifier.fillMaxWidth().testTag("add_event_desc_en")
                    )
                }

                item {
                    OutlinedTextField(
                        value = descBn,
                        onValueChange = { descBn = it },
                        label = { Text("Description (বাংলা)") },
                        modifier = Modifier.fillMaxWidth().testTag("add_event_desc_bn")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (titleEn.isNotBlank() || titleBn.isNotBlank()) {
                        onAddEvent(
                            EventEntity(
                                titleEn = titleEn.ifBlank { titleBn },
                                titleBn = titleBn.ifBlank { titleEn },
                                descriptionEn = descEn.ifBlank { "Scheduled club event" },
                                descriptionBn = descBn.ifBlank { "নির্ধারিত ক্লাব ইভেন্ট" },
                                eventType = eventType,
                                date = date,
                                time = time,
                                locationEn = locationEn,
                                locationBn = locationBn,
                                amount = amountText.toDoubleOrNull() ?: 0.0,
                                isReminderSet = true
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("confirm_add_event_btn")
            ) {
                Text(if (lang == Language.BN) "সংরক্ষণ করুন" else "Save Event")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (lang == Language.BN) "বাতিল" else "Cancel")
            }
        }
    )
}
