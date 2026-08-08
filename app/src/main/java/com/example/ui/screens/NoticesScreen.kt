package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.AnnouncementEntity
import com.example.ui.language.AppLanguage
import com.example.ui.language.Language
import com.example.ui.viewmodel.ClubViewModel
import com.example.ui.viewmodel.NoticesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticesScreen(
    clubViewModel: ClubViewModel,
    noticesViewModel: NoticesViewModel = viewModel()
) {
    val lang by clubViewModel.language.collectAsState()
    val currentUser by clubViewModel.currentUser.collectAsState()
    val notices by noticesViewModel.filteredNotices.collectAsState()
    val isLoading by noticesViewModel.isLoading.collectAsState()
    val selectedCategory by noticesViewModel.selectedCategory.collectAsState()
    val isOfflineCachedMode by noticesViewModel.isOfflineCachedMode.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddNoticeDialog by remember { mutableStateOf(false) }

    val categories = listOf("All", "Urgent Notice", "General Notice", "Maintenance", "Upcoming Event")

    // Filter by search query
    val displayedNotices = remember(notices, searchQuery, lang) {
        if (searchQuery.isBlank()) {
            notices
        } else {
            notices.filter {
                it.titleEn.contains(searchQuery, ignoreCase = true) ||
                it.titleBn.contains(searchQuery, ignoreCase = true) ||
                it.descriptionEn.contains(searchQuery, ignoreCase = true) ||
                it.descriptionBn.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (lang == Language.BN) "ঘোষণা ও নোটিশ বোর্ড" else "Announcements & Notices",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isOfflineCachedMode) Icons.Default.CloudOff else Icons.Default.CloudDone,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = if (isOfflineCachedMode) Color(0xFFE65100) else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isOfflineCachedMode) {
                                    if (lang == Language.BN) "রুম লোকাল ক্যাশ (অফলাইন মোড)" else "Room Local Cache (Offline)"
                                } else {
                                    if (lang == Language.BN) "ফায়ারস্টোর লাইভ + রুম ক্যাশড" else "Firestore Live + Room Cached"
                                },
                                fontSize = 11.sp,
                                color = if (isOfflineCachedMode) Color(0xFFE65100) else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    if (currentUser?.hasNoticePermission() == true) {
                        IconButton(
                            onClick = { showAddNoticeDialog = true },
                            modifier = Modifier.testTag("add_notice_button")
                        ) {
                            Icon(
                                Icons.Default.AddAlert,
                                contentDescription = "Add Notice",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (currentUser?.hasNoticePermission() == true) {
                ExtendedFloatingActionButton(
                    onClick = { showAddNoticeDialog = true },
                    icon = { Icon(Icons.Default.Campaign, contentDescription = null) },
                    text = { Text(if (lang == Language.BN) "নতুন নোটিশ প্রকাশ" else "Publish Notice") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_publish_notice")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("search_notices_input"),
                placeholder = {
                    Text(
                        text = if (lang == Language.BN) "নোটিশ খুঁজুন..." else "Search notices..."
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Category Chips Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                items(categories) { category ->
                    val isSelected = selectedCategory.equals(category, ignoreCase = true)
                    val label = when (category) {
                        "All" -> if (lang == Language.BN) "সকল" else "All"
                        "Urgent Notice" -> if (lang == Language.BN) "জরুরী নোটিশ" else "Urgent Notice"
                        "General Notice" -> if (lang == Language.BN) "সাধারণ বিজ্ঞপ্তি" else "General Notice"
                        "Maintenance" -> if (lang == Language.BN) "রক্ষণাবেক্ষণ" else "Maintenance"
                        "Upcoming Event" -> if (lang == Language.BN) "আগামী ইভেন্ট" else "Upcoming Event"
                        else -> category
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = { noticesViewModel.setCategoryFilter(category) },
                        label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.testTag("filter_chip_$category")
                    )
                }
            }

            // Header Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (lang == Language.BN) "অফিসিয়াল বুলেটিন ও সিকিউরিটি আপডেট" else "Official Bulletins & Security Alerts",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (lang == Language.BN) "কুঞ্জছায়া ক্লাব পরিচালনা পর্ষদ কর্তৃক প্রকাশিত" else "Published by Kunjachaya Club Committee",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Loading / Empty / Content View
            if (isLoading && displayedNotices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (displayedNotices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.MarkEmailUnread,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (lang == Language.BN) "কোন নোটিশ পাওয়া যায়নি" else "No notices found",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                // Vertical Scrollable List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("notices_vertical_list"),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayedNotices, key = { it.id }) { notice ->
                        NoticeVerticalCard(notice = notice, lang = lang)
                    }
                    item {
                        Spacer(modifier = Modifier.height(64.dp))
                    }
                }
            }
        }
    }

    // Add Notice Dialog for Admin
    if (showAddNoticeDialog) {
        AddNoticeDialog(
            lang = lang,
            onDismiss = { showAddNoticeDialog = false },
            onSubmit = { titleEn, titleBn, descEn, descBn, categoryEn, categoryBn, priority ->
                noticesViewModel.publishNoticeToFirestore(
                    titleEn = titleEn,
                    titleBn = titleBn,
                    descriptionEn = descEn,
                    descriptionBn = descBn,
                    categoryEn = categoryEn,
                    categoryBn = categoryBn,
                    priority = priority
                )
                showAddNoticeDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticeVerticalCard(notice: AnnouncementEntity, lang: Language) {
    var expanded by remember { mutableStateOf(false) }
    val isHighPriority = notice.priority.equals("High", ignoreCase = true) || notice.categoryEn.contains("Urgent", ignoreCase = true)

    val cardBg = if (isHighPriority) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val chipBg = if (isHighPriority) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .testTag("notice_card_${notice.id}"),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Priority Tag & Date Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = chipBg,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (lang == Language.BN) notice.categoryBn.ifEmpty { notice.categoryEn } else notice.categoryEn,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    if (isHighPriority) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.error,
                            shape = CircleShape
                        ) {
                            Box(modifier = Modifier.size(8.dp))
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = notice.date,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Primary Title (Current Language)
            Text(
                text = if (lang == Language.BN) notice.titleBn.ifEmpty { notice.titleEn } else notice.titleEn,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Secondary Title (Alternate Language Header for Bilingual Context)
            val secondaryTitle = if (lang == Language.BN) notice.titleEn else notice.titleBn
            if (secondaryTitle.isNotEmpty()) {
                Text(
                    text = secondaryTitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Description Body
            Text(
                text = if (lang == Language.BN) notice.descriptionBn.ifEmpty { notice.descriptionEn } else notice.descriptionEn,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis
            )

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Alternate Language Full Description for Bilingual Detail
                    val secondaryDesc = if (lang == Language.BN) notice.descriptionEn else notice.descriptionBn
                    if (secondaryDesc.isNotEmpty()) {
                        Text(
                            text = if (lang == Language.BN) "English Summary:" else "বাংলা বিবরণ:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = secondaryDesc,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${if (lang == Language.BN) "প্রকাশক: " else "Publisher: "}${notice.author}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${if (lang == Language.BN) "অগ্রাধিকার: " else "Priority: "}${notice.priority}",
                            fontSize = 11.sp,
                            color = if (isHighPriority) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = if (expanded) (if (lang == Language.BN) "সংক্ষেপ করুন ▲" else "Show Less ▲")
                           else (if (lang == Language.BN) "বিস্তারিত দেখুন ▼" else "Read More ▼"),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun AddNoticeDialog(
    lang: Language,
    onDismiss: () -> Unit,
    onSubmit: (titleEn: String, titleBn: String, descEn: String, descBn: String, categoryEn: String, categoryBn: String, priority: String) -> Unit
) {
    var titleEn by remember { mutableStateOf("") }
    var titleBn by remember { mutableStateOf("") }
    var descEn by remember { mutableStateOf("") }
    var descBn by remember { mutableStateOf("") }
    var categoryEn by remember { mutableStateOf("Urgent Notice") }
    var categoryBn by remember { mutableStateOf("জরুরী নোটিশ") }
    var priority by remember { mutableStateOf("High") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (lang == Language.BN) "নতুন নোটিশ প্রকাশ করুন (ফায়ারস্টোর)" else "Publish New Notice (Firestore)",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = titleEn,
                    onValueChange = { titleEn = it },
                    label = { Text("Title (English)") },
                    modifier = Modifier.fillMaxWidth().testTag("input_title_en"),
                    singleLine = true
                )
                OutlinedTextField(
                    value = titleBn,
                    onValueChange = { titleBn = it },
                    label = { Text("Title (বাংলা)") },
                    modifier = Modifier.fillMaxWidth().testTag("input_title_bn"),
                    singleLine = true
                )
                OutlinedTextField(
                    value = descEn,
                    onValueChange = { descEn = it },
                    label = { Text("Description (English)") },
                    modifier = Modifier.fillMaxWidth().testTag("input_desc_en"),
                    maxLines = 3
                )
                OutlinedTextField(
                    value = descBn,
                    onValueChange = { descBn = it },
                    label = { Text("Description (বাংলা)") },
                    modifier = Modifier.fillMaxWidth().testTag("input_desc_bn"),
                    maxLines = 3
                )

                // Category Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = priority == "High",
                        onClick = {
                            priority = "High"
                            categoryEn = "Urgent Notice"
                            categoryBn = "জরুরী নোটিশ"
                        },
                        label = { Text(if (lang == Language.BN) "জরুরী" else "Urgent") }
                    )
                    FilterChip(
                        selected = priority == "Medium",
                        onClick = {
                            priority = "Medium"
                            categoryEn = "General Notice"
                            categoryBn = "সাধারণ বিজ্ঞপ্তি"
                        },
                        label = { Text(if (lang == Language.BN) "সাধারণ" else "General") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (titleEn.isNotBlank() || titleBn.isNotBlank()) {
                        onSubmit(
                            titleEn.ifBlank { titleBn },
                            titleBn.ifBlank { titleEn },
                            descEn.ifBlank { descBn },
                            descBn.ifBlank { descEn },
                            categoryEn,
                            categoryBn,
                            priority
                        )
                    }
                },
                modifier = Modifier.testTag("submit_notice_btn")
            ) {
                Text(if (lang == Language.BN) "প্রকাশ করুন" else "Publish")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (lang == Language.BN) "বাতিল" else "Cancel")
            }
        }
    )
}
