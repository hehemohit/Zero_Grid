package com.example.zerogrid.admin.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zerogrid.R
import com.example.zerogrid.admin.data.model.AdminSosEventDto
import com.example.zerogrid.admin.util.AdminFormatters
import com.example.zerogrid.ui.theme.ZeroGridTheme
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*

@Composable
fun AdminGoogleMapView(
    incidents: List<AdminSosEventDto>,
    selectedIncident: AdminSosEventDto?,
    onIncidentSelected: (AdminSosEventDto?) -> Unit,
    onInspectDetails: (AdminSosEventDto) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = ZeroGridTheme.colors

    // Load tactical dark JSON map style in dark mode, or use clean native vector styling in light mode
    val mapStyleOptions = remember(colors.isDark) {
        if (colors.isDark) {
            try {
                MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark)
            } catch (e: Exception) {
                null
            }
        } else {
            null // Native Google Maps light styling with full street labels and road networks
        }
    }

    val mapProperties = remember(mapStyleOptions) {
        MapProperties(
            mapStyleOptions = mapStyleOptions,
            isMyLocationEnabled = false
        )
    }

    val uiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false,
            compassEnabled = true,
            myLocationButtonEnabled = false,
            mapToolbarEnabled = false
        )
    }

    // Determine initial center from the first valid incident or fallback
    val initialCenter = remember(incidents) {
        incidents.firstOrNull { ev ->
            val lat = ev.location?.latitude
            val lng = ev.location?.longitude
            lat != null && lng != null && (lat != 0.0 || lng != 0.0)
        }?.location?.let { LatLng(it.latitude, it.longitude) } ?: LatLng(28.6139, 77.2090)
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialCenter, 13f)
    }

    // Auto-focus on selected incident
    LaunchedEffect(selectedIncident) {
        if (selectedIncident != null) {
            val lat = selectedIncident.location?.latitude
            val lng = selectedIncident.location?.longitude
            if (lat != null && lng != null && (lat != 0.0 || lng != 0.0)) {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 15.5f),
                    durationMs = 600
                )
            }
        }
    }

    // When map finishes measuring and loading, auto-fit all active incidents
    var isMapLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(isMapLoaded, incidents) {
        if (isMapLoaded && selectedIncident == null && incidents.isNotEmpty()) {
            val validPoints = incidents.mapNotNull { ev ->
                val lat = ev.location?.latitude
                val lng = ev.location?.longitude
                if (lat != null && lng != null && (lat != 0.0 || lng != 0.0)) {
                    LatLng(lat, lng)
                } else null
            }

            if (validPoints.size == 1) {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(validPoints.first(), 14f),
                    durationMs = 600
                )
            } else if (validPoints.size > 1) {
                val builder = LatLngBounds.builder()
                validPoints.forEach { builder.include(it) }
                try {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngBounds(builder.build(), 90),
                        durationMs = 600
                    )
                } catch (_: Exception) {}
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, colors.divider, RoundedCornerShape(14.dp))
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = uiSettings,
            onMapLoaded = { isMapLoaded = true },
            onMapClick = {
                onIncidentSelected(null)
            }
        ) {
            incidents.forEach { incident ->
                val lat = incident.location?.latitude
                val lng = incident.location?.longitude
                if (lat != null && lng != null && (lat != 0.0 || lng != 0.0)) {
                    val position = LatLng(lat, lng)
                    val isSelected = incident.eventId == selectedIncident?.eventId

                    MarkerComposable(
                        keys = arrayOf(incident.eventId, isSelected, incident.status, incident.category),
                        state = rememberMarkerState(key = incident.eventId, position = position),
                        title = incident.displayId,
                        snippet = incident.userName,
                        onClick = {
                            onIncidentSelected(incident)
                            true
                        }
                    ) {
                        TacticalMapMarker(
                            incident = incident,
                            isSelected = isSelected
                        )
                    }
                }
            }
        }

        // Sector overlay header tag
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp),
            shape = RoundedCornerShape(8.dp),
            color = colors.cardBackground.copy(alpha = 0.92f),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.divider)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(colors.accentRed, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "LIVE SATELLITE / VECTOR GRID",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = colors.textPrimary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${incidents.size} ACTIVE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = colors.primary
                )
            }
        }

        // Floating Quick Info Card pinned to bottom overlay
        AnimatedVisibility(
            visible = selectedIncident != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp)
        ) {
            selectedIncident?.let { inc ->
                TacticalQuickInfoCard(
                    incident = inc,
                    onInspect = { onInspectDetails(inc) },
                    onDismiss = { onIncidentSelected(null) }
                )
            }
        }
    }
}

@Composable
private fun TacticalMapMarker(
    incident: AdminSosEventDto,
    isSelected: Boolean
) {
    val colors = ZeroGridTheme.colors
    val categoryColor = AdminFormatters.getCategoryColor(incident.category)
    val statusColor = AdminFormatters.getStatusColor(incident.status)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.wrapContentSize()
    ) {
        // Tag badge above marker
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = if (isSelected) categoryColor else colors.cardBackground.copy(alpha = 0.95f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isSelected) Color.White else categoryColor
            ),
            shadowElevation = 4.dp
        ) {
            Text(
                text = incident.displayId,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (isSelected) Color.White else colors.textPrimary,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // High-performance static tactical pin with halo
        Box(
            modifier = Modifier.size(if (isSelected) 36.dp else 28.dp),
            contentAlignment = Alignment.Center
        ) {
            // Outer target halo
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = categoryColor.copy(alpha = if (isSelected) 0.35f else 0.20f),
                        shape = CircleShape
                    )
                    .border(
                        1.dp,
                        categoryColor.copy(alpha = if (isSelected) 0.8f else 0.4f),
                        CircleShape
                    )
            )

            // Inner solid core
            Box(
                modifier = Modifier
                    .size(if (isSelected) 20.dp else 16.dp)
                    .background(Color.White, CircleShape)
                    .border(2.dp, categoryColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 10.dp else 8.dp)
                        .background(statusColor, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun TacticalQuickInfoCard(
    incident: AdminSosEventDto,
    onInspect: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = ZeroGridTheme.colors
    val categoryColor = AdminFormatters.getCategoryColor(incident.category)
    val batteryColor = AdminFormatters.getBatteryColor(incident.batteryPercentage)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onInspect() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground.copy(alpha = 0.96f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, categoryColor.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row: Category Pill, ID, Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = categoryColor.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, categoryColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = incident.category.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = categoryColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = incident.displayId,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = colors.textPrimary
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Body: User Name, Coordinates, Battery
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = incident.userName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = incident.location?.let {
                            String.format("%.4f, %.4f", it.latitude, it.longitude)
                        } ?: "GPS Fix Pending",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = colors.textSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    incident.batteryPercentage?.let { bat ->
                        Icon(
                            imageVector = when {
                                bat <= 20 -> Icons.Outlined.BatteryAlert
                                bat <= 50 -> Icons.Outlined.Battery3Bar
                                else -> Icons.Outlined.BatteryFull
                            },
                            contentDescription = null,
                            tint = batteryColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "$bat%",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = batteryColor
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    Button(
                        onClick = onInspect,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(
                            text = "Inspect",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
