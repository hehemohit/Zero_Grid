package com.example.zerogrid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.zerogrid.ui.theme.BadgeGreen
import com.example.zerogrid.ui.theme.ZeroGridTheme
import java.util.Locale

/**
 * Live Nearby Hazard Radar Card on the Home Dashboard.
 * Displays cached active hazards within 10 km with distances and severity badges.
 * Responsively adapts its colors and elevation to Light and Dark themes.
 */
@Composable
fun NearbyHazardsRadarCard(
    nearbyHazards: List<NearbyHazardInfo>,
    onPlanSafeRouteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ZeroGridTheme.colors
    val hasHazards = nearbyHazards.isNotEmpty()
    val isCritical = nearbyHazards.any { it.riskSeverity == "CRITICAL" || it.riskSeverity == "HIGH" }

    val cardBorderColor = when {
        !hasHazards -> colors.badgeGreen.copy(alpha = 0.35f)
        isCritical -> colors.accentRed.copy(alpha = 0.5f)
        else -> Color(0xFFF59E0B).copy(alpha = 0.5f)
    }

    val iconBgColor = when {
        !hasHazards -> colors.badgeGreen.copy(alpha = 0.15f)
        isCritical -> colors.accentRed.copy(alpha = 0.15f)
        else -> Color(0xFFF59E0B).copy(alpha = 0.15f)
    }

    val statusTextColor = when {
        !hasHazards -> colors.badgeGreen
        isCritical -> colors.accentRed
        else -> Color(0xFFD97706)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, cardBorderColor, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
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
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(iconBgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (hasHazards) "⚡" else "🛡️",
                            fontSize = 18.sp
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "HAZARD RADAR (10 KM)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusTextColor,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (hasHazards) {
                                "${nearbyHazards.size} Active Risk Zones Detected"
                            } else {
                                "Surrounding Area Clear"
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                    }
                }

                // Proximity count pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconBgColor)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (hasHazards) "${nearbyHazards.size} NEARBY" else "SAFE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusTextColor
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Hazard List or Safe State ────────────────────────────────────────
            if (!hasHazards) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.badgeGreen.copy(alpha = 0.08f))
                        .border(1.dp, colors.badgeGreen.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("✅", fontSize = 16.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "No flooding, wire drops, or road blockages reported within 10 km.",
                            fontSize = 12.sp,
                            color = colors.textPrimary,
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
                        color = colors.textSecondary,
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
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (hasHazards) colors.primary else colors.surfaceNested,
                    contentColor = if (hasHazards) Color.White else colors.textPrimary
                )
            ) {
                Text(
                    text = "🧭 Plan Safe Route with Agent →",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun HazardRadarRow(item: NearbyHazardInfo) {
    val colors = ZeroGridTheme.colors
    val isCritical = item.riskSeverity == "CRITICAL"
    val isHigh = item.riskSeverity == "HIGH"

    val severityBg = when {
        isCritical -> colors.accentRed.copy(alpha = 0.15f)
        isHigh -> Color(0xFFF59E0B).copy(alpha = 0.15f)
        else -> colors.primary.copy(alpha = 0.15f)
    }
    val severityTextColor = when {
        isCritical -> colors.accentRed
        isHigh -> Color(0xFFD97706)
        else -> colors.primary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surfaceNested)
            .border(1.dp, colors.divider, RoundedCornerShape(10.dp))
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
                    color = colors.textPrimary,
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
                    color = colors.textSecondary
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(severityBg)
                .padding(horizontal = 8.dp, vertical = 3.dp)
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

