package com.example.zerogrid.emergency

import android.content.Context
import android.util.Log
import com.example.zerogrid.mesh.engine.MeshEngine
import com.example.zerogrid.network.ConnectivityChecker
import com.example.zerogrid.network.RetrofitInstance
import com.example.zerogrid.network.SosApiService
import com.example.zerogrid.network.SosDispatchRequest

data class SosDispatchResult(
    val meshDispatched: Boolean,
    val onlineDispatched: Boolean,
    val queuedOffline: Boolean = false,
    val sosId: String? = null,
    val errorMessage: String? = null
)

/**
 * Unified emergency dispatcher that fans out SOS triggers simultaneously to:
 * 1. Offline BLE & Wi-Fi Direct Mesh (always fires immediately).
 * 2. Online central API (POST /api/sos) when internet is available, or queues for retry.
 */
class UnifiedSosDispatcher(
    private val context: Context,
    private val meshEngine: MeshEngine = MeshEngine.getInstance(context),
    private val sosApi: SosApiService = RetrofitInstance.sosApi,
    private val connectivityChecker: ConnectivityChecker = ConnectivityChecker(context)
) {
    companion object {
        private const val TAG = "UnifiedSosDispatcher"
    }

    suspend fun triggerSos(
        lat: Double? = null,
        lng: Double? = null,
        accuracy: Float? = null,
        category: String = "OTHER",
        message: String = "",
        waterDepthCm: Int = 0,
        passability: String = "ALL_PASSABLE"
    ): SosDispatchResult {
        // 1. Local Mesh Broadcast (always executed)
        val sessionManager = com.zerogrid.mesh.app.ui.UserSessionManager.getInstance(context)
        val displayName = sessionManager.getUserDisplayName().ifBlank { sessionManager.getUserName() }
        val isHazard = category.trim().uppercase() in setOf(
            "WATERLOGGING", "SUBMERGED_UNDERPASS", "DRAINAGE_OVERFLOW", "HEATWAVE", "FALLEN_GRID"
        )
        val meshDispatched = try {
            if (isHazard) {
                meshEngine.triggerHazardBeacon(
                    category = category,
                    waterDepthCm = waterDepthCm,
                    passability = passability,
                    message = message,
                    lat = lat,
                    lon = lng,
                    accuracy = accuracy,
                    senderName = displayName.ifBlank { null }
                )
            } else {
                meshEngine.triggerSosBeacon(
                    category = category,
                    message = message,
                    lat = lat,
                    lon = lng,
                    accuracy = accuracy,
                    senderName = displayName.ifBlank { null }
                )
            }
            Log.d(TAG, "Mesh beacon successfully broadcasted (isHazard=$isHazard).")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to broadcast to local mesh", e)
            false
        }

        // 2. Format Category to match backend schema
        val normalizedCategory = when (category.trim().uppercase()) {
            "WATERLOGGING", "SUBMERGED_UNDERPASS", "DRAINAGE_OVERFLOW",
            "HEATWAVE", "FALLEN_GRID", "MEDICAL",
            "DISASTER", "TRAPPED", "SECURITY" -> category.trim().uppercase()
            else -> "OTHER"
        }

        // 2b. Obtain live Device Battery Percentage
        val batteryPercentage = getDeviceBatteryPercentage(context)

        val request = SosDispatchRequest(
            lat = lat ?: 0.0,
            lng = lng ?: 0.0,
            accuracy = accuracy,
            category = normalizedCategory,
            message = message.ifBlank { if (isHazard) "Hazard beacon reported" else "Emergency SOS triggered" },
            transport = "BOTH",
            batteryPercentage = batteryPercentage,
            waterDepthCm = if (isHazard && waterDepthCm > 0) waterDepthCm else null,
            passability = if (isHazard) passability else null
        )

        // 3. Online Rescue Network Dispatch
        return if (connectivityChecker.isInternetAvailable()) {
            try {
                Log.d(TAG, "Internet connection active. Posting SOS to central rescue API...")
                val response = sosApi.dispatchSos(request)
                if (response.isSuccessful) {
                    val sosId = response.body()?.sos?.id
                    Log.d(TAG, "Online SOS dispatched successfully. Incident ID: $sosId")
                    SosDispatchResult(
                        meshDispatched = meshDispatched,
                        onlineDispatched = true,
                        queuedOffline = false,
                        sosId = sosId
                    )
                } else {
                    Log.w(TAG, "Online SOS failed with HTTP ${response.code()}: ${response.errorBody()?.string()}")
                    enqueueOfflineSos(request)
                    SosDispatchResult(
                        meshDispatched = meshDispatched,
                        onlineDispatched = false,
                        queuedOffline = true,
                        errorMessage = "Server response: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network exception during online SOS dispatch", e)
                enqueueOfflineSos(request)
                SosDispatchResult(
                    meshDispatched = meshDispatched,
                    onlineDispatched = false,
                    queuedOffline = true,
                    errorMessage = e.localizedMessage
                )
            }
        } else {
            Log.d(TAG, "No internet connection detected. Queuing SOS dispatch for retry.")
            enqueueOfflineSos(request)
            SosDispatchResult(
                meshDispatched = meshDispatched,
                onlineDispatched = false,
                queuedOffline = true
            )
        }
    }

    private fun enqueueOfflineSos(request: SosDispatchRequest) {
        try {
            SosUploadWorker.enqueue(context, request)
            Log.d(TAG, "SOS enqueued via WorkManager for background delivery once internet returns.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enqueue SOS with WorkManager", e)
        }
    }

    private fun getDeviceBatteryPercentage(context: Context): Int? {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? android.os.BatteryManager
            val level = bm?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
            if (level != null && level in 0..100) {
                level
            } else {
                val ifilter = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
                val batteryStatus = context.registerReceiver(null, ifilter)
                val rawLevel = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
                if (rawLevel >= 0 && scale > 0) {
                    ((rawLevel / scale.toFloat()) * 100).toInt()
                } else null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not read battery level", e)
            null
        }
    }
}
