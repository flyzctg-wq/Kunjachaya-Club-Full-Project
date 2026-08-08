package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AnnouncementEntity
import com.example.data.model.ActivityEntity
import com.example.ui.components.ComplaintDashboardSummaryCard
import com.example.ui.language.AppLanguage
import com.example.ui.language.Language
import com.example.ui.viewmodel.ClubViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: ClubViewModel,
    onNavigateToFinancials: () -> Unit,
    onNavigateToComplaints: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSchemaDocs: () -> Unit,
    onNavigateToNotices: () -> Unit = {},
    onNavigateToCalendar: () -> Unit = {},
    onNavigateToDirectory: () -> Unit = {}
) {
    val lang by viewModel.language.collectAsState()
    val user by viewModel.currentUser.collectAsState()
    val announcements by viewModel.allAnnouncements.collectAsState()
    val activities by viewModel.allActivities.collectAsState()
    val financials by viewModel.userFinancials.collectAsState()
    val userComplaints by viewModel.userComplaints.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var searchCategoryFilter by remember { mutableStateOf("All") }

    val trimmedQuery = searchQuery.trim()

    // Filtered lists for Global Search
    val matchingNotices = remember(trimmedQuery, announcements) {
        if (trimmedQuery.isEmpty()) emptyList()
        else announcements.filter { notice ->
            notice.titleEn.contains(trimmedQuery, ignoreCase = true) ||
            notice.titleBn.contains(trimmedQuery, ignoreCase = true) ||
            notice.descriptionEn.contains(trimmedQuery, ignoreCase = true) ||
            notice.descriptionBn.contains(trimmedQuery, ignoreCase = true) ||
            notice.categoryEn.contains(trimmedQuery, ignoreCase = true) ||
            notice.categoryBn.contains(trimmedQuery, ignoreCase = true)
        }
    }

    val matchingComplaints = remember(trimmedQuery, userComplaints) {
        if (trimmedQuery.isEmpty()) emptyList()
        else userComplaints.filter { complaint ->
            complaint.titleEn.contains(trimmedQuery, ignoreCase = true) ||
            complaint.titleBn.contains(trimmedQuery, ignoreCase = true) ||
            complaint.descriptionEn.contains(trimmedQuery, ignoreCase = true) ||
            complaint.descriptionBn.contains(trimmedQuery, ignoreCase = true) ||
            complaint.categoryEn.contains(trimmedQuery, ignoreCase = true) ||
            complaint.categoryBn.contains(trimmedQuery, ignoreCase = true) ||
            complaint.status.contains(trimmedQuery, ignoreCase = true)
        }
    }

    val matchingFinancials = remember(trimmedQuery, financials) {
        if (trimmedQuery.isEmpty()) emptyList()
        else financials.filter { record ->
            record.titleEn.contains(trimmedQuery, ignoreCase = true) ||
            record.titleBn.contains(trimmedQuery, ignoreCase = true) ||
            record.monthYear.contains(trimmedQuery, ignoreCase = true) ||
            record.transactionId.contains(trimmedQuery, ignoreCase = true) ||
            record.paymentGateway.contains(trimmedQuery, ignoreCase = true) ||
            record.status.contains(trimmedQuery, ignoreCase = true) ||
            record.type.contains(trimmedQuery, ignoreCase = true)
        }
    }

    val totalSearchMatches = matchingNotices.size + matchingComplaints.size + matchingFinancials.size

    val pendingDues = financials.filter { it.status == "Pending" }.sumOf { it.amount }
    val totalPaid = financials.filter { it.status == "Completed" && it.type == "Paid" }.sumOf { it.amount }

    val pendingComplaintsCount = userComplaints.count { it.status.equals("Pending", ignoreCase = true) }
    val inProgressComplaintsCount = userComplaints.count { it.status.equals("Under Review", ignoreCase = true) || it.status.contains("progress", ignoreCase = true) }
    val resolvedComplaintsCount = userComplaints.count { it.status.equals("Resolved", ignoreCase = true) }

    fun formatMoney(amount: Double): String {
        val rounded = amount.toInt()
        return if (lang == Language.BN) {
            "৳ " + rounded.toString()
                .replace('0', '০')
                .replace('1', '১')
                .replace('2', '২')
                .replace('3', '৩')
                .replace('4', '৪')
                .replace('5', '৫')
                .replace('6', '৬')
                .replace('7', '৭')
                .replace('8', '৮')
                .replace('9', '৯')
        } else {
            "৳ %,d".format(rounded)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.img_club_logo),
                            contentDescription = "Club Logo",
                            modifier = Modifier.size(36.dp).clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                AppLanguage.clubName(lang),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = user?.let { if (lang == Language.BN) it.nameBn else it.nameEn } ?: "",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    val isDarkTheme by viewModel.isDarkTheme.collectAsState()

                    // Global Theme Toggle
                    IconButton(
                        onClick = { viewModel.toggleTheme() },
                        modifier = Modifier.testTag("global_theme_toggle_btn")
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Dark/Light Mode",
                            tint = if (isDarkTheme) Color(0xFFFFD54F) else MaterialTheme.colorScheme.primary
                        )
                    }

                    // Language Switch Button
                    FilterChip(
                        selected = lang == Language.BN,
                        onClick = { viewModel.toggleLanguage() },
                        label = {
                            Text(
                                if (lang == Language.BN) "বাংলা" else "EN",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Language,
                                contentDescription = "Language Switcher",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.padding(end = 8.dp).testTag("dash_lang_toggle")
                    )
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Global Search Bar
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dashboard_global_search_input"),
                        placeholder = {
                            Text(
                                text = if (lang == Language.BN) "নোটিশ, অভিযোগ বা আর্থিক রেকর্ড খুঁজুন..." else "Search notices, complaints, financials...",
                                fontSize = 13.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search Icon",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.testTag("clear_search_btn")
                                ) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Clear Search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    // Quick Search Filter Chips when searching
                    if (trimmedQuery.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            item {
                                FilterChip(
                                    selected = searchCategoryFilter == "All",
                                    onClick = { searchCategoryFilter = "All" },
                                    label = { Text(if (lang == Language.BN) "সব ($totalSearchMatches)" else "All ($totalSearchMatches)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.testTag("filter_all_results")
                                )
                            }
                            item {
                                FilterChip(
                                    selected = searchCategoryFilter == "Notices",
                                    onClick = { searchCategoryFilter = "Notices" },
                                    label = { Text(if (lang == Language.BN) "নোটিশ (${matchingNotices.size})" else "Notices (${matchingNotices.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    leadingIcon = { Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                    modifier = Modifier.testTag("filter_notice_results")
                                )
                            }
                            item {
                                FilterChip(
                                    selected = searchCategoryFilter == "Complaints",
                                    onClick = { searchCategoryFilter = "Complaints" },
                                    label = { Text(if (lang == Language.BN) "অভিযোগ (${matchingComplaints.size})" else "Complaints (${matchingComplaints.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    leadingIcon = { Icon(Icons.Default.ReportProblem, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                    modifier = Modifier.testTag("filter_complaint_results")
                                )
                            }
                            item {
                                FilterChip(
                                    selected = searchCategoryFilter == "Financials",
                                    onClick = { searchCategoryFilter = "Financials" },
                                    label = { Text(if (lang == Language.BN) "আর্থিক (${matchingFinancials.size})" else "Financials (${matchingFinancials.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    leadingIcon = { Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                    modifier = Modifier.testTag("filter_financial_results")
                                )
                            }
                        }
                    }
                }
            }

            if (trimmedQuery.isNotEmpty()) {
                // Render Search Results Mode
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (lang == Language.BN) "অনুসন্ধান ফলাফল ($totalSearchMatches)" else "Search Results ($totalSearchMatches)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(onClick = { searchQuery = "" }) {
                            Text(
                                if (lang == Language.BN) "অনুসন্ধান মুছুন" else "Clear Search",
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                if (totalSearchMatches == 0) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.SearchOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (lang == Language.BN) "\"$trimmedQuery\" এর জন্য কোন তথ্য পাওয়া যায়নি" else "No matching records found for \"$trimmedQuery\"",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    // Matching Notices
                    if ((searchCategoryFilter == "All" || searchCategoryFilter == "Notices") && matchingNotices.isNotEmpty()) {
                        item {
                            Text(
                                text = if (lang == Language.BN) "📢 নোটিশসমূহ (${matchingNotices.size})" else "📢 Notices & Announcements (${matchingNotices.size})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        items(matchingNotices) { notice ->
                            SearchResultNoticeItem(
                                notice = notice,
                                lang = lang,
                                onClick = onNavigateToNotices
                            )
                        }
                    }

                    // Matching Complaints
                    if ((searchCategoryFilter == "All" || searchCategoryFilter == "Complaints") && matchingComplaints.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (lang == Language.BN) "⚠️ অভিযোগসমূহ (${matchingComplaints.size})" else "⚠️ Active Complaints (${matchingComplaints.size})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        items(matchingComplaints) { complaint ->
                            SearchResultComplaintItem(
                                complaint = complaint,
                                lang = lang,
                                onClick = onNavigateToComplaints
                            )
                        }
                    }

                    // Matching Financials
                    if ((searchCategoryFilter == "All" || searchCategoryFilter == "Financials") && matchingFinancials.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (lang == Language.BN) "💳 আর্থিক রেকর্ডস (${matchingFinancials.size})" else "💳 Financial Records (${matchingFinancials.size})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        items(matchingFinancials) { record ->
                            SearchResultFinancialItem(
                                record = record,
                                lang = lang,
                                formatMoney = ::formatMoney,
                                onClick = onNavigateToFinancials
                            )
                        }
                    }
                }
            } else {
                // Regular Dashboard View when query is empty

                // Hero Community Banner Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Box(modifier = Modifier.height(160.dp).fillMaxWidth()) {
                            Image(
                                painter = painterResource(id = R.drawable.img_club_banner),
                                contentDescription = "Club Grounds",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.45f))
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = if (lang == Language.BN) "আবাসিক নিবাসী সোসাইটি" else "Gated Society",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = if (user?.membershipStatus == "Active") Color(0xFF2E7D32) else Color(0xFFE65100),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = if (user?.membershipStatus == "Active") AppLanguage.active(lang) else AppLanguage.pending(lang),
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (lang == Language.BN) "কুঞ্জছায়া ক্লাবে আপনাকে স্বাগতম" else "Welcome to Kunjachaya Club",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${user?.holding} • ${user?.road}",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

            // Financial Summary Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Dues Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToFinancials() }
                            .testTag("dues_summary_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = AppLanguage.duesAmount(lang),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Icon(
                                    Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = formatMoney(pendingDues),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.error,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = AppLanguage.payDues(lang),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    // Total Paid Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToFinancials() }
                            .testTag("paid_summary_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = AppLanguage.totalPaid(lang),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = formatMoney(totalPaid),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = AppLanguage.makeDonation(lang),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Complaint Status Tracker Widget
            item {
                ComplaintDashboardSummaryCard(
                    pendingCount = pendingComplaintsCount,
                    inProgressCount = inProgressComplaintsCount,
                    resolvedCount = resolvedComplaintsCount,
                    lang = lang,
                    onClick = onNavigateToComplaints
                )
            }

            // Quick Actions Grid
            item {
                Column {
                    Text(
                        text = if (lang == Language.BN) "দ্রুত সার্ভিসসমূহ" else "Quick Actions",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ActionChip(
                            icon = Icons.Default.People,
                            label = if (lang == Language.BN) "ডিরেক্টরি" else "Directory",
                            onClick = onNavigateToDirectory,
                            modifier = Modifier.weight(1f).testTag("action_directory")
                        )
                        ActionChip(
                            icon = Icons.Default.Event,
                            label = if (lang == Language.BN) "ক্যালেন্ডার" else "Calendar",
                            onClick = onNavigateToCalendar,
                            modifier = Modifier.weight(1f).testTag("action_calendar")
                        )
                        ActionChip(
                            icon = Icons.Default.ReportProblem,
                            label = AppLanguage.complaints(lang),
                            onClick = onNavigateToComplaints,
                            modifier = Modifier.weight(1f).testTag("action_complaint")
                        )
                        ActionChip(
                            icon = Icons.Default.ReceiptLong,
                            label = AppLanguage.financials(lang),
                            onClick = onNavigateToFinancials,
                            modifier = Modifier.weight(1f).testTag("action_financials")
                        )
                        ActionChip(
                            icon = Icons.Default.Badge,
                            label = AppLanguage.profile(lang),
                            onClick = onNavigateToProfile,
                            modifier = Modifier.weight(1f).testTag("action_profile")
                        )
                        ActionChip(
                            icon = Icons.Default.Code,
                            label = AppLanguage.schemaDocs(lang),
                            onClick = onNavigateToSchemaDocs,
                            modifier = Modifier.weight(1f).testTag("action_schema")
                        )
                    }
                }
            }

            // Official Notices Bulletin
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = AppLanguage.noticeBoard(lang),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (lang == Language.BN) "সবগুলি" else "View All",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onNavigateToNotices() }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(announcements) { notice ->
                            NoticeCard(notice = notice, lang = lang)
                        }
                    }
                }
            }

            // Latest Activities Feed
            item {
                Column {
                    Text(
                        text = AppLanguage.latestActivities(lang),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    activities.forEach { activity ->
                        ActivityCard(activity = activity, lang = lang)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
            } // end else (regular dashboard view)
        }
    }
}

@Composable
fun ActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun NoticeCard(notice: AnnouncementEntity, lang: Language) {
    val isUrgent = notice.priority == "High"
    Card(
        modifier = Modifier
            .width(260.dp)
            .height(140.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUrgent) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                text = if (lang == Language.BN) notice.categoryBn else notice.categoryEn,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isUrgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            labelColor = Color.White
                        )
                    )
                    Text(
                        text = notice.date,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (lang == Language.BN) notice.titleBn else notice.titleEn,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (lang == Language.BN) notice.descriptionBn else notice.descriptionEn,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ActivityCard(activity: ActivityEntity, lang: Language) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Event,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (lang == Language.BN) activity.titleBn else activity.titleEn,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${if (lang == Language.BN) activity.locationBn else activity.locationEn} • ${activity.date}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (lang == Language.BN) activity.summaryBn else activity.summaryEn,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun SearchResultNoticeItem(
    notice: AnnouncementEntity,
    lang: Language,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().testTag("search_result_notice_${notice.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Campaign,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (lang == Language.BN) notice.categoryBn else notice.categoryEn,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = notice.date,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (lang == Language.BN) notice.titleBn else notice.titleEn,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (lang == Language.BN) notice.descriptionBn else notice.descriptionEn,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SearchResultComplaintItem(
    complaint: com.example.data.model.ComplaintEntity,
    lang: Language,
    onClick: () -> Unit
) {
    val statusColor = when (complaint.status) {
        "Resolved" -> Color(0xFF2E7D32)
        "Under Review" -> Color(0xFFE65100)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().testTag("search_result_complaint_${complaint.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = statusColor.copy(alpha = 0.15f),
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.ReportProblem,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (lang == Language.BN) complaint.categoryBn else complaint.categoryEn,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        color = statusColor,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = when (complaint.status) {
                                "Resolved" -> AppLanguage.resolved(lang)
                                "Under Review" -> AppLanguage.underReview(lang)
                                else -> AppLanguage.pending(lang)
                            },
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (lang == Language.BN) complaint.titleBn else complaint.titleEn,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (lang == Language.BN) complaint.descriptionBn else complaint.descriptionEn,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SearchResultFinancialItem(
    record: com.example.data.model.FinancialRecordEntity,
    lang: Language,
    formatMoney: (Double) -> String,
    onClick: () -> Unit
) {
    val isPaid = record.type == "Paid" || record.status == "Completed"
    val cardColor = if (isPaid) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().testTag("search_result_financial_${record.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = cardColor.copy(alpha = 0.15f),
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (isPaid) Icons.Default.CheckCircle else Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = cardColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (lang == Language.BN) record.titleBn else record.titleEn,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatMoney(record.amount),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = cardColor
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${record.monthYear} • ${record.paymentGateway.ifEmpty { record.type }}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        color = cardColor,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = when (record.status) {
                                "Completed" -> if (lang == Language.BN) "পরিশোধিত" else "Paid"
                                "Pending" -> if (lang == Language.BN) "বকেয়া" else "Pending"
                                else -> record.status
                            },
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
