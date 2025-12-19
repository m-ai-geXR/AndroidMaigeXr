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
 * Subtle glow for cards and containers (6dp blur).
 * Balanced to add presence without overwhelming the UI.
 *
 * @param color The neon color to glow
 */
fun Modifier.neonCardGlow(
    color: Color
) = this.neonGlow(color, blurRadius = 6.dp)

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
