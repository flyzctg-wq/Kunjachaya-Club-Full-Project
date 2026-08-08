package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ActivityLogEntity
import com.example.data.model.AnnouncementEntity
import com.example.data.model.ComplaintEntity
import com.example.data.model.UserEntity
import com.example.ui.components.AdminDashboardOverview
import com.example.ui.components.RoleAndModerationPrivilegeDialog
import com.example.ui.components.VisualSpendingDashboard
import com.example.ui.language.AppLanguage
import com.example.ui.language.Language
import com.example.ui.viewmodel.AdminDashboardViewModel
import com.example.ui.viewmodel.ClubViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPortalScreen(
    viewModel: ClubViewModel,
    adminDashboardViewModel: AdminDashboardViewModel = viewModel()
) {
    val lang by viewModel.language.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val allComplaints by viewModel.allComplaints.collectAsState()
    val allAnnouncements by viewModel.allAnnouncements.collectAsState()
    val allActivityLogs by viewModel.allActivityLogs.collectAsState()
    val allFinancials by viewModel.allFinancials.collectAsState()

    // Restricted Access Check: only sitting Executive Committee members
    // (any of the 15 real posts per ধারা-১৪) may view this dashboard.
    val isAdmin = currentUser?.isExecutiveCommittee() == true

    if (!isAdmin) {
        RestrictedAccessGuardScreen(
            currentUser = currentUser,
            lang = lang
        )
        return
    }

    var selectedTab by remember { mutableStateOf(0) }
    var showPublishNoticeDialog by remember { mutableStateOf(false) }
    var showFinancialAdjustmentDialog by remember { mutableStateOf(false) }

    val pendingUsers = allUsers.filter { it.membershipStatus == "Pending" }
    val pendingComplaints = allComplaints.filter { it.status == "Pending" || it.status == "Under Review" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(AppLanguage.admin(lang), fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = if (lang == Language.BN) "নিয়ন্ত্রিত অ্যাডমিন কন্ট্রোল প্যানেল" else "Restricted Admin Control Dashboard",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showFinancialAdjustmentDialog = true },
                        modifier = Modifier.testTag("admin_financial_adj_btn")
                    ) {
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = "Record Financial Adjustment",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Button(
                        onClick = { showPublishNoticeDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("publish_notice_btn")
                    ) {
                        Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (lang == Language.BN) "বিজ্ঞপ্তি প্রকাশ" else "Publish Notice", fontSize = 12.sp)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Main Admin Navigation Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 0.dp,
                modifier = Modifier.testTag("admin_tabs_row")
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            if (lang == Language.BN) "ড্যাশবোর্ড ওভারভিউ" else "Dashboard Overview",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    modifier = Modifier.testTag("tab_overview")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        BadgedBox(
                            badge = {
                                if (pendingUsers.isNotEmpty()) {
                                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                                        Text("${pendingUsers.size}")
                                    }
                                }
                            }
                        ) {
                            Text(
                                if (lang == Language.BN) "সদস্য ব্যবস্থাপনা" else "Manage Members",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    },
                    modifier = Modifier.testTag("tab_members")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        BadgedBox(
                            badge = {
                                if (pendingComplaints.isNotEmpty()) {
                                    Badge(containerColor = Color(0xFFE65100)) {
                                        Text("${pendingComplaints.size}")
                                    }
                                }
                            }
                        ) {
                            Text(
                                if (lang == Language.BN) "অভিযোগ সমাধান" else "Process Complaints",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    },
                    modifier = Modifier.testTag("tab_complaints")
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = {
                        Text(
                            if (lang == Language.BN) "বিজ্ঞপ্তি প্রকাশ" else "Publish Notices",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    modifier = Modifier.testTag("tab_notices")
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    text = {
                        Text(
                            if (lang == Language.BN) "অডিট লগ ও বাজেট" else "Audit & Analytics",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    modifier = Modifier.testTag("tab_analytics")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedTab) {
                0 -> {
                    // TAB 0: DASHBOARD OVERVIEW (Metrics, KPIs & Quick Audit Logs)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            AdminDashboardOverview(
                                adminViewModel = adminDashboardViewModel,
                                lang = lang,
                                onNavigateToComplaintsTab = { selectedTab = 2 },
                                onNavigateToAuditLogsTab = { selectedTab = 4 }
                            )
                        }
                    }
                }
                1 -> {
                    // TAB 1: MANAGE MEMBERS
                    MemberManagementSection(
                        allUsers = allUsers,
                        viewModel = viewModel,
                        lang = lang
                    )
                }
                2 -> {
                    // TAB 2: PROCESS COMPLAINTS
                    ComplaintProcessingSection(
                        allComplaints = allComplaints,
                        viewModel = viewModel,
                        lang = lang
                    )
                }
                3 -> {
                    // TAB 3: PUBLISH NOTICES
                    NoticePublishingSection(
                        announcements = allAnnouncements,
                        viewModel = viewModel,
                        lang = lang
                    )
                }
                4 -> {
                    // TAB 4: AUDIT LOGS & SPENDING ANALYTICS
                    AuditAndAnalyticsSection(
                        allActivityLogs = allActivityLogs,
                        allFinancials = allFinancials,
                        lang = lang
                    )
                }
            }
        }
    }

    // Modal: Publish Notice Quick Action
    if (showPublishNoticeDialog) {
        PublishNoticeModal(
            lang = lang,
            onDismiss = { showPublishNoticeDialog = false },
            onPublish = { title, desc, cat, priority ->
                viewModel.publishNotice(
                    titleEn = title,
                    titleBn = title,
                    descEn = desc,
                    descBn = desc,
                    categoryEn = cat,
                    categoryBn = cat,
                    priority = priority
                )
                showPublishNoticeDialog = false
            }
        )
    }

    // Modal: Record Financial Adjustment
    if (showFinancialAdjustmentDialog) {
        FinancialAdjustmentModal(
            lang = lang,
            onDismiss = { showFinancialAdjustmentDialog = false },
            onSave = { targetUser, title, amount, type, note ->
                viewModel.recordFinancialAdjustment(
                    targetUserId = targetUser,
                    titleEn = title,
                    titleBn = title,
                    amount = amount,
                    adjustmentType = type,
                    noteEn = note,
                    noteBn = note
                )
                showFinancialAdjustmentDialog = false
            }
        )
    }
}

// ==========================================
// RESTRICTED ACCESS SECURITY GUARD SCREEN
// ==========================================

@Composable
fun RestrictedAccessGuardScreen(
    currentUser: UserEntity?,
    lang: Language
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("restricted_access_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = "Restricted Access",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Text(
                    text = if (lang == Language.BN) "অ্যাক্সেস সংরক্ষিত: অ্যাডমিন অনুমতি প্রয়োজন" else "Restricted Access: Administrator Privileges Required",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )

                Text(
                    text = if (lang == Language.BN)
                        "এই প্যানেলটি শুধুমাত্র কুঞ্জছায়া ক্লাবের কার্যনির্বাহী কমিটি এবং মনোনীত অ্যাডমিনিস্ট্রেটরদের জন্য নির্ধারিত। আপনি সাধারণ সদস্য বা অতিথি হিসেবে লগইন করে থাকলে এটি দেখতে পারবেন না।"
                    else
                        "This portal is strictly reserved for Authorized Executive Committee Members and System Administrators of Kunjachaya Club to manage sensitive records.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (lang == Language.BN) "বর্তমান অ্যাকাউন্ট:" else "Current Account Status:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = currentUser?.let { "${it.nameEn} (${it.phone})" } ?: (if (lang == Language.BN) "অননুমোদিত / অতিথি" else "Unauthenticated / Guest"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Committee Post: ${AppLanguage.committeePostLabel(lang, currentUser?.committeePost).ifBlank { if (lang == Language.BN) "কোনো পদ নেই" else "None" }}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                Text(
                    text = if (lang == Language.BN)
                        "কার্যনির্বাহী কমিটির পদ পাওয়ার জন্য সাধারণ সভায় নির্বাচিত বা কো-অপ্ট হতে হবে। এই স্ক্রিন থেকে সরাসরি অ্যাডমিন অ্যাক্সেস দেওয়া সম্ভব নয়।"
                    else
                        "Executive Committee access is granted only by election or co-option (ধারা-১৫), recorded against your real account by another committee member — it can't be switched on from this screen.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ==========================================
// 1. MEMBER MANAGEMENT SECTION
// ==========================================

@Composable
fun MemberManagementSection(
    allUsers: List<UserEntity>,
    viewModel: ClubViewModel,
    lang: Language
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Pending") }
    var selectedUserForDetails by remember { mutableStateOf<UserEntity?>(null) }
    var userForRoleManagement by remember { mutableStateOf<UserEntity?>(null) }

    val filteredUsers = allUsers.filter { user ->
        val matchesFilter = when (selectedFilter) {
            "Pending" -> user.membershipStatus == "Pending"
            "Active" -> user.membershipStatus == "Active"
            "Rejected" -> user.membershipStatus == "Rejected"
            else -> true
        }
        val matchesSearch = searchQuery.isBlank() ||
            user.nameEn.contains(searchQuery, ignoreCase = true) ||
            user.nameBn.contains(searchQuery, ignoreCase = true) ||
            user.phone.contains(searchQuery, ignoreCase = true) ||
            user.holding.contains(searchQuery, ignoreCase = true) ||
            user.road.contains(searchQuery, ignoreCase = true)

        matchesFilter && matchesSearch
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("member_search_input"),
            placeholder = { Text(if (lang == Language.BN) "সদস্য নাম, ফোন নম্বর বা হোল্ডিং খুঁজুন..." else "Search member name, phone, or holding...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        // Filter Chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val pendingCount = allUsers.count { it.membershipStatus == "Pending" }
            val activeCount = allUsers.count { it.membershipStatus == "Active" }

            FilterChip(
                selected = selectedFilter == "Pending",
                onClick = { selectedFilter = "Pending" },
                label = { Text("${if (lang == Language.BN) "পেন্ডিং আবেদন" else "Pending"} ($pendingCount)", fontSize = 11.sp) },
                modifier = Modifier.testTag("filter_pending_members")
            )
            FilterChip(
                selected = selectedFilter == "Active",
                onClick = { selectedFilter = "Active" },
                label = { Text("${if (lang == Language.BN) "সক্রিয় সদস্য" else "Active"} ($activeCount)", fontSize = 11.sp) },
                modifier = Modifier.testTag("filter_active_members")
            )
            FilterChip(
                selected = selectedFilter == "All",
                onClick = { selectedFilter = "All" },
                label = { Text(if (lang == Language.BN) "সকল" else "All (${allUsers.size})", fontSize = 11.sp) },
                modifier = Modifier.testTag("filter_all_members")
            )
        }

        if (filteredUsers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (lang == Language.BN) "কোন সদস্য রেকর্ড পাওয়া যায়নি" else "No matching member applications or records found.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filteredUsers) { user ->
                    MemberAdminCard(
                        user = user,
                        lang = lang,
                        onApprove = { viewModel.approveUserMembership(user.id) },
                        onReject = { viewModel.rejectUserMembership(user.id) },
                        onViewDetails = { selectedUserForDetails = user },
                        onManageRole = { userForRoleManagement = user }
                    )
                }
            }
        }
    }

    // Role & Moderation Privileges Dialog
    userForRoleManagement?.let { targetUser ->
        RoleAndModerationPrivilegeDialog(
            user = targetUser,
            lang = lang,
            viewModel = viewModel,
            onDismiss = { userForRoleManagement = null }
        )
    }

    // Modal: Member Dossier / Detailed Info
    selectedUserForDetails?.let { user ->
        AlertDialog(
            onDismissRequest = { selectedUserForDetails = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (lang == Language.BN) "সদস্য পূর্ণাঙ্গ তথ্য" else "Resident Member Profile")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Name: ${user.nameEn} / ${user.nameBn}", fontWeight = FontWeight.Bold)
                    Text(text = "Phone/User ID: ${user.phone} (${user.id})")
                    Text(text = "Residence: Holding ${user.holding}, ${user.road}, ${user.block}")
                    Text(text = "Profession: ${user.professionEn}")
                    Text(text = "Guardian: ${user.fatherOrSpouseNameEn}")
                    Text(text = "Emergency Contact: ${user.emergencyContact}")
                    Text(text = "Blood Group: ${user.bloodGroup} • Family Size: ${user.familyMembersCount}")
                    Text(text = "Membership Status: ${user.membershipStatus} (${AppLanguage.committeePostLabel(lang, user.committeePost).ifBlank { AppLanguage.roleGeneral(lang) }})")
                    Text(text = "NID Verification: ${user.nidFrontUrl}")
                }
            },
            confirmButton = {
                Button(onClick = { selectedUserForDetails = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun MemberAdminCard(
    user: UserEntity,
    lang: Language,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onViewDetails: () -> Unit,
    onManageRole: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("member_card_${user.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = user.nameEn.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (lang == Language.BN) user.nameBn else user.nameEn,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "${user.holding}, ${user.road} • Phone: ${user.phone}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    AssistChip(
                        onClick = {},
                        label = { Text(AppLanguage.committeePostLabel(lang, user.committeePost).ifBlank { AppLanguage.roleGeneral(lang) }, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = when {
                                user.isPresidentOrGeneralSecretary() -> Color(0xFFFFEBEE)
                                user.isExecutiveCommittee() -> MaterialTheme.colorScheme.primaryContainer
                                user.isFoundingMember() -> Color(0xFFFFF3E0)
                                user.isPendingApproval() -> Color(0xFFE0F2F1)
                                else -> Color(0xFFE8F5E9)
                            },
                            labelColor = when {
                                user.isPresidentOrGeneralSecretary() -> Color(0xFFC62828)
                                user.isExecutiveCommittee() -> MaterialTheme.colorScheme.primary
                                user.isFoundingMember() -> Color(0xFFE65100)
                                user.isPendingApproval() -> Color(0xFF00695C)
                                else -> Color(0xFF2E7D32)
                            }
                        )
                    )

                    AssistChip(
                        onClick = {},
                        label = { Text(user.membershipStatus, fontSize = 10.sp) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = when (user.membershipStatus) {
                                "Active" -> Color(0xFFE8F5E9)
                                "Pending" -> Color(0xFFFFF3E0)
                                else -> Color(0xFFFFEBEE)
                            },
                            labelColor = when (user.membershipStatus) {
                                "Active" -> Color(0xFF2E7D32)
                                "Pending" -> Color(0xFFE65100)
                                else -> Color(0xFFC62828)
                            }
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Profession: ${user.professionEn} • NID Ref: ${user.nidFrontUrl}",
                fontSize = 11.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (user.membershipStatus == "Pending") {
                    Button(
                        onClick = onApprove,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("approve_btn_${user.id}")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (lang == Language.BN) "অনুমোদন করুন" else "Approve", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onReject,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reject_btn_${user.id}")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (lang == Language.BN) "প্রত্যাখ্যান" else "Reject", fontSize = 12.sp)
                    }
                }

                if (onManageRole != null) {
                    IconButton(
                        onClick = onManageRole,
                        modifier = Modifier.testTag("manage_role_btn_${user.id}")
                    ) {
                        Icon(
                            Icons.Default.AdminPanelSettings,
                            contentDescription = "Manage Role",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                OutlinedButton(
                    onClick = onViewDetails,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("view_dossier_btn_${user.id}")
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (lang == Language.BN) "বিস্তারিত" else "Details", fontSize = 12.sp)
                }
            }
        }
    }
}

// ==========================================
// 2. COMPLAINT PROCESSING SECTION
// ==========================================

@Composable
fun ComplaintProcessingSection(
    allComplaints: List<ComplaintEntity>,
    viewModel: ClubViewModel,
    lang: Language
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Pending/Under Review") }

    val filteredComplaints = allComplaints.filter { cmp ->
        val matchesFilter = when (selectedFilter) {
            "Pending/Under Review" -> cmp.status == "Pending" || cmp.status == "Under Review"
            "Resolved" -> cmp.status == "Resolved"
            else -> true
        }
        val matchesSearch = searchQuery.isBlank() ||
            cmp.titleEn.contains(searchQuery, ignoreCase = true) ||
            cmp.titleBn.contains(searchQuery, ignoreCase = true) ||
            cmp.userNameEn.contains(searchQuery, ignoreCase = true) ||
            cmp.holdingNo.contains(searchQuery, ignoreCase = true)

        matchesFilter && matchesSearch
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Search
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("complaint_search_input"),
            placeholder = { Text(if (lang == Language.BN) "অভিযোগের শিরোনাম বা রেসিডেন্ট দিয়ে খুঁজুন..." else "Search complaint title or resident...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        // Filter chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val unresolvedCount = allComplaints.count { it.status == "Pending" || it.status == "Under Review" }
            val resolvedCount = allComplaints.count { it.status == "Resolved" }

            FilterChip(
                selected = selectedFilter == "Pending/Under Review",
                onClick = { selectedFilter = "Pending/Under Review" },
                label = { Text("${if (lang == Language.BN) "প্রক্রিয়াধীন" else "Active/Pending"} ($unresolvedCount)", fontSize = 11.sp) },
                modifier = Modifier.testTag("filter_unresolved_complaints")
            )
            FilterChip(
                selected = selectedFilter == "Resolved",
                onClick = { selectedFilter = "Resolved" },
                label = { Text("${if (lang == Language.BN) "মীমাংসিত" else "Resolved"} ($resolvedCount)", fontSize = 11.sp) },
                modifier = Modifier.testTag("filter_resolved_complaints")
            )
            FilterChip(
                selected = selectedFilter == "All",
                onClick = { selectedFilter = "All" },
                label = { Text(if (lang == Language.BN) "সকল" else "All (${allComplaints.size})", fontSize = 11.sp) },
                modifier = Modifier.testTag("filter_all_complaints")
            )
        }

        if (filteredComplaints.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (lang == Language.BN) "কোন অভিযোগ পাওয়া যায়নি" else "No complaints match the selected filter.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filteredComplaints) { cmp ->
                    AdminComplaintItem(complaint = cmp, viewModel = viewModel, lang = lang)
                }
            }
        }
    }
}

// ==========================================
// 3. NOTICE PUBLISHING SECTION
// ==========================================

@Composable
fun NoticePublishingSection(
    announcements: List<AnnouncementEntity>,
    viewModel: ClubViewModel,
    lang: Language
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Urgent Notice") }
    var priority by remember { mutableStateOf("High") }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Form Card for Publishing Notice
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("notice_publisher_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Campaign,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (lang == Language.BN) "নতুন বুলেটিন ও ঘোষণা প্রকাশ করুন" else "Publish Official Announcement",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (lang == Language.BN) "বিজ্ঞপ্তি শিরোনাম" else "Notice Title") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("inline_notice_title_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(if (lang == Language.BN) "বিজ্ঞপ্তি বিষয়বস্তু / বিবরণ" else "Notice Content / Details") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .testTag("inline_notice_desc_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Category
                    listOf("Urgent Notice", "General", "Security", "Maintenance").forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 10.sp) }
                        )
                    }
                }

                Button(
                    onClick = {
                        if (title.isNotBlank() && description.isNotBlank()) {
                            viewModel.publishNotice(
                                titleEn = title,
                                titleBn = title,
                                descEn = description,
                                descBn = description,
                                categoryEn = category,
                                categoryBn = category,
                                priority = priority
                            )
                            title = ""
                            description = ""
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("inline_submit_notice_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (lang == Language.BN) "বিজ্ঞপ্তি প্রকাশ ও পুশ নোটিফিকেশন পাঠান" else "Publish & Send Push Notification")
                }
            }
        }

        // Active Announcements Feed
        Text(
            text = if (lang == Language.BN) "প্রকাশিত সকল সাম্প্রতিক বিজ্ঞপ্তি (${announcements.size})" else "Published Announcements (${announcements.size})",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(announcements) { notice ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("announcement_admin_item_${notice.id}"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = if (notice.priority == "High") Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = notice.priority,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (notice.priority == "High") Color(0xFFC62828) else Color(0xFF2E7D32),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(notice.date, fontSize = 10.sp, color = Color.Gray)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (lang == Language.BN && notice.titleBn.isNotBlank()) notice.titleBn else notice.titleEn,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (lang == Language.BN && notice.descriptionBn.isNotBlank()) notice.descriptionBn else notice.descriptionEn,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = { viewModel.deleteAnnouncement(notice.id) },
                            modifier = Modifier.testTag("delete_notice_btn_${notice.id}")
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. AUDIT & ANALYTICS SECTION
// ==========================================

@Composable
fun AuditAndAnalyticsSection(
    allActivityLogs: List<ActivityLogEntity>,
    allFinancials: List<com.example.data.model.FinancialRecordEntity>,
    lang: Language
) {
    var selectedLogFilter by remember { mutableStateOf("All") }

    val filteredActivityLogs = when (selectedLogFilter) {
        "Notices" -> allActivityLogs.filter { it.actionType == "NOTICE_CREATION" }
        "Complaints" -> allActivityLogs.filter { it.actionType == "COMPLAINT_UPDATE" }
        "Financials" -> allActivityLogs.filter { it.actionType == "FINANCIAL_ADJUSTMENT" }
        "Members" -> allActivityLogs.filter { it.actionType == "MEMBER_APPROVAL" }
        else -> allActivityLogs
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Visual Spending Dashboard Component
            VisualSpendingDashboard(
                financials = allFinancials,
                lang = lang,
                formatMoney = { amount -> "৳ ${amount.toInt()}" }
            )
        }

        item {
            Text(
                text = if (lang == Language.BN) "অ্যাডমিন অডিট ট্রেইল ও অ্যাক্টিভিটি লগস" else "Administrator Audit Trail & Logs",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(
                    "All" to if (lang == Language.BN) "সকল" else "All",
                    "Notices" to if (lang == Language.BN) "বিজ্ঞপ্তি" else "Notices",
                    "Complaints" to if (lang == Language.BN) "অভিযোগ" else "Complaints",
                    "Financials" to if (lang == Language.BN) "আর্থিক" else "Financials",
                    "Members" to if (lang == Language.BN) "সদস্য পদ" else "Members"
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = selectedLogFilter == key,
                        onClick = { selectedLogFilter = key },
                        label = { Text(label, fontSize = 11.sp) }
                    )
                }
            }
        }

        if (filteredActivityLogs.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(if (lang == Language.BN) "কোন অডিট লগ রেকর্ড পাওয়া যায়নি" else "No audit logs recorded yet.", fontWeight = FontWeight.Medium)
                }
            }
        } else {
            items(filteredActivityLogs) { log ->
                ActivityLogCard(log = log, lang = lang)
            }
        }
    }
}

// ==========================================
// MODALS & DIALOGS
// ==========================================

@Composable
fun PublishNoticeModal(
    lang: Language,
    onDismiss: () -> Unit,
    onPublish: (title: String, desc: String, category: String, priority: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Urgent Notice") }
    var priority by remember { mutableStateOf("High") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (lang == Language.BN) "নতুন বুলেটিন প্রকাশ করুন" else "Publish Official Announcement") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth().testTag("modal_notice_title_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth().height(100.dp).testTag("modal_notice_desc_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && description.isNotBlank()) {
                        onPublish(title, description, category, priority)
                    }
                },
                modifier = Modifier.testTag("submit_notice_btn")
            ) {
                Text("Publish")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun FinancialAdjustmentModal(
    lang: Language,
    onDismiss: () -> Unit,
    onSave: (targetUser: String, title: String, amount: Double, type: String, note: String) -> Unit
) {
    var targetUser by remember { mutableStateOf("USR-101") }
    var title by remember { mutableStateOf("Quarterly Security Fee Waiver") }
    var amountStr by remember { mutableStateOf("500") }
    var adjustmentType by remember { mutableStateOf("Fee Waiver") }
    var note by remember { mutableStateOf("Approved discount for active community service.") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (lang == Language.BN) "আর্থিক সমন্বয় নথিবদ্ধকরণ" else "Record Financial Adjustment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = targetUser,
                    onValueChange = { targetUser = it },
                    label = { Text("Resident User ID (e.g. USR-101)") },
                    modifier = Modifier.fillMaxWidth().testTag("adj_user_input")
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Adjustment Title") },
                    modifier = Modifier.fillMaxWidth().testTag("adj_title_input")
                )
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount (৳)") },
                    modifier = Modifier.fillMaxWidth().testTag("adj_amount_input")
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Admin Note / Explanation") },
                    modifier = Modifier.fillMaxWidth().testTag("adj_note_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (targetUser.isNotBlank() && title.isNotBlank()) {
                        onSave(targetUser, title, amt, adjustmentType, note)
                    }
                },
                modifier = Modifier.testTag("submit_financial_adj_btn")
            ) {
                Text(if (lang == Language.BN) "সংরক্ষণ ও অডিট লগ তৈরী" else "Save & Log to ActivityLogs")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ActivityLogCard(log: ActivityLogEntity, lang: Language) {
    val (badgeColor, badgeText, icon) = when (log.actionType) {
        "NOTICE_CREATION" -> Triple(MaterialTheme.colorScheme.primary, if (lang == Language.BN) "বিজ্ঞপ্তি" else "Notice", Icons.Default.Campaign)
        "COMPLAINT_UPDATE" -> Triple(Color(0xFFE65100), if (lang == Language.BN) "অভিযোগ" else "Complaint", Icons.Default.Build)
        "FINANCIAL_ADJUSTMENT" -> Triple(Color(0xFF2E7D32), if (lang == Language.BN) "আর্থিক" else "Financial", Icons.Default.AccountBalanceWallet)
        "MEMBER_APPROVAL" -> Triple(Color(0xFF6A1B9A), if (lang == Language.BN) "সদস্য পদ" else "Membership", Icons.Default.VerifiedUser)
        else -> Triple(MaterialTheme.colorScheme.secondary, log.actionType, Icons.Default.Info)
    }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("activity_log_card_${log.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = badgeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(badgeText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = badgeColor)
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "Firestore: ActivityLogs",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (lang == Language.BN && log.titleBn.isNotBlank()) log.titleBn else log.titleEn,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            if (log.detailsEn.isNotBlank() || log.detailsBn.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (lang == Language.BN && log.detailsBn.isNotBlank()) log.detailsBn else log.detailsEn,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "By: ${log.adminName} (${log.adminId})",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = log.timestamp,
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun AdminComplaintItem(complaint: ComplaintEntity, viewModel: ClubViewModel, lang: Language) {
    var showResolveDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("complaint_admin_card_${complaint.id}"),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (lang == Language.BN) complaint.titleBn else complaint.titleEn, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                AssistChip(
                    onClick = {},
                    label = { Text(complaint.status, fontSize = 10.sp) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = when (complaint.status) {
                            "Resolved" -> Color(0xFFE8F5E9)
                            "Under Review" -> Color(0xFFE3F2FD)
                            else -> Color(0xFFFFF3E0)
                        },
                        labelColor = when (complaint.status) {
                            "Resolved" -> Color(0xFF2E7D32)
                            "Under Review" -> Color(0xFF1976D2)
                            else -> Color(0xFFE65100)
                        }
                    )
                )
            }

            Text("${complaint.userNameEn} (${complaint.holdingNo})", fontSize = 11.sp, color = Color.Gray)
            Text(if (lang == Language.BN) complaint.descriptionBn else complaint.descriptionEn, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { showResolveDialog = true },
                    modifier = Modifier.testTag("admin_resolve_btn_${complaint.id}")
                ) {
                    Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (lang == Language.BN) "স্ট্যাটাস আপডেট" else "Update Status", fontSize = 11.sp)
                }
            }
        }
    }

    if (showResolveDialog) {
        var note by remember { mutableStateOf("") }
        var status by remember { mutableStateOf("Resolved") }

        AlertDialog(
            onDismissRequest = { showResolveDialog = false },
            title = { Text(if (lang == Language.BN) "অভিযোগ সমাধান আপডেট" else "Update Complaint Status") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(if (lang == Language.BN) "নতুন স্ট্যাটাস নির্বাচন করুন:" else "Select Status:")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = status == "Under Review",
                            onClick = { status = "Under Review" },
                            label = { Text("Under Review") }
                        )
                        FilterChip(
                            selected = status == "Resolved",
                            onClick = { status = "Resolved" },
                            label = { Text("Resolved") }
                        )
                    }
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text(if (lang == Language.BN) "অ্যাডমিন মন্তব্য / নোট" else "Admin Resolution Note") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateComplaintStatus(complaint.id, status, note, note)
                        showResolveDialog = false
                    },
                    modifier = Modifier.testTag("save_complaint_status_btn")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResolveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
