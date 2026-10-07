package com.example.zerogrid.admin.util

import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.*

object AdminFormatters {

    val StatusActiveRed = Color(0xFFFF3B30)
    val StatusAckAmber = Color(0xFFFF9500)
    val StatusResolvedGreen = Color(0xFF30D158)
    val DarkSurfaceNested = Color(0xFF161F1E)
    val TealPrimary = Color(0xFF00E5FF)

    fun getCategoryColor(category: String): Color {
        return when (category.uppercase()) {
            "MEDICAL" -> Color(0xFFFF3B30)
            "TRAPPED" -> Color(0xFFFF9500)
            "FIRE" -> Color(0xFFFF453A)
            "SECURITY" -> Color(0xFFFF9F0A)
            "DISASTER" -> Color(0xFFFF375F)
            else -> Color(0xFF64D2FF)
        }
    }

    fun getStatusColor(status: String): Color {
        return when (status.uppercase()) {
            "ACTIVE" -> StatusActiveRed
            "ACKNOWLEDGED" -> StatusAckAmber
            "RESOLVED" -> StatusResolvedGreen
            else -> Color.Gray
        }
    }

    fun getBatteryColor(battery: Int?): Color {
        if (battery == null || battery !in 0..100) return Color(0xFF8E8E93)
        return when {
            battery > 50 -> Color(0xFF30D158)
            battery >= 20 -> Color(0xFFFF9500)
            else -> Color(0xFFFF3B30)
        }
    }

    fun formatRelativeTime(isoString: String?): String {
        if (isoString.isNullOrBlank()) return "Just now"
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = format.parse(isoString) ?: return "Recently"
            val diffMs = System.currentTimeMillis() - date.time
            val diffSec = diffMs / 1000
            val diffMin = diffSec / 60
            val diffHours = diffMin / 60
            val diffDays = diffHours / 24

            when {
                diffSec < 60 -> "Just now"
                diffMin < 60 -> "${diffMin}m ago"
                diffHours < 24 -> "${diffHours}h ago"
                diffDays < 7 -> "${diffDays}d ago"
                else -> {
                    val displayFmt = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                    displayFmt.format(date)
                }
            }
        } catch (_: Exception) {
            "Recently"
        }
    }
}
