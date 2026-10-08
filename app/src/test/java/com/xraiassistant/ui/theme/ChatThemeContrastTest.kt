package com.xraiassistant.ui.theme

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Every chat style, in light and dark, must keep text readable: WCAG AA asks
 * for 4.5:1 between text and its background. Covers bubbles, accent links on
 * the backdrop, and button text on the accent.
 */
class ChatThemeContrastTest {

    private fun luminance(argb: Long): Double {
        fun channel(v: Long): Double {
            val c = v / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel((argb shr 16) and 0xFF) + 0.7152 * channel((argb shr 8) and 0xFF) + 0.0722 * channel(argb and 0xFF)
    }

    private fun contrast(a: Long, b: Long): Double {
        val (l1, l2) = luminance(a) to luminance(b)
        return (max(l1, l2) + 0.05) / (min(l1, l2) + 0.05)
    }

    private fun check(what: String, a: Long, b: Long) {
        val ratio = contrast(a, b)
        assertTrue("$what is %.2f:1, needs 4.5:1".format(ratio), ratio >= 4.5)
    }

    @Test
    fun everyStyleIsReadableInLightAndDark() {
        for (theme in ChatTheme.entries) for (dark in listOf(false, true)) {
            val p = theme.palette(dark)
            val name = "${theme.displayName} ${if (dark) "dark" else "light"}"
            check("$name sent text", p.userText, p.userBubble)
            check("$name reply text", p.aiText, p.aiBubble)
            check("$name accent on backdrop top", p.accent, p.backdropTop)
            check("$name accent on backdrop bottom", p.accent, p.backdropBottom)
            check("$name button text", if (dark) 0xFF000000 else 0xFFFFFFFF, p.accent)
        }
    }

    @Test
    fun darkStylesAreDarkAndLightOnesLight() {
        for (theme in ChatTheme.entries) {
            assertTrue("${theme.displayName} dark", luminance(theme.palette(true).backdropTop) < 0.05)
            assertTrue("${theme.displayName} light", luminance(theme.palette(false).backdropTop) > 0.8)
        }
    }
}
