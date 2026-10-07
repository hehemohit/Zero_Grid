package com.example.zerogrid.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zerogrid.navigation.Screen
import com.example.zerogrid.ui.theme.*

@Composable
fun DebugConsoleScreen(onNavigate: (Screen) -> Unit = {}) {
    val logs by DebugLogger.logs.collectAsState()
    val peerStates by DebugLogger.peerStates.collectAsState()
    val listState = rememberLazyListState()
    var filterLevel by remember { mutableStateOf<DebugLevel?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }

    // Auto-scroll to newest log
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) listState.animateScrollToItem(logs.size - 1)
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            DebugTopBar(
                logCount = logs.size,
                onBack = { onNavigate(Screen.SETTINGS) },
                onClear = { DebugLogger.clear() }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            // Tab row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBackground)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("LOG", "PEERS").forEachIndexed { idx, label ->
                    val selected = selectedTab == idx
                    Box(
                        modifier = Modifier
                            .background(
                                if (selected) StatusActive else SurfaceDarker,
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (selected) Color.Black else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.clickableNoRipple { selectedTab = idx }
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Level filter chips
                listOf(null, DebugLevel.INFO, DebugLevel.WARN, DebugLevel.ERROR).forEach { level ->
                    val label = level?.name?.take(3) ?: "ALL"
                    val isSelected = filterLevel == level
                    Box(
                        modifier = Modifier
                            .background(
                                if (isSelected) levelColor(level).copy(alpha = 0.2f) else SurfaceDarker,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) levelColor(level) else TextSecondary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.clickableNoRipple { filterLevel = level }
                        )
                    }
                }
            }

            HorizontalDivider(color = DividerColor, thickness = 1.dp)

            if (selectedTab == 0) {
                // ── LOG TAB ──────────────────────────────────────────────────
                val filtered = if (filterLevel == null) logs
                               else logs.filter { it.level >= filterLevel!! }

                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Outlined.Terminal, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No log entries yet", color = TextSecondary, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(filtered, key = { it.id }) { entry ->
                            LogEntryRow(entry)
                        }
                    }
                }
            } else {
                // ── PEERS TAB ────────────────────────────────────────────────
                if (peerStates.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Outlined.Bluetooth, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No peer states tracked", color = TextSecondary, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(peerStates.values.toList(), key = { it.address }) { state ->
                            PeerStateCard(state)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogEntryRow(entry: DebugLogEntry) {
    val bg    = levelColor(entry.level).copy(alpha = 0.06f)
    val color = levelColor(entry.level)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = entry.timeStr,
            color = TextSecondary,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(74.dp)
        )
        Text(
            text = entry.level.name.take(1),
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(12.dp)
        )
        Text(
            text = "[${entry.tag.takeLast(14)}] ",
            color = TextSecondary.copy(alpha = 0.6f),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = entry.message,
            color = color,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 14.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PeerStateCard(state: PeerDebugState) {
    val stageColor = when {
        state.connectionStage.contains("READY") -> Color(0xFF4CAF50)
        state.connectionStage.contains("ERROR") || state.connectionStage.contains("TIMEOUT") -> Color(0xFFFF5252)
        state.connectionStage.contains("CONNECT") -> Color(0xFFFF9800)
        else -> TextSecondary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(stageColor, CircleShape))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = state.address,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = state.direction.ifEmpty { "?" },
                    color = when (state.direction) {
                        "BOTH"   -> Color(0xFF4CAF50)
                        "CLIENT" -> Color(0xFF4D8CFF)
                        "SERVER" -> Color(0xFFFF9800)
                        else     -> TextSecondary
                    },
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .background(SurfaceDarker, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = DividerColor.copy(alpha = 0.4f), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Stage + MTU row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text("STAGE", color = TextSecondary, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    Text(state.connectionStage, color = stageColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Column {
                    Text("MTU", color = TextSecondary, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    Text("${state.mtu}B", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Column {
                    Text("NOTIFY", color = TextSecondary, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    Text(if (state.notifyEnabled) "ON ✅" else "OFF", color = if (state.notifyEnabled) Color(0xFF4CAF50) else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Packet counters
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text("SENT", color = TextSecondary, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    Text("${state.packetsSent}", color = StatusActive, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Column {
                    Text("RECEIVED", color = TextSecondary, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    Text("${state.packetsReceived}", color = Color(0xFF4D8CFF), fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                if (state.lastError.isNotEmpty()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("LAST ERROR", color = TextSecondary, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        Text(state.lastError, color = Color(0xFFFF5252), fontSize = 10.sp, fontFamily = FontFamily.Monospace, maxLines = 2)
                    }
                }
            }
        }
    }
}

@Composable
private fun DebugTopBar(logCount: Int, onBack: () -> Unit, onClear: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = StatusActive, modifier = Modifier.size(24.dp))
            }
            Icon(imageVector = Icons.Outlined.BugReport, contentDescription = null, tint = StatusActive, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Debug Console", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text("$logCount entries • BLE mesh trace", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
            IconButton(onClick = onClear) {
                Icon(imageVector = Icons.Outlined.Delete, contentDescription = "Clear", tint = Color(0xFFFF5252), modifier = Modifier.size(22.dp))
            }
        }
        HorizontalDivider(color = DividerColor, thickness = 1.dp)
    }
}

private fun levelColor(level: DebugLevel?): Color = when (level) {
    DebugLevel.ERROR   -> Color(0xFFFF5252)
    DebugLevel.WARN    -> Color(0xFFFFAB40)
    DebugLevel.INFO    -> Color(0xFF69F0AE)
    DebugLevel.DEBUG   -> Color(0xFF82B1FF)
    DebugLevel.VERBOSE -> Color(0xFF78909C)
    null               -> Color(0xFF82B1FF)
}

@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return this.then(
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = { onClick() }
        )
    )
}

