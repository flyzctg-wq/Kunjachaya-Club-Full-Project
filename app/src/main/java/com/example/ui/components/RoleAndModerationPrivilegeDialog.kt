package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CommitteePost
import com.example.data.model.MemberClass
import com.example.data.model.UserEntity
import com.example.ui.language.AppLanguage
import com.example.ui.language.Language
import com.example.ui.viewmodel.ClubViewModel

/**
 * Executive Committee dialog for assigning a member's constitutional class
 * (ধারা-৬) and, if applicable, one of the 15 Executive Committee posts
 * (ধারা-১৪), plus the day-to-day moderation privileges tied to that post.
 */
@Composable
fun RoleAndModerationPrivilegeDialog(
    user: UserEntity,
    lang: Language,
    viewModel: ClubViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var selectedClass by remember { mutableStateOf(MemberClass.fromLabel(user.memberClass)) }
    var selectedPost by remember { mutableStateOf(CommitteePost.fromLabel(user.committeePost)) }
    var canNotices by remember { mutableStateOf(user.canManageNotices) }
    var canComplaints by remember { mutableStateOf(user.canManageComplaints) }
    var canMembers by remember { mutableStateOf(user.canManageMembers) }
    var canFinancials by remember { mutableStateOf(user.canManageFinancials) }
    var canDelete by remember { mutableStateOf(user.canDeleteItems) }
    var isSaving by remember { mutableStateOf(false) }

    val currentUser by viewModel.currentUser.collectAsState()
    val isR5Leader = currentUser?.canAppointOfficers() == true
    val isTopPost = selectedPost == CommitteePost.PRESIDENT || selectedPost == CommitteePost.GENERAL_SECRETARY

    // President / General Secretary carry the constitution's broadest authority (ধারা-১৭) — all ticks follow automatically.
    LaunchedEffect(selectedPost) {
        if (isTopPost) {
            canNotices = true
            canComplaints = true
            canMembers = true
            canFinancials = true
            canDelete = true
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (lang == Language.BN) "সদস্য শ্রেণি ও কমিটি পদ" else "Member Class & Committee Post",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "${user.nameEn} • ID: ${user.id}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (lang == Language.BN) "১. সদস্য শ্রেণি (ধারা-৬):" else "1. Member Class (ধারা-৬):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RoleSelectionOption(
                        title = if (lang == Language.BN) "সাধারণ সদস্য" else "General Member",
                        description = if (lang == Language.BN) "ভোটাধিকারসহ সক্রিয় নিবাসী সদস্য" else "Active resident with full voting rights",
                        icon = Icons.Default.Person,
                        isSelected = selectedClass == MemberClass.GENERAL,
                        color = MaterialTheme.colorScheme.secondary,
                        onClick = { selectedClass = MemberClass.GENERAL }
                    )
                    RoleSelectionOption(
                        title = if (lang == Language.BN) "প্রতিষ্ঠাতা সদস্য" else "Founding Member",
                        description = if (lang == Language.BN) "স্থায়ী পরিষদের সদস্য (ধারা-১৩খ)" else "Automatically seats on the Standing Council (ধারা-১৩খ)",
                        icon = Icons.Default.Stars,
                        isSelected = selectedClass == MemberClass.FOUNDING,
                        color = Color(0xFFFF9800),
                        onClick = { selectedClass = MemberClass.FOUNDING }
                    )
                    RoleSelectionOption(
                        title = if (lang == Language.BN) "আজীবন সদস্য" else "Lifetime Member",
                        description = if (lang == Language.BN) "উপস্থিত ও মতামত দিতে পারবেন, ভোটাধিকার নেই (ধারা-৯খ)" else "May attend & speak, no vote or office (ধারা-৯খ)",
                        icon = Icons.Default.WorkspacePremium,
                        isSelected = selectedClass == MemberClass.LIFETIME,
                        color = Color(0xFF6D4C41),
                        onClick = { selectedClass = MemberClass.LIFETIME }
                    )
                    RoleSelectionOption(
                        title = if (lang == Language.BN) "দাতা সদস্য" else "Donor Member",
                        description = if (lang == Language.BN) "উপস্থিত ও মতামত দিতে পারবেন, ভোটাধিকার নেই (ধারা-৯খ)" else "May attend & speak, no vote or office (ধারা-৯খ)",
                        icon = Icons.Default.VolunteerActivism,
                        isSelected = selectedClass == MemberClass.DONOR,
                        color = Color(0xFF00838F),
                        onClick = { selectedClass = MemberClass.DONOR }
                    )
                    RoleSelectionOption(
                        title = if (lang == Language.BN) "উপদেষ্টা সদস্য" else "Advisory Member",
                        description = if (lang == Language.BN) "উপদেষ্টা পরিষদের সদস্য, সর্বোচ্চ ১৫ জন (ধারা-১৩ক)" else "Sits on the Advisory Council, max 15 (ধারা-১৩ক)",
                        icon = Icons.Default.EmojiPeople,
                        isSelected = selectedClass == MemberClass.ADVISORY,
                        color = Color(0xFF5E35B1),
                        onClick = { selectedClass = MemberClass.ADVISORY }
                    )
                    RoleSelectionOption(
                        title = if (lang == Language.BN) "নতুন সদস্য (অনুমোদনের অপেক্ষায়)" else "New Member (Pending Approval)",
                        description = if (lang == Language.BN) "অনবোর্ডিং ফেইজ, অনুমোদনের অপেক্ষায় (ধারা-১০)" else "Onboarding phase, awaiting EC approval (ধারা-১০)",
                        icon = Icons.Default.PersonAdd,
                        isSelected = selectedClass == MemberClass.NEW,
                        color = Color(0xFF009688),
                        onClick = { selectedClass = MemberClass.NEW }
                    )
                }

                Text(
                    text = if (lang == Language.BN) "২. কার্যনির্বাহী পরিষদ পদ (ধারা-১৪, ঐচ্ছিক):" else "2. Executive Committee Post (ধারা-১৪, optional):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RoleSelectionOption(
                        title = if (lang == Language.BN) "কোনো কমিটি পদ নেই" else "No committee post",
                        description = if (!isR5Leader) (if (lang == Language.BN) "পদ পরিবর্তনের জন্য R5 লিডার (সভাপতি/সাধারণ সম্পাদক) অনুমতি প্রয়োজন" else "R5 Leader (President/GS) permission required to change officer post") else "",
                        icon = Icons.Default.Person,
                        isSelected = selectedPost == null,
                        color = MaterialTheme.colorScheme.outline,
                        onClick = {
                            if (isR5Leader) selectedPost = null
                            else Toast.makeText(context, if (lang == Language.BN) "শুধুমাত্র R5 লিডাররা অফিসার পদ নিয়োগ/বহিস্কার করতে পারেন" else "Only R5 Leaders can appoint or demote R4 Officers", Toast.LENGTH_SHORT).show()
                        }
                    )
                    CommitteePost.entries.forEach { post ->
                        RoleSelectionOption(
                            title = AppLanguage.committeePostLabel(lang, post.name),
                            description = "",
                            icon = if (post == CommitteePost.PRESIDENT || post == CommitteePost.GENERAL_SECRETARY)
                                Icons.Default.VerifiedUser else Icons.Default.Security,
                            isSelected = selectedPost == post,
                            color = if (post == CommitteePost.PRESIDENT || post == CommitteePost.GENERAL_SECRETARY)
                                MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            onClick = {
                                if (isR5Leader) selectedPost = post
                                else Toast.makeText(context, if (lang == Language.BN) "শুধুমাত্র R5 লিডাররা অফিসার পদ নিয়োগ/বহিস্কার করতে পারেন" else "Only R5 Leaders can appoint or demote R4 Officers", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }

                // Moderation privilege ticks — shown for anyone holding a committee post
                AnimatedVisibility(visible = selectedPost != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (lang == Language.BN) "৩. নির্দিষ্ট মডারেশন প্রিভিলেজ (Tick Marks):" else "3. Specific Moderation Privileges (Tick Marks):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        PrivilegeTickRow(
                            title = if (lang == Language.BN) "নোটিশ ও ইভেন্ট ব্যবস্থাপনা (Manage Notices)" else "Manage Notices & Broadcasts",
                            isChecked = canNotices,
                            enabled = !isTopPost,
                            onCheckedChange = { canNotices = it }
                        )

                        PrivilegeTickRow(
                            title = if (lang == Language.BN) "অভিযোগ বক্স পর্যালোচনা ও আপডেট (Manage Complaints)" else "Manage Complaints & Requests",
                            isChecked = canComplaints,
                            enabled = !isTopPost,
                            onCheckedChange = { canComplaints = it }
                        )

                        PrivilegeTickRow(
                            title = if (lang == Language.BN) "রেসিডেন্ট ডিরেক্টরি ও মেম্বার অনুমোদন (Manage Members)" else "Manage Resident Directory & Approvals",
                            isChecked = canMembers,
                            enabled = !isTopPost,
                            onCheckedChange = { canMembers = it }
                        )

                        PrivilegeTickRow(
                            title = if (lang == Language.BN) "আর্থিক রেকর্ড ও রসিদ তৈরি (Manage Financials)" else "Manage Financial Dues & PDF Receipts",
                            isChecked = canFinancials,
                            enabled = !isTopPost,
                            onCheckedChange = { canFinancials = it }
                        )

                        PrivilegeTickRow(
                            title = if (lang == Language.BN) "রেকর্ড ও অভিযোগ মুছে ফেলার ক্ষমতা (Delete Rights)" else "Delete Records & Entries Authority",
                            isChecked = canDelete,
                            enabled = !isTopPost,
                            onCheckedChange = { canDelete = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isSaving = true
                    viewModel.updateUserRoleAndPrivileges(
                        userId = user.id,
                        memberClass = selectedClass.name,
                        committeePost = selectedPost?.name ?: "",
                        canManageNotices = canNotices,
                        canManageComplaints = canComplaints,
                        canManageMembers = canMembers,
                        canManageFinancials = canFinancials,
                        canDeleteItems = canDelete,
                        onComplete = { success ->
                            isSaving = false
                            if (success) {
                                Toast.makeText(
                                    context,
                                    if (lang == Language.BN) "ফায়ারস্টোরে ভূমিকা ও প্রিভিলেজ আপডেট হয়েছে!" else "Role & Privileges synced to Firestore successfully!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onDismiss()
                            } else {
                                Toast.makeText(
                                    context,
                                    if (lang == Language.BN) "হালনাগাদ করতে ব্যর্থ হয়েছে" else "Failed to update privileges in Firestore",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                },
                enabled = !isSaving,
                modifier = Modifier.testTag("save_role_privileges_btn")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(if (lang == Language.BN) "ফায়ারস্টোরে সংরক্ষণ করুন" else "Save Privileges")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_role_dialog_btn")
            ) {
                Text(if (lang == Language.BN) "বাতিল" else "Cancel")
            }
        }
    )
}

@Composable
private fun RoleSelectionOption(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) color else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (description.isNotBlank()) {
                    Text(
                        text = description,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PrivilegeTickRow(
    title: String,
    isChecked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!isChecked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = if (enabled) onCheckedChange else null,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}
