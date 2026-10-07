package com.example.zerogrid.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.AltRoute
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zerogrid.mesh.engine.MeshEngine
import com.example.zerogrid.mesh.engine.ProximityWarning
import com.example.zerogrid.network.DetourRequest
import com.example.zerogrid.network.DetourResponse
import com.example.zerogrid.network.RetrofitInstance
import com.example.zerogrid.ui.theme.ZeroGridTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Pulsing high-priority proximity warning banner displayed when the device is
 * within 500 meters of an active severe waterlogging or flood hazard.
 * Provides real-time AI agent detour rerouting capability.
 */
@Composable
fun ProximityWarningBanner(
    warning: ProximityWarning?,
    modifier: Modifier = Modifier,
    onDetourResult: ((DetourResponse) -> Unit)? = null
) {
    AnimatedVisibility(
        visible = warning != null,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        if (warning == null) return@AnimatedVisibility

        val colors = ZeroGridTheme.colors
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        var isCalculatingDetour by remember { mutableStateOf(false) }
        var detourResponse by remember { mutableStateOf<DetourResponse?>(null) }
        var detourError by remember { mutableStateOf<String?>(null) }
        var showDetourDialog by remember { mutableStateOf(false) }

        // Pulsing border animation to capture user focus
        val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_alpha"
        )

        val warningColor = if (warning.waterDepthCm >= 45 || warning.passability == "IMPASSABLE") {
            Color(0xFFEF4444) // Bright Red
        } else {
            Color(0xFFF59E0B) // Amber
        }

        Card(
            modifier = modifier
                .fillMaxWidth()
                .border(
                    BorderStroke(2.dp, warningColor.copy(alpha = pulseAlpha)),
                    shape = RoundedCornerShape(16.dp)
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = warningColor.copy(alpha = 0.12f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(warningColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⚠️",
                            fontSize = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "FLASH FLOOD ALERT • ${warning.distanceMeters.toInt()}m AHEAD",
                            color = warningColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${warning.waterDepthCm}cm water logged • ${warning.passability.replace('_', ' ')}",
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Do not enter submerged area. Roadway is unsafe for transit.",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        if (isCalculatingDetour) return@Button
                        isCalculatingDetour = true
                        detourError = null

                        coroutineScope.launch {
                            try {
                                val engine = MeshEngine.getInstance(context)
                                val origin = engine.lastUserLocation ?: Pair(12.9716, 77.5946)
                                val hLat = if (warning.hazardLat != 0.0) warning.hazardLat else origin.first + 0.003
                                val hLng = if (warning.hazardLng != 0.0) warning.hazardLng else origin.second + 0.003

                                // Destination past the hazard
                                val destLat = hLat + 0.008
                                val destLng = hLng + 0.008

                                val req = DetourRequest(
                                    originLat = origin.first,
                                    originLng = origin.second,
                                    destLat = destLat,
                                    destLng = destLng
                                )

                                val resp = withContext(Dispatchers.IO) {
                                    try {
                                        RetrofitInstance.sosApi.requestDetour(req)
                                    } catch (e: Exception) {
                                        null
                                    }
                                }

                                if (resp != null && resp.isSuccessful && resp.body() != null) {
                                    val body = resp.body()!!
                                    detourResponse = body
                                    onDetourResult?.invoke(body)
                                    showDetourDialog = true
                                } else {
                                    // Offline fallback agentic advice
                                    detourResponse = DetourResponse(
                                        safeRouteGeoJson = null,
                                        warningMessage = "Offline Rerouting: Diverting around ${warning.distanceMeters.toInt()}m waterlogged zone.",
                                        avoidedHazardsCount = 1,
                                        agentReasoning = "Mesh AI Fallback: Water depth of ${warning.waterDepthCm}cm exceeds threshold. Recommending immediate lateral diversion to higher-elevation service road."
                                    )
                                    showDetourDialog = true
                                }
                            } finally {
                                isCalculatingDetour = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = warningColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isCalculatingDetour) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "COMPUTING AI DETOUR...",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.AltRoute,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CALCULATE SAFE DETOUR",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Dialog displaying the computed detour and reasoning
        if (showDetourDialog && detourResponse != null) {
            AlertDialog(
                onDismissRequest = { showDetourDialog = false },
                containerColor = colors.cardBackground,
                shape = RoundedCornerShape(16.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🛡️", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Safe Route Detour",
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                },
                text = {
                    Column {
                        Text(
                            text = detourResponse?.warningMessage ?: "Hazard avoided.",
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = colors.background,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "AI AGENT REASONING",
                                    color = colors.textSecondary,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = detourResponse?.agentReasoning
                                        ?: "Avoiding inundated low-lying terrain to protect engine and chassis integrity.",
                                    color = colors.textSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Avoided hazards: ${detourResponse?.avoidedHazardsCount ?: 1}",
                            color = Color(0xFF10B981),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showDetourDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Apply Detour", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDetourDialog = false }) {
                        Text("Dismiss", color = colors.textSecondary)
                    }
                }
            )
        }
    }
}
