package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {

    const val CHANNEL_NOTICES = "channel_notices"
    const val CHANNEL_COMPLAINTS = "channel_complaints"

    /**
     * Create required Notification Channels for Android O+ (API 26+)
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val noticesChannel = NotificationChannel(
                CHANNEL_NOTICES,
                "Club Notices & Announcements",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Push notifications for new club notices, updates, and emergency announcements"
                enableVibration(true)
            }

            val complaintsChannel = NotificationChannel(
                CHANNEL_COMPLAINTS,
                "Complaint Status Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Push notifications for complaint status changes and resolution notices"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(noticesChannel)
            notificationManager.createNotificationChannel(complaintsChannel)
        }
    }

    /**
     * Initialize FCM and subscribe residents to 'notices' and 'complaints' topics.
     */
    fun initializeFcmTopics(context: Context, userId: String? = null) {
        createNotificationChannels(context)
        try {
            val app = try { com.google.firebase.FirebaseApp.getInstance() } catch (e: Throwable) { null }
            val apiKey = app?.options?.apiKey ?: ""
            val markers = listOf("dummy", "replace_with", "placeholder", "your_api_key", "xxxx")
            if (apiKey.isBlank() || markers.any { apiKey.contains(it, ignoreCase = true) }) {
                android.util.Log.d("FCM_HELPER", "No real Firebase API key configured, skipping FCM topic subscription")
                return
            }
            val fcm = com.google.firebase.messaging.FirebaseMessaging.getInstance()
            fcm.subscribeToTopic("notices")
            fcm.subscribeToTopic("complaints")
            if (!userId.isNullOrEmpty()) {
                fcm.subscribeToTopic("user_$userId")
            }
            fcm.token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    android.util.Log.d("FCM_HELPER", "FCM Registration Token: ${task.result}")
                }
            }
        } catch (e: Throwable) {
            android.util.Log.w("FCM_HELPER", "FCM Topic initialization skipped: ${e.message}")
        }
    }

    /**
     * Retrieve the active FCM registration token. Returns null when there is
     * no real, configured backend or the token fetch failed — callers must
     * treat null as "not registered for push" rather than substituting a
     * fake placeholder token.
     */
    fun getFcmToken(onTokenReceived: (String?) -> Unit) {
        try {
            val app = try { com.google.firebase.FirebaseApp.getInstance() } catch (e: Throwable) { null }
            val apiKey = app?.options?.apiKey ?: ""
            val markers = listOf("dummy", "replace_with", "placeholder", "your_api_key", "xxxx")
            if (apiKey.isBlank() || markers.any { apiKey.contains(it, ignoreCase = true) }) {
                onTokenReceived(null)
                return
            }
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                .addOnCompleteListener { task ->
                    if (task.isSuccessful && task.result != null) {
                        onTokenReceived(task.result)
                    } else {
                        onTokenReceived(null)
                    }
                }
        } catch (e: Throwable) {
            onTokenReceived(null)
        }
    }

    /**
     * Dispatch a system notification to the user device
     */
    fun showNotification(
        context: Context,
        title: String,
        body: String,
        channelId: String = CHANNEL_NOTICES,
        notificationId: Int = (System.currentTimeMillis() % 100000).toInt()
    ) {
        // Create channels if not created yet
        createNotificationChannels(context)

        // Check runtime permission for Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Permission not granted; skip posting system notification
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
