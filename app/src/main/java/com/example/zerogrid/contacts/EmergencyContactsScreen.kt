package com.example.zerogrid.contacts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zerogrid.network.ContactDto
import com.example.zerogrid.network.ContactsRepository
import com.example.zerogrid.navigation.Screen
import com.example.zerogrid.ui.theme.*
import com.example.zerogrid.util.ValidationUtils
import com.zerogrid.mesh.app.ui.UserSessionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyContactsScreen(
    onNavigate: (Screen) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val sessionManager = remember { UserSessionManager.getInstance(context) }
    val repository = remember { ContactsRepository(sessionManager) }
    val viewModel = remember { ContactsViewModel(repository) }
    val colors = ZeroGridTheme.colors

    val contacts by viewModel.contacts.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val addState by viewModel.addState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    var showAddDialog by remember { mutableStateOf(false) }
    var contactToDelete by remember { mutableStateOf<ContactDto?>(null) }

    // Load contacts on first display
    LaunchedEffect(Unit) {
        viewModel.loadContacts()
    }

    // Handle add contact state changes
    LaunchedEffect(addState) {
        when (val state = addState) {
            is AddContactState.Success -> {
                showAddDialog = false
                viewModel.resetAddState()
                snackbarHostState.showSnackbar(
                    "Added ${state.contact.name} to emergency contacts",
                    duration = SnackbarDuration.Short
                )
            }
            is AddContactState.Error -> {
                // Keep dialog open to show error
            }
            else -> {}
        }
    }

    // Handle delete confirmation dialog
    if (contactToDelete != null) {
        val target = contactToDelete!!
        AlertDialog(
            onDismissRequest = { contactToDelete = null },
            containerColor = colors.cardBackground,
            title = {
                Text("Remove Contact", color = colors.textPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Are you sure you want to remove ${target.name} from your emergency contacts?",
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteContact(target.id)
                    contactToDelete = null
                }) {
                    Text("Remove", color = colors.accentRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { contactToDelete = null }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    // Add Contact Dialog
    if (showAddDialog) {
        AddContactDialog(
            addState = addState,
            onDismiss = {
                showAddDialog = false
                viewModel.resetAddState()
            },
            onAdd = { emailOrPhone, label ->
                viewModel.addContact(emailOrPhone, label)
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
                            text = "Emergency Contacts",
                            color = colors.textPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "SOS Dispatch List",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.PersonAdd,
                            contentDescription = "Add Contact",
                            tint = colors.primary
                        )
                    }
                }
                HorizontalDivider(color = colors.divider)
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

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
                                imageVector = Icons.Outlined.Emergency,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "When you trigger an online SOS alert, these verified contacts will be notified with your distress signal and coordinates.",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TRUSTED CONTACTS (${contacts.size})",
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { showAddDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add New", color = colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (uiState is ContactsUiState.Loading && contacts.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = colors.primary, strokeWidth = 2.dp)
                    }
                } else if (contacts.isEmpty()) {
                    // Empty State
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(colors.surfaceNested, CircleShape)
                                .border(1.dp, colors.divider, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContactPhone,
                                contentDescription = null,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Contacts Configured",
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Add family, rescue coordinators, or peers so they can receive emergency broadcasts when you need help.",
                            color = colors.textSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { showAddDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                        ) {
                            Icon(Icons.Outlined.Add, null, tint = if (colors.isDark) Color.Black else Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Emergency Contact", color = if (colors.isDark) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        items(contacts, key = { it.id.ifEmpty { it.hashCode().toString() } }) { contact ->
                            ContactItemCard(
                                contact = contact,
                                onDelete = { contactToDelete = contact }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Contact Item Card ───────────────────────────────────────────────────────

@Composable
private fun ContactItemCard(
    contact: ContactDto,
    onDelete: () -> Unit
) {
    val colors = ZeroGridTheme.colors
    val initials = contact.name.trim().take(2).uppercase().ifEmpty { "EC" }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, colors.divider)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(46.dp)
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

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = contact.name,
                        color = colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = colors.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = contact.relationship.uppercase(),
                            color = colors.primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = contact.phoneNumber,
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Remove Contact",
                    tint = colors.accentRed.copy(alpha = 0.85f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ── Add Contact Modal Dialog ───────────────────────────────────────────────

@Composable
private fun AddContactDialog(
    addState: AddContactState,
    onDismiss: () -> Unit,
    onAdd: (emailOrPhone: String, label: String) -> Unit
) {
    val colors = ZeroGridTheme.colors
    var emailOrPhone by remember { mutableStateOf("") }
    var selectedRelationship by remember { mutableStateOf("Family") }
    var customLabel by remember { mutableStateOf("") }

    val presetRelationships = listOf("Family", "Parent", "Spouse", "Sibling", "Friend", "Doctor", "Other")

    val trimmedInput = emailOrPhone.trim()
    val isInputValid = remember(trimmedInput) {
        trimmedInput.isNotBlank() && ValidationUtils.isValidEmailOrPhone(trimmedInput)
    }

    val isLoading = addState is AddContactState.Loading
    val errorMessage = (addState as? AddContactState.Error)?.message

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.cardBackground,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(colors.primary.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PersonSearch,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text("Add Emergency Contact", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Add a registered ZeroGrid user by their Email or Phone. They will receive your emergency broadcast notifications and GPS coordinates when you trigger an SOS.",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = emailOrPhone,
                    onValueChange = { emailOrPhone = it },
                    label = { Text("Email or Phone Number", color = colors.textSecondary, fontSize = 13.sp) },
                    placeholder = { Text("alice@example.com or +919999999999", color = colors.textSecondary.copy(alpha = 0.5f), fontSize = 12.sp) },
                    singleLine = true,
                    isError = trimmedInput.isNotBlank() && !isInputValid,
                    leadingIcon = {
                        Icon(Icons.Outlined.PersonSearch, null, tint = colors.textSecondary, modifier = Modifier.size(20.dp))
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

                if (trimmedInput.isNotBlank() && !isInputValid) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enter a valid email (user@example.com) or phone number (7-15 digits)",
                        color = colors.accentRed,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "LABEL / RELATIONSHIP",
                    color = colors.textSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetRelationships.take(4).forEach { rel ->
                        FilterChip(
                            selected = selectedRelationship == rel,
                            onClick = { selectedRelationship = rel },
                            label = { Text(rel, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.primary.copy(alpha = 0.2f),
                                selectedLabelColor = colors.primary,
                                containerColor = colors.surfaceNested,
                                labelColor = colors.textSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedRelationship == rel,
                                selectedBorderColor = colors.primary,
                                borderColor = colors.divider
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetRelationships.drop(4).forEach { rel ->
                        FilterChip(
                            selected = selectedRelationship == rel,
                            onClick = { selectedRelationship = rel },
                            label = { Text(rel, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.primary.copy(alpha = 0.2f),
                                selectedLabelColor = colors.primary,
                                containerColor = colors.surfaceNested,
                                labelColor = colors.textSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedRelationship == rel,
                                selectedBorderColor = colors.primary,
                                borderColor = colors.divider
                            )
                        )
                    }
                }

                if (selectedRelationship == "Other") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customLabel,
                        onValueChange = { customLabel = it },
                        label = { Text("Custom Label (e.g. Neighbor, Guardian)", color = colors.textSecondary, fontSize = 12.sp) },
                        singleLine = true,
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

                if (!errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colors.accentRed.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, colors.accentRed.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = colors.accentRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage,
                                color = colors.accentRed,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalLabel = if (selectedRelationship == "Other") {
                        customLabel.trim().ifEmpty { "Emergency Contact" }
                    } else {
                        selectedRelationship
                    }
                    onAdd(trimmedInput, finalLabel)
                },
                enabled = !isLoading && isInputValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    disabledContainerColor = colors.primary.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = if (colors.isDark) Color.Black else Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Add Contact", color = if (colors.isDark) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel", color = colors.textSecondary)
            }
        }
    )
}