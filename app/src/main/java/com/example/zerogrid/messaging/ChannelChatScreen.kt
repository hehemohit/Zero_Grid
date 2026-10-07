package com.example.zerogrid.messaging

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zerogrid.navigation.Screen
import com.example.zerogrid.ui.theme.*

@Composable
fun ChatDetailScreen(onNavigate: (Screen) -> Unit = {}) {
    var messageText by remember { mutableStateOf("") }
    val colors = ZeroGridTheme.colors // 1. Grab dynamic colors

    Scaffold(
        containerColor = Color.Transparent, // Overdraw elimination: root Scaffold owns background
        topBar = { ChatDetailTopBar(onBackClick = { onNavigate(Screen.MESSAGES) }) },
        bottomBar = { ChatBottomBar(messageText = messageText, onValueChange = { messageText = it }) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "msg_alex", contentType = "OtherMessage") {
                OtherMessageItem(
                    initial = "A",
                    name = "Alex",
                    time = "10:42 AM",
                    message = "Coordinates confirmed for Sector 4. Proceeding with caution.",
                    badgeText = "Direct",
                    badgeIcon = Icons.Outlined.Check,
                    badgeColor = colors.primary
                )
            }

            item(key = "msg_rt", contentType = "OtherMessage") {
                OtherMessageItem(
                    initial = "RT",
                    name = "Rescue Team",
                    time = "10:45 AM",
                    message = "Copy that. ETA 15 mikes. Ensure LZ is clear.",
                    badgeText = "2 hops   ↗ Routed through 2 peers",
                    badgeColor = colors.primary,
                    isCustomBadge = true
                )
            }

            item(key = "notif_joined", contentType = "Notification") {
                SystemNotificationBadge(text = "Device-7A42 joined #mesh")
            }

            item(key = "msg_router", contentType = "OtherMessage") {
                OtherMessageItem(
                    icon = Icons.Outlined.Router,
                    name = "Device-7A42",
                    time = "10:47 AM",
                    message = "[AUTOMATED] Signal strength optimal. Establishing relay link.",
                    badgeText = "Relay  •  1 hop",
                    badgeColor = colors.primary
                )
            }

            item(key = "msg_mine", contentType = "MyMessage") {
                MyMessageItem(
                    time = "10:50 AM",
                    message = "LZ is secure. Standing by for visual.",
                    statusText = "Delivered ✓"
                )
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ChatDetailTopBar(onBackClick: () -> Unit = {}) {
    val colors = ZeroGridTheme.colors
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(colors.surfaceNested, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Hub,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "#mesh",
                            color = colors.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "18 participants • Mesh Active",
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search",
                    tint = colors.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(20.dp))
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Info",
                    tint = colors.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        HorizontalDivider(color = colors.divider, thickness = 1.dp)
    }
}

@Composable
private fun OtherMessageItem(
    initial: String? = null,
    icon: ImageVector? = null,
    name: String,
    time: String,
    message: String,
    badgeText: String,
    badgeColor: Color,
    badgeIcon: ImageVector? = null,
    isCustomBadge: Boolean = false
) {
    val colors = ZeroGridTheme.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(colors.surfaceNested, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (icon != null) {
                        Icon(imageVector = icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                    } else if (initial != null) {
                        Text(text = initial, color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = name,
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                text = time,
                color = colors.textSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 42.dp),
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = message,
                    color = colors.textPrimary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .background(colors.surfaceNested, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (badgeIcon != null) {
                        Icon(imageVector = badgeIcon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    if (isCustomBadge) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(colors.primary, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = badgeText,
                        color = badgeColor,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun SystemNotificationBadge(text: String) {
    val colors = ZeroGridTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = colors.surfaceNested,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.wrapContentWidth()
        ) {
            Text(
                text = text,
                color = colors.textSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MyMessageItem(
    time: String,
    message: String,
    statusText: String
) {
    val colors = ZeroGridTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = time,
                color = colors.textSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "You",
                color = colors.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 42.dp),
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.End
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Keep distinct contrast for user messages, but tint it using primary
                        .background(colors.primary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = message,
                        color = colors.primary,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = statusText,
                    color = colors.primary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ChatBottomBar(messageText: String, onValueChange: (String) -> Unit) {
    val colors = ZeroGridTheme.colors
    Column {
        HorizontalDivider(color = colors.divider, thickness = 1.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.background)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { },
                    modifier = Modifier
                        .size(40.dp)
                        .background(colors.surfaceNested, CircleShape)
                ) {
                    Icon(imageVector = Icons.Outlined.Add, contentDescription = "Add Attachment", tint = colors.primary)
                }
                OutlinedTextField(
                    value = messageText,
                    onValueChange = onValueChange,
                    placeholder = { Text("Message #mesh...", color = colors.textSecondary, fontSize = 14.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colors.cardBackground,
                        unfocusedContainerColor = colors.cardBackground,
                        disabledContainerColor = colors.cardBackground,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    singleLine = true
                )
                Button(
                    onClick = { },
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.Black, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(imageVector = Icons.Outlined.Lock, contentDescription = null, tint = colors.primary, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Icon(imageVector = Icons.Outlined.Lock, contentDescription = null, tint = colors.primary, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "End-to-end encrypted",
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        DashboardBottomNavChatActive()
    }
}

@Composable
private fun DashboardBottomNavChatActive() {
    val colors = ZeroGridTheme.colors
    NavigationBar(
        containerColor = colors.background,
        contentColor = colors.textSecondary,
        tonalElevation = 0.dp
    ) {
        val items = listOf(
            Triple("Home", Icons.Outlined.Home, false),
            Triple("Messages", Icons.Outlined.ChatBubbleOutline, true),
            Triple("Mesh", Icons.Outlined.Share, false),
            Triple("Files", Icons.Outlined.Folder, false),
            Triple("Settings", Icons.Outlined.Settings, false)
        )
        items.forEach { (label, icon, selected) ->
            NavigationBarItem(
                selected = selected,
                onClick = { },
                icon = { Icon(imageVector = icon, contentDescription = label) },
                label = { Text(text = label, fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Black,
                    unselectedIconColor = colors.textSecondary,
                    selectedTextColor = colors.textSecondary,
                    unselectedTextColor = colors.textSecondary,
                    indicatorColor = colors.primary
                )
            )
        }
    }
}

@Composable
fun ZeroGridChatDetailScreen() = ChatDetailScreen()

@Preview(showBackground = true)
@Composable
fun ZeroGridChatDetailPreview() {
    ZeroGridTheme {
        ZeroGridChatDetailScreen()
    }
}