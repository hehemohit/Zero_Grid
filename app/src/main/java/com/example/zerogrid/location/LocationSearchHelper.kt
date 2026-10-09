package com.example.zerogrid.location

import android.content.Context
import android.location.Geocoder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit

data class LocationSearchResult(
    val title: String,
    val subtitle: String,
    val lat: Double,
    val lng: Double,
    val distanceMeters: Float? = null
)

object LocationSearchHelper {

    private const val TAG = "LocationSearchHelper"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    /**
     * Resolves user query to geographic coordinates via:
     * 1. Direct "lat,lng" string parse
     * 2. Android system Geocoder (biased to user's 25km radius when coordinates supplied)
     * 3. OpenStreetMap Nominatim fallback (biased to user's bounding box)
     *
     * Automatically computes distance to user and sorts closest places first.
     */
    suspend fun searchLocations(
        context: Context,
        query: String,
        userLat: Double? = null,
        userLng: Double? = null
    ): List<LocationSearchResult> =
        withContext(Dispatchers.IO) {
            val trimmed = query.trim()
            if (trimmed.isBlank()) return@withContext emptyList()

            val results = mutableListOf<LocationSearchResult>()

            // 0. Match any known local cached hazards / landmarks
            try {
                if (userLat != null && userLng != null) {
                    val nearbyHazards = HazardCacheManager.getNearbyHazards(userLat, userLng, 25_000f)
                    val matching = nearbyHazards.filter {
                        it.title.contains(trimmed, ignoreCase = true) || it.hazard.category.contains(trimmed, ignoreCase = true)
                    }
                    for (nh in matching) {
                        results.add(
                            LocationSearchResult(
                                title = nh.title,
                                subtitle = "Active Hazard Sector (${nh.riskSeverity})",
                                lat = nh.hazard.lat,
                                lng = nh.hazard.lng,
                                distanceMeters = nh.distanceMeters
                            )
                        )
                    }
                }
            } catch (_: Exception) {}

            // 1. Direct lat,lng coordinates
            val parts = trimmed.split(",")
            if (parts.size == 2) {
                val lat = parts[0].trim().toDoubleOrNull()
                val lng = parts[1].trim().toDoubleOrNull()
                if (lat != null && lng != null && lat in -90.0..90.0 && lng in -180.0..180.0) {
                    val dist = if (userLat != null && userLng != null) {
                        HazardCacheManager.haversineMeters(userLat, userLng, lat, lng)
                    } else null

                    results.add(
                        LocationSearchResult(
                            title = "GPS Coordinates",
                            subtitle = String.format(Locale.US, "%.5f, %.5f", lat, lng),
                            lat = lat,
                            lng = lng,
                            distanceMeters = dist
                        )
                    )
                    return@withContext results
                }
            }

            // 2. Android Geocoder (try proximity-biased first, then global)
            try {
                @Suppress("DEPRECATION")
                val geocoder = Geocoder(context)
                var geoResults = if (userLat != null && userLng != null) {
                    val delta = 0.3 // ~30 km radius
                    try {
                        geocoder.getFromLocationName(
                            trimmed,
                            8,
                            userLat - delta,
                            userLng - delta,
                            userLat + delta,
                            userLng + delta
                        )
                    } catch (_: Exception) {
                        null
                    }
                } else null

                if (geoResults.isNullOrEmpty()) {
                    @Suppress("DEPRECATION")
                    geoResults = geocoder.getFromLocationName(trimmed, 8)
                }

                if (!geoResults.isNullOrEmpty()) {
                    for (g in geoResults) {
                        val title = g.featureName ?: g.locality ?: g.subAdminArea ?: trimmed
                        val subtitle = g.getAddressLine(0) ?: "$title, ${g.countryName ?: ""}"
                        val dist = if (userLat != null && userLng != null) {
                            HazardCacheManager.haversineMeters(userLat, userLng, g.latitude, g.longitude)
                        } else null

                        results.add(
                            LocationSearchResult(
                                title = title,
                                subtitle = subtitle,
                                lat = g.latitude,
                                lng = g.longitude,
                                distanceMeters = dist
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Native Geocoder error: ${e.message}")
            }

            // 3. Fallback: OSM Nominatim
            if (results.isEmpty()) {
                try {
                    val encoded = URLEncoder.encode(trimmed, "UTF-8")
                    val viewboxParam = if (userLat != null && userLng != null) {
                        val delta = 0.35
                        "&viewbox=${userLng - delta},${userLat + delta},${userLng + delta},${userLat - delta}&bounded=0"
                    } else ""
                    val url = "https://nominatim.openstreetmap.org/search?q=$encoded&format=json&limit=8&addressdetails=1$viewboxParam"
                    val request = Request.Builder()
                        .url(url)
                        .header("User-Agent", "ZeroGrid-Android-Disaster-Mesh/1.0")
                        .build()

                    httpClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val body = response.body?.string().orEmpty()
                            val jsonArray = JSONArray(body)
                            for (i in 0 until jsonArray.length()) {
                                val obj = jsonArray.getJSONObject(i)
                                val lat = obj.optDouble("lat", Double.NaN)
                                val lon = obj.optDouble("lon", Double.NaN)
                                val name = obj.optString("name", "").ifBlank {
                                    obj.optString("display_name", "").split(",").firstOrNull() ?: trimmed
                                }
                                val displayName = obj.optString("display_name", "")
                                if (!lat.isNaN() && !lon.isNaN()) {
                                    val dist = if (userLat != null && userLng != null) {
                                        HazardCacheManager.haversineMeters(userLat, userLng, lat, lon)
                                    } else null

                                    results.add(
                                        LocationSearchResult(
                                            title = name,
                                            subtitle = displayName,
                                            lat = lat,
                                            lng = lon,
                                            distanceMeters = dist
                                        )
                                    )
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Nominatim error: ${e.message}")
                }
            }

            // Deduplicate close results (<50m or identical title)
            val unique = mutableListOf<LocationSearchResult>()
            for (r in results) {
                val isDup = unique.any { existing ->
                    (existing.title.equals(r.title, ignoreCase = true) &&
                     Math.abs(existing.lat - r.lat) < 0.001 &&
                     Math.abs(existing.lng - r.lng) < 0.001) ||
                    (Math.abs(existing.lat - r.lat) < 0.0004 && Math.abs(existing.lng - r.lng) < 0.0004)
                }
                if (!isDup) {
                    unique.add(r)
                }
            }

            // Sort closest to user first if distance is known
            unique.sortedBy { it.distanceMeters ?: Float.MAX_VALUE }
        }
}
