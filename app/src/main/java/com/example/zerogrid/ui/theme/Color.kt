package com.example.zerogrid.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Tactical Mesh Palette (Stitch Specification)
val PrimaryTeal = Color(0xFF005953)
val PrimaryTealLight = Color(0xFF008779)
val PrimaryTealDark = Color(0xFF00C4B4)

val SecondaryTeal = Color(0xFF008779)
val SecondaryTealLight = Color(0xFF10B981)

val AccentRed = Color(0xFFB91C1C)
val AccentRedBright = Color(0xFFEF4444)
val BadgeGreen = Color(0xFF10B981)

// Light Canvas & Surfaces
val BackgroundLight = Color(0xFFF8FAFC)
val CardBackgroundLight = Color(0xFFFFFFFF)
val SurfaceDarkerLight = Color(0xFFEEF2F6)
val TextDark = Color(0xFF0F172A)
val TextGray = Color(0xFF64748B)
val DividerColorLight = Color(0xFFE2E8F0)

// Dark Canvas & Surfaces
val BackgroundDark = Color(0xFF0B1312)
val CardBackgroundDark = Color(0xFF142220)
val SurfaceDarkerDark = Color(0xFF1A2C2A)
val TextLight = Color(0xFFF8FAFC)
val TextMuted = Color(0xFF94A3B8)
val DividerColorDark = Color(0xFF1E3330)

// Backward-compatibility legacy fallbacks
val DarkBackground = BackgroundDark
val CardBackground = CardBackgroundDark
val PrimaryCyan = PrimaryTealDark
val TextPrimary = TextLight
val TextSecondary = TextMuted
val LineColor = Color(0xFF1D5A6C)
val GreenGranted = Color(0xFF2E7D32)
val RedNotGranted = Color(0xFFD32F2F)
val GreyLabel = Color(0xFF4D5363)
val DisabledButton = Color(0xFF32363D)
val StatusActive = BadgeGreen
val StatusStable = Color(0xFFE0E0E0)
val AlertPink = AccentRedBright
val AlertRedBorder = Color(0x4DEF4444)
val AlertYellow = Color(0xFFEAB308)
val SurfaceDarker = SurfaceDarkerDark
val BottomNavBg = CardBackgroundLight
val DividerColor = DividerColorLight
val PrimaryCyanGlow = Color(0xFF00E5FF)
val InputFieldBackground = Color(0xFF1E1E1E)
val MeshLineColor = Color(0xFF333333)
val DarkIndicator = Color(0xFF2C2C2C)

@Immutable
data class ZeroGridColorScheme(
    val primary: Color,
    val secondary: Color,
    val background: Color,
    val cardBackground: Color,
    val surfaceNested: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accentRed: Color,
    val badgeGreen: Color,
    val divider: Color,
    val isDark: Boolean
)

val LightZeroGridColors = ZeroGridColorScheme(
    primary = PrimaryTeal,
    secondary = SecondaryTeal,
    background = BackgroundLight,
    cardBackground = CardBackgroundLight,
    surfaceNested = SurfaceDarkerLight,
    textPrimary = TextDark,
    textSecondary = TextGray,
    accentRed = AccentRedBright,
    badgeGreen = BadgeGreen,
    divider = DividerColorLight,
    isDark = false
)

val DarkZeroGridColors = ZeroGridColorScheme(
    primary = PrimaryTealDark,
    secondary = SecondaryTealLight,
    background = BackgroundDark,
    cardBackground = CardBackgroundDark,
    surfaceNested = SurfaceDarkerDark,
    textPrimary = TextLight,
    textSecondary = TextMuted,
    accentRed = AccentRedBright,
    badgeGreen = BadgeGreen,
    divider = DividerColorDark,
    isDark = true
)