package com.example.zerogrid.mesh

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.zerogrid.mesh.engine.MeshEngine
import com.example.zerogrid.mesh.engine.MeshNode
import com.example.zerogrid.navigation.Screen
import com.example.zerogrid.navigation.ZeroGridBottomBar
import com.example.zerogrid.ui.theme.*

@Composable
fun NetworkStatusScreen(onNavigate: (Screen) -> Unit = {}) {
    val meshEngine = MeshEngine.getInstance(LocalContext.current)
    val peers by meshEngine.connectedPeers.collectAsState()
    val isMeshActive by meshEngine.isMeshActive.collectAsState()
    val activeChannelMode by meshEngine.activeChannelMode.collectAsState()
    val receivedMessages by meshEngine.receivedMessages.collectAsState()
    val sosAlerts by meshEngine.sosAlerts.collectAsState()
    val packetsRelayedCount by meshEngine.packetsRelayedCount.collectAsState()

    val directPeers = peers.count { it.hopDistance == 1 }
    val relayedPeers = peers.count { it.hopDistance > 1 }
    val totalRouted = packetsRelayedCount + receivedMessages.size + sosAlerts.size

    Scaffold(
        containerColor = DarkBackground,
        topBar = { NetworkStatusTopBar(onBackClick = { onNavigate(Screen.MESH) }) },
        bottomBar = { ZeroGridBottomBar(currentScreen = Screen.MESH, onNavigate = onNavigate) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            NetworkHealthCard(
                isMeshActive = isMeshActive,
                activeChannel = activeChannelMode.label,
                nodeCount = peers.size + 1
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "NETWORK METRICS",
                color = TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Connected Nodes",
                    value = (peers.size + 1).toString(),
                    subtext = "${directPeers} direct • ${relayedPeers} relayed",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Max Hop Reach",
                    value = (peers.maxOfOrNull { it.hopDistance } ?: 1).toString(),
                    subtext = "Mesh radius limit: 5",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Packets Processed",
                    value = totalRouted.toString(),
                    subtext = "${receivedMessages.size} rx • ${packetsRelayedCount} relay • ${sosAlerts.size} SOS",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Active Channel",
                    value = activeChannelMode.label,
                    subtext = if (activeChannelMode == com.example.zerogrid.mesh.engine.MeshChannelMode.BLE) "Low-power (~2 KB/s)" else "High-speed (~10 MB/s)",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "ACTIVE TRANSPORTS (SINGLE-RADIO EXCLUSIVE)",
                color = TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            val isBleActive = isMeshActive && activeChannelMode == com.example.zerogrid.mesh.engine.MeshChannelMode.BLE
            val isWifiActive = isMeshActive && activeChannelMode == com.example.zerogrid.mesh.engine.MeshChannelMode.WIFI_DIRECT

            TransportStatusCard(
                name = "Bluetooth Low Energy (BLE)",
                status = if (!isMeshActive) "Offline" else if (isBleActive) "Advertising & Scanning (Active)" else "Standby (Single-Radio Policy)",
                details = if (isBleActive) "Frequency: 2.4 GHz • GATT Mesh Server Active" else "Radio standby • Conflict prevention policy",
                isActive = isBleActive
            )

            Spacer(modifier = Modifier.height(10.dp))

            TransportStatusCard(
                name = "Wi-Fi Direct (P2P)",
                status = if (!isMeshActive) "Offline" else if (isWifiActive) "P2P Discovery & Server Active" else "Standby (Single-Radio Policy)",
                details = if (isWifiActive) "Band: 2.4/5 GHz • TCP Server: port 8888" else "Radio standby • Conflict prevention policy",
                isActive = isWifiActive
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun NetworkStatusTopBar(onBackClick: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Network Topology & Status",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        HorizontalDivider(color = DividerColor, thickness = 1.dp)
    }
}

@Composable
private fun NetworkHealthCard(isMeshActive: Boolean, activeChannel: String, nodeCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(if (isMeshActive) StatusActive else AlertPink, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isMeshActive) "MESH OPERATIONAL" else "MESH OFFLINE",
                        color = if (isMeshActive) StatusActive else AlertPink,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = activeChannel,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (isMeshActive) "$nodeCount nodes in local reachability cluster" else "Radio transports stopped",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, subtext: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, color = TextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, color = StatusActive, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtext, color = TextPrimary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun TransportStatusCard(name: String, status: String, details: String, isActive: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .background(if (isActive) StatusActive else TextSecondary, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isActive) "ACTIVE" else "INACTIVE",
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = status, color = StatusActive, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = details, color = TextSecondary, fontSize = 11.sp)
        }
    }
}
