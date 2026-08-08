package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.fragment.app.FragmentActivity
import android.widget.Toast
import com.example.utils.BiometricAuthManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.UserEntity
import com.example.ui.language.AppLanguage
import com.example.ui.language.Language
import com.example.ui.viewmodel.ClubViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ClubViewModel,
    onLogout: () -> Unit
) {
    val lang by viewModel.language.collectAsState()
    val user by viewModel.currentUser.collectAsState()
    val userFinancials by viewModel.userFinancials.collectAsState()
    val context = LocalContext.current

    var showNidDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var biometricEnabled by remember { mutableStateOf(true) }
    var financialFilter by remember { mutableStateOf("ALL") } // ALL, Paid, Due, Donation

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(AppLanguage.profile(lang), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = { showEditDialog = true },
                        modifier = Modifier.testTag("edit_profile_btn")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Profile")
                    }
                    IconButton(
                        onClick = {
                            viewModel.logout()
                            onLogout()
                        },
                        modifier = Modifier.testTag("logout_btn")
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { paddingValues ->
        if (user == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No User Selected")
            }
            return@Scaffold
        }

        val u = user!!

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Header Profile Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("profile_header_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_club_logo),
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier.size(64.dp).clip(CircleShape)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = if (lang == Language.BN) u.nameBn else u.nameEn,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (lang == Language.BN) u.professionBn else u.professionEn,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AssistChip(
                                onClick = {},
                                label = { Text("ID: ${u.id}", fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            )
                            AssistChip(
                                onClick = {},
                                label = {
                                    Text(
                                        if (u.membershipStatus == "Active") AppLanguage.active(lang) else AppLanguage.pending(lang),
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (u.membershipStatus == "Active") Color(0xFF2E7D32) else Color(0xFFE65100),
                                    labelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // 2. Firebase Auth & Account Status Card
            item {
                ProfileSectionCard(
                    title = if (lang == Language.BN) "অ্যাকাউন্ট স্ট্যাটাস ও অথেন্টিকেশন" else "Account Status & Firebase Auth",
                    icon = Icons.Default.VerifiedUser
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (lang == Language.BN) "ফায়ারবেস অথেন্টিকেশন স্ট্যাটাস" else "Firebase Auth Status",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (lang == Language.BN) "যাচাইকৃত ফোন নম্বর: ${u.primaryContact}" else "Verified Phone: ${u.primaryContact}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                        Surface(
                            color = Color(0xFF2E7D32).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("VERIFIED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    ProfileField(
                        label = if (lang == Language.BN) "সদস্য শ্রেণি" else "Member Class",
                        value = when {
                            u.isAdvisoryCouncilMember() -> AppLanguage.get(lang, "Advisory Member", "উপদেষ্টা সদস্য")
                            u.isFoundingMember() -> AppLanguage.roleFounding(lang)
                            u.isLifetimeMember() -> AppLanguage.get(lang, "Lifetime Member", "আজীবন সদস্য")
                            u.isDonorMember() -> AppLanguage.get(lang, "Donor Member", "দাতা সদস্য")
                            u.isPendingApproval() -> AppLanguage.roleNew(lang)
                            else -> AppLanguage.roleGeneral(lang)
                        },
                        isHighlight = true
                    )
                    if (u.isExecutiveCommittee()) {
                        ProfileField(
                            label = if (lang == Language.BN) "কার্যনির্বাহী পদ" else "Executive Committee Post",
                            value = AppLanguage.committeePostLabel(lang, u.committeePost),
                            isHighlight = true
                        )
                    }
                    ProfileField(
                        label = if (lang == Language.BN) "ফায়ারস্টোর ইউজার ডকুমেন্টস আইডি" else "Firestore Document ID",
                        value = u.id
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (lang == Language.BN) "অ্যাক্টিভ সদস্য অধিকার ও সুবিধাসমূহ:" else "Active Member Privileges:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val privileges = if (lang == Language.BN) {
                        listOf(
                            "✓ এজিএম (AGM) ভোটাধিকার এবং সিদ্ধান্ত গ্রহণের অধিকার",
                            "✓ কমিউনিটি হল ও অডিটোরিয়াম বুকিংয়ে ৫০% বিশেষ ছাড়",
                            "✓ ২৪/৭ জরুরী সড়ক সিকিউরিটি ও মেইনটেন্যান্স রেসপন্স",
                            "✓ ফ্রি হেলথ চেকআপ ও রক্তদান ক্যাম্পে অগ্রাধিকার"
                        )
                    } else {
                        listOf(
                            "✓ Full AGM voting rights & policy participation",
                            "✓ 50% discount on Community Hall & Venue bookings",
                            "✓ 24/7 Priority Emergency Maintenance & Gate Security",
                            "✓ Free Health Camp & Blood Donation priority access"
                        )
                    }

                    privileges.forEach { item ->
                        Text(
                            text = item,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }

            // 3. Member Payment History Section
            item {
                val totalPaid = userFinancials.filter { it.type == "Paid" && it.status == "Completed" }.sumOf { it.amount }
                val totalDue = userFinancials.filter { it.type == "Due" && it.status == "Pending" }.sumOf { it.amount }
                val totalDonations = userFinancials.filter { it.type == "Donation" }.sumOf { it.amount }

                ProfileSectionCard(
                    title = if (lang == Language.BN) "সদস্য পেমেন্ট হিস্ট্রি ও পরিশোধের বিবরণ" else "Member Payment History & Statements",
                    icon = Icons.Default.ReceiptLong
                ) {
                    // Financial summary row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32).copy(alpha = 0.12f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(if (lang == Language.BN) "মোট পরিশোধিত" else "Total Paid", fontSize = 10.sp, color = Color(0xFF2E7D32))
                                Text("৳ ${totalPaid.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F).copy(alpha = 0.12f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(if (lang == Language.BN) "বকেয়া চার্জ" else "Pending Due", fontSize = 10.sp, color = Color(0xFFD32F2F))
                                Text("৳ ${totalDue.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0288D1).copy(alpha = 0.12f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(if (lang == Language.BN) "মোট অনুদান" else "Donations", fontSize = 10.sp, color = Color(0xFF0288D1))
                                Text("৳ ${totalDonations.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0288D1))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Filter chips row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = financialFilter == "ALL",
                            onClick = { financialFilter = "ALL" },
                            label = { Text("All (${userFinancials.size})", fontSize = 11.sp) },
                            modifier = Modifier.testTag("payment_filter_all")
                        )
                        FilterChip(
                            selected = financialFilter == "Paid",
                            onClick = { financialFilter = "Paid" },
                            label = { Text("Paid Receipts", fontSize = 11.sp) },
                            modifier = Modifier.testTag("payment_filter_paid")
                        )
                        FilterChip(
                            selected = financialFilter == "Due",
                            onClick = { financialFilter = "Due" },
                            label = { Text("Dues", fontSize = 11.sp) },
                            modifier = Modifier.testTag("payment_filter_due")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val filteredList = when (financialFilter) {
                        "Paid" -> userFinancials.filter { it.type == "Paid" }
                        "Due" -> userFinancials.filter { it.type == "Due" }
                        "Donation" -> userFinancials.filter { it.type == "Donation" }
                        else -> userFinancials
                    }

                    if (filteredList.isEmpty()) {
                        Text(
                            text = if (lang == Language.BN) "কোনো লেনদেনের তথ্য পাওয়া যায়নি" else "No payment statements found",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        filteredList.forEach { record ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .testTag("financial_record_${record.id}"),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (lang == Language.BN) record.titleBn else record.titleEn,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "৳ ${record.amount.toInt()}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (record.type) {
                                                "Paid" -> Color(0xFF2E7D32)
                                                "Donation" -> Color(0xFF0288D1)
                                                else -> Color(0xFFD32F2F)
                                            }
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (record.type == "Paid") Icons.Default.CheckCircle else Icons.Default.Schedule,
                                                contentDescription = null,
                                                tint = if (record.type == "Paid") Color(0xFF2E7D32) else Color(0xFFE65100),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${record.monthYear} • ${record.date}",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }

                                        if (record.transactionId.isNotBlank()) {
                                            Text(
                                                text = "${record.paymentGateway} • ${record.transactionId}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier
                                                    .clickable {
                                                        Toast.makeText(context, "Copied Txn: ${record.transactionId}", Toast.LENGTH_SHORT).show()
                                                    }
                                                    .testTag("copy_txn_${record.id}")
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Personal Information
            item {
                ProfileSectionCard(
                    title = AppLanguage.personalInformation(lang),
                    icon = Icons.Default.Person
                ) {
                    ProfileField(label = if (lang == Language.BN) "নাম (ইংরেজী)" else "Name (English)", value = u.nameEn)
                    ProfileField(label = if (lang == Language.BN) "নাম (বাংলা)" else "Name (Bangla)", value = u.nameBn)
                    ProfileField(label = if (lang == Language.BN) "জন্ম তারিখ" else "Date of Birth", value = u.dob)
                    ProfileField(label = if (lang == Language.BN) "রক্তের গ্রুপ" else "Blood Group", value = u.bloodGroup, isHighlight = true)
                    ProfileField(label = if (lang == Language.BN) "পেশা" else "Profession", value = if (lang == Language.BN) u.professionBn else u.professionEn)
                }
            }

            // 5. Detailed Address & Apartment
            item {
                ProfileSectionCard(
                    title = if (lang == Language.BN) "অ্যাপার্টমেন্ট ও আবাসিক ঠিকানা" else "Apartment & Residence Details",
                    icon = Icons.Default.Home
                ) {
                    ProfileField(label = if (lang == Language.BN) "অ্যাপার্টমেন্ট / ফ্ল্যাট নং" else "Apartment / Unit No.", value = u.holding, isHighlight = true)
                    ProfileField(label = if (lang == Language.BN) "রোড নং" else "Road No.", value = u.road)
                    ProfileField(label = if (lang == Language.BN) "ব্লক" else "Block", value = u.block)
                    ProfileField(label = if (lang == Language.BN) "তলা/ফ্লোর" else "Floor", value = u.floor)
                }
            }

            // 6. Contact Numbers
            item {
                ProfileSectionCard(
                    title = if (lang == Language.BN) "যোগাযোগ নম্বরসমূহ" else "Contact Numbers",
                    icon = Icons.Default.Phone
                ) {
                    ProfileField(label = if (lang == Language.BN) "প্রধান মোবাইল" else "Primary Contact", value = u.primaryContact, isHighlight = true)
                    ProfileField(label = if (lang == Language.BN) "জরুরী যোগাযোগ" else "Emergency Contact", value = u.emergencyContact)
                    ProfileField(label = if (lang == Language.BN) "রেজিস্টার্ড ফোন" else "Registered Phone", value = u.phone)
                }
            }

            // 6b. App Language Preference
            item {
                ProfileSectionCard(
                    title = if (lang == Language.BN) "ভাষা পছন্দ (Language Preference)" else "App Language Preference (EN/BN)",
                    icon = Icons.Default.Language
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (lang == Language.BN) "বর্তমান সক্রিয় ভাষা:" else "Current Active Language:",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = if (lang == Language.BN) "বাংলা (BN)" else "English (EN)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.setLanguage(Language.EN) },
                            modifier = Modifier.weight(1f).testTag("profile_lang_en_btn"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (lang == Language.EN) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent
                            )
                        ) {
                            Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("English (EN)", fontSize = 12.sp, fontWeight = if (lang == Language.EN) FontWeight.Bold else FontWeight.Normal)
                        }

                        OutlinedButton(
                            onClick = { viewModel.setLanguage(Language.BN) },
                            modifier = Modifier.weight(1f).testTag("profile_lang_bn_btn"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (lang == Language.BN) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent
                            )
                        ) {
                            Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("বাংলা (BN)", fontSize = 12.sp, fontWeight = if (lang == Language.BN) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }

            // 6c. Global Theme Preference (Light/Dark Mode)
            item {
                val isDarkTheme by viewModel.isDarkTheme.collectAsState()
                ProfileSectionCard(
                    title = if (lang == Language.BN) "থিম পছন্দ (Theme Mode)" else "Theme Mode Preference",
                    icon = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (lang == Language.BN) "বর্তমান থিম মোড:" else "Active Display Theme:",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = if (isDarkTheme) (if (lang == Language.BN) "ডার্ক মোড" else "Dark Mode") else (if (lang == Language.BN) "লাইট মোড" else "Light Mode"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.isDarkTheme.value = false },
                            modifier = Modifier.weight(1f).testTag("profile_theme_light_btn"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (!isDarkTheme) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent
                            )
                        ) {
                            Icon(Icons.Default.LightMode, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (lang == Language.BN) "লাইট মোড" else "Light Mode", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.isDarkTheme.value = true },
                            modifier = Modifier.weight(1f).testTag("profile_theme_dark_btn"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isDarkTheme) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent
                            )
                        ) {
                            Icon(Icons.Default.DarkMode, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (lang == Language.BN) "ডার্ক মোড" else "Dark Mode", fontSize = 12.sp)
                        }
                    }
                }
            }

            // 6d. Firebase Cloud Messaging (FCM) Push Notification Channels
            item {
                var fcmSubscribed by remember { mutableStateOf(true) }
                var fcmTokenText by remember { mutableStateOf("Fetching token...") }

                LaunchedEffect(Unit) {
                    com.example.util.NotificationHelper.getFcmToken { token ->
                        fcmTokenText = if (token != null) token.take(24) + "..."
                        else if (lang == Language.BN) "সংযুক্ত নয় — কোনো ব্যাকএন্ড কনফিগার করা হয়নি" else "Not connected — no backend configured"
                    }
                }

                ProfileSectionCard(
                    title = if (lang == Language.BN) "ফায়ারবেস ক্লাউড মেসেজিং (FCM পুশ)" else "Firebase Push Notifications (FCM)",
                    icon = Icons.Default.NotificationsActive
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (lang == Language.BN) "ক্লাব পুশ নোটিফিকেশন সার্ভিস" else "Realtime Push Notifications",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = if (lang == Language.BN) "নতুন নোটিশ ও অভিযোগ আপডেটে তাৎক্ষণিক পুশ পাবেন" else "Get instant alerts on new notices & ticket status updates",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = fcmSubscribed,
                            onCheckedChange = { checked ->
                                fcmSubscribed = checked
                                if (checked) {
                                    com.example.util.NotificationHelper.initializeFcmTopics(context, u.id)
                                    Toast.makeText(context, if (lang == Language.BN) "FCM পুশ সার্ভিস সক্রিয় করা হয়েছে" else "FCM Push Notifications Subscribed", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, if (lang == Language.BN) "FCM পুশ সার্ভিস নিষ্ক্রিয় করা হয়েছে" else "FCM Push Notifications Muted", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("fcm_subscription_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = if (lang == Language.BN) "সাবস্ক্রাইব করা চ্যানেলসমূহ:" else "Subscribed FCM Channels:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (lang == Language.BN) "• ক্লাব নোটিশ ও ঘোষণা (notices)" else "• Club Notices & Bulletins (notices)", fontSize = 10.sp)
                        }
                        Row(modifier = Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ReportProblem, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (lang == Language.BN) "• অভিযোগ সমাধান ট্র্যাকার (complaints)" else "• Ticket Status Tracker (complaints)", fontSize = 10.sp)
                        }
                        Row(modifier = Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("• FCM Token: $fcmTokenText", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                com.example.util.NotificationHelper.showNotification(
                                    context = context,
                                    title = "📢 Test FCM Notice: Committee Alert",
                                    body = "This is a test FCM push notification for club notices.",
                                    channelId = com.example.util.NotificationHelper.CHANNEL_NOTICES
                                )
                                Toast.makeText(context, if (lang == Language.BN) "টেস্ট নোটিশ নোটিফিকেশন পাঠানো হয়েছে" else "Test notice FCM sent", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f).testTag("test_fcm_notice_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (lang == Language.BN) "টেস্ট নোটিশ পুশ" else "Test Notice Push", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                com.example.util.NotificationHelper.showNotification(
                                    context = context,
                                    title = "🔔 Test FCM Complaint Update",
                                    body = "Ticket status updated to Resolved. Thank you!",
                                    channelId = com.example.util.NotificationHelper.CHANNEL_COMPLAINTS
                                )
                                Toast.makeText(context, if (lang == Language.BN) "টেস্ট অভিযোগ আপডেট পাঠানো হয়েছে" else "Test complaint status FCM sent", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f).testTag("test_fcm_complaint_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.ReportProblem, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (lang == Language.BN) "টেস্ট কমপ্লেন্ট পুশ" else "Test Ticket Push", fontSize = 11.sp)
                        }
                    }
                }
            }

            // 7. Family Details
            item {
                ProfileSectionCard(
                    title = AppLanguage.familyDetails(lang),
                    icon = Icons.Default.Groups
                ) {
                    ProfileField(label = if (lang == Language.BN) "পিতা/স্বামীর নাম" else "Father/Spouse", value = if (lang == Language.BN) u.fatherOrSpouseNameBn else u.fatherOrSpouseNameEn)
                    ProfileField(label = if (lang == Language.BN) "মাতার নাম" else "Mother Name", value = if (lang == Language.BN) u.motherNameBn else u.motherNameEn)
                    ProfileField(label = if (lang == Language.BN) "পরিবারের সদস্য সংখ্যা" else "Family Count", value = "${u.familyMembersCount} Persons")
                }
            }

            // 8. Biometric Security Settings
            item {
                ProfileSectionCard(
                    title = if (lang == Language.BN) "বায়োমেট্রিক নিরাপত্তা ও দ্রুত লগইন" else "Biometric Security & Fast Login",
                    icon = Icons.Default.Fingerprint
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (lang == Language.BN) "ফিঙ্গারপ্রিন্ট / ফেস আইডি সুবিধা" else "Fingerprint / Face ID Login",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (lang == Language.BN) "ক্লাব ড্যাশবোর্ডে দ্রুত ও নিরাপদ প্রবেশ" else "Instant biometric login for secure access",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = biometricEnabled,
                            onCheckedChange = {
                                biometricEnabled = it
                                Toast.makeText(
                                    context,
                                    if (it) (if (lang == Language.BN) "বায়োমেট্রিক লগইন চালু হয়েছে" else "Biometric login enabled")
                                    else (if (lang == Language.BN) "বায়োমেট্রিক লগইন বন্ধ হয়েছে" else "Biometric login disabled"),
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.testTag("toggle_biometric_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            val fragmentActivity = context as? FragmentActivity
                            if (fragmentActivity != null) {
                                BiometricAuthManager.authenticate(
                                    activity = fragmentActivity,
                                    title = if (lang == Language.BN) "বায়োমেট্রিক সেন্সর পরীক্ষা" else "Biometric Sensor Test",
                                    subtitle = if (lang == Language.BN) "আঙ্গুলের ছাপ বা ফেস আইডি পরীক্ষা করুন" else "Test device fingerprint or face recognition sensor",
                                    negativeButtonText = if (lang == Language.BN) "বাতিল" else "Cancel",
                                    onSuccess = {
                                        Toast.makeText(context, if (lang == Language.BN) "বায়োমেট্রিক সেন্সর সঠিকভাবে কাজ করছে!" else "Biometric sensor verified successfully!", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { _, errStr ->
                                        Toast.makeText(context, "Biometric: $errStr", Toast.LENGTH_SHORT).show()
                                    },
                                    onFailed = {
                                        Toast.makeText(context, if (lang == Language.BN) "বায়োমেট্রিক মেলে নাই" else "Biometric match failed", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            } else {
                                Toast.makeText(context, if (lang == Language.BN) "বায়োমেট্রিক সক্রিয় আছে!" else "Biometric recognition ready!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("test_biometric_btn")
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (lang == Language.BN) "বায়োমেট্রিক সেন্সর টেস্ট করুন" else "Test Biometric Sensor")
                    }
                }
            }

            // 9. Documents & NID
            item {
                ProfileSectionCard(
                    title = AppLanguage.documentsNid(lang),
                    icon = Icons.Default.Badge
                ) {
                    ProfileField(label = "NID Number", value = u.nidFrontUrl)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { showNidDialog = true },
                        modifier = Modifier.fillMaxWidth().testTag("view_nid_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (lang == Language.BN) "জাতীয় পরিচয়পত্র দেখুন" else "View NID Card Documents")
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    // NID Viewer Modal Dialog
    if (showNidDialog) {
        AlertDialog(
            onDismissRequest = { showNidDialog = false },
            title = { Text(if (lang == Language.BN) "জাতীয় পরিচয়পত্র (এনআইডি)" else "National ID Documents") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (lang == Language.BN) "সামনের অংশ (Front View):" else "Front View:")
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(36.dp))
                                Text(user?.nidFrontUrl ?: "", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Government of Bangladesh - Smart NID", fontSize = 10.sp)
                            }
                        }
                    }

                    Text(if (lang == Language.BN) "পেছনের অংশ (Back View):" else "Back View:")
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(32.dp))
                                Text(user?.nidBackUrl ?: "", fontSize = 11.sp)
                                Text("Residential Address Verified", fontSize = 10.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNidDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Edit Profile Modal
    if (showEditDialog && user != null) {
        Dialog(
            onDismissRequest = { showEditDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                ProfileEditorScreen(
                    clubViewModel = viewModel,
                    onNavigateBack = { showEditDialog = false }
                )
            }
        }
    }
}

@Composable
fun ProfileSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
            content()
        }
    }
}

@Composable
fun ProfileField(label: String, value: String, isHighlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (isHighlight) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = value,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        } else {
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
