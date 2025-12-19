package com.xraiassistant.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Neon Cyberpunk Dark Color Scheme
 * Based on m{ai}geXR branding guide - vaporwave/cyberpunk aesthetic
 */
private val NeonCyberpunkColorScheme = darkColorScheme(
    // Primary colors (neon cyan for main UI elements)
    primary = NeonCyan,
    onPrimary = CyberpunkBlack,  // Dark text on bright neon buttons
    primaryContainer = NeonCyanGlow,
    onPrimaryContainer = CyberpunkWhite,

    // Secondary colors (neon purple for accents)
    secondary = NeonPurple,
    onSecondary = CyberpunkBlack,
    secondaryContainer = NeonPurpleGlow,
    onSecondaryContainer = CyberpunkWhite,

    // Tertiary colors (neon pink for highlights)
    tertiary = NeonPink,
    onTertiary = CyberpunkBlack,
    tertiaryContainer = NeonPinkGlow,
    onTertiaryContainer = CyberpunkWhite,

    // Background & Surface (dark cyberpunk backgrounds)
    background = CyberpunkBlack,
    onBackground = CyberpunkWhite,
    surface = CyberpunkDarkGray,
    onSurface = CyberpunkWhite,
    surfaceVariant = CyberpunkDarkGray,
    onSurfaceVariant = CyberpunkGray,

    // Status colors (neon versions)
    error = ErrorNeon,
    onError = CyberpunkWhite,
    errorContainer = Color(0x33FF0055),  // Error glow
    onErrorContainer = ErrorNeon,

    // Outlines & borders
    outline = CyberpunkGray,
    outlineVariant = CyberpunkDimGray,

    // Container colors
    surfaceContainer = CyberpunkDarkGray,
    surfaceContainerHigh = CyberpunkDarkGray,
    surfaceContainerHighest = CyberpunkDarkGray,
    surfaceContainerLow = CyberpunkBlack,
    surfaceContainerLowest = CyberpunkBlack,
)

/**
 * m{ai}geXR Theme
 *
 * Neon cyberpunk aesthetic with:
 * - DARK MODE ONLY (no light theme support)
 * - Vibrant neon colors (cyan, pink, purple, blue, green)
 * - Jet black backgrounds
 * - Futuristic typography (Exo 2 font)
 * - Subtle glow effects
 */
@Composable
fun XRAiAssistantTheme(
    content: @Composable () -> Unit
) {
    // ALWAYS use dark neon theme - no dynamic color, no light mode
    val colorScheme = NeonCyberpunkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Status bar: Cyberpunk black with light icons
            window.statusBarColor = CyberpunkBlack.toArgb()
            // Navigation bar: Match background
            window.navigationBarColor = CyberpunkBlack.toArgb()

            // Light icons on dark background
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
