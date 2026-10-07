package com.example.zerogrid.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zerogrid.auth.AuthViewModel
import com.example.zerogrid.auth.ProfileUiState
import com.example.zerogrid.mesh.engine.MeshEngine
import com.example.zerogrid.navigation.Screen
import com.example.zerogrid.navigation.ZeroGridBottomBar
import com.example.zerogrid.network.AuthRepository
import com.example.zerogrid.network.ContactsRepository
import com.example.zerogrid.ui.components.ZeroGridDatePickerDialog
import com.example.zerogrid.ui.theme.BadgeGreen
import com.example.zerogrid.ui.theme.ZeroGridTheme
import com.example.zerogrid.util.ValidationUtils
import com.zerogrid.mesh.app.ui.UserRole
import com.zerogrid.mesh.app.ui.UserSessionManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigate: (Screen) -> Unit = {},
    onBack: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val sessionManager = remember { UserSessionManager.getInstance(context) }
    val authViewModel = remember { AuthViewModel(AuthRepository(sessionManager)) }
    val contactsRepository = remember { ContactsRepository(sessionManager) }
    val profileState by authViewModel.profileState.collectAsState()

    val meshEngine = remember { MeshEngine.getInstance(context) }
    val colors = ZeroGridTheme.colors
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // User data states
    var displayName by remember { mutableStateOf(sessionManager.getUserDisplayName().ifBlank { sessionManager.getUserName().ifBlank { "Survivor" } }) }
    var phoneNumber by remember { mutableStateOf(sessionManager.getPhoneNumber()) }
    var dateOfBirth by remember { mutableStateOf(sessionManager.getDateOfBirth()) }
    val email = sessionManager.getUserEmail() ?: "Not registered"
    val userRole = sessionManager.getUserRole() ?: UserRole.CITIZEN

    var isEditingName by remember { mutableStateOf(false) }
    var isEditingPhone by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Count emergency contacts
    var emergencyContactsCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        when (val res = contactsRepository.getContacts()) {
            is com.example.zerogrid.network.ContactsResult.Success -> emergencyContactsCount = res.data.size
            else -> emergencyContactsCount = 0
        }
    }

    val isPhoneValid = remember(phoneNumber) {
        phoneNumber.isBlank() || ValidationUtils.isValidPhone(phoneNumber)
    }

    val isProfileComplete = remember(displayName, phoneNumber, dateOfBirth) {
        displayName.isNotBlank() && phoneNumber.isNotBlank() && dateOfBirth.isNotBlank()
    }

    val isLoading = profileState is ProfileUiState.Loading

    // Calendar Picker Dialog
    if (showDatePicker) {
        ZeroGridDatePickerDialog(
            onDateSelected = { pickedDate ->
                dateOfBirth = pickedDate
                showDatePicker = false
                sessionManager.setDateOfBirth(pickedDate)
            },
            onDismiss = { showDatePicker = false }
        )
    }

    // Observe Profile Update feedback
    LaunchedEffect(profileState) {
        when (val state = profileState) {
            is ProfileUiState.Success -> {
                snackbarHostState.showSnackbar("Profile updated & synced to cloud", duration = SnackbarDuration.Short)
                authViewModel.resetProfileState()
            }
            is ProfileUiState.Error -> {
                snackbarHostState.showSnackbar("Sync failed: ${state.message}", duration = SnackbarDuration.Short)
                authViewModel.resetProfileState()
            }
            else -> {}
        }
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text("Sign Out?", fontWeight = FontWeight.Bold, color = colors.textPrimary)
            },
            text = {
                Text(
                    "You will be signed out of ZeroGrid. Mesh broadcast and background relays will be stopped.",
                    color = colors.textSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accentRed)
                ) {
                    Text("Sign Out", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.cardBackground
        )
    }

    Scaffold(
        containerColor = colors.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.background)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back",
                                tint = colors.textPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "My Profile & Identity",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    }

                    // Cloud Sync Action Button
                    FilledTonalButton(
                        onClick = {
                            if (!isPhoneValid) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Please enter a valid phone number")
                                }
                                return@FilledTonalButton
                            }
                            sessionManager.setUserName(displayName)
                            sessionManager.setPhoneNumber(phoneNumber)
                            sessionManager.setDateOfBirth(dateOfBirth)
                            meshEngine.setCustomDisplayName(displayName)

                            authViewModel.updateProfile(
                                displayName = displayName,
                                phoneNumber = phoneNumber,
                                dateOfBirth = dateOfBirth
                            )
                        },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = colors.primary.copy(alpha = 0.15f),
                            contentColor = colors.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = colors.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Syncing...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.CloudSync,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                HorizontalDivider(color = colors.divider, thickness = 1.dp)
            }
        },
        bottomBar = {
            ZeroGridBottomBar(
                currentScreen = Screen.SETTINGS,
                onNavigate = onNavigate
            )
        }
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
                            .widthIn(max = 700.dp)
                            .fillMaxWidth()
                    ) {
                        Column {
                            // ── Hero Profile Identity Card ───────────────────────
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(colors.divider)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Initials Avatar
                                    val initials = displayName.trim().take(2).uppercase().ifBlank { "ZG" }
                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .background(colors.primary.copy(alpha = 0.15f), CircleShape)
                                            .border(2.dp, colors.primary, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = initials,
                                            color = colors.primary,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = displayName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 20.sp,
                                                color = colors.textPrimary
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = email,
                                            fontSize = 13.sp,
                                            color = colors.textSecondary
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            // Role Badge
                                            Surface(
                                                color = colors.surfaceNested,
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = userRole.name,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                    color = colors.primary,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            // Profile Complete Badge
                                            Surface(
                                                color = if (isProfileComplete) BadgeGreen.copy(alpha = 0.15f) else colors.accentRed.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = if (isProfileComplete) "Verified" else "Incomplete",
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                    color = if (isProfileComplete) BadgeGreen else colors.accentRed,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // ── Section 1: Personal & Emergency Contact Info ────
                            ProfileSectionHeader("PERSONAL & EMERGENCY CONTACT INFO")
                            Spacer(modifier = Modifier.height(8.dp))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(colors.divider)
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    // 1. Display Name Field
                                    OutlinedTextField(
                                        value = displayName,
                                        onValueChange = {
                                            displayName = it
                                            sessionManager.setUserName(it)
                                            meshEngine.setCustomDisplayName(it)
                                        },
                                        label = { Text("Display Name / Alias") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.Badge,
                                                contentDescription = null,
                                                tint = colors.primary
                                            )
                                        },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = colors.primary,
                                            focusedLabelColor = colors.primary,
                                            unfocusedLabelColor = colors.textSecondary,
                                            focusedTextColor = colors.textPrimary,
                                            unfocusedTextColor = colors.textPrimary
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // 2. Email Field (Read-only Authenticated)
                                    OutlinedTextField(
                                        value = email,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Registered Email Address") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.Email,
                                                contentDescription = null,
                                                tint = colors.primary
                                            )
                                        },
                                        trailingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.Lock,
                                                contentDescription = "Verified Account",
                                                tint = colors.textSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = colors.divider,
                                            unfocusedBorderColor = colors.divider,
                                            focusedLabelColor = colors.textSecondary,
                                            unfocusedLabelColor = colors.textSecondary,
                                            focusedTextColor = colors.textSecondary,
                                            unfocusedTextColor = colors.textSecondary
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // 3. Emergency Phone Number with Validation
                                    OutlinedTextField(
                                        value = phoneNumber,
                                        onValueChange = {
                                            phoneNumber = it
                                            sessionManager.setPhoneNumber(it)
                                        },
                                        label = { Text("Emergency Phone Number") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.Phone,
                                                contentDescription = null,
                                                tint = colors.primary
                                            )
                                        },
                                        supportingText = {
                                            if (!isPhoneValid) {
                                                Text(
                                                    text = "Enter valid phone e.g. +14155552671",
                                                    color = colors.accentRed,
                                                    fontSize = 11.sp
                                                )
                                            } else {
                                                Text(
                                                    text = "Used for SOS SMS & coordinator dispatches",
                                                    color = colors.textSecondary,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        },
                                        isError = !isPhoneValid,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Phone,
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onDone = { focusManager.clearFocus() }
                                        ),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = colors.primary,
                                            focusedLabelColor = colors.primary,
                                            unfocusedLabelColor = colors.textSecondary,
                                            focusedTextColor = colors.textPrimary,
                                            unfocusedTextColor = colors.textPrimary
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // 4. Date of Birth with Calendar Picker
                                    OutlinedTextField(
                                        value = dateOfBirth.ifBlank { "Tap to select Date of Birth" },
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Date of Birth") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.CalendarToday,
                                                contentDescription = null,
                                                tint = colors.primary
                                            )
                                        },
                                        trailingIcon = {
                                            IconButton(onClick = { showDatePicker = true }) {
                                                Icon(
                                                    imageVector = Icons.Outlined.EditCalendar,
                                                    contentDescription = "Pick Date",
                                                    tint = colors.primary
                                                )
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showDatePicker = true },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = colors.primary,
                                            focusedLabelColor = colors.primary,
                                            unfocusedLabelColor = colors.textSecondary,
                                            focusedTextColor = if (dateOfBirth.isBlank()) colors.textSecondary else colors.textPrimary,
                                            unfocusedTextColor = if (dateOfBirth.isBlank()) colors.textSecondary else colors.textPrimary
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // ── Section 2: Emergency Preparedness ────────────────
                            ProfileSectionHeader("EMERGENCY PREPAREDNESS & CONTACTS")
                            Spacer(modifier = Modifier.height(8.dp))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(colors.divider)
                                )
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
                                                    .size(40.dp)
                                                    .background(colors.primary.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.ContactPhone,
                                                    contentDescription = null,
                                                    tint = colors.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = "Emergency Contacts",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = colors.textPrimary
                                                )
                                                Text(
                                                    text = "$emergencyContactsCount verified contacts linked",
                                                    fontSize = 12.sp,
                                                    color = colors.textSecondary
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = { onNavigate(Screen.EMERGENCY_CONTACTS) },
                                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "Manage",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (colors.isDark) Color.Black else Color.White
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // ── Section 3: Mesh Hardware Identity ────────────────
                            ProfileSectionHeader("CRYPTOGRAPHIC NODE HARDWARE")
                            Spacer(modifier = Modifier.height(8.dp))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(colors.divider)
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Local Node ID (Hardware Fingerprint)",
                                                fontSize = 12.sp,
                                                color = colors.textSecondary
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = meshEngine.localNodeId,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = colors.primary
                                            )
                                        }
                                        Surface(
                                            color = BadgeGreen.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "Ed25519",
                                                color = BadgeGreen,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(28.dp))

                            // ── Sign Out Button ──────────────────────────────────
                            OutlinedButton(
                                onClick = { showLogoutDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.accentRed),
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.accentRed.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ExitToApp,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Sign Out of ZeroGrid",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileSectionHeader(text: String) {
    val colors = ZeroGridTheme.colors
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = colors.textSecondary,
        modifier = Modifier.padding(start = 4.dp)
    )
}
