package com.example.zerogrid.mesh.engine

import androidx.compose.runtime.Stable

/**
 * Represents a unified peer node in the ZeroGrid Mesh Network.
 * Supports multi-interface tracking (BLE and Wi-Fi Direct) under a single logical profile.
 */
@Stable
data class MeshNode(
    val nodeId: String,
    var alias: String,
    var rssi: Int = 0,
    var transportType: String = TRANSPORT_BLE,
    var bleRssi: Int? = null,
    var wifiRssi: Int? = null,
    var lastSeenTimestamp: Long = System.currentTimeMillis(),
    var hopDistance: Int = 1,
    var isDirectNeighbor: Boolean = true,
    var nextHopNodeId: String? = null,
    val availableTransports: MutableSet<String> = java.util.concurrent.ConcurrentHashMap.newKeySet()
) {
    companion object {
        const val TRANSPORT_BLE = "BLE"
        const val TRANSPORT_WIFI_DIRECT = "Wi-Fi Direct"
        const val TRANSPORT_MULTI_HOP = "Multi-Hop Mesh"
    }

    /** Returns true if this device is reachable concurrently over both BLE and Wi-Fi */
    fun isMultiInterface(): Boolean = availableTransports.size > 1

    /** Returns the best available signal strength across all active interfaces */
    fun getBestSignalRssi(): Int {
        val b = bleRssi ?: -999
        val w = wifiRssi ?: -999
        return maxOf(b, w).takeIf { it > -900 } ?: rssi
    }
}
