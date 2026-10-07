package com.example.zerogrid.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.zerogrid.MainActivity
import com.example.zerogrid.mesh.engine.MeshEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Android Foreground Service holding the Mesh Engine active in the background.
 */
class MeshForegroundService : Service() {

    companion object {
        private const val TAG = "MeshForegroundService"
        private const val CHANNEL_ID = "zerogrid_mesh_channel"
        private const val SOS_CHANNEL_ID = "zerogrid_sos_channel"
        private const val MESSAGES_CHANNEL_ID = "zerogrid_chat_channel"
        private const val HAZARD_CHANNEL_ID = "zerogrid_hazard_channel"
        private const val NOTIFICATION_ID = 1001
        private const val SOS_NOTIFICATION_ID = 9999
        private const val HAZARD_NOTIFICATION_ID = 8888

        fun startService(context: Context) {
            try {
                val intent = Intent(context, MeshForegroundService::class.java)
                context.startForegroundService(intent)
            } catch (e: Throwable) {
                Log.e(TAG, "Error starting MeshForegroundService", e)
            }
        }

        fun stopService(context: Context) {
            try {
                val intent = Intent(context, MeshForegroundService::class.java)
                context.stopService(intent)
            } catch (e: Throwable) {
                Log.e(TAG, "Error stopping MeshForegroundService", e)
            }
        }

        fun showMessageNotification(context: Context, peerId: String, senderName: String, payload: String) {
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(
                        MESSAGES_CHANNEL_ID,
                        "ZeroGrid Direct Messages",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Notifications for incoming mesh direct messages"
                        enableVibration(true)
                        enableLights(true)
                    }
                    notificationManager.createNotificationChannel(channel)
                }

                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("OPEN_PEER_CHAT", peerId)
                }
                val requestCode = (peerId.hashCode() and 0xFFFF)
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                val notification = NotificationCompat.Builder(context, MESSAGES_CHANNEL_ID)
                    .setContentTitle(senderName)
                    .setContentText(payload)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(payload))
                    .setSmallIcon(android.R.drawable.stat_notify_chat)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build()

                val notifId = 20000 + (peerId.hashCode() and 0x7FFF)
                notificationManager.notify(notifId, notification)
            } catch (e: Exception) {
                Log.e(TAG, "Error showing message notification", e)
            }
        }

        fun showSosNotification(context: Context, senderId: String, payload: String) {
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(
                        SOS_CHANNEL_ID,
                        "ZeroGrid Emergency SOS Alerts",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "High-priority alerts for incoming emergency SOS beacons"
                        enableVibration(true)
                        enableLights(true)
                    }
                    notificationManager.createNotificationChannel(channel)
                }

                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    SOS_NOTIFICATION_ID,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                val notification = NotificationCompat.Builder(context, SOS_CHANNEL_ID)
                    .setContentTitle("🚨 EMERGENCY SOS ALERT: $senderId")
                    .setContentText(payload)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(payload))
                    .setSmallIcon(android.R.drawable.stat_notify_error)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build()

                notificationManager.notify(SOS_NOTIFICATION_ID, notification)
            } catch (e: Exception) {
                Log.e(TAG, "Error showing SOS notification", e)
            }
        }

        fun showHazardNotification(context: Context, title: String, payload: String) {
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(
                        HAZARD_CHANNEL_ID,
                        "ZeroGrid Environmental Hazard Alerts",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "High-priority alerts for floods, waterlogging, and extreme weather"
                        enableVibration(true)
                        enableLights(true)
                    }
                    notificationManager.createNotificationChannel(channel)
                }

                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    HAZARD_NOTIFICATION_ID,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                val notification = NotificationCompat.Builder(context, HAZARD_CHANNEL_ID)
                    .setContentTitle(title)
                    .setContentText(payload)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(payload))
                    .setSmallIcon(android.R.drawable.stat_notify_error)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build()

                notificationManager.notify(HAZARD_NOTIFICATION_ID, notification)
            } catch (e: Exception) {
                Log.e(TAG, "Error showing hazard notification", e)
            }
        }
    }

    private var meshEngine: MeshEngine? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Call startForeground IMMEDIATELY to satisfy Android's startup timeout.
        try {
            val notification = createNotification("Initializing ZeroGrid Mesh...")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            serviceScope.launch(Dispatchers.IO) {
                val initStartTime = System.currentTimeMillis()
                try {
                    Log.d(TAG, "Initializing MeshEngine in background...")
                    val engine = MeshEngine.getInstance(applicationContext)
                    meshEngine = engine
                    
                    val nodeIdText = engine.localNodeId
                    val updatedNotification = createNotification("Node ID: $nodeIdText • Mesh Active")
                    val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(NOTIFICATION_ID, updatedNotification)

                    Log.d(TAG, "Starting mesh discovery...")
                    engine.startMesh()
                    Log.d(TAG, "MeshEngine initialized and started in ${System.currentTimeMillis() - initStartTime}ms")
                } catch (e: Exception) {
                    Log.e(TAG, "Error initializing mesh engine in background", e)
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to enter foreground mode", e)
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.i(TAG, "App task removed (swiped away from Recent Apps). Shutting down mesh operations.")
        try {
            serviceScope.cancel()
            meshEngine?.stopMesh()
        } catch (e: Throwable) {
            Log.e(TAG, "Error stopping mesh engine in onTaskRemoved", e)
        }
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (_: Throwable) {
            try {
                @Suppress("DEPRECATION")
                stopForeground(true)
            } catch (_: Throwable) {}
        }
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        try {
            serviceScope.cancel()
            meshEngine?.stopMesh()
        } catch (e: Throwable) {
            Log.e(TAG, "Error stopping mesh engine", e)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(statusText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ZeroGrid Mesh Active")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "ZeroGrid Mesh Daemon",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps BLE & Wi-Fi Direct multi-hop routing active"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }
}
