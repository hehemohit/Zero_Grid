package com.example.zerogrid.emergency

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zerogrid.navigation.Screen
import com.example.zerogrid.ui.theme.BadgeGreen
import com.example.zerogrid.ui.theme.ZeroGridTheme
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

/**
 * Permanent First-Class Safe Route Copilot Screen.
 * Integrates an interactive Google Map on top with a persistent Conversational
 * Route Safety Agent Copilot drawer below.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafeRouteCopilotScreen(
    onBack: () -> Unit,
    onNavigate: (Screen) -> Unit = {},
    copilotViewModel: RouteCopilotViewModel = remember { RouteCopilotViewModel() }
) {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val colors = ZeroGridTheme.colors
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val uiState by copilotViewModel.uiState.collectAsState()
    var inputQuery by remember { mutableStateOf("") }
    val chatListState = rememberLazyListState()

    // Detect soft keyboard state
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0
    val mapWeight by animateFloatAsState(
        targetValue = if (isImeVisible) 0.35f else 1.05f,
        label = "mapWeight"
    )

    // Initialize on launch
    LaunchedEffect(Unit) {
        copilotViewModel.initialize(context)
    }

    // Auto-scroll chat to bottom when new messages arrive or keyboard appears
    LaunchedEffect(uiState.messages.size, isImeVisible) {
        if (uiState.messages.isNotEmpty()) {
            chatListState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    // Google Map Camera State
    val defaultPos = uiState.userLocation ?: LatLng(19.4534, 72.8061)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultPos, 14f)
    }

    // Fit camera to origin, destination, and evasive waypoints whenever activeReport changes
    LaunchedEffect(uiState.activeReport) {
        val report = uiState.activeReport
        if (report != null) {
            try {
                val builder = LatLngBounds.builder()
                builder.include(report.origin)
                builder.include(report.destination)
                report.waypoints.forEach { builder.include(it) }
                report.avoidedHazards.forEach { builder.include(LatLng(it.lat, it.lng)) }
                val bounds = builder.build()
                cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 120))
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Safe Route Copilot",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = BadgeGreen.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "● AI Active",
                                color = BadgeGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        },
        contentWindowInsets = WindowInsets.statusBars,
        containerColor = colors.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .navigationBarsPadding()
                .imePadding()
        ) {
            // ── TOP SECTION: Interactive Route & Hazard Map ───────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(mapWeight)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = false,
                        scrollGesturesEnabled = true,
                        zoomGesturesEnabled = true,
                        rotationGesturesEnabled = true,
                        compassEnabled = true,
                        myLocationButtonEnabled = false
                    ),
                    properties = MapProperties(isMyLocationEnabled = false)
                ) {
                    // Safe Route Polyline (Emerald Green)
                    uiState.activeReport?.let { report ->
                        if (report.routePoints.isNotEmpty()) {
                            Polyline(
                                points = report.routePoints,
                                color = Color(0xFF10B981),
                                width = 14f
                            )
                        }

                        // Destination Pin
                        Marker(
                            state = MarkerState(position = report.destination),
                            title = uiState.destinationTitle.ifBlank { "Destination" },
                            snippet = "Protected Destination Target"
                        )

                        // Avoided Hazards with protective rings
                        report.avoidedHazards.forEach { avoided ->
                            val pos = LatLng(avoided.lat, avoided.lng)
                            Marker(
                                state = MarkerState(position = pos),
                                title = "🛡️ Avoided: ${avoided.title}",
                                snippet = avoided.evasionDetail
                            )
                            Circle(
                                center = pos,
                                radius = 140.0,
                                fillColor = Color(0x3310B981),
                                strokeColor = Color(0xFF10B981),
                                strokeWidth = 3f
                            )
                        }

                        // Unavoidable hazards with caution rings
                        report.unavoidableHazards.forEach { unavoidable ->
                            val pos = LatLng(unavoidable.lat, unavoidable.lng)
                            Marker(
                                state = MarkerState(position = pos),
                                title = "⚠️ Warning: ${unavoidable.title}",
                                snippet = unavoidable.advice
                            )
                            Circle(
                                center = pos,
                                radius = 120.0,
                                fillColor = Color(0x33EF4444),
                                strokeColor = Color(0xFFEF4444),
                                strokeWidth = 3f
                            )
                        }
                    }

                    // User Origin Pin
                    uiState.userLocation?.let { origin ->
                        Marker(
                            state = MarkerState(position = origin),
                            title = "Your Location",
                            snippet = "Active GPS Origin"
                        )
                    }
                }

                // Floating Map HUD (Distance & Avoidance stats + Maps Launch)
                if (uiState.activeReport != null) {
                    uiState.activeReport?.let { report ->
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 10.dp),
                            color = colors.cardBackground.copy(alpha = 0.95f),
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.divider),
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "🛡️ ${report.avoidedHazards.size} Avoided",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BadgeGreen
                                )
                                Text(
                                    text = "•",
                                    color = colors.textSecondary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = String.format(java.util.Locale.US, "%.1f km", report.distanceMeters / 1000.0),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "•",
                                    color = colors.textSecondary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "~${Math.max(1, (report.durationSeconds / 60.0).toInt())}m",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )

                                Spacer(Modifier.width(2.dp))

                                // Direct Google Maps Launch Pill
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            val mode = when (uiState.selectedVehicle.id) {
                                                "PEDESTRIAN" -> "walking"
                                                "TWO_WHEELER" -> "bicycling"
                                                else -> "driving"
                                            }
                                            com.example.zerogrid.util.MapsIntentBuilder.launchWithReport(context, report, mode)
                                        },
                                    color = colors.primary,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Navigation,
                                            contentDescription = "Maps",
                                            tint = Color.White,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(Modifier.width(3.dp))
                                        Text(
                                            text = "Maps ↗",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Vehicle Selector Chips (Top Left)
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    uiState.availableVehicles.forEach { vehicle ->
                        val isSelected = vehicle.id == uiState.selectedVehicle.id
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { copilotViewModel.setVehicleClearance(vehicle, context) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) colors.primary else colors.cardBackground.copy(alpha = 0.9f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) colors.primary else colors.divider),
                            shadowElevation = 2.dp
                        ) {
                            Text(
                                text = "${vehicle.iconEmoji} ${vehicle.label}",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else colors.textPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // ── BOTTOM SECTION: Conversational Agent Copilot Drawer (Weight 0.95f) ─
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.95f),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                color = colors.surfaceNested,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.divider),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 10.dp, start = 14.dp, end = 14.dp, bottom = 8.dp)
                ) {
                    // Drawer Header & Drag Indicator
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(colors.divider)
                            .align(Alignment.CenterHorizontally)
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🤖", fontSize = 16.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Route Safety Agent",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        }

                        if (uiState.destination != null) {
                            Text(
                                text = "Target: ${uiState.destinationTitle}",
                                fontSize = 11.sp,
                                color = colors.primary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }

                    // Dedicated "Continue to Google Maps with Waypoints" Action Card
                    if (uiState.activeReport != null) {
                        uiState.activeReport?.let { report ->
                            val waypointsCount = if (report.waypoints.isNotEmpty()) report.waypoints.size else report.routePoints.size.coerceAtMost(8)
                            Button(
                                onClick = {
                                    val mode = when (uiState.selectedVehicle.id) {
                                        "PEDESTRIAN" -> "walking"
                                        "TWO_WHEELER" -> "bicycling"
                                        else -> "driving"
                                    }
                                    com.example.zerogrid.util.MapsIntentBuilder.launchWithReport(context, report, mode)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.primary,
                                    contentColor = Color.White
                                )
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Navigation,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Continue to Google Maps ($waypointsCount Waypoints Enforced)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // Message List
                    LazyColumn(
                        state = chatListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.messages, key = { it.id }) { msg ->
                            CopilotMessageBubble(
                                message = msg,
                                onChipClick = { chip ->
                                    copilotViewModel.handleUserPrompt(chip, context)
                                }
                            )
                        }

                        if (uiState.isCalculating) {
                            item {
                                Surface(
                                    color = colors.cardBackground,
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.divider)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            color = colors.primary,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "Agent evaluating flood corridors & corridor geometry...",
                                            fontSize = 11.sp,
                                            color = colors.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Suggestion Chips Bar
                    val latestChips = uiState.messages.lastOrNull()?.suggestedChips ?: emptyList()
                    if (latestChips.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                        ) {
                            items(latestChips) { chipText ->
                                SuggestionChip(
                                    onClick = {
                                        copilotViewModel.handleUserPrompt(chipText, context)
                                    },
                                    label = {
                                        Text(
                                            text = chipText,
                                            fontSize = 11.sp,
                                            color = colors.textPrimary
                                        )
                                    },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = colors.cardBackground
                                    ),
                                    border = SuggestionChipDefaults.suggestionChipBorder(
                                        enabled = true,
                                        borderColor = colors.divider
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                )
                            }
                        }
                    }

                    // Live Nearby Recommendations Card
                    AnimatedVisibility(
                        visible = inputQuery.isNotBlank() && (uiState.suggestions.isNotEmpty() || uiState.isSearchingSuggestions),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 210.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = colors.cardBackground,
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.primary.copy(alpha = 0.35f)),
                            shadowElevation = 8.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "📍 Nearby Places",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.primary
                                        )
                                        if (uiState.isSearchingSuggestions) {
                                            Spacer(Modifier.width(6.dp))
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(10.dp),
                                                color = colors.primary,
                                                strokeWidth = 1.5.dp
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { copilotViewModel.clearSuggestions() },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Close suggestions",
                                            tint = colors.textSecondary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }

                                HorizontalDivider(
                                    color = colors.divider.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )

                                if (uiState.suggestions.isEmpty() && !uiState.isSearchingSuggestions) {
                                    Text(
                                        text = "No nearby landmarks found for \"$inputQuery\". You can tap send to query broadly.",
                                        fontSize = 11.sp,
                                        color = colors.textSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        items(uiState.suggestions) { suggestion ->
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        inputQuery = ""
                                                        keyboard?.hide()
                                                        copilotViewModel.selectSuggestion(suggestion, context)
                                                    },
                                                color = Color.Transparent
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 6.dp, vertical = 5.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = colors.primary.copy(alpha = 0.12f),
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                imageVector = Icons.Outlined.Place,
                                                                contentDescription = null,
                                                                tint = colors.primary,
                                                                modifier = Modifier.size(15.dp)
                                                            )
                                                        }
                                                    }

                                                    Spacer(Modifier.width(8.dp))

                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = suggestion.title,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = colors.textPrimary,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        if (suggestion.subtitle.isNotBlank() && suggestion.subtitle != suggestion.title) {
                                                            Text(
                                                                text = suggestion.subtitle,
                                                                fontSize = 10.sp,
                                                                color = colors.textSecondary,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }

                                                    suggestion.distanceMeters?.let { dist ->
                                                        val distStr = if (dist < 1000) {
                                                            "${dist.toInt()} m"
                                                        } else {
                                                            String.format(java.util.Locale.US, "%.1f km", dist / 1000f)
                                                        }
                                                        Spacer(Modifier.width(6.dp))
                                                        Surface(
                                                            shape = RoundedCornerShape(10.dp),
                                                            color = BadgeGreen.copy(alpha = 0.15f)
                                                        ) {
                                                            Text(
                                                                text = distStr,
                                                                color = BadgeGreen,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Input Box
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(25.dp))
                            .background(colors.cardBackground)
                            .border(1.dp, colors.divider, RoundedCornerShape(25.dp))
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = inputQuery,
                            onValueChange = {
                                inputQuery = it
                                copilotViewModel.onSearchInputChanged(it, context)
                            },
                            placeholder = {
                                Text(
                                    text = "Ask copilot (e.g. \"Navigate to Virar Station\")",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary
                            ),
                            singleLine = true
                        )

                        if (inputQuery.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    inputQuery = ""
                                    copilotViewModel.clearSuggestions()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                if (inputQuery.isNotBlank()) {
                                    val q = inputQuery
                                    inputQuery = ""
                                    keyboard?.hide()
                                    copilotViewModel.clearSuggestions()
                                    copilotViewModel.handleUserPrompt(q, context)
                                }
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(colors.primary)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CopilotMessageBubble(
    message: CopilotMessage,
    onChipClick: (String) -> Unit
) {
    val colors = ZeroGridTheme.colors
    val isUser = message.sender == CopilotSender.USER

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            color = if (isUser) colors.primary else colors.cardBackground,
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isUser) 14.dp else 2.dp,
                bottomEnd = if (isUser) 2.dp else 14.dp
            ),
            border = if (!isUser) androidx.compose.foundation.BorderStroke(1.dp, colors.divider) else null,
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = message.text,
                    fontSize = 12.sp,
                    color = if (isUser) Color.White else colors.textPrimary,
                    lineHeight = 17.sp
                )
            }
        }
    }
}
