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
import androidx.compose.ui.text.font.FontFamily

/**
 * Presets for the conversation canvas: its backdrop and how bubbles look.
 * Matches the iOS presets. Presets rather than free-form styling so every
 * option stays readable; dark presets darken only the message area.
 */
enum class ChatTheme(val storageValue: String, val displayName: String) {
    CLEAN("clean", "Clean"),
    NEON_GRID("neonGrid", "Neon Grid"),
    TERMINAL("terminal", "Terminal"),
    MIDNIGHT("midnight", "Midnight");

    val isDark: Boolean get() = this != CLEAN
    val monospaced: Boolean get() = this == TERMINAL

    /** Fixed bubble colours; null keeps the brand colour. */
    val userBubble: Color? get() = when (this) {
        NEON_GRID -> Color(0xFFA21CAF)
        // Bright enough to read as an accent too: buttons share this colour.
        TERMINAL -> Color(0xFF22C55E)
        else -> null
    }
    val aiBubble: Color? get() = when (this) {
        NEON_GRID -> Color.Black.copy(alpha = 0.55f)
        TERMINAL -> Color(0xFF0A140C)
        MIDNIGHT -> Color(0xFF141A33)
        CLEAN -> null
    }
    val textColor: Color? get() = if (this == TERMINAL) Color(0xFFC8FFD9) else null

    /** A thin outline gives bubbles an edge against busy backdrops. */
    val bubbleStroke: Color get() = when (this) {
        CLEAN -> Color(0x2E201E1D)
        NEON_GRID -> Color(0x7322D3EE)
        TERMINAL -> Color(0x5939FF88)
        MIDNIGHT -> Color(0x14FFFFFF)
    }

    val swatch: List<Color> get() = when (this) {
        CLEAN -> listOf(Color(0xFFF3F2F2), Color(0xFF2050E0))
        NEON_GRID -> listOf(Color(0xFF1A0B2E), Color(0xFFA21CAF))
        TERMINAL -> listOf(Color(0xFF050805), Color(0xFF39FF88))
        MIDNIGHT -> listOf(Color(0xFF0B1026), Color(0xFF2050E0))
    }

    companion object {
        fun from(value: String?): ChatTheme = entries.firstOrNull { it.storageValue == value } ?: CLEAN
    }
}

/** The active chat theme, for bubble outlines deep in the message tree. */
val LocalChatTheme = staticCompositionLocalOf { ChatTheme.CLEAN }

/**
 * The message area in the chosen chat theme: its backdrop behind [content],
 * a colour scheme whose primary and surfaceVariant are the bubble colours, and
 * a monospaced face for Terminal.
 */
@Composable
fun ChatThemeArea(theme: ChatTheme, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val base = if (theme.isDark) BrandDarkColorScheme else MaterialTheme.colorScheme
    val scheme = base.copy(
        primary = theme.userBubble ?: base.primary,
        surfaceVariant = theme.aiBubble ?: base.surfaceVariant,
        onPrimary = if (theme == ChatTheme.TERMINAL) Color.Black else base.onPrimary,
        onSurface = theme.textColor ?: base.onSurface,
        onSurfaceVariant = theme.textColor ?: base.onSurfaceVariant
    )
    val typography = if (theme.monospaced) MaterialTheme.typography.monospaced() else MaterialTheme.typography

    MaterialTheme(colorScheme = scheme, typography = typography) {
        CompositionLocalProvider(LocalChatTheme provides theme) {
            Box(modifier = modifier.chatBackdrop(theme, base.background), content = content)
        }
    }
}

private fun Typography.monospaced(): Typography = copy(
    bodyLarge = bodyLarge.copy(fontFamily = FontFamily.Monospace),
    bodyMedium = bodyMedium.copy(fontFamily = FontFamily.Monospace),
    bodySmall = bodySmall.copy(fontFamily = FontFamily.Monospace),
    labelSmall = labelSmall.copy(fontFamily = FontFamily.Monospace)
)

private fun Modifier.chatBackdrop(theme: ChatTheme, background: Color): Modifier = when (theme) {
    ChatTheme.CLEAN -> background(background).then(Modifier.dotGrid(Color(0x2E201E1D), 22f))
    ChatTheme.NEON_GRID -> background(Brush.verticalGradient(listOf(Color(0xFF1A0B2E), Color(0xFF0B0D12))))
        .then(Modifier.lineGrid(Color(0x1A22D3EE), 28f))
    ChatTheme.TERMINAL -> background(Color(0xFF050805)).then(Modifier.scanlines(Color(0x0A39FF88)))
    ChatTheme.MIDNIGHT -> background(Brush.linearGradient(listOf(Color(0xFF0B1026), Color(0xFF05070F))))
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
