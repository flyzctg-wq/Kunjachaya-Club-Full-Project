package com.example.util

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Centralized manager for handling Firebase Connection, Authentication State,
 * Firestore NoSQL Database initialization, and Firebase Cloud Messaging (FCM).
 */
object FirebaseManager {

    private const val TAG = "FirebaseManager"

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _fcmToken = MutableStateFlow<String?>(null)
    val fcmToken: StateFlow<String?> = _fcmToken.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    /**
     * True when Firebase is running against a real, configured backend.
     * False whenever the API key is missing or still a placeholder — in that
     * case the app falls back to local-only Room storage and the UI should
     * clearly surface that data is not being synced anywhere, rather than
     * implying a live connection that doesn't exist.
     */
    private val _isBackendConnected = MutableStateFlow(false)
    val isBackendConnected: StateFlow<Boolean> = _isBackendConnected.asStateFlow()

    /**
     * Recognizes unset / placeholder Firebase API keys so we never mistake a
     * template value for a real, working credential.
     */
    private fun isPlaceholderApiKey(apiKey: String): Boolean {
        if (apiKey.isBlank()) return true
        val markers = listOf("dummy", "replace_with", "placeholder", "your_api_key", "xxxx")
        return markers.any { apiKey.contains(it, ignoreCase = true) }
    }

    var auth: FirebaseAuth? = null
        private set

    var firestore: FirebaseFirestore? = null
        private set

    var messaging: FirebaseMessaging? = null
        private set

    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    /**
     * Initializes Firebase App, FirebaseAuth, FirebaseFirestore, and FirebaseMessaging modules.
     */
    @Synchronized
    fun initialize(context: Context) {
        if (_isInitialized.value) {
            Log.d(TAG, "FirebaseManager is already initialized.")
            return
        }

        try {
            // 1. Ensure FirebaseApp is initialized
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
                Log.d(TAG, "FirebaseApp initialized successfully.")
            }

            val app = try { FirebaseApp.getInstance() } catch (e: Throwable) { null }
            val apiKey = app?.options?.apiKey ?: ""
            val isDummyKey = isPlaceholderApiKey(apiKey)

            if (isDummyKey) {
                Log.w(TAG, "Firebase API Key is not configured ($apiKey). No backend is connected — running local-only.")
                _isBackendConnected.value = false
                _fcmToken.value = null
                _isInitialized.value = true
                return
            }

            // 2. Initialize FirebaseAuth & AuthState Listener
            initAuth()

            // 3. Initialize FirebaseFirestore with Offline Persistence settings
            initFirestore()

            // 4. Initialize Firebase Cloud Messaging (FCM) & Topics
            initMessaging(context)

            _isBackendConnected.value = true
            _isInitialized.value = true
            Log.d(TAG, "FirebaseManager initialization completed successfully.")
        } catch (e: Throwable) {
            Log.w(TAG, "Firebase initialization failed, running local-only: ${e.message}")
            _isBackendConnected.value = false
            _fcmToken.value = null
            _isInitialized.value = true
        }
    }

    private fun initAuth() {
        try {
            val app = try { FirebaseApp.getInstance() } catch (e: Throwable) { null }
            val apiKey = app?.options?.apiKey ?: ""
            if (isPlaceholderApiKey(apiKey)) {
                Log.w(TAG, "FirebaseAuth skipped due to dummy API key.")
                return
            }
            auth = FirebaseAuth.getInstance()
            _currentUser.value = auth?.currentUser

            // Listen to auth state changes in real time
            authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                val user = firebaseAuth.currentUser
                _currentUser.value = user
                Log.d(TAG, "FirebaseAuth state updated. User ID: ${user?.uid ?: "Anonymous/Logged Out"}")
            }
            authStateListener?.let { auth?.addAuthStateListener(it) }
        } catch (e: Throwable) {
            Log.w(TAG, "FirebaseAuth initialization skipped: ${e.message}")
        }
    }

    private fun initFirestore() {
        try {
            val app = try { FirebaseApp.getInstance() } catch (e: Throwable) { null }
            val apiKey = app?.options?.apiKey ?: ""
            if (isPlaceholderApiKey(apiKey)) {
                Log.w(TAG, "FirebaseFirestore skipped due to dummy API key.")
                return
            }
            val db = FirebaseFirestore.getInstance()
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            db.firestoreSettings = settings
            firestore = db
            Log.d(TAG, "FirebaseFirestore initialized with offline persistence enabled.")
        } catch (e: Throwable) {
            Log.w(TAG, "FirebaseFirestore initialization skipped: ${e.message}")
        }
    }

    private fun initMessaging(context: Context) {
        try {
            // Notification Channels setup
            NotificationHelper.createNotificationChannels(context)

            val app = try { FirebaseApp.getInstance() } catch (e: Throwable) { null }
            val apiKey = app?.options?.apiKey ?: ""
            if (isPlaceholderApiKey(apiKey)) {
                Log.w(TAG, "FirebaseMessaging skipped due to dummy API key.")
                _fcmToken.value = null
                return
            }

            messaging = FirebaseMessaging.getInstance()

            // Subscribe to default topics
            try {
                messaging?.subscribeToTopic("notices")
                    ?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d(TAG, "Subscribed to FCM topic: notices")
                        }
                    }
            } catch (t: Throwable) {
                Log.w(TAG, "Notice topic subscription skipped: ${t.message}")
            }

            try {
                messaging?.subscribeToTopic("complaints")
                    ?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d(TAG, "Subscribed to FCM topic: complaints")
                        }
                    }
            } catch (t: Throwable) {
                Log.w(TAG, "Complaints topic subscription skipped: ${t.message}")
            }

            // Fetch registration token
            try {
                messaging?.token?.addOnCompleteListener { task ->
                    if (task.isSuccessful && task.result != null) {
                        val token = task.result
                        _fcmToken.value = token
                        Log.d(TAG, "FCM Token retrieved: $token")
                    } else {
                        Log.w(TAG, "Fetching FCM registration token failed: ${task.exception?.message}")
                        _fcmToken.value = null
                    }
                }
            } catch (t: Throwable) {
                Log.w(TAG, "FCM token call failed: ${t.message}")
                _fcmToken.value = null
            }
        } catch (e: Throwable) {
            Log.w(TAG, "FirebaseMessaging initialization skipped: ${e.message}")
            _fcmToken.value = null
        }
    }

    /**
     * Subscribe user to custom topic (e.g. user-specific or admin topic)
     */
    fun subscribeToTopic(topic: String, onComplete: ((Boolean) -> Unit)? = null) {
        messaging?.subscribeToTopic(topic)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "Successfully subscribed to topic: $topic")
            } else {
                Log.e(TAG, "Failed to subscribe to topic: $topic", task.exception)
            }
            onComplete?.invoke(task.isSuccessful)
        }
    }

    /**
     * Unsubscribe from a custom topic
     */
    fun unsubscribeFromTopic(topic: String, onComplete: ((Boolean) -> Unit)? = null) {
        messaging?.unsubscribeFromTopic(topic)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "Successfully unsubscribed from topic: $topic")
            } else {
                Log.e(TAG, "Failed to unsubscribe from topic: $topic", task.exception)
            }
            onComplete?.invoke(task.isSuccessful)
        }
    }

    /**
     * Returns true if user is logged in via Firebase Auth
     */
    fun isUserLoggedIn(): Boolean {
        return auth?.currentUser != null
    }

    /**
     * Get current user UID
     */
    fun getCurrentUid(): String? {
        return auth?.currentUser?.uid
    }

    /**
     * Sign out current Firebase Auth session
     */
    fun signOut() {
        try {
            auth?.signOut()
            _currentUser.value = null
            Log.d(TAG, "Signed out from Firebase Auth.")
        } catch (e: Exception) {
            Log.e(TAG, "Error during signOut: ${e.message}", e)
        }
    }
}
