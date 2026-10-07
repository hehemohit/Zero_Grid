package com.example.zerogrid.mesh

import androidx.compose.material.icons.automirrored.outlined.AltRoute

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.zerogrid.mesh.engine.MeshEngine
import com.example.zerogrid.mesh.engine.MeshNode
import com.example.zerogrid.navigation.Screen
import com.example.zerogrid.navigation.ZeroGridBottomBar
import com.example.zerogrid.ui.theme.ZeroGridTheme

@Composable
fun NearbyDevicesScreen(
    onNavigate: (Screen) -> Unit = {},
    onBackClick: () -> Unit = { onNavigate(Screen.HOME) },
    onOpenPeerDetails: (String) -> Unit = {},
    onOpenPeerChat: (String) -> Unit = {}
) {
    val colors = ZeroGridTheme.colors
    var selectedFilter by remember { mutableStateOf("All") }
    val meshEngine = MeshEngine.getInstance(LocalContext.current)
    val rawPeers by meshEngine.connectedPeers.collectAsState()
    val activeChannelMode by meshEngine.activeChannelMode.collectAsState()
    val peers = remember(rawPeers, activeChannelMode) {
        rawPeers.filter { it.transportType == activeChannelMode.transportName }
    }

    val filteredPeers = remember(peers, selectedFilter) {
        when (selectedFilter) {
            "Direct" -> peers.filter { it.hopDistance == 1 }
            "2 Hops" -> peers.filter { it.hopDistance == 2 }
            "Relay" -> peers.filter { it.hopDistance > 1 }
            else -> peers
        }
    }

    Scaffold(
        containerColor = Color.Transparent, // Overdraw elimination: root Scaffold owns background
        topBar = { NearbyTopBar(onBackClick = onBackClick) },
        bottomBar = { ZeroGridBottomBar(currentScreen = Screen.MESH, onNavigate = onNavigate) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "discovery_card") {
                MeshDiscoverySubheader(peers.size)
            }
            item(key = "radar_card") {
                RadarGraphicCard(peersCount = peers.size)
            }
            item(key = "filter_chips") {
                FilterChipsRow(selected = selectedFilter, onSelected = { selectedFilter = it })
            }
            if (filteredPeers.isEmpty()) {
                item(key = "empty_devices") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.Devices,
                                contentDescription = null,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "No mesh devices discovered yet", color = colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Bring another ZeroGrid device closer or ensure radio is enabled.", color = colors.textSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            } else {
                items(filteredPeers, key = { it.nodeId }, contentType = { "PeerDevice" }) { peer ->
                    DeviceCard(
                        icon = if (peer.transportType == MeshNode.TRANSPORT_BLE) Icons.Outlined.Bluetooth else Icons.Outlined.Wifi,
                        name = peer.alias,
                        status = if (peer.hopDistance == 1) "Direct • ${peer.transportType}" else "${peer.hopDistance} hops via Mesh",
                        subStatus = "Node ID: ${peer.nodeId.takeLast(6)}",
                        signalBars = if (peer.rssi > -60) 4 else if (peer.rssi > -80) 2 else 1,
                        onViewClick = { onOpenPeerDetails(peer.nodeId) },
                        onChatClick = { onOpenPeerChat(peer.nodeId) }
                    )
                }
            }
            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun NearbyTopBar(onBackClick: () -> Unit = {}) {
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Nearby Devices",
                color = colors.primary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
        HorizontalDivider(color = colors.divider, thickness = 1.dp)
    }
}

@Composable
private fun MeshDiscoverySubheader(devicesFound: Int) {
    val colors = ZeroGridTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(colors.primary, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Mesh Discovery Active",
                color = colors.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            text = "$devicesFound ${if (devicesFound == 1) "device" else "devices"} found",
            color = colors.textSecondary,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun RadarGraphicCard(peersCount: Int) {
    val colors = ZeroGridTheme.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                drawCircle(color = colors.surfaceNested, radius = 40.dp.toPx(), center = center)
                drawCircle(color = colors.divider, radius = 65.dp.toPx(), center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f))
                drawCircle(color = colors.divider.copy(alpha = 0.5f), radius = 90.dp.toPx(), center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f))

                // Draw blips for connected peers
                if (peersCount > 0) {
                    val radiusPx = 65.dp.toPx()
                    val angleStep = (2 * Math.PI) / peersCount
                    for (i in 0 until peersCount) {
                        val angle = i * angleStep
                        val x = center.x + (radiusPx * Math.cos(angle)).toFloat()
                        val y = center.y + (radiusPx * Math.sin(angle)).toFloat()
                        drawCircle(color = colors.primary, radius = 5.dp.toPx(), center = Offset(x, y))
                    }
                }
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(colors.surfaceNested, CircleShape)
                    .border(1.dp, if (peersCount > 0) colors.primary else colors.textSecondary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = "Device",
                    tint = if (peersCount > 0) colors.primary else colors.textSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun FilterChipsRow(selected: String, onSelected: (String) -> Unit) {
    val colors = ZeroGridTheme.colors
    val filters = listOf("All", "Direct", "2 Hops", "Relay")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { filter ->
            val isSelected = filter == selected
            Button(
                onClick = { onSelected(filter) },
                modifier = Modifier
                    .height(36.dp)
                    .weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) colors.primary else colors.cardBackground,
                    contentColor = if (isSelected) (if (colors.isDark) Color.Black else Color.White) else colors.textSecondary
                ),
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = filter,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun DevicesListSection(
    peers: List<MeshNode>,
    onOpenPeerDetails: (String) -> Unit,
    onOpenPeerChat: (String) -> Unit
) {
    val colors = ZeroGridTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (peers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Devices,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "No mesh devices discovered yet", color = colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Bring another ZeroGrid device closer or ensure radio is enabled.", color = colors.textSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }
        } else {
            peers.forEach { peer ->
                DeviceCard(
                    icon = if (peer.transportType == MeshNode.TRANSPORT_BLE) Icons.Outlined.Bluetooth else Icons.Outlined.Wifi,
                    name = peer.alias,
                    status = if (peer.hopDistance == 1) "Direct • ${peer.transportType}" else "${peer.hopDistance} hops via Mesh",
                    subStatus = "Node ID: ${peer.nodeId.takeLast(6)}",
                    signalBars = if (peer.rssi > -60) 4 else if (peer.rssi > -80) 2 else 1,
                    onViewClick = { onOpenPeerDetails(peer.nodeId) },
                    onChatClick = { onOpenPeerChat(peer.nodeId) }
                )
            }
        }
    }
}

@Composable
private fun DeviceCard(
    icon: ImageVector,
    name: String,
    status: String,
    subStatus: String? = null,
    signalBars: Int? = null,
    onViewClick: () -> Unit,
    onChatClick: () -> Unit
) {
    val colors = ZeroGridTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = name, color = colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = status, color = colors.textSecondary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)

                    if (signalBars != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            for (i in 1..4) {
                                Box(
                                    modifier = Modifier
                                        .width(10.dp)
                                        .height(4.dp)
                                        .background(
                                            if (i <= signalBars) colors.primary else colors.surfaceNested,
                                            RoundedCornerShape(2.dp)
                                        )
                                )
                            }
                        }
                    }

                    if (subStatus != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = subStatus, color = colors.textSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onViewClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.divider),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(text = "View", fontSize = 13.sp)
                }

                Button(
                    onClick = onChatClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = if (colors.isDark) Color.Black else Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(text = "Chat", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ZeroGridNearbyDevicesScreen() = NearbyDevicesScreen()

@Preview(showBackground = true)
@Composable
fun ZeroGridNearbyDevicesPreview() {
    ZeroGridTheme {
        ZeroGridNearbyDevicesScreen()
    }
}