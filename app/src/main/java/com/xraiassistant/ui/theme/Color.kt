package com.xraiassistant.ui.theme

import androidx.compose.ui.graphics.Color

// ================================
// NEON CYBERPUNK COLOR PALETTE
// ================================

// PRIMARY NEON COLORS (from m{ai}geXR branding guide)
val NeonPink = Color(0xFFFF00C1)           // Electric pink/magenta
val NeonCyan = Color(0xFF00FFF9)           // Aqua cyan
val NeonPurple = Color(0xFF9600FF)         // Deep purple
val NeonBlue = Color(0xFF00B8FF)           // Electric blue
val NeonGreen = Color(0xFF0CE907)          // Acid green (accent)

// DARK BACKGROUNDS
val CyberpunkBlack = Color(0xFF0A0A0A)     // Jet black
val CyberpunkDarkGray = Color(0xFF1A1A1A)  // Very dark gray
val CyberpunkNavy = Color(0xFF0D0D1F)      // Deep navy (optional for future gradients)

// GLOW VARIANTS (semi-transparent for shadow/glow effects)
val NeonPinkGlow = Color(0x33FF00C1)       // 20% opacity
val NeonCyanGlow = Color(0x3300FFF9)       // 20% opacity
val NeonPurpleGlow = Color(0x339600FF)     // 20% opacity
val NeonBlueGlow = Color(0x3300B8FF)       // 20% opacity
val NeonGreenGlow = Color(0x330CE907)      // 20% opacity

// TEXT COLORS
val CyberpunkWhite = Color(0xFFE0E0E0)     // Soft white for readability
val CyberpunkGray = Color(0xFF808080)      // Muted gray for secondary text
val CyberpunkDimGray = Color(0xFF4A4A4A)   // Dim gray for disabled states

// STATUS COLORS (neon versions)
val SuccessNeon = NeonGreen                // Success = neon green
val ErrorNeon = Color(0xFFFF0055)          // Neon red/pink for errors
val WarningNeon = Color(0xFFFFAA00)        // Neon orange for warnings

// ================================
// GLASSMORPHISM VARIANTS
// ================================

// GLASS OPACITY VARIANTS (for glassmorphic effects)
val GlassCyberpunkDarkGray = Color(0x59 shl 24 or 0x1A1A1A)  // 35% opacity - for cards
val GlassCyberpunkBlack = Color(0x40 shl 24 or 0x0A0A0A)      // 25% opacity - for overlays
val GlassCyberpunkNavy = Color(0x4D shl 24 or 0x0D0D1F)       // 30% opacity - for accents

// ================================
// GRADIENT COLOR PAIRS
// ================================
// Pre-defined gradient combinations for common UI patterns

// Cyan → Pink (AI messages, primary CTAs)
val CyanPinkGradient = listOf(NeonCyan, NeonPink)

// Purple → Blue (secondary accents)
val PurpleBlueGradient = listOf(NeonPurple, NeonBlue)

// Cyan → Green (success states, positive actions)
val CyanGreenGradient = listOf(NeonCyan, NeonGreen)

// Blue Fade (user messages, subtle highlights)
val BlueFadeGradient = listOf(NeonBlue, NeonBlue.copy(alpha = 0.3f))

// Pink Fade (error states, warnings)
val PinkFadeGradient = listOf(NeonPink, NeonPink.copy(alpha = 0.3f))

// Cyan Fade (dividers, accent lines)
val CyanFadeGradient = listOf(NeonCyan, Color.Transparent)

// Multi-color navigation gradient (cyan → pink → purple)
val NavigationGradient = listOf(NeonCyan, NeonPink, NeonPurple)
