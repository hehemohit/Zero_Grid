package com.example.zerogrid.emergency

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.zerogrid.hardware.HardwareStateManager
import com.example.zerogrid.hardware.WifiRequiredDialog
import com.example.zerogrid.location.LocationHelper
import com.example.zerogrid.mesh.engine.MeshEngine
import com.example.zerogrid.mesh.engine.MeshNode
import com.example.zerogrid.navigation.Screen
import com.example.zerogrid.ui.theme.*
import kotlinx.coroutines.launch

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun SendSosScreen(onNavigate: (Screen) -> Unit = {}) {
    val context = LocalContext.current
    val meshEngine = MeshEngine.getInstance(context)
    val coroutineScope = rememberCoroutineScope()
    val sosDispatcher = remember { UnifiedSosDispatcher(context) }
    var selectedType by remember { mutableStateOf("WATERLOGGING") }
    var waterDepthCm by remember { mutableStateOf(30) }
    var selectedPassability by remember { mutableStateOf("ALL_PASSABLE") }
    val isWaterCategory = selectedType in setOf("WATERLOGGING", "SUBMERGED_UNDERPASS", "DRAINAGE_OVERFLOW")
    var emergencyMessage by remember { mutableStateOf("") }
    var locationSharingEnabled by remember { mutableStateOf(true) }
    // Live GPS state shown in the location card
    var gpsLat by remember { mutableStateOf<Double?>(null) }
    var gpsLng by remember { mutableStateOf<Double?>(null) }
    var gpsAccuracy by remember { mutableStateOf<Float?>(null) }
    var gpsFetching by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    val activeChannelMode by meshEngine.activeChannelMode.collectAsState()
    val colors = ZeroGridTheme.colors

    // Eagerly fetch GPS when location sharing is enabled
    LaunchedEffect(locationSharingEnabled) {
        if (locationSharingEnabled) {
            gpsFetching = true
            val result = LocationHelper.getCurrentLocation(context)
            gpsLat = result?.lat
            gpsLng = result?.lng
            gpsAccuracy = result?.accuracy
            gpsFetching = false
        } else {
            gpsLat = null; gpsLng = null; gpsAccuracy = null
        }
    }

    Scaffold(
        containerColor = colors.background,
        topBar = { SendSosTopBar(onBackClick = { onNavigate(Screen.HOME) }) }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val isTablet = maxWidth >= 600.dp
            val horizontalPadding = if (isTablet) 32.dp else 16.dp

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 840.dp)
                            .fillMaxWidth()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            EmergencyBroadcastWarningCard()
                            Spacer(modifier = Modifier.height(20.dp))

                            // Emergency / Hazard Incident Type Section
                            Text(
                                text = "INCIDENT TYPE",
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            EmergencyTypeChipsRow(selected = selectedType, onSelected = { selectedType = it })

                            // Environmental Hazard Controls (conditional)
                            AnimatedVisibility(
                                visible = isWaterCategory,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    WaterDepthCard(
                                        waterDepthCm = waterDepthCm,
                                        onDepthChange = { waterDepthCm = it }
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    PassabilitySelectorCard(
                                        selectedPassability = selectedPassability,
                                        onPassabilityChange = { selectedPassability = it }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))

                            // Emergency Message Input Section
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "EMERGENCY MESSAGE",
                                    color = colors.textSecondary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${emergencyMessage.length} / 500",
                                    color = colors.textSecondary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            EmergencyMessageInput(
                                value = emergencyMessage,
                                onValueChange = { if (it.length <= 500) emergencyMessage = it }
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            // Location Sharing Card with live GPS status
                            LocationSharingCard(
                                checked = locationSharingEnabled,
                                onCheckedChange = { locationSharingEnabled = it },
                                lat = gpsLat,
                                lng = gpsLng,
                                accuracy = gpsAccuracy,
                                isFetching = gpsFetching
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            // Attach Information Section (Photo Only)
                            Text(
                                text = "ATTACH INFORMATION",
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            AttachButton(
                                modifier = Modifier.fillMaxWidth(),
                                icon = Icons.Outlined.PhotoCamera,
                                label = "Attach Photo"
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Optional. Keep attachments small for faster emergency delivery.",
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            // Emergency Transport Section
                            Text(
                                text = "EMERGENCY CHANNEL",
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            ActiveSosChannelCard(
                                activeMode = activeChannelMode,
                                onSwitchMode = {
                                    val newMode = if (activeChannelMode == com.example.zerogrid.mesh.engine.MeshChannelMode.BLE) {
                                        com.example.zerogrid.mesh.engine.MeshChannelMode.WIFI_DIRECT
                                    } else {
                                        com.example.zerogrid.mesh.engine.MeshChannelMode.BLE
                                    }
                                    meshEngine.setMeshChannelMode(newMode)
                                }
                            )
                            Spacer(modifier = Modifier.height(24.dp))

                            // Broadcast SOS Action Button
                            Button(
                                onClick = {
                                    if (isSubmitting) return@Button
                                    isSubmitting = true
                                    coroutineScope.launch {
                                        try {
                                            // Acquire fresh GPS right before dispatch so coords are up-to-date
                                            val (lat, lng, accuracy) = if (locationSharingEnabled) {
                                                val r = LocationHelper.getCurrentLocation(context)
                                                Triple(r?.lat, r?.lng, r?.accuracy)
                                            } else {
                                                Triple(null, null, null)
                                            }
                                            sosDispatcher.triggerSos(
                                                lat = lat,
                                                lng = lng,
                                                accuracy = accuracy,
                                                category = selectedType,
                                                message = emergencyMessage,
                                                waterDepthCm = if (isWaterCategory) waterDepthCm else 0,
                                                passability = if (isWaterCategory) selectedPassability else "ALL_PASSABLE"
                                            )
                                        } finally {
                                            isSubmitting = false
                                        }
                                    }
                                    onNavigate(Screen.SOS_CENTER)
                                },
                                enabled = !isSubmitting,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isWaterCategory) Color(0xFF1E88E5) else colors.accentRed,
                                    disabledContainerColor = (if (isWaterCategory) Color(0xFF1E88E5) else colors.accentRed).copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isWaterCategory) "HAZARD" else "SOS",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isSubmitting) "DISPATCHING..." else if (isWaterCategory) "BROADCAST HAZARD" else "BROADCAST SOS",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            // Footer Status
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Encrypted  •  Local Mesh  •  Multi-Hop",
                                    color = colors.textSecondary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SendSosTopBar(onBackClick: () -> Unit = {}) {
    val colors = ZeroGridTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = BadgeGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Send SOS",
                    color = colors.textPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Surface(
                color = colors.surfaceNested,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, colors.divider)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = BadgeGreen,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ENCRYPTED",
                        color = BadgeGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
        HorizontalDivider(color = colors.divider, thickness = 1.dp)
    }
}

@Composable
private fun EmergencyBroadcastWarningCard() {
    val colors = ZeroGridTheme.colors
    var isExpanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "chevron_rotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.accentRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = colors.accentRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Emergency Broadcast",
                        color = colors.accentRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    if (!isExpanded) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "• Tap for info",
                            color = colors.textSecondary.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = colors.accentRed,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(rotationAngle)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    HorizontalDivider(
                        color = colors.accentRed.copy(alpha = 0.2f),
                        thickness = 1.dp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "This message will be broadcast to reachable ZeroGrid devices and may be relayed across the mesh.",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.WifiOff,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "No internet connection required.",
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmergencyTypeChipsRow(selected: String, onSelected: (String) -> Unit) {
    val colors = ZeroGridTheme.colors
    val types = listOf(
        "WATERLOGGING" to "🌊 Waterlogging",
        "SUBMERGED_UNDERPASS" to "🚗 Submerged Pass",
        "DRAINAGE_OVERFLOW" to "🌀 Drain Overflow",
        "HEATWAVE" to "🌡️ Heatwave Alert",
        "FALLEN_GRID" to "⚡ Fallen Grid Line",
        "MEDICAL" to "🚨 Medical SOS"
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        types.chunked(2).forEach { rowPair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowPair.forEach { (categoryKey, displayLabel) ->
                    val isSelected = categoryKey == selected
                    Button(
                        onClick = { onSelected(categoryKey) },
                        modifier = Modifier
                            .height(44.dp)
                            .weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) colors.primary else colors.cardBackground,
                            contentColor = if (isSelected) (if (colors.isDark) Color.Black else Color.White) else colors.textPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = if (!isSelected) BorderStroke(1.dp, colors.divider) else null,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = displayLabel,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WaterDepthCard(
    waterDepthCm: Int,
    onDepthChange: (Int) -> Unit
) {
    val colors = ZeroGridTheme.colors
    val (statusColor, statusLabel) = when {
        waterDepthCm >= 90 -> Pair(colors.accentRed, "CRITICAL: SUBMERGED")
        waterDepthCm >= 45 -> Pair(Color(0xFFFF9800), "DANGER: HIGH WATER")
        waterDepthCm >= 15 -> Pair(Color(0xFFFFC107), "CAUTION: WATERLOGGED")
        else -> Pair(Color(0xFF4CAF50), "MINOR: NORMAL RUNOFF")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, colors.divider)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "WATER DEPTH (CM)",
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = statusLabel,
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "$waterDepthCm cm",
                        color = statusColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Slider(
                value = waterDepthCm.toFloat(),
                onValueChange = { onDepthChange(it.toInt()) },
                valueRange = 0f..150f,
                steps = 29,
                colors = SliderDefaults.colors(
                    thumbColor = statusColor,
                    activeTrackColor = statusColor,
                    inactiveTrackColor = colors.divider
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0 cm", fontSize = 10.sp, color = colors.textSecondary, fontFamily = FontFamily.Monospace)
                Text("15 cm (Yellow)", fontSize = 10.sp, color = Color(0xFFFFC107), fontFamily = FontFamily.Monospace)
                Text("45 cm (Orange)", fontSize = 10.sp, color = Color(0xFFFF9800), fontFamily = FontFamily.Monospace)
                Text("90+ cm (Red)", fontSize = 10.sp, color = colors.accentRed, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun PassabilitySelectorCard(
    selectedPassability: String,
    onPassabilityChange: (String) -> Unit
) {
    val colors = ZeroGridTheme.colors
    val options = listOf(
        "ALL_PASSABLE" to "All Passable",
        "TWO_WHEELER_ONLY" to "2-Wheeler Only",
        "FOUR_WHEELER_ONLY" to "SUV / 4WD Only",
        "IMPASSABLE" to "⛔ Impassable"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, colors.divider)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "ROAD PASSABILITY CONDITION",
                color = colors.textSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.chunked(2).forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowOptions.forEach { (key, label) ->
                            val isSelected = selectedPassability == key
                            val itemColor = if (key == "IMPASSABLE") colors.accentRed else colors.primary
                            Button(
                                onClick = { onPassabilityChange(key) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) itemColor else colors.cardBackground,
                                    contentColor = if (isSelected) (if (colors.isDark && itemColor != colors.accentRed) Color.Black else Color.White) else colors.textPrimary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (isSelected) itemColor else colors.divider),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmergencyMessageInput(value: String, onValueChange: (String) -> Unit) {
    val colors = ZeroGridTheme.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = "Describe the emergency and what assistance is needed...",
                color = colors.textSecondary,
                fontSize = 14.sp
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = colors.cardBackground,
            unfocusedContainerColor = colors.cardBackground,
            disabledContainerColor = colors.cardBackground,
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.divider,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary
        )
    )
}

@Composable
private fun LocationSharingCard(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    lat: Double? = null,
    lng: Double? = null,
    accuracy: Float? = null,
    isFetching: Boolean = false
) {
    val colors = ZeroGridTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, colors.divider)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = if (checked && lat != null) BadgeGreen else colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Location sharing",
                            color = colors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        when {
                            !checked -> Text(
                                text = "Disabled — will not share location",
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            isFetching -> Text(
                                text = "Acquiring GPS fix...",
                                color = Color(0xFFFBBF24),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            lat != null && lng != null -> Text(
                                text = "%.5f, %.5f %s".format(
                                    lat, lng,
                                    if (accuracy != null) "(±${accuracy.toInt()}m)" else ""
                                ),
                                color = BadgeGreen,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium
                            )
                            else -> Text(
                                text = "GPS unavailable — will retry on send",
                                color = colors.accentRed,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = if (colors.isDark) Color.Black else Color.White,
                        checkedTrackColor = BadgeGreen,
                        uncheckedThumbColor = colors.textSecondary,
                        uncheckedTrackColor = colors.surfaceNested,
                        uncheckedBorderColor = Color.Transparent
                    )
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Share your current location with this SOS. Location will be included only with this emergency broadcast.",
                color = colors.textSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { /* Approximate location settings */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceNested),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, colors.divider)
            ) {
                Text(
                    text = "Use approximate location",
                    color = colors.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun AttachButton(modifier: Modifier = Modifier, icon: ImageVector, label: String) {
    val colors = ZeroGridTheme.colors
    Card(
        modifier = modifier.height(60.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, colors.divider)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ActiveSosChannelCard(
    activeMode: com.example.zerogrid.mesh.engine.MeshChannelMode,
    onSwitchMode: () -> Unit
) {
    val colors = ZeroGridTheme.colors
    val isBle = activeMode == com.example.zerogrid.mesh.engine.MeshChannelMode.BLE
    val channelIcon = if (isBle) Icons.Outlined.Bluetooth else Icons.Outlined.Wifi
    val nextModeName = if (isBle) "Wi-Fi Direct" else "BLE"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, colors.divider)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(colors.surfaceNested, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(channelIcon, null, tint = colors.primary, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = activeMode.displayName,
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Exclusively broadcasting across this radio",
                        color = colors.textSecondary,
                        fontSize = 11.sp
                    )
                }
            }
            OutlinedButton(
                onClick = onSwitchMode,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.6f)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("Switch to $nextModeName", color = colors.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ZeroGridSendSosScreen() = SendSosScreen()

@Preview(showBackground = true)
@Composable
fun ZeroGridSendSosPreview() {
    ZeroGridTheme {
        ZeroGridSendSosScreen()
    }
}