package com.example.zerogrid.onboarding

import com.example.zerogrid.navigation.*

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zerogrid.ui.theme.*

@Composable
fun CreateIdentityScreen(onNavigate: (Screen) -> Unit = {}) {
    var displayName by remember { mutableStateOf("") }
    var deviceName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Placeholder for ZeroGrid Logo
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color.DarkGray, RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ZeroGrid",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "Step 3 of 3",
                color = TextSecondary,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Segmented Progress Bar (All 3 complete)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1f).height(4.dp).background(PrimaryCyan, RoundedCornerShape(2.dp)))
            Box(modifier = Modifier.weight(1f).height(4.dp).background(PrimaryCyan, RoundedCornerShape(2.dp)))
            Box(modifier = Modifier.weight(1f).height(4.dp).background(PrimaryCyan, RoundedCornerShape(2.dp)))
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Titles
        Text(
            text = "Create Your Mesh Identity",
            color = TextPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "This identity is used by nearby ZeroGrid devices to recognize you. No account, phone number, email, or internet connection is required.",
            color = TextSecondary,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Identity Form Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar with Glow Effect
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(PrimaryCyanGlow, CircleShape)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DarkBackground, CircleShape)
                            .border(1.dp, Color(0xFF2C3242), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Avatar",
                            tint = TextPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Display Name Field
                IdentityTextField(
                    label = "Display Name",
                    placeholder = "e.g. Alex",
                    value = displayName,
                    onValueChange = { displayName = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Device Name Field
                IdentityTextField(
                    label = "Device Name",
                    placeholder = "e.g. Alex's Phone",
                    value = deviceName,
                    onValueChange = { deviceName = it }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Your identity stays on this device.",
                    color = PrimaryCyan,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Secure Device Identity Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Security",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Secure Device Identity",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "ZeroGrid will generate cryptographic keys for encrypted communication with other devices.",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Status Indicator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkIndicator, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(TextPrimary, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Identity Key • Generating locally",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(24.dp))

        // Bottom Action Button
        Button(
            onClick = { onNavigate(Screen.HOME) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
            shape = RoundedCornerShape(28.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Create Identity",
                    color = Color.Black,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Forward",
                    tint = Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { /* Handle Advanced Settings */ }) {
            Text(
                text = "Advanced Security Settings",
                color = TextPrimary,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun IdentityTextField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
        label = {
            Text(
                text = label,
                color = TextPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
        },
        placeholder = {
            Text(
                text = placeholder,
                color = TextSecondary,
                fontSize = 16.sp
            )
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = InputFieldBackground,
            unfocusedContainerColor = InputFieldBackground,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = PrimaryCyan,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        singleLine = true
    )
}