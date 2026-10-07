package com.example.zerogrid.navigation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zerogrid.home.MeshDashboardScreen
import com.example.zerogrid.mesh.engine.MeshEngine
import com.example.zerogrid.ui.theme.ZeroGridTheme
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf


enum class NavTab(val label: String, val icon: ImageVector, val screen: Screen) {
    MESH("Mesh", Icons.Outlined.Hub, Screen.HOME),
    MESSAGES("Messages", Icons.Outlined.ChatBubbleOutline, Screen.MESSAGES),
    SOS("SOS", Icons.Outlined.WarningAmber, Screen.SOS_CENTER),
    SETTINGS("Settings", Icons.Outlined.Settings, Screen.SETTINGS)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ZeroGridMainScreen(
    onOpenPeerChat: (String) -> Unit = {}
) {
    val pagerState = rememberPagerState(pageCount = { NavTab.entries.size })
    val coroutineScope = rememberCoroutineScope()

    // Map the active pager page index to your Screen enum for bottom bar selection
    val currentScreen = when (pagerState.currentPage) {
        0 -> Screen.HOME
        1 -> Screen.MESSAGES
        2 -> Screen.SOS_CENTER
        3 -> Screen.SETTINGS
        else -> Screen.HOME
    }

    Scaffold(
        bottomBar = {
            ZeroGridBottomBar(
                currentScreen = currentScreen,
                onNavigate = { targetScreen ->
                    val targetIndex = when (targetScreen) {
                        Screen.HOME, Screen.MESH, Screen.PEER_DETAILS, Screen.NETWORK_STATUS -> 0
                        Screen.MESSAGES, Screen.CHANNELS, Screen.CHAT_DETAIL, Screen.PEER_DIRECT_CHAT -> 1
                        Screen.SOS_CENTER, Screen.SEND_SOS -> 2
                        Screen.SETTINGS, Screen.SECURITY_PRIVACY, Screen.EMERGENCY_CONTACTS, Screen.FAMILY_LINKS, Screen.PROFILE -> 3
                        else -> 0
                    }
                    // Smoothly scroll the pager when a bottom bar item is tapped
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(targetIndex)
                    }
                }
            )
        }
    ) { innerPadding ->
        // HorizontalPager enables native fluid swiping between screens like WhatsApp
        HorizontalPager(
            state = pagerState,
            key = { page -> NavTab.entries[page].screen.name },
            beyondViewportPageCount = 1,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            when (page) {
                0 -> MeshDashboardScreen(
                    onNavigate = { targetScreen ->
                        val targetIndex = when (targetScreen) {
                            Screen.HOME, Screen.MESH -> 0
                            Screen.MESSAGES, Screen.CHANNELS -> 1
                            Screen.SOS_CENTER -> 2
                            Screen.SETTINGS, Screen.PROFILE -> 3
                            else -> 0
                        }
                        coroutineScope.launch { pagerState.animateScrollToPage(targetIndex) }
                    },
                    onOpenPeerChat = onOpenPeerChat
                )
                1 -> MessagesScreenPlaceholder()
                2 -> SosScreenPlaceholder()
                3 -> SettingsScreenPlaceholder()
            }
        }
    }
}

@Composable
fun ZeroGridBottomBar(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit
) {
    val colors = ZeroGridTheme.colors
    val context = LocalContext.current
    val meshEngine = remember { MeshEngine.getInstance(context) }
    val sosAlerts by meshEngine.sosAlerts.collectAsState()
    val conversations by meshEngine.conversations.collectAsState()

    // Count unread incoming messages when not on Messages screen (derivedStateOf + sumOf eliminates per-frame list allocations)
    val incomingMessagesCount by remember(conversations, currentScreen) {
        derivedStateOf {
            if (currentScreen == Screen.MESSAGES || currentScreen == Screen.PEER_DIRECT_CHAT) {
                0
            } else {
                conversations.values.sumOf { msgList -> msgList.count { !it.isMine } }
            }
        }
    }
    val activeSosAlertsCount = sosAlerts.size

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        HorizontalDivider(color = colors.divider, thickness = 1.dp)
        NavigationBar(
            containerColor = colors.cardBackground,
            contentColor = colors.textSecondary,
            tonalElevation = 0.dp
        ) {
            NavTab.entries.forEach { tab ->
                val selected = when (tab) {
                    NavTab.MESH -> (currentScreen == Screen.HOME || currentScreen == Screen.MESH || currentScreen == Screen.PEER_DETAILS || currentScreen == Screen.NETWORK_STATUS)
                    NavTab.MESSAGES -> (currentScreen == Screen.MESSAGES || currentScreen == Screen.CHANNELS || currentScreen == Screen.CHAT_DETAIL || currentScreen == Screen.PEER_DIRECT_CHAT)
                    NavTab.SOS -> (currentScreen == Screen.SOS_CENTER || currentScreen == Screen.SEND_SOS)
                    NavTab.SETTINGS -> (currentScreen == Screen.SETTINGS || currentScreen == Screen.SECURITY_PRIVACY || currentScreen == Screen.EMERGENCY_CONTACTS || currentScreen == Screen.FAMILY_LINKS || currentScreen == Screen.PROFILE)
                }

                val itemColor = if (tab == NavTab.SOS) {
                    if (selected) colors.accentRed else colors.accentRed.copy(alpha = 0.8f)
                } else if (selected) {
                    colors.primary
                } else {
                    colors.textSecondary
                }

                NavigationBarItem(
                    selected = selected,
                    onClick = { onNavigate(tab.screen) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (tab == NavTab.MESSAGES && incomingMessagesCount > 0) {
                                    Badge(
                                        containerColor = colors.primary,
                                        contentColor = if (colors.isDark) Color.Black else Color.White
                                    ) {
                                        Text(
                                            text = if (incomingMessagesCount > 9) "9+" else incomingMessagesCount.toString(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else if (tab == NavTab.SOS && activeSosAlertsCount > 0) {
                                    Badge(
                                        containerColor = colors.accentRed,
                                        contentColor = Color.White
                                    ) {
                                        Text(
                                            text = "!",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = itemColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    label = {
                        Text(
                            text = tab.label,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = itemColor
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = itemColor,
                        unselectedIconColor = colors.textSecondary,
                        selectedTextColor = itemColor,
                        unselectedTextColor = colors.textSecondary,
                        indicatorColor = if (tab == NavTab.SOS) colors.accentRed.copy(alpha = 0.12f) else colors.primary.copy(alpha = 0.12f)
                    )
                )
            }
        }
    }
}

// Placeholders for remaining tab destinations (replace with your actual screens)
@Composable fun MessagesScreenPlaceholder() { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Messages Screen") } }
@Composable fun SosScreenPlaceholder() { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("SOS Screen") } }
@Composable fun SettingsScreenPlaceholder() { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Settings Screen") } }