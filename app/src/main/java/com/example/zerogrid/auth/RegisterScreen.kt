package com.example.zerogrid.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zerogrid.network.AuthRepository
import com.example.zerogrid.ui.theme.*
import com.zerogrid.mesh.app.ui.UserRole
import com.zerogrid.mesh.app.ui.UserSessionManager

private val AdminAmber = Color(0xFFFF9500)

@Composable
fun RegisterScreen(
    sessionManager: UserSessionManager,
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: (role: UserRole, profileComplete: Boolean) -> Unit
) {
    val viewModel = remember { AuthViewModel(AuthRepository(sessionManager)) }
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf(UserRole.CITIZEN) }
    var showPendingDialog by remember { mutableStateOf(false) }
    var pendingMessage by remember { mutableStateOf("") }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AuthUiState.Success -> {
                onRegisterSuccess(state.role, state.profileComplete)
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

    if (showPendingDialog) {
        AlertDialog(
            onDismissRequest = { showPendingDialog = false },
            containerColor = CardBackground,
            title = {
                Text("Account Pending Approval", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // ── Header ─────────────────────────────────────────────────────
            Text(
                text = "Create Account",
                color = TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Join the ZeroGrid network.",
                color = TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 28.dp)
            )

            // ── Display Name ───────────────────────────────────────────────
            AuthTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = "Display Name",
                leadingIcon = Icons.Outlined.Person,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )

            Spacer(modifier = Modifier.height(14.dp))

            // ── Email ──────────────────────────────────────────────────────
            AuthTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email",
                leadingIcon = Icons.Outlined.Email,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )

            Spacer(modifier = Modifier.height(14.dp))

            // ── Password ───────────────────────────────────────────────────
            AuthTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password (min. 8 characters)",
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
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )

            Spacer(modifier = Modifier.height(22.dp))

            // ── Role Toggle ────────────────────────────────────────────────
            Text(
                "Select Your Role",
                color = TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RoleCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Person,
                    title = "Citizen",
                    subtitle = "Standard user\nand mesh node",
                    isSelected = selectedRole == UserRole.CITIZEN,
                    accentColor = PrimaryCyan,
                    onClick = { selectedRole = UserRole.CITIZEN }
                )
                RoleCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Shield,
                    title = "Admin",
                    subtitle = "Requires manual\napproval to login",
                    isSelected = selectedRole == UserRole.ADMIN,
                    accentColor = AdminAmber,
                    onClick = { selectedRole = UserRole.ADMIN }
                )
            }

            // Admin info chip
            if (selectedRole == UserRole.ADMIN) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AdminAmber.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = AdminAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Admin accounts require manual approval before you can sign in.",
                        color = AdminAmber,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Register Button ────────────────────────────────────────────
            Button(
                onClick = { viewModel.register(email, password, displayName, selectedRole) },
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
                        "Create Account",
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Login link ─────────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                Text("Already have an account? ", color = TextSecondary, fontSize = 14.sp)
                TextButton(
                    onClick = onNavigateToLogin,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(20.dp)
                ) {
                    Text("Sign In", color = PrimaryCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun RoleCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) accentColor else DividerColor
    val bgColor = if (isSelected) accentColor.copy(alpha = 0.08f) else CardBackground

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) accentColor else TextSecondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                title,
                color = if (isSelected) accentColor else TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, color = TextSecondary, fontSize = 11.sp, lineHeight = 14.sp)
        }
    }
}
