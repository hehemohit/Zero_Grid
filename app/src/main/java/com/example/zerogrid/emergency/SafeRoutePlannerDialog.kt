package com.example.zerogrid.emergency

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.zerogrid.location.HazardCacheManager
import com.example.zerogrid.location.LocationHelper
import com.example.zerogrid.location.LocationSearchHelper
import com.example.zerogrid.location.LocationSearchResult
import com.example.zerogrid.ui.components.SafeRoutePreviewMap
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

private enum class PlannerStep {
    DEST_SELECT,
    AGENT_ANALYZING,
    ROUTE_READY
}

/**
 * Full-screen modal for planning a safe route from current location.
 * Uses local hazard database + OSRM via RuleBasedRouteSafetyAgent.
 */
@Composable
fun SafeRoutePlannerDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current

    var currentStep by remember { mutableStateOf(PlannerStep.DEST_SELECT) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<LocationSearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var selectedDestination by remember { mutableStateOf<LocationSearchResult?>(null) }

    var safetyReport by remember { mutableStateOf<RouteSafetyReport?>(null) }

    // Fetch initial user location
    LaunchedEffect(Unit) {
        val loc = LocationHelper.getLastKnownLocation(context) ?: LocationHelper.getCurrentLocation(context)
        if (loc != null) {
            userLocation = LatLng(loc.lat, loc.lng)
        } else {
            // Default to center if location permissions not yet ready
            userLocation = LatLng(19.0760, 72.8777)
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userLocation ?: LatLng(19.0760, 72.8777), 13f)
    }

    // Sync camera if user location arrives later
    LaunchedEffect(userLocation) {
        userLocation?.let {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(it, 13f)
        }
    }

    fun triggerSearch(target: String = searchQuery) {
        val trimmed = target.trim()
        if (trimmed.isBlank()) return
        keyboard?.hide()
        isSearching = true
        searchError = null
        scope.launch {
            val list = LocationSearchHelper.searchLocations(context, trimmed)
            isSearching = false
            if (list.isEmpty()) {
                searchError = "No matching location found. Try landmark name or coordinates."
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

    // Debounced search
    LaunchedEffect(searchQuery) {
        val trimmed = searchQuery.trim()
        if (trimmed.length >= 3 && !trimmed.contains(",")) {
            delay(600)
            triggerSearch(trimmed)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xE6000000))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, Color(0xFF2C2C4E), RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF14142B))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ── Header Bar ───────────────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🧭", fontSize = 18.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "SAFE ROUTE NAVIGATOR",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64B5F6),
                                letterSpacing = 1.sp
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0x22FFFFFF))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    when (currentStep) {
                        PlannerStep.DEST_SELECT -> {
                            val scrollState = rememberScrollState()
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(scrollState),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Where are you heading?",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "GridZero Agent will analyze the path against active hazards in local DB",
                                    fontSize = 11.sp,
                                    color = Color(0xFF8888AA),
                                    textAlign = TextAlign.Center
                                )

                                Spacer(Modifier.height(12.dp))

                                // Origin indicator (current location)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF1E2846))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🟢", fontSize = 12.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            "ORIGIN: Current GPS Location",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF81C784)
                                        )
                                        userLocation?.let {
                                            Text(
                                                String.format(Locale.US, "%.5f, %.5f", it.latitude, it.longitude),
                                                fontSize = 11.sp,
                                                color = Color.White
                                            )
                                        } ?: Text("Acquiring GPS fix...", fontSize = 11.sp, color = Color(0xFFAAAAAA))
                                    }
                                }

                                Spacer(Modifier.height(10.dp))

                                // Search box
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = {
                                        searchQuery = it
                                        if (searchError != null) searchError = null
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = {
                                        Text("Search destination or landmark...", color = Color(0xFF666677), fontSize = 12.sp)
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF8888AA))
                                    },
                                    trailingIcon = {
                                        if (isSearching) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFF64B5F6))
                                        } else if (searchQuery.isNotBlank()) {
                                            IconButton(onClick = { searchQuery = ""; searchResults = emptyList() }) {
                                                Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF8888AA))
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(onSearch = { triggerSearch() }),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF64B5F6),
                                        unfocusedBorderColor = Color(0xFF33335A),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = Color(0xFF131326),
                                        unfocusedContainerColor = Color(0xFF131326)
                                    )
                                )

                                Spacer(Modifier.height(8.dp))

                                // Quick presets chips
                                val presets = listOf("🏥 Hospital", "🚆 Station", "⛺ Shelter", "✈ Airport")
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(presets) { preset ->
                                        Surface(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    val queryTerm = preset.substring(2).trim()
                                                    searchQuery = queryTerm
                                                    triggerSearch(queryTerm)
                                                },
                                            color = Color(0xFF22223D),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = preset,
                                                fontSize = 11.sp,
                                                color = Color(0xFF90CAF9),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }

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
                                                Text(if (isSelected) "📍" else "▫", fontSize = 13.sp)
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

                                // Interactive Map for Pin Tap
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(190.dp)
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
                                            val formatted = String.format(Locale.US, "%.5f, %.5f", clickedLatLng.latitude, clickedLatLng.longitude)
                                            selectedDestination = LocationSearchResult(
                                                title = "Selected Map Pin",
                                                subtitle = formatted,
                                                lat = clickedLatLng.latitude,
                                                lng = clickedLatLng.longitude
                                            )
                                            searchQuery = formatted
                                        }
                                    ) {
                                        userLocation?.let {
                                            Marker(
                                                state = MarkerState(position = it),
                                                title = "You",
                                                snippet = "Origin"
                                            )
                                        }
                                        selectedDestination?.let {
                                            Marker(
                                                state = MarkerState(position = LatLng(it.lat, it.lng)),
                                                title = it.title,
                                                snippet = it.subtitle
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(14.dp))

                                // Submit action
                                Button(
                                    onClick = {
                                        val dest = selectedDestination ?: return@Button
                                        val origin = userLocation ?: return@Button
                                        keyboard?.hide()
                                        currentStep = PlannerStep.AGENT_ANALYZING
                                        scope.launch {
                                            try {
                                                val report = RuleBasedRouteSafetyAgent.executeSafeRoute(
                                                    origin = origin,
                                                    destination = LatLng(dest.lat, dest.lng),
                                                    cachedHazards = HazardCacheManager.getCachedHazards()
                                                )
                                                safetyReport = report
                                                currentStep = PlannerStep.ROUTE_READY
                                            } catch (e: Exception) {
                                                Log.e("SafeRoutePlanner", "Agent routing failed: ${e.message}", e)
                                                searchError = "Could not compute route: ${e.message}"
                                                currentStep = PlannerStep.DEST_SELECT
                                            }
                                        }
                                    },
                                    enabled = selectedDestination != null && userLocation != null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF1565C0),
                                        disabledContainerColor = Color(0xFF222238)
                                    )
                                ) {
                                    Text(
                                        "🤖 Execute Safe Route with Agent →",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                        PlannerStep.AGENT_ANALYZING -> {
                            Spacer(Modifier.height(24.dp))
                            CircularProgressIndicator(color = Color(0xFF29B6F6), modifier = Modifier.size(52.dp))
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "🤖 Safety Agent Analyzing Corridor...",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Intersecting route against active hazards in local database & executing OSRM bypass...",
                                fontSize = 12.sp,
                                color = Color(0xFFAAAAAA),
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(24.dp))
                        }

                        PlannerStep.ROUTE_READY -> {
                            safetyReport?.let { report ->
                                SafeRoutePreviewMap(
                                    report = report,
                                    destinationTitle = selectedDestination?.title ?: "Destination",
                                    onClose = onDismiss
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
