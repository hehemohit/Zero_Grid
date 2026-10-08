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
    val lng: Double
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
     * 2. Android system Geocoder
     * 3. OpenStreetMap Nominatim fallback
     */
    suspend fun searchLocations(context: Context, query: String): List<LocationSearchResult> =
        withContext(Dispatchers.IO) {
            val trimmed = query.trim()
            if (trimmed.isBlank()) return@withContext emptyList()

            val results = mutableListOf<LocationSearchResult>()

            // 1. Direct lat,lng coordinates
            val parts = trimmed.split(",")
            if (parts.size == 2) {
                val lat = parts[0].trim().toDoubleOrNull()
                val lng = parts[1].trim().toDoubleOrNull()
                if (lat != null && lng != null && lat in -90.0..90.0 && lng in -180.0..180.0) {
                    results.add(
                        LocationSearchResult(
                            title = "GPS Coordinates",
                            subtitle = String.format(Locale.US, "%.5f, %.5f", lat, lng),
                            lat = lat,
                            lng = lng
                        )
                    )
                    return@withContext results
                }
            }

            // 2. Android Geocoder
            try {
                @Suppress("DEPRECATION")
                val geoResults = Geocoder(context).getFromLocationName(trimmed, 5)
                if (!geoResults.isNullOrEmpty()) {
                    for (g in geoResults) {
                        val title = g.featureName ?: g.locality ?: g.subAdminArea ?: trimmed
                        val subtitle = g.getAddressLine(0) ?: "$title, ${g.countryName ?: ""}"
                        results.add(
                            LocationSearchResult(
                                title = title,
                                subtitle = subtitle,
                                lat = g.latitude,
                                lng = g.longitude
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
                    val url = "https://nominatim.openstreetmap.org/search?q=$encoded&format=json&limit=5&addressdetails=1"
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
                                    results.add(
                                        LocationSearchResult(
                                            title = name,
                                            subtitle = displayName,
                                            lat = lat,
                                            lng = lon
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

            results
        }
}
