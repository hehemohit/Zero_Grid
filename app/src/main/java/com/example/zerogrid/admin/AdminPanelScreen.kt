package com.example.zerogrid.admin

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zerogrid.admin.data.AdminSocketEvent
import com.example.zerogrid.admin.data.AdminSocketManager
import com.example.zerogrid.admin.data.AdminSosRepository
import com.example.zerogrid.admin.data.AdminUserRepository
import com.example.zerogrid.admin.data.model.AdminSosEventDto
import com.example.zerogrid.admin.data.model.AdminUserDto
import com.example.zerogrid.admin.ui.components.*
import com.example.zerogrid.admin.ui.detail.SosDetailBottomSheet
import com.example.zerogrid.admin.ui.history.SosHistoryTab
import com.example.zerogrid.admin.ui.users.UserManagementTab
import com.example.zerogrid.ui.theme.*
import com.zerogrid.mesh.app.ui.UserSessionManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class AdminTab(val label: String, val icon: ImageVector) {
    FEED("Tactical Feed", Icons.Outlined.Emergency),
    HISTORY("History", Icons.Outlined.History),
    USERS("Nodes & Users", Icons.Outlined.Group)
}

@Composable
fun AdminPanelScreen(
    sessionManager: UserSessionManager,
    onOpenMeshApp: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val sosRepository = remember { AdminSosRepository() }
    val userRepository = remember { AdminUserRepository() }
    val socketManager = remember { AdminSocketManager.getInstance() }

    var currentTab by remember { mutableStateOf(AdminTab.FEED) }
    var activeEvents by remember { mutableStateOf<List<AdminSosEventDto>>(emptyList()) }
    var historyEvents by remember { mutableStateOf<List<AdminSosEventDto>>(emptyList()) }
    var usersList by remember { mutableStateOf<List<AdminUserDto>>(emptyList()) }

    var selectedFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var userSearchQuery by remember { mutableStateOf("") }
    var isMapVisible by remember { mutableStateOf(true) }
    var isGoogleMapSelected by remember { mutableStateOf(true) }

    var inspectedIncident by remember { mutableStateOf<AdminSosEventDto?>(null) }
    var isFeedLoading by remember { mutableStateOf(false) }
    var isHistoryLoading by remember { mutableStateOf(false) }
    var isUsersLoading by remember { mutableStateOf(false) }
    var isActionLoading by remember { mutableStateOf(false) }

    val socketState by socketManager.connectionState.collectAsState()
    val colors = ZeroGridTheme.colors

    fun showToast(msg: String) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(msg)
        }
    }

    fun loadActiveFeed() {
        coroutineScope.launch {
            isFeedLoading = true
            val result = sosRepository.fetchSosEvents(selectedFilter)
            result.onSuccess { events ->
                activeEvents = events
            }.onFailure { err ->
                showToast(err.message ?: "Failed to fetch active alerts.")
            }
            isFeedLoading = false
        }
    }

    fun loadHistory() {
        coroutineScope.launch {
            isHistoryLoading = true
            val result = sosRepository.fetchSosHistory()
            result.onSuccess { events ->
                historyEvents = events
            }.onFailure { err ->
                showToast(err.message ?: "Failed to load history.")
            }
            isHistoryLoading = false
        }
    }

    fun loadUsers(q: String = "") {
        coroutineScope.launch {
            isUsersLoading = true
            val result = userRepository.fetchUsers(q)
            result.onSuccess { users ->
                usersList = users
            }.onFailure { err ->
                showToast(err.message ?: "Failed to search users.")
            }
            isUsersLoading = false
        }
    }

    // Connect to Socket.IO and listen for real-time events
    LaunchedEffect(Unit) {
        socketManager.connect(context)
        loadActiveFeed()
    }

    LaunchedEffect(selectedFilter) {
        loadActiveFeed()
    }

    LaunchedEffect(currentTab) {
        when (currentTab) {
            AdminTab.FEED -> loadActiveFeed()
            AdminTab.HISTORY -> loadHistory()
            AdminTab.USERS -> loadUsers(userSearchQuery)
        }
    }

    LaunchedEffect(userSearchQuery) {
        if (currentTab == AdminTab.USERS) {
            loadUsers(userSearchQuery)
        }
    }

    // Real-time Socket Event Collector
    LaunchedEffect(Unit) {
        socketManager.events.collectLatest { event ->
            when (event) {
                is AdminSocketEvent.NewSos -> {
                    showToast("🚨 High-Priority SOS Dispatched!")
                    loadActiveFeed()
                }
                is AdminSocketEvent.SosUpdated -> {
                    loadActiveFeed()
                    inspectedIncident?.let { cur ->
                        coroutineScope.launch {
                            sosRepository.fetchSosDetails(cur.eventId).onSuccess { updated ->
                                inspectedIncident = updated
                            }
                        }
                    }
                }
            }
        }
    }

    val activeCount = remember(activeEvents) {
        activeEvents.count { it.isActive }
    }

    val filteredActiveFeed = remember(activeEvents, searchQuery) {
        if (searchQuery.isBlank()) {
            activeEvents
        } else {
            val q = searchQuery.lowercase()
            activeEvents.filter {
                it.userName.lowercase().contains(q) ||
                it.displayId.lowercase().contains(q) ||
                it.eventId.lowercase().contains(q) ||
                (it.message?.lowercase()?.contains(q) == true)
            }
        }
    }

    Scaffold(
        containerColor = colors.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AdminTopBar(
                activeAlertsCount = activeCount,
                socketState = socketState,
                onOpenMeshApp = onOpenMeshApp,
                onLogout = {
                    socketManager.disconnect()
                    sessionManager.clearSession()
                    onLogout()
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = colors.surfaceNested,
                tonalElevation = 8.dp
            ) {
                AdminTab.entries.forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (tab == AdminTab.FEED && activeCount > 0) {
                                        Badge(
                                            containerColor = colors.accentRed,
                                            contentColor = Color.White
                                        ) {
                                            Text("$activeCount")
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label
                                )
                            }
                        },
                        label = {
                            Text(
                                text = tab.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = colors.primary,
                            selectedTextColor = colors.primary,
                            unselectedIconColor = colors.textSecondary,
                            unselectedTextColor = colors.textSecondary,
                            indicatorColor = colors.primary.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val isTabletOrLandscape = maxWidth >= 600.dp

            when (currentTab) {
                AdminTab.FEED -> {
                    if (isTabletOrLandscape) {
                        // Two-Pane Command Console for Foldables and Tablets (>= 600dp)
                        Row(modifier = Modifier.fillMaxSize()) {
                            // Left Pane: Map / Radar Console (52% width)
                            Column(
                                modifier = Modifier
                                    .weight(0.52f)
                                    .fillMaxHeight()
                                    .padding(start = 16.dp, top = 8.dp, bottom = 12.dp, end = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "TACTICAL SITUATION MAP",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = colors.textPrimary,
                                        letterSpacing = 1.sp
                                    )
                                    Row(
                                        modifier = Modifier
                                            .background(colors.surfaceNested, RoundedCornerShape(8.dp))
                                            .border(1.dp, colors.divider, RoundedCornerShape(8.dp))
                                            .padding(2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clickable { isGoogleMapSelected = true }
                                                .background(
                                                    if (isGoogleMapSelected) colors.primary else Color.Transparent,
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "Google Maps",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isGoogleMapSelected) Color.White else colors.textSecondary
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clickable { isGoogleMapSelected = false }
                                                .background(
                                                    if (!isGoogleMapSelected) colors.primary else Color.Transparent,
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "Radar 2D",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (!isGoogleMapSelected) Color.White else colors.textSecondary
                                            )
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                ) {
                                    if (isGoogleMapSelected) {
                                        AdminGoogleMapView(
                                            incidents = filteredActiveFeed,
                                            selectedIncident = inspectedIncident,
                                            onIncidentSelected = { inspectedIncident = it },
                                            onInspectDetails = { inc ->
                                                inspectedIncident = inc
                                                coroutineScope.launch {
                                                    sosRepository.fetchSosDetails(inc.eventId).onSuccess {
                                                        inspectedIncident = it
                                                    }
                                                }
                                            }
                                        )
                                    } else {
                                        TacticalRadarCanvas(
                                            incidents = filteredActiveFeed,
                                            selectedIncident = inspectedIncident,
                                            onIncidentSelected = { inspectedIncident = it }
                                        )
                                    }
                                }
                            }

                            // Right Pane: Filters + Incident Cards (48% width)
                            Column(
                                modifier = Modifier
                                    .weight(0.48f)
                                    .fillMaxHeight()
                                    .padding(start = 6.dp, top = 4.dp, bottom = 12.dp, end = 16.dp)
                            ) {
                                IncidentFilterHeader(
                                    searchQuery = searchQuery,
                                    onSearchQueryChange = { searchQuery = it },
                                    selectedFilter = selectedFilter,
                                    onFilterSelected = { selectedFilter = it },
                                    isRadarView = isGoogleMapSelected,
                                    onToggleRadarView = { isGoogleMapSelected = !isGoogleMapSelected }
                                )

                                if (isFeedLoading && activeEvents.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = colors.primary)
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        contentPadding = PaddingValues(vertical = 10.dp)
                                    ) {
                                        if (filteredActiveFeed.isEmpty()) {
                                            item(key = "empty_feed_tablet") {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(top = 40.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.CheckCircle,
                                                            contentDescription = null,
                                                            tint = colors.textSecondary,
                                                            modifier = Modifier.size(42.dp)
                                                        )
                                                        Spacer(modifier = Modifier.height(10.dp))
                                                        Text(
                                                            text = "No active emergency alerts",
                                                            color = colors.textPrimary,
                                                            fontSize = 15.sp,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                            text = "All sectors normal or filters clear.",
                                                            color = colors.textSecondary,
                                                            fontSize = 12.sp,
                                                            fontFamily = FontFamily.Monospace
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            items(filteredActiveFeed, key = { it.eventId }) { inc ->
                                                IncidentCard(
                                                    incident = inc,
                                                    onInspectDetails = {
                                                        inspectedIncident = inc
                                                        coroutineScope.launch {
                                                            sosRepository.fetchSosDetails(inc.eventId).onSuccess {
                                                                inspectedIncident = it
                                                            }
                                                        }
                                                    },
                                                    onAcknowledge = {
                                                        coroutineScope.launch {
                                                            isActionLoading = true
                                                            sosRepository.acknowledgeSos(inc.eventId)
                                                                .onSuccess { updated ->
                                                                    showToast("Incident [${updated.displayId}] ACKNOWLEDGED")
                                                                    loadActiveFeed()
                                                                }
                                                                .onFailure { err ->
                                                                    showToast(err.message ?: "Acknowledge failed.")
                                                                }
                                                            isActionLoading = false
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Standard Vertical Stack Layout for Compact Phones (< 600dp)
                        Column(modifier = Modifier.fillMaxSize()) {
                            IncidentFilterHeader(
                                searchQuery = searchQuery,
                                onSearchQueryChange = { searchQuery = it },
                                selectedFilter = selectedFilter,
                                onFilterSelected = { selectedFilter = it },
                                isRadarView = isMapVisible,
                                onToggleRadarView = { isMapVisible = !isMapVisible }
                            )

                            if (isFeedLoading && activeEvents.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = colors.primary)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(vertical = 10.dp)
                                ) {
                                    // Tactical Map / Radar when enabled
                                    if (isMapVisible) {
                                        item(key = "tactical_map_view_phone") {
                                            Column {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(bottom = 6.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = if (isGoogleMapSelected) "SATELLITE / VECTOR MAP" else "TACTICAL RADAR 2D",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = colors.textSecondary
                                                    )
                                                    Row(
                                                        modifier = Modifier
                                                            .background(colors.surfaceNested, RoundedCornerShape(6.dp))
                                                            .border(1.dp, colors.divider, RoundedCornerShape(6.dp))
                                                            .padding(2.dp)
                                                    ) {
                                                        Text(
                                                            text = "Maps",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isGoogleMapSelected) Color.White else colors.textSecondary,
                                                            modifier = Modifier
                                                                .clickable { isGoogleMapSelected = true }
                                                                .background(
                                                                    if (isGoogleMapSelected) colors.primary else Color.Transparent,
                                                                    RoundedCornerShape(4.dp)
                                                                )
                                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                                        )
                                                        Text(
                                                            text = "Radar",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (!isGoogleMapSelected) Color.White else colors.textSecondary,
                                                            modifier = Modifier
                                                                .clickable { isGoogleMapSelected = false }
                                                                .background(
                                                                    if (!isGoogleMapSelected) colors.primary else Color.Transparent,
                                                                    RoundedCornerShape(4.dp)
                                                                )
                                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(260.dp)
                                                ) {
                                                    if (isGoogleMapSelected) {
                                                        AdminGoogleMapView(
                                                            incidents = filteredActiveFeed,
                                                            selectedIncident = inspectedIncident,
                                                            onIncidentSelected = { inspectedIncident = it },
                                                            onInspectDetails = { inc ->
                                                                inspectedIncident = inc
                                                                coroutineScope.launch {
                                                                    sosRepository.fetchSosDetails(inc.eventId).onSuccess {
                                                                        inspectedIncident = it
                                                                    }
                                                                }
                                                            }
                                                        )
                                                    } else {
                                                        TacticalRadarCanvas(
                                                            incidents = filteredActiveFeed,
                                                            selectedIncident = inspectedIncident,
                                                            onIncidentSelected = { inspectedIncident = it }
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                            }
                                        }
                                    }

                                    if (filteredActiveFeed.isEmpty()) {
                                        item(key = "empty_feed") {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 40.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.CheckCircle,
                                                        contentDescription = null,
                                                        tint = colors.textSecondary,
                                                        modifier = Modifier.size(42.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(10.dp))
                                                    Text(
                                                        text = "No active emergency alerts",
                                                        color = colors.textPrimary,
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "All sectors normal or filters clear.",
                                                        color = colors.textSecondary,
                                                        fontSize = 12.sp,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        items(filteredActiveFeed, key = { it.eventId }) { inc ->
                                            IncidentCard(
                                                incident = inc,
                                                onInspectDetails = {
                                                    inspectedIncident = inc
                                                    coroutineScope.launch {
                                                        sosRepository.fetchSosDetails(inc.eventId).onSuccess {
                                                            inspectedIncident = it
                                                        }
                                                    }
                                                },
                                                onAcknowledge = {
                                                    coroutineScope.launch {
                                                        isActionLoading = true
                                                        sosRepository.acknowledgeSos(inc.eventId)
                                                            .onSuccess { updated ->
                                                                showToast("Incident [${updated.displayId}] ACKNOWLEDGED")
                                                                loadActiveFeed()
                                                            }
                                                            .onFailure { err ->
                                                                showToast(err.message ?: "Acknowledge failed.")
                                                            }
                                                        isActionLoading = false
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                AdminTab.HISTORY -> {
                    SosHistoryTab(
                        historyEvents = historyEvents,
                        isLoading = isHistoryLoading,
                        onRefresh = { loadHistory() },
                        onInspectDetails = { event ->
                            inspectedIncident = event
                            coroutineScope.launch {
                                sosRepository.fetchSosDetails(event.eventId).onSuccess {
                                    inspectedIncident = it
                                }
                            }
                        }
                    )
                }

                AdminTab.USERS -> {
                    UserManagementTab(
                        users = usersList,
                        isLoading = isUsersLoading,
                        searchQuery = userSearchQuery,
                        onSearchQueryChange = { userSearchQuery = it },
                        onPromoteAdmin = { email ->
                            coroutineScope.launch {
                                isUsersLoading = true
                                userRepository.promoteAdmin(email)
                                    .onSuccess { msg ->
                                        showToast(msg)
                                        loadUsers(userSearchQuery)
                                    }
                                    .onFailure { err ->
                                        showToast(err.message ?: "Failed to promote user.")
                                    }
                                isUsersLoading = false
                            }
                        },
                        onRevokeAdmin = { userId ->
                            coroutineScope.launch {
                                isUsersLoading = true
                                userRepository.revokeAdmin(userId)
                                    .onSuccess { msg ->
                                        showToast(msg)
                                        loadUsers(userSearchQuery)
                                    }
                                    .onFailure { err ->
                                        showToast(err.message ?: "Failed to revoke admin privileges.")
                                    }
                                isUsersLoading = false
                            }
                        }
                    )
                }
            }

            // Inspect Incident BottomSheet
            inspectedIncident?.let { incident ->
                SosDetailBottomSheet(
                    incident = incident,
                    isActionLoading = isActionLoading,
                    onDismiss = { inspectedIncident = null },
                    onAcknowledge = {
                        coroutineScope.launch {
                            isActionLoading = true
                            sosRepository.acknowledgeSos(incident.eventId)
                                .onSuccess { updated ->
                                    inspectedIncident = updated
                                    showToast("Incident [${updated.displayId}] ACKNOWLEDGED")
                                    loadActiveFeed()
                                }
                                .onFailure { err ->
                                    showToast(err.message ?: "Acknowledge failed.")
                                }
                            isActionLoading = false
                        }
                    },
                    onResolve = {
                        coroutineScope.launch {
                            isActionLoading = true
                            sosRepository.resolveSos(incident.eventId)
                                .onSuccess { updated ->
                                    inspectedIncident = updated
                                    showToast("Incident [${updated.displayId}] RESOLVED")
                                    loadActiveFeed()
                                    loadHistory()
                                }
                                .onFailure { err ->
                                    showToast(err.message ?: "Resolve failed.")
                                }
                            isActionLoading = false
                        }
                    },
                    onAddNote = { text ->
                        coroutineScope.launch {
                            isActionLoading = true
                            sosRepository.addNote(incident.eventId, text)
                                .onSuccess { updated ->
                                    inspectedIncident = updated
                                    showToast("Case dispatch note appended.")
                                    loadActiveFeed()
                                }
                                .onFailure { err ->
                                    showToast(err.message ?: "Failed to append note.")
                                }
                            isActionLoading = false
                        }
                    }
                )
            }
        }
    }
}
