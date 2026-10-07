package com.example.zerogrid.admin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zerogrid.ui.theme.*

@Composable
fun IncidentFilterHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedFilter: String, // ALL, ACTIVE, ACKNOWLEDGED
    onFilterSelected: (String) -> Unit,
    isRadarView: Boolean,
    onToggleRadarView: () -> Unit
) {
    val colors = ZeroGridTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Search bar + radar toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        text = "Search victim, sector, ID...",
                        color = colors.textSecondary,
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Clear",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
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
                    .defaultMinSize(minHeight = 48.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Radar Map toggle button
            IconButton(
                onClick = onToggleRadarView,
                modifier = Modifier
                    .size(50.dp)
                    .background(
                        if (isRadarView) colors.primary.copy(alpha = 0.15f) else colors.cardBackground,
                        RoundedCornerShape(10.dp)
                    )
                    .border(
                        1.dp,
                        if (isRadarView) colors.primary else colors.divider,
                        RoundedCornerShape(10.dp)
                    )
            ) {
                Icon(
                    imageVector = if (isRadarView) Icons.Outlined.ViewList else Icons.Outlined.Map,
                    contentDescription = "Toggle Radar Map",
                    tint = if (isRadarView) colors.primary else colors.textSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Status filter chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("ALL", "ACTIVE", "ACKNOWLEDGED").forEach { status ->
                val isSelected = status == selectedFilter
                val chipColor = when (status) {
                    "ACTIVE" -> colors.accentRed
                    "ACKNOWLEDGED" -> Color(0xFFFF9500)
                    else -> colors.primary
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onFilterSelected(status) }
                        .background(
                            if (isSelected) chipColor.copy(alpha = 0.15f) else colors.surfaceNested,
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            1.dp,
                            if (isSelected) chipColor else colors.divider,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = status,
                        color = if (isSelected) chipColor else colors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
