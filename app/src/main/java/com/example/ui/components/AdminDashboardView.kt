package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActivityLogEntity
import com.example.data.model.ComplaintEntity
import com.example.data.model.FinancialRecordEntity
import com.example.ui.language.Language
import com.example.ui.viewmodel.AdminDashboardMetrics
import com.example.ui.viewmodel.AdminDashboardViewModel

@Composable
fun AdminDashboardOverview(
    adminViewModel: AdminDashboardViewModel,
    lang: Language,
    onNavigateToComplaintsTab: () -> Unit = {},
    onNavigateToAuditLogsTab: () -> Unit = {}
) {
    val metrics by adminViewModel.metrics.collectAsState()
    val pendingComplaints by adminViewModel.pendingComplaints.collectAsState()
    val recentFinancials by adminViewModel.recentFinancials.collectAsState()
    val activityLogs by adminViewModel.activityLogs.collectAsState()

    var resolveComplaintId by remember { mutableStateOf<Long?>(null) }
    var resolveNote by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_dashboard_overview")
    ) {
        // --- SECTION 1: AGGREGATED METRICS HEADER CARDS ---
        Text(
            text = if (lang == Language.BN) "অ্যাক্টিভিটি লগস এগ্রিগেশন ও মেট্রিক্স" else "ActivityLogs Aggregated Metrics",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Metrics Grid / Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                MetricSummaryCard(
                    title = if (lang == Language.BN) "মোট অডিট লগস" else "Total Audit Logs",
                    value = "${metrics.totalLogsCount}",
                    subtitle = if (lang == Language.BN) "Firestore ActivityLogs" else "Firestore ActivityLogs",
                    icon = Icons.Default.ListAlt,
                    accentColor = MaterialTheme.colorScheme.primary,
                    testTag = "metric_total_logs"
                )
            }
            item {
                MetricSummaryCard(
                    title = if (lang == Language.BN) "পেন্ডিং অভিযোগ" else "Pending Complaints",
                    value = "${metrics.pendingComplaintsCount}",
                    subtitle = if (lang == Language.BN) "জরুরী সমাধান প্রয়োজন" else "Requires Resolution",
                    icon = Icons.Default.ReportProblem,
                    accentColor = if (metrics.pendingComplaintsCount > 0) Color(0xFFD32F2F) else Color(0xFF2E7D32),
                    testTag = "metric_pending_complaints"
                )
            }
            item {
                MetricSummaryCard(
                    title = if (lang == Language.BN) "মোট সংগৃহীত ফান্ড" else "Collected Revenue",
                    value = "৳ ${metrics.totalCollectedAmount.toInt()}",
                    subtitle = if (lang == Language.BN) "মোট লেনদেন: ${metrics.totalFinancialTransactionsCount}" else "${metrics.totalFinancialTransactionsCount} Transactions",
                    icon = Icons.Default.AccountBalanceWallet,
                    accentColor = Color(0xFF2E7D32),
                    testTag = "metric_total_revenue"
                )
            }
            item {
                MetricSummaryCard(
                    title = if (lang == Language.BN) "প্রকাশিত নোটিশ" else "Notices Published",
                    value = "${metrics.noticesCreatedCount}",
                    subtitle = if (lang == Language.BN) "অ্যাডমিন বুলেটিন" else "Admin Bulletins",
                    icon = Icons.Default.Campaign,
                    accentColor = Color(0xFF0288D1),
                    testTag = "metric_notices_published"
                )
            }
            item {
                MetricSummaryCard(
                    title = if (lang == Language.BN) "অভিযোগ আপডেট" else "Complaint Updates",
                    value = "${metrics.complaintUpdatesCount}",
                    subtitle = if (lang == Language.BN) "সমাধান অগ্রগতি" else "Processed Items",
                    icon = Icons.Default.Build,
                    accentColor = Color(0xFFE65100),
                    testTag = "metric_complaint_updates"
                )
            }
            item {
                MetricSummaryCard(
                    title = if (lang == Language.BN) "আর্থিক সমন্বয়" else "Financial Adjustments",
                    value = "${metrics.financialAdjustmentsCount}",
                    subtitle = if (lang == Language.BN) "ডিসকাউন্ট ও ফি এন্ট্রি" else "Ledger Entries",
                    icon = Icons.Default.Payments,
                    accentColor = Color(0xFF6A1B9A),
                    testTag = "metric_financial_adjustments"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- SECTION 2: PENDING COMPLAINTS QUICK RESOLUTION CENTER ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ReportProblem,
                            contentDescription = null,
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (lang == Language.BN) "পেন্ডিং ও বিচারাধীন অভিযোগসমূহ" else "Pending & Under Review Complaints",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Badge(containerColor = Color(0xFFD32F2F), contentColor = Color.White) {
                        Text("${pendingComplaints.size}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (pendingComplaints.isEmpty()) {
                    Surface(
                        color = Color(0xFF2E7D32).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (lang == Language.BN) "সব অভিযোগ সমাধান করা হয়েছে! কোন পেন্ডিং অভিযোগ নেই।" else "All complaints resolved! No pending issues at present.",
                                fontSize = 12.sp,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        pendingComplaints.take(3).forEach { complaint ->
                            PendingComplaintDashboardCard(
                                complaint = complaint,
                                lang = lang,
                                onQuickResolve = { id ->
                                    resolveComplaintId = id
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- SECTION 3: RECENT FINANCIAL TRANSACTIONS & ADJUSTMENTS ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (lang == Language.BN) "সাম্প্রতিক আর্থিক লেনদেন ও সমন্বয়" else "Recent Financials & Adjustments",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = if (lang == Language.BN) "মোট ${recentFinancials.size} টি" else "${recentFinancials.size} Records",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (recentFinancials.isEmpty()) {
                    Text(
                        text = if (lang == Language.BN) "কোন নতুন লেনদেন রেকর্ড নেই" else "No recent transactions recorded",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        recentFinancials.forEach { fin ->
                            RecentFinancialDashboardItem(record = fin, lang = lang)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- SECTION 4: LIVE FIRESTORE ACTIVITYLOGS STREAM ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.HistoryToggleOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (lang == Language.BN) "ফায়ারস্টোর ActivityLogs লাইভ স্ট্রিম" else "Live Firestore ActivityLogs Audit Stream",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    TextButton(onClick = onNavigateToAuditLogsTab) {
                        Text(
                            text = if (lang == Language.BN) "সব দেখুন (${activityLogs.size})" else "View All (${activityLogs.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (activityLogs.isEmpty()) {
                    Text(
                        text = if (lang == Language.BN) "কোন অডিট লগ রেকর্ড পাওয়া যায়নি" else "No activity logs available in Firestore",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        activityLogs.take(4).forEach { log ->
                            ActivityLogMiniItem(log = log, lang = lang)
                        }
                    }
                }
            }
        }
    }

    // Resolve Complaint Dialog
    if (resolveComplaintId != null) {
        AlertDialog(
            onDismissRequest = { resolveComplaintId = null },
            title = { Text(if (lang == Language.BN) "অভিযোগ দ্রুত সমাধান" else "Quick Resolve Complaint") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (lang == Language.BN) "অ্যাডমিন মন্তব্য / নোট:" else "Admin Resolution Note:")
                    OutlinedTextField(
                        value = resolveNote,
                        onValueChange = { resolveNote = it },
                        label = { Text("Resolution Remark") },
                        modifier = Modifier.fillMaxWidth().testTag("quick_resolve_note_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.updateComplaintStatus(
                            id = resolveComplaintId!!,
                            status = "Resolved",
                            adminNoteEn = resolveNote.ifBlank { "Resolved by Admin Committee" },
                            adminNoteBn = resolveNote.ifBlank { "অ্যাডমিন কমিটি কর্তৃক সমাধান করা হয়েছে" }
                        )
                        resolveComplaintId = null
                        resolveNote = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    modifier = Modifier.testTag("confirm_quick_resolve_btn")
                ) {
                    Text(if (lang == Language.BN) "সমাধান সম্পন্ন করুন" else "Mark Resolved & Log")
                }
            },
            dismissButton = {
                TextButton(onClick = { resolveComplaintId = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MetricSummaryCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    testTag: String
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = accentColor.copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                    }
                }

                Surface(
                    color = accentColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Box(modifier = Modifier.size(8.dp).background(accentColor, CircleShape))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                fontSize = 9.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PendingComplaintDashboardCard(
    complaint: ComplaintEntity,
    lang: Language,
    onQuickResolve: (Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (lang == Language.BN) complaint.titleBn else complaint.titleEn,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${complaint.userNameEn} • ${complaint.holdingNo} • ${complaint.createdAt}",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { onQuickResolve(complaint.id) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                modifier = Modifier
                    .height(32.dp)
                    .testTag("quick_resolve_btn_${complaint.id}"),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (lang == Language.BN) "সমাধান" else "Resolve", fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun RecentFinancialDashboardItem(record: FinancialRecordEntity, lang: Language) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = if (record.status == "Completed" || record.status == "Paid") Color(0xFF2E7D32).copy(alpha = 0.15f) else Color(0xFFD32F2F).copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (record.status == "Completed" || record.status == "Paid") Icons.Default.CheckCircle else Icons.Default.Pending,
                            contentDescription = null,
                            tint = if (record.status == "Completed" || record.status == "Paid") Color(0xFF2E7D32) else Color(0xFFD32F2F),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = if (lang == Language.BN) record.titleBn else record.titleEn,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${record.type} • User: ${record.userId} • ${record.date}",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }

            Text(
                text = "৳ ${record.amount.toInt()}",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ActivityLogMiniItem(log: ActivityLogEntity, lang: Language) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val icon = when (log.actionType) {
                    "NOTICE_CREATION" -> Icons.Default.Campaign
                    "COMPLAINT_UPDATE" -> Icons.Default.Build
                    "FINANCIAL_ADJUSTMENT" -> Icons.Default.AccountBalanceWallet
                    else -> Icons.Default.VerifiedUser
                }
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = if (lang == Language.BN && log.titleBn.isNotBlank()) log.titleBn else log.titleEn,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "By ${log.adminName} • ${log.timestamp}",
                        fontSize = 9.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}
