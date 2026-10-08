package com.xraiassistant.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily

/**
 * Presets for the conversation canvas: its backdrop and how bubbles look.
 * Matches the iOS presets. Each has a light and a dark palette and follows the
 * app appearance (System, Light or Dark) like the rest of the UI. Text pairs
 * meet WCAG AA (4.5:1); ChatThemeContrastTest checks them.
 */
enum class ChatTheme(val storageValue: String, val displayName: String) {
    CLEAN("clean", "Clean"),
    NEON_GRID("neonGrid", "Neon Grid"),
    TERMINAL("terminal", "Terminal"),
    MIDNIGHT("midnight", "Midnight");

    val monospaced: Boolean get() = this == TERMINAL

    fun palette(dark: Boolean): ChatPalette = when (this) {
        CLEAN -> if (!dark) ChatPalette(0xFF2050E0, 0xFFFFFFFF, 0xFFEAE9E9, 0xFF201E1D, 0x2E201E1D, 0xFFF3F2F2, 0xFFF3F2F2, 0x2E201E1D, 0xFF2050E0)
                 else ChatPalette(0xFF3F6BF0, 0xFFFFFFFF, 0xFF151821, 0xFFF3F2F2, 0x2EF3F2F2, 0xFF0B0D12, 0xFF0B0D12, 0x1FF3F2F2, 0xFF7B9BFF)
        NEON_GRID -> if (!dark) ChatPalette(0xFFA21CAF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF201E1D, 0x730891B2, 0xFFFAF0FF, 0xFFF3F2F2, 0x1F0891B2, 0xFFA21CAF)
                 else ChatPalette(0xFFA21CAF, 0xFFFFFFFF, 0xFF14091F, 0xFFF3F2F2, 0x7322D3EE, 0xFF1A0B2E, 0xFF0B0D12, 0x1A22D3EE, 0xFFE879F9)
        TERMINAL -> if (!dark) ChatPalette(0xFF166534, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF052E16, 0x6616A34A, 0xFFF2FAF4, 0xFFF2FAF4, 0x0F16A34A, 0xFF166534)
                 else ChatPalette(0xFF0F3D1A, 0xFF7CFFB0, 0xFF0A140C, 0xFFC8FFD9, 0x5939FF88, 0xFF050805, 0xFF050805, 0x0A39FF88, 0xFF4ADE80)
        MIDNIGHT -> if (!dark) ChatPalette(0xFF2050E0, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF201E1D, 0x1F1E293B, 0xFFE9EDFB, 0xFFF3F2F2, 0x00000000, 0xFF2050E0)
                 else ChatPalette(0xFF3F6BF0, 0xFFFFFFFF, 0xFF141A33, 0xFFF3F2F2, 0x14FFFFFF, 0xFF0B1026, 0xFF05070F, 0x00000000, 0xFF7B9BFF)
    }

    companion object {
        fun from(value: String?): ChatTheme = entries.firstOrNull { it.storageValue == value } ?: CLEAN
    }
}

/**
 * One appearance of a chat preset, as 0xAARRGGBB values so contrast can be
 * tested. [accent] colours links and buttons; it is separate from the sent
 * bubble because on a dark backdrop no single colour can carry white text
 * and also stand out as text itself.
 */
data class ChatPalette(
    val userBubble: Long,
    val userText: Long,
    val aiBubble: Long,
    val aiText: Long,
    val stroke: Long,
    val backdropTop: Long,
    val backdropBottom: Long,
    val pattern: Long,
    val accent: Long
)

fun Long.asColor(): Color = Color(this.toInt())

/** The active chat palette, for bubbles deep in the message tree. */
val LocalChatPalette = staticCompositionLocalOf { ChatTheme.CLEAN.palette(dark = false) }

/**
 * The message area in the chosen chat theme. Light or dark follows the app
 * (read from the surrounding background), the accent and reply bubble come
 * from the palette, and Terminal gets a monospaced face.
 */
@Composable
fun ChatThemeArea(theme: ChatTheme, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val base = MaterialTheme.colorScheme
    val dark = base.background.luminance() < 0.5f
    val palette = theme.palette(dark)
    val scheme = base.copy(
        primary = palette.accent.asColor(),
        onPrimary = if (dark) Color.Black else Color.White,
        surfaceVariant = palette.aiBubble.asColor(),
        onSurfaceVariant = palette.aiText.asColor()
    )
    val typography = if (theme.monospaced) MaterialTheme.typography.monospaced() else MaterialTheme.typography

    MaterialTheme(colorScheme = scheme, typography = typography) {
        CompositionLocalProvider(LocalChatPalette provides palette) {
            Box(modifier = modifier.chatBackdrop(theme, palette), content = content)
        }
    }
}

private fun Typography.monospaced(): Typography = copy(
    bodyLarge = bodyLarge.copy(fontFamily = FontFamily.Monospace),
    bodyMedium = bodyMedium.copy(fontFamily = FontFamily.Monospace),
    bodySmall = bodySmall.copy(fontFamily = FontFamily.Monospace),
    labelSmall = labelSmall.copy(fontFamily = FontFamily.Monospace)
)

private fun Modifier.chatBackdrop(theme: ChatTheme, p: ChatPalette): Modifier {
    val base = background(Brush.verticalGradient(listOf(p.backdropTop.asColor(), p.backdropBottom.asColor())))
    return when (theme) {
        ChatTheme.CLEAN -> base.dotGrid(p.pattern.asColor(), 22f)
        ChatTheme.NEON_GRID -> base.lineGrid(p.pattern.asColor(), 28f)
        ChatTheme.TERMINAL -> base.scanlines(p.pattern.asColor())
        ChatTheme.MIDNIGHT -> base
    }
}

private fun Modifier.dotGrid(color: Color, spacingDp: Float): Modifier = drawBehind {
    val step = spacingDp * density
    var y = step / 2
    while (y < size.height) {
        var x = step / 2
        while (x < size.width) {
            drawCircle(color, radius = 0.75f * density, center = Offset(x, y))
            x += step
        }
        y += step
    }
}

private fun Modifier.lineGrid(color: Color, spacingDp: Float): Modifier = drawBehind {
    val step = spacingDp * density
    var x = 0f
    while (x <= size.width) { drawLine(color, Offset(x, 0f), Offset(x, size.height), 0.5f * density); x += step }
    var y = 0f
    while (y <= size.height) { drawLine(color, Offset(0f, y), Offset(size.width, y), 0.5f * density); y += step }
}

private fun Modifier.scanlines(color: Color): Modifier = drawBehind {
    val step = 3f * density
    var y = 0f
    while (y <= size.height) { drawRect(color, Offset(0f, y), Size(size.width, density)); y += step }
}
