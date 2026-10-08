package com.example.zerogrid.location

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.zerogrid.network.ConnectivityChecker
import com.example.zerogrid.network.RetrofitInstance
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import java.util.concurrent.atomic.AtomicReference

// ---------------------------------------------------------------------------
// DataStore instance (one per process)
// ---------------------------------------------------------------------------
private val Context.hazardDataStore by preferencesDataStore(name = "hazard_cache")

/**
 * Singleton that owns the in-memory + persistent hazard cache.
 *
 * FETCH: called when online (connectivity restored, every 5 min, or user moves >1 km).
 * READ:  zero-cost memory read called every 60 s by [HazardProximityMonitor].
 *
 * Only flood-type SOS events (WATERLOGGING, SUBMERGED_UNDERPASS, DRAINAGE_OVERFLOW)
 * with status ACTIVE are retained.
 */
object HazardCacheManager {

    private const val TAG = "HazardCacheManager"
    private const val CACHE_TTL_MS = 5 * 60 * 1000L   // 5 minutes
    private const val LOCATION_DRIFT_M = 1_000f         // 1 km

    private val CACHE_KEY = stringPreferencesKey("hazard_json")
    private val gson = Gson()

    val MONITORED_CATEGORIES = setOf(
        "WATERLOGGING",
        "SUBMERGED_UNDERPASS",
        "DRAINAGE_OVERFLOW",
        "FALLEN_POWERLINE",
        "POWER_OUTAGE",
        "LIVE_WIRE",
        "FALLEN_TREE",
        "STRUCTURAL_COLLAPSE",
        "ROAD_BLOCKAGE"
    )

    // ── In-memory store (AtomicReference → thread-safe, zero-lock read) ──────
    private val memCache = AtomicReference<List<CachedHazard>>(emptyList())

    private var lastFetchMs: Long = 0L
    private var lastFetchLat: Double = 0.0
    private var lastFetchLng: Double = 0.0

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Returns the in-memory hazard list.
     * ZERO network call — suitable for the 60 s tick.
     */
    fun getCachedHazards(): List<CachedHazard> = memCache.get()

    /** Age of the in-memory cache in milliseconds. */
    fun cacheAgeMs(): Long = System.currentTimeMillis() - lastFetchMs

    /**
     * True when the user has drifted more than [LOCATION_DRIFT_M] from the
     * last fetch origin, meaning the 10 km geo-window may have shifted.
     */
    fun shouldRefreshForLocation(newLat: Double, newLng: Double): Boolean {
        val drift = haversineMeters(lastFetchLat, lastFetchLng, newLat, newLng)
        return drift > LOCATION_DRIFT_M
    }

    /**
     * Loads the DataStore cache into memory at service startup.
     * Call once before starting [HazardProximityMonitor].
     */
    suspend fun loadFromDisk(context: Context) {
        try {
            val prefs = context.hazardDataStore.data.first()
            val json = prefs[CACHE_KEY] ?: return
            val type = object : TypeToken<List<CachedHazard>>() {}.type
            val loaded: List<CachedHazard> = gson.fromJson(json, type) ?: emptyList()
            memCache.set(loaded)
            Log.d(TAG, "Loaded ${loaded.size} hazards from disk cache.")
        } catch (e: Exception) {
            Log.w(TAG, "Could not load disk cache: ${e.message}")
        }
    }

    /**
     * Fetches hazard SOS events within 10 km of [userLat],[userLng] from the backend
     * (only when internet is available).  Updates in-memory and DataStore caches.
     */
    suspend fun refreshIfOnline(context: Context, userLat: Double, userLng: Double) {
        if (!ConnectivityChecker(context).isInternetAvailable()) {
            Log.d(TAG, "Offline — skipping hazard cache refresh.")
            return
        }
        try {
            Log.d(TAG, "Fetching hazards within 10 km of ($userLat, $userLng)…")
            val response = RetrofitInstance.sosApi.getActiveSos(
                lat = userLat,
                lng = userLng,
                radiusKm = 10.0
            )
            if (!response.isSuccessful) {
                Log.w(TAG, "Hazard fetch failed: HTTP ${response.code()}")
                return
            }
            val body = response.body() ?: return
            val filtered = body.events
                .filter { it.category in MONITORED_CATEGORIES || (it.waterDepthCm ?: 0) > 0 }
                .mapNotNull { dto ->
                    val coords = dto.location?.coordinates ?: return@mapNotNull null
                    if (coords.size < 2) return@mapNotNull null
                    CachedHazard(
                        eventId      = dto.id,
                        lat          = coords[1],   // GeoJSON: [lng, lat]
                        lng          = coords[0],
                        waterDepthCm = dto.waterDepthCm ?: 0,
                        passability  = dto.passability ?: "UNKNOWN",
                        category     = dto.category,
                        fetchedAtMs  = System.currentTimeMillis()
                    )
                }

            memCache.set(filtered)
            lastFetchMs  = System.currentTimeMillis()
            lastFetchLat = userLat
            lastFetchLng = userLng

            Log.d(TAG, "Cache refreshed: ${filtered.size} hazards in 10 km radius.")

            // Persist to DataStore for offline resilience
            val json = gson.toJson(filtered)
            context.hazardDataStore.edit { prefs -> prefs[CACHE_KEY] = json }

        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing hazard cache: ${e.message}", e)
        }
    }

    /** True if the cache is old enough to warrant a background refresh. */
    fun isStaleTtl(): Boolean = cacheAgeMs() > CACHE_TTL_MS

    /**
     * Returns cached hazards within [maxRadiusMeters] of ([userLat], [userLng]),
     * sorted closest-first with hazard title, emoji icon, and risk level.
     */
    fun getNearbyHazards(
        userLat: Double,
        userLng: Double,
        maxRadiusMeters: Float = 10_000f
    ): List<NearbyHazardInfo> {
        val hazards = getCachedHazards()
        return hazards.mapNotNull { h ->
            val dist = haversineMeters(userLat, userLng, h.lat, h.lng)
            if (dist <= maxRadiusMeters) {
                val (title, icon, severity) = categorizeHazard(h)
                NearbyHazardInfo(
                    hazard = h,
                    distanceMeters = dist,
                    title = title,
                    iconEmoji = icon,
                    riskSeverity = severity
                )
            } else null
        }.sortedBy { it.distanceMeters }
    }

    private fun categorizeHazard(h: CachedHazard): Triple<String, String, String> {
        return when (h.category.uppercase()) {
            "FALLEN_POWERLINE", "LIVE_WIRE" -> Triple("Live Power Cable Drop", "⚡", "CRITICAL")
            "POWER_OUTAGE" -> Triple("Grid Blackout Zone", "🔌", "MODERATE")
            "FALLEN_TREE" -> Triple("Fallen Tree Obstruction", "🌲", "HIGH")
            "STRUCTURAL_COLLAPSE" -> Triple("Structural Debris / Collapse", "🏚", "CRITICAL")
            "ROAD_BLOCKAGE" -> Triple("Road Impassable", "🛑", "HIGH")
            "SUBMERGED_UNDERPASS" -> Triple("Submerged Underpass", "🌊", if (h.waterDepthCm >= 30) "CRITICAL" else "HIGH")
            "WATERLOGGING", "DRAINAGE_OVERFLOW" -> {
                val label = if (h.waterDepthCm > 0) "Waterlogging (${h.waterDepthCm} cm)" else "Severe Waterlogging"
                Triple(label, "🌊", if (h.waterDepthCm >= 35) "CRITICAL" else if (h.waterDepthCm >= 20) "HIGH" else "CAUTION")
            }
            else -> {
                if (h.waterDepthCm > 0) {
                    Triple("Waterlogging (${h.waterDepthCm} cm)", "🌊", if (h.waterDepthCm >= 30) "CRITICAL" else "HIGH")
                } else {
                    Triple("Active Incident Area", "⚠️", "MODERATE")
                }
            }
        }
    }

    // ── Haversine helper ───────────────────────────────────────────────────────

    fun haversineMeters(
        lat1: Double, lng1: Double,
        lat2: Double, lng2: Double
    ): Float {
        val R = 6_371_000.0
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val dPhi = Math.toRadians(lat2 - lat1)
        val dLambda = Math.toRadians(lng2 - lng1)
        val a = Math.sin(dPhi / 2).let { it * it } +
                Math.cos(phi1) * Math.cos(phi2) *
                Math.sin(dLambda / 2).let { it * it }
        return (2 * R * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))).toFloat()
    }
}

// ---------------------------------------------------------------------------
// Data models
// ---------------------------------------------------------------------------

/**
 * Lightweight representation of a cached flood hazard.
 * Serialised to DataStore JSON for offline persistence.
 */
data class CachedHazard(
    val eventId: String,
    val lat: Double,
    val lng: Double,
    val waterDepthCm: Int,
    val passability: String,
    val category: String,
    val fetchedAtMs: Long
)

/**
 * Enriched nearby hazard view-model with distance and UI formatting.
 */
data class NearbyHazardInfo(
    val hazard: CachedHazard,
    val distanceMeters: Float,
    val title: String,
    val iconEmoji: String,
    val riskSeverity: String
)
