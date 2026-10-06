package com.xraiassistant.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Accessibility checks for both colour schemes: WCAG AA contrast for the pairs
 * the screens actually draw, and a guard against colours that only work on the
 * dark ground (they made the API key fields unreadable in the light theme).
 */
class ThemeContrastTest {

    private val schemes = mapOf(
        "light" to BrandLightColorScheme,
        "dark" to BrandDarkColorScheme
    )

    /** WCAG 2.x contrast ratio; translucent colours are flattened onto [ground]. */
    private fun contrast(foreground: Color, ground: Color): Double {
        val fg = foreground.compositeOver(ground).luminance() + 0.05
        val bg = ground.luminance() + 0.05
        return maxOf(fg, bg) / minOf(fg, bg)
    }

    private fun assertContrast(scheme: String, pair: String, fg: Color, bg: Color, minimum: Double) {
        val ratio = contrast(fg, bg)
        assertTrue(
            "$scheme: $pair contrast is ${"%.2f".format(ratio)}:1, needs $minimum:1",
            ratio >= minimum
        )
    }

    /** Text pairs need 4.5:1 (WCAG AA body text). */
    private fun textPairs(c: ColorScheme) = listOf(
        Triple("onBackground on background", c.onBackground, c.background),
        Triple("onSurface on surface", c.onSurface, c.surface),
        Triple("onSurface on surfaceVariant", c.onSurface, c.surfaceVariant),
        Triple("onSurfaceVariant on surface", c.onSurfaceVariant, c.surface),
        Triple("onSurfaceVariant on surfaceVariant", c.onSurfaceVariant, c.surfaceVariant),
        Triple("primary on background", c.primary, c.background),
        Triple("primary on surfaceVariant", c.primary, c.surfaceVariant),
        Triple("onPrimary on primary", c.onPrimary, c.primary),
        Triple("onSecondary on secondary", c.onSecondary, c.secondary),
        Triple("secondary on background", c.secondary, c.background),
        Triple("onPrimaryContainer on primaryContainer", c.onPrimaryContainer, c.primaryContainer),
        Triple("error on background", c.error, c.background),
        Triple("error on surfaceVariant", c.error, c.surfaceVariant),
        Triple("onError on error", c.onError, c.error)
    )

    @Test
    fun `text colours meet WCAG AA in both themes`() {
        for ((name, scheme) in schemes) {
            for ((pair, fg, bg) in textPairs(scheme)) {
                assertContrast(name, pair, fg, bg, 4.5)
            }
        }
    }

    @Test
    fun `outlines and icons meet the 3 to 1 non-text minimum in both themes`() {
        for ((name, c) in schemes) {
            assertContrast(name, "outline on surface", c.outline, c.surface, 3.0)
            assertContrast(name, "outline on surfaceVariant", c.outline, c.surfaceVariant, 3.0)
            assertContrast(name, "secondary on surfaceVariant", c.secondary, c.surfaceVariant, 3.0)
        }
    }

    @Test
    fun `status colours meet WCAG AA on their own ground`() {
        val light = BrandLightColorScheme
        val dark = BrandDarkColorScheme
        for (ground in listOf(light.background, light.surfaceVariant)) {
            assertContrast("light", "success", BrandSuccess, ground, 4.5)
            assertContrast("light", "warning", BrandWarningOnLight, ground, 4.5)
        }
        for (ground in listOf(dark.background, dark.surfaceVariant)) {
            assertContrast("dark", "success", BrandSuccessOnDark, ground, 4.5)
            assertContrast("dark", "warning", BrandWarningOnDark, ground, 4.5)
        }
    }

    @Test
    fun `screens do not use colours that only work on the dark ground`() {
        // Fixed dark surfaces and the old Material palette hues, which fall below
        // AA on one theme or the other. Use MaterialTheme.colorScheme or StatusColors.
        val forbidden = Regex(
            """\b(CyberpunkDarkGray|CyberpunkNavy|CyberpunkBlack|CyberpunkGray|CyberpunkWhite|""" +
                """GlassCyberpunk\w+|NeonCyan|NeonBlue|NeonPink|NeonPurple|NeonGreen|ErrorNeon)\b|""" +
                """Color\(0xFF(4CAF50|FF9800|2196F3|F44336|FF5722)\)"""
        )
        val sources = File("src/main/java").walkTopDown()
            .filter { it.extension == "kt" && !it.path.contains("/ui/theme/") }

        val offenders = sources.flatMap { file ->
            file.readLines().mapIndexedNotNull { index, line ->
                if (line.trimStart().startsWith("import ")) return@mapIndexedNotNull null
                forbidden.find(line)?.let { "${file.path}:${index + 1} ${it.value}" }
            }
        }.toList()

        assertTrue("Theme-unsafe colours in use:\n" + offenders.joinToString("\n"), offenders.isEmpty())
    }
}
