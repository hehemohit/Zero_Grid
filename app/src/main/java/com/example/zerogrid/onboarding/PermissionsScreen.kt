package com.example.zerogrid.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import com.example.zerogrid.navigation.*
import com.example.zerogrid.ui.theme.*
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SettingsBluetooth // Use a different Bluetooth icon for variety
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zerogrid.ui.theme.*

@Composable
fun PermissionsScreen(onNavigate: (Screen) -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Top Logo and Heading
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Share, // Placeholder for the mesh network logo
                contentDescription = "Logo",
                tint = PrimaryCyan,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "ZeroGrid",
                color = PrimaryCyan,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Enable ZeroGrid Connectivity",
            color = PrimaryCyan,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 34.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "To build a resilient local mesh network, ZeroGrid requires access to your device's connectivity hardware.",
            color = TextSecondary,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Custom Stepper Component
        ZeroGridStepper(currentStep = 2, totalSteps = 3)

        Spacer(modifier = Modifier.height(32.dp))

        // Permissions List
        PermissionItemCard(
            icon = Icons.Filled.SettingsBluetooth,
            title = "Nearby Devices",
            status = "Not granted",
            isGranted = false,
            description = "Core requirement for detecting and connecting to other mesh nodes via Bluetooth and local protocols.",
            label = "REQUIRED",
            onAllowClick = { /* Handle Nearby Devices allow */ }
        )

        Spacer(modifier = Modifier.height(12.dp))

        PermissionItemCard(
            icon = Icons.Filled.Wifi,
            title = "Wi-Fi & Local Network",
            status = "Not granted",
            isGranted = false,
            description = "Enables high-bandwidth peer-to-peer data transfer independent of external internet access.",
            label = "REQUIRED",
            onAllowClick = { /* Handle Wi-Fi allow */ }
        )

        Spacer(modifier = Modifier.height(12.dp))

        PermissionItemCard(
            icon = Icons.Filled.LocationOn,
            title = "Location",
            status = "Not granted",
            isGranted = false,
            description = "Android requires location access to scan for nearby Bluetooth and Wi-Fi networks.",
            label = "REQUIRED",
            onAllowClick = { /* Handle Location allow */ }
        )

        Spacer(modifier = Modifier.height(12.dp))

        PermissionItemCard(
            icon = Icons.Filled.Notifications,
            title = "Notifications",
            status = "Not granted",
            isGranted = false,
            description = "Receive alerts for critical mesh updates, SOS signals, and network status changes.",
            label = "RECOMMENDED",
            onAllowClick = { /* Handle Notifications allow */ }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Info Box Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = "Info",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Why does ZeroGrid need these?",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ZeroGrid operates completely offline. These hardware permissions are necessary to form the encrypted, decentralized local mesh network. Your data never leaves the local grid.",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Bottom Actions - Continue Button (Disabled look in screenshot)
        Button(
            onClick = { onNavigate(Screen.CREATE_IDENTITY) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryCyan,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(12.dp),
            enabled = true
        ) {
            Text(
                text = "Continue",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { /* Handle Review permissions later */ }) {
            Text(
                text = "Review permissions later",
                color = PrimaryCyan,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * Reusable component for each permission item.
 */
@Composable
fun PermissionItemCard(
    icon: ImageVector,
    title: String,
    status: String,
    isGranted: Boolean,
    description: String,
    label: String,
    onAllowClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = PrimaryCyan,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    Column {
                        Text(
                            text = title,
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = status,
                            color = if (isGranted) GreenGranted else RedNotGranted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = label,
                    color = TextPrimary,
                    modifier = Modifier
                        .background(GreyLabel, RoundedCornerShape(4.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = description,
                color = TextSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                FilledTonalButton(
                    onClick = onAllowClick,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Allow",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Custom line-based stepper component.
 */
@Composable
fun ZeroGridStepper(currentStep: Int, totalSteps: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(modifier = Modifier.width(180.dp).height(10.dp)) {
            val totalLineWidth = size.width
            val totalLineHeight = size.height
            val segmentWidth = totalLineWidth / totalSteps
            val lineThickness = 3.dp.toPx()

            for (i in 0 until totalSteps) {
                val startX = segmentWidth * i
                val isActive = i < currentStep
                drawLine(
                    color = if (isActive) PrimaryCyan else LineColor,
                    start = Offset(startX, totalLineHeight / 2),
                    end = Offset(startX + segmentWidth, totalLineHeight / 2),
                    strokeWidth = lineThickness
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "STEP $currentStep OF $totalSteps",
            color = TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ZeroGridPermissionsScreen() = PermissionsScreen()

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun ZeroGridPermissionsScreenPreview() {
    ZeroGridTheme {
        ZeroGridPermissionsScreen()
    }
}