package com.example.service

import android.util.Log
import com.example.util.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM registration token: $token")
        // Store or send token to backend/Firestore
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a notification payload
        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "Kunjachaya Club Notice"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: remoteMessage.data["message"]
            ?: "New update from club committee."

        val channelType = remoteMessage.data["type"] ?: "notice"
        val targetChannel = if (channelType.lowercase().contains("complaint")) {
            NotificationHelper.CHANNEL_COMPLAINTS
        } else {
            NotificationHelper.CHANNEL_NOTICES
        }

        // Show push notification
        NotificationHelper.showNotification(
            context = applicationContext,
            title = title,
            body = body,
            channelId = targetChannel
        )
    }

    companion object {
        private const val TAG = "FCM_MessagingService"
    }
}
