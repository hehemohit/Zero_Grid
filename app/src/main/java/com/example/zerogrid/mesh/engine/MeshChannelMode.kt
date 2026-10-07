package com.example.zerogrid.mesh.engine

import android.content.Context

/**
 * Dedicated, mutually exclusive communication channel modes for ZeroGrid.
 * Operates strictly on ONE radio at a time to eliminate dual-radio concurrency bugs,
 * socket/GATT collisions, and mixed-channel state divergence.
 */
enum class MeshChannelMode(
    val id: String,
    val displayName: String,
    val transportName: String
) {
    BLE(
        id = "BLE",
        displayName = "Bluetooth Low Energy",
        transportName = MeshNode.TRANSPORT_BLE
    ),
    WIFI_DIRECT(
        id = "WIFI_DIRECT",
        displayName = "Wi-Fi Direct",
        transportName = MeshNode.TRANSPORT_WIFI_DIRECT
    );

    val label: String
        get() = if (this == BLE) "BLE" else "Wi-Fi Direct"

    companion object {
        private const val PREFS_NAME = "zerogrid_identity_prefs"
        private const val KEY_CHANNEL_MODE = "mesh_channel_mode"

        fun getSavedMode(context: Context): MeshChannelMode {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val saved = prefs.getString(KEY_CHANNEL_MODE, null)
            return values().firstOrNull { it.id.equals(saved, ignoreCase = true) } ?: BLE
        }

        fun saveMode(context: Context, mode: MeshChannelMode) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_CHANNEL_MODE, mode.id).apply()
        }
    }
}
