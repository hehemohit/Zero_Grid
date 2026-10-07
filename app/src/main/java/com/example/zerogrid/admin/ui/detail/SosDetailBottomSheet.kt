package com.example.zerogrid.admin.ui.detail

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zerogrid.admin.data.model.AdminSosEventDto
import com.example.zerogrid.admin.util.AdminFormatters
import com.example.zerogrid.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SosDetailBottomSheet(
    incident: AdminSosEventDto,
    onDismiss: () -> Unit,
    onAcknowledge: () -> Unit,
    onResolve: () -> Unit,
    onAddNote: (String) -> Unit,
    isActionLoading: Boolean = false
) {
    val context = LocalContext.current
    val colors = ZeroGridTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var noteInput by remember { mutableStateOf("") }
    var showResolveDialog by remember { mutableStateOf(false) }

    val categoryColor = AdminFormatters.getCategoryColor(incident.category)
    val statusColor = AdminFormatters.getStatusColor(incident.status)
    val batteryColor = AdminFormatters.getBatteryColor(incident.batteryPercentage)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.cardBackground,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = colors.divider)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header: Category, ID, Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(categoryColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .border(1.dp, categoryColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = incident.category.uppercase(),
                            color = categoryColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = incident.displayId,
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(6.dp).background(statusColor, CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = incident.status,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable Content
            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Reporter Profile Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = colors.surfaceNested),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(colors.primary.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Outlined.Person, null, tint = colors.primary, modifier = Modifier.size(22.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(incident.userName, color = colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                        incident.userEmail?.let {
                                            Text(it, color = colors.textSecondary, fontSize = 12.sp)
                                        }
                                        incident.userPhone?.let {
                                            Text(it, color = colors.primary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                }

                                // Battery Chip
                                Row(
                                    modifier = Modifier
                                        .background(colors.cardBackground, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.BatteryStd, null, tint = batteryColor, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = incident.formattedBattery,
                                        color = batteryColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            // Quick Contact Actions
                            if (!incident.userPhone.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${incident.userPhone}"))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.primary.copy(alpha = 0.5f))
                                ) {
                                    Icon(Icons.Outlined.Phone, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Call Reporter (${incident.userPhone})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                // Emergency Message
                if (!incident.message.isNullOrBlank()) {
                    item {
                        Column {
                            Text(
                                text = "EMERGENCY MESSAGE",
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = colors.surfaceNested),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = incident.message,
                                    color = colors.textPrimary,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }

                // Telemetry & GPS Section
                item {
                    Column {
                        Text(
                            text = "SECTOR TELEMETRY",
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = colors.surfaceNested),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (incident.location?.hasValidCoordinates == true) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "${incident.location.latitude}, ${incident.location.longitude}",
                                                color = colors.textPrimary,
                                                fontSize = 13.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            )
                                            incident.accuracyMeters?.let {
                                                Text(
                                                    text = "Accuracy: ±${it.toInt()} meters",
                                                    color = colors.textSecondary,
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                        // Open in maps intent
                                        IconButton(
                                            onClick = {
                                                val uri = Uri.parse("geo:0,0?q=${incident.location.latitude},${incident.location.longitude}(Emergency-${incident.displayId})")
                                                val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                                                context.startActivity(mapIntent)
                                            },
                                            modifier = Modifier
                                                .background(colors.cardBackground, RoundedCornerShape(8.dp))
                                                .size(36.dp)
                                        ) {
                                            Icon(Icons.Outlined.Navigation, null, tint = colors.primary, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "Grid Sector Telemetry (Mesh broadcast without fine GPS)",
                                        color = colors.textSecondary,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }

                // Dispatch Notes Section
                item {
                    Text(
                        text = "CASE DISPATCH TIMELINE (${incident.notes.size})",
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (incident.notes.isEmpty()) {
                    item {
                        Text(
                            text = "No dispatcher notes logged yet.",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                } else {
                    items(incident.notes, key = { it.noteId.ifEmpty { it.text.hashCode().toString() } }) { note ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = colors.surfaceNested),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = note.authorName,
                                        color = colors.primary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = AdminFormatters.formatRelativeTime(note.effectiveTime),
                                        color = colors.textSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = note.text,
                                    color = colors.textPrimary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // Add Note Input
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = noteInput,
                            onValueChange = { noteInput = it },
                            placeholder = { Text("Log case dispatch note...", color = colors.textSecondary, fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = colors.surfaceNested,
                                unfocusedContainerColor = colors.surfaceNested,
                                focusedBorderColor = colors.primary,
                                unfocusedBorderColor = colors.divider,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (noteInput.isNotBlank()) {
                                    onAddNote(noteInput.trim())
                                    noteInput = ""
                                }
                            },
                            enabled = noteInput.isNotBlank() && !isActionLoading,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                            modifier = Modifier.height(46.dp)
                        ) {
                            Text("Post", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = colors.divider)
            Spacer(modifier = Modifier.height(12.dp))

            // Primary Bottom Actions (Acknowledge / Resolve)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (incident.isActive) {
                    Button(
                        onClick = onAcknowledge,
                        enabled = !isActionLoading,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9500)),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Outlined.CheckCircle, null, modifier = Modifier.size(18.dp), tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Acknowledge", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                if (!incident.isResolved) {
                    Button(
                        onClick = { showResolveDialog = true },
                        enabled = !isActionLoading,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30D158)),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Outlined.TaskAlt, null, modifier = Modifier.size(18.dp), tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Resolve SOS", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Incident Closed — Done")
                    }
                }
            }
        }
    }

    // Resolve Confirmation Dialog
    if (showResolveDialog) {
        AlertDialog(
            onDismissRequest = { showResolveDialog = false },
            title = { Text("Resolve Emergency SOS?", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "This will mark ${incident.displayId} as fully RESOLVED. The incident will be archived to History.",
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResolveDialog = false
                        onResolve()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30D158))
                ) {
                    Text("Confirm Resolution", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResolveDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.cardBackground
        )
    }
}
