package com.example.zerogrid.home

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zerogrid.location.LocationHelper
import com.example.zerogrid.mesh.engine.MeshChannelMode
import com.example.zerogrid.mesh.engine.MeshEngine
import com.example.zerogrid.mesh.engine.MeshNode
import com.example.zerogrid.navigation.Screen
import com.example.zerogrid.ui.components.ProximityWarningBanner
import com.example.zerogrid.ui.components.ZeroGridTopBar
import com.example.zerogrid.ui.theme.BadgeGreen
import com.example.zerogrid.ui.theme.ZeroGridTheme

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun MeshDashboardScreen(
    onNavigate: (Screen) -> Unit = {},
    onOpenPeerChat: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val meshEngine = remember { MeshEngine.getInstance(context) }
    val rawPeers by meshEngine.connectedPeers.collectAsState()
    val isMeshActive by meshEngine.isMeshActive.collectAsState()
    val activeChannelMode by meshEngine.activeChannelMode.collectAsState()
    val proximityWarning by meshEngine.proximityWarning.collectAsState()
    val peers = remember(rawPeers, activeChannelMode) {
        rawPeers.filter { it.transportType == activeChannelMode.transportName }
    }
    val colors = ZeroGridTheme.colors

    // Periodically update user location for real-time proximity alerts
    LaunchedEffect(Unit) {
        while (true) {
            try {
                val loc = LocationHelper.getCurrentLocation(context)
                if (loc != null) {
                    meshEngine.updateUserLocation(loc.lat, loc.lng)
                }
            } catch (_: Exception) {}
            kotlinx.coroutines.delay(10_000L)
        }
    }

    val batteryPercent by produceState(initialValue = 100, context) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            getBatteryPercentage(context)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            ZeroGridTopBar(
                peerCount = peers.size,
                isMeshActive = isMeshActive,
                onProfileClick = { onNavigate(Screen.PROFILE) }
            )
        }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val isTablet = maxWidth >= 600.dp
            val horizontalPadding = if (isTablet) 32.dp else 16.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Scrollable Content Area (Takes up all remaining space)
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp) // Added bottom padding to prevent sticking to the SOS button
                ) {
                    item(key = "proximity_warning_banner") {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 840.dp)
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            ProximityWarningBanner(warning = proximityWarning)
                        }
                    }

                    item(key = "hero_mesh_and_devices", contentType = "HeroSection") {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 840.dp)
                                .fillMaxWidth()
                        ) {
                            MeshActiveAndDevicesSection(
                                peers = peers,
                                relayedPeersCount = peers.count { it.hopDistance > 1 },
                                activeChannelMode = activeChannelMode,
                                batteryPercent = batteryPercent,
                                onScanClick = { onNavigate(Screen.MESH) },
                                onSelectChannel = { meshEngine.setMeshChannelMode(it) },
                                onOpenChat = onOpenPeerChat,
                                onViewAllClick = { onNavigate(Screen.MESH) }
                            )
                        }
                    }
                }

                // 2. Fixed Bottom SOS Button Area (Permanently visible)
                Box(
                    modifier = Modifier
                        .widthIn(max = 840.dp)
                        .fillMaxWidth()
                        .padding(bottom = 24.dp, top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = { onNavigate(Screen.SOS_CENTER) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accentRed,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Campaign,
                            contentDescription = "Send SOS",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SEND SOS",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MeshActiveAndDevicesSection(
    peers: List<MeshNode>,
    relayedPeersCount: Int,
    activeChannelMode: MeshChannelMode,
    batteryPercent: Int,
    onScanClick: () -> Unit,
    onSelectChannel: (MeshChannelMode) -> Unit,
    onOpenChat: (String) -> Unit,
    onViewAllClick: () -> Unit
) {
    val colors = ZeroGridTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // --- TOP SECTION: MESH STATUS (Borderless) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = "Mesh Active & Connected",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Off-grid direct communication",
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
            }

            Surface(
                color = BadgeGreen.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.BatteryFull,
                        contentDescription = "Battery",
                        tint = BadgeGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$batteryPercent%",
                        color = BadgeGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HeroStatPill(
                modifier = Modifier.weight(1f),
                label = "Peers",
                value = "${peers.size} Active",
                valueColor = colors.textPrimary
            )
            HeroStatPill(
                modifier = Modifier.weight(1f),
                label = "Reach",
                value = if (relayedPeersCount > 0) "Direct & Relay" else if (peers.isNotEmpty()) "Direct Only" else "Scanning",
                valueColor = colors.textPrimary
            )
            HeroStatPill(
                modifier = Modifier.weight(1f),
                label = "Mode",
                value = activeChannelMode.label,
                valueColor = colors.primary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Integrated Radio Channel Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surfaceNested, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Radio Channel: ${activeChannelMode.label}",
                color = colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedButton(
                onClick = {
                    val next = if (activeChannelMode == MeshChannelMode.BLE) MeshChannelMode.WIFI_DIRECT else MeshChannelMode.BLE
                    onSelectChannel(next)
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text(
                    text = if (activeChannelMode == MeshChannelMode.BLE) "Switch to Wi-Fi" else "Switch to BLE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- BOTTOM SECTION: DEVICE DISCOVERY CONTAINER (Outlined Box) ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(colors.divider)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Inside Container: 1. Scan Button
                Button(
                    onClick = onScanClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = if (colors.isDark) Color.Black else Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Scan for Devices",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Inside Container: 2. Devices List (Max 3)
                if (peers.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Scanning Mesh Radios...",
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Devices in range will appear here.",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // Strictly limit to 3 items
                    peers.take(3).forEach { peer ->
                        Box(modifier = Modifier.padding(vertical = 4.dp)) {
                            NearbyPeerItem(
                                peer = peer,
                                onOpenChat = { onOpenChat(peer.nodeId) },
                                onCardClick = onViewAllClick
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Inside Container: 3. View All Button
                    TextButton(
                        onClick = onViewAllClick,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Text(
                            text = "View All (${peers.size})",
                            color = colors.primary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroStatPill(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: Color
) {
    val colors = ZeroGridTheme.colors

    Surface(
        modifier = modifier,
        color = colors.surfaceNested,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Text(
                text = label,
                color = colors.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = valueColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun NearbyPeerItem(
    peer: MeshNode,
    onOpenChat: () -> Unit,
    onCardClick: () -> Unit
) {
    val colors = ZeroGridTheme.colors
    val initials = if (peer.alias.length >= 2) peer.alias.take(2).uppercase() else "ZG"

    Surface(
        onClick = onCardClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = colors.surfaceNested
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(colors.primary.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            color = colors.primary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(BadgeGreen, CircleShape)
                            .border(2.dp, colors.surfaceNested, CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = peer.alias,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = colors.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (peer.hopDistance <= 1) "Direct • Strong Signal" else "Relayed via ${peer.hopDistance} hops",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }
            }

            IconButton(
                onClick = onOpenChat,
                modifier = Modifier
                    .size(36.dp)
                    .background(colors.cardBackground, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = "Chat with peer",
                    tint = colors.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun getBatteryPercentage(context: Context): Int {
    return try {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        if (level >= 0 && scale > 0) {
            (level * 100 / scale.toFloat()).toInt()
        } else {
            88
        }
    } catch (e: Exception) {
        88
    }
}