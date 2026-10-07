package com.example.zerogrid.family

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import com.example.zerogrid.network.ChildLocationDto
import com.example.zerogrid.network.FamilyLinkDto
import com.example.zerogrid.ui.theme.ZeroGridTheme
import com.example.zerogrid.util.ValidationUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyLinksScreen(
    onNavigate: (Screen) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val viewModel = remember { FamilyViewModel() }
    val colors = ZeroGridTheme.colors

    val links by viewModel.links.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val childLocation by viewModel.childLocation.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showLinkDialog by remember { mutableStateOf(false) }
    var selectedChildIdForLocation by remember { mutableStateOf<String?>(null) }
    var linkToRevoke by remember { mutableStateOf<FamilyLinkDto?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadLinks()
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is FamilyUiState.Success -> {
                state.message?.let {
                    snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
                }
            }
            is FamilyUiState.Error -> {
                snackbarHostState.showSnackbar(state.message, duration = SnackbarDuration.Short)
            }
            else -> {}
        }
    }

    if (linkToRevoke != null) {
        val target = linkToRevoke!!
        AlertDialog(
            onDismissRequest = { linkToRevoke = null },
            containerColor = colors.cardBackground,
            title = {
                Text("Revoke Family Link", color = colors.textPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Are you sure you want to revoke the link with ${target.childName ?: target.childEmail ?: "this account"}?",
                    color = colors.textSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.revokeLink(target.id)
                    linkToRevoke = null
                }) {
                    Text("Revoke", color = colors.accentRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { linkToRevoke = null }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    if (showLinkDialog) {
        LinkChildDialog(
            uiState = uiState,
            onDismiss = {
                showLinkDialog = false
                viewModel.resetState()
            },
            onSubmit = { email ->
                viewModel.requestLink(email)
                showLinkDialog = false
            }
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
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Family Links",
                            color = colors.textPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Guardian & Child Safety Network",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    IconButton(onClick = { showLinkDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.PersonAdd,
                            contentDescription = "Link Child",
                            tint = colors.primary
                        )
                    }
                }
                HorizontalDivider(color = colors.divider)
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                // Info Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.cardBackground,
                    border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(colors.primary.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FamilyRestroom,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "REAL-TIME SAFETY LINKS",
                                color = colors.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Link child accounts to monitor safety status and retrieve last known location during emergencies.",
                                color = colors.textSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            if (links.isEmpty()) {
                item {
                    EmptyFamilyLinksCard(onLinkClick = { showLinkDialog = true })
                }
            } else {
                items(links) { link ->
                    FamilyLinkItemCard(
                        link = link,
                        isLocationVisible = selectedChildIdForLocation == (link.childId ?: link.id),
                        childLocation = if (selectedChildIdForLocation == (link.childId ?: link.id)) childLocation else null,
                        onAccept = { viewModel.acceptLink(link.id) },
                        onRevoke = { linkToRevoke = link },
                        onToggleLocation = {
                            val id = link.childId ?: link.id
                            if (selectedChildIdForLocation == id) {
                                selectedChildIdForLocation = null
                            } else {
                                selectedChildIdForLocation = id
                                viewModel.fetchChildLocation(id)
                            }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun FamilyLinkItemCard(
    link: FamilyLinkDto,
    isLocationVisible: Boolean,
    childLocation: ChildLocationDto?,
    onAccept: () -> Unit,
    onRevoke: () -> Unit,
    onToggleLocation: () -> Unit
) {
    val colors = ZeroGridTheme.colors
    val displayName = link.childName ?: link.childEmail ?: "Family Member"
    val initials = displayName.take(2).uppercase()
    val isPending = link.status.equals("PENDING", ignoreCase = true)
    val isAccepted = link.status.equals("ACCEPTED", ignoreCase = true)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, colors.divider)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(colors.surfaceNested, RoundedCornerShape(12.dp))
                        .border(1.dp, colors.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        color = colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    link.childEmail?.let {
                        Text(
                            text = it,
                            color = colors.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isAccepted) colors.primary.copy(alpha = 0.15f) else colors.accentRed.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = link.status.uppercase(),
                        color = if (isAccepted) colors.primary else colors.accentRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isPending) {
                    Button(
                        onClick = onAccept,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Accept Link", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (isAccepted) {
                    OutlinedButton(
                        onClick = onToggleLocation,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, colors.primary),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Outlined.LocationOn, null, tint = colors.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Location", color = colors.primary, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                TextButton(onClick = onRevoke, modifier = Modifier.height(34.dp)) {
                    Text("Revoke", color = colors.accentRed, fontSize = 12.sp)
                }
            }

            // Expanded Location Info
            if (isLocationVisible) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.surfaceNested,
                    border = BorderStroke(1.dp, colors.divider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "LAST KNOWN LOCATION",
                            color = colors.primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (childLocation?.location?.coordinates != null && childLocation.location.coordinates.size >= 2) {
                            val lng = childLocation.location.coordinates[0]
                            val lat = childLocation.location.coordinates[1]
                            Text(
                                text = "Coordinates: $lat, $lng",
                                color = colors.textPrimary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            childLocation.lastLocationAt?.let {
                                Text(
                                    text = "Updated: $it",
                                    color = colors.textSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        } else {
                            Text(
                                text = "Location telemetry currently offline or unavailable.",
                                color = colors.textSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyFamilyLinksCard(onLinkClick: () -> Unit) {
    val colors = ZeroGridTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, colors.divider)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.LinkOff,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No Family Links Configured",
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Send a link request to your child's email address to monitor emergency telemetry.",
                color = colors.textSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onLinkClick,
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Link Child Account", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LinkChildDialog(
    uiState: FamilyUiState,
    onDismiss: () -> Unit,
    onSubmit: (email: String) -> Unit
) {
    val colors = ZeroGridTheme.colors
    var email by remember { mutableStateOf("") }
    val isValid = remember(email) { ValidationUtils.isValidEmail(email) }
    val isLoading = uiState is FamilyUiState.Loading

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.cardBackground,
        title = {
            Text("Link Child Account", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Enter your child's registered ZeroGrid email to send an account linking invitation.",
                    color = colors.textSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Child Email Address", color = colors.textSecondary, fontSize = 13.sp) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Outlined.Email, null, tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedContainerColor = colors.surfaceNested,
                        unfocusedContainerColor = colors.surfaceNested,
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.divider,
                        focusedLabelColor = colors.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(email.trim()) },
                enabled = !isLoading && isValid,
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Send Request", color = if (colors.isDark) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        }
    )
}
