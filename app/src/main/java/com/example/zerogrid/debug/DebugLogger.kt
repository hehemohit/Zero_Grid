package com.example.zerogrid.debug

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Locale

enum class DebugLevel { VERBOSE, DEBUG, INFO, WARN, ERROR }

data class DebugLogEntry(
    val id: Long = System.nanoTime(),
    val timestamp: Long = System.currentTimeMillis(),
    val level: DebugLevel,
    val tag: String,
    val message: String
) {
    val timeStr: String get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(timestamp)
}

/**
 * In-app debug logger — live StateFlow circular buffer of log entries, shown in DebugConsoleScreen.
 * Also mirrors every call to Android logcat.
 */
object DebugLogger {
    private const val MAX_ENTRIES = 600

    private val _logs = MutableStateFlow<List<DebugLogEntry>>(emptyList())
    val logs: StateFlow<List<DebugLogEntry>> = _logs.asStateFlow()

    /** BLE peer connection state, keyed by device address */
    private val _peerStates = MutableStateFlow<Map<String, PeerDebugState>>(emptyMap())
    val peerStates: StateFlow<Map<String, PeerDebugState>> = _peerStates.asStateFlow()

    fun log(tag: String, message: String, level: DebugLevel = DebugLevel.DEBUG) {
        val entry = DebugLogEntry(level = level, tag = tag, message = message)
        val current = _logs.value.toMutableList()
        current.add(entry)
        if (current.size > MAX_ENTRIES) current.removeAt(0)
        _logs.value = current
        when (level) {
            DebugLevel.VERBOSE -> Log.v(tag, message)
            DebugLevel.DEBUG   -> Log.d(tag, message)
            DebugLevel.INFO    -> Log.i(tag, message)
            DebugLevel.WARN    -> Log.w(tag, message)
            DebugLevel.ERROR   -> Log.e(tag, message)
        }
    }

    fun updatePeerState(address: String, update: PeerDebugState.() -> PeerDebugState) {
        val current = _peerStates.value.toMutableMap()
        val existing = current[address] ?: PeerDebugState(address = address)
        current[address] = existing.update()
        _peerStates.value = current
    }

    fun clear() {
        _logs.value = emptyList()
        _peerStates.value = emptyMap()
    }
}

data class PeerDebugState(
    val address: String,
    val connectionStage: String = "IDLE",
    val mtu: Int = 23,
    val notifyEnabled: Boolean = false,
    val isReady: Boolean = false,
    val packetsSent: Int = 0,
    val packetsReceived: Int = 0,
    val lastError: String = "",
    val direction: String = ""  // "CLIENT" = we connected out, "SERVER" = they connected in, "BOTH"
)
