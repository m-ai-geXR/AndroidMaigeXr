package com.xraiassistant.domain.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Kotlin port of the Nova64 kit's template picker must agree with the JS kit
 * (WebMaigeXr/nova64-kit), which generated [Nova64TemplatesData.testVectors].
 */
class Nova64TemplatesTest {

    @Test
    fun chooseMatchesTheJavaScriptKit() {
        assertTrue(Nova64TemplatesData.testVectors.isNotEmpty())
        for (v in Nova64TemplatesData.testVectors) {
            val (id, words) = Nova64Templates.pick(v.message, v.current)
            assertEquals("\"${v.message}\" with current ${v.current}", v.expected, id)
            assertEquals("words for \"${v.message}\"", v.words, words)
        }
    }

    @Test
    fun noteNamesTheTemplateAndTheWords() {
        assertEquals(
            "\uD83C\uDFAE Started from the **Dungeon crawler \u00B7 Torchlit stone** template, because you said *dungeon*, *crawler*. Tap a template below to preview it, or say *from scratch* to write your own.",
            Nova64Templates.note("Make a dungeon crawler about four monks in a flooded abbey", null)
        )
        assertEquals("", Nova64Templates.note("make it faster", "// NOVA64 TEMPLATE: lane-runner-grid\n"))
    }

    @Test
    fun chipLabelsUseTheFirstGameAndStyleWords() {
        assertEquals(
            listOf("dungeon \u00B7 stone", "dungeon \u00B7 neon", "runner \u00B7 neon", "runner \u00B7 torch", "arena \u00B7 neon", "arena \u00B7 cave"),
            Nova64Templates.all.map { Nova64Templates.chipLabel(it) }
        )
    }

    @Test
    fun everyTemplateConfigNamesItself() {
        assertEquals(6, Nova64Templates.all.size)
        for (t in Nova64Templates.all) {
            assertEquals(t.id, Nova64Templates.templateIdOf(t.config))
            assertTrue(t.id, t.config.contains(Nova64Templates.CONFIG_MARK))
            assertFalse(t.id, t.config.contains(Nova64Templates.ENGINE_MARK))
        }
    }

    @Test
    fun promptBlockCarriesOnlyTheConfig() {
        val block = Nova64Templates.promptBlock("make a dungeon crawler about monks", null)
        assertNotNull(block)
        assertTrue(block!!.contains("GAME TEMPLATE MODE"))
        assertTrue(block.contains("// NOVA64 TEMPLATE: fp-dungeon-wizardry"))
        assertFalse(block.contains("{config}"))
        assertFalse(block.contains(Nova64Templates.ENGINE_MARK))
        assertTrue(block.length < 8000)
    }

    @Test
    fun followUpEditsTheConfigInTheEditor() {
        val edited = Nova64Templates.template("lane-runner-grid")!!.config
            .replace("title: 'SHARDGRID RUN'", "title: 'EMBER DASH'")
        val cart = "const { sfx } = nova64.audio;\n\n$edited\n${Nova64Templates.ENGINE_MARK}\nfunction init() {}\n"
        val block = Nova64Templates.promptBlock("make it faster", cart)!!
        assertTrue(block.contains("title: 'EMBER DASH'"))
        assertFalse(block.contains("function init() {}"))
    }

    @Test
    fun nonGameRequestsStayFreeForm() {
        assertNull(Nova64Templates.promptBlock("a spinning cube with a glowing orb", null))
    }

    @Test
    fun nova64ExamplesIncludeTheGameTemplates() {
        val ids = Nova64Library().examples.map { it.id }
        for (t in Nova64Templates.all) assertTrue(t.id, ids.contains("nova64-template-${t.id}"))
    }

    @Test
    fun systemPromptTeachesTheVerifiedLessons() {
        val prompt = Nova64Library().systemPrompt
        assertTrue(prompt.contains("createPlane stands upright"))
        assertTrue(prompt.contains("GAME TEMPLATE MODE"))
        assertTrue(Nova64Library().defaultSceneCode.contains("setRotation(groundId, -Math.PI / 2, 0, 0)"))
    }
}
