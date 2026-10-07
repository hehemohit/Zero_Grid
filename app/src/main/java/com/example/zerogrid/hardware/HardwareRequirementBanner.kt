package com.example.zerogrid.hardware

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zerogrid.ui.theme.*

import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.runtime.collectAsState
import com.example.zerogrid.mesh.engine.MeshChannelMode
import com.example.zerogrid.mesh.engine.MeshEngine

private val WarningAmber = Color(0xFFFF9500)
private val WarningAmberBg = Color(0x1AFF9500)
private val WarningAmberBorder = Color(0x66FF9500)

private val ErrorRedBg = Color(0x1AFF3B30)
private val ErrorRedBorder = Color(0x66FF3B30)

@Composable
fun HardwareRequirementBanner(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val meshEngine = remember { MeshEngine.getInstance(context) }
    val activeMode by meshEngine.activeChannelMode.collectAsState()
    val hardwareState by rememberHardwareState()

    val needsBluetooth = activeMode == MeshChannelMode.BLE && !hardwareState.isBluetoothEnabled
    val needsWifi = activeMode == MeshChannelMode.WIFI_DIRECT && !hardwareState.isWifiEnabled
    val needsLocation = !hardwareState.isLocationEnabled

    AnimatedVisibility(
        visible = needsBluetooth || needsWifi || needsLocation,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (needsBluetooth) {
                HardwareWarningCard(
                    icon = Icons.Default.BluetoothDisabled,
                    title = "BLUETOOTH IS DISABLED",
                    message = "Bluetooth is required for ZeroGrid BLE mesh discovery and packet exchange.",
                    buttonText = "Turn On",
                    isCritical = true,
                    onClick = { HardwareStateManager.openBluetoothSettings(context) }
                )
            }

            if (needsWifi) {
                HardwareWarningCard(
                    icon = Icons.Default.WifiOff,
                    title = "WI-FI IS DISABLED",
                    message = "Wi-Fi is required for ZeroGrid Wi-Fi Direct mesh communication.",
                    buttonText = "Turn On",
                    isCritical = true,
                    onClick = { HardwareStateManager.openWifiSettings(context) }
                )
            }

            if (needsLocation) {
                HardwareWarningCard(
                    icon = Icons.Default.LocationOff,
                    title = "LOCATION SERVICES DISABLED",
                    message = "Android requires device Location to be ON for wireless mesh peer discovery.",
                    buttonText = "Enable",
                    isCritical = false,
                    onClick = { HardwareStateManager.openLocationSettings(context) }
                )
            }
        }
    }
}

@Composable
fun WifiRequiredDialog(
    onDismiss: () -> Unit,
    onEnableClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        title = {
            Text(
                text = "Wi-Fi Is Turned Off",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Text(
                text = "Wi-Fi Direct requires device Wi-Fi to be active to establish peer connections and transmit packets.",
                color = TextSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onEnableClick()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan)
            ) {
                Text("Turn On Wi-Fi", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun HardwareWarningCard(
    icon: ImageVector,
    title: String,
    message: String,
    buttonText: String,
    isCritical: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isCritical) ErrorRedBg else WarningAmberBg
    val borderColor = if (isCritical) ErrorRedBorder else WarningAmberBorder
    val accentColor = if (isCritical) Color(0xFFFF5252) else WarningAmber

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = accentColor,
            modifier = Modifier.size(24.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = accentColor,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = message,
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }

        Button(
            onClick = onClick,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.height(34.dp)
        ) {
            Text(
                text = buttonText,
                color = Color.Black,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
