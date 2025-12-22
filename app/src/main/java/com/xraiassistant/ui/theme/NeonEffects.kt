package com.xraiassistant.ui.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Neon Glow Effects for Cyberpunk Theme
 *
 * Enhanced glow effects with stronger intensity for interactive elements:
 * - Interactive elements (buttons, inputs): 8-12dp blur
 * - Cards/containers: 4-6dp blur (subtle)
 * - Static, no pulsing/flickering - clean and professional
 */

/**
 * Basic neon glow effect
 *
 * Adds a glow around the element using a semi-transparent color.
 * Uses shadow elevation for the glow effect.
 *
 * @param color The neon color to glow
 * @param blurRadius The blur radius (default 8dp for noticeable glow)
 */
fun Modifier.neonGlow(
    color: Color,
    blurRadius: Dp = 8.dp
) = this.then(
    shadow(
        elevation = blurRadius,
        shape = RoundedCornerShape(8.dp),
        ambientColor = color.copy(alpha = 0.35f),  // Slightly more opaque
        spotColor = color.copy(alpha = 0.35f)
    )
)

/**
 * Neon border with glow
 *
 * Adds a neon-colored border with a glow effect around it.
 * Perfect for cards, buttons, and containers.
 *
 * @param color The neon border color
 * @param width Border width (default 1.5dp for visibility)
 * @param glowRadius Glow blur radius (default 8dp for stronger presence)
 * @param shape Border shape (default rounded corners)
 */
fun Modifier.neonBorder(
    color: Color,
    width: Dp = 1.5.dp,
    glowRadius: Dp = 8.dp,
    shape: Shape = RoundedCornerShape(8.dp)
) = this
    .neonGlow(color, glowRadius)
    .border(width, color, shape)

/**
 * Neon button glow
 *
 * Strongest glow for interactive buttons (12dp blur).
 * Use with filled neon-colored buttons for maximum impact.
 *
 * @param color The neon color to glow
 */
fun Modifier.neonButtonGlow(
    color: Color
) = this.neonGlow(color, blurRadius = 12.dp)

/**
 * Neon input field glow
 *
 * Strong glow for text input fields (10dp blur).
 * Use for TextField, OutlinedTextField, and other input components.
 *
 * @param color The neon color to glow
 */
fun Modifier.neonInputGlow(
    color: Color
) = this.neonGlow(color, blurRadius = 10.dp)

/**
 * Neon card glow
 *
 * Enhanced glow for cards and containers (8dp blur).
 * Increased from 6dp for more presence and modern look.
 *
 * @param color The neon color to glow
 */
fun Modifier.neonCardGlow(
    color: Color
) = this.neonGlow(color, blurRadius = 8.dp)

/**
 * Neon outline glow
 *
 * For text or icons that need a glowing outline effect.
 * Creates a soft halo around the element.
 *
 * @param color The neon color to glow
 * @param strokeWidth The outline width
 * @param blurRadius The blur radius
 */
fun Modifier.neonOutlineGlow(
    color: Color,
    strokeWidth: Dp = 1.dp,
    blurRadius: Dp = 6.dp
) = this.then(
    drawBehind {
        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                this.color = color.copy(alpha = 0.4f)
                style = PaintingStyle.Stroke
                this.strokeWidth = strokeWidth.toPx()
            }

            // Draw multiple layers for glow effect
            for (i in 1..3) {
                val offset = (blurRadius.toPx() / 3f) * i
                paint.alpha = (0.4f / i)
                canvas.drawRoundRect(
                    left = -offset,
                    top = -offset,
                    right = size.width + offset,
                    bottom = size.height + offset,
                    radiusX = 8.dp.toPx(),
                    radiusY = 8.dp.toPx(),
                    paint = paint
                )
            }
        }
    }
)

/**
 * Neon text glow
 *
 * Creates a glow effect specifically for text elements.
 * Very subtle to maintain readability.
 *
 * @param color The neon color to glow
 */
fun Modifier.neonTextGlow(
    color: Color
) = this.then(
    drawBehind {
        drawRoundRect(
            color = color.copy(alpha = 0.15f),
            size = size
        )
    }
)

/**
 * Neon accent line
 *
 * Draws a glowing horizontal or vertical line for dividers and accents.
 *
 * @param color The neon color
 * @param thickness Line thickness
 * @param isVertical Whether the line is vertical (default false = horizontal)
 */
fun Modifier.neonAccentLine(
    color: Color,
    thickness: Dp = 2.dp,
    isVertical: Boolean = false
) = this.then(
    drawBehind {
        if (isVertical) {
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(size.width / 2, 0f),
                end = androidx.compose.ui.geometry.Offset(size.width / 2, size.height),
                strokeWidth = thickness.toPx()
            )
        } else {
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(0f, size.height / 2),
                end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2),
                strokeWidth = thickness.toPx()
            )
        }
    }
)

/**
 * Neon dual glow
 *
 * Layered glow effect with two colors for more dynamic visual depth.
 * Creates an inner and outer glow for enhanced presence.
 *
 * Perfect for: Emphasized buttons, important cards, hero elements
 *
 * @param primaryColor Outer glow color (larger radius)
 * @param secondaryColor Inner glow color (smaller radius)
 * @param primaryBlur Primary glow radius (default 12dp)
 * @param secondaryBlur Secondary glow radius (default 6dp)
 * @param shape Glow shape (default rounded corners)
 */
fun Modifier.neonDualGlow(
    primaryColor: Color,
    secondaryColor: Color,
    primaryBlur: Dp = 12.dp,
    secondaryBlur: Dp = 6.dp,
    shape: Shape = RoundedCornerShape(8.dp)
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
 * Neon gradient glow
 *
 * Creates a glow effect that transitions between two colors.
 * Simulates gradient glow by averaging the colors.
 *
 * Note: True gradient shadows aren't possible in Compose, so this
 * creates a blended color glow as an approximation.
 *
 * @param startColor Starting color of gradient
 * @param endColor Ending color of gradient
 * @param blurRadius Glow blur radius (default 8dp)
 * @param shape Glow shape
 */
fun Modifier.neonGradientGlow(
    startColor: Color,
    endColor: Color,
    blurRadius: Dp = 8.dp,
    shape: Shape = RoundedCornerShape(8.dp)
): Modifier {
    // Average the two colors for a blended glow effect
    val blendedColor = Color(
        red = (startColor.red + endColor.red) / 2f,
        green = (startColor.green + endColor.green) / 2f,
        blue = (startColor.blue + endColor.blue) / 2f,
        alpha = 0.35f
    )
    return this.shadow(
        elevation = blurRadius,
        shape = shape,
        ambientColor = blendedColor,
        spotColor = blendedColor
    )
}
