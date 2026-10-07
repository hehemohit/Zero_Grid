package com.example.zerogrid.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.zerogrid.navigation.ZeroGridBottomBar
import com.example.zerogrid.ui.theme.*

@Composable
fun SecurityPrivacyScreen(onNavigate: (Screen) -> Unit = {}) {
    var e2eEncryptionEnabled by remember { mutableStateOf(true) }
    var anonymousRoutingEnabled by remember { mutableStateOf(true) }
    var metadataObfuscation by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent, // Overdraw elimination: root Scaffold owns background
        topBar = { SecurityPrivacyTopBar(onBackClick = { onNavigate(Screen.SETTINGS) }) },
        bottomBar = { ZeroGridBottomBar(currentScreen = Screen.SETTINGS, onNavigate = onNavigate) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "ENCRYPTION & KEYS",
                color = TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            SecurityToggleCard(
                title = "End-to-End Encryption",
                subtitle = "Encrypt all 1-to-1 payload messages using Ed25519/X25519 keys",
                checked = e2eEncryptionEnabled,
                onCheckedChange = { e2eEncryptionEnabled = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Public Key Fingerprint",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "8F3A - 9C12 - B4E5 - 77D1 - 09AA - 33FE",
                        color = StatusActive,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { /* Export Key / QR */ },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusActive)
                    ) {
                        Icon(imageVector = Icons.Outlined.QrCode, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Export Key / Show QR", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "ANONYMITY & ROUTING",
                color = TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            SecurityToggleCard(
                title = "Anonymous Multi-Hop Routing",
                subtitle = "Strip immediate sender address during packet forwarding",
                checked = anonymousRoutingEnabled,
                onCheckedChange = { anonymousRoutingEnabled = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            SecurityToggleCard(
                title = "Metadata Obfuscation",
                subtitle = "Pad packet length to mask payload signatures",
                checked = metadataObfuscation,
                onCheckedChange = { metadataObfuscation = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { /* Reset Identity */ },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B1A1E), contentColor = AlertPink),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Outlined.DeleteForever, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Reset Identity & Re-generate Keys", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SecurityPrivacyTopBar(onBackClick: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Security & Privacy",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        HorizontalDivider(color = DividerColor, thickness = 1.dp)
    }
}

@Composable
private fun SecurityToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = subtitle, color = TextSecondary, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = StatusActive,
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = SurfaceDarker
                )
            )
        }
    }
}
