package com.example.zerogrid.emergency

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zerogrid.location.HazardAlert
import com.example.zerogrid.location.VehicleRiskCalculator
import com.example.zerogrid.network.DetourRequest
import com.example.zerogrid.network.RetrofitInstance
import com.example.zerogrid.util.MapsIntentBuilder
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

// ── Dialog step enum ─────────────────────────────────────────────────────────

private enum class OverlayStep {
    WARNING,
    VEHICLE_SELECT,
    RISK_RESULT,
    DEST_INPUT,
    FETCHING_ROUTE,
    ROUTE_READY
}

// ── Root composable ───────────────────────────────────────────────────────────

/**
 * Full-screen dimmed overlay content driven by a simple state machine.
 * Embedded into a [android.view.WindowManager] view by [OverlayAlertManager].
 */
@Composable
fun HazardOverlayContent(
    alert: HazardAlert,
    onDismiss: () -> Unit
) {
    var step            by remember { mutableStateOf(OverlayStep.WARNING) }
    var selectedVehicle by remember { mutableStateOf<VehicleRiskCalculator.VehicleType?>(null) }
    var riskAssessment  by remember { mutableStateOf<VehicleRiskCalculator.RiskAssessment?>(null) }
    var detourGeoJson   by remember { mutableStateOf<String?>(null) }
    var detourSummary   by remember { mutableStateOf("") }
    var detourDestLat   by remember { mutableStateOf(0.0) }
    var detourDestLng   by remember { mutableStateOf(0.0) }
    val scope           = rememberCoroutineScope()
    val context         = LocalContext.current
    val scrollState     = rememberScrollState()

    // Full-screen semi-transparent dim
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        // Alert card with vertical scrolling for adaptability across devices
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1A1A2E))
                .verticalScroll(scrollState)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with Alert badge and Close 'X' button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x2EFF5722))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "🚨 HAZARD PROXIMITY ALERT",
                        color = Color(0xFFFF7043),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Cross 'X' Button to close / stop overlay immediately
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0x22FFFFFF))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close overlay",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            when (step) {

                // ── Step 1: Warning ───────────────────────────────────────────
                OverlayStep.WARNING -> WarningStep(
                    alert    = alert,
                    onCheck  = { step = OverlayStep.VEHICLE_SELECT },
                    onIgnore = onDismiss
                )

                // ── Step 2: Vehicle selection ─────────────────────────────────
                OverlayStep.VEHICLE_SELECT -> VehicleSelectStep(
                    onSelect = { vehicle ->
                        selectedVehicle = vehicle
                        riskAssessment  = VehicleRiskCalculator.assess(vehicle, alert.waterDepthCm)
                        step = OverlayStep.RISK_RESULT
                    }
                )

                // ── Step 3: Risk result ───────────────────────────────────────
                OverlayStep.RISK_RESULT -> RiskResultStep(
                    risk      = riskAssessment!!,
                    onReroute = { step = OverlayStep.DEST_INPUT },
                    onIgnore  = onDismiss
                )

                // ── Step 4: Destination input with Google Maps Search ─────────
                OverlayStep.DEST_INPUT -> DestInputStep(
                    alert = alert,
                    onFetchRoute = { lat, lng, destinationTitle ->
                        detourDestLat = lat
                        detourDestLng = lng
                        step = OverlayStep.FETCHING_ROUTE
                        scope.launch {
                            try {
                                val resp = RetrofitInstance.sosApi.requestDetour(
                                    DetourRequest(
                                        originLat = alert.hazardLat,
                                        originLng = alert.hazardLng,
                                        destLat   = detourDestLat,
                                        destLng   = detourDestLng
                                    )
                                )
                                if (resp.isSuccessful && resp.body() != null) {
                                    val body = resp.body()!!
                                    detourGeoJson = body.safeRouteGeoJson
                                    detourSummary = body.warningMessage.ifBlank {
                                        "Safe detour route avoiding ${body.avoidedHazardsCount} hazard(s) to $destinationTitle"
                                    }
                                } else {
                                    detourSummary = "Direct route to $destinationTitle prepared."
                                }
                            } catch (e: Exception) {
                                detourSummary = "Offline route to $destinationTitle ready."
                            }
                            step = OverlayStep.ROUTE_READY
                        }
                    }
                )

                // ── Step 5: Loading ───────────────────────────────────────────
                OverlayStep.FETCHING_ROUTE -> FetchingStep()

                // ── Step 6: Route ready ───────────────────────────────────────
                OverlayStep.ROUTE_READY -> RouteReadyStep(
                    summary = detourSummary,
                    onOpen  = {
                        MapsIntentBuilder.launch(
                            context    = context,
                            originLat  = alert.hazardLat,
                            originLng  = alert.hazardLng,
                            destLat    = detourDestLat,
                            destLng    = detourDestLng,
                            geoJson    = detourGeoJson
                        )
                        onDismiss()
                    },
                    onDismiss = onDismiss
                )
            }
        }
    }
}

// ── Individual step composables ───────────────────────────────────────────────

@Composable
private fun WarningStep(
    alert: HazardAlert,
    onCheck: () -> Unit,
    onIgnore: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Text("⚠️", fontSize = 48.sp, modifier = Modifier.scale(pulseScale))
    Spacer(Modifier.height(8.dp))
    Text(
        "FLOOD HAZARD AHEAD",
        fontSize = 20.sp,
        fontWeight = FontWeight.ExtraBold,
        color = Color(0xFFFF5722),
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(8.dp))
    Text(
        "Waterlogging detected ~${alert.distanceMeters.toInt()}m ahead.",
        fontSize = 14.sp,
        color = Color.White,
        textAlign = TextAlign.Center
    )
    Text(
        "Reported depth: ${alert.waterDepthCm} cm",
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFFFFCC00),
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(20.dp))
    Button(
        onClick = onCheck,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
    ) {
        Text("Check Vehicle Risk & Reroute", fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
    Spacer(Modifier.height(8.dp))
    OutlinedButton(
        onClick = onIgnore,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF888888))
    ) {
        Text("Dismiss Warning")
    }
}

@Composable
private fun VehicleSelectStep(
    onSelect: (VehicleRiskCalculator.VehicleType) -> Unit
) {
    Text(
        "What vehicle are you using?",
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(6.dp))
    Text(
        "We evaluate flood depth against your vehicle's safe wading limit.",
        fontSize = 12.sp,
        color = Color(0xFFAAAAAA),
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(16.dp))

    VehicleRiskCalculator.VehicleType.entries.forEach { vehicle ->
        VehicleButton(vehicle = vehicle, onClick = { onSelect(vehicle) })
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun VehicleButton(
    vehicle: VehicleRiskCalculator.VehicleType,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF252540))
            .border(1.dp, Color(0xFF3A3A60), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = "${vehicle.emoji} ${vehicle.label}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = Color.White
                )
                Text(
                    text = "Max safe depth: ${vehicle.safeDepthCm} cm",
                    fontSize = 12.sp,
                    color = Color(0xFFAAAAAA)
                )
            }
            Text("Select →", fontSize = 13.sp, color = Color(0xFF64B5F6))
        }
    }
}

@Composable
private fun RiskResultStep(
    risk: VehicleRiskCalculator.RiskAssessment,
    onReroute: () -> Unit,
    onIgnore: () -> Unit
) {
    val statusColor = Color(risk.riskLevel.color)

    Text(
        text = risk.headline,
        fontSize = 20.sp,
        fontWeight = FontWeight.ExtraBold,
        color = statusColor,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = risk.detail,
        fontSize = 14.sp,
        color = Color.White,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = "Vehicle: ${risk.vehicleType.emoji} ${risk.vehicleType.label} (safe up to ${risk.vehicleType.safeDepthCm} cm)",
        fontSize = 12.sp,
        color = Color(0xFFAAAAAA)
    )
    Spacer(Modifier.height(20.dp))

    if (risk.riskLevel != VehicleRiskCalculator.RiskLevel.SAFE) {
        Button(
            onClick = onReroute,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
        ) {
            Text("Find Safe Bypass Route →", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Spacer(Modifier.height(8.dp))
    }
    OutlinedButton(
        onClick = onIgnore,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF888888))
    ) {
        Text("I'll Proceed Anyway")
    }
}

// ── Search result model ───────────────────────────────────────────────────────

data class LocationSearchResult(
    val title: String,
    val subtitle: String,
    val lat: Double,
    val lng: Double
)

@Composable
private fun DestInputStep(
    alert: HazardAlert,
    onFetchRoute: (Double, Double, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<LocationSearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var selectedDestination by remember { mutableStateOf<LocationSearchResult?>(null) }

    val initialPos = LatLng(alert.hazardLat, alert.hazardLng)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPos, 13f)
    }

    fun triggerSearch(targetQuery: String = searchQuery) {
        val trimmed = targetQuery.trim()
        if (trimmed.isBlank()) return
        keyboard?.hide()
        isSearching = true
        searchError = null
        scope.launch {
            val list = searchLocations(context, trimmed)
            isSearching = false
            if (list.isEmpty()) {
                searchError = "No matching location found. Try city/neighborhood name or GPS."
            } else {
                searchResults = list
                val best = list.first()
                selectedDestination = best
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(LatLng(best.lat, best.lng), 14f)
                )
            }
        }
    }

    // Auto-search debounce as user types
    LaunchedEffect(searchQuery) {
        val trimmed = searchQuery.trim()
        if (trimmed.length >= 3 && !trimmed.contains(",")) {
            delay(600)
            triggerSearch(trimmed)
        }
    }

    Text(
        "Where do you want to go?",
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )
    Spacer(Modifier.height(4.dp))
    Text(
        "Search location or tap directly on the map below",
        fontSize = 12.sp,
        color = Color(0xFFAAAAAA),
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(12.dp))

    // Search bar with clear button and search trigger
    OutlinedTextField(
        value = searchQuery,
        onValueChange = {
            searchQuery = it
            if (searchError != null) searchError = null
        },
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(
                "e.g. Bandra Terminus, Lilavati Hospital, 19.05,72.82",
                color = Color(0xFF666677),
                fontSize = 12.sp
            )
        },
        singleLine = true,
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = {
                        searchQuery = ""
                        searchResults = emptyList()
                        searchError = null
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = Color(0xFFAAAAAA),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF29B6F6)
                    )
                } else {
                    IconButton(onClick = { triggerSearch() }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF29B6F6)
                        )
                    }
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { triggerSearch() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = Color(0xFF29B6F6),
            unfocusedBorderColor = Color(0xFF444466),
            focusedTextColor     = Color.White,
            unfocusedTextColor   = Color.White
        )
    )

    Spacer(Modifier.height(8.dp))

    // Quick destination shortcut chips
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf("🏥 Hospital", "🚉 Station", "📍 Center").forEach { chip ->
            val term = chip.substringAfter(" ")
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF24243D))
                    .border(1.dp, Color(0xFF3A3A5E), RoundedCornerShape(12.dp))
                    .clickable {
                        searchQuery = term
                        triggerSearch(term)
                    }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(chip, fontSize = 11.sp, color = Color(0xFFB0BEC5), fontWeight = FontWeight.Medium)
            }
        }
    }

    // Search Results Dropdown
    if (searchResults.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF131326))
                .border(1.dp, Color(0xFF2E2E50), RoundedCornerShape(10.dp))
        ) {
            searchResults.take(3).forEach { result ->
                val isSelected = selectedDestination?.lat == result.lat && selectedDestination?.lng == result.lng
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedDestination = result
                            searchQuery = result.title
                            scope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(LatLng(result.lat, result.lng), 14f)
                                )
                            }
                        }
                        .background(if (isSelected) Color(0x3329B6F6) else Color.Transparent)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (isSelected) "📍" else "▫", fontSize = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = result.title,
                            color = if (isSelected) Color(0xFF29B6F6) else Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                        Text(
                            text = result.subtitle,
                            color = Color(0xFF8888AA),
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }

    if (searchError != null) {
        Spacer(Modifier.height(6.dp))
        Text(
            text = searchError!!,
            color = Color(0xFFFF5252),
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }

    Spacer(Modifier.height(10.dp))

    // Interactive Google Map View
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF33335A), RoundedCornerShape(12.dp))
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = true,
                scrollGesturesEnabled = true,
                zoomGesturesEnabled = true,
                rotationGesturesEnabled = true,
                compassEnabled = true,
                myLocationButtonEnabled = false
            ),
            properties = MapProperties(isMyLocationEnabled = false),
            onMapClick = { clickedLatLng ->
                val formatted = String.format(java.util.Locale.US, "%.5f, %.5f", clickedLatLng.latitude, clickedLatLng.longitude)
                selectedDestination = LocationSearchResult(
                    title = "Selected Map Pin",
                    subtitle = formatted,
                    lat = clickedLatLng.latitude,
                    lng = clickedLatLng.longitude
                )
                searchQuery = formatted
            }
        ) {
            // Hazard Zone Origin Pin (Red)
            Marker(
                state = MarkerState(position = LatLng(alert.hazardLat, alert.hazardLng)),
                title = "⚠ Hazard Zone",
                snippet = "Water depth: ${alert.waterDepthCm} cm",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
            )

            // Destination Pin (Green)
            selectedDestination?.let { dest ->
                Marker(
                    state = MarkerState(position = LatLng(dest.lat, dest.lng)),
                    title = "🏁 ${dest.title}",
                    snippet = dest.subtitle,
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                )
            }
        }
    }

    Spacer(Modifier.height(8.dp))

    // Selected destination indicator
    if (selectedDestination != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0F263F))
                .border(1.dp, Color(0xFF1E4976), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🏁", fontSize = 14.sp)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = selectedDestination!!.title,
                    color = Color(0xFF64B5F6),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1
                )
                Text(
                    text = String.format(java.util.Locale.US, "Lat: %.5f  •  Lng: %.5f", selectedDestination!!.lat, selectedDestination!!.lng),
                    color = Color(0xFF90CAF9),
                    fontSize = 10.sp
                )
            }
        }
    } else {
        Text(
            text = "Type location above or tap map directly to set destination pin",
            color = Color(0xFF8888AA),
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }

    Spacer(Modifier.height(14.dp))

    // Submit button extracting lat & lng for OSRM
    Button(
        onClick = {
            val dest = selectedDestination ?: return@Button
            keyboard?.hide()
            onFetchRoute(dest.lat, dest.lng, dest.title)
        },
        enabled = selectedDestination != null,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF1565C0),
            disabledContainerColor = Color(0xFF222238)
        )
    ) {
        Text("Calculate Safe Route via OSRM →", fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
private fun FetchingStep() {
    Spacer(Modifier.height(16.dp))
    CircularProgressIndicator(color = Color(0xFF1565C0), modifier = Modifier.size(48.dp))
    Spacer(Modifier.height(16.dp))
    Text("Calculating safe bypass via OSRM…",
        fontSize = 15.sp, color = Color(0xFFAAAAAA), textAlign = TextAlign.Center)
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun RouteReadyStep(
    summary: String,
    onOpen: () -> Unit,
    onDismiss: () -> Unit
) {
    Text("✅", fontSize = 48.sp)
    Spacer(Modifier.height(12.dp))
    Text("Safe Route Ready",
        fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF69F0AE))
    Spacer(Modifier.height(8.dp))
    Text(summary,
        fontSize = 14.sp, color = Color(0xFFBBBBBB), textAlign = TextAlign.Center)
    Spacer(Modifier.height(20.dp))
    Button(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
    ) {
        Text("🗺 Open in Google Maps", fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
    Spacer(Modifier.height(8.dp))
    OutlinedButton(
        onClick = onDismiss,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF666666))
    ) {
        Text("Close")
    }
}

// ── Search & Geocoding Resolver ───────────────────────────────────────────────

private val geocodeHttpClient by lazy {
    okhttp3.OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .build()
}

/**
 * Searches places by name or coordinates using Android Geocoder and OpenStreetMap Nominatim fallback.
 */
private suspend fun searchLocations(
    context: Context,
    query: String
): List<LocationSearchResult> = withContext(Dispatchers.IO) {
    val trimmed = query.trim()
    if (trimmed.isBlank()) return@withContext emptyList()

    val results = mutableListOf<LocationSearchResult>()

    // 1. Direct lat,lng coordinate parse
    val parts = trimmed.split(",")
    if (parts.size == 2) {
        val lat = parts[0].trim().toDoubleOrNull()
        val lng = parts[1].trim().toDoubleOrNull()
        if (lat != null && lng != null && lat in -90.0..90.0 && lng in -180.0..180.0) {
            results.add(
                LocationSearchResult(
                    title = "GPS Coordinates",
                    subtitle = String.format(java.util.Locale.US, "%.5f, %.5f", lat, lng),
                    lat = lat,
                    lng = lng
                )
            )
            return@withContext results
        }
    }

    // 2. Android native Geocoder (Google Maps backed on GMS devices)
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
        Log.w("HazardOverlayView", "Native Geocoder failed: ${e.message}")
    }

    // 3. Fallback: OpenStreetMap Nominatim API via OkHttp
    if (results.isEmpty()) {
        try {
            val encoded = java.net.URLEncoder.encode(trimmed, "UTF-8")
            val url = "https://nominatim.openstreetmap.org/search?q=$encoded&format=json&limit=5&addressdetails=1"
            val request = okhttp3.Request.Builder()
                .url(url)
                .header("User-Agent", "ZeroGrid-Android-Disaster-Mesh/1.0 (disaster-mesh@zerogrid.org)")
                .build()

            geocodeHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string().orEmpty()
                    val jsonArray = JSONArray(bodyString)
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
            Log.w("HazardOverlayView", "Nominatim fallback failed: ${e.message}")
        }
    }

    results
}
