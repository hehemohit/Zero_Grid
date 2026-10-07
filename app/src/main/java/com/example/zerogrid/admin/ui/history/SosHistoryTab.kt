package com.example.zerogrid.admin.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zerogrid.admin.data.model.AdminSosEventDto
import com.example.zerogrid.admin.ui.components.IncidentCard
import com.example.zerogrid.ui.theme.*

@Composable
fun SosHistoryTab(
    historyEvents: List<AdminSosEventDto>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onInspectDetails: (AdminSosEventDto) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ZeroGridTheme.colors
    var selectedCategory by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }

    val categories = listOf("ALL", "MEDICAL", "DISASTER", "TRAPPED", "FIRE", "SECURITY", "OTHER")

    val filteredEvents = remember(historyEvents, selectedCategory, searchQuery) {
        historyEvents.filter { event ->
            val matchCat = selectedCategory == "ALL" || event.category.equals(selectedCategory, ignoreCase = true)
            val matchQuery = searchQuery.isBlank() ||
                event.userName.contains(searchQuery, ignoreCase = true) ||
                event.displayId.contains(searchQuery, ignoreCase = true) ||
                (event.message?.contains(searchQuery, ignoreCase = true) == true)
            matchCat && matchQuery
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Search bar + refresh
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search resolved archive...", color = colors.textSecondary, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
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
                    .weight(1f)
                    .height(48.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .size(48.dp)
                    .background(colors.cardBackground, RoundedCornerShape(10.dp))
                    .border(1.dp, colors.divider, RoundedCornerShape(10.dp))
            ) {
                Icon(Icons.Outlined.Refresh, "Refresh", tint = colors.primary, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Category Filter Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { cat ->
                val isSelected = cat == selectedCategory
                Box(
                    modifier = Modifier
                        .clickable { selectedCategory = cat }
                        .background(
                            if (isSelected) colors.primary.copy(alpha = 0.15f) else colors.cardBackground,
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            1.dp,
                            if (isSelected) colors.primary else colors.divider,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = cat,
                        color = if (isSelected) colors.primary else colors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

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
        } else if (filteredEvents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No resolved incidents found",
                        color = colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Resolved emergency dispatches will appear here.",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredEvents, key = { it.eventId }) { event ->
                    IncidentCard(
                        incident = event,
                        onInspectDetails = { onInspectDetails(event) },
                        onAcknowledge = {}
                    )
                }
            }
        }
    }
}
