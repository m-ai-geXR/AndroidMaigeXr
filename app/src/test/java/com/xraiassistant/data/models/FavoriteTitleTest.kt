package com.xraiassistant.data.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoriteTitleTest {

    private val lanternSeaCode = "let S, G, H;\n\nfunction init() {\n  nova64.scene.clearScene();\n}"

    @Test
    fun `uses the markdown heading the AI gave the scene`() {
        val message = "## The Lantern Sea\n\nAn asset-free, five-chapter dream voyage.\n\n[INSERT_CODE]```javascript\n$lanternSeaCode\n```"
        assertEquals("The Lantern Sea", FavoriteTitle.from(message, lanternSeaCode))
    }

    @Test
    fun `uses a bold lead sentence when there is no heading`() {
        val message = "**Follow the storm.** This cart creates a four-minute journey.\n\n[INSERT_CODE]```javascript\nlet S;\n```"
        assertEquals("Follow the storm", FavoriteTitle.from(message, "let S;"))
    }

    @Test
    fun `ignores headings inside the code block`() {
        val message = "Here is your scene.\n\n```javascript\n// ## not a title\nconst scene = new BABYLON.Scene(engine);\n```"
        assertEquals(
            "const scene = new BABYLON.Scene(engine);",
            FavoriteTitle.from(message, "// ## not a title\nconst scene = new BABYLON.Scene(engine);")
        )
    }

    @Test
    fun `code fallback skips blank and comment lines like iOS`() {
        val code = "\n// Nova64 cart\n/* lifecycle */\n * detail\n  let cubes = [];\n"
        assertEquals("let cubes = [];", FavoriteTitle.fromCode(code))
    }

    @Test
    fun `strips inline markdown and links`() {
        val message = "# A *glowing* `torus` [field](https://example.com)\n```js\nx\n```"
        assertEquals("A glowing torus field", FavoriteTitle.from(message, "x"))
    }

    @Test
    fun `long titles are cut at a word boundary`() {
        val message = "## An extraordinarily long scene name that keeps going well past the limit\n```js\nx\n```"
        val title = FavoriteTitle.from(message, "x")
        assertTrue(title, title.length <= FavoriteTitle.MAX_LENGTH)
        assertTrue(title, title.endsWith("…") && !title.contains("  "))
    }

    @Test
    fun `falls back to Untitled Scene when there is nothing to use`() {
        assertEquals(FavoriteTitle.UNTITLED, FavoriteTitle.from(null, "\n// only a comment\n"))
    }

    @Test
    fun `legacy reproduces the old rule so old titles can be found`() {
        assertEquals("let S, G, H;", FavoriteTitle.legacy(lanternSeaCode))
    }
}
