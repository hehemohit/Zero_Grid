package com.example.zerogrid.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.zerogrid.MainActivity
import com.example.zerogrid.R
import com.example.zerogrid.ZeroGridApplication
import com.example.zerogrid.network.AuthRepository
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.zerogrid.mesh.app.ui.UserSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ZeroGridFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "ZeroGridFCM"
        private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        /**
         * Fetch current FCM token and sync to backend if user has an active session.
         */
        fun syncTokenWithBackend(context: Context) {
            val sessionManager = UserSessionManager.getInstance(context)
            if (!sessionManager.isLoggedIn()) {
                Log.d(TAG, "User not logged in; skipping FCM token backend sync.")
                return
            }

            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                    return@addOnCompleteListener
                }

                val token = task.result
                Log.d(TAG, "Current FCM token fetched: ${token.take(15)}...")
                sessionManager.setFcmToken(token)

                serviceScope.launch {
                    try {
                        val authRepo = AuthRepository(sessionManager)
                        val success = authRepo.updateFcmToken(token)
                        Log.d(TAG, "FCM token synced with backend: $success")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to sync FCM token with backend", e)
                    }
                }
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token received: ${token.take(15)}...")

        val sessionManager = UserSessionManager.getInstance(applicationContext)
        sessionManager.setFcmToken(token)

        if (sessionManager.isLoggedIn()) {
            serviceScope.launch {
                try {
                    val authRepo = AuthRepository(sessionManager)
                    val success = authRepo.updateFcmToken(token)
                    Log.d(TAG, "FCM token refreshed and sent to backend: $success")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send refreshed FCM token to server", e)
                }
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM Message received from: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "Emergency SOS Alert"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: "An emergency alert was triggered."

        val sosId = remoteMessage.data["sosId"] ?: ""
        val category = remoteMessage.data["category"] ?: "EMERGENCY"
        val lat = remoteMessage.data["lat"] ?: ""
        val lng = remoteMessage.data["lng"] ?: ""
        val senderName = remoteMessage.data["senderName"] ?: "Emergency Contact"
        val message = remoteMessage.data["message"] ?: ""

        // 1. Immediately inject into MeshEngine state so Emergency Center shows the alert card and logs
        val latD = lat.toDoubleOrNull() ?: 0.0
        val lngD = lng.toDoubleOrNull() ?: 0.0
        try {
            com.example.zerogrid.mesh.engine.MeshEngine.getInstance(applicationContext).recordExternalSosAlert(
                sosId = sosId,
                senderName = senderName,
                category = category,
                message = message.ifBlank { body },
                lat = latD,
                lng = lngD
            )
            Log.d(TAG, "Successfully recorded FCM SOS alert ($sosId) from $senderName in MeshEngine")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record external SOS alert in MeshEngine", e)
        }

        showSosNotification(title, body, sosId, category, lat, lng, senderName)
    }

    private fun showSosNotification(
        title: String,
        body: String,
        sosId: String,
        category: String,
        lat: String,
        lng: String,
        senderName: String
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_SOS_ID", sosId)
            putExtra("EXTRA_CATEGORY", category)
            putExtra("EXTRA_LAT", lat)
            putExtra("EXTRA_LNG", lng)
            putExtra("EXTRA_SENDER_NAME", senderName)
            putExtra("EXTRA_NAVIGATE_TO", "TRACK_SOS")
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, ZeroGridApplication.CHANNEL_SOS_ALERTS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        try {
            NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: SecurityException) {
            Log.e(TAG, "Notification permission missing to show SOS push", e)
        }
    }
}
