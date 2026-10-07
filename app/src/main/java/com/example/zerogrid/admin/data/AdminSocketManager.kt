package com.example.zerogrid.admin.data

import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.zerogrid.BuildConfig
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AdminSocketState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

sealed class AdminSocketEvent {
    object NewSos : AdminSocketEvent()
    object SosUpdated : AdminSocketEvent()
}

/**
 * Singleton Socket.IO manager subscribing to /sos namespace on the shared backend.
 * Dispatches real-time events to active admin UI and triggers alert sound/haptics.
 */
class AdminSocketManager private constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var socket: Socket? = null

    private val _connectionState = MutableStateFlow(AdminSocketState.DISCONNECTED)
    val connectionState = _connectionState.asStateFlow()

    private val _events = MutableSharedFlow<AdminSocketEvent>(extraBufferCapacity = 16)
    val events = _events.asSharedFlow()

    fun connect(context: Context) {
        if (socket?.connected() == true) return

        try {
            val base = BuildConfig.BASE_URL.trimEnd('/')
            val endpoint = "$base/sos"
            Log.d(TAG, "Connecting to Socket.IO namespace: $endpoint")

            _connectionState.value = AdminSocketState.CONNECTING

            val opts = IO.Options().apply {
                transports = arrayOf("websocket", "polling")
                reconnection = true
                reconnectionAttempts = Int.MAX_VALUE
                reconnectionDelay = 1000
                timeout = 20000
            }

            socket = IO.socket(endpoint, opts).apply {
                on(Socket.EVENT_CONNECT) {
                    Log.i(TAG, "Connected to /sos namespace")
                    _connectionState.value = AdminSocketState.CONNECTED
                }

                on(Socket.EVENT_DISCONNECT) {
                    Log.w(TAG, "Disconnected from /sos namespace")
                    _connectionState.value = AdminSocketState.DISCONNECTED
                }

                on(Socket.EVENT_CONNECT_ERROR) { args ->
                    val err = args.getOrNull(0)
                    Log.e(TAG, "Socket.IO connection error: $err")
                    _connectionState.value = AdminSocketState.DISCONNECTED
                }

                on("sos:new") {
                    Log.i(TAG, "Received real-time event: sos:new")
                    triggerAlertSoundAndVibe(context.applicationContext)
                    scope.launch {
                        _events.emit(AdminSocketEvent.NewSos)
                    }
                }

                on("sos:updated") {
                    Log.i(TAG, "Received real-time event: sos:updated")
                    scope.launch {
                        _events.emit(AdminSocketEvent.SosUpdated)
                    }
                }

                connect()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Socket.IO client", e)
            _connectionState.value = AdminSocketState.DISCONNECTED
        }
    }

    fun disconnect() {
        try {
            socket?.disconnect()
            socket?.off()
            socket = null
            _connectionState.value = AdminSocketState.DISCONNECTED
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting socket", e)
        }
    }

    private fun triggerAlertSoundAndVibe(context: Context) {
        try {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, notificationUri)
            ringtone?.play()
        } catch (_: Exception) {}

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 250, 100, 250), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 250, 100, 250), -1)
            }
        } catch (_: Exception) {}
    }

    companion object {
        private const val TAG = "AdminSocketManager"

        @Volatile
        private var instance: AdminSocketManager? = null

        fun getInstance(): AdminSocketManager {
            return instance ?: synchronized(this) {
                instance ?: AdminSocketManager().also { instance = it }
            }
        }
    }
}
