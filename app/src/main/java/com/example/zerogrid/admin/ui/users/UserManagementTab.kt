package com.example.zerogrid.admin.ui.users

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.zerogrid.admin.data.model.AdminUserDto
import com.example.zerogrid.ui.theme.*

@Composable
fun UserManagementTab(
    users: List<AdminUserDto>,
    isLoading: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onPromoteAdmin: (String) -> Unit,
    onRevokeAdmin: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ZeroGridTheme.colors
    var userToPromote by remember { mutableStateOf<AdminUserDto?>(null) }
    var userToRevoke by remember { mutableStateOf<AdminUserDto?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search by name or email...", color = colors.textSecondary, fontSize = 13.sp) },
            leadingIcon = {
                Icon(Icons.Outlined.Search, null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Outlined.Close, null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.cardBackground,
                unfocusedContainerColor = colors.cardBackground,
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.divider,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colors.primary)
            }
        } else if (users.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Group,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No users found",
                        color = colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(users, key = { it.userId }) { user ->
                    UserCard(
                        user = user,
                        onRequestPromote = { userToPromote = user },
                        onRequestRevoke = { userToRevoke = user }
                    )
                }
            }
        }
    }

    // Promote Confirmation Dialog
    userToPromote?.let { user ->
        AlertDialog(
            onDismissRequest = { userToPromote = null },
            title = { Text("Promote to Admin?", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Grant Authority Admin privileges to ${user.nameDisplay} (${user.email})? They will have full access to emergency dispatches and system administration.",
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        user.email?.let { onPromoteAdmin(it) }
                        userToPromote = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text("Grant Admin", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToPromote = null }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.cardBackground
        )
    }

    // Revoke Confirmation Dialog
    userToRevoke?.let { user ->
        AlertDialog(
            onDismissRequest = { userToRevoke = null },
            title = { Text("Revoke Admin Status?", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Are you sure you want to demote ${user.nameDisplay} from Admin role? They will lose access to the authority command center.",
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRevokeAdmin(user.userId)
                        userToRevoke = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accentRed)
                ) {
                    Text("Revoke Admin", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToRevoke = null }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.cardBackground
        )
    }
}

@Composable
private fun UserCard(
    user: AdminUserDto,
    onRequestPromote: () -> Unit,
    onRequestRevoke: () -> Unit
) {
    val colors = ZeroGridTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.divider)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar initials
                val avatarColor = if (user.isAdmin) Color(0xFFFF9500) else colors.primary
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(avatarColor.copy(alpha = 0.15f), CircleShape)
                        .border(1.dp, avatarColor.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.nameDisplay.take(2).uppercase(),
                        color = avatarColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user.nameDisplay,
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // Role Badge
                        Box(
                            modifier = Modifier
                                .background(avatarColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .border(1.dp, avatarColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = user.role.uppercase(),
                                color = avatarColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    user.email?.let {
                        Text(it, color = colors.textSecondary, fontSize = 11.sp)
                    }

                    Text(
                        text = "${user.nodeAddress} • ${user.statusDisplay}",
                        color = colors.textSecondary.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Action Button (Promote or Revoke)
            if (user.isAdmin) {
                OutlinedButton(
                    onClick = onRequestRevoke,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.accentRed),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.accentRed.copy(alpha = 0.4f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Demote", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onRequestPromote,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Make Admin", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
