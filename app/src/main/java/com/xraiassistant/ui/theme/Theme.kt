package com.xraiassistant.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * m{ai}geXR colour schemes.
 *
 * One cobalt accent over a neutral ground, per brand/brand.json. Both schemes
 * are defined so the app can follow the system setting; it was dark-only before.
 */
private val BrandDarkColorScheme = darkColorScheme(
    primary = BrandAccentDark,
    onPrimary = Color.White,
    primaryContainer = BrandAccent700,
    onPrimaryContainer = BrandTextDark,

    secondary = BrandAccent2,
    onSecondary = Color.White,
    secondaryContainer = BrandSurfaceDark,
    onSecondaryContainer = BrandTextDark,

    tertiary = BrandAccent400,
    onTertiary = BrandBgDark,
    tertiaryContainer = BrandSurfaceDark,
    onTertiaryContainer = BrandTextDark,

    background = BrandBgDark,
    onBackground = BrandTextDark,
    surface = BrandBgDark,
    onSurface = BrandTextDark,
    surfaceVariant = BrandSurfaceDark,
    onSurfaceVariant = BrandMutedDark,

    error = BrandError,
    onError = Color.White,
    errorContainer = BrandSurfaceDark,
    onErrorContainer = BrandError,

    outline = BrandMutedDark,
    outlineVariant = BrandDividerDark,

    surfaceContainer = BrandSurfaceDark,
    surfaceContainerHigh = BrandSurfaceDark,
    surfaceContainerHighest = BrandSurfaceDark,
    surfaceContainerLow = BrandBgDark,
    surfaceContainerLowest = BrandBgDark,
)

private val BrandLightColorScheme = lightColorScheme(
    primary = BrandAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBE3FF),
    onPrimaryContainer = BrandAccent700,

    secondary = BrandAccent2,
    onSecondary = Color.White,
    secondaryContainer = BrandSurfaceLight,
    onSecondaryContainer = BrandTextLight,

    tertiary = BrandAccent700,
    onTertiary = Color.White,
    tertiaryContainer = BrandSurfaceLight,
    onTertiaryContainer = BrandTextLight,

    background = BrandBgLight,
    onBackground = BrandTextLight,
    surface = BrandBgLight,
    onSurface = BrandTextLight,
    surfaceVariant = BrandSurfaceLight,
    onSurfaceVariant = BrandMutedLight,

    error = BrandError,
    onError = Color.White,
    errorContainer = Color(0xFFFFE0D9),
    onErrorContainer = BrandError,

    outline = BrandMutedLight,
    outlineVariant = BrandDividerLight,

    surfaceContainer = BrandSurfaceLight,
    surfaceContainerHigh = BrandSurfaceLight,
    surfaceContainerHighest = BrandSurfaceLight,
    surfaceContainerLow = BrandBgLight,
    surfaceContainerLowest = BrandBgLight,
)

/**
 * m{ai}geXR theme.
 *
 * Follows the system light/dark setting. Dynamic colour is deliberately off:
 * the cobalt accent is the brand, so letting the wallpaper pick the palette
 * would defeat the point.
 */
@Composable
fun XRAiAssistantTheme(
    darkTheme: Boolean = resolveDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) BrandDarkColorScheme else BrandLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()

            // Icon contrast has to invert with the ground, or the status bar
            // disappears into it in light mode.
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}


/**
 * Whether to use the dark scheme, honouring the user's choice over the system.
 */
@Composable
private fun resolveDarkTheme(): Boolean {
    val mode by AppearanceStore.mode.collectAsState()
    return when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
}
