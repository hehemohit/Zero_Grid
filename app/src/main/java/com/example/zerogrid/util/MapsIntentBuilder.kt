package com.example.zerogrid.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import org.json.JSONObject

/**
 * Builds a Google Maps deep-link URL from an OSRM GeoJSON route,
 * extracting intermediate coordinates as waypoints (max 8 per Maps limit).
 *
 * Falls back to opening in the browser if Google Maps is not installed.
 */
object MapsIntentBuilder {

    private const val TAG = "MapsIntentBuilder"
    private const val MAPS_PACKAGE = "com.google.android.apps.maps"
    private const val MAX_WAYPOINTS = 8

    /**
     * Launches Google Maps with a pre-calculated safe route.
     *
     * @param context       Application/Activity context.
     * @param originLat     User's current latitude.
     * @param originLng     User's current longitude.
     * @param destLat       Destination latitude.
     * @param destLng       Destination longitude.
     * @param geoJson       OSRM LineString GeoJSON string (from /api/routes/detour).
     *                      Pass null to open a direct origin→destination route.
     */
    fun launch(
        context: Context,
        originLat: Double,
        originLng: Double,
        destLat: Double,
        destLng: Double,
        geoJson: String? = null
    ) {
        val waypoints = geoJson?.let { extractWaypoints(it) } ?: emptyList()
        val url = buildUrl(originLat, originLng, destLat, destLng, waypoints)
        Log.d(TAG, "Opening Maps: $url")

        val uri = Uri.parse(url)

        // Prefer Google Maps app
        val mapsIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(MAPS_PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (mapsIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(mapsIntent)
        } else {
            // Fallback: browser
            val browserIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
        }
    }

    // ── URL builder ───────────────────────────────────────────────────────────

    private fun buildUrl(
        originLat: Double, originLng: Double,
        destLat: Double,   destLng: Double,
        waypoints: List<Pair<Double, Double>>
    ): String {
        val sb = StringBuilder()
        sb.append("https://www.google.com/maps/dir/?api=1")
        sb.append("&origin=$originLat,$originLng")
        sb.append("&destination=$destLat,$destLng")
        sb.append("&travelmode=driving")

        if (waypoints.isNotEmpty()) {
            val waypointStr = waypoints.joinToString("|") { (lat, lng) -> "$lat,$lng" }
            sb.append("&waypoints=${Uri.encode(waypointStr)}")
        }
        return sb.toString()
    }

    // ── GeoJSON waypoint extractor ────────────────────────────────────────────

    /**
     * Parses a GeoJSON LineString and samples up to [MAX_WAYPOINTS] evenly-spaced
     * intermediate points (first and last coordinate are excluded — they are
     * origin and destination already).
     */
    private fun extractWaypoints(geoJson: String): List<Pair<Double, Double>> {
        return try {
            val json = JSONObject(geoJson)
            val coords = json.getJSONArray("coordinates")
            val total = coords.length()

            if (total <= 2) return emptyList()

            // Intermediate points only (exclude index 0 and last)
            val intermediates = (1 until total - 1).map { i ->
                val point = coords.getJSONArray(i)
                Pair(point.getDouble(1), point.getDouble(0)) // [lng, lat] → (lat, lng)
            }

            if (intermediates.size <= MAX_WAYPOINTS) {
                intermediates
            } else {
                // Sample evenly
                val step = intermediates.size.toFloat() / MAX_WAYPOINTS
                (0 until MAX_WAYPOINTS).map { i ->
                    intermediates[(i * step).toInt()]
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "GeoJSON parse failed: ${e.message}")
            emptyList()
        }
    }
}
