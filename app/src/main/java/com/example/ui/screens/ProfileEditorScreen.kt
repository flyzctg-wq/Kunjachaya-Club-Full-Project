package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.UserEntity
import com.example.ui.language.Language
import com.example.ui.viewmodel.ClubViewModel
import com.example.ui.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditorScreen(
    clubViewModel: ClubViewModel,
    profileViewModel: ProfileViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val lang by clubViewModel.language.collectAsState()
    val currentUser by clubViewModel.currentUser.collectAsState()
    val isSaving by profileViewModel.isSaving.collectAsState()

    if (currentUser == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(if (lang == Language.BN) "কোন ব্যবহারকারী নির্বাচিত নেই" else "No User Selected")
        }
        return
    }

    val u = currentUser!!

    // State holders for editable fields
    var nameEn by remember { mutableStateOf(u.nameEn) }
    var nameBn by remember { mutableStateOf(u.nameBn) }
    var primaryContact by remember { mutableStateOf(u.primaryContact) }
    var emergencyContact by remember { mutableStateOf(u.emergencyContact) }
    var phone by remember { mutableStateOf(u.phone) }
    var selectedLang by remember { mutableStateOf(lang) }
    
    var professionEn by remember { mutableStateOf(u.professionEn) }
    var professionBn by remember { mutableStateOf(u.professionBn) }

    var fatherOrSpouseNameEn by remember { mutableStateOf(u.fatherOrSpouseNameEn) }
    var fatherOrSpouseNameBn by remember { mutableStateOf(u.fatherOrSpouseNameBn) }
    var motherNameEn by remember { mutableStateOf(u.motherNameEn) }
    var motherNameBn by remember { mutableStateOf(u.motherNameBn) }
    var familyMembersCountText by remember { mutableStateOf(u.familyMembersCount.toString()) }

    var road by remember { mutableStateOf(u.road) }
    var holding by remember { mutableStateOf(u.holding) }
    var block by remember { mutableStateOf(u.block) }
    var floor by remember { mutableStateOf(u.floor) }
    var bloodGroup by remember { mutableStateOf(u.bloodGroup) }
    var dob by remember { mutableStateOf(u.dob) }

    var showSuccessBanner by remember { mutableStateOf(false) }

    val bloodGroups = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")

    // Real-Time Form Validation Rules
    val isNameValid = nameEn.trim().length >= 2
    val isPrimaryContactValid = primaryContact.isBlank() || primaryContact.trim().length >= 6
    val isEmergencyContactValid = emergencyContact.isBlank() || emergencyContact.trim().length >= 6
    val isPhoneValid = phone.isBlank() || phone.trim().length >= 6
    val isDobValid = dob.isBlank() || dob.trim().length >= 4
    val isFamilyCountValid = familyMembersCountText.isEmpty() || (familyMembersCountText.toIntOrNull()?.let { it in 1..30 } == true)
    val isHoldingValid = holding.trim().isNotEmpty()

    fun performSave() {
        if (!isNameValid) {
            Toast.makeText(
                context,
                if (lang == Language.BN) "দয়া করে নাম ফিল্ডটি পূরণ করুন" else "Please enter a valid name",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        if (!isPrimaryContactValid) {
            Toast.makeText(
                context,
                if (lang == Language.BN) "প্রাথমিক ফোন নম্বরটি ১১ ডিজিটের সঠিক মোবাইল নম্বর হতে হবে (যেমন: 01711122233)" else "Primary phone must be a valid 11-digit mobile number",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        if (!isEmergencyContactValid) {
            Toast.makeText(
                context,
                if (lang == Language.BN) "জরুরি পরিচিতির ফোন নম্বরটি সঠিক নয়" else "Emergency contact phone number format is invalid",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        if (!isDobValid) {
            Toast.makeText(
                context,
                if (lang == Language.BN) "জন্ম তারিখের ফর্ম্যাট YYYY-MM-DD হতে হবে" else "Date of birth format must be YYYY-MM-DD",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        if (!isFamilyCountValid) {
            Toast.makeText(
                context,
                if (lang == Language.BN) "পরিবারের সদস্য সংখ্যা ১ থেকে ৩০ এর মধ্যে হতে হবে" else "Family count must be between 1 and 30",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val parsedFamilyCount = familyMembersCountText.toIntOrNull() ?: u.familyMembersCount

        val updatedUser = u.copy(
            nameEn = nameEn,
            nameBn = if (nameBn.isNotBlank()) nameBn else nameEn,
            primaryContact = primaryContact,
            emergencyContact = emergencyContact,
            phone = if (phone.isNotBlank()) phone else primaryContact,
            professionEn = professionEn,
            professionBn = if (professionBn.isNotBlank()) professionBn else professionEn,
            fatherOrSpouseNameEn = fatherOrSpouseNameEn,
            fatherOrSpouseNameBn = if (fatherOrSpouseNameBn.isNotBlank()) fatherOrSpouseNameBn else fatherOrSpouseNameEn,
            motherNameEn = motherNameEn,
            motherNameBn = if (motherNameBn.isNotBlank()) motherNameBn else motherNameEn,
            familyMembersCount = parsedFamilyCount,
            road = road,
            holding = holding,
            block = block,
            floor = floor,
            bloodGroup = bloodGroup,
            dob = dob
        )

        clubViewModel.setLanguage(selectedLang)

        profileViewModel.saveUserProfile(
            updatedUser = updatedUser,
            onSuccess = {
                clubViewModel.selectUser(updatedUser)
                clubViewModel.updateUser(updatedUser)
                showSuccessBanner = true
                Toast.makeText(
                    context,
                    if (selectedLang == Language.BN) "ফায়ারবেস অথ ও ফায়ারস্টোরে প্রোফাইল আপডেট সফল হয়েছে!" else "Profile & preferences updated in Firebase Auth & Firestore successfully!",
                    Toast.LENGTH_LONG
                ).show()
            },
            onError = { err ->
                Toast.makeText(context, "Error: $err", Toast.LENGTH_SHORT).show()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (lang == Language.BN) "প্রোফাইল সম্পাদনা (ফায়ারস্টোর)" else "Profile Editor (Firestore)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("profile_editor_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { performSave() },
                        enabled = !isSaving,
                        modifier = Modifier.testTag("save_profile_top_btn")
                    ) {
                        Text(
                            text = if (lang == Language.BN) "সংরক্ষণ" else "Save",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Success Feedback Banner
            item {
                AnimatedVisibility(visible = showSuccessBanner) {
                    Surface(
                        color = Color(0xFF2E7D32).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (lang == Language.BN) "ফায়ারস্টোরে ব্যক্তিগত তথ্য সফলভাবে আপডেট হয়েছে!" else "Personal details updated in Firestore successfully!",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20)
                            )
                        }
                    }
                }
            }

            // 1. Personal Basic Details
            item {
                SectionHeaderCard(
                    title = if (lang == Language.BN) "ব্যক্তিগত পরিচয় ও নাম" else "Personal Identity & Names",
                    icon = Icons.Default.Person
                ) {
                    OutlinedTextField(
                        value = nameEn,
                        onValueChange = { nameEn = it },
                        label = { Text(if (lang == Language.BN) "পূর্ণ নাম (English)" else "Full Name (English)") },
                        isError = !isNameValid,
                        supportingText = {
                            if (!isNameValid) {
                                Text(
                                    text = if (lang == Language.BN) "কমপক্ষে ৩ অক্ষরের নাম আবশ্যক" else "Minimum 3 characters required",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 11.sp
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("profile_edit_name_en"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = nameBn,
                        onValueChange = { nameBn = it },
                        label = { Text(if (lang == Language.BN) "পূর্ণ নাম (বাংলা)" else "Full Name (Bangla)") },
                        modifier = Modifier.fillMaxWidth().testTag("profile_edit_name_bn"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = dob,
                            onValueChange = { dob = it },
                            label = { Text(if (lang == Language.BN) "জন্ম তারিখ" else "Date of Birth") },
                            placeholder = { Text("YYYY-MM-DD") },
                            isError = !isDobValid,
                            supportingText = {
                                if (!isDobValid) {
                                    Text(
                                        text = if (lang == Language.BN) "ফর্ম্যাট: YYYY-MM-DD" else "Format must be YYYY-MM-DD",
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 11.sp
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("profile_edit_dob"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (lang == Language.BN) "রক্তের গ্রুপ" else "Blood Group",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(bloodGroups) { bg ->
                            FilterChip(
                                selected = bloodGroup == bg,
                                onClick = { bloodGroup = bg },
                                label = { Text(bg, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.testTag("blood_group_$bg")
                            )
                        }
                    }
                }
            }

            // 2. Contact Numbers (Explicit User Requirement)
            item {
                SectionHeaderCard(
                    title = if (lang == Language.BN) "যোগাযোগ নম্বরসমূহ" else "Contact Numbers",
                    icon = Icons.Default.Phone
                ) {
                    OutlinedTextField(
                        value = primaryContact,
                        onValueChange = { primaryContact = it },
                        label = { Text(if (lang == Language.BN) "প্রাথমিক ফোন নম্বর" else "Primary Contact Number") },
                        isError = !isPrimaryContactValid,
                        supportingText = {
                            if (!isPrimaryContactValid) {
                                Text(
                                    text = if (lang == Language.BN) "১১ ডিজিটের মোবাইল নম্বর প্রদান করুন (যেমন: 01711223344)" else "Valid 11-digit mobile number required (e.g., 01711223344)",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 11.sp
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("profile_edit_primary_contact"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Call, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = emergencyContact,
                        onValueChange = { emergencyContact = it },
                        label = { Text(if (lang == Language.BN) "জরুরি পরিচিতির নম্বর" else "Emergency Contact Number") },
                        isError = !isEmergencyContactValid,
                        supportingText = {
                            if (!isEmergencyContactValid) {
                                Text(
                                    text = if (lang == Language.BN) "সঠিক মোবাইল নম্বর প্রদান করুন" else "Format must be a valid mobile number",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 11.sp
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("profile_edit_emergency_contact"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.PhoneInTalk, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(if (lang == Language.BN) "লগইন মোবাইল নম্বর" else "Registered Phone Number") },
                        modifier = Modifier.fillMaxWidth().testTag("profile_edit_phone"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // 3. Profession Details (Explicit User Requirement)
            item {
                SectionHeaderCard(
                    title = if (lang == Language.BN) "পেশাগত তথ্য" else "Profession Details",
                    icon = Icons.Default.Work
                ) {
                    OutlinedTextField(
                        value = professionEn,
                        onValueChange = { professionEn = it },
                        label = { Text(if (lang == Language.BN) "পেশা (English)" else "Profession (English)") },
                        placeholder = { Text("e.g. Business, Engineer, Doctor, Govt Official") },
                        modifier = Modifier.fillMaxWidth().testTag("profile_edit_profession_en"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = professionBn,
                        onValueChange = { professionBn = it },
                        label = { Text(if (lang == Language.BN) "পেশা (বাংলা)" else "Profession (Bangla)") },
                        placeholder = { Text("যেমন: ব্যবসায়ী, প্রকৌশলী, চিকিৎসক, শিক্ষক") },
                        modifier = Modifier.fillMaxWidth().testTag("profile_edit_profession_bn"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // 4. Family Details & Names (Explicit User Requirement)
            item {
                SectionHeaderCard(
                    title = if (lang == Language.BN) "পারিবারিক তথ্য ও নাম" else "Family Names & Details",
                    icon = Icons.Default.FamilyRestroom
                ) {
                    OutlinedTextField(
                        value = fatherOrSpouseNameEn,
                        onValueChange = { fatherOrSpouseNameEn = it },
                        label = { Text(if (lang == Language.BN) "পিতা/স্বামীর নাম (English)" else "Father or Spouse Name (English)") },
                        modifier = Modifier.fillMaxWidth().testTag("profile_edit_father_spouse_en"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = fatherOrSpouseNameBn,
                        onValueChange = { fatherOrSpouseNameBn = it },
                        label = { Text(if (lang == Language.BN) "পিতা/স্বামীর নাম (বাংলা)" else "Father or Spouse Name (Bangla)") },
                        modifier = Modifier.fillMaxWidth().testTag("profile_edit_father_spouse_bn"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = motherNameEn,
                        onValueChange = { motherNameEn = it },
                        label = { Text(if (lang == Language.BN) "মাতার নাম (English)" else "Mother's Name (English)") },
                        modifier = Modifier.fillMaxWidth().testTag("profile_edit_mother_en"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = motherNameBn,
                        onValueChange = { motherNameBn = it },
                        label = { Text(if (lang == Language.BN) "মাতার নাম (বাংলা)" else "Mother's Name (Bangla)") },
                        modifier = Modifier.fillMaxWidth().testTag("profile_edit_mother_bn"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = familyMembersCountText,
                        onValueChange = { familyMembersCountText = it },
                        label = { Text(if (lang == Language.BN) "পরিবারের মোট সদস্য সংখ্যা" else "Family Members Count") },
                        isError = !isFamilyCountValid,
                        supportingText = {
                            if (!isFamilyCountValid) {
                                Text(
                                    text = if (lang == Language.BN) "১ থেকে ৩০ এর মধ্যে একটি সংখ্যা লিখুন" else "Must be a number between 1 and 30",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 11.sp
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("profile_edit_family_count"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // 5. Apartment & Residential Address
            item {
                SectionHeaderCard(
                    title = if (selectedLang == Language.BN) "অ্যাপার্টমেন্ট ও আবাসিক ঠিকানা" else "Apartment & Residence Details",
                    icon = Icons.Default.Home
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = holding,
                            onValueChange = { holding = it },
                            label = { Text(if (selectedLang == Language.BN) "অ্যাপার্টমেন্ট / ফ্ল্যাট নং" else "Apartment / Unit No.") },
                            placeholder = { Text("e.g. Apt 4B, Flat 2A") },
                            modifier = Modifier.weight(1f).testTag("profile_edit_holding"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = road,
                            onValueChange = { road = it },
                            label = { Text(if (selectedLang == Language.BN) "রোড নং" else "Road No.") },
                            modifier = Modifier.weight(1f).testTag("profile_edit_road"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = block,
                            onValueChange = { block = it },
                            label = { Text(if (selectedLang == Language.BN) "ব্লক" else "Block") },
                            modifier = Modifier.weight(1f).testTag("profile_edit_block"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = floor,
                            onValueChange = { floor = it },
                            label = { Text(if (selectedLang == Language.BN) "তলা / ফ্লোর" else "Floor") },
                            modifier = Modifier.weight(1f).testTag("profile_edit_floor"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // 6. Language Preference (EN / BN)
            item {
                SectionHeaderCard(
                    title = if (selectedLang == Language.BN) "অ্যাপ ভাষা পছন্দ (Language Preference)" else "Language Preference (EN/BN)",
                    icon = Icons.Default.Language
                ) {
                    Text(
                        text = if (selectedLang == Language.BN) "পছন্দের ভাষা নির্বাচন করুন (ফায়ারবেস প্রোফাইলে সংরক্ষিত হবে):" else "Select your preferred app language (persisted to Firebase profile):",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = selectedLang == Language.EN,
                            onClick = {
                                selectedLang = Language.EN
                                clubViewModel.setLanguage(Language.EN)
                            },
                            label = { Text("English (EN)", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            leadingIcon = {
                                if (selectedLang == Language.EN) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("lang_chip_en")
                        )

                        FilterChip(
                            selected = selectedLang == Language.BN,
                            onClick = {
                                selectedLang = Language.BN
                                clubViewModel.setLanguage(Language.BN)
                            },
                            label = { Text("বাংলা (BN)", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            leadingIcon = {
                                if (selectedLang == Language.BN) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("lang_chip_bn")
                        )
                    }
                }
            }

            // 7. Save Action Button
            item {
                Button(
                    onClick = { performSave() },
                    enabled = !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_profile_firestore_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    } else {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (selectedLang == Language.BN) "ফায়ারবেস প্রোফাইলে সংরক্ষণ করুন" else "Save Settings to Firebase Profile",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionHeaderCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            content()
        }
    }
}
