package com.example.zerogrid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zerogrid.ui.theme.BadgeGreen
import com.example.zerogrid.ui.theme.ZeroGridTheme

@Composable
fun ZeroGridTopBar(
    peerCount: Int = 0,
    isMeshActive: Boolean = true,
    onProfileClick: () -> Unit = {}
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
            // Brand Logo & Title (Using Mesh Network Hub Icon)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(colors.surfaceNested, RoundedCornerShape(10.dp))
                        .border(1.dp, colors.primary.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Hub,
                        contentDescription = "ZeroGrid Mesh Emblem",
                        tint = colors.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "ZeroGrid",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )
            }

            // Right Status Badge & Profile Icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Online / Standby Badge
                Surface(
                    color = if (isMeshActive) BadgeGreen.copy(alpha = 0.12f) else colors.surfaceNested,
                    shape = RoundedCornerShape(16.dp),
                    border = null
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    color = if (isMeshActive) BadgeGreen else colors.textSecondary,
                                    shape = CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (!isMeshActive) {
                                "Offline"
                            } else if (peerCount > 0) {
                                "Online • $peerCount Nearby"
                            } else {
                                "Online • Standby"
                            },
                            color = if (isMeshActive) BadgeGreen else colors.textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Profile Avatar Icon Button
                IconButton(
                    onClick = onProfileClick,
                    modifier = Modifier
                        .size(36.dp)
                        .background(colors.surfaceNested, CircleShape)
                        .border(1.dp, colors.divider, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = "Profile & Settings",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        HorizontalDivider(color = colors.divider, thickness = 1.dp)
    }
}
