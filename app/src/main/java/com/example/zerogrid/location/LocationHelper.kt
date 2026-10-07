package com.example.zerogrid.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Offline-first GPS location helper.
 *
 * Uses Android's native [LocationManager] — no Google Play Services required.
 * Works completely off-grid in disaster zones and remote areas.
 *
 * Priority chain:
 * 1. Last known GPS fix (instant, no power cost).
 * 2. Last known network location (fallback).
 * 3. One-shot location update from GPS or network (max [TIMEOUT_MS] wait).
 */
object LocationHelper {

    private const val TAG = "LocationHelper"
    private const val TIMEOUT_MS = 5_000L  // Max 5 s wait before giving up

    data class LocationResult(
        val lat: Double,
        val lng: Double,
        val accuracy: Float?,          // meters
        val provider: String
    )

    /**
     * Returns the best available location or null if none can be obtained within [TIMEOUT_MS].
     * Must be called from a coroutine (suspending).
     */
    suspend fun getCurrentLocation(context: Context): LocationResult? {
        if (!hasPermission(context)) {
            Log.w(TAG, "Location permission not granted – cannot fetch GPS coordinates.")
            return null
        }

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        // 1. Try cached last-known locations first — instant, zero power cost
        val cached = getBestCachedLocation(lm)
        if (cached != null && isRecent(cached)) {
            Log.d(TAG, "Using cached location: ${cached.latitude}, ${cached.longitude}")
            return cached.toResult()
        }

        // 2. Request a fresh one-shot update — wait up to TIMEOUT_MS
        val fresh = withTimeoutOrNull(TIMEOUT_MS) {
            requestFreshLocation(context, lm)
        }

        if (fresh != null) {
            Log.d(TAG, "Fresh location acquired: ${fresh.latitude}, ${fresh.longitude} (${fresh.provider})")
            return fresh.toResult()
        }

        // 3. Fall back to stale cached location rather than returning null
        if (cached != null) {
            Log.w(TAG, "Returning stale cached location as fallback.")
            return cached.toResult()
        }

        Log.e(TAG, "Could not obtain any location within timeout.")
        return null
    }

    /**
     * Synchronously returns the best cached last-known location without coroutine suspension.
     * Ideal for non-blocking proximity calculations and immediate telemetry.
     */
    fun getLastKnownLocation(context: Context): LocationResult? {
        if (!hasPermission(context)) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        return getBestCachedLocation(lm)?.toResult()
    }

    // ─────────────────────────────────────────────────────────────────────────

    private fun hasPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun getBestCachedLocation(lm: LocationManager): Location? {
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        return providers
            .mapNotNull { provider ->
                runCatching { lm.getLastKnownLocation(provider) }.getOrNull()
            }
            .maxByOrNull { it.time } // pick most recent
    }

    /** Location is "recent" if it's within the last 2 minutes */
    private fun isRecent(location: Location): Boolean =
        System.currentTimeMillis() - location.time < 2 * 60 * 1000L

    private suspend fun requestFreshLocation(
        context: Context,
        lm: LocationManager
    ): Location? = suspendCancellableCoroutine { cont ->
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                lm.removeUpdates(this)
                if (cont.isActive) cont.resume(location)
            }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        // Try GPS first, then network as fallback
        val enabledProviders = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { lm.isProviderEnabled(it) }

        if (enabledProviders.isEmpty()) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }

        // Register listener on all available enabled providers — first result wins
        enabledProviders.forEach { provider ->
            runCatching {
                lm.requestLocationUpdates(
                    provider,
                    0L,   // minTime
                    0f,   // minDistance
                    listener,
                    Looper.getMainLooper()
                )
            }
        }

        cont.invokeOnCancellation {
            lm.removeUpdates(listener)
        }
    }

    private fun Location.toResult() = LocationResult(
        lat = latitude,
        lng = longitude,
        accuracy = if (hasAccuracy()) accuracy else null,
        provider = provider ?: "unknown"
    )
}
