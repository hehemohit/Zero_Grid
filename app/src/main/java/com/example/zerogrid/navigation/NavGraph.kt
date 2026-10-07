package com.example.zerogrid.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.zerogrid.emergency.*
import com.example.zerogrid.hardware.HardwareRequirementBanner
import com.example.zerogrid.home.*
import com.example.zerogrid.mesh.*
import com.example.zerogrid.mesh.engine.MeshEngine
import com.example.zerogrid.messaging.*
import com.example.zerogrid.onboarding.*
import com.example.zerogrid.settings.*
import com.example.zerogrid.debug.*
import com.example.zerogrid.ui.theme.*
import kotlinx.coroutines.launch

private val SosRed = Color(0xFFFF3B30)
private val SosAmber = Color(0xFFFF9500)
private val SosCyan = Color(0xFF00E5FF)

data class SosTrackingTarget(
    val lat: Double,
    val lng: Double,
    val name: String = "Emergency Contact",
    val category: String = "SOS",
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ZeroGridApp(
    initialScreen: Screen = Screen.HOME,
    initialTrackTarget: SosTrackingTarget? = null,
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val meshEngine = remember { MeshEngine.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()

    val sosAlerts by meshEngine.sosAlerts.collectAsState()
    val acknowledgedAlertIds by meshEngine.acknowledgedAlertIds.collectAsState()

    val activeSosAlert = sosAlerts.firstOrNull {
        it.packetId !in acknowledgedAlertIds && it.senderId != meshEngine.localNodeId
    }

    val initialPage = when (initialScreen) {
        Screen.HOME -> 0
        Screen.MESSAGES -> 1
        Screen.SOS_CENTER -> 2
        Screen.SETTINGS -> 3
        else -> 0
    }
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { NavTab.entries.size })
    val subScreenStack = remember {
        mutableStateListOf<Screen>().apply {
            if (initialTrackTarget != null) {
                add(Screen.TRACK_SOS)
            } else if (initialScreen !in setOf(Screen.HOME, Screen.MESSAGES, Screen.SOS_CENTER, Screen.SETTINGS)) {
                add(initialScreen)
            }
        }
    }
    val currentSubScreen = subScreenStack.lastOrNull()
    var selectedPeerId by remember { mutableStateOf("") }
    // Track SOS target coordinates passed from SosCenterScreen alert cards or notification
    var trackSosLat by remember { mutableStateOf(initialTrackTarget?.lat ?: 0.0) }
    var trackSosLng by remember { mutableStateOf(initialTrackTarget?.lng ?: 0.0) }
    var trackSosName by remember { mutableStateOf(initialTrackTarget?.name ?: "Unknown") }
    var trackSosCategory by remember { mutableStateOf(initialTrackTarget?.category ?: "SOS") }
    var trackSosTimestamp by remember { mutableStateOf(initialTrackTarget?.timestamp ?: 0L) }

    val currentTabScreen = when (pagerState.currentPage) {
        0 -> Screen.HOME
        1 -> Screen.MESSAGES
        2 -> Screen.SOS_CENTER
        3 -> Screen.SETTINGS
        else -> Screen.HOME
    }

    // Prefetch & preload data for adjacent pages (currentPage - 1 and currentPage + 1) off the main thread
    LaunchedEffect(pagerState.currentPage) {
        val currentPage = pagerState.currentPage
        val adjacentPages = listOf(currentPage - 1, currentPage + 1).filter { it in 0 until 4 }
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            adjacentPages.forEach { page ->
                when (page) {
                    0 -> meshEngine.connectedPeers.value
                    1 -> meshEngine.conversations.value
                    2 -> meshEngine.sosAlerts.value
                    3 -> com.zerogrid.mesh.app.ui.UserSessionManager.getInstance(context).getUserName()
                }
            }
        }
    }

    // Only these 4 screens are true pager tab roots — navigating to them scrolls the pager.
    // ALL other screens (sub-screens like EMERGENCY_CONTACTS, CHANNELS, FAMILY_LINKS, etc.)
    // must be pushed as currentSubScreen overlays, never as pager tab switches.
    val tabRootScreens = setOf(
        Screen.HOME, Screen.MESSAGES, Screen.SOS_CENTER, Screen.SETTINGS
    )

    fun navigateTo(screen: Screen) {
        if (screen in tabRootScreens) {
            subScreenStack.clear()
            val index = when (screen) {
                Screen.HOME -> 0
                Screen.MESSAGES -> 1
                Screen.SOS_CENTER -> 2
                Screen.SETTINGS -> 3
                else -> return
            }
            coroutineScope.launch { pagerState.animateScrollToPage(index) }
        } else {
            // Push sub-screen onto navigation stack (avoid duplicate consecutive pushes)
            if (subScreenStack.lastOrNull() != screen) {
                subScreenStack.add(screen)
            }
        }
    }

    fun navigateBack() {
        if (subScreenStack.isNotEmpty()) {
            subScreenStack.removeAt(subScreenStack.lastIndex)
        }
    }

    // Back-handling Layer 1: If any sub-screens are open, mobile back button pops the top sub-screen
    BackHandler(enabled = subScreenStack.isNotEmpty()) {
        navigateBack()
    }

    // Back-handling Layer 2: If no sub-screens open and not on Home tab, mobile back button returns to Home
    BackHandler(enabled = subScreenStack.isEmpty() && pagerState.currentPage != 0) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(0)
        }
    }

    Scaffold(
        containerColor = ZeroGridTheme.colors.background,
        bottomBar = {
            // Render bottom bar ONLY when on root tabs to prevent double-rendering
            if (currentSubScreen == null) {
                ZeroGridBottomBar(
                    currentScreen = currentTabScreen,
                    onNavigate = { targetScreen ->
                        val targetTab = NavTab.entries.find { it.screen == targetScreen || getTabRootScreens(it).contains(targetScreen) }
                        if (targetTab != null) {
                            val index = NavTab.entries.indexOf(targetTab)
                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // Crucial: Ensures smooth clipping and removes layout lag
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                HardwareRequirementBanner()

                AnimatedContent(
                    targetState = currentSubScreen,
                    transitionSpec = {
                        if (targetState != null) {
                            // Opening a sub-screen (Chat / Channel / Details): Slide in from right (WhatsApp style)
                            (slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }, animationSpec = tween(280)) + fadeIn(animationSpec = tween(280)))
                                .togetherWith(slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth / 3 }, animationSpec = tween(280)) + fadeOut(animationSpec = tween(280)))
                        } else {
                            // Navigating back to Main Paging Tabs: Slide sub-screen out to right
                            (slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth / 3 }, animationSpec = tween(280)) + fadeIn(animationSpec = tween(280)))
                                .togetherWith(slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }, animationSpec = tween(280)) + fadeOut(animationSpec = tween(280)))
                        }
                    },
                    label = "SubScreenNavigation",
                    modifier = Modifier.weight(1f)
                ) { targetScreen ->
                    if (targetScreen == Screen.TRACK_SOS) {
                        TrackSosScreen(
                            targetLat = trackSosLat,
                            targetLng = trackSosLng,
                            targetName = trackSosName,
                            category = trackSosCategory,
                            sosTimestamp = trackSosTimestamp,
                            onBack = { navigateBack() }
                        )
                    } else if (targetScreen != null) {
                        RenderScreen(
                            screen = targetScreen,
                            selectedPeerId = selectedPeerId,
                            onNavigate = { navigateTo(it) },
                            onBack = { navigateBack() },
                            onOpenPeerChat = { peerId ->
                                selectedPeerId = peerId
                                navigateTo(Screen.PEER_DIRECT_CHAT)
                            },
                            onLogout = onLogout
                        )
                    } else {
                        // Optimized HorizontalPager with hardware acceleration boundary
                        HorizontalPager(
                            state = pagerState,
                            key = { page -> NavTab.entries[page].screen.name },
                            modifier = Modifier.fillMaxSize(),
                            beyondViewportPageCount = 1 // Keeps adjacent pages pre-rendered to eliminate swipe stutter/lag
                        ) { page ->
                            when (page) {
                                0 -> MeshDashboardScreen(
                                    onNavigate = { navigateTo(it) },
                                    onOpenPeerChat = { peerId ->
                                        selectedPeerId = peerId
                                        navigateTo(Screen.PEER_DIRECT_CHAT)
                                    }
                                )
                                1 -> MessagesScreen(
                                    onNavigate = { navigateTo(it) },
                                    onOpenPeerChat = { peerId ->
                                        selectedPeerId = peerId
                                        navigateTo(Screen.PEER_DIRECT_CHAT)
                                    }
                                )
                                2 -> SosCenterScreen(
                                    onNavigate = { navigateTo(it) },
                                    onTrackSos = { lat, lng, name, category, timestamp ->
                                        trackSosLat = lat
                                        trackSosLng = lng
                                        trackSosName = name
                                        trackSosCategory = category
                                        trackSosTimestamp = timestamp
                                        navigateTo(Screen.TRACK_SOS)
                                    }
                                )
                                3 -> SettingsScreen(onNavigate = { navigateTo(it) }, onLogout = onLogout)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderScreen(
    screen: Screen,
    selectedPeerId: String,
    onNavigate: (Screen) -> Unit,
    onBack: () -> Unit,
    onOpenPeerChat: (String) -> Unit,
    onLogout: () -> Unit
) {
    when (screen) {
        Screen.HOME -> MeshDashboardScreen(onNavigate = onNavigate, onOpenPeerChat = onOpenPeerChat)
        Screen.MESSAGES -> MessagesScreen(onNavigate = onNavigate, onOpenPeerChat = onOpenPeerChat)
        Screen.PEER_DIRECT_CHAT -> PeerDirectChatScreen(peerId = selectedPeerId, onNavigate = onNavigate)
        Screen.MESH -> NearbyDevicesScreen(
            onNavigate = onNavigate,
            onBackClick = onBack,
            onOpenPeerDetails = { peerId -> onOpenPeerChat(peerId) },
            onOpenPeerChat = onOpenPeerChat
        )
        Screen.SETTINGS -> SettingsScreen(onNavigate = onNavigate, onLogout = onLogout)
        Screen.SOS_CENTER -> SosCenterScreen(onNavigate = onNavigate)
        Screen.SEND_SOS -> SendSosScreen(onNavigate = onNavigate)
        Screen.PEER_DETAILS -> PeerDetailsScreen(
            peerId = selectedPeerId,
            onNavigate = onNavigate,
            onOpenPeerChat = onOpenPeerChat,
            onBackClick = onBack
        )
        Screen.CHANNELS -> ChannelsScreen(onNavigate = onNavigate)
        Screen.CHAT_DETAIL -> ChatDetailScreen(onNavigate = onNavigate)
        Screen.SPLASH -> SplashScreen(onNavigate = onNavigate)
        Screen.ONBOARDING -> OnBoardingScreen(onNavigate = onNavigate)
        Screen.PERMISSIONS -> PermissionsScreen(onNavigate = onNavigate)
        Screen.CREATE_IDENTITY -> CreateIdentityScreen(onNavigate = onNavigate)
        Screen.NETWORK_STATUS -> NetworkStatusScreen(onNavigate = onNavigate)
        Screen.SECURITY_PRIVACY -> SecurityPrivacyScreen(onNavigate = onNavigate)
        Screen.DEBUG_CONSOLE -> DebugConsoleScreen(onNavigate = onNavigate)
        Screen.EMERGENCY_CONTACTS -> com.example.zerogrid.contacts.EmergencyContactsScreen(
            onNavigate = onNavigate,
            onBack = onBack
        )
        Screen.FAMILY_LINKS -> com.example.zerogrid.family.FamilyLinksScreen(
            onNavigate = onNavigate,
            onBack = onBack
        )
        Screen.PROFILE -> com.example.zerogrid.profile.ProfileScreen(
            onNavigate = onNavigate,
            onBack = onBack,
            onLogout = onLogout
        )
        Screen.TRACK_SOS -> { /* handled in caller with full state */ }
    }
}

private fun getTabRootScreens(tab: NavTab): List<Screen> {
    return when (tab) {
        NavTab.MESH -> listOf(Screen.HOME, Screen.MESH, Screen.PEER_DETAILS, Screen.NETWORK_STATUS)
        NavTab.MESSAGES -> listOf(Screen.MESSAGES, Screen.CHANNELS, Screen.CHAT_DETAIL, Screen.PEER_DIRECT_CHAT)
        NavTab.SOS -> listOf(Screen.SOS_CENTER, Screen.SEND_SOS, Screen.TRACK_SOS)
        NavTab.SETTINGS -> listOf(Screen.SETTINGS, Screen.SECURITY_PRIVACY, Screen.EMERGENCY_CONTACTS, Screen.FAMILY_LINKS, Screen.PROFILE)
    }
}