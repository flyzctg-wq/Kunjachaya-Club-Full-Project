package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ComplaintEntity
import com.example.ui.components.ComplaintForm
import com.example.ui.components.ComplaintProgressStepper
import com.example.ui.components.ComplaintStatusBadge
import com.example.ui.components.EvidenceImageThumbnail
import com.example.ui.components.ComplaintImageGalleryDialog
import com.example.ui.language.AppLanguage
import com.example.ui.language.Language
import com.example.ui.viewmodel.ClubViewModel
import com.example.ui.viewmodel.ComplaintsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplaintsScreen(
    viewModel: ClubViewModel,
    complaintsViewModel: ComplaintsViewModel = viewModel()
) {
    val lang by viewModel.language.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    
    LaunchedEffect(currentUser) {
        currentUser?.let {
            complaintsViewModel.setCurrentUserId(it.id)
        }
    }

    val complaints by complaintsViewModel.userComplaints.collectAsState()

    var viewScope by remember { mutableStateOf("My Complaints") } // "My Complaints" or "Community Feed"
    var selectedTab by remember { mutableStateOf("All") }
    var showNewComplaintDialog by remember { mutableStateOf(false) }
    var selectedGalleryComplaint by remember { mutableStateOf<ComplaintEntity?>(null) }

    val scopeList = if (viewScope == "My Complaints") {
        val currentUid = currentUser?.id ?: ""
        val currentHolding = currentUser?.holding ?: ""
        complaints.filter { 
            (currentUid.isNotEmpty() && it.userId == currentUid) ||
            (currentHolding.isNotEmpty() && it.holdingNo == currentHolding) ||
            it.userNameEn == (currentUser?.nameEn ?: "")
        }.ifEmpty { 
            // Fallback to all if user created demo complaints or no specific UID match
            complaints 
        }
    } else {
        complaints
    }

    val filteredList = when (selectedTab) {
        "Pending" -> scopeList.filter { it.status == "Pending" }
        "Under Review" -> scopeList.filter { it.status == "Under Review" }
        "Resolved" -> scopeList.filter { it.status == "Resolved" }
        else -> scopeList
    }

    val pendingCount = scopeList.count { it.status == "Pending" }
    val reviewCount = scopeList.count { it.status == "Under Review" }
    val resolvedCount = scopeList.count { it.status == "Resolved" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewScope == "My Complaints") (if (lang == Language.BN) "আমার অভিযোগ ও স্ট্যাটাস" else "My Complaints & Status") else AppLanguage.complaints(lang), fontWeight = FontWeight.Bold) },
                actions = {
                    Button(
                        onClick = { showNewComplaintDialog = true },
                        modifier = Modifier.padding(end = 12.dp).testTag("new_complaint_top_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (lang == Language.BN) "নতুন অভিযোগ" else "New Complaint", fontSize = 12.sp)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showNewComplaintDialog = true },
                icon = { Icon(Icons.Default.PostAdd, contentDescription = "Add Complaint") },
                text = { Text(if (lang == Language.BN) "অভিযোগ জমা দিন" else "Submit Complaint") },
                modifier = Modifier.testTag("fab_add_complaint")
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Scope View Switcher (My Complaints vs Community Feed)
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).testTag("complaint_scope_row")
            ) {
                SegmentedButton(
                    selected = viewScope == "My Complaints",
                    onClick = { viewScope = "My Complaints" },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Icon(Icons.Default.AssignmentInd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (lang == Language.BN) "আমার অভিযোগসমূহ" else "My Complaints", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                SegmentedButton(
                    selected = viewScope == "Community Feed",
                    onClick = { viewScope = "Community Feed" },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Icon(Icons.Default.Forum, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (lang == Language.BN) "সকল অভিযোগ (কমিউনিটি)" else "Community Feed", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Summary Header Card for My Complaints
            if (viewScope == "My Complaints") {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).testTag("my_complaints_summary_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (lang == Language.BN) "আমার টিকিট ট্র্যাকার" else "My Ticket Resolution Tracker",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (lang == Language.BN) "অ্যাপার্টমেন্ট: ${currentUser?.holding ?: "Apt 4B"}" else "Apartment: ${currentUser?.holding ?: "Apt 4B"}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = { showNewComplaintDialog = true },
                                modifier = Modifier.testTag("summary_lodge_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.PostAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (lang == Language.BN) "অভিযোগ লিখুন" else "Lodge Ticket", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            MetricBadge(
                                count = scopeList.size,
                                label = if (lang == Language.BN) "মোট" else "Total",
                                color = MaterialTheme.colorScheme.primary
                            )
                            MetricBadge(
                                count = pendingCount,
                                label = if (lang == Language.BN) "অপেক্ষমান" else "Pending",
                                color = Color(0xFFD32F2F)
                            )
                            MetricBadge(
                                count = reviewCount,
                                label = if (lang == Language.BN) "পর্যালোচনায়" else "In Review",
                                color = Color(0xFFF57C00)
                            )
                            MetricBadge(
                                count = resolvedCount,
                                label = if (lang == Language.BN) "সমাধানকৃত" else "Resolved",
                                color = Color(0xFF388E3C)
                            )
                        }
                    }
                }
            }

            // Filter Tabs
            ScrollableTabRow(
                selectedTabIndex = when (selectedTab) {
                    "Pending" -> 1
                    "Under Review" -> 2
                    "Resolved" -> 3
                    else -> 0
                },
                edgePadding = 0.dp,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                val tabs = listOf("All", "Pending", "Under Review", "Resolved")
                tabs.forEachIndexed { index, tab ->
                    val tabText = when (tab) {
                        "All" -> if (lang == Language.BN) "সকল (${scopeList.size})" else "All (${scopeList.size})"
                        "Pending" -> "${AppLanguage.pending(lang)} ($pendingCount)"
                        "Under Review" -> "${AppLanguage.underReview(lang)} ($reviewCount)"
                        "Resolved" -> "${AppLanguage.resolved(lang)} ($resolvedCount)"
                        else -> tab
                    }
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(tabText, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        modifier = Modifier.testTag("complaint_tab_$index")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (lang == Language.BN) "কোন অভিযোগ পাওয়া যায়নি" else "No Complaints Found",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredList) { complaint ->
                        ComplaintCard(
                            complaint = complaint,
                            lang = lang,
                            canManage = currentUser?.hasComplaintPermission() == true,
                            onOpenGallery = { selectedGalleryComplaint = it },
                            onUpdateStatus = { comp, status ->
                                val noteEn = if (status == "Resolved") "Action taken & verified by Committee." else "Assigned to Maintenance Lead."
                                val noteBn = if (status == "Resolved") "কমিটি কর্তৃক পদক্ষেপ নেওয়া হয়েছে এবং যাচাই করা হয়েছে।" else "রক্ষণাবেক্ষণ প্রধানকে দায়িত্ব প্রদান করা হয়েছে।"
                                complaintsViewModel.updateComplaintStatus(comp, status, noteEn, noteBn)
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    // Full-size Evidence Image Gallery Modal Dialog
    if (selectedGalleryComplaint != null) {
        ComplaintImageGalleryDialog(
            complaint = selectedGalleryComplaint!!,
            lang = lang,
            onDismiss = { selectedGalleryComplaint = null }
        )
    }

    // New Complaint Form Dialog with Camera capture & Firestore push
    if (showNewComplaintDialog) {
        Dialog(onDismissRequest = { showNewComplaintDialog = false }) {
            ComplaintForm(
                complaintsViewModel = complaintsViewModel,
                currentUser = currentUser,
                lang = lang,
                onDismiss = { showNewComplaintDialog = false },
                onSubmittedSuccessfully = { showNewComplaintDialog = false }
            )
        }
    }
}

@Composable
fun ComplaintCard(
    complaint: ComplaintEntity,
    lang: Language,
    canManage: Boolean = false,
    onOpenGallery: (ComplaintEntity) -> Unit = {},
    onUpdateStatus: (ComplaintEntity, String) -> Unit = { _, _ -> }
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("complaint_item_${complaint.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = {},
                    label = { Text(if (lang == Language.BN) complaint.categoryBn else complaint.categoryEn, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                )
                
                // Visual Status Indicator Badge with Icon and State Color
                ComplaintStatusBadge(
                    status = complaint.status,
                    lang = lang,
                    compact = true
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (lang == Language.BN) complaint.titleBn else complaint.titleEn,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = if (lang == Language.BN) complaint.descriptionBn else complaint.descriptionEn,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Interactive Progress Stepper Indicator
            ComplaintProgressStepper(
                status = complaint.status,
                lang = lang,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            if (complaint.imageUrl.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                EvidenceImageThumbnail(
                    imageUrl = complaint.imageUrl,
                    complaintTitle = if (lang == Language.BN) complaint.titleBn else complaint.titleEn,
                    lang = lang,
                    onClick = { onOpenGallery(complaint) },
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }

            if (complaint.adminNoteEn.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = if (lang == Language.BN) "অ্যাডমিন সমাধান নোট:" else "Admin Resolution Note:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (lang == Language.BN) complaint.adminNoteBn else complaint.adminNoteEn,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (canManage) {
                Spacer(modifier = Modifier.height(10.dp))

                // Action row for changing status and triggering FCM Push Notification
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (lang == Language.BN) "স্ট্যাটাস পরিবর্তন করুন (FCM পুশ প্রসংগ):" else "Update Status (FCM Push):",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (complaint.status != "Pending") {
                            FilterChip(
                                selected = false,
                                onClick = { onUpdateStatus(complaint, "Pending") },
                                label = { Text(if (lang == Language.BN) "অপেক্ষমান" else "Pending", fontSize = 10.sp) },
                                modifier = Modifier.testTag("status_pending_btn_${complaint.id}")
                            )
                        }
                        if (complaint.status != "Under Review") {
                            FilterChip(
                                selected = false,
                                onClick = { onUpdateStatus(complaint, "Under Review") },
                                label = { Text(if (lang == Language.BN) "পর্যালোচনায়" else "Under Review", fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(containerColor = Color(0xFFFFF3E0)),
                                modifier = Modifier.testTag("status_review_btn_${complaint.id}")
                            )
                        }
                        if (complaint.status != "Resolved") {
                            FilterChip(
                                selected = false,
                                onClick = { onUpdateStatus(complaint, "Resolved") },
                                label = { Text(if (lang == Language.BN) "সমাধানকৃত" else "Resolved", fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(containerColor = Color(0xFFE8F5E9)),
                                modifier = Modifier.testTag("status_resolved_btn_${complaint.id}")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${complaint.userNameEn} (${complaint.holdingNo})",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = complaint.createdAt,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MetricBadge(
    count: Int,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(
            text = "$count",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

