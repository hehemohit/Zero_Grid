package com.example.zerogrid.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zerogrid.network.AuthRepository
import com.example.zerogrid.ui.theme.*
import com.zerogrid.mesh.app.ui.UserRole
import com.zerogrid.mesh.app.ui.UserSessionManager

@Composable
fun LoginScreen(
    sessionManager: UserSessionManager,
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: (role: UserRole, profileComplete: Boolean) -> Unit
) {
    val context = LocalContext.current
    val viewModel = remember {
        AuthViewModel(AuthRepository(sessionManager))
    }
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showPendingDialog by remember { mutableStateOf(false) }
    var pendingMessage by remember { mutableStateOf("") }

    // React to state changes
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AuthUiState.Success -> {
                onLoginSuccess(state.role, state.profileComplete)
                viewModel.resetState()
            }
            is AuthUiState.Error -> {
                snackbarHostState.showSnackbar(state.message, duration = SnackbarDuration.Short)
                viewModel.resetState()
            }
            is AuthUiState.AdminPending -> {
                pendingMessage = state.message
                showPendingDialog = true
                viewModel.resetState()
            }
            else -> {}
        }
    }

    // Admin pending approval dialog
    if (showPendingDialog) {
        AlertDialog(
            onDismissRequest = { showPendingDialog = false },
            containerColor = CardBackground,
            title = {
                Text(
                    "Account Pending Approval",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(pendingMessage, color = TextSecondary, fontSize = 14.sp, lineHeight = 20.sp)
            },
            confirmButton = {
                TextButton(onClick = { showPendingDialog = false }) {
                    Text("OK", color = PrimaryCyan, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Scaffold(
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(72.dp))

                // ── Logo / Header ──────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(CardBackground, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ZG",
                        color = PrimaryCyan,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "ZeroGrid",
                    color = TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Resilient Mesh Network",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(48.dp))

                Text(
                    text = "Sign In",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Welcome back to the grid.",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 24.dp)
                )

                // ── Email Field ────────────────────────────────────────────
                AuthTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email",
                    leadingIcon = Icons.Outlined.Email,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // ── Password Field ─────────────────────────────────────────
                AuthTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    leadingIcon = Icons.Outlined.Lock,
                    visualTransformation = if (passwordVisible) VisualTransformation.None
                                           else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff
                                              else Icons.Outlined.Visibility,
                                contentDescription = "Toggle password",
                                tint = TextSecondary
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            viewModel.login(email, password)
                        }
                    )
                )

                Spacer(modifier = Modifier.height(28.dp))

                // ── Login Button ───────────────────────────────────────────
                Button(
                    onClick = { viewModel.login(email, password) },
                    enabled = uiState !is AuthUiState.Loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryCyan,
                        disabledContainerColor = PrimaryCyan.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (uiState is AuthUiState.Loading) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Text(
                            "Sign In",
                            color = Color.Black,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Register link ──────────────────────────────────────────
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Don't have an account? ", color = TextSecondary, fontSize = 14.sp)
                    TextButton(
                        onClick = onNavigateToRegister,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(20.dp)
                    ) {
                        Text(
                            "Register",
                            color = PrimaryCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}
