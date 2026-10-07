package com.example.zerogrid.mesh

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zerogrid.mesh.engine.MeshEngine
import com.example.zerogrid.mesh.engine.MeshNode
import com.example.zerogrid.navigation.Screen
import com.example.zerogrid.navigation.ZeroGridBottomBar
import com.example.zerogrid.ui.theme.*

@Composable
fun PeerDetailsScreen(
    peerId: String = "",
    onNavigate: (Screen) -> Unit = {},
    onOpenPeerChat: (String) -> Unit = {},
    onBackClick: () -> Unit = { onNavigate(Screen.MESH) }
) {
    var trustDevice by remember { mutableStateOf(true) }
    val meshEngine = MeshEngine.getInstance(LocalContext.current)
    val peers by meshEngine.connectedPeers.collectAsState()
    val activeChannelMode by meshEngine.activeChannelMode.collectAsState()

    val peer = remember(peers, peerId) { peers.find { it.nodeId == peerId } }
    val peerAlias = peer?.alias ?: if (peerId.isNotEmpty()) "Node-${peerId.takeLast(6)}" else "Direct Mesh Node"
    val isConnected = peer != null
    val hopDistance = peer?.hopDistance ?: 1
    val transportType = peer?.transportType ?: activeChannelMode.label
    val rssi = peer?.rssi ?: -70
    val signalBars = if (rssi > -60) 4 else if (rssi > -80) 3 else 1
    val deviceIdString = if (peerId.isNotEmpty()) "ZG-${peerId.takeLast(6).uppercase()}" else "ZG-LOCAL"
    val fingerprintString = if (peerId.isNotEmpty()) peerId.chunked(2).take(4).joinToString(":") { it.uppercase() } else "DIRECT-LINK"

    Scaffold(
        containerColor = DarkBackground,
        topBar = { PeerDetailsTopBar(onBackClick = onBackClick) },
        bottomBar = { ZeroGridBottomBar(currentScreen = Screen.MESH, onNavigate = onNavigate) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            PeerHeaderSection(
                alias = peerAlias,
                isConnected = isConnected,
                hopDistance = hopDistance,
                transportType = transportType
            )
            Spacer(modifier = Modifier.height(24.dp))
            ActionButtonsRow(
                onMessageClick = {
                    if (peerId.isNotEmpty()) onOpenPeerChat(peerId)
                    else onNavigate(Screen.MESSAGES)
                }
            )
            Spacer(modifier = Modifier.height(24.dp))
            ConnectionInfoCard(
                transport = transportType,
                isConnected = isConnected,
                signalBars = signalBars,
                hopDistance = hopDistance,
                peerId = peerId
            )
            Spacer(modifier = Modifier.height(20.dp))
            RouteToPeerCard(
                alias = peerAlias,
                hopDistance = hopDistance,
                isConnected = isConnected
            )
            Spacer(modifier = Modifier.height(20.dp))
            IdentitySecurityCard(
                deviceId = deviceIdString,
                fingerprint = fingerprintString
            )
            Spacer(modifier = Modifier.height(16.dp))
            TrustDeviceCard(checked = trustDevice, onCheckedChange = { trustDevice = it })
            Spacer(modifier = Modifier.height(16.dp))
            Spacer(modifier = Modifier.height(8.dp))
            MenuNavigationItem(
                icon = Icons.Outlined.DeleteOutline,
                title = "Back to Network",
                titleColor = TextSecondary,
                showIcon = false,
                onClick = onBackClick
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PeerDetailsTopBar(onBackClick: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkBackground)
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
                        tint = StatusActive,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Peer Details",
                    color = StatusActive,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = "More",
                tint = StatusActive,
                modifier = Modifier.size(24.dp)
            )
        }
        HorizontalDivider(color = DividerColor, thickness = 1.dp)
    }
}

@Composable
private fun PeerHeaderSection(
    alias: String,
    isConnected: Boolean,
    hopDistance: Int,
    transportType: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(SurfaceDarker, CircleShape)
                .border(1.dp, if (isConnected) StatusActive else TextSecondary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Router,
                contentDescription = "Device",
                tint = if (isConnected) StatusActive else TextSecondary,
                modifier = Modifier.size(36.dp)
            )
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(if (isConnected) StatusActive else TextSecondary, CircleShape)
                    .align(Alignment.BottomEnd)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = alias,
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(6.dp).background(if (isConnected) StatusActive else TextSecondary, CircleShape))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isConnected) "Connected • ${if (hopDistance == 1) "Direct Link" else "$hopDistance hops away"}" else "Standby / Searching",
                color = if (isConnected) StatusActive else TextSecondary,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "ZeroGrid Node ($transportType)",
            color = TextSecondary,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .background(Color(0xFF1A3B40), RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Shield,
                contentDescription = "Secure",
                tint = StatusActive,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "End-to-End Encrypted",
                color = StatusActive,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ActionButtonsRow(
    onMessageClick: () -> Unit
) {
    Button(
        onClick = onMessageClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = StatusActive, contentColor = Color.Black),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(imageVector = Icons.Outlined.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "Message", fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ConnectionInfoCard(
    transport: String,
    isConnected: Boolean,
    signalBars: Int,
    hopDistance: Int,
    peerId: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "CONNECTION",
                color = TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            ConnectionRow("Transport", transport)
            HorizontalDivider(color = DividerColor, thickness = 1.dp)
            ConnectionRow("Status", if (isConnected) "Connected" else "Standby")
            HorizontalDivider(color = DividerColor, thickness = 1.dp)
            ConnectionSignalRow("Signal", if (isConnected) (if (signalBars >= 3) "Strong" else "Stable") else "N/A", signalBars)
            HorizontalDivider(color = DividerColor, thickness = 1.dp)
            ConnectionRow("Hop Distance", if (isConnected) "$hopDistance hop(s)" else "N/A")
            HorizontalDivider(color = DividerColor, thickness = 1.dp)
            ConnectionRow("Link Type", if (hopDistance == 1) "Direct P2P Link" else "Multi-hop Relay")
            HorizontalDivider(color = DividerColor, thickness = 1.dp)
            ConnectionRow("Node ID", if (peerId.isNotEmpty()) peerId.take(12) + "..." else "Standby", valueColor = StatusActive)
        }
    }
}

@Composable
private fun ConnectionRow(label: String, value: String, valueColor: Color = TextPrimary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextSecondary, fontSize = 14.sp)
        Text(text = value, color = valueColor, fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ConnectionSignalRow(label: String, value: String, signalBars: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextSecondary, fontSize = 14.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.padding(end = 8.dp)) {
                for (i in 1..4) {
                    Box(
                        modifier = Modifier
                            .width(8.dp)
                            .height(14.dp)
                            .background(
                                if (i <= signalBars) StatusActive else SurfaceDarker,
                                RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
            Text(text = value, color = TextPrimary, fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun RouteToPeerCard(
    alias: String,
    hopDistance: Int,
    isConnected: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = "ROUTE TO PEER",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (!isConnected) {
                Text(
                    text = "No active mesh route • Node is out of direct and relay range",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
            } else {
                // Route Graph Visual
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(SurfaceDarker, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Outlined.PhoneAndroid, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                    Text(text = "You", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 4.dp))

                    Box(modifier = Modifier.width(2.dp).height(24.dp).background(StatusActive))

                    if (hopDistance > 1) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(SurfaceDarker, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Outlined.Router, contentDescription = null, tint = StatusActive, modifier = Modifier.size(16.dp))
                        }
                        Text(text = "Relay (${hopDistance - 1} hops)", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 4.dp))

                        Box(modifier = Modifier.width(2.dp).height(24.dp).background(StatusActive))
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(SurfaceDarker, CircleShape)
                            .border(1.dp, StatusActive, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Outlined.Router, contentDescription = null, tint = StatusActive, modifier = Modifier.size(20.dp))
                    }
                    Text(text = alias, color = StatusActive, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (hopDistance == 1) "Direct peer-to-peer radio link established." else "Traffic forwarded through encrypted mesh relay nodes.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}

@Composable
private fun IdentitySecurityCard(
    deviceId: String,
    fingerprint: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "IDENTITY & SECURITY",
                color = TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Outlined.VerifiedUser, contentDescription = null, tint = StatusActive, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = "Peer Identity Verified", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Authenticated via ZeroGrid Keys", color = StatusActive, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = DividerColor, thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))
            ConnectionRow("Device ID", deviceId)
            ConnectionRow("Fingerprint", fingerprint)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Packets signed and validated with local cryptographic keys.",
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun TrustDeviceCard(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Trust this device", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Allow ZeroGrid to recognize this peer as a trusted device.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = StatusActive,
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = SurfaceDarker
                )
            )
        }
    }
}

@Composable
private fun MenuNavigationItem(
    icon: ImageVector,
    title: String,
    titleColor: Color,
    showIcon: Boolean = true,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = titleColor, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = title, color = titleColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
            if (showIcon) {
                Icon(imageVector = Icons.Outlined.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun ZeroGridPeerDetailsScreen() = PeerDetailsScreen()

@Preview(showBackground = true)
@Composable
fun ZeroGridPeerDetailsPreview() {
    ZeroGridTheme {
        ZeroGridPeerDetailsScreen()
    }
}