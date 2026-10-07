package com.example.zerogrid.admin.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zerogrid.admin.data.AdminSocketState
import com.example.zerogrid.ui.theme.*

@Composable
fun AdminTopBar(
    activeAlertsCount: Int,
    socketState: AdminSocketState,
    onOpenMeshApp: () -> Unit,
    onLogout: () -> Unit
) {
    val colors = ZeroGridTheme.colors

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
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "COMMAND CENTER",
                        color = colors.primary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Live socket badge
                    val (statusColor, statusLabel) = when (socketState) {
                        AdminSocketState.CONNECTED -> Color(0xFF30D158) to "LIVE"
                        AdminSocketState.CONNECTING -> Color(0xFFFF9500) to "SYNCING"
                        AdminSocketState.DISCONNECTED -> Color(0xFF8E8E93) to "OFFLINE"
                    }

                    Row(
                        modifier = Modifier
                            .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(statusColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = statusLabel,
                            color = statusColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Authority Dispatch Node",
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    )
                    if (activeAlertsCount > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(colors.accentRed.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .border(1.dp, colors.accentRed.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "$activeAlertsCount ACTIVE",
                                color = colors.accentRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Switch to Mesh App button
                IconButton(
                    onClick = onOpenMeshApp,
                    modifier = Modifier
                        .size(38.dp)
                        .background(colors.surfaceNested, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Router,
                        contentDescription = "Mesh App",
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Sign out button
                IconButton(
                    onClick = onLogout,
                    modifier = Modifier
                        .size(38.dp)
                        .background(colors.surfaceNested, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ExitToApp,
                        contentDescription = "Sign Out",
                        tint = colors.accentRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        HorizontalDivider(color = colors.divider, thickness = 1.dp)
    }
}
