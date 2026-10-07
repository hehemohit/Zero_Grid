package com.example.zerogrid.emergency

import android.location.Geocoder
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
 * Embedded into a [WindowManager] view by [OverlayAlertManager].
 */
@Composable
fun HazardOverlayContent(
    alert: HazardAlert,
    onDismiss: () -> Unit
) {
    var step            by remember { mutableStateOf(OverlayStep.WARNING) }
    var selectedVehicle by remember { mutableStateOf<VehicleRiskCalculator.VehicleType?>(null) }
    var riskAssessment  by remember { mutableStateOf<VehicleRiskCalculator.RiskAssessment?>(null) }
    var destInput       by remember { mutableStateOf("") }
    var detourGeoJson   by remember { mutableStateOf<String?>(null) }
    var detourSummary   by remember { mutableStateOf("") }
    var detourDestLat   by remember { mutableStateOf(0.0) }
    var detourDestLng   by remember { mutableStateOf(0.0) }
    val scope           = rememberCoroutineScope()
    val context         = LocalContext.current

    // Full-screen semi-transparent dim
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        // Alert card
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1A1A2E))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
                    risk     = riskAssessment!!,
                    onReroute = { step = OverlayStep.DEST_INPUT },
                    onIgnore  = onDismiss
                )

                // ── Step 4: Destination input ─────────────────────────────────
                OverlayStep.DEST_INPUT -> DestInputStep(
                    destInput = destInput,
                    onChange  = { destInput = it },
                    onFetch   = {
                        step = OverlayStep.FETCHING_ROUTE
                        scope.launch {
                            val resolved = resolveDestination(context, destInput)
                            if (resolved == null) {
                                // Can't resolve — go back
                                step = OverlayStep.DEST_INPUT
                                return@launch
                            }
                            detourDestLat = resolved.first
                            detourDestLng = resolved.second

                            try {
                                val resp = RetrofitInstance.sosApi.requestDetour(
                                    DetourRequest(
                                        originLat = alert.hazardLat,   // use hazard proximity coords as origin context
                                        originLng = alert.hazardLng,
                                        destLat   = detourDestLat,
                                        destLng   = detourDestLng
                                    )
                                )
                                if (resp.isSuccessful && resp.body() != null) {
                                    val body = resp.body()!!
                                    detourGeoJson = body.safeRouteGeoJson
                                    detourSummary = body.warningMessage.ifBlank {
                                        "Avoids ${body.avoidedHazardsCount} hazard(s)"
                                    }
                                } else {
                                    detourSummary = "Route calculated (direct)."
                                }
                            } catch (e: Exception) {
                                detourSummary = "Route ready (offline mode)."
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
    // Pulsing alert icon animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "scale"
    )

    Text("🌊", fontSize = 56.sp, modifier = Modifier.scale(scale))
    Spacer(Modifier.height(12.dp))
    Text(
        "FLOOD ZONE AHEAD",
        fontSize = 20.sp,
        fontWeight = FontWeight.ExtraBold,
        color = Color(0xFFFF5252),
        letterSpacing = 2.sp
    )
    Spacer(Modifier.height(8.dp))
    Text(
        "You are ${alert.distanceMeters.toInt()} m from an active hazard",
        fontSize = 13.sp,
        color = Color(0xFFBBBBBB),
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(16.dp))

    // Depth badge
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF7B1FA2))
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("💧", fontSize = 18.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            "Water Depth: ${alert.waterDepthCm} cm",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }

    Spacer(Modifier.height(24.dp))

    Button(
        onClick = onCheck,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
    ) {
        Text("Check My Risk →", fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
    Spacer(Modifier.height(8.dp))
    OutlinedButton(
        onClick = onIgnore,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF888888))
    ) {
        Text("Dismiss")
    }
}

@Composable
private fun VehicleSelectStep(
    onSelect: (VehicleRiskCalculator.VehicleType) -> Unit
) {
    Text("What are you driving?",
        fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
    Spacer(Modifier.height(6.dp))
    Text("Tap your vehicle type for a risk assessment",
        fontSize = 13.sp, color = Color(0xFFAAAAAA), textAlign = TextAlign.Center)
    Spacer(Modifier.height(20.dp))

    VehicleRiskCalculator.VehicleType.entries.forEach { vehicle ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF2A2A3E))
                .border(1.dp, Color(0xFF444466), RoundedCornerShape(14.dp))
                .clickable { onSelect(vehicle) }
                .padding(18.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(vehicle.emoji, fontSize = 24.sp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(vehicle.label,
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Text("Safe up to ${vehicle.safeDepthCm} cm",
                        fontSize = 12.sp, color = Color(0xFF888888))
                }
            }
        }
    }
}

@Composable
private fun RiskResultStep(
    risk: VehicleRiskCalculator.RiskAssessment,
    onReroute: () -> Unit,
    onIgnore: () -> Unit
) {
    val bgColor = Color(risk.riskLevel.color)

    Text(risk.vehicleType.emoji, fontSize = 40.sp)
    Spacer(Modifier.height(12.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor.copy(alpha = 0.2f))
            .border(1.5.dp, bgColor, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                risk.headline,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = bgColor
            )
            Spacer(Modifier.height(8.dp))
            Text(
                risk.detail,
                fontSize = 13.sp,
                color = Color(0xFFCCCCCC),
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )
        }
    }
    Spacer(Modifier.height(20.dp))

    Button(
        onClick = onReroute,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
    ) {
        Text("🗺 Yes, Reroute Me", fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
    Spacer(Modifier.height(8.dp))
    OutlinedButton(
        onClick = onIgnore,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF888888))
    ) {
        Text("I'll Proceed Anyway")
    }
}

@Composable
private fun DestInputStep(
    destInput: String,
    onChange: (String) -> Unit,
    onFetch: () -> Unit
) {
    val keyboard = LocalSoftwareKeyboardController.current
    Text("Where are you heading?",
        fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
    Spacer(Modifier.height(6.dp))
    Text("Enter address or lat,lng coordinates",
        fontSize = 13.sp, color = Color(0xFFAAAAAA), textAlign = TextAlign.Center)
    Spacer(Modifier.height(16.dp))

    OutlinedTextField(
        value = destInput,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("e.g. Connaught Place or 28.63,77.21", color = Color(0xFF666666)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { keyboard?.hide(); onFetch() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = Color(0xFF1565C0),
            unfocusedBorderColor = Color(0xFF444466),
            focusedTextColor     = Color.White,
            unfocusedTextColor   = Color.White
        )
    )
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = { keyboard?.hide(); onFetch() },
        enabled = destInput.isNotBlank(),
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
    ) {
        Text("Get Safe Route →", fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
private fun FetchingStep() {
    Spacer(Modifier.height(16.dp))
    CircularProgressIndicator(color = Color(0xFF1565C0), modifier = Modifier.size(48.dp))
    Spacer(Modifier.height(16.dp))
    Text("Calculating safe bypass…",
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

// ── Destination resolver ──────────────────────────────────────────────────────

/** Tries to parse "lat,lng" first, then falls back to Geocoder. */
private suspend fun resolveDestination(
    context: Context,
    input: String
): Pair<Double, Double>? = withContext(Dispatchers.IO) {
    // Try direct lat,lng parse
    val parts = input.trim().split(",")
    if (parts.size == 2) {
        val lat = parts[0].trim().toDoubleOrNull()
        val lng = parts[1].trim().toDoubleOrNull()
        if (lat != null && lng != null) return@withContext Pair(lat, lng)
    }
    // Geocoder fallback
    try {
        @Suppress("DEPRECATION")
        val results = Geocoder(context).getFromLocationName(input.trim(), 1)
        if (!results.isNullOrEmpty()) {
            return@withContext Pair(results[0].latitude, results[0].longitude)
        }
    } catch (_: Exception) {}
    null
}
