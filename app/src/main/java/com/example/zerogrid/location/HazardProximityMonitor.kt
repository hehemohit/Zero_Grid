package com.example.zerogrid.location

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Lightweight proximity monitor.
 *
 * Runs a 60-second coroutine loop that:
 * 1. Reads the user's last-known GPS position (zero power).
 * 2. Reads hazards from [HazardCacheManager] (zero network).
 * 3. Computes Haversine distance to each cached flood hazard.
 * 4. Emits a [HazardAlert] on [alertFlow] when the user enters the 200 m trigger radius.
 *
 * The cache itself is refreshed in a non-blocking side-coroutine when:
 * - The user has moved more than 1 km from the last fetch origin, OR
 * - The cache is older than 5 minutes AND internet is available.
 */
object HazardProximityMonitor {

    private const val TAG            = "HazardProximityMonitor"
    private const val POLL_MS        = 60_000L   // 60-second tick
    private const val TRIGGER_RADIUS = 200f       // metres

    // SharedFlow so multiple collectors can observe without queuing (replay = 0)
    private val _alertFlow = MutableSharedFlow<HazardAlert>(replay = 0, extraBufferCapacity = 8)
    val alertFlow: SharedFlow<HazardAlert> = _alertFlow

    /** IDs of hazards for which the user has already been warned this session. */
    private val warnedIds = mutableSetOf<String>()

    /**
     * Start the monitoring loop inside [scope].
     * The loop cancels automatically when [scope] is cancelled (service destroyed).
     *
     * @param context  Application context for GPS + connectivity access.
     * @param scope    CoroutineScope tied to the service lifetime.
     */
    fun startMonitoring(context: Context, scope: CoroutineScope) {
        scope.launch {
            // Prime the cache from disk before the first tick
            HazardCacheManager.loadFromDisk(context)

            // Initial fetch on startup if online
            val initLoc = LocationHelper.getLastKnownLocation(context)
            if (initLoc != null) {
                HazardCacheManager.refreshIfOnline(context, initLoc.lat, initLoc.lng)
            }

            Log.d(TAG, "Monitoring started.")

            while (isActive) {
                delay(POLL_MS)
                tick(context, scope)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    private fun tick(context: Context, scope: CoroutineScope) {
        val userLoc = LocationHelper.getLastKnownLocation(context) ?: return
        val hazards = HazardCacheManager.getCachedHazards()

        // Refresh cache in background (non-blocking) if stale or user moved
        val shouldRefresh = HazardCacheManager.isStaleTtl() ||
                HazardCacheManager.shouldRefreshForLocation(userLoc.lat, userLoc.lng)
        if (shouldRefresh) {
            scope.launch {
                HazardCacheManager.refreshIfOnline(context, userLoc.lat, userLoc.lng)
            }
        }

        // Check proximity against every cached hazard
        for (hazard in hazards) {
            if (hazard.eventId in warnedIds) continue

            val dist = haversineMeters(userLoc.lat, userLoc.lng, hazard.lat, hazard.lng)
            if (dist <= TRIGGER_RADIUS) {
                warnedIds += hazard.eventId
                val alert = HazardAlert(
                    eventId        = hazard.eventId,
                    waterDepthCm   = hazard.waterDepthCm,
                    passability    = hazard.passability,
                    category       = hazard.category,
                    hazardLat      = hazard.lat,
                    hazardLng      = hazard.lng,
                    distanceMeters = dist
                )
                Log.w(TAG, "⚠ Hazard within ${dist.toInt()} m: ${hazard.eventId}")
                scope.launch { _alertFlow.emit(alert) }
            }
        }
    }

    // ── Haversine formula ─────────────────────────────────────────────────────

    fun haversineMeters(
        lat1: Double, lng1: Double,
        lat2: Double, lng2: Double
    ): Float {
        val R = 6_371_000.0
        val phi1    = Math.toRadians(lat1)
        val phi2    = Math.toRadians(lat2)
        val dPhi    = Math.toRadians(lat2 - lat1)
        val dLambda = Math.toRadians(lng2 - lng1)
        val a = Math.sin(dPhi / 2).let { it * it } +
                Math.cos(phi1) * Math.cos(phi2) *
                Math.sin(dLambda / 2).let { it * it }
        return (2 * R * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))).toFloat()
    }
}

// ── Alert data class ──────────────────────────────────────────────────────────

/**
 * Emitted by [HazardProximityMonitor] when the user enters a flood hazard zone.
 */
data class HazardAlert(
    val eventId: String,
    val waterDepthCm: Int,
    val passability: String,
    val category: String,
    val hazardLat: Double,
    val hazardLng: Double,
    val distanceMeters: Float
)
