package com.example.zerogrid.hardware

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.location.LocationManagerCompat

data class HardwareState(
    val isBluetoothEnabled: Boolean,
    val isLocationEnabled: Boolean,
    val isWifiEnabled: Boolean
)

object HardwareStateManager {

    fun isBluetoothEnabled(context: Context): Boolean {
        return try {
            val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            bm?.adapter?.isEnabled == true
        } catch (_: Exception) {
            false
        }
    }

    fun isLocationEnabled(context: Context): Boolean {
        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (lm != null) {
                LocationManagerCompat.isLocationEnabled(lm)
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun isWifiEnabled(context: Context): Boolean {
        return try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            wm?.isWifiEnabled == true
        } catch (_: Exception) {
            false
        }
    }

    fun getCurrentState(context: Context): HardwareState {
        return HardwareState(
            isBluetoothEnabled = isBluetoothEnabled(context),
            isLocationEnabled = isLocationEnabled(context),
            isWifiEnabled = isWifiEnabled(context)
        )
    }

    fun openBluetoothSettings(context: Context) {
        try {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    fun openLocationSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun openWifiSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}

@Composable
fun rememberHardwareState(): State<HardwareState> {
    val context = LocalContext.current
    val hardwareState = remember { mutableStateOf(HardwareStateManager.getCurrentState(context)) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                hardwareState.value = HardwareStateManager.getCurrentState(context)
            }
        }

        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        }

        context.registerReceiver(receiver, filter)

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }

    return hardwareState
}
