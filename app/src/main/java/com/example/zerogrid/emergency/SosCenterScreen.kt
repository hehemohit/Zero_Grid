package com.example.zerogrid.emergency

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.zerogrid.mesh.engine.MeshEngine
import com.example.zerogrid.mesh.engine.MeshPacket
import com.example.zerogrid.navigation.Screen
import com.example.zerogrid.ui.components.ProximityWarningBanner
import com.example.zerogrid.ui.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SosCenterScreen(
    onNavigate: (Screen) -> Unit = {},
    onTrackSos: ((Double, Double, String, String, Long) -> Unit)? = null
) {
    val meshEngine = MeshEngine.getInstance(LocalContext.current)
    val alerts by meshEngine.sosAlerts.collectAsState()
    val peers by meshEngine.connectedPeers.collectAsState()
    val acknowledgedIds by meshEngine.acknowledgedAlertIds.collectAsState()
    val proximityWarning by meshEngine.proximityWarning.collectAsState()
    val localNodeId = meshEngine.localNodeId
    val colors = ZeroGridTheme.colors

    // Derive the three alert buckets reactively
    val relativeAlerts = remember(alerts, acknowledgedIds) {
        alerts.filter { it.isCloudSos() && it.packetId !in acknowledgedIds && it.senderId != localNodeId }
    }
    val localMeshAlerts = remember(alerts, acknowledgedIds) {
        alerts.filter { !it.isCloudSos() && it.packetId !in acknowledgedIds && it.senderId != localNodeId }
    }
    val myActiveAlerts = remember(alerts, acknowledgedIds) {
        alerts.filter { it.senderId == localNodeId && it.packetId !in acknowledgedIds }
    }
    val acknowledgedAlerts = remember(alerts, acknowledgedIds) {
        alerts.filter { it.packetId in acknowledgedIds && it.senderId != localNodeId }
    }

    // Safety confirmation dialog state
    var pendingAckAlert by remember { mutableStateOf<MeshPacket?>(null) }

    pendingAckAlert?.let { alert ->
        val isCloud = alert.isCloudSos()
        val senderName = alert.getSosSenderName() ?: "Node-${alert.senderId.takeLast(4)}"
        AlertDialog(
            onDismissRequest = { pendingAckAlert = null },
            containerColor = colors.cardBackground,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = if (isCloud) "🛡️ Confirm Relative Safety" else "📍 Confirm Local Area",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = if (isCloud)
                        "Are you sure $senderName is safe? This will remove the alert from your active list and move it to acknowledged history."
                    else
                        "Are you sure the surrounding area and this local peer are attended to and safe?",
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isCloud) {
                            meshEngine.acknowledgeCloudSosAlert(alert.packetId, confirmedSafe = true)
                        } else {
                            meshEngine.acknowledgeSosAlert(alert.packetId)
                        }
                        pendingAckAlert = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCloud) colors.accentRed else Color(0xFF10B981)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isCloud) "Confirm Safe" else "Confirm & Dismiss",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingAckAlert = null }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    // Sync cloud SOS events from backend into local state on screen open
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val response = com.example.zerogrid.network.RetrofitInstance.sosApi.getActiveSos()
                if (response.isSuccessful) {
                    response.body()?.events?.forEach { ev ->
                        val coords = ev.location?.coordinates
                        if (coords != null && coords.size >= 2) {
                            meshEngine.recordExternalSosAlert(
                                sosId = ev.id,
                                senderName = ev.triggeredBy?.displayName ?: "Emergency Contact",
                                category = ev.category,
                                message = ev.message ?: "",
                                lat = coords[1],
                                lng = coords[0],
                                accuracy = ev.accuracyMeters
                            )
                            // If this SOS was already acknowledged by the current user on the backend,
                            // immediately mark it as acknowledged locally so it does not resurrect in the active list!
                            if (ev.isAcknowledgedByMe) {
                                meshEngine.acknowledgeSosAlert(ev.id)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SosCenterScreen", "Failed to sync active SOS from cloud", e)
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { EmergencyTopBar() },
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val isTablet = maxWidth >= 600.dp
            val horizontalPadding = if (isTablet) 32.dp else 20.dp

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // ── 1. EMERGENCY SOS BUTTON (Sticky Header) ─────────────────
                stickyHeader(key = "emergency_sos_button") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.background) // Prevents items from bleeding through when scrolling
                            .padding(top = 4.dp, bottom = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
                            EmergencySosStickyButton(onSendSosClick = { onNavigate(Screen.SEND_SOS) })
                        }
                    }
                }

                // ── 1.25 PROXIMITY WARNING BANNER ───────────────────────────
                item(key = "proximity_warning_banner") {
                    Box(modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
                        ProximityWarningBanner(warning = proximityWarning)
                    }
                }

                // ── 1.5 EMERGENCY SOS INFO CARD (Scrolls under the button) ──
                item(key = "emergency_sos_info") {
                    Box(modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
                        EmergencySosInfoCard()
                    }
                }

                // ── 2. EMERGENCY CONTACTS QUICK ACCESS ─────────
                item(key = "emergency_contacts_quick") {
                    Box(modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onNavigate(Screen.EMERGENCY_CONTACTS) },
                            colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, colors.divider)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(38.dp).background(colors.primary.copy(alpha = 0.12f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                    Icon(imageVector = Icons.Outlined.ContactPhone, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Emergency Contacts", color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Manage trusted contacts for SOS dispatch", color = colors.textSecondary, fontSize = 12.sp)
                                }
                                Icon(imageVector = Icons.Outlined.ChevronRight, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                item(key = "mesh_status_banner") {
                    Box(modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
                        MeshStatusBanner(peers.size)
                    }
                }

                // ── 3. FAMILY & RELATIVE EMERGENCY SOS ─────────
                item(key = "relative_sos_header") {
                    Box(modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
                        SosSectionHeader(
                            label = "FAMILY & RELATIVE EMERGENCY SOS",
                            count = relativeAlerts.size,
                            dotColor = colors.accentRed,
                            isActive = relativeAlerts.isNotEmpty()
                        )
                    }
                }
                item(key = "relative_sos_content") {
                    Box(modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
                        if (relativeAlerts.isEmpty()) {
                            SosAllClearCard(
                                message = "No active family alerts",
                                subtitle = "Family & relative emergencies appear here"
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                relativeAlerts.forEach { alert ->
                                    SosAlertCard(
                                        alert = alert,
                                        sectionType = SosType.RELATIVE,
                                        onAcknowledgeClick = { pendingAckAlert = alert },
                                        onTrackSos = onTrackSos
                                    )
                                }
                            }
                        }
                    }
                }

                // ── 4. LOCAL AREA MESH SOS ─────────────────────────
                item(key = "local_sos_header") {
                    Box(modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
                        SosSectionHeader(
                            label = "LOCAL AREA MESH SOS",
                            count = localMeshAlerts.size,
                            dotColor = Color(0xFFF59E0B),
                            isActive = localMeshAlerts.isNotEmpty()
                        )
                    }
                }
                item(key = "local_sos_content") {
                    Box(modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
                        if (localMeshAlerts.isEmpty()) {
                            SosAllClearCard(
                                message = "No active local mesh alerts",
                                subtitle = "Nearby peer SOS beacons appear here"
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                localMeshAlerts.forEach { alert ->
                                    SosAlertCard(
                                        alert = alert,
                                        sectionType = SosType.LOCAL_MESH,
                                        onAcknowledgeClick = { pendingAckAlert = alert },
                                        onTrackSos = onTrackSos
                                    )
                                }
                            }
                        }
                    }
                }

                // ── 5. MY OWN ACTIVE BROADCASTS ────────────────────────────────
                if (myActiveAlerts.isNotEmpty()) {
                    item(key = "my_sos_header") {
                        Box(modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
                            SosSectionHeader(
                                label = "MY ACTIVE BROADCAST",
                                count = myActiveAlerts.size,
                                dotColor = colors.primary,
                                isActive = true
                            )
                        }
                    }
                    item(key = "my_sos_content") {
                        Box(modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                myActiveAlerts.forEach { alert ->
                                    SosAlertCard(
                                        alert = alert,
                                        sectionType = SosType.MINE,
                                        onAcknowledgeClick = {},
                                        onTrackSos = onTrackSos
                                    )
                                }
                            }
                        }
                    }
                }

                // ── 6. ACKNOWLEDGED SOS HISTORY ───────────────────────────────
                if (acknowledgedAlerts.isNotEmpty()) {
                    item(key = "acked_header") {
                        Box(modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
                            SosSectionHeader(
                                label = "ACKNOWLEDGED SOS HISTORY",
                                count = acknowledgedAlerts.size,
                                dotColor = colors.primary,
                                isActive = false
                            )
                        }
                    }
                    item(key = "acked_content") {
                        Box(modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                acknowledgedAlerts.forEach { alert ->
                                    AcknowledgedAlertRow(alert = alert, onTrackSos = onTrackSos)
                                }
                            }
                        }
                    }
                }

                // ── 7. NETWORK REACH ──────────────────────────────────────────
                item(key = "network_reach_section") {
                    Box(modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
                        val maxHops = if (peers.isEmpty()) 0 else peers.maxOf { it.hopDistance }
                        val lastAlert = alerts.maxByOrNull { it.timestamp }
                        val lastBroadcastTime = if (lastAlert != null) {
                            java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                                .format(java.util.Date(lastAlert.timestamp))
                        } else "None"
                        val deliveryStatus = if (peers.isNotEmpty()) "100% (Mesh)" else if (alerts.isNotEmpty()) "Relayed" else "Standby"
                        Column {
                            Text(text = "NETWORK REACH", color = colors.textSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(10.dp))
                            NetworkReachSection(reachableCount = peers.size, maxHops = maxHops, lastBroadcastTime = lastBroadcastTime, deliveryStatus = deliveryStatus)
                        }
                    }
                }

                // ── 8. HOW SOS WORKS ─────────────────────────────────────────
                item(key = "how_sos_works_card") {
                    Box(modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
                        HowSosWorksCard()
                    }
                }
            }
        }
    }
}

/** SOS source type — drives dialog text and colour treatment */
private enum class SosType { RELATIVE, LOCAL_MESH, MINE }

@Composable
private fun SosSectionHeader(label: String, count: Int, dotColor: Color, isActive: Boolean) {
    val colors = ZeroGridTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(if (isActive) dotColor else colors.textSecondary, CircleShape))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (count > 0) "$label ($count)" else label,
            color = if (isActive) dotColor else colors.textSecondary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SosAllClearCard(message: String, subtitle: String) {
    val colors = ZeroGridTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, colors.divider)
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(imageVector = Icons.Outlined.CheckCircle, contentDescription = null, tint = colors.primary, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = message, color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = subtitle, color = colors.textSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun SosAlertCard(
    alert: MeshPacket,
    sectionType: SosType,
    onAcknowledgeClick: () -> Unit,
    onTrackSos: ((Double, Double, String, String, Long) -> Unit)? = null
) {
    val colors = ZeroGridTheme.colors
    val isRelative = sectionType == SosType.RELATIVE

    val accentColor = when (sectionType) {
        SosType.RELATIVE   -> colors.accentRed
        SosType.LOCAL_MESH -> Color(0xFFF59E0B)
        SosType.MINE       -> colors.primary
    }
    val tagLabel = when (sectionType) {
        SosType.RELATIVE   -> "URGENT FAMILY ALERT"
        SosType.LOCAL_MESH -> "LOCAL MESH"
        SosType.MINE       -> "SENT BY ME"
    }

    val senderDisplayName = alert.getSosSenderName()?.takeIf { it.isNotBlank() }
        ?: "Node-${alert.senderId.takeLast(4)}"
    val category = alert.getSosCategory()
    val message = alert.getSosMessage()
    val sosCoords = alert.getSosCoordinates()
    val accuracy = alert.getSosAccuracy()

    Card(
        modifier = Modifier.fillMaxWidth().border(
            width = if (isRelative) 2.dp else 1.dp,
            color = if (isRelative) colors.accentRed else accentColor.copy(alpha = 0.7f),
            shape = RoundedCornerShape(12.dp)
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isRelative) colors.accentRed.copy(alpha = 0.08f) else colors.cardBackground
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isRelative) {
                        Icon(imageVector = Icons.Outlined.WarningAmber, contentDescription = null, tint = colors.accentRed, modifier = Modifier.size(20.dp))
                    } else {
                        Box(modifier = Modifier.size(8.dp).background(accentColor, CircleShape))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (sectionType) { SosType.LOCAL_MESH -> "Local Peer Alert"; SosType.MINE -> "My Broadcast"; else -> "Family Emergency" },
                        color = if (isRelative) colors.accentRed else colors.textPrimary,
                        fontSize = if (isRelative) 16.sp else 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Text(
                    text = tagLabel, color = if (isRelative) Color.White else accentColor, fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                    modifier = Modifier.background(if (isRelative) colors.accentRed else accentColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = if (isRelative) colors.accentRed.copy(alpha = 0.3f) else colors.divider.copy(alpha = 0.5f), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Outlined.DeviceHub, contentDescription = null, tint = if (isRelative) colors.accentRed.copy(alpha = 0.8f) else colors.textSecondary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "FROM: $senderDisplayName", color = colors.textPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Outlined.AccessTime, contentDescription = null, tint = if (isRelative) colors.accentRed.copy(alpha = 0.8f) else colors.textSecondary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                val timeLabel = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(alert.timestamp)
                Text(
                    text = if (sectionType == SosType.LOCAL_MESH) "${alert.hopCount} hops  •  $timeLabel" else timeLabel,
                    color = colors.textSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = if (isRelative) colors.cardBackground else colors.surfaceNested),
                shape = RoundedCornerShape(8.dp),
                border = if (isRelative) BorderStroke(1.dp, colors.accentRed.copy(alpha = 0.2f)) else null
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "$category EMERGENCY", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        if (sosCoords != null) Text(text = "GPS LOCK ✓", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    if (message.isNotBlank() && message != "Emergency SOS triggered") {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "\"$message\"", color = colors.textPrimary, fontSize = 14.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontWeight = if (isRelative) FontWeight.Medium else FontWeight.Normal)
                    }
                    if (sosCoords != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Location: ${String.format(java.util.Locale.US, "%.5f", sosCoords.first)}, ${String.format(java.util.Locale.US, "%.5f", sosCoords.second)}${if (accuracy != null) " (±${accuracy.toInt()}m)" else ""}",
                            color = colors.textSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
            if (sosCoords != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onTrackSos?.invoke(sosCoords.first, sosCoords.second, senderDisplayName, category, alert.timestamp) },
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF3B82F6)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Icon(imageVector = Icons.Outlined.Navigation, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "TRACK LOCATION", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
            if (sectionType != SosType.MINE) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onAcknowledgeClick,
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isRelative) colors.cardBackground else accentColor.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Icon(imageVector = Icons.Outlined.CheckCircle, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (sectionType == SosType.RELATIVE) "CONFIRM PERSON SAFE" else "CONFIRM AREA SAFE",
                        color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun AcknowledgedAlertRow(
    alert: MeshPacket,
    onTrackSos: ((Double, Double, String, String, Long) -> Unit)? = null
) {
    val colors = ZeroGridTheme.colors
    val senderName = alert.getSosSenderName() ?: "Node-${alert.senderId.takeLast(4)}"
    val category = alert.getSosCategory()
    val coords = alert.getSosCoordinates()
    val formattedTime = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(alert.timestamp))
    val typeLabel = if (alert.isCloudSos()) "Relative" else "Mesh Peer"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceNested),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(32.dp).background(colors.primary.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Outlined.Shield, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "CONFIRMED SAFE ✓  •  $typeLabel", color = colors.primary, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "$category Alert — $senderName", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = formattedTime, color = colors.textSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
            if (coords != null) {
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { onTrackSos?.invoke(coords.first, coords.second, senderName, category, alert.timestamp) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color(0xFF3B82F6)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF60A5FA)),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(imageVector = Icons.Outlined.Navigation, contentDescription = null, modifier = Modifier.size(11.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("TRACK", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

// Clean bold TopBar matching the reference image's typography style
@Composable
private fun EmergencyTopBar() {
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
            Text(
                text = "Emergency Center",
                color = colors.textPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MeshStatusBanner(reachableCount: Int) {
    val colors = ZeroGridTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(modifier = Modifier.size(6.dp).background(colors.primary, CircleShape))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "ZeroGrid Mesh Active",
            color = colors.primary,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(16.dp))
        Icon(imageVector = Icons.Outlined.Hub, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$reachableCount devices reachable",
            color = colors.textSecondary,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun EmergencySosStickyButton(onSendSosClick: () -> Unit = {}) {
    val colors = ZeroGridTheme.colors
    Button(
        onClick = onSendSosClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = colors.accentRed),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Outlined.Campaign, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "SEND SOS",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun EmergencySosInfoCard() {
    val colors = ZeroGridTheme.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.accentRed.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(colors.accentRed.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Outlined.Warning, contentDescription = null, tint = colors.accentRed, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Emergency Dispatch",
                color = colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Broadcast an emergency alert to nearby ZeroGrid devices.",
                color = colors.textSecondary,
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Your SOS will be relayed across the local mesh. Use only for genuine emergencies.",
                color = colors.textSecondary,
                fontSize = 11.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun NetworkReachSection(
    reachableCount: Int,
    maxHops: Int,
    lastBroadcastTime: String,
    deliveryStatus: String
) {
    val colors = ZeroGridTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Devices reached", color = colors.textSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Outlined.Devices, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = reachableCount.toString(),
                            color = colors.primary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Relay hops", color = colors.textSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = maxHops.toString(),
                        color = colors.textPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = colors.divider, thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Delivery status", color = colors.textSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = deliveryStatus,
                        color = colors.primary,
                        fontSize = if (deliveryStatus.length > 8) 18.sp else 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Last broadcast", color = colors.textSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = lastBroadcastTime,
                        color = colors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun HowSosWorksCard() {
    val colors = ZeroGridTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, colors.divider)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(imageVector = Icons.Outlined.Info, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "How SOS works",
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Your emergency message is encrypted and propagated through reachable ZeroGrid devices. Relayed hops ensure maximum reachability.",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun ZeroGridEmergencyCenterScreen() = SosCenterScreen()

@Preview(showBackground = true)
@Composable
fun ZeroGridEmergencyCenterPreview() {
    ZeroGridTheme {
        ZeroGridEmergencyCenterScreen()
    }
}