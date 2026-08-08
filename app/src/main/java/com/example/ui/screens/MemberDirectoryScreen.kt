package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.UserEntity
import com.example.ui.components.RoleAndModerationPrivilegeDialog
import com.example.ui.language.AppLanguage
import com.example.ui.language.Language
import com.example.ui.viewmodel.ClubViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberDirectoryScreen(
    viewModel: ClubViewModel
) {
    val lang by viewModel.language.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val isUsersOfflineCached by viewModel.isUsersOfflineCached.collectAsState()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("All") } // "All", "Active", "Pending", "Admin"
    var selectedRoadFilter by remember { mutableStateOf("All") }   // "All", "Road 01", "Road 02", "Road 03", etc.
    var selectedUserForDetails by remember { mutableStateOf<UserEntity?>(null) }
    var userToDelete by remember { mutableStateOf<UserEntity?>(null) }
    var userForRoleManagement by remember { mutableStateOf<UserEntity?>(null) }
    var showCreateUserDialog by remember { mutableStateOf(false) }

    val isSuperAdmin = currentUser?.isPresidentOrGeneralSecretary() == true

    // Unique roads for filter
    val availableRoads = remember(allUsers) {
        val roads = allUsers.map { it.road }.filter { it.isNotBlank() }.distinct().sorted()
        listOf("All") + roads
    }

    // Filtered Users
    val filteredUsers = remember(allUsers, searchQuery, selectedStatusFilter, selectedRoadFilter) {
        val q = searchQuery.trim().lowercase()
        allUsers.filter { user ->
            // Search query check
            val matchesQuery = q.isEmpty() ||
                    user.nameEn.lowercase().contains(q) ||
                    user.nameBn.lowercase().contains(q) ||
                    user.phone.lowercase().contains(q) ||
                    user.primaryContact.lowercase().contains(q) ||
                    user.holding.lowercase().contains(q) ||
                    user.road.lowercase().contains(q) ||
                    user.block.lowercase().contains(q) ||
                    user.professionEn.lowercase().contains(q) ||
                    user.professionBn.lowercase().contains(q)

            // Status filter check
            val matchesStatus = when (selectedStatusFilter) {
                "Active" -> user.membershipStatus.equals("Active", ignoreCase = true)
                "Pending" -> user.membershipStatus.equals("Pending", ignoreCase = true)
                "Committee" -> user.isExecutiveCommittee()
                "Founding" -> user.isFoundingMember()
                "General" -> user.isGeneralMember()
                "New" -> user.isPendingApproval()
                else -> true
            }

            // Road filter check
            val matchesRoad = selectedRoadFilter == "All" || user.road.equals(selectedRoadFilter, ignoreCase = true)

            matchesQuery && matchesStatus && matchesRoad
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (lang == Language.BN) "সদস্য ডিরেক্টরি" else "Member Directory",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (lang == Language.BN) "মোট সদস্য: ${allUsers.size} জন (প্রদর্শিত: ${filteredUsers.size})" else "Total Members: ${allUsers.size} (Showing: ${filteredUsers.size})",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = if (isUsersOfflineCached) Color(0xFFFFF3E0) else Color(0xFFE8F5E9),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isUsersOfflineCached) Icons.Default.CloudOff else Icons.Default.Storage,
                                        contentDescription = null,
                                        modifier = Modifier.size(10.dp),
                                        tint = if (isUsersOfflineCached) Color(0xFFE65100) else Color(0xFF2E7D32)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = if (isUsersOfflineCached) {
                                            if (lang == Language.BN) "অফলাইন ক্যাশ" else "Offline Cache"
                                        } else {
                                            if (lang == Language.BN) "রুম সমণ্বিত" else "Room Synced"
                                        },
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isUsersOfflineCached) Color(0xFFE65100) else Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }
                    }
                },
                actions = {
                    if (isSuperAdmin) {
                        IconButton(
                            onClick = { showCreateUserDialog = true },
                            modifier = Modifier.testTag("super_admin_create_user_btn")
                        ) {
                            Icon(
                                Icons.Default.PersonAdd,
                                contentDescription = "Create User",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    FilterChip(
                        selected = lang == Language.BN,
                        onClick = { viewModel.toggleLanguage() },
                        label = {
                            Text(
                                text = if (lang == Language.BN) "বাংলা" else "EN",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Language,
                                contentDescription = "Toggle Language",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.padding(end = 8.dp).testTag("directory_lang_toggle")
                    )
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
            Spacer(modifier = Modifier.height(4.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("member_directory_search_input"),
                placeholder = {
                    Text(
                        text = if (lang == Language.BN) "নাম, মোবাইল নম্বর, হোল্ডিং বা রোড নম্বর দিয়ে খুঁজুন..." else "Search by name, phone, holding or road...",
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
                            modifier = Modifier.testTag("clear_directory_search")
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

            Spacer(modifier = Modifier.height(10.dp))

            // Status Filter Chips Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedStatusFilter == "All",
                        onClick = { selectedStatusFilter = "All" },
                        label = { Text(if (lang == Language.BN) "সকল সদস্য (${allUsers.size})" else "All Members (${allUsers.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("filter_all_members")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedStatusFilter == "Active",
                        onClick = { selectedStatusFilter = "Active" },
                        label = { Text(if (lang == Language.BN) "সক্রিয় (${allUsers.count { it.membershipStatus == "Active" }})" else "Active (${allUsers.count { it.membershipStatus == "Active" }})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF2E7D32)) },
                        modifier = Modifier.testTag("filter_active_members")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedStatusFilter == "Committee",
                        onClick = { selectedStatusFilter = "Committee" },
                        label = { Text(if (lang == Language.BN) "কার্যনির্বাহী পরিষদ (${allUsers.count { it.isExecutiveCommittee() }})" else "Executive Committee (${allUsers.count { it.isExecutiveCommittee() }})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.testTag("filter_admin_members")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedStatusFilter == "Founding",
                        onClick = { selectedStatusFilter = "Founding" },
                        label = { Text(if (lang == Language.BN) "প্রতিষ্ঠাতা সদস্য (${allUsers.count { it.isFoundingMember() }})" else "Founding (${allUsers.count { it.isFoundingMember() }})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.Stars, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFFF9800)) },
                        modifier = Modifier.testTag("filter_entrepreneurial_members")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedStatusFilter == "General",
                        onClick = { selectedStatusFilter = "General" },
                        label = { Text(if (lang == Language.BN) "সাধারণ সদস্য (${allUsers.count { it.isGeneralMember() }})" else "General (${allUsers.count { it.isGeneralMember() }})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary) },
                        modifier = Modifier.testTag("filter_general_members")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedStatusFilter == "New",
                        onClick = { selectedStatusFilter = "New" },
                        label = { Text(if (lang == Language.BN) "নতুন সদস্য (${allUsers.count { it.isPendingApproval() }})" else "New (${allUsers.count { it.isPendingApproval() }})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF009688)) },
                        modifier = Modifier.testTag("filter_new_members")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedStatusFilter == "Pending",
                        onClick = { selectedStatusFilter = "Pending" },
                        label = { Text(if (lang == Language.BN) "অনুমোদনের অপেক্ষায় (${allUsers.count { it.membershipStatus == "Pending" }})" else "Pending (${allUsers.count { it.membershipStatus == "Pending" }})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.HourglassEmpty, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFE65100)) },
                        modifier = Modifier.testTag("filter_pending_members")
                    )
                }
            }

            // Road Filter Chips Row (if multiple roads exist)
            if (availableRoads.size > 2) {
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(availableRoads) { road ->
                        val isSelected = selectedRoadFilter == road
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedRoadFilter = road },
                            label = { Text(if (road == "All") (if (lang == Language.BN) "সকল রোড" else "All Roads") else road, fontSize = 10.sp) },
                            leadingIcon = { Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(12.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Member Cards List
            if (filteredUsers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.PersonOff,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (lang == Language.BN) "কোন সদস্য খুঁজে পাওয়া যায়নি" else "No matching club members found",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize().padding(bottom = 12.dp)
                ) {
                    items(filteredUsers, key = { it.id }) { member ->
                        MemberCard(
                            member = member,
                            lang = lang,
                            isSuperAdmin = isSuperAdmin,
                            onCardClick = { selectedUserForDetails = member },
                            onDeleteClick = { userToDelete = member },
                            onManageRoleClick = { userForRoleManagement = member },
                            onCallClick = {
                                val phoneToCall = member.primaryContact.ifBlank { member.phone }
                                if (phoneToCall.isNotBlank()) {
                                    try {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneToCall"))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Cannot open phone dialer", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "No contact number available", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onSmsClick = {
                                val phoneToSend = member.primaryContact.ifBlank { member.phone }
                                if (phoneToSend.isNotBlank()) {
                                    try {
                                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phoneToSend"))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Cannot open messaging app", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "No contact number available", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Detail Dialog when member card is clicked
    selectedUserForDetails?.let { user ->
        MemberDetailsDialog(
            user = user,
            lang = lang,
            onDismiss = { selectedUserForDetails = null },
            context = context
        )
    }

    // Super Admin Delete Single User Dialog
    userToDelete?.let { user ->
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = {
                Text(
                    text = if (lang == Language.BN) "সদস্য অ্যাকাউন্ট অপসারণ" else "Delete User Account",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    text = if (lang == Language.BN)
                        "আপনি কি নিশ্চিত যে ${user.nameEn} (${user.phone}) অ্যাকাউন্টটি স্থায়ীভাবে মুছে ফেলতে চান?"
                    else
                        "Are you sure you want to permanently delete the account for ${user.nameEn} (${user.phone})?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteUser(user)
                        userToDelete = null
                        Toast.makeText(context, "User ${user.nameEn} removed", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_user_btn")
                ) {
                    Text(if (lang == Language.BN) "অপসারণ করুন" else "Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { userToDelete = null },
                    modifier = Modifier.testTag("cancel_delete_user_btn")
                ) {
                    Text(if (lang == Language.BN) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Role & Moderation Privilege Dialog
    userForRoleManagement?.let { targetUser ->
        RoleAndModerationPrivilegeDialog(
            user = targetUser,
            lang = lang,
            viewModel = viewModel,
            onDismiss = { userForRoleManagement = null }
        )
    }

    // Super Admin Create User Dialog
    if (showCreateUserDialog) {
        var newName by remember { mutableStateOf("") }
        var newEmailOrPhone by remember { mutableStateOf("") }
        var newHolding by remember { mutableStateOf("Holding 10") }
        var newRoad by remember { mutableStateOf("Road 01") }
        var newBlock by remember { mutableStateOf("Block A") }
        var newRole by remember { mutableStateOf(com.example.data.model.MemberClass.GENERAL.name) }

        AlertDialog(
            onDismissRequest = { showCreateUserDialog = false },
            title = {
                Text(
                    text = if (lang == Language.BN) "নতুন সদস্য তৈরি করুন" else "Create New User Account",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text(if (lang == Language.BN) "সদস্যের নাম" else "Full Name") },
                        modifier = Modifier.fillMaxWidth().testTag("create_user_name_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newEmailOrPhone,
                        onValueChange = { newEmailOrPhone = it },
                        label = { Text(if (lang == Language.BN) "ইমেইল / ফোন নম্বর" else "Email or Phone Number") },
                        modifier = Modifier.fillMaxWidth().testTag("create_user_contact_input"),
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newHolding,
                            onValueChange = { newHolding = it },
                            label = { Text(if (lang == Language.BN) "হোল্ডিং" else "Holding") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newRoad,
                            onValueChange = { newRoad = it },
                            label = { Text(if (lang == Language.BN) "রোড" else "Road") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = newRole == com.example.data.model.MemberClass.GENERAL.name,
                            onClick = { newRole = com.example.data.model.MemberClass.GENERAL.name },
                            label = { Text(if (lang == Language.BN) "সাধারণ সদস্য" else "General Member") }
                        )
                        FilterChip(
                            selected = newRole == com.example.data.model.MemberClass.FOUNDING.name,
                            onClick = { newRole = com.example.data.model.MemberClass.FOUNDING.name },
                            label = { Text(if (lang == Language.BN) "প্রতিষ্ঠাতা সদস্য" else "Founding Member") }
                        )
                        FilterChip(
                            selected = newRole == com.example.data.model.MemberClass.LIFETIME.name,
                            onClick = { newRole = com.example.data.model.MemberClass.LIFETIME.name },
                            label = { Text(if (lang == Language.BN) "আজীবন সদস্য" else "Lifetime Member") }
                        )
                        FilterChip(
                            selected = newRole == com.example.data.model.MemberClass.DONOR.name,
                            onClick = { newRole = com.example.data.model.MemberClass.DONOR.name },
                            label = { Text(if (lang == Language.BN) "দাতা সদস্য" else "Donor Member") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank() && newEmailOrPhone.isNotBlank()) {
                            viewModel.createNewUserByAdmin(
                                name = newName,
                                emailOrPhone = newEmailOrPhone,
                                holding = newHolding,
                                road = newRoad,
                                block = newBlock,
                                memberClass = newRole,
                                onComplete = {
                                    showCreateUserDialog = false
                                    Toast.makeText(context, "User created successfully", Toast.LENGTH_SHORT).show()
                                }
                            )
                        } else {
                            Toast.makeText(context, "Please enter name and contact", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("submit_create_user_btn")
                ) {
                    Text(if (lang == Language.BN) "সংরক্ষণ" else "Create")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCreateUserDialog = false },
                    modifier = Modifier.testTag("cancel_create_user_btn")
                ) {
                    Text(if (lang == Language.BN) "বাতিল" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun MemberCard(
    member: UserEntity,
    lang: Language,
    isSuperAdmin: Boolean = false,
    onCardClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null,
    onManageRoleClick: (() -> Unit)? = null,
    onCallClick: () -> Unit,
    onSmsClick: () -> Unit
) {
    val isActive = member.membershipStatus.equals("Active", ignoreCase = true)
    val isAdmin = member.isExecutiveCommittee()
    val isSuper = member.isPresidentOrGeneralSecretary()

    Card(
        onClick = onCardClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("member_card_${member.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Avatar / Initials Circle
                Box(modifier = Modifier.size(54.dp)) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = CircleShape,
                        color = when {
                            isSuper -> MaterialTheme.colorScheme.errorContainer
                            isAdmin -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.secondaryContainer
                        }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = member.nameEn.take(1).uppercase().ifBlank { "M" },
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isSuper -> MaterialTheme.colorScheme.error
                                    isAdmin -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.secondary
                                }
                            )
                        }
                    }

                    // Blood Group Badge
                    if (member.bloodGroup.isNotBlank()) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 4.dp, y = 4.dp),
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFC62828)
                        ) {
                            Text(
                                text = member.bloodGroup,
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (lang == Language.BN) member.nameBn.ifBlank { member.nameEn } else member.nameEn,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        // Role or Status Badge
                        Surface(
                            color = when {
                                isSuper -> MaterialTheme.colorScheme.error
                                isAdmin -> MaterialTheme.colorScheme.primary
                                member.isFoundingMember() -> Color(0xFFFF9800)
                                member.isPendingApproval() -> Color(0xFF009688)
                                member.isGeneralMember() -> Color(0xFF2E7D32)
                                !isActive -> Color(0xFFE65100)
                                else -> Color(0xFF2E7D32)
                            },
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = when {
                                    isAdmin -> AppLanguage.committeePostLabel(lang, member.committeePost)
                                    member.isFoundingMember() -> AppLanguage.roleFounding(lang)
                                    member.isPendingApproval() -> AppLanguage.roleNew(lang)
                                    member.isGeneralMember() -> AppLanguage.roleGeneral(lang)
                                    !isActive -> AppLanguage.pending(lang)
                                    else -> AppLanguage.roleGeneral(lang)
                                },
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${member.holding} • ${member.road} (${member.block})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = if (lang == Language.BN) member.professionBn.ifBlank { member.professionEn } else member.professionEn,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // Contact & Super Admin Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = member.primaryContact.ifBlank { member.phone },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isSuperAdmin && onManageRoleClick != null) {
                        // Role & Privileges Button for Super Admin
                        OutlinedIconButton(
                            onClick = onManageRoleClick,
                            modifier = Modifier.size(32.dp).testTag("manage_role_btn_${member.id}"),
                            shape = CircleShape,
                            colors = IconButtonDefaults.outlinedIconButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                Icons.Default.AdminPanelSettings,
                                contentDescription = "Manage Privileges",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (isSuperAdmin && !isSuper && onDeleteClick != null) {
                        // Delete User Button for Super Admin
                        OutlinedIconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(32.dp).testTag("delete_member_btn_${member.id}"),
                            shape = CircleShape,
                            colors = IconButtonDefaults.outlinedIconButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Member",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // SMS Button
                    OutlinedIconButton(
                        onClick = onSmsClick,
                        modifier = Modifier.size(32.dp).testTag("sms_member_btn_${member.id}"),
                        shape = CircleShape
                    ) {
                        Icon(
                            Icons.Default.Message,
                            contentDescription = "Send SMS",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Call Button
                    FilledIconButton(
                        onClick = onCallClick,
                        modifier = Modifier.size(32.dp).testTag("call_member_btn_${member.id}"),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            Icons.Default.Call,
                            contentDescription = "Call Member",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MemberDetailsDialog(
    user: UserEntity,
    lang: Language,
    onDismiss: () -> Unit,
    context: Context
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = user.nameEn.take(1).uppercase().ifBlank { "M" },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (lang == Language.BN) user.nameBn.ifBlank { user.nameEn } else user.nameEn,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ID: ${user.id} • ${AppLanguage.committeePostLabel(lang, user.committeePost).ifBlank { AppLanguage.roleGeneral(lang) }}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HorizontalDivider()

                DetailRow(
                    label = if (lang == Language.BN) "আবাসিক হোল্ডিং" else "Holding / Floor",
                    value = "${user.holding}, ${user.floor}"
                )
                DetailRow(
                    label = if (lang == Language.BN) "রোড ও ব্লক" else "Road & Block",
                    value = "${user.road}, ${user.block}"
                )
                DetailRow(
                    label = if (lang == Language.BN) "প্রাথমিক ফোন" else "Primary Contact",
                    value = user.primaryContact
                )
                DetailRow(
                    label = if (lang == Language.BN) "জরুরি ফোন" else "Emergency Contact",
                    value = user.emergencyContact.ifBlank { "N/A" }
                )
                DetailRow(
                    label = if (lang == Language.BN) "পেশা" else "Profession",
                    value = if (lang == Language.BN) user.professionBn.ifBlank { user.professionEn } else user.professionEn
                )
                DetailRow(
                    label = if (lang == Language.BN) "রক্তের গ্রুপ" else "Blood Group",
                    value = user.bloodGroup.ifBlank { "N/A" }
                )
                DetailRow(
                    label = if (lang == Language.BN) "পিতা/স্বামীর নাম" else "Father/Spouse Name",
                    value = if (lang == Language.BN) user.fatherOrSpouseNameBn.ifBlank { user.fatherOrSpouseNameEn } else user.fatherOrSpouseNameEn
                )
                DetailRow(
                    label = if (lang == Language.BN) "পরিবারের সদস্য" else "Family Members",
                    value = "${user.familyMembersCount} Person(s)"
                )
                DetailRow(
                    label = if (lang == Language.BN) "সদস্যপদ অবস্থা" else "Membership Status",
                    value = user.membershipStatus
                )
                DetailRow(
                    label = if (lang == Language.BN) "যোগদানের তারিখ" else "Joined Date",
                    value = user.joinedDate
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val phone = user.primaryContact.ifBlank { user.phone }
                        if (phone.isNotBlank()) {
                            try {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Dialer error", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.testTag("dialog_call_btn")
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (lang == Language.BN) "কল করুন" else "Call")
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("dialog_close_btn")
                ) {
                    Text(if (lang == Language.BN) "বন্ধ করুন" else "Close")
                }
            }
        }
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End
        )
    }
}
