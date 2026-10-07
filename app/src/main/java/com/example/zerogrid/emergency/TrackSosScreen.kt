package com.example.zerogrid.emergency

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zerogrid.navigation.Screen
import com.example.zerogrid.ui.theme.*
import androidx.compose.material3.ExperimentalMaterial3Api
import kotlin.math.*

/**
 * Track SOS Screen — real-time directional compass pointer for family members & responders.
 *
 * Features:
 * - Live bearing + distance calculation from responder to victim using LocationManager.
 * - Device heading via SensorManager.TYPE_ROTATION_VECTOR (magnetometer/accelerometer fallback).
 * - Animated radar needle that rotates toward the victim as the responder turns their phone.
 * - Tactical HUD: distance ring, category badge, coordinates, accuracy, estimated walk time.
 * - "OPEN IN MAPS" intent for Google Maps / OsmAnd turn-by-turn navigation.
 * - ON TARGET indicator when heading is within ±10° of bearing.
 */
@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackSosScreen(
    targetLat: Double,
    targetLng: Double,
    targetName: String = "Unknown",
    category: String = "SOS",
    sosTimestamp: Long = System.currentTimeMillis(),
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current

    // ── State ──────────────────────────────────────────────────────────────────
    var responderLat by remember { mutableStateOf<Double?>(null) }
    var responderLng by remember { mutableStateOf<Double?>(null) }
    var distanceMeters by remember { mutableStateOf<Float?>(null) }
    var targetBearing by remember { mutableStateOf(0f) }   // normalized bearing to target (0–360° True North)
    var deviceAzimuth by remember { mutableStateOf(0f) }   // smoothed phone heading (0–360° True North)
    var magneticDeclination by remember { mutableStateOf(0f) }
    var hasLocationPermission by remember { mutableStateOf(true) }

    // Relative target bearing from phone's current heading (0° = straight ahead)
    val relativeAngle = (targetBearing - deviceAzimuth + 360f) % 360f

    // Continuous angle accumulation to prevent 360° spinning across North boundary
    var continuousNeedleAngle by remember { mutableStateOf(0f) }
    LaunchedEffect(relativeAngle) {
        var diff = (relativeAngle - (continuousNeedleAngle % 360f + 360f) % 360f) % 360f
        if (diff < -180f) diff += 360f
        if (diff > 180f) diff -= 360f
        continuousNeedleAngle += diff
    }

    // Animated needle angle with smooth spring physics
    val needleAngle by animateFloatAsState(
        targetValue = continuousNeedleAngle,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
        label = "needleAngle"
    )

    // ON TARGET indicator: True when facing victim within ±15° straight ahead
    val normalizedRelative = ((needleAngle % 360f) + 360f) % 360f
    val deviationFromTarget = if (normalizedRelative > 180f) 360f - normalizedRelative else normalizedRelative
    val isOnTarget = deviationFromTarget <= 15f && distanceMeters != null

    // Radar pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val radarPulse by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarPulse"
    )

    // ── GPS Listener ───────────────────────────────────────────────────────────
    DisposableEffect(Unit) {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val listener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                responderLat = loc.latitude
                responderLng = loc.longitude
                val results = FloatArray(2)
                Location.distanceBetween(loc.latitude, loc.longitude, targetLat, targetLng, results)
                distanceMeters = results[0]
                targetBearing = (results[1] + 360f) % 360f

                try {
                    val geo = GeomagneticField(
                        loc.latitude.toFloat(),
                        loc.longitude.toFloat(),
                        loc.altitude.toFloat(),
                        System.currentTimeMillis()
                    )
                    magneticDeclination = geo.declination
                } catch (_: Exception) {}
            }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(p: String?, s: Int, e: Bundle?) {}
        }
        try {
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                .filter { lm?.isProviderEnabled(it) == true }
            providers.forEach { provider ->
                lm?.requestLocationUpdates(provider, 1000L, 1f, listener)
            }
            // Seed with last-known
            providers.mapNotNull { lm?.getLastKnownLocation(it) }
                .maxByOrNull { it.time }?.let { loc ->
                    responderLat = loc.latitude
                    responderLng = loc.longitude
                    val results = FloatArray(2)
                    Location.distanceBetween(loc.latitude, loc.longitude, targetLat, targetLng, results)
                    distanceMeters = results[0]
                    targetBearing = (results[1] + 360f) % 360f

                    try {
                        val geo = GeomagneticField(
                            loc.latitude.toFloat(),
                            loc.longitude.toFloat(),
                            loc.altitude.toFloat(),
                            System.currentTimeMillis()
                        )
                        magneticDeclination = geo.declination
                    } catch (_: Exception) {}
                }
        } catch (_: SecurityException) { hasLocationPermission = false }

        onDispose { lm?.removeUpdates(listener) }
    }

    // ── Sensor Listener (Compass / Azimuth with Tilt Compensation & Low-Pass Filter) ──
    DisposableEffect(magneticDeclination) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationSensor = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magSensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)

        fun updateAzimuthWithFilter(rawMagAzimuth: Float) {
            val trueHeading = (rawMagAzimuth + magneticDeclination + 360f) % 360f
            // Low-pass exponential moving average filter
            var diff = (trueHeading - deviceAzimuth) % 360f
            if (diff < -180f) diff += 360f
            if (diff > 180f) diff -= 360f
            deviceAzimuth = (deviceAzimuth + 0.22f * diff + 360f) % 360f
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        val rotMatrix = FloatArray(9)
                        SensorManager.getRotationMatrixFromVector(rotMatrix, event.values)

                        // Check pitch/tilt: if phone is held upright in hand, remap coordinate axes
                        val flatOrientation = FloatArray(3)
                        SensorManager.getOrientation(rotMatrix, flatOrientation)
                        val pitchDeg = Math.toDegrees(flatOrientation[1].toDouble())

                        val rawAzimuth = if (abs(pitchDeg) > 40.0) {
                            val adjustedR = FloatArray(9)
                            SensorManager.remapCoordinateSystem(rotMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, adjustedR)
                            val uprightOrientation = FloatArray(3)
                            SensorManager.getOrientation(adjustedR, uprightOrientation)
                            (Math.toDegrees(uprightOrientation[0].toDouble()).toFloat() + 360f) % 360f
                        } else {
                            (Math.toDegrees(flatOrientation[0].toDouble()).toFloat() + 360f) % 360f
                        }

                        updateAzimuthWithFilter(rawAzimuth)
                    }
                    Sensor.TYPE_ACCELEROMETER -> gravity.apply { event.values.copyInto(this) }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        geomagnetic.apply { event.values.copyInto(this) }
                        val R = FloatArray(9)
                        val I = FloatArray(9)
                        if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                            val flatOrientation = FloatArray(3)
                            SensorManager.getOrientation(R, flatOrientation)
                            val pitchDeg = Math.toDegrees(flatOrientation[1].toDouble())

                            val rawAzimuth = if (abs(pitchDeg) > 40.0) {
                                val adjustedR = FloatArray(9)
                                SensorManager.remapCoordinateSystem(R, SensorManager.AXIS_X, SensorManager.AXIS_Z, adjustedR)
                                val uprightOrientation = FloatArray(3)
                                SensorManager.getOrientation(adjustedR, uprightOrientation)
                                (Math.toDegrees(uprightOrientation[0].toDouble()).toFloat() + 360f) % 360f
                            } else {
                                (Math.toDegrees(flatOrientation[0].toDouble()).toFloat() + 360f) % 360f
                            }

                            updateAzimuthWithFilter(rawAzimuth)
                        }
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (rotationSensor != null) {
            sm.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_GAME)
        } else {
            accelSensor?.let { sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
            magSensor?.let { sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
        }

        onDispose { sm.unregisterListener(listener) }
    }

    // ── UI ─────────────────────────────────────────────────────────────────────
    val colors = ZeroGridTheme.colors

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "TRACK LOCATION",
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "SOS by $targetName",
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val isTablet = maxWidth >= 600.dp
            val horizontalPadding = if (isTablet) 32.dp else 20.dp
            val compassSize = if (maxWidth < 360.dp) 240.dp else if (isTablet) 320.dp else 280.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = horizontalPadding, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Category + elapsed time badge row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryChip(category)
                            Text(
                                text = formatElapsed(sosTimestamp),
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ── Compass Radar ──────────────────────────────────────────────────
                        Box(
                            modifier = Modifier
                                .size(compassSize)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            colors.surfaceNested,
                                            colors.background
                                        )
                                    )
                                )
                                .border(
                                    width = 2.dp,
                                    color = if (isOnTarget) colors.badgeGreen else colors.accentRed.copy(alpha = 0.6f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawRadarGrid(radarPulse, isOnTarget, colors)
                                drawCompassRose(colors)
                                drawNeedle(needleAngle, isOnTarget, colors)
                            }

                            // Center dot
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(
                                        if (isOnTarget) colors.badgeGreen else colors.textPrimary,
                                        CircleShape
                                    )
                            )

                            // ON TARGET indicator
                            if (isOnTarget) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 32.dp)
                                        .background(
                                            colors.badgeGreen.copy(alpha = 0.15f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(1.dp, colors.badgeGreen, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        "◎  ON TARGET",
                                        color = colors.badgeGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // ── Distance & Direction Readout ───────────────────────────────────
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                MetricCard(
                                    label = "DISTANCE",
                                    value = formatDistance(distanceMeters),
                                    icon = Icons.Outlined.Route,
                                    highlight = distanceMeters != null && distanceMeters!! < 100f
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                MetricCard(
                                    label = "BEARING",
                                    value = "${targetBearing.toInt()}°",
                                    icon = Icons.Outlined.Explore,
                                    highlight = false
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                MetricCard(
                                    label = "ETA (WALK)",
                                    value = formatWalkTime(distanceMeters),
                                    icon = Icons.AutoMirrored.Outlined.DirectionsWalk,
                                    highlight = false
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ── Coordinates Card ───────────────────────────────────────────────
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, colors.divider)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "TARGET COORDINATES",
                                    color = colors.textSecondary,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.PinDrop, contentDescription = null, tint = colors.accentRed, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "%.6f, %.6f".format(targetLat, targetLng),
                                        color = colors.textPrimary,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                if (responderLat != null && responderLng != null) {
                                    Spacer(Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.MyLocation, contentDescription = null, tint = colors.badgeGreen, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "Your position: %.6f, %.6f".format(responderLat, responderLng),
                                            color = colors.textSecondary,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ── Open in Maps Button ────────────────────────────────────────────
                        Button(
                            onClick = {
                                val uri = Uri.parse("geo:$targetLat,$targetLng?q=$targetLat,$targetLng(Emergency+SOS)")
                                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                    setPackage("com.google.android.apps.maps")
                                }
                                if (intent.resolveActivity(context.packageManager) != null) {
                                    context.startActivity(intent)
                                } else {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Outlined.Map, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "OPEN IN MAPS",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

// ── Canvas Drawing ──────────────────────────────────────────────────────────────

private fun DrawScope.drawRadarGrid(pulse: Float, isOnTarget: Boolean, colors: ZeroGridColorScheme) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val maxR = size.minDimension / 2f
    val gridColor = if (isOnTarget) colors.badgeGreen else colors.accentRed

    // Concentric rings
    for (i in 1..4) {
        val r = maxR * (i / 4f)
        drawCircle(
            color = gridColor.copy(alpha = 0.15f),
            radius = r,
            center = center,
            style = Stroke(1.dp.toPx())
        )
    }

    // Pulse ring
    val pulseR = maxR * (0.4f + pulse * 0.6f)
    drawCircle(
        color = gridColor.copy(alpha = (1f - pulse) * 0.4f),
        radius = pulseR,
        center = center,
        style = Stroke(1.5.dp.toPx())
    )

    // Cross-hair lines
    val lineColor = gridColor.copy(alpha = 0.2f)
    drawLine(lineColor, Offset(center.x, center.y - maxR), Offset(center.x, center.y + maxR), strokeWidth = 1.dp.toPx())
    drawLine(lineColor, Offset(center.x - maxR, center.y), Offset(center.x + maxR, center.y), strokeWidth = 1.dp.toPx())
}

private fun DrawScope.drawCompassRose(colors: ZeroGridColorScheme) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val r = size.minDimension / 2f - 12.dp.toPx()
    val tickColor = colors.textPrimary.copy(alpha = 0.3f)

    for (deg in 0 until 360 step 45) {
        val rad = Math.toRadians(deg.toDouble())
        val inner = if (deg % 90 == 0) r * 0.82f else r * 0.88f
        val outer = r
        val start = Offset(
            center.x + inner * sin(rad).toFloat(),
            center.y - inner * cos(rad).toFloat()
        )
        val end = Offset(
            center.x + outer * sin(rad).toFloat(),
            center.y - outer * cos(rad).toFloat()
        )
        drawLine(tickColor, start, end, strokeWidth = if (deg % 90 == 0) 2.dp.toPx() else 1.dp.toPx(), cap = StrokeCap.Round)
    }
}

private fun DrawScope.drawNeedle(angleFromNorth: Float, isOnTarget: Boolean, colors: ZeroGridColorScheme) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val needleLength = size.minDimension / 2f * 0.7f
    val color = if (isOnTarget) colors.badgeGreen else colors.accentRed

    rotate(angleFromNorth, pivot = center) {
        // Arrow head pointing "up" = toward target bearing
        val tip = Offset(center.x, center.y - needleLength)
        val leftWing = Offset(center.x - 10.dp.toPx(), center.y - needleLength * 0.55f)
        val rightWing = Offset(center.x + 10.dp.toPx(), center.y - needleLength * 0.55f)
        val tail = Offset(center.x, center.y + needleLength * 0.3f)

        val arrowPath = Path().apply {
            moveTo(tip.x, tip.y)
            lineTo(leftWing.x, leftWing.y)
            lineTo(center.x, center.y - needleLength * 0.2f)
            lineTo(rightWing.x, rightWing.y)
            close()
        }
        drawPath(arrowPath, color = color.copy(alpha = 0.9f))

        // Tail stub
        drawLine(
            color = color.copy(alpha = 0.4f),
            start = center,
            end = tail,
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Glow line
        drawLine(
            color = color.copy(alpha = 0.25f),
            start = center,
            end = tip,
            strokeWidth = 12.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

// ── Utility Components ────────────────────────────────────────────────────────

@Composable
private fun MetricCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, highlight: Boolean) {
    val colors = ZeroGridTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) colors.badgeGreen.copy(alpha = 0.1f) else colors.cardBackground
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (highlight) colors.badgeGreen.copy(alpha = 0.5f) else colors.divider)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (highlight) colors.badgeGreen else colors.textSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                color = if (highlight) colors.badgeGreen else colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
            Text(
                text = label,
                color = colors.textSecondary,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CategoryChip(category: String) {
    val colors = ZeroGridTheme.colors
    val (bg, fg) = when (category.uppercase()) {
        "MEDICAL"  -> Pair(colors.accentRed.copy(alpha = 0.15f), colors.accentRed)
        "DISASTER" -> Pair(Color(0xFFF97316).copy(alpha = 0.15f), Color(0xFFF97316))
        "TRAPPED"  -> Pair(Color(0xFFEAB308).copy(alpha = 0.15f), Color(0xFFEAB308))
        "SECURITY" -> Pair(Color(0xFF8B5CF6).copy(alpha = 0.15f), Color(0xFF8B5CF6))
        else       -> Pair(colors.accentRed.copy(alpha = 0.15f), colors.accentRed)
    }
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(6.dp))
            .border(1.dp, fg.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = "⚠ ${category.uppercase()}",
            color = fg,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

// ── Formatters ─────────────────────────────────────────────────────────────────

private fun formatDistance(meters: Float?): String {
    if (meters == null) return "---"
    return if (meters >= 1000f) "%.1f km".format(meters / 1000f) else "${meters.toInt()} m"
}

private fun formatWalkTime(meters: Float?): String {
    if (meters == null) return "---"
    val walkingSpeedMs = 1.4f // ~5 km/h
    val seconds = meters / walkingSpeedMs
    return when {
        seconds < 60 -> "${seconds.toInt()}s"
        seconds < 3600 -> "${(seconds / 60).toInt()} min"
        else -> "${(seconds / 3600).toInt()} hr"
    }
}

private fun formatElapsed(timestamp: Long): String {
    val delta = System.currentTimeMillis() - timestamp
    val minutes = delta / 60000
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        else -> "${minutes / 60}h ${minutes % 60}m ago"
    }
}

