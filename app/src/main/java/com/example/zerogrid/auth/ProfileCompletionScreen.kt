package com.example.zerogrid.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zerogrid.network.AuthRepository
import com.example.zerogrid.ui.theme.*
import com.zerogrid.mesh.app.ui.UserRole
import com.zerogrid.mesh.app.ui.UserSessionManager

import com.example.zerogrid.ui.components.ZeroGridDatePickerDialog
import com.example.zerogrid.util.ValidationUtils

@Composable
fun ProfileCompletionScreen(
    sessionManager: UserSessionManager,
    onProfileCompleted: () -> Unit,
    onSkip: () -> Unit
) {
    val viewModel = remember { AuthViewModel(AuthRepository(sessionManager)) }
    val profileState by viewModel.profileState.collectAsState()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    var phoneNumber by remember { mutableStateOf(sessionManager.getPhoneNumber()) }
    var dateOfBirth by remember { mutableStateOf(sessionManager.getDateOfBirth()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val isPhoneValid = remember(phoneNumber) {
        phoneNumber.isBlank() || ValidationUtils.isValidPhone(phoneNumber)
    }

    val userRole = sessionManager.getUserRole() ?: UserRole.CITIZEN
    val displayName = sessionManager.getUserDisplayName() ?: "Survivor"
    val email = sessionManager.getUserEmail() ?: ""

    val isLoading = profileState is ProfileUiState.Loading

    // Calendar Date Picker Modal
    if (showDatePicker) {
        ZeroGridDatePickerDialog(
            onDateSelected = { pickedDate ->
                dateOfBirth = pickedDate
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    // Observe state updates
    LaunchedEffect(profileState) {
        when (val state = profileState) {
            is ProfileUiState.Success -> {
                viewModel.resetProfileState()
                onProfileCompleted()
            }
            is ProfileUiState.Error -> {
                snackbarHostState.showSnackbar(state.message, duration = SnackbarDuration.Short)
                viewModel.resetProfileState()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DarkBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Brand icon
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = CardBackground,
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.35f)),
                    modifier = Modifier.size(76.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = PrimaryCyan,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Complete Your Profile",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Provide your emergency contact details to help rescue coordinators and mesh peers identify you in crisis.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Summary card with registered info
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CardBackground,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DividerColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = displayName,
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = email,
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PrimaryCyan.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = PrimaryCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = userRole.name,
                                    color = PrimaryCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Phone number
                AuthTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = "Phone Number (with country code e.g. +1234567890)",
                    leadingIcon = Icons.Outlined.Phone,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            focusManager.clearFocus()
                            showDatePicker = true
                        }
                    )
                )

                if (phoneNumber.isNotBlank() && !isPhoneValid) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Invalid format. Use 7-15 digits (e.g. +1 555-0199)",
                        color = Color(0xFFFF5252),
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth().padding(start = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Date of birth (Clickable calendar picker)
                Surface(
                    onClick = {
                        focusManager.clearFocus()
                        showDatePicker = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = CardBackground,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (dateOfBirth.isNotBlank()) PrimaryCyan else DividerColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            tint = if (dateOfBirth.isNotBlank()) PrimaryCyan else TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Date of Birth",
                                color = if (dateOfBirth.isNotBlank()) PrimaryCyan else TextSecondary,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = dateOfBirth.ifEmpty { "Select date from calendar" },
                                color = if (dateOfBirth.isNotBlank()) TextPrimary else TextSecondary,
                                fontSize = 15.sp,
                                fontWeight = if (dateOfBirth.isNotBlank()) FontWeight.Medium else FontWeight.Normal
                            )
                        }
                        Text(
                            text = "Pick",
                            color = PrimaryCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Save button
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.completeProfile(phoneNumber, dateOfBirth)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading && phoneNumber.isNotBlank() && isPhoneValid && dateOfBirth.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryCyan,
                        disabledContainerColor = PrimaryCyan.copy(alpha = 0.35f)
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = DarkBackground,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Save & Continue",
                            color = DarkBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Skip button
                TextButton(
                    onClick = onSkip,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Skip for now",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
