package com.example.zerogrid.emergency

import android.util.Log
import com.example.zerogrid.location.CachedHazard
import com.example.zerogrid.location.HazardCacheManager
import com.example.zerogrid.network.OsrmRoutingService
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.*

/**
 * A hazard that was successfully circumvented using GridZero's safe route.
 */
data class AvoidedHazardItem(
    val eventId: String,
    val title: String,
    val iconEmoji: String,
    val category: String,
    val originalDistanceMeters: Float,
    val evasionDetail: String,
    val lat: Double,
    val lng: Double
)

/**
 * A residual hazard or condition that the user must still navigate through
 * even on the safest available route.
 */
data class UnavoidableHazardItem(
    val eventId: String,
    val title: String,
    val iconEmoji: String,
    val category: String,
    val proximityMeters: Float,
    val advice: String,
    val lat: Double,
    val lng: Double
)

/**
 * Complete assessment produced by the Safety Agent.
 */
data class RouteSafetyReport(
    val origin: LatLng,
    val destination: LatLng,
    val routePoints: List<LatLng>,
    val waypoints: List<LatLng>,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val avoidedHazards: List<AvoidedHazardItem>,
    val unavoidableHazards: List<UnavoidableHazardItem>,
    val agentSummary: String,
    val geoJsonString: String?
)

/**
 * Pluggable contract for Route Safety Agents.
 * Will support conversational / LLM agents in future phases.
 */
interface RouteSafetyAgent {
    suspend fun executeSafeRoute(
        origin: LatLng,
        destination: LatLng,
        cachedHazards: List<CachedHazard>
    ): RouteSafetyReport
}

/**
 * Deterministic / Rule-based Safety Agent.
 * Runs geometric corridor intersection, computes evasive waypoints around
 * infected hazard clusters, executes OSRM routing, and classifies risks into
 * Avoided vs Unavoidable lists.
 */
object RuleBasedRouteSafetyAgent : RouteSafetyAgent {

    private const val TAG = "RuleBasedRouteAgent"
    private const val DIRECT_CORRIDOR_BUFFER_M = 160.0 // buffer around direct line
    private const val SAFE_PROXIMITY_THRESHOLD_M = 180.0 // threshold on final route

    override suspend fun executeSafeRoute(
        origin: LatLng,
        destination: LatLng,
        cachedHazards: List<CachedHazard>
    ): RouteSafetyReport = withContext(Dispatchers.Default) {

        // 1. Identify hazards conflicting with the direct corridor
        val conflictingHazards = mutableListOf<CachedHazard>()
        for (h in cachedHazards) {
            val distToDirect = distancePointToSegmentMeters(
                h.lat, h.lng,
                origin.latitude, origin.longitude,
                destination.latitude, destination.longitude
            )
            if (distToDirect <= DIRECT_CORRIDOR_BUFFER_M) {
                conflictingHazards.add(h)
            }
        }

        // 2. Generate evasive waypoints around conflicting hazards (max 3 for OSRM stability)
        val evasionWaypoints = mutableListOf<LatLng>()
        val bearingDirect = computeBearing(origin.latitude, origin.longitude, destination.latitude, destination.longitude)

        val prioritizedConflicts = conflictingHazards
            .sortedByDescending { hazardSeverityScore(it) }
            .take(3)

        for ((idx, conflict) in prioritizedConflicts.withIndex()) {
            // Alternate detour offset direction (left / right of direct bearing)
            val offsetAngle = if (idx % 2 == 0) bearingDirect + 90.0 else bearingDirect - 90.0
            val offsetDistMeters = 350.0 // ~350m detour around hazard
            val detourPoint = computeDestinationPoint(conflict.lat, conflict.lng, offsetDistMeters, offsetAngle)
            evasionWaypoints.add(detourPoint)
        }

        // 3. Execute OSRM routing with the calculated waypoints
        val osrmResult = OsrmRoutingService.calculateRoute(
            origin = origin,
            destination = destination,
            waypoints = evasionWaypoints
        )

        val finalPolyline = osrmResult.polylinePoints

        // 4. Classify hazards: Avoided vs Unavoidable
        val avoidedList = mutableListOf<AvoidedHazardItem>()
        val unavoidableList = mutableListOf<UnavoidableHazardItem>()

        for (h in cachedHazards) {
            val (title, icon) = getHazardMetadata(h)
            val distToFinalRoute = minDistancePointToPolylineMeters(h.lat, h.lng, finalPolyline)

            // Was it originally in the conflict zone?
            val wasInConflict = conflictingHazards.any { it.eventId == h.eventId }

            if (wasInConflict && distToFinalRoute > SAFE_PROXIMITY_THRESHOLD_M) {
                avoidedList.add(
                    AvoidedHazardItem(
                        eventId = h.eventId,
                        title = title,
                        iconEmoji = icon,
                        category = h.category,
                        originalDistanceMeters = distToFinalRoute.toFloat(),
                        evasionDetail = "Circumvented by +${distToFinalRoute.toInt()}m via detour corridor",
                        lat = h.lat,
                        lng = h.lng
                    )
                )
            } else if (distToFinalRoute <= SAFE_PROXIMITY_THRESHOLD_M) {
                val advice = generateSurvivalAdvice(h, distToFinalRoute)
                unavoidableList.add(
                    UnavoidableHazardItem(
                        eventId = h.eventId,
                        title = title,
                        iconEmoji = icon,
                        category = h.category,
                        proximityMeters = distToFinalRoute.toFloat(),
                        advice = advice,
                        lat = h.lat,
                        lng = h.lng
                    )
                )
            }
        }

        // 5. Synthesize Agent Summary
        val summary = buildAgentSummary(
            avoidedCount = avoidedList.size,
            unavoidableCount = unavoidableList.size,
            distanceKm = osrmResult.distanceMeters / 1000.0,
            hasEvasion = evasionWaypoints.isNotEmpty()
        )

        Log.d(TAG, "Route verified: ${avoidedList.size} avoided, ${unavoidableList.size} unavoidable")

        RouteSafetyReport(
            origin = origin,
            destination = destination,
            routePoints = finalPolyline,
            waypoints = evasionWaypoints,
            distanceMeters = osrmResult.distanceMeters,
            durationSeconds = osrmResult.durationSeconds,
            avoidedHazards = avoidedList,
            unavoidableHazards = unavoidableList,
            agentSummary = summary,
            geoJsonString = osrmResult.geoJsonString
        )
    }

    // ── Helper math & heuristics ─────────────────────────────────────────────

    private fun hazardSeverityScore(h: CachedHazard): Int {
        return when (h.category.uppercase()) {
            "LIVE_WIRE", "FALLEN_POWERLINE" -> 100
            "STRUCTURAL_COLLAPSE" -> 90
            "SUBMERGED_UNDERPASS" -> 85
            "WATERLOGGING" -> if (h.waterDepthCm >= 35) 80 else 50
            "ROAD_BLOCKAGE", "FALLEN_TREE" -> 70
            else -> 40
        }
    }

    private fun getHazardMetadata(h: CachedHazard): Pair<String, String> {
        return when (h.category.uppercase()) {
            "FALLEN_POWERLINE", "LIVE_WIRE" -> Pair("Live Power Cable Drop", "⚡")
            "POWER_OUTAGE" -> Pair("Blackout Zone", "🔌")
            "FALLEN_TREE" -> Pair("Fallen Tree Obstruction", "🌲")
            "STRUCTURAL_COLLAPSE" -> Pair("Debris & Structural Risk", "🏚")
            "ROAD_BLOCKAGE" -> Pair("Road Blockage", "🛑")
            "SUBMERGED_UNDERPASS" -> Pair("Submerged Underpass", "🌊")
            "WATERLOGGING", "DRAINAGE_OVERFLOW" -> {
                val depth = if (h.waterDepthCm > 0) " (${h.waterDepthCm} cm)" else ""
                Pair("Waterlogging$depth", "🌊")
            }
            else -> Pair("Incident Zone", "⚠️")
        }
    }

    private fun generateSurvivalAdvice(h: CachedHazard, distMeters: Double): String {
        return when (h.category.uppercase()) {
            "FALLEN_POWERLINE", "LIVE_WIRE" ->
                "⚠️ High voltage risk within ${distMeters.toInt()}m. Do NOT step out of vehicle."
            "SUBMERGED_UNDERPASS", "WATERLOGGING", "DRAINAGE_OVERFLOW" -> {
                if (h.waterDepthCm >= 30) {
                    "⚠️ ${h.waterDepthCm}cm water on approach. Maintain continuous throttle; avoid braking."
                } else {
                    "⚠️ Moderate pooling within ${distMeters.toInt()}m. Keep low speed under 20 km/h."
                }
            }
            "FALLEN_TREE", "ROAD_BLOCKAGE" ->
                "⚠️ Lane restriction within ${distMeters.toInt()}m. Expect single-lane passage."
            else ->
                "⚠️ Active hazard zone within ${distMeters.toInt()}m. Drive with caution."
        }
    }

    private fun buildAgentSummary(
        avoidedCount: Int,
        unavoidableCount: Int,
        distanceKm: Double,
        hasEvasion: Boolean
    ): String {
        val distStr = String.format(Locale.US, "%.1f km", distanceKm)
        return when {
            avoidedCount > 0 && unavoidableCount == 0 ->
                "Route Verified: GridZero successfully circumvented $avoidedCount high-risk hazard(s) over $distStr with 0 residual obstacles."
            avoidedCount > 0 && unavoidableCount > 0 ->
                "Route Verified: Detoured around $avoidedCount hazard(s). However, $unavoidableCount localized risk(s) remain unavoidable near route path."
            avoidedCount == 0 && unavoidableCount > 0 ->
                "Caution: Direct path selected. $unavoidableCount hazard(s) reported along this corridor."
            else ->
                "Route Clear: 0 active hazards detected along the $distStr corridor."
        }
    }

    // ── Geodesic spatial helpers ─────────────────────────────────────────────

    private fun minDistancePointToPolylineMeters(
        pointLat: Double, pointLng: Double,
        polyline: List<LatLng>
    ): Double {
        if (polyline.isEmpty()) return Double.MAX_VALUE
        if (polyline.size == 1) return HazardCacheManager.haversineMeters(pointLat, pointLng, polyline[0].latitude, polyline[0].longitude).toDouble()

        var minDist = Double.MAX_VALUE
        for (i in 0 until polyline.size - 1) {
            val dist = distancePointToSegmentMeters(
                pointLat, pointLng,
                polyline[i].latitude, polyline[i].longitude,
                polyline[i + 1].latitude, polyline[i + 1].longitude
            )
            if (dist < minDist) minDist = dist
        }
        return minDist
    }

    private fun distancePointToSegmentMeters(
        pLat: Double, pLng: Double,
        aLat: Double, aLng: Double,
        bLat: Double, bLng: Double
    ): Double {
        val distAB = HazardCacheManager.haversineMeters(aLat, aLng, bLat, bLng).toDouble()
        if (distAB < 1.0) return HazardCacheManager.haversineMeters(pLat, pLng, aLat, aLng).toDouble()

        // Vector projection in flat plane approximation for short distances
        val cosLat = cos(Math.toRadians((aLat + bLat) / 2.0))
        val ax = aLng * cosLat
        val ay = aLat
        val bx = bLng * cosLat
        val by = bLat
        val px = pLng * cosLat
        val py = pLat

        val dx = bx - ax
        val dy = by - ay
        val t = (((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy)).coerceIn(0.0, 1.0)

        val closestLat = aLat + t * (bLat - aLat)
        val closestLng = aLng + t * (bLng - aLng)

        return HazardCacheManager.haversineMeters(pLat, pLng, closestLat, closestLng).toDouble()
    }

    private fun computeBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        val theta = atan2(y, x)
        return (Math.toDegrees(theta) + 360.0) % 360.0
    }

    private fun computeDestinationPoint(
        lat: Double, lon: Double,
        distanceMeters: Double, bearingDegrees: Double
    ): LatLng {
        val r = 6371000.0
        val d = distanceMeters / r
        val brng = Math.toRadians(bearingDegrees)
        val lat1 = Math.toRadians(lat)
        val lon1 = Math.toRadians(lon)

        val lat2 = asin(sin(lat1) * cos(d) + cos(lat1) * sin(d) * cos(brng))
        val lon2 = lon1 + atan2(sin(brng) * sin(d) * cos(lat1), cos(d) - sin(lat1) * sin(lat2))

        return LatLng(Math.toDegrees(lat2), Math.toDegrees(lon2))
    }
}
