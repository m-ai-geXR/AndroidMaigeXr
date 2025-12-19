package com.xraiassistant.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.xraiassistant.R

/**
 * Exo 2 Font Family - Futuristic geometric sans-serif
 *
 * TEMPORARY: Using system SansSerif as fallback due to Downloadable Fonts complexity.
 * System SansSerif (Roboto on Android) is clean, modern, and works immediately.
 *
 * TODO: Implement proper Downloadable Fonts with async loading in future update.
 * Requires additional AndroidManifest.xml configuration and async font loading.
 *
 * Alternative: Roboto is Android's default and has a modern, tech-forward aesthetic.
 */
val ExoFontFamily = FontFamily.SansSerif  // System default (Roboto on most devices)

/**
 * Neon Cyberpunk Typography System
 *
 * Uses Exo 2 for modern futuristic feel with geometric letterforms.
 * Monospace for code/technical elements to emphasize developer focus.
 */
val Typography = Typography(
    // Display styles (large headers, hero text)
    displayLarge = TextStyle(
        fontFamily = ExoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = ExoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = ExoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),

    // Headline styles (section headers)
    headlineLarge = TextStyle(
        fontFamily = ExoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = ExoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = ExoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),

    // Title styles (card titles, dialog titles)
    titleLarge = TextStyle(
        fontFamily = ExoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.5.sp  // Slightly wider for futuristic feel
    ),
    titleMedium = TextStyle(
        fontFamily = ExoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = ExoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),

    // Body text (main content, descriptions)
    bodyLarge = TextStyle(
        fontFamily = ExoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.25.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = ExoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = ExoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),

    // Label styles (buttons, tabs, form labels)
    labelLarge = TextStyle(
        fontFamily = ExoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = ExoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),

    // labelSmall - Used for CODE and technical elements (monospace)
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,  // Monospace for code/tech
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
