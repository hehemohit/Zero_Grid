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
        openUri(context, Uri.parse(url))
    }

    /**
     * Launches Google Maps enforcing all calculated evasion waypoints from a [RouteSafetyReport].
     * Prevents Google Maps from ignoring flooded corridors and recomputing its own unsafe trajectory.
     */
    fun launchWithReport(
        context: Context,
        report: com.example.zerogrid.emergency.RouteSafetyReport,
        travelMode: String = "driving"
    ) {
        val originLat = report.origin.latitude
        val originLng = report.origin.longitude
        val destLat = report.destination.latitude
        val destLng = report.destination.longitude

        // Prioritize explicit detour evasion checkpoints
        val waypoints = if (report.waypoints.isNotEmpty()) {
            report.waypoints.take(MAX_WAYPOINTS).map { Pair(it.latitude, it.longitude) }
        } else if (report.geoJsonString != null) {
            extractWaypoints(report.geoJsonString)
        } else if (report.routePoints.size > 2) {
            // Sample intermediate checkpoints along the safe corridor polyline
            val intermediates = report.routePoints.subList(1, report.routePoints.size - 1)
            if (intermediates.size <= MAX_WAYPOINTS) {
                intermediates.map { Pair(it.latitude, it.longitude) }
            } else {
                val step = intermediates.size.toFloat() / MAX_WAYPOINTS
                (0 until MAX_WAYPOINTS).map { i ->
                    val pt = intermediates[(i * step).toInt()]
                    Pair(pt.latitude, pt.longitude)
                }
            }
        } else {
            emptyList()
        }

        val url = buildUrl(originLat, originLng, destLat, destLng, waypoints, travelMode)
        Log.d(TAG, "Opening Google Maps with ${waypoints.size} forced waypoints: $url")
        openUri(context, Uri.parse(url))
    }

    private fun openUri(context: Context, uri: Uri) {
        val mapsIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(MAPS_PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(mapsIntent)
        } catch (e: Exception) {
            // Fallback: system browser
            val browserIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(browserIntent)
            } catch (err: Exception) {
                Log.e(TAG, "Failed to launch maps intent: ${err.message}")
            }
        }
    }

    // ── URL builder ───────────────────────────────────────────────────────────

    private fun buildUrl(
        originLat: Double, originLng: Double,
        destLat: Double,   destLng: Double,
        waypoints: List<Pair<Double, Double>>,
        travelMode: String = "driving"
    ): String {
        val sb = StringBuilder()
        sb.append("https://www.google.com/maps/dir/?api=1")
        sb.append("&origin=$originLat,$originLng")
        sb.append("&destination=$destLat,$destLng")
        sb.append("&travelmode=$travelMode")

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
