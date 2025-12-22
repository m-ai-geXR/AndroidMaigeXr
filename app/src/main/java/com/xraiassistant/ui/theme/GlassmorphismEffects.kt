package com.xraiassistant.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Glassmorphism Effects for Modern Cyberpunk UI
 *
 * Inspired by modern dark cyberpunk designs with:
 * - Semi-transparent glass cards with backdrop blur simulation
 * - Gradient borders (cyan→pink, purple→blue)
 * - Gradient backgrounds for buttons and accents
 * - Layered dual-color glows
 *
 * Design references: DRQ Cyberpunk, AI Portrait Generator, PIXO
 */

/**
 * Glass card effect with semi-transparent background and glow
 *
 * Creates a modern glassmorphic card with:
 * - Semi-transparent dark background (simulates frosted glass)
 * - Subtle border glow
 * - Elevated shadow for depth
 *
 * Perfect for: Cards, containers, modals
 *
 * @param backgroundColor Base color (use glass variants like GlassCyberpunkDarkGray)
 * @param blurRadius Glow intensity (8-12dp recommended)
 * @param borderGlow Optional border glow color (null = no border glow)
 * @param shape Card shape (default 14dp rounded corners)
 */
fun Modifier.glassCard(
    backgroundColor: Color,
    blurRadius: Dp = 10.dp,
    borderGlow: Color? = null,
    shape: Shape = RoundedCornerShape(14.dp)
) = this
    .then(
        if (borderGlow != null) {
            Modifier.shadow(
                elevation = blurRadius,
                shape = shape,
                ambientColor = borderGlow.copy(alpha = 0.35f),
                spotColor = borderGlow.copy(alpha = 0.35f)
            )
        } else Modifier
    )
    .clip(shape)
    .background(backgroundColor, shape)

/**
 * Gradient border effect
 *
 * Creates a smooth gradient border transitioning between colors.
 * Key feature from design references - used heavily in AI Portrait Generator.
 *
 * Common gradients:
 * - AI messages: Cyan → Pink (listOf(NeonCyan, NeonPink))
 * - User messages: Blue fade (listOf(NeonBlue, NeonBlue.copy(alpha = 0.3f)))
 * - Accent borders: Purple → Blue (listOf(NeonPurple, NeonBlue))
 *
 * @param colors List of colors for gradient (2-4 colors recommended)
 * @param width Border width (1.5-2dp recommended)
 * @param shape Border shape (should match card shape)
 * @param angle Gradient angle in degrees (0 = left-to-right, 90 = top-to-bottom)
 */
fun Modifier.gradientBorder(
    colors: List<Color>,
    width: Dp = 1.5.dp,
    shape: Shape = RoundedCornerShape(14.dp),
    angle: Float = 45f  // Diagonal gradient by default
) = this.border(
    width = width,
    brush = Brush.linearGradient(
        colors = colors,
        start = androidx.compose.ui.geometry.Offset(
            x = if (angle == 0f) 0f else Float.POSITIVE_INFINITY,
            y = if (angle == 90f) 0f else Float.POSITIVE_INFINITY
        ),
        end = androidx.compose.ui.geometry.Offset(
            x = if (angle == 0f) Float.POSITIVE_INFINITY else 0f,
            y = if (angle == 90f) Float.POSITIVE_INFINITY else 0f
        )
    ),
    shape = shape
)

/**
 * Gradient background effect
 *
 * Creates a smooth gradient background for buttons, headers, or accent areas.
 * Heavily used in all design references for CTAs and interactive elements.
 *
 * Common patterns:
 * - Buttons: Cyan → Purple horizontal (listOf(NeonCyan, NeonPurple))
 * - Accents: Cyan → Pink (listOf(NeonCyan, NeonPink))
 * - Dividers: Color → Transparent fade (listOf(NeonCyan, Color.Transparent))
 *
 * @param colors List of colors for gradient
 * @param angle Gradient angle in degrees (0 = horizontal, 90 = vertical)
 * @param shape Background shape
 */
fun Modifier.gradientBackground(
    colors: List<Color>,
    angle: Float = 0f,  // Horizontal by default (left-to-right)
    shape: Shape = RoundedCornerShape(14.dp)
) = this
    .clip(shape)
    .background(
        brush = when (angle) {
            0f -> Brush.horizontalGradient(colors)
            90f -> Brush.verticalGradient(colors)
            else -> Brush.linearGradient(
                colors = colors,
                start = androidx.compose.ui.geometry.Offset.Zero,
                end = androidx.compose.ui.geometry.Offset(
                    x = kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat() * 1000f,
                    y = kotlin.math.sin(Math.toRadians(angle.toDouble())).toFloat() * 1000f
                )
            )
        },
        shape = shape
    )

/**
 * Layered dual-color glow effect
 *
 * Creates a layered glow with two colors for more dynamic visual depth.
 * Useful for highlighting important interactive elements.
 *
 * @param primaryColor Main glow color (outer layer)
 * @param secondaryColor Secondary glow color (inner layer)
 * @param primaryBlur Primary glow blur radius (default 12dp)
 * @param secondaryBlur Secondary glow blur radius (default 6dp)
 * @param shape Glow shape
 */
fun Modifier.layeredGlow(
    primaryColor: Color,
    secondaryColor: Color,
    primaryBlur: Dp = 12.dp,
    secondaryBlur: Dp = 6.dp,
    shape: Shape = RoundedCornerShape(14.dp)
) = this
    .shadow(
        elevation = primaryBlur,
        shape = shape,
        ambientColor = primaryColor.copy(alpha = 0.3f),
        spotColor = primaryColor.copy(alpha = 0.3f)
    )
    .shadow(
        elevation = secondaryBlur,
        shape = shape,
        ambientColor = secondaryColor.copy(alpha = 0.4f),
        spotColor = secondaryColor.copy(alpha = 0.4f)
    )

/**
 * Modern button with gradient background and glow
 *
 * Combines gradient background with glow for modern CTA buttons.
 * Matches the style from AI Portrait Generator design reference.
 *
 * @param gradientColors Gradient colors (e.g., cyan → purple)
 * @param glowColor Glow color (typically matches gradient start color)
 * @param shape Button shape
 */
fun Modifier.gradientButton(
    gradientColors: List<Color>,
    glowColor: Color,
    shape: Shape = RoundedCornerShape(12.dp)
) = this
    .neonGlow(glowColor, blurRadius = 12.dp)
    .gradientBackground(gradientColors, angle = 0f, shape = shape)

/**
 * Sleek card with glass effect and gradient border
 *
 * Complete card styling matching design references.
 * Combines glass background with gradient border for maximum visual impact.
 *
 * @param glassBackground Glass background color (use GlassCyberpunkDarkGray)
 * @param borderGradient List of colors for border gradient
 * @param glowColor Glow color (typically first color in gradient)
 * @param shape Card shape
 */
fun Modifier.sleekCard(
    glassBackground: Color,
    borderGradient: List<Color>,
    glowColor: Color,
    shape: Shape = RoundedCornerShape(14.dp)
) = this
    .glassCard(
        backgroundColor = glassBackground,
        blurRadius = 10.dp,
        borderGlow = glowColor,
        shape = shape
    )
    .gradientBorder(
        colors = borderGradient,
        width = 1.5.dp,
        shape = shape,
        angle = 45f
    )
