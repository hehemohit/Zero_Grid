package com.example.zerogrid.onboarding

import com.example.zerogrid.navigation.*

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zerogrid.ui.theme.*

@Composable
fun OnBoardingScreen(onNavigate: (Screen) -> Unit = {}) {
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
                Icon(
                    imageVector = Icons.Outlined.Share, // Placeholder for ZeroGrid logo
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
            Text(
                text = "1 • 3",
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Titles
        Text(
            text = "Stay Connected When Networks Fail",
            color = TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 34.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "ZeroGrid creates a local device-to-device mesh so you can communicate even when cellular networks and the internet are unavailable.",
            color = TextSecondary,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Hero Graphic Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(16.dp)
        ) {
            MeshGraphicCanvas()
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Features List
        FeatureItem(
            icon = Icons.Outlined.CloudOff,
            title = "No Internet Required",
            description = "Communicate locally without internet access."
        )

        Spacer(modifier = Modifier.height(12.dp))

        FeatureItem(
            icon = Icons.Outlined.Share,
            title = "Multi-Hop Mesh",
            description = "Messages can travel through nearby devices."
        )

        Spacer(modifier = Modifier.height(12.dp))

        FeatureItem(
            icon = Icons.Outlined.Lock,
            title = "End-to-End Encrypted",
            description = "Your messages remain protected across the mesh."
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Bottom Actions
        Button(
            onClick = { onNavigate(Screen.PERMISSIONS) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Continue",
                color = Color.Black,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { /* Handle Learn More */ }) {
            Text(
                text = "Learn how ZeroGrid works",
                color = PrimaryCyan,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun FeatureItem(icon: ImageVector, title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
fun MeshGraphicCanvas() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

        // Define node positions
        val node1 = Offset(size.width * 0.2f, size.height * 0.5f)
        val node2 = Offset(size.width * 0.4f, size.height * 0.3f)
        val node3 = Offset(size.width * 0.6f, size.height * 0.6f)
        val node4 = Offset(size.width * 0.8f, size.height * 0.4f)
        val node5 = Offset(size.width * 0.5f, size.height * 0.8f)
        val node6 = Offset(size.width * 0.7f, size.height * 0.85f)

        // Draw connections
        val lines = listOf(
            node1 to node2, node2 to node3, node3 to node4,
            node1 to node5, node5 to node3, node5 to node6
        )

        for ((start, end) in lines) {
            drawLine(
                color = MeshLineColor,
                start = start,
                end = end,
                strokeWidth = 3f,
                pathEffect = dashEffect
            )
        }

        // Draw node devices
        val nodes = listOf(node1, node2, node3, node4, node5, node6)
        for (node in nodes) {
            val rectSize = Size(40f, 70f)
            drawRoundRect(
                color = PrimaryCyan,
                topLeft = Offset(node.x - (rectSize.width / 2), node.y - (rectSize.height / 2)),
                size = rectSize,
                cornerRadius = CornerRadius(8f, 8f),
                style = Stroke(width = 3f)
            )
            drawCircle(
                color = PrimaryCyan,
                radius = 4f,
                center = node
            )
        }
    }
}