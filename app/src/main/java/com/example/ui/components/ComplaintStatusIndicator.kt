package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.language.Language

enum class ComplaintState(
    val key: String,
    val labelEn: String,
    val labelBn: String,
    val color: Color,
    val containerColor: Color,
    val icon: ImageVector,
    val stepOrder: Int
) {
    PENDING(
        key = "Pending",
        labelEn = "Pending",
        labelBn = "অপেক্ষমাণ",
        color = Color(0xFFE65100), // Dark Orange
        containerColor = Color(0xFFFFF3E0), // Soft Orange Tint
        icon = Icons.Default.HourglassEmpty,
        stepOrder = 1
    ),
    UNDER_REVIEW(
        key = "Under Review",
        labelEn = "In Progress",
        labelBn = "পর্যবেক্ষণে",
        color = Color(0xFF1976D2), // Blue
        containerColor = Color(0xFFE3F2FD), // Soft Blue Tint
        icon = Icons.Default.Autorenew,
        stepOrder = 2
    ),
    RESOLVED(
        key = "Resolved",
        labelEn = "Resolved",
        labelBn = "মীমাংসিত",
        color = Color(0xFF2E7D32), // Green
        containerColor = Color(0xE8E8F5E9), // Soft Green Tint
        icon = Icons.Default.CheckCircle,
        stepOrder = 3
    ),
    REJECTED(
        key = "Rejected",
        labelEn = "Rejected",
        labelBn = "বাতিল",
        color = Color(0xFFC62828), // Red
        containerColor = Color(0xFFFFEBEE), // Soft Red Tint
        icon = Icons.Default.Cancel,
        stepOrder = 4
    );

    companion object {
        fun fromString(status: String?): ComplaintState {
            return values().find { it.key.equals(status, ignoreCase = true) }
                ?: if (status?.contains("progress", ignoreCase = true) == true) UNDER_REVIEW else PENDING
        }
    }
}

/**
 * Compact Status Badge / Chip with Icon and Colored Background.
 */
@Composable
fun ComplaintStatusBadge(
    status: String,
    lang: Language = Language.EN,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val state = ComplaintState.fromString(status)
    val label = if (lang == Language.BN) state.labelBn else state.labelEn

    Surface(
        color = state.containerColor,
        contentColor = state.color,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, state.color.copy(alpha = 0.3f)),
        modifier = modifier.testTag("status_badge_${state.key.lowercase().replace(" ", "_")}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(
                horizontal = if (compact) 8.dp else 12.dp,
                vertical = if (compact) 3.dp else 5.dp
            )
        ) {
            Icon(
                imageVector = state.icon,
                contentDescription = label,
                tint = state.color,
                modifier = Modifier.size(if (compact) 12.dp else 16.dp)
            )
            Spacer(modifier = Modifier.width(if (compact) 4.dp else 6.dp))
            Text(
                text = label,
                color = state.color,
                fontSize = if (compact) 10.sp else 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Visual Progress Stepper Bar showing stages: Pending -> In Progress -> Resolved.
 */
@Composable
fun ComplaintProgressStepper(
    status: String,
    lang: Language = Language.EN,
    modifier: Modifier = Modifier
) {
    val currentState = ComplaintState.fromString(status)
    val steps = listOf(ComplaintState.PENDING, ComplaintState.UNDER_REVIEW, ComplaintState.RESOLVED)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("complaint_progress_stepper")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            steps.forEachIndexed { index, stepState ->
                val isReached = currentState.stepOrder >= stepState.stepOrder
                val isCurrent = currentState == stepState

                val iconColor = when {
                    isCurrent -> stepState.color
                    isReached -> Color(0xFF2E7D32)
                    else -> MaterialTheme.colorScheme.outline
                }

                val bgColor = when {
                    isCurrent -> stepState.containerColor
                    isReached -> Color(0xFFE8F5E9)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(bgColor)
                            .border(
                                width = if (isCurrent) 2.dp else 1.dp,
                                color = iconColor,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = stepState.icon,
                            contentDescription = stepState.labelEn,
                            tint = iconColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (lang == Language.BN) stepState.labelBn else stepState.labelEn,
                        fontSize = 10.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        color = if (isReached) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                // Connector line between steps
                if (index < steps.size - 1) {
                    val linePassed = currentState.stepOrder > stepState.stepOrder
                    Box(
                        modifier = Modifier
                            .weight(0.8f)
                            .height(3.dp)
                            .padding(bottom = 12.dp)
                            .background(
                                color = if (linePassed) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        }
    }
}

/**
 * Dashboard Overview Widget counting complaints by status (Pending, In Progress, Resolved).
 */
@Composable
fun ComplaintDashboardSummaryCard(
    pendingCount: Int,
    inProgressCount: Int,
    resolvedCount: Int,
    lang: Language = Language.EN,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("complaint_dashboard_summary_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Assignment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (lang == Language.BN) "অভিযোগ ট্র্যাক ও স্ট্যাটাস" else "Complaint Status Tracker",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Navigate",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusMetricItem(
                    count = pendingCount,
                    state = ComplaintState.PENDING,
                    lang = lang,
                    modifier = Modifier.weight(1f)
                )
                StatusMetricItem(
                    count = inProgressCount,
                    state = ComplaintState.UNDER_REVIEW,
                    lang = lang,
                    modifier = Modifier.weight(1f)
                )
                StatusMetricItem(
                    count = resolvedCount,
                    state = ComplaintState.RESOLVED,
                    lang = lang,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatusMetricItem(
    count: Int,
    state: ComplaintState,
    lang: Language,
    modifier: Modifier = Modifier
) {
    Surface(
        color = state.containerColor,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, state.color.copy(alpha = 0.2f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = state.icon,
                    contentDescription = null,
                    tint = state.color,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = count.toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = state.color
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (lang == Language.BN) state.labelBn else state.labelEn,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = state.color,
                textAlign = TextAlign.Center
            )
        }
    }
}
