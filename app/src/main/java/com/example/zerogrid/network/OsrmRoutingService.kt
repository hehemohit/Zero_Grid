package com.example.zerogrid.network

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Result of an OSRM route computation.
 */
data class OsrmRouteResult(
    val polylinePoints: List<LatLng>,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val geoJsonString: String?,
    val isFallback: Boolean = false
)

/**
 * Service to execute driving routes via OSRM (Open Source Routing Machine)
 * with robust offline interpolation fallback.
 */
object OsrmRoutingService {

    private const val TAG = "OsrmRoutingService"
    private const val OSRM_BASE_URL = "https://router.project-osrm.org/route/v1/driving"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Calculates driving route geometry from [origin] through intermediate [waypoints]
     * to [destination].
     *
     * Coordinates are passed in OSRM format: lng,lat;lng,lat...
     */
    suspend fun calculateRoute(
        origin: LatLng,
        destination: LatLng,
        waypoints: List<LatLng> = emptyList()
    ): OsrmRouteResult = withContext(Dispatchers.IO) {
        val allPoints = mutableListOf<LatLng>().apply {
            add(origin)
            addAll(waypoints)
            add(destination)
        }

        // Build coordinates query string: "lng,lat;lng,lat;..."
        val coordsParam = allPoints.joinToString(";") {
            String.format(Locale.US, "%.6f,%.6f", it.longitude, it.latitude)
        }

        val url = "$OSRM_BASE_URL/$coordsParam?overview=full&geometries=geojson&steps=false"

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "GridZero-Android-DisasterResponse/1.0")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    val json = JSONObject(body)
                    val code = json.optString("code")
                    if (code.equals("Ok", ignoreCase = true)) {
                        val routes = json.getJSONArray("routes")
                        if (routes.length() > 0) {
                            val route = routes.getJSONObject(0)
                            val distance = route.optDouble("distance", 0.0)
                            val duration = route.optDouble("duration", 0.0)
                            val geometry = route.getJSONObject("geometry")
                            val coordinates = geometry.getJSONArray("coordinates")

                            val polyline = mutableListOf<LatLng>()
                            for (i in 0 until coordinates.length()) {
                                val pt = coordinates.getJSONArray(i)
                                val lng = pt.getDouble(0)
                                val lat = pt.getDouble(1)
                                polyline.add(LatLng(lat, lng))
                            }

                            Log.d(TAG, "OSRM route success: ${polyline.size} points, distance=${distance}m")
                            return@withContext OsrmRouteResult(
                                polylinePoints = polyline,
                                distanceMeters = distance,
                                durationSeconds = duration,
                                geoJsonString = geometry.toString(),
                                isFallback = false
                            )
                        }
                    }
                }
            }
            Log.w(TAG, "OSRM returned unsuccessful response: ${response.code}")
        } catch (e: Exception) {
            Log.w(TAG, "OSRM network request failed, falling back to offline interpolation: ${e.message}")
        }

        // Fallback: Offline piecewise interpolation
        buildOfflineInterpolatedRoute(allPoints)
    }

    /**
     * Fallback route generator when offline or OSRM unavailable.
     * Interpolates smooth points between the route checkpoints.
     */
    private fun buildOfflineInterpolatedRoute(checkpoints: List<LatLng>): OsrmRouteResult {
        val polyline = mutableListOf<LatLng>()
        var totalDist = 0.0

        for (i in 0 until checkpoints.size - 1) {
            val start = checkpoints[i]
            val end = checkpoints[i + 1]
            val segDist = haversineMeters(start.latitude, start.longitude, end.latitude, end.longitude)
            totalDist += segDist

            // Interpolate roughly every 150 meters
            val steps = (segDist / 150.0).toInt().coerceAtLeast(3)
            for (s in 0..steps) {
                val frac = s.toDouble() / steps
                val lat = start.latitude + (end.latitude - start.latitude) * frac
                val lng = start.longitude + (end.longitude - start.longitude) * frac
                polyline.add(LatLng(lat, lng))
            }
        }

        val estimatedDuration = (totalDist / 8.33) // ~30 km/h in seconds
        return OsrmRouteResult(
            polylinePoints = polyline,
            distanceMeters = totalDist,
            durationSeconds = estimatedDuration,
            geoJsonString = null,
            isFallback = true
        )
    }

    private fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371000.0
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val dPhi = Math.toRadians(lat2 - lat1)
        val dLambda = Math.toRadians(lng2 - lng1)
        val a = Math.sin(dPhi / 2).let { it * it } +
                Math.cos(phi1) * Math.cos(phi2) *
                Math.sin(dLambda / 2).let { it * it }
        return 2 * r * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }
}
