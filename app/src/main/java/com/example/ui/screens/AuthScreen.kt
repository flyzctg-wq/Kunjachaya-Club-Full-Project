package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.UserEntity
import com.example.ui.language.AppLanguage
import com.example.ui.language.Language
import com.example.ui.viewmodel.ClubViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: ClubViewModel,
    onLoginSuccess: () -> Unit
) {
    val lang by viewModel.language.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    
    val context = LocalContext.current
    var authMethod by remember { mutableStateOf("Email") } // "Email" or "Phone"
    var isRegisterMode by remember { mutableStateOf(false) }

    var emailText by remember { mutableStateOf("") }
    var passwordText by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var fullNameText by remember { mutableStateOf("") }
    
    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf("Member") }
    var errorMessage by remember { mutableStateOf("") }
    var isAuthLoading by remember { mutableStateOf(false) }

    fun triggerBiometricAuth() {
        val fragmentActivity = context as? FragmentActivity
        val status = BiometricAuthManager.checkBiometricStatus(context)
        // Biometrics can only unlock an ALREADY-established Firebase Auth
        // session on this device — never to pick or create an account. If no
        // one is really signed in, there's nothing for a fingerprint to unlock.
        val realSignedInUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (realSignedInUser == null) {
            Toast.makeText(
                context,
                if (lang == Language.BN) "বায়োমেট্রিক আনলক করতে প্রথমে ইমেইল/ফোন দিয়ে সাইন ইন করুন।" else "Sign in with email or phone first to enable biometric unlock.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        if (fragmentActivity != null && status == BiometricAuthManager.BiometricStatus.AVAILABLE) {
            BiometricAuthManager.authenticate(
                activity = fragmentActivity,
                title = if (lang == Language.BN) "বায়োমেট্রিক ড্যাশবোর্ড লগইন" else "Biometric Dashboard Login",
                subtitle = if (lang == Language.BN) "আঙ্গুলের ছাপ বা ফেস রিকগনিশন দিয়ে সহজে লগইন করুন" else "Log in using fingerprint or face recognition",
                negativeButtonText = if (lang == Language.BN) "পাসওয়ার্ড ব্যবহার করুন" else "Use Password / PIN",
                onSuccess = {
                    // Re-confirms the SAME real Firebase session that was already
                    // signed in — the fingerprint never selects a different account.
                    val targetUser = viewModel.currentUser.value
                        ?: allUsers.firstOrNull {
                            it.phone.equals(realSignedInUser.email ?: realSignedInUser.phoneNumber, ignoreCase = true) ||
                                it.primaryContact.equals(realSignedInUser.email ?: realSignedInUser.phoneNumber, ignoreCase = true)
                        }
                    if (targetUser != null) viewModel.selectUser(targetUser)
                    Toast.makeText(
                        context,
                        if (lang == Language.BN) "বায়োমেট্রিক যাচাইকরণ সফল!" else "Biometric verification successful!",
                        Toast.LENGTH_SHORT
                    ).show()
                    onLoginSuccess()
                },
                onError = { _, errStr ->
                    Toast.makeText(context, errStr.toString(), Toast.LENGTH_SHORT).show()
                },
                onFailed = {
                    Toast.makeText(
                        context,
                        if (lang == Language.BN) "বায়োমেট্রিক মেলে নাই। আবার চেষ্টা করুন।" else "Biometric match failed. Try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        } else {
            Toast.makeText(
                context,
                if (lang == Language.BN) "এই ডিভাইসে বায়োমেট্রিক লগইন উপলব্ধ নেই।" else "Biometric login isn't available on this device.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(AppLanguage.clubName(lang), fontWeight = FontWeight.Bold) },
                actions = {
                    FilterChip(
                        selected = lang == Language.BN,
                        onClick = { viewModel.toggleLanguage() },
                        label = { Text(if (lang == Language.BN) "বাংলা" else "English", fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.Language, contentDescription = "Language") },
                        modifier = Modifier.padding(end = 12.dp).testTag("auth_lang_toggle")
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            
            // Club Logo
            Surface(
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 3.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.img_club_logo),
                        contentDescription = "Club Logo",
                        modifier = Modifier.size(60.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = AppLanguage.clubName(lang),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Text(
                text = AppLanguage.clubSubtitle(lang),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = AppLanguage.loginTitle(lang),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = AppLanguage.loginSubtitle(lang),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )

                    // Auth Method Selector Tabs (Email/Pass vs Phone OTP)
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_method_segmented_row")
                    ) {
                        SegmentedButton(
                            selected = authMethod == "Email",
                            onClick = { authMethod = "Email" },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            icon = {
                                SegmentedButtonDefaults.Icon(active = authMethod == "Email") {
                                    Icon(
                                        Icons.Default.Email,
                                        contentDescription = null,
                                        modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                                    )
                                }
                            }
                        ) {
                            Text(
                                text = if (lang == Language.BN) "ইমেইল ও পাসওয়ার্ড" else "Email / Pass",
                                fontSize = 11.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                maxLines = 2,
                                lineHeight = 14.sp
                            )
                        }
                        SegmentedButton(
                            selected = authMethod == "Phone",
                            onClick = { authMethod = "Phone" },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            icon = {
                                SegmentedButtonDefaults.Icon(active = authMethod == "Phone") {
                                    Icon(
                                        Icons.Default.PhoneAndroid,
                                        contentDescription = null,
                                        modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                                    )
                                }
                            }
                        ) {
                            Text(
                                text = if (lang == Language.BN) "ফোন নম্বর (ওটিপি)" else "Phone OTP",
                                fontSize = 11.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                maxLines = 2,
                                lineHeight = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Role Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { selectedRole = "Member" },
                            modifier = Modifier.weight(1f).testTag("role_member_btn"),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            colors = if (selectedRole == "Member") ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.primary
                            ) else ButtonDefaults.outlinedButtonColors()
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = AppLanguage.roleMember(lang),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = { selectedRole = "Admin" },
                            modifier = Modifier.weight(1f).testTag("role_admin_btn"),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            colors = if (selectedRole == "Admin") ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.secondary
                            ) else ButtonDefaults.outlinedButtonColors()
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = AppLanguage.roleAdmin(lang),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (errorMessage.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(errorMessage, fontSize = 12.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }

                    if (authMethod == "Email") {
                        if (isRegisterMode) {
                            OutlinedTextField(
                                value = fullNameText,
                                onValueChange = { fullNameText = it },
                                label = { Text(if (lang == Language.BN) "পূর্ণ নাম" else "Full Name") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth().testTag("register_name_input"),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        OutlinedTextField(
                            value = emailText,
                            onValueChange = { emailText = it; errorMessage = "" },
                            label = { Text(if (lang == Language.BN) "ইমেইল এড্রেস" else "Email Address") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth().testTag("email_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = passwordText,
                            onValueChange = { passwordText = it; errorMessage = "" },
                            label = { Text(if (lang == Language.BN) "পাসওয়ার্ড" else "Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle password visibility"
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth().testTag("password_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true
                        )

                        if (!isRegisterMode) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = {
                                        if (emailText.isBlank()) {
                                            Toast.makeText(context, if (lang == Language.BN) "পাসওয়ার্ড রিসেটের জন্য ইমেইল টাইপ করুন" else "Enter email address for password reset", Toast.LENGTH_SHORT).show()
                                        } else {
                                            viewModel.sendPasswordResetEmail(emailText.trim()) { success, err ->
                                                if (success) {
                                                    Toast.makeText(context, if (lang == Language.BN) "পাসওয়ার্ড রিসেট লিংক আপনার ইমেইলে পাঠানো হয়েছে!" else "Password reset email sent to your inbox!", Toast.LENGTH_LONG).show()
                                                } else {
                                                    Toast.makeText(context, err ?: "Reset failed", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.testTag("forgot_password_btn")
                                ) {
                                    Text(
                                        text = if (lang == Language.BN) "পাসওয়ার্ড ভুলে গেছেন?" else "Forgot Password?",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                if (emailText.isBlank() || passwordText.isBlank()) {
                                    errorMessage = if (lang == Language.BN) "ইমেইল ও পাসওয়ার্ড প্রদান করুন" else "Please enter email and password"
                                    return@Button
                                }
                                isAuthLoading = true
                                errorMessage = ""
                                if (isRegisterMode) {
                                    viewModel.registerWithEmail(
                                        email = emailText.trim(),
                                        pass = passwordText,
                                        name = fullNameText.ifBlank { "Resident" },
                                        phone = phoneNumber,
                                        selectedRole = selectedRole,
                                        onResult = { success, error ->
                                            isAuthLoading = false
                                            if (success) {
                                                Toast.makeText(context, if (lang == Language.BN) "ফায়ারবেস একাউন্ট তৈরি ও লগইন সফল!" else "Firebase account registered and logged in!", Toast.LENGTH_SHORT).show()
                                                onLoginSuccess()
                                            } else {
                                                errorMessage = error ?: "Registration failed"
                                            }
                                        }
                                    )
                                } else {
                                    viewModel.loginWithEmail(
                                        email = emailText.trim(),
                                        pass = passwordText,
                                        selectedRole = selectedRole,
                                        onResult = { success, error ->
                                            isAuthLoading = false
                                            if (success) {
                                                Toast.makeText(context, if (lang == Language.BN) "ফায়ারবেস অথেন্টিকেশন সফল!" else "Firebase Auth successful!", Toast.LENGTH_SHORT).show()
                                                onLoginSuccess()
                                            } else {
                                                errorMessage = error ?: "Invalid credentials or account missing"
                                            }
                                        }
                                    )
                                }
                            },
                            enabled = !isAuthLoading,
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("email_auth_btn")
                        ) {
                            if (isAuthLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (lang == Language.BN) "যাচাই করা হচ্ছে..." else "Verifying Auth...", fontSize = 14.sp)
                            } else {
                                Icon(if (isRegisterMode) Icons.Default.PersonAdd else Icons.Default.LockOpen, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isRegisterMode) {
                                        if (lang == Language.BN) "ফায়ারবেস একাউন্ট খুলুন" else "Create Firebase Account"
                                    } else {
                                        if (lang == Language.BN) "ফায়ারবেস অথিঃ লগইন" else "Log In with Firebase Auth"
                                    },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        TextButton(
                            onClick = {
                                isRegisterMode = !isRegisterMode
                                errorMessage = ""
                            },
                            modifier = Modifier.testTag("toggle_register_mode_btn")
                        ) {
                            Text(
                                text = if (isRegisterMode) {
                                        if (lang == Language.BN) "ইতোমধ্যে একাউন্ট আছে? লগইন করুন" else "Already have an account? Sign In"
                                    } else {
                                        if (lang == Language.BN) "নতুন সদস্য? নতুন একাউন্ট খুলুন" else "New resident? Create Firebase Account"
                                    },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        // Phone OTP Auth Mode — real Firebase Phone Auth (SMS),
                        // no local shortcut and no fixed/demo code.
                        if (!otpSent) {
                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it },
                                label = { Text(AppLanguage.phoneNumber(lang)) },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth().testTag("phone_input"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    val activity = context as? androidx.activity.ComponentActivity
                                    if (phoneNumber.isBlank()) {
                                        errorMessage = "Please enter a valid phone number"
                                    } else if (activity == null) {
                                        errorMessage = "Unable to start phone verification"
                                    } else {
                                        isAuthLoading = true
                                        errorMessage = ""
                                        viewModel.sendPhoneOtp(activity, phoneNumber) {
                                            isAuthLoading = false
                                            otpSent = true
                                            otpCode = ""
                                        }
                                    }
                                },
                                enabled = !isAuthLoading,
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("send_otp_btn")
                            ) {
                                if (isAuthLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                } else {
                                    Icon(Icons.Default.Sms, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(AppLanguage.sendOtp(lang), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = otpCode,
                                onValueChange = { otpCode = it },
                                label = { Text(AppLanguage.enterOtp(lang)) },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth().testTag("otp_input"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )

                            Text(
                                text = if (lang == Language.BN) "আপনার ফোনে পাঠানো এসএমএস কোডটি লিখুন" else "Enter the SMS code sent to your phone",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                            )

                            Button(
                                onClick = {
                                    isAuthLoading = true
                                    viewModel.verifyPhoneOtp(otpCode) { success, error ->
                                        isAuthLoading = false
                                        if (success) {
                                            onLoginSuccess()
                                        } else {
                                            errorMessage = error ?: "Invalid or expired code"
                                        }
                                    }
                                },
                                enabled = !isAuthLoading,
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("verify_login_btn")
                            ) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(AppLanguage.verifyAndLogin(lang), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Biometric Authentication Shortcut Button
                    OutlinedButton(
                        onClick = { triggerBiometricAuth() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("biometric_login_btn"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Biometric Auth",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (lang == Language.BN) "বায়োমেট্রিক লগইন (ফিঙ্গারপ্রিন্ট / ফেস)" else "Biometric Login (Fingerprint / Face ID)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}


