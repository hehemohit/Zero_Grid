package com.example.zerogrid.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalZeroGridColors = staticCompositionLocalOf { LightZeroGridColors }

object ZeroGridTheme {
    // 1. Provides color access: ZeroGridTheme.colors
    val colors: ZeroGridColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalZeroGridColors.current

    // 2. Provides theme wrapper: ZeroGridTheme(darkTheme = ...) { ... }
    @Composable
    operator fun invoke(
        darkTheme: Boolean = isSystemInDarkTheme(),
        content: @Composable () -> Unit
    ) {
        val customColors = if (darkTheme) DarkZeroGridColors else LightZeroGridColors
        val materialColors = if (darkTheme) {
            darkColorScheme(
                primary = customColors.primary,
                secondary = customColors.secondary,
                background = customColors.background,
                surface = customColors.cardBackground,
                onPrimary = customColors.background,
                onBackground = customColors.textPrimary,
                onSurface = customColors.textPrimary
            )
        } else {
            lightColorScheme(
                primary = customColors.primary,
                secondary = customColors.secondary,
                background = customColors.background,
                surface = customColors.cardBackground,
                onPrimary = customColors.cardBackground,
                onBackground = customColors.textPrimary,
                onSurface = customColors.textPrimary
            )
        }

        val view = LocalView.current
        if (!view.isInEditMode && view.context is Activity) {
            SideEffect {
                val window = (view.context as Activity).window
                window.statusBarColor = customColors.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }

        CompositionLocalProvider(LocalZeroGridColors provides customColors) {
            MaterialTheme(
                colorScheme = materialColors,
                content = content
            )
        }
    }
}
