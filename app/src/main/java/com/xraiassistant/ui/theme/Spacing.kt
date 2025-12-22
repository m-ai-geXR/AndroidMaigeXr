package com.xraiassistant.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Centralized Spacing System for m{ai}geXR
 *
 * Provides consistent spacing values across the app based on 4dp grid.
 * Inspired by modern cyberpunk design patterns with generous padding.
 *
 * Design philosophy:
 * - Use consistent multiples of 4dp for alignment
 * - Increase padding for better readability (20dp vs old 16dp)
 * - Larger corner radius for modern look (14dp vs old 8dp)
 * - Clear visual hierarchy through spacing
 */
object Spacing {
    // Base spacing scale (4dp grid)
    val extraSmall = 4.dp
    val small = 8.dp
    val medium = 12.dp
    val large = 16.dp
    val extraLarge = 20.dp
    val xxLarge = 24.dp
    val xxxLarge = 32.dp

    // Card-specific spacing
    val cardPadding = 20.dp           // Increased from 16dp for better breathing room
    val cardPaddingSmall = 16.dp      // For compact cards
    val cardPaddingLarge = 24.dp      // For emphasized cards
    val cardCornerRadius = 14.dp      // Increased from 8dp for modern rounded look
    val cardBorderWidth = 1.5.dp      // Standard border width
    val cardGlowRadius = 8.dp         // Increased from 6dp for more presence
    val cardSpacing = 12.dp           // Space between cards in lists

    // Button spacing
    val buttonPadding = 16.dp
    val buttonPaddingHorizontal = 24.dp
    val buttonPaddingVertical = 14.dp
    val buttonCornerRadius = 12.dp    // Slightly smaller than cards
    val buttonGlowRadius = 12.dp      // Strong glow for CTAs

    // Input field spacing
    val inputPadding = 16.dp
    val inputCornerRadius = 14.dp     // Match card radius
    val inputGlowRadius = 10.dp       // Medium glow

    // List item spacing
    val listItemPadding = 20.dp
    val listItemSpacing = 12.dp
    val listItemCornerRadius = 14.dp

    // Section spacing
    val sectionPadding = 24.dp
    val sectionSpacing = 20.dp

    // Icon sizes
    val iconSmall = 16.dp
    val iconMedium = 20.dp
    val iconLarge = 24.dp
    val iconExtraLarge = 32.dp

    // Avatar/thumbnail sizes
    val avatarSmall = 40.dp
    val avatarMedium = 60.dp
    val avatarLarge = 80.dp           // Used for conversation thumbnails
    val avatarExtraLarge = 120.dp

    // Divider thickness
    val dividerThin = 1.dp
    val dividerMedium = 2.dp
    val dividerThick = 3.dp

    // Navigation bar
    val navBarHeight = 80.dp
    val navBarPadding = 12.dp
    val navBarIconSize = 24.dp

    // Glow intensity scale
    val glowSubtle = 4.dp             // Minimal presence
    val glowMedium = 6.dp             // Balanced
    val glowStrong = 8.dp             // Cards, containers
    val glowIntense = 10.dp           // Input fields
    val glowMaximum = 12.dp           // Buttons, CTAs
}
