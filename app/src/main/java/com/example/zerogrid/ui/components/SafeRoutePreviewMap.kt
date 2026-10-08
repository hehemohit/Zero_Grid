package com.example.zerogrid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zerogrid.emergency.AvoidedHazardItem
import com.example.zerogrid.emergency.RouteSafetyReport
import com.example.zerogrid.emergency.UnavoidableHazardItem
import com.example.zerogrid.util.MapsIntentBuilder
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import java.util.Locale

/**
 * Shared In-App Visual Route Preview & Safety Assessment Component.
 * Used in both the Emergency Hazard Overlay and the Home Safe Route Planner.
 */
@Composable
fun SafeRoutePreviewMap(
    report: RouteSafetyReport,
    destinationTitle: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(report.origin, 13f)
    }

    // Auto-fit camera to bound origin, destination, and all waypoints
    LaunchedEffect(report) {
        try {
            val builder = LatLngBounds.builder()
            builder.include(report.origin)
            builder.include(report.destination)
            report.waypoints.forEach { builder.include(it) }
            report.avoidedHazards.forEach { builder.include(LatLng(it.lat, it.lng)) }
            report.unavoidableHazards.forEach { builder.include(LatLng(it.lat, it.lng)) }
            val bounds = builder.build()
            cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 80))
        } catch (_: Exception) {}
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── 1. Interactive In-App Route Preview Map ──────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFF33335A), RoundedCornerShape(16.dp))
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
                properties = MapProperties(isMyLocationEnabled = false)
            ) {
                // Route polyline
                if (report.routePoints.isNotEmpty()) {
                    Polyline(
                        points = report.routePoints,
                        color = Color(0xFF29B6F6),
                        width = 12f
                    )
                }

                // Origin pin
                Marker(
                    state = MarkerState(position = report.origin),
                    title = "Your Location",
                    snippet = "Starting point"
                )

                // Destination pin
                Marker(
                    state = MarkerState(position = report.destination),
                    title = destinationTitle,
                    snippet = "Target Destination"
                )

                // Detour waypoints
                report.waypoints.forEachIndexed { i, pt ->
                    Marker(
                        state = MarkerState(position = pt),
                        title = "Detour Waypoint #${i + 1}",
                        snippet = "Evasion path checkpoint"
                    )
                }

                // Avoided hazards (green circle perimeter)
                report.avoidedHazards.forEach { avoided ->
                    val pos = LatLng(avoided.lat, avoided.lng)
                    Marker(
                        state = MarkerState(position = pos),
                        title = "🛡 Avoided: ${avoided.title}",
                        snippet = avoided.evasionDetail
                    )
                    Circle(
                        center = pos,
                        radius = 120.0,
                        fillColor = Color(0x334CAF50),
                        strokeColor = Color(0xFF4CAF50),
                        strokeWidth = 3f
                    )
                }

                // Unavoidable hazards (red/amber circle perimeter)
                report.unavoidableHazards.forEach { unavoidable ->
                    val pos = LatLng(unavoidable.lat, unavoidable.lng)
                    Marker(
                        state = MarkerState(position = pos),
                        title = "⚠️ Caution: ${unavoidable.title}",
                        snippet = unavoidable.advice
                    )
                    Circle(
                        center = pos,
                        radius = 100.0,
                        fillColor = Color(0x33FF5252),
                        strokeColor = Color(0xFFFF5252),
                        strokeWidth = 3f
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // ── 2. Route Metrics Bar ─────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1B1B36))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("ESTIMATED DISTANCE", fontSize = 10.sp, color = Color(0xFF8888AA), fontWeight = FontWeight.Bold)
                Text(
                    text = String.format(Locale.US, "%.1f km", report.distanceMeters / 1000.0),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("ESTIMATED TRAVEL", fontSize = 10.sp, color = Color(0xFF8888AA), fontWeight = FontWeight.Bold)
                val mins = (report.durationSeconds / 60.0).toInt().coerceAtLeast(1)
                Text(
                    text = "$mins mins",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF64B5F6)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── 3. Agent Assessment Summary ──────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x1A29B6F6))
                .border(1.dp, Color(0x3329B6F6), RoundedCornerShape(10.dp))
                .padding(12.dp)
        ) {
            Text(
                text = "🤖 ${report.agentSummary}",
                fontSize = 12.sp,
                color = Color(0xFFE1F5FE),
                lineHeight = 16.sp
            )
        }

        Spacer(Modifier.height(14.dp))

        // ── 4. Things Avoided Using the Application (Shield / Green) ─────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF13221A))
                .border(1.dp, Color(0x334CAF50), RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🛡 AVOIDED BY GRIDZERO (${report.avoidedHazards.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF66BB6A)
                )
                Text("Successfully Detoured", fontSize = 10.sp, color = Color(0xFF81C784))
            }

            Spacer(Modifier.height(8.dp))

            if (report.avoidedHazards.isEmpty()) {
                Text(
                    text = "No direct hazard collisions were on your path.",
                    fontSize = 12.sp,
                    color = Color(0xFFAAAAAA)
                )
            } else {
                report.avoidedHazards.forEach { item ->
                    AvoidedHazardCard(item)
                    Spacer(Modifier.height(6.dp))
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── 5. Things You Must Face Anyway (Amber / Caution) ─────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF261919))
                .border(1.dp, Color(0x33FF5252), RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "⚠️ UNAVOIDABLE CONDITIONS (${report.unavoidableHazards.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF7043)
                )
                Text("Proceed With Caution", fontSize = 10.sp, color = Color(0xFFFFAB91))
            }

            Spacer(Modifier.height(8.dp))

            if (report.unavoidableHazards.isEmpty()) {
                Text(
                    text = "✅ 0 residual hazards reported along this corridor.",
                    fontSize = 12.sp,
                    color = Color(0xFFAAAAAA)
                )
            } else {
                report.unavoidableHazards.forEach { item ->
                    UnavoidableHazardCard(item)
                    Spacer(Modifier.height(6.dp))
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // ── 6. Navigation Actions ────────────────────────────────────────────
        Button(
            onClick = {
                val waypointsList = report.waypoints.map { Pair(it.latitude, it.longitude) }
                MapsIntentBuilder.launch(
                    context = context,
                    originLat = report.origin.latitude,
                    originLng = report.origin.longitude,
                    destLat = report.destination.latitude,
                    destLng = report.destination.longitude,
                    geoJson = report.geoJsonString
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
        ) {
            Text(
                "🗺 Open in Google Maps Navigation",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.White
            )
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFAAAAAA))
        ) {
            Text("Close Preview")
        }
    }
}

@Composable
private fun AvoidedHazardCard(item: AvoidedHazardItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1B2F24))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(item.iconEmoji, fontSize = 20.sp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = item.evasionDetail,
                fontSize = 11.sp,
                color = Color(0xFF81C784)
            )
        }
        Text("BYPASSED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
    }
}

@Composable
private fun UnavoidableHazardCard(item: UnavoidableHazardItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF331E1E))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(item.iconEmoji, fontSize = 20.sp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = item.advice,
                fontSize = 11.sp,
                color = Color(0xFFFFCC80)
            )
        }
        Text("WATCH OUT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF5252))
    }
}
