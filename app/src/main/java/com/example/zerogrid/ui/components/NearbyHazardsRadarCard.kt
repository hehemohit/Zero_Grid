package com.example.zerogrid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zerogrid.location.NearbyHazardInfo
import java.util.Locale

/**
 * Live Nearby Hazard Radar Card on the Home Dashboard.
 * Displays cached active hazards within 10 km with distances and severity badges.
 */
@Composable
fun NearbyHazardsRadarCard(
    nearbyHazards: List<NearbyHazardInfo>,
    onPlanSafeRouteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF2C2C4E), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF14142B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // ── Header ──────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (nearbyHazards.isNotEmpty()) Color(0x2EFF5722) else Color(0x2E4CAF50)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (nearbyHazards.isNotEmpty()) "⚡" else "🛡",
                            fontSize = 16.sp
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "HAZARD RADAR (10 KM)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (nearbyHazards.isNotEmpty()) Color(0xFFFF7043) else Color(0xFF81C784),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (nearbyHazards.isNotEmpty()) {
                                "${nearbyHazards.size} Active Risk Zones Detected"
                            } else {
                                "Surrounding Area Clear"
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                // Proximity count pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (nearbyHazards.isNotEmpty()) Color(0x33FF5722) else Color(0x334CAF50))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (nearbyHazards.isNotEmpty()) "${nearbyHazards.size} NEARBY" else "SAFE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (nearbyHazards.isNotEmpty()) Color(0xFFFF8A65) else Color(0xFF81C784)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Hazard List or Safe State ────────────────────────────────────────
            if (nearbyHazards.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1B2F24))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("✅", fontSize = 16.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "No flooding, wire drops, or road blockages reported within 10 km.",
                            fontSize = 12.sp,
                            color = Color(0xFFA5D6A7),
                            lineHeight = 16.sp
                        )
                    }
                }
            } else {
                // Show up to 3 closest hazards
                nearbyHazards.take(3).forEach { item ->
                    HazardRadarRow(item)
                    Spacer(Modifier.height(8.dp))
                }

                if (nearbyHazards.size > 3) {
                    Text(
                        text = "+ ${nearbyHazards.size - 3} more hazards monitored in background",
                        fontSize = 11.sp,
                        color = Color(0xFF8888AA),
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Action: Safe Route Button ────────────────────────────────────────
            Button(
                onClick = onPlanSafeRouteClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (nearbyHazards.isNotEmpty()) Color(0xFF1565C0) else Color(0xFF1E2846)
                )
            ) {
                Text(
                    text = "🧭 Plan Safe Route with Agent →",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun HazardRadarRow(item: NearbyHazardInfo) {
    val severityBg = when (item.riskSeverity) {
        "CRITICAL" -> Color(0x33FF5252)
        "HIGH" -> Color(0x33FF9800)
        else -> Color(0x3303A9F4)
    }
    val severityTextColor = when (item.riskSeverity) {
        "CRITICAL" -> Color(0xFFFF5252)
        "HIGH" -> Color(0xFFFFB74D)
        else -> Color(0xFF81D4FA)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1C1C35))
            .border(1.dp, Color(0xFF2E2E52), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(item.iconEmoji, fontSize = 18.sp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = item.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1
                )
                val distStr = if (item.distanceMeters < 1000) {
                    "${item.distanceMeters.toInt()}m away"
                } else {
                    String.format(Locale.US, "%.1f km away", item.distanceMeters / 1000f)
                }
                Text(
                    text = distStr,
                    fontSize = 11.sp,
                    color = Color(0xFF90CAF9)
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(severityBg)
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Text(
                text = item.riskSeverity,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = severityTextColor
            )
        }
    }
}
