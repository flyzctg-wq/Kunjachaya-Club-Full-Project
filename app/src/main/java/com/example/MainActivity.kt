package com.example

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.language.AppLanguage
import com.example.ui.language.AppLanguageProvider
import com.example.ui.language.Language
import com.example.ui.screens.*
import com.example.ui.theme.KunjachayaTheme
import com.example.ui.viewmodel.CalendarViewModel
import com.example.ui.viewmodel.ClubViewModel
import com.example.ui.viewmodel.ComplaintsViewModel
import com.example.ui.viewmodel.NoticesViewModel
import com.example.ui.viewmodel.ProfileViewModel

class MainActivity : FragmentActivity() {
    private val viewModel: ClubViewModel by viewModels()
    private val noticesViewModel: NoticesViewModel by viewModels()
    private val complaintsViewModel: ComplaintsViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()
    private val calendarViewModel: CalendarViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleDeepLink(intent)
        try {
            if (com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
                com.google.firebase.FirebaseApp.initializeApp(this)
            }
            com.example.util.NotificationHelper.createNotificationChannels(this)
            com.example.util.NotificationHelper.initializeFcmTopics(this)

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        enableEdgeToEdge()

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            KunjachayaTheme(darkTheme = isDarkTheme) {
                val currentUser by viewModel.currentUser.collectAsState()
                val lang by viewModel.language.collectAsState()

                AppLanguageProvider(language = lang) {
                    var showOpeningSplash by remember { mutableStateOf(true) }
                    var currentTab by remember { mutableStateOf("dashboard") }

                    if (showOpeningSplash) {
                        OpeningSplashScreen(
                            onSplashFinished = { showOpeningSplash = false }
                        )
                    } else if (currentUser == null) {
                        AuthScreen(
                            viewModel = viewModel,
                            onLoginSuccess = { currentTab = "dashboard" }
                        )
                    } else {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            bottomBar = {
                                NavigationBar {
                                    NavigationBarItem(
                                        selected = currentTab == "dashboard",
                                        onClick = { currentTab = "dashboard" },
                                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                                        label = { Text(AppLanguage.dashboard(lang), fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                        modifier = Modifier.testTag("nav_dashboard")
                                    )
                                    NavigationBarItem(
                                        selected = currentTab == "calendar",
                                        onClick = { currentTab = "calendar" },
                                        icon = { Icon(Icons.Default.Event, contentDescription = "Calendar") },
                                        label = { Text(AppLanguage.calendar(lang), fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                        modifier = Modifier.testTag("nav_calendar")
                                    )
                                     NavigationBarItem(
                                        selected = currentTab == "notices",
                                        onClick = { currentTab = "notices" },
                                        icon = { Icon(Icons.Default.Campaign, contentDescription = "Notices") },
                                        label = { Text(AppLanguage.notices(lang), fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                        modifier = Modifier.testTag("nav_notices")
                                    )
                                    NavigationBarItem(
                                        selected = currentTab == "directory",
                                        onClick = { currentTab = "directory" },
                                        icon = { Icon(Icons.Default.People, contentDescription = "Directory") },
                                        label = { Text(AppLanguage.directory(lang), fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                        modifier = Modifier.testTag("nav_directory")
                                    )
                                    NavigationBarItem(
                                        selected = currentTab == "profile",
                                        onClick = { currentTab = "profile" },
                                        icon = { Icon(Icons.Default.Badge, contentDescription = "Profile") },
                                        label = { Text(AppLanguage.profile(lang), fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                        modifier = Modifier.testTag("nav_profile")
                                    )
                                    NavigationBarItem(
                                        selected = currentTab == "complaints",
                                        onClick = { currentTab = "complaints" },
                                        icon = { Icon(Icons.Default.ReportProblem, contentDescription = "Complaints") },
                                        label = { Text(AppLanguage.complaints(lang), fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                        modifier = Modifier.testTag("nav_complaints")
                                    )
                                    NavigationBarItem(
                                        selected = currentTab == "financials",
                                        onClick = { currentTab = "financials" },
                                        icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Financials") },
                                        label = { Text(AppLanguage.financials(lang), fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                        modifier = Modifier.testTag("nav_financials")
                                    )
                                    if (currentUser?.isExecutiveCommittee() == true) {
                                        NavigationBarItem(
                                            selected = currentTab == "admin",
                                            onClick = { currentTab = "admin" },
                                            icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin") },
                                            label = { Text(AppLanguage.admin(lang), fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                            modifier = Modifier.testTag("nav_admin")
                                        )
                                        NavigationBarItem(
                                            selected = currentTab == "schema",
                                            onClick = { currentTab = "schema" },
                                            icon = { Icon(Icons.Default.Code, contentDescription = "Schema") },
                                            label = { Text("Dev Docs", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                            modifier = Modifier.testTag("nav_schema")
                                        )
                                    }
                                }
                            }
                        ) { innerPadding ->
                            Box(modifier = Modifier.padding(innerPadding)) {
                                when (currentTab) {
                                    "dashboard" -> DashboardScreen(
                                        viewModel = viewModel,
                                        onNavigateToFinancials = { currentTab = "financials" },
                                        onNavigateToComplaints = { currentTab = "complaints" },
                                        onNavigateToProfile = { currentTab = "profile" },
                                        onNavigateToSchemaDocs = { currentTab = "schema" },
                                        onNavigateToNotices = { currentTab = "notices" },
                                        onNavigateToCalendar = { currentTab = "calendar" },
                                        onNavigateToDirectory = { currentTab = "directory" }
                                    )
                                    "calendar" -> EventsCalendarScreen(
                                        clubViewModel = viewModel,
                                        calendarViewModel = calendarViewModel
                                    )
                                    "notices" -> NoticesScreen(
                                        clubViewModel = viewModel,
                                        noticesViewModel = noticesViewModel
                                    )
                                    "directory" -> MemberDirectoryScreen(
                                        viewModel = viewModel
                                    )
                                    "profile" -> ProfileScreen(
                                        viewModel = viewModel,
                                        onLogout = { currentTab = "dashboard" }
                                    )
                                    "profile_editor" -> ProfileEditorScreen(
                                        clubViewModel = viewModel,
                                        profileViewModel = profileViewModel,
                                        onNavigateBack = { currentTab = "profile" }
                                    )
                                    "complaints" -> ComplaintsScreen(
                                        viewModel = viewModel,
                                        complaintsViewModel = complaintsViewModel
                                    )
                                    "financials" -> FinancialsScreen(viewModel = viewModel)
                                    "admin" -> AdminPortalScreen(viewModel = viewModel)
                                    "schema" -> DevDocsScreen(viewModel = viewModel)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: android.content.Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "kunjachayaclub" && uri.host == "payment-result") {
            val orderId = uri.getQueryParameter("orderId") ?: ""
            val status = uri.getQueryParameter("status") ?: ""
            if (status.equals("completed", ignoreCase = true)) {
                android.widget.Toast.makeText(
                    this,
                    "PipraPay Payment Successful! (Order: $orderId)",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            } else if (status.equals("cancelled", ignoreCase = true)) {
                android.widget.Toast.makeText(
                    this,
                    "PipraPay Payment Cancelled.",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } else if (status.equals("failed", ignoreCase = true)) {
                android.widget.Toast.makeText(
                    this,
                    "PipraPay Payment Failed. Please try again.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
