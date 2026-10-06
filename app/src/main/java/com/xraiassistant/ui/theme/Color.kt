package com.xraiassistant.ui.theme

import androidx.compose.ui.graphics.Color

// ================================
// m{ai}geXR BRAND PALETTE
// ================================
//
// Values come from brand/brand.json, which mirrors maigexr.seacloud9.studio.
// Cobalt is the single accent; everything else is a neutral ground. The old
// neon/cyberpunk names are kept at the bottom as aliases so existing call sites
// keep compiling, but they now resolve to brand colours.
//
// Prefer MaterialTheme.colorScheme over these constants in new code, so that
// light and dark both work. A top-level val cannot follow the theme.

// Accent — cobalt. Dark holds a lighter step so it keeps contrast on near-black.
val BrandAccent = Color(0xFF2050E0)
val BrandAccentDark = Color(0xFF3F6BF0)
val BrandAccent2 = Color(0xFF4A74EA)
val BrandAccent400 = Color(0xFF8AA6FF)
val BrandAccent700 = Color(0xFF102F96)

// Light ground
val BrandBgLight = Color(0xFFF3F2F2)
val BrandSurfaceLight = Color(0xFFEAE9E9)
val BrandTextLight = Color(0xFF201E1D)
val BrandMutedLight = Color(0xFF605D5D)
val BrandDividerLight = Color(0x66201E1D)

// Dark ground
val BrandBgDark = Color(0xFF0B0D12)
val BrandSurfaceDark = Color(0xFF151821)
val BrandTextDark = Color(0xFFF3F2F2)
val BrandMutedDark = Color(0xFF9B9797)
val BrandDividerDark = Color(0x2EF3F2F2)

// Status
val BrandError = Color(0xFFD92D20)
val BrandWarning = Color(0xFFB54708)
val BrandSuccess = Color(0xFF067647)

// ================================
// LEGACY ALIASES
// ================================
//
// The neon palette these named is gone. They map onto brand colours so the
// screens that still reference them render correctly; migrate them to
// MaterialTheme.colorScheme and delete this block.

@Deprecated("Use MaterialTheme.colorScheme.primary", ReplaceWith("BrandAccent"))
val NeonPink = BrandAccent

@Deprecated("Use MaterialTheme.colorScheme.primary", ReplaceWith("BrandAccent"))
val NeonCyan = BrandAccentDark

@Deprecated("Use MaterialTheme.colorScheme.secondary", ReplaceWith("BrandAccent2"))
val NeonPurple = BrandAccent2

@Deprecated("Use MaterialTheme.colorScheme.primary", ReplaceWith("BrandAccent"))
val NeonBlue = BrandAccentDark

@Deprecated("Use BrandSuccess", ReplaceWith("BrandSuccess"))
val NeonGreen = BrandSuccess

@Deprecated("Use MaterialTheme.colorScheme.background", ReplaceWith("BrandBgDark"))
val CyberpunkBlack = BrandBgDark

@Deprecated("Use MaterialTheme.colorScheme.surface", ReplaceWith("BrandSurfaceDark"))
val CyberpunkDarkGray = BrandSurfaceDark

@Deprecated("Use MaterialTheme.colorScheme.surface", ReplaceWith("BrandSurfaceDark"))
val CyberpunkNavy = BrandSurfaceDark

@Deprecated("Use MaterialTheme.colorScheme.onBackground", ReplaceWith("BrandTextDark"))
val CyberpunkWhite = BrandTextDark

@Deprecated("Use MaterialTheme.colorScheme.onSurfaceVariant", ReplaceWith("BrandMutedDark"))
val CyberpunkGray = BrandMutedDark

@Deprecated("Use MaterialTheme.colorScheme.outlineVariant", ReplaceWith("BrandDividerDark"))
val CyberpunkDimGray = BrandDividerDark

@Deprecated("Use MaterialTheme.colorScheme.error", ReplaceWith("BrandError"))
val ErrorNeon = BrandError

@Deprecated("Use BrandWarning", ReplaceWith("BrandWarning"))
val WarningNeon = BrandWarning

// Glow tints. The brand has no glow; these are flat surface tints now so the
// screens that paint with them stay legible.
@Deprecated("Flat surfaces have no glow", ReplaceWith("BrandSurfaceDark"))
val NeonPinkGlow = Color(0x332050E0)

@Deprecated("Flat surfaces have no glow", ReplaceWith("BrandSurfaceDark"))
val NeonCyanGlow = Color(0x333F6BF0)

@Deprecated("Flat surfaces have no glow", ReplaceWith("BrandSurfaceDark"))
val NeonPurpleGlow = Color(0x334A74EA)

@Deprecated("Flat surfaces have no glow", ReplaceWith("BrandSurfaceDark"))
val NeonBlueGlow = Color(0x333F6BF0)

@Deprecated("Flat surfaces have no glow", ReplaceWith("BrandSurfaceDark"))
val NeonGreenGlow = Color(0x33067647)

// Glass fills. Kept so glassCard callers compile; they now read as plain
// surface tones rather than a blurred pane.
@Deprecated("Flat surfaces are not glass", ReplaceWith("BrandSurfaceDark"))
val GlassCyberpunkDarkGray = Color(0xE6151821)

@Deprecated("Flat surfaces are not glass", ReplaceWith("BrandBgDark"))
val GlassCyberpunkBlack = Color(0xE60B0D12)

@Deprecated("Flat surfaces are not glass", ReplaceWith("BrandSurfaceDark"))
val GlassCyberpunkNavy = Color(0xE6151821)

// ================================
// LEGACY GRADIENTS
// ================================
//
// The brand is flat: one accent on a neutral ground, no multi-hue gradients.
// These are kept so existing borders and dividers compile, and are reduced to
// single-hue cobalt fades so they read as a subtle edge rather than a rainbow.
// Migrate call sites to a plain outline and delete this block.

@Deprecated("The brand is flat; use a plain outline")
val CyanPinkGradient = listOf(BrandAccentDark, BrandAccent2)

@Deprecated("The brand is flat; use a plain outline")
val PurpleBlueGradient = listOf(BrandAccent2, BrandAccentDark)

@Deprecated("The brand is flat; use a plain outline")
val CyanGreenGradient = listOf(BrandAccentDark, BrandAccent400)

@Deprecated("The brand is flat; use a plain outline")
val BlueFadeGradient = listOf(BrandAccentDark, BrandAccentDark.copy(alpha = 0.3f))

@Deprecated("The brand is flat; use a plain outline")
val PinkFadeGradient = listOf(BrandAccent, BrandAccent.copy(alpha = 0.3f))

@Deprecated("The brand is flat; use a plain outline")
val CyanFadeGradient = listOf(BrandAccentDark, Color.Transparent)

@Deprecated("The brand is flat; use a plain outline")
val NavigationGradient = listOf(BrandAccentDark, BrandAccent2, BrandAccentDark)
