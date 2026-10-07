package com.example.zerogrid.admin.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zerogrid.admin.data.model.AdminSosEventDto
import com.example.zerogrid.admin.util.AdminFormatters
import com.example.zerogrid.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TacticalRadarCanvas(
    incidents: List<AdminSosEventDto>,
    selectedIncident: AdminSosEventDto?,
    onIncidentSelected: (AdminSosEventDto) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ZeroGridTheme.colors
    val infiniteTransition = rememberInfiniteTransition(label = "radar_anim")
    val sweepRadius by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_radius"
    )

    val validIncidents = remember(incidents) {
        incidents.filter { it.location?.hasValidCoordinates == true }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(colors.cardBackground, RoundedCornerShape(16.dp))
            .border(1.dp, colors.divider, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(validIncidents) {
                    detectTapGestures { tapOffset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val maxR = size.width.coerceAtMost(size.height) * 0.42f
                        validIncidents.forEachIndexed { idx, inc ->
                            val angle = (idx * 65.0 + (inc.location!!.latitude * 100)) % 360.0
                            val rad = Math.toRadians(angle)
                            val distFraction = 0.25f + ((idx * 17) % 65) / 100f
                            val px = center.x + (maxR * distFraction * cos(rad)).toFloat()
                            val py = center.y + (maxR * distFraction * sin(rad)).toFloat()
                            val hitRadius = 24.dp.toPx()
                            if ((tapOffset - Offset(px, py)).getDistance() <= hitRadius) {
                                onIncidentSelected(inc)
                            }
                        }
                    }
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.width.coerceAtMost(size.height) * 0.42f

            // 1. Concentric radar grid rings
            val ringColors = colors.primary.copy(alpha = 0.2f)
            drawCircle(ringColors, radius = maxRadius, center = center, style = Stroke(width = 1.dp.toPx()))
            drawCircle(ringColors, radius = maxRadius * 0.66f, center = center, style = Stroke(width = 1.dp.toPx()))
            drawCircle(ringColors, radius = maxRadius * 0.33f, center = center, style = Stroke(width = 1.dp.toPx()))

            // Crosshairs
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            drawLine(
                color = colors.primary.copy(alpha = 0.15f),
                start = Offset(center.x - maxRadius, center.y),
                end = Offset(center.x + maxRadius, center.y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = dashEffect
            )
            drawLine(
                color = colors.primary.copy(alpha = 0.15f),
                start = Offset(center.x, center.y - maxRadius),
                end = Offset(center.x, center.y + maxRadius),
                strokeWidth = 1.dp.toPx(),
                pathEffect = dashEffect
            )

            // Animated expanding sweep pulse
            drawCircle(
                color = colors.primary.copy(alpha = (1f - sweepRadius) * 0.25f),
                radius = maxRadius * sweepRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Center dispatch node marker
            drawCircle(colors.primary, radius = 5.dp.toPx(), center = center)
            drawCircle(colors.primary.copy(alpha = 0.4f), radius = 9.dp.toPx(), center = center, style = Stroke(1.5.dp.toPx()))

            // Plotted incident markers
            validIncidents.forEachIndexed { idx, inc ->
                val angle = (idx * 65.0 + (inc.location!!.latitude * 100)) % 360.0
                val rad = Math.toRadians(angle)
                val distFraction = 0.25f + ((idx * 17) % 65) / 100f
                val pinX = center.x + (maxRadius * distFraction * cos(rad)).toFloat()
                val pinY = center.y + (maxRadius * distFraction * sin(rad)).toFloat()
                val pinPos = Offset(pinX, pinY)

                val pinColor = AdminFormatters.getCategoryColor(inc.category)
                val isSelected = inc.eventId == selectedIncident?.eventId

                // Outer aura for selected or active
                if (isSelected) {
                    drawCircle(
                        color = if (colors.isDark) Color.White else Color.Black,
                        radius = 12.dp.toPx(),
                        center = pinPos,
                        style = Stroke(2.dp.toPx())
                    )
                }

                drawCircle(
                    color = pinColor.copy(alpha = 0.35f),
                    radius = 9.dp.toPx(),
                    center = pinPos
                )
                drawCircle(
                    color = pinColor,
                    radius = 5.dp.toPx(),
                    center = pinPos
                )
            }
        }

        // Radar Overlay Labels
        Column(
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Text(
                text = "TACTICAL SECTOR RADAR",
                color = colors.primary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "${validIncidents.size} GEO-TAGGED INCIDENTS",
                color = colors.textSecondary,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Text(
            text = "CENTER: DISPATCH",
            color = colors.primary.copy(alpha = 0.7f),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}
