package com.example.zerogrid.settings

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zerogrid.auth.AuthViewModel
import com.example.zerogrid.auth.ProfileUiState
import com.example.zerogrid.mesh.engine.MeshChannelMode
import com.example.zerogrid.mesh.engine.MeshEngine
import com.example.zerogrid.navigation.Screen
import com.example.zerogrid.navigation.ZeroGridBottomBar
import com.example.zerogrid.network.AuthRepository
import com.example.zerogrid.ui.components.ZeroGridDatePickerDialog
import com.example.zerogrid.ui.theme.BadgeGreen
import com.example.zerogrid.ui.theme.ZeroGridTheme
import com.example.zerogrid.util.ValidationUtils
import com.zerogrid.mesh.app.ui.UserSessionManager
import com.example.zerogrid.ui.ThemeMode
import com.example.zerogrid.ui.ThemePreferenceManager
import kotlinx.coroutines.launch

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun SettingsScreen(
    onNavigate: (Screen) -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val sessionManager = remember { UserSessionManager.getInstance(context) }
    val authViewModel = remember { AuthViewModel(AuthRepository(sessionManager)) }
    val meshEngine = remember { MeshEngine.getInstance(context) }
    val peers by meshEngine.connectedPeers.collectAsState()
    val themePreferenceManager = remember { ThemePreferenceManager(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val currentThemeMode by themePreferenceManager.themeMode.collectAsState(initial = themePreferenceManager.getThemeModeSync())
    val isMeshActive by meshEngine.isMeshActive.collectAsState()
    val activeChannelMode by meshEngine.activeChannelMode.collectAsState()
    val colors = ZeroGridTheme.colors

    var meshRelayEnabled by remember { mutableStateOf(true) }
    var offlineDiscoveryEnabled by remember { mutableStateOf(true) }
    var autoConnectEnabled by remember { mutableStateOf(true) }
    var stealthModeEnabled by remember { mutableStateOf(false) }

    // Channel selection dialog
    var showChannelDialog by remember { mutableStateOf(false) }

    // Edit alias/name dialog
    var showEditNameDialog by remember { mutableStateOf(false) }
    var editNameValue by remember { mutableStateOf(sessionManager.getUserName().ifBlank { meshEngine.localNodeId }) }
    var displayName by remember { mutableStateOf(sessionManager.getUserName().ifBlank { meshEngine.localNodeId }) }

    // Reset identity confirmation dialog
    var showResetDialog by remember { mutableStateOf(false) }

    // Edit profile dialog state
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var editPhoneValue by remember { mutableStateOf(sessionManager.getPhoneNumber()) }
    var editDobValue by remember { mutableStateOf(sessionManager.getDateOfBirth()) }
    var showSettingsDatePicker by remember { mutableStateOf(false) }

    if (showSettingsDatePicker) {
        ZeroGridDatePickerDialog(
            onDateSelected = { pickedDate ->
                editDobValue = pickedDate
                showSettingsDatePicker = false
            },
            onDismiss = { showSettingsDatePicker = false }
        )
    }

    if (showChannelDialog) {
        AlertDialog(
            onDismissRequest = { showChannelDialog = false },
            title = {
                Text(
                    text = "Communication Channel",
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "ZeroGrid uses dedicated single-radio exclusive transport to eliminate packet collisions.",
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                meshEngine.setMeshChannelMode(MeshChannelMode.BLE)
                                showChannelDialog = false
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = activeChannelMode == MeshChannelMode.BLE,
                            onClick = {
                                meshEngine.setMeshChannelMode(MeshChannelMode.BLE)
                                showChannelDialog = false
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = colors.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Bluetooth Low Energy (BLE)",
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Low battery drain • Discrete mesh radius",
                                color = colors.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                meshEngine.setMeshChannelMode(MeshChannelMode.WIFI_DIRECT)
                                showChannelDialog = false
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = activeChannelMode == MeshChannelMode.WIFI_DIRECT,
                            onClick = {
                                meshEngine.setMeshChannelMode(MeshChannelMode.WIFI_DIRECT)
                                showChannelDialog = false
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = colors.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Wi-Fi Direct (P2P)",
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "High bandwidth • Fast files & media",
                                color = colors.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showChannelDialog = false }) {
                    Text("Close", color = colors.primary, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = colors.cardBackground
        )
    }

    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = {
                Text(
                    text = "Edit Mesh Node Alias",
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "This alias will be broadcast to nearby peers across the mesh.",
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = editNameValue,
                        onValueChange = { editNameValue = it },
                        label = { Text("Display Alias") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            focusedLabelColor = colors.primary,
                            unfocusedLabelColor = colors.textSecondary,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editNameValue.isNotBlank()) {
                            displayName = editNameValue.trim()
                            sessionManager.setUserName(displayName)
                            meshEngine.setCustomDisplayName(displayName)
                        }
                        showEditNameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text("Save", color = if (colors.isDark) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.cardBackground
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    text = "Reset Cryptographic Identity?",
                    fontWeight = FontWeight.Bold,
                    color = colors.accentRed
                )
            },
            text = {
                Text(
                    text = "This will generate a completely new cryptographic node ID and keypair. Existing paired peer trust relationships will be revoked.",
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showResetDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accentRed)
                ) {
                    Text("Confirm Reset", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.cardBackground
        )
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

            // Separated into distinct `item` blocks for improved rendering responsiveness and scroll efficiency
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(
                    top = 8.dp,
                    bottom = 16.dp,
                    start = 0.dp,
                    end = 0.dp  ),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Header Item
                item(key = "header") {
                    Box(modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Settings",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Mesh radios, identity & offline storage",
                                    fontSize = 13.sp,
                                    color = colors.textSecondary
                                )
                            }

                            Surface(
                                color = if (isMeshActive) BadgeGreen.copy(alpha = 0.12f) else colors.surfaceNested,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CellTower,
                                        contentDescription = null,
                                        tint = if (isMeshActive) BadgeGreen else colors.textSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isMeshActive) "Active Mesh" else "Mesh Offline",
                                        color = if (isMeshActive) BadgeGreen else colors.textSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Profile Item
                item(key = "profile") {
                    Box(modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
                        val userEmail = sessionManager.getUserEmail() ?: ""
                        val userRole = sessionManager.getUserRole()?.name ?: "CITIZEN"
                        val userPhone = sessionManager.getPhoneNumber()
                        val userDob = sessionManager.getDateOfBirth()
                        val isProfileComplete = displayName.isNotBlank() && userPhone.isNotBlank() && userDob.isNotBlank()

                        IdentityProfileHeroCard(
                            alias = displayName,
                            email = userEmail,
                            role = userRole,
                            isProfileComplete = isProfileComplete,
                            nodeId = meshEngine.localNodeId,
                            peersCount = peers.size,
                            onEditClick = { showEditNameDialog = true },
                            onProfileClick = { onNavigate(Screen.PROFILE) }
                        )
                    }
                }

                // Connectivity Section
                item(key = "connectivity") {
                    Box(modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
                        Column {
                            SectionLabel(text = "MESH RADIOS & CONNECTIVITY")
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(colors.divider))
                            ) {
                                Column {
                                    SettingsSwitchItem(
                                        icon = Icons.Outlined.AltRoute,
                                        title = "Mesh Relay Mode",
                                        subtitle = "Help pass encrypted packets across peer hops",
                                        checked = meshRelayEnabled,
                                        onCheckedChange = { meshRelayEnabled = it }
                                    )
                                    HorizontalDivider(color = colors.divider, thickness = 1.dp)
                                    SettingsActionItem(
                                        icon = Icons.Outlined.Sensors,
                                        title = "Active Channel: ${activeChannelMode.label}",
                                        subtitle = if (activeChannelMode == MeshChannelMode.BLE) "Bluetooth Low Energy • Single-radio exclusive" else "Wi-Fi Direct P2P • Single-radio exclusive",
                                        actionText = "Change",
                                        onClick = { showChannelDialog = true }
                                    )
                                    HorizontalDivider(color = colors.divider, thickness = 1.dp)
                                    SettingsSwitchItem(
                                        icon = Icons.Outlined.GroupAdd,
                                        title = "Auto-Connect Trusted Peers",
                                        subtitle = "Instantly pair with devices in your trusted ledger",
                                        checked = autoConnectEnabled,
                                        onCheckedChange = { autoConnectEnabled = it }
                                    )
                                }
                            }
                        }
                    }
                }

                // Security Section
                item(key = "security") {
                    Box(modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
                        Column {
                            SectionLabel(text = "SECURITY & ENCRYPTION")
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(colors.divider))
                            ) {
                                Column {
                                    SettingsBadgeItem(
                                        icon = Icons.Outlined.Shield,
                                        title = "End-to-End Encryption",
                                        subtitle = "Zero-Trust Ed25519 & ChaCha20",
                                        badgeText = "Active / Verified",
                                        badgeColor = BadgeGreen
                                    )
                                    HorizontalDivider(color = colors.divider, thickness = 1.dp)
                                    SettingsSwitchItem(
                                        icon = Icons.Outlined.VisibilityOff,
                                        title = "Stealth / Anonymous Mode",
                                        subtitle = "Mask device identifier from non-whitelisted peers",
                                        checked = stealthModeEnabled,
                                        onCheckedChange = { stealthModeEnabled = it }
                                    )
                                }
                            }
                        }
                    }
                }

                // Appearance Section
                item(key = "appearance") {
                    Box(modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
                        Column {
                            SectionLabel(text = "APPEARANCE")
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(colors.divider))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Theme",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                        val options = listOf(ThemeMode.LIGHT, ThemeMode.SYSTEM, ThemeMode.DARK)
                                        val labels = listOf("Light", "System", "Dark")
                                        options.forEachIndexed { index, mode ->
                                            SegmentedButton(
                                                selected = currentThemeMode == mode,
                                                onClick = {
                                                    scope.launch {
                                                        themePreferenceManager.setThemeMode(mode)
                                                    }
                                                },
                                                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                                            ) {
                                                Text(labels[index])
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Emergency Section (Highly Responsive & Themed)
                item(key = "emergency") {
                    Box(modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
                        Column {
                            SectionLabel(text = "EMERGENCY & CRISIS RESPONSE")
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(colors.divider))
                            ) {
                                Column {
                                    SettingsActionItem(
                                        icon = Icons.Outlined.ContactPhone,
                                        title = "Emergency Contacts",
                                        subtitle = "Configured SOS recipients • Direct mesh pings",
                                        actionText = "Manage",
                                        onClick = { onNavigate(Screen.EMERGENCY_CONTACTS) }
                                    )
                                    HorizontalDivider(color = colors.divider, thickness = 1.dp)
                                    SettingsActionItem(
                                        icon = Icons.Outlined.FamilyRestroom,
                                        title = "Family Links",
                                        subtitle = "Guardian network • Child safety & location telemetry",
                                        actionText = "Manage",
                                        onClick = { onNavigate(Screen.FAMILY_LINKS) }
                                    )
                                    HorizontalDivider(color = colors.divider, thickness = 1.dp)
                                    // Colored specifically to stand out visually for immediate crisis access
                                    SettingsActionItem(
                                        icon = Icons.Outlined.WarningAmber,
                                        title = "SOS Emergency Center",
                                        subtitle = "Distress beacons & real-time responder alerts",
                                        actionText = "Open",
                                        iconTint = colors.accentRed,
                                        actionTint = colors.accentRed,
                                        boxBgTint = colors.accentRed.copy(alpha = 0.15f),
                                        onClick = { onNavigate(Screen.SOS_CENTER) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Cryptographic Identity Section
                item(key = "identity") {
                    Box(modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
                        Column {
                            SectionLabel(text = "CRYPTOGRAPHIC IDENTITY")
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = colors.accentRed.copy(alpha = 0.05f)),
                                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(colors.accentRed.copy(alpha = 0.25f)))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(colors.accentRed.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.RestartAlt,
                                                contentDescription = null,
                                                tint = colors.accentRed,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Reset Identity & Keys",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = colors.textPrimary
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Revokes current peer trust and produces a fresh cryptographic mesh ID.",
                                                fontSize = 12.sp,
                                                color = colors.textSecondary,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                    OutlinedButton(
                                        onClick = { showResetDialog = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.accentRed),
                                        border = BorderStroke(1.dp, colors.accentRed.copy(alpha = 0.6f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.RestartAlt,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Reset Mesh Identity (Requires confirmation)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Sign Out Button Item
                item(key = "signout") {
                    Box(modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
                        OutlinedButton(
                            onClick = onLogout,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textSecondary),
                            border = BorderStroke(1.dp, colors.divider)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ExitToApp,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sign Out of Device",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IdentityProfileHeroCard(
    alias: String,
    email: String,
    role: String,
    isProfileComplete: Boolean,
    nodeId: String,
    peersCount: Int,
    onEditClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    val colors = ZeroGridTheme.colors
    val initials = if (alias.length >= 2) alias.take(2).uppercase() else "ZG"

    Card(
        onClick = onProfileClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(colors.divider))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large Avatar with online badge
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(colors.primary, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            color = if (colors.isDark) Color.Black else Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(BadgeGreen, CircleShape)
                            .border(2.dp, colors.cardBackground, CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
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
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = onEditClick,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "Edit alias",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        Surface(
                            color = colors.surfaceNested,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = role,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                color = colors.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (email.isNotBlank()) {
                        Text(
                            text = email,
                            color = colors.textSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Node Reachability Status
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.CellTower,
                            contentDescription = null,
                            tint = BadgeGreen,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Direct & Relay Node • $peersCount Peers Synced",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = colors.divider, thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Action row: View & Edit Full Profile
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "View & Edit Full Profile",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (isProfileComplete) BadgeGreen.copy(alpha = 0.12f) else colors.accentRed.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (isProfileComplete) "Verified" else "Action Needed",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = if (isProfileComplete) BadgeGreen else colors.accentRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    val colors = ZeroGridTheme.colors
    Text(
        text = text,
        color = colors.textSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 1.sp
    )
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = ZeroGridTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(colors.surfaceNested, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = null, // Managed by Row click for better hit-box responsiveness
            colors = SwitchDefaults.colors(
                checkedThumbColor = if (colors.isDark) Color.Black else Color.White,
                checkedTrackColor = colors.primary,
                uncheckedThumbColor = colors.textSecondary,
                uncheckedTrackColor = colors.surfaceNested
            )
        )
    }
}

@Composable
private fun SettingsActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionText: String,
    iconTint: Color = ZeroGridTheme.colors.primary,
    actionTint: Color = ZeroGridTheme.colors.primary,
    boxBgTint: Color = ZeroGridTheme.colors.surfaceNested,
    onClick: () -> Unit
) {
    val colors = ZeroGridTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(boxBgTint, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = actionText,
                color = actionTint,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SettingsBadgeItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeText: String,
    badgeColor: Color
) {
    val colors = ZeroGridTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(colors.surfaceNested, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }
        }

        Surface(
            color = badgeColor.copy(alpha = 0.12f),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = badgeText,
                    color = badgeColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}