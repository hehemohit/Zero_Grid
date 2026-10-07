package com.example.zerogrid

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.example.zerogrid.network.RetrofitInstance
import com.zerogrid.mesh.app.ui.UserSessionManager

class ZeroGridApplication : Application() {

    companion object {
        const val CHANNEL_SOS_ALERTS = "sos_alerts"
        private const val TAG = "ZeroGridApp"
    }

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize Retrofit with TokenStore
        val sessionManager = UserSessionManager.getInstance(this)
        RetrofitInstance.initialize(sessionManager)

        // 2. Setup High-Priority SOS Notification Channel
        setupNotificationChannels()

        // 3. Graceful Firebase initialization
        try {
            com.google.firebase.FirebaseApp.initializeApp(this)
            val projectId = com.google.firebase.FirebaseApp.getInstance().options.projectId
            Log.d("FCM_CHECK", "Firebase initialized: $projectId")

            com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("FCM_CHECK", "Token fetch failed", task.exception)
                    return@addOnCompleteListener
                }
                Log.d("FCM_CHECK", "Token: ${task.result}")
            }
        } catch (e: Exception) {
            Log.w("FCM_CHECK", "Firebase not configured", e)
        }
    }

    private fun setupNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_SOS_ALERTS,
                "Emergency SOS Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical emergency alerts from family and contacts"
                enableVibration(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel '$CHANNEL_SOS_ALERTS' created.")
        }
    }
}
