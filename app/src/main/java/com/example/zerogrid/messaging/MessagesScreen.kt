package com.example.zerogrid.messaging

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zerogrid.mesh.engine.MeshEngine
import com.example.zerogrid.mesh.engine.MeshNode
import com.example.zerogrid.navigation.Screen
import com.example.zerogrid.ui.components.ZeroGridTopBar
import com.example.zerogrid.ui.theme.BadgeGreen
import com.example.zerogrid.ui.theme.ZeroGridTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Immutable
data class ChatSummaryUiModel(
    val peerId: String,
    val alias: String,
    val isOnline: Boolean,
    val hopDistance: Int,
    val lastMessage: String,
    val timestamp: Long,
    val unreadCount: Int
)

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun MessagesScreen(
    onNavigate: (Screen) -> Unit = {},
    onOpenPeerChat: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val meshEngine = remember { MeshEngine.getInstance(context) }
    val connectedPeers by meshEngine.connectedPeers.collectAsState()
    val sosAlerts by meshEngine.sosAlerts.collectAsState()
    val conversations by meshEngine.conversations.collectAsState()
    val isMeshActive by meshEngine.isMeshActive.collectAsState()
    val activeChannelMode by meshEngine.activeChannelMode.collectAsState()
    val colors = ZeroGridTheme.colors

    val localNodeId = meshEngine.localNodeId
    val localSuffix = remember(localNodeId) { localNodeId.removePrefix("NODE-") }
    val localDisplayName by meshEngine.displayName.collectAsState()

    val filteredPeers = remember(connectedPeers, activeChannelMode, localNodeId, localDisplayName) {
        connectedPeers.filter { node ->
            node.transportType == activeChannelMode.transportName &&
                    !node.nodeId.equals(localNodeId, ignoreCase = true) &&
                    !node.nodeId.removePrefix("NODE-").equals(localSuffix, ignoreCase = true) &&
                    !node.alias.equals(localDisplayName, ignoreCase = true) &&
                    !node.alias.equals(android.os.Build.MODEL, ignoreCase = true)
        }.distinctBy { it.nodeId }
    }

    val allChatPeerIds = remember(conversations, filteredPeers) {
        val fromConversations = conversations.filter { it.value.isNotEmpty() }.keys.filter { peerId ->
            !peerId.equals(localNodeId, ignoreCase = true) &&
                    !peerId.removePrefix("NODE-").equals(localSuffix, ignoreCase = true)
        }
        val fromConnected = filteredPeers.map { it.nodeId }
        (fromConversations + fromConnected).distinct()
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Direct Chats, 1: Channels
    var searchQuery by remember { mutableStateOf("") }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    val peerMap = remember(filteredPeers) { filteredPeers.associateBy { it.nodeId } }

    val displayChats by produceState(
        initialValue = emptyList<ChatSummaryUiModel>(),
        allChatPeerIds, searchQuery, conversations, peerMap
    ) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            allChatPeerIds.mapNotNull { peerId ->
                val peer = peerMap[peerId]
                val messages = conversations[peerId] ?: emptyList()
                if (peer == null && messages.isEmpty()) {
                    return@mapNotNull null
                }
                val alias = peer?.alias ?: meshEngine.getPeerDisplayName(peerId)
                val lastMsg = messages.lastOrNull()
                val lastMsgText = lastMsg?.text ?: if (peer != null) "Connected • Ready to chat" else "Offline"
                val lastTimestamp = lastMsg?.timestamp ?: (peer?.lastSeenTimestamp ?: System.currentTimeMillis())

                if (searchQuery.isNotBlank() &&
                    !alias.contains(searchQuery, ignoreCase = true) &&
                    !lastMsgText.contains(searchQuery, ignoreCase = true)
                ) {
                    return@mapNotNull null
                }

                val unreadCount = messages.count { !it.isMine }
                ChatSummaryUiModel(
                    peerId = peerId,
                    alias = alias,
                    isOnline = peer != null,
                    hopDistance = peer?.hopDistance ?: -1,
                    lastMessage = lastMsgText,
                    timestamp = lastTimestamp,
                    unreadCount = unreadCount
                )
            }.sortedByDescending { it.timestamp }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
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
                            // Screen Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Messages",
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = if (isMeshActive) BadgeGreen.copy(alpha = 0.12f) else colors.surfaceNested,
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(if (isMeshActive) BadgeGreen else colors.textSecondary, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isMeshActive) "${activeChannelMode.label} Active" else "Offline",
                                                color = if (isMeshActive) BadgeGreen else colors.textSecondary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }

                            if (showClearConfirmDialog) {
                                AlertDialog(
                                    onDismissRequest = { showClearConfirmDialog = false },
                                    title = {
                                        Text(
                                            text = "Clear All Session Chats?",
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textPrimary
                                        )
                                    },
                                    text = {
                                        Text(
                                            text = "This will immediately clear all messages and chat history from this session. They will not be saved.",
                                            color = colors.textSecondary,
                                            fontSize = 14.sp
                                        )
                                    },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                meshEngine.clearAllConversations()
                                                showClearConfirmDialog = false
                                            }
                                        ) {
                                            Text("Clear All", color = colors.accentRed, fontWeight = FontWeight.Bold)
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showClearConfirmDialog = false }) {
                                            Text("Cancel", color = colors.textSecondary)
                                        }
                                    },
                                    containerColor = colors.cardBackground,
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // 1. Slim, Responsive Search Bar (BasicTextField)
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth() // Fully responsive
                                    .height(44.dp), // Slim height
                                shape = RoundedCornerShape(12.dp),
                                color = colors.cardBackground,
                                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(colors.divider))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Search,
                                        contentDescription = "Search",
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        textStyle = TextStyle(
                                            color = colors.textPrimary,
                                            fontSize = 14.sp
                                        ),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        decorationBox = { innerTextField ->
                                            Box(contentAlignment = Alignment.CenterStart) {
                                                if (searchQuery.isEmpty()) {
                                                    Text(
                                                        text = "Search chats or channels...",
                                                        color = colors.textSecondary,
                                                        fontSize = 14.sp
                                                    )
                                                }
                                                innerTextField()
                                            }
                                        },
                                        cursorBrush = SolidColor(colors.primary)
                                    )
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(
                                            onClick = { searchQuery = "" },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Close,
                                                contentDescription = "Clear",
                                                tint = colors.textSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Segmented Tab Selector
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.surfaceNested, RoundedCornerShape(12.dp))
                                    .padding(4.dp)
                            ) {
                                SegmentedTabItem(
                                    modifier = Modifier.weight(1f),
                                    title = "Direct Chats",
                                    badge = displayChats.size.toString(),
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 }
                                )
                                SegmentedTabItem(
                                    modifier = Modifier.weight(1f),
                                    title = "Channels",
                                    badge = "2",
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 }
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Section Title
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (selectedTab == 0) "RECENT CHATS" else "AVAILABLE CHANNELS",
                                    color = colors.textSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )

                                // Replaced "Activity" with the "Clear All" action, visible only on the Chats tab
                                if (selectedTab == 0) {
                                    Row(
                                        modifier = Modifier
                                            .clickable { showClearConfirmDialog = true }
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteSweep,
                                            contentDescription = "Clear Session Chats",
                                            tint = colors.textSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Clear All",
                                            color = colors.textSecondary,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }

                // 2. Smooth Animated Transition
                item {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 840.dp)
                            .fillMaxWidth()
                    ) {
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = {
                                val animSpec = tween<IntOffset>(durationMillis = 350, easing = FastOutSlowInEasing)
                                val fadeSpec = tween<Float>(durationMillis = 250)
                                if (targetState > initialState) {
                                    (slideInHorizontally(animSpec) { width -> width } + fadeIn(fadeSpec)).togetherWith(
                                        slideOutHorizontally(animSpec) { width -> -width } + fadeOut(fadeSpec)
                                    )
                                } else {
                                    (slideInHorizontally(animSpec) { width -> -width } + fadeIn(fadeSpec)).togetherWith(
                                        slideOutHorizontally(animSpec) { width -> width } + fadeOut(fadeSpec)
                                    )
                                }
                            },
                            label = "tab_transition"
                        ) { tab ->
                            if (tab == 0) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (displayChats.isEmpty()) {
                                        EmptyMessagesCard(
                                            hasConnectedPeers = filteredPeers.isNotEmpty(),
                                            onStartChat = {
                                                filteredPeers.firstOrNull()?.let { onOpenPeerChat?.invoke(it.nodeId) }
                                                    ?: onNavigate(Screen.MESH)
                                            }
                                        )
                                    } else {
                                        displayChats.forEach { chat ->
                                            RecentChatCard(
                                                alias = chat.alias,
                                                isOnline = chat.isOnline,
                                                hopDistance = chat.hopDistance,
                                                lastMessage = chat.lastMessage,
                                                timestamp = chat.timestamp,
                                                unreadCount = chat.unreadCount,
                                                onClick = { onOpenPeerChat?.invoke(chat.peerId) }
                                            )
                                        }
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    ChannelRowCard(
                                        channelName = "#emergency-broadcast",
                                        description = "All-station emergency announcements and SOS broadcast feed.",
                                        memberCount = "${filteredPeers.size + 1} Nearby",
                                        isAlert = sosAlerts.isNotEmpty(),
                                        onClick = { onNavigate(Screen.SOS_CENTER) }
                                    )
                                    ChannelRowCard(
                                        channelName = "#mesh-general",
                                        description = "Public community mesh chat. All local nodes can broadcast here.",
                                        memberCount = "${filteredPeers.size} Peers",
                                        isAlert = false,
                                        onClick = { onNavigate(Screen.CHANNELS) }
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Bottom CTA Button (Also aligned responsively)
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(
                        modifier = Modifier
                            .widthIn(max = 840.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = {
                                filteredPeers.firstOrNull()?.let { onOpenPeerChat?.invoke(it.nodeId) }
                                    ?: onNavigate(Screen.MESH)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primary,
                                contentColor = if (colors.isDark) Color.Black else Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Start Chat",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun SegmentedTabItem(
    modifier: Modifier = Modifier,
    title: String,
    badge: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = ZeroGridTheme.colors

    Surface(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) colors.primary else Color.Transparent,
        contentColor = if (selected) (if (colors.isDark) Color.Black else Color.White) else colors.textSecondary
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                color = if (selected) {
                    (if (colors.isDark) Color.Black.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.25f))
                } else {
                    colors.cardBackground
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = badge,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun RecentChatCard(
    alias: String,
    isOnline: Boolean,
    hopDistance: Int,
    lastMessage: String,
    timestamp: Long,
    unreadCount: Int,
    onClick: () -> Unit
) {
    val colors = ZeroGridTheme.colors
    val initials = if (alias.length >= 2) alias.take(2).uppercase() else "ZG"

    val timeString = remember(timestamp) {
        val diff = System.currentTimeMillis() - timestamp
        when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000}m ago"
            diff < 86400_000 -> "${diff / 3600_000}h ago"
            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
        }
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(colors.divider.copy(alpha = 0.7f)))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Modern Avatar
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    colors.primary.copy(alpha = 0.20f),
                                    colors.primary.copy(alpha = 0.08f)
                                )
                            ),
                            shape = CircleShape
                        )
                        .border(1.dp, colors.primary.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        color = colors.primary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(if (isOnline) BadgeGreen else colors.textSecondary.copy(alpha = 0.5f), CircleShape)
                        .border(2.dp, colors.cardBackground, CircleShape)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Name + Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = alias,
                            color = colors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (alias.contains("Rescue", ignoreCase = true) || alias.contains("Admin", ignoreCase = true)) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Outlined.Verified,
                                contentDescription = "Verified",
                                tint = colors.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text = timeString,
                        color = if (unreadCount > 0) colors.primary else colors.textSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Last Message Preview
                Text(
                    text = lastMessage,
                    color = if (unreadCount > 0) colors.textPrimary else colors.textSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (unreadCount > 0) FontWeight.Medium else FontWeight.Normal
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Badges Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val (hopLabel, hopColor) = when {
                        !isOnline -> "Offline" to colors.textSecondary
                        hopDistance <= 1 -> "Direct" to BadgeGreen
                        hopDistance == 2 -> "1 Hop" to colors.primary
                        else -> "$hopDistance Hops" to colors.textSecondary
                    }

                    Surface(
                        color = hopColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (hopDistance <= 1) Icons.Outlined.NearMe else Icons.Outlined.AltRoute,
                                contentDescription = null,
                                tint = hopColor,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = hopLabel,
                                color = hopColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    if (unreadCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .defaultMinSize(minWidth = 18.dp)
                                    .height(18.dp)
                                    .background(colors.primary, RoundedCornerShape(9.dp))
                                    .padding(horizontal = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                    color = if (colors.isDark) Color.Black else Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
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
private fun ChannelRowCard(
    channelName: String,
    description: String,
    memberCount: String,
    isAlert: Boolean,
    onClick: () -> Unit
) {
    val colors = ZeroGridTheme.colors

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(colors.divider))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        if (isAlert) colors.accentRed.copy(alpha = 0.12f) else colors.primary.copy(alpha = 0.12f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isAlert) Icons.Outlined.Campaign else Icons.Outlined.Tag,
                    contentDescription = null,
                    tint = if (isAlert) colors.accentRed else colors.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = channelName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isAlert) colors.accentRed else colors.textPrimary
                    )
                    Text(
                        text = memberCount,
                        fontSize = 11.sp,
                        color = colors.textSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EmptyMessagesCard(
    hasConnectedPeers: Boolean,
    onStartChat: () -> Unit
) {
    val colors = ZeroGridTheme.colors

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(colors.divider))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(colors.primary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "No Active Mesh Chats",
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (hasConnectedPeers) {
                    "Peers are currently within mesh range. Select a peer to start encrypted off-grid communication."
                } else {
                    "ZeroGrid operates with ephemeral chat: conversations exist in-memory during this session and vanish once the app closes."
                },
                color = colors.textSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onStartChat,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = if (colors.isDark) Color.Black else Color.White
                )
            ) {
                Text(
                    text = if (hasConnectedPeers) "Start Chat with Nearby Peer" else "Scan Nearby Devices",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}