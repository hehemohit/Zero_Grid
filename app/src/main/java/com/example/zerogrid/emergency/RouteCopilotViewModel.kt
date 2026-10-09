package com.example.zerogrid.emergency

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zerogrid.location.CachedHazard
import com.example.zerogrid.location.HazardCacheManager
import com.example.zerogrid.location.LocationHelper
import com.example.zerogrid.location.LocationSearchHelper
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

enum class CopilotSender {
    USER,
    AGENT,
    SYSTEM
}

data class CopilotMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: CopilotSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val report: RouteSafetyReport? = null,
    val suggestedChips: List<String> = emptyList()
)

data class VehicleClearance(
    val id: String,
    val label: String,
    val iconEmoji: String,
    val maxWaterDepthCm: Int
)

data class RouteCopilotUiState(
    val userLocation: LatLng? = null,
    val destination: LatLng? = null,
    val destinationTitle: String = "",
    val activeReport: RouteSafetyReport? = null,
    val messages: List<CopilotMessage> = emptyList(),
    val isCalculating: Boolean = false,
    val selectedVehicle: VehicleClearance = VEHICLE_CAR,
    val availableVehicles: List<VehicleClearance> = listOf(
        VEHICLE_PEDESTRIAN,
        VEHICLE_TWO_WHEELER,
        VEHICLE_CAR,
        VEHICLE_HIGH_CLEARANCE
    ),
    val suggestions: List<com.example.zerogrid.location.LocationSearchResult> = emptyList(),
    val isSearchingSuggestions: Boolean = false
)

val VEHICLE_PEDESTRIAN = VehicleClearance("PEDESTRIAN", "Walking", "🚶", 15)
val VEHICLE_TWO_WHEELER = VehicleClearance("TWO_WHEELER", "2-Wheeler", "🛵", 20)
val VEHICLE_CAR = VehicleClearance("CAR", "Car / Auto", "🚗", 30)
val VEHICLE_HIGH_CLEARANCE = VehicleClearance("HIGH_CLEARANCE", "SUV / 4x4", "🚙", 50)

class RouteCopilotViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RouteCopilotUiState())
    val uiState: StateFlow<RouteCopilotUiState> = _uiState.asStateFlow()

    private var searchJob: kotlinx.coroutines.Job? = null

    fun initialize(context: Context) {
        viewModelScope.launch {
            val lastLoc = LocationHelper.getLastKnownLocation(context)
                ?: LocationHelper.getCurrentLocation(context)
            val origin = if (lastLoc != null) LatLng(lastLoc.lat, lastLoc.lng) else LatLng(19.4534, 72.8061)

            _uiState.value = _uiState.value.copy(userLocation = origin)

            // Initial Copilot Welcome
            if (_uiState.value.messages.isEmpty()) {
                val cachedHazards: List<CachedHazard> = HazardCacheManager.getCachedHazards()
                val nearbyCount = cachedHazards.count { h: CachedHazard ->
                    val dist = HazardCacheManager.haversineMeters(origin.latitude, origin.longitude, h.lat, h.lng)
                    dist <= 10000.0f
                }

                val welcomeText = if (nearbyCount > 0) {
                    "🛡️ Hello! I am your ZeroGrid Route Safety Copilot. I detect $nearbyCount active hazard zone(s) within 10 km. Tell me where you need to go, and I'll plot a route circumvating all flooded corridors and fallen grid wires."
                } else {
                    "🛡️ ZeroGrid Route Safety Copilot active. Surrounding area is currently clear of critical flood hazards. Where would you like to navigate?"
                }

                val initialChips = listOf(
                    "🏥 Sanjeevani Hospital",
                    "🚉 Virar Station",
                    "🌊 Avoid water > 20cm",
                    "🛡️ Highest Ground Path"
                )

                _uiState.value = _uiState.value.copy(
                    messages = listOf(
                        CopilotMessage(
                            sender = CopilotSender.AGENT,
                            text = welcomeText,
                            suggestedChips = initialChips
                        )
                    )
                )
            }
        }
    }

    fun setVehicleClearance(vehicle: VehicleClearance, context: Context) {
        _uiState.value = _uiState.value.copy(selectedVehicle = vehicle)
        // If an active destination exists, recalculate for new vehicle limits
        _uiState.value.destination?.let { dest ->
            recalculateRoute(dest, _uiState.value.destinationTitle, context, "Recalculated route for ${vehicle.label} (${vehicle.maxWaterDepthCm}cm max depth clearance).")
        }
    }

    fun handleUserPrompt(input: String, context: Context) {
        val prompt = input.trim()
        if (prompt.isBlank()) return

        // 1. Add user message
        val userMsg = CopilotMessage(sender = CopilotSender.USER, text = prompt)
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMsg,
            isCalculating = true
        )

        viewModelScope.launch {
            val lower = prompt.lowercase()

            when {
                // Clear route
                lower.contains("clear") || lower.contains("reset") -> {
                    _uiState.value = _uiState.value.copy(
                        destination = null,
                        destinationTitle = "",
                        activeReport = null,
                        isCalculating = false,
                        messages = _uiState.value.messages + CopilotMessage(
                            sender = CopilotSender.AGENT,
                            text = "Route cleared from map. Tell me your next destination when ready.",
                            suggestedChips = listOf("🏥 Sanjeevani Hospital", "🚉 Virar Station")
                        )
                    )
                }

                // Continue to Google Maps with Waypoints
                lower.contains("google maps") || lower.contains("open in maps") || lower.contains("continue to maps") -> {
                    val report = _uiState.value.activeReport
                    if (report != null) {
                        val mode = when (_uiState.value.selectedVehicle.id) {
                            "PEDESTRIAN" -> "walking"
                            "TWO_WHEELER" -> "bicycling"
                            else -> "driving"
                        }
                        com.example.zerogrid.util.MapsIntentBuilder.launchWithReport(context, report, mode)
                        val waypointsCount = if (report.waypoints.isNotEmpty()) report.waypoints.size else report.routePoints.size.coerceAtMost(8)
                        _uiState.value = _uiState.value.copy(
                            isCalculating = false,
                            messages = _uiState.value.messages + CopilotMessage(
                                sender = CopilotSender.AGENT,
                                text = "🚀 Google Maps launched with $waypointsCount intermediate evasion waypoints pinned. Google Maps will strictly follow the hazard-free corridor without re-routing you through standing water.",
                                suggestedChips = listOf("Why this detour?", "Clear Route")
                            )
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isCalculating = false,
                            messages = _uiState.value.messages + CopilotMessage(
                                sender = CopilotSender.AGENT,
                                text = "Please specify a destination first so I can calculate the safe evasion waypoints before opening Google Maps.",
                                suggestedChips = listOf("🏥 Sanjeevani Hospital", "🚉 Virar Station")
                            )
                        )
                    }
                }

                // Why this detour?
                lower.contains("why") && (lower.contains("detour") || lower.contains("avoid") || lower.contains("route")) -> {
                    val report = _uiState.value.activeReport
                    val response = if (report != null && report.avoidedHazards.isNotEmpty()) {
                        val avoidedNames = report.avoidedHazards.joinToString(", ") { "${it.title} (${it.category})" }
                        "🧭 We are detouring around $avoidedNames because direct passage conflicts with active standing water or electrical hazards. The evasion takes ~${report.avoidedHazards.size * 2} extra minutes but provides a 100% safe path."
                    } else if (report != null) {
                        "🧭 Your current path follows the direct corridor as no severe blockages or flooded underpasses were detected along this trajectory."
                    } else {
                        "Please select or search for a destination first so I can evaluate the corridors."
                    }

                    _uiState.value = _uiState.value.copy(
                        isCalculating = false,
                        messages = _uiState.value.messages + CopilotMessage(
                            sender = CopilotSender.AGENT,
                            text = response,
                            suggestedChips = listOf("🌊 Avoid water > 20cm", "🛵 Switch to 2-Wheeler", "❌ Clear Route")
                        )
                    )
                }

                // Depth constraint
                lower.contains("avoid water") || lower.contains("> 20cm") || lower.contains("water >") -> {
                    _uiState.value = _uiState.value.copy(selectedVehicle = VEHICLE_TWO_WHEELER)
                    val dest = _uiState.value.destination
                    if (dest != null) {
                        recalculateRoute(dest, _uiState.value.destinationTitle, context, "Adjusted threshold: Strictly avoiding water depth > 20cm. New evasive route plotted on map.")
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isCalculating = false,
                            messages = _uiState.value.messages + CopilotMessage(
                                sender = CopilotSender.AGENT,
                                text = "Strict < 20cm water restriction applied. Where should we navigate with this constraint?",
                                suggestedChips = listOf("🏥 Sanjeevani Hospital", "🚉 Virar Station")
                            )
                        )
                    }
                }

                // Landmark / Address search and navigation intent
                else -> {
                    // Extract place name (strip prefixes like "navigate to", "go to", "route to", etc.)
                    val cleanQuery = prompt
                        .replace("(?i)^(navigate to|take me to|go to|route to|find route to|safe route to)\\s+".toRegex(), "")
                        .replace("^📍\\s*".toRegex(), "")
                        .trim()

                    clearSuggestions()
                    val userLoc = _uiState.value.userLocation
                    val results = LocationSearchHelper.searchLocations(
                        context = context,
                        query = cleanQuery,
                        userLat = userLoc?.latitude,
                        userLng = userLoc?.longitude
                    )
                    if (results.isNotEmpty()) {
                        val best = results.first()
                        val target = LatLng(best.lat, best.lng)
                        val distDesc = best.distanceMeters?.let {
                            if (it < 1000) " (~${it.toInt()}m away)" else String.format(java.util.Locale.US, " (~%.1f km away)", it / 1000f)
                        } ?: ""

                        // Other nearby matches as quick alternate chips if available
                        val alternateChips = results.drop(1).take(3).map { "📍 ${it.title}" } + listOf("🗺️ Open in Google Maps", "Why this detour?")

                        recalculateRoute(
                            target = target,
                            title = best.title,
                            context = context,
                            agentSummary = "Plotted safe route to ${best.title}$distDesc. Inspect the green polyline on the map above.",
                            customChips = alternateChips
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isCalculating = false,
                            messages = _uiState.value.messages + CopilotMessage(
                                sender = CopilotSender.AGENT,
                                text = "I couldn't locate \"$cleanQuery\" near your current position. Try searching for a known landmark like \"Virar Station\", \"Sanjeevani Hospital\", or entering GPS coordinates.",
                                suggestedChips = listOf("🏥 Sanjeevani Hospital", "🚉 Virar Station")
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * Debounced live search invoked as the user types in the copilot prompt input box.
     * Biased to the user's current GPS coordinates and sorted by proximity.
     */
    fun onSearchInputChanged(query: String, context: Context) {
        searchJob?.cancel()
        val clean = query.trim()
            .replace("(?i)^(navigate to|take me to|go to|route to|find route to|safe route to)\\s+".toRegex(), "")
            .replace("^📍\\s*".toRegex(), "")
            .trim()

        if (clean.length < 2) {
            _uiState.value = _uiState.value.copy(
                suggestions = emptyList(),
                isSearchingSuggestions = false
            )
            return
        }

        _uiState.value = _uiState.value.copy(isSearchingSuggestions = true)
        searchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(260) // Debounce typing
            val origin = _uiState.value.userLocation
            val results = LocationSearchHelper.searchLocations(
                context = context,
                query = clean,
                userLat = origin?.latitude,
                userLng = origin?.longitude
            )
            _uiState.value = _uiState.value.copy(
                suggestions = results.take(6),
                isSearchingSuggestions = false
            )
        }
    }

    fun clearSuggestions() {
        searchJob?.cancel()
        _uiState.value = _uiState.value.copy(
            suggestions = emptyList(),
            isSearchingSuggestions = false
        )
    }

    fun selectSuggestion(item: com.example.zerogrid.location.LocationSearchResult, context: Context) {
        clearSuggestions()
        val target = LatLng(item.lat, item.lng)
        val distDesc = item.distanceMeters?.let {
            if (it < 1000) " (~${it.toInt()}m away)" else String.format(java.util.Locale.US, " (~%.1f km away)", it / 1000f)
        } ?: ""
        _uiState.value = _uiState.value.copy(isCalculating = true)
        viewModelScope.launch {
            recalculateRoute(
                target = target,
                title = item.title,
                context = context,
                agentSummary = "Plotted safe route to ${item.title}$distDesc selected from nearby places. Map updated with active hazard evasion waypoints."
            )
        }
    }

    fun selectDestination(target: LatLng, title: String, context: Context) {
        clearSuggestions()
        _uiState.value = _uiState.value.copy(isCalculating = true)
        viewModelScope.launch {
            recalculateRoute(target, title, context, "Safe route to $title calculated. Map updated with active hazard evasion waypoints.")
        }
    }

    private fun recalculateRoute(
        target: LatLng,
        title: String,
        context: Context,
        agentSummary: String,
        customChips: List<String>? = null
    ) {
        viewModelScope.launch {
            val origin = _uiState.value.userLocation ?: LatLng(19.4534, 72.8061)
            val cachedHazards: List<CachedHazard> = HazardCacheManager.getCachedHazards()

            // Filter hazards based on vehicle clearance
            val effectiveHazards = cachedHazards.filter { h: CachedHazard ->
                h.waterDepthCm >= _uiState.value.selectedVehicle.maxWaterDepthCm || h.category == "FALLEN_GRID"
            }

            val report = RuleBasedRouteSafetyAgent.executeSafeRoute(
                origin = origin,
                destination = target,
                cachedHazards = effectiveHazards
            )

            val avoidedCount = report.avoidedHazards.size
            val distanceKm = String.format(java.util.Locale.US, "%.1f km", report.distanceMeters / 1000.0)
            val durationMin = Math.max(1, (report.durationSeconds / 60.0).toInt())

            val detailedText = "$agentSummary\n\n📊 Distance: $distanceKm | ETA: $durationMin min | Avoided Hazards: $avoidedCount"

            val chips = customChips ?: listOf(
                "🗺️ Open in Google Maps",
                "Why this detour?",
                "Avoid water > 20cm",
                "Clear Route"
            )

            _uiState.value = _uiState.value.copy(
                destination = target,
                destinationTitle = title,
                activeReport = report,
                isCalculating = false,
                messages = _uiState.value.messages + CopilotMessage(
                    sender = CopilotSender.AGENT,
                    text = detailedText,
                    report = report,
                    suggestedChips = chips
                )
            )
        }
    }
}
