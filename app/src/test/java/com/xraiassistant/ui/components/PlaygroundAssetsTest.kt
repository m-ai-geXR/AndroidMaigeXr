package com.xraiassistant.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Static checks on the playground HTML shipped in assets, for input regressions
 * that are slow to notice by hand on a device.
 */
class PlaygroundAssetsTest {

    private val assetsDir = File("src/main/assets")

    private val playgrounds = listOf(
        "playground-babylonjs.html",
        "playground-threejs.html",
        "playground-aframe.html",
        "playground-nova64.html"
    )

    private fun read(name: String): String = File(assetsDir, name).readText()

    @Test
    fun `touchmove listeners are removed again so they only run during a drag`() {
        val add = Regex("""document\.addEventListener\('touchmove',\s*(\w+)""")
        val remove = Regex("""document\.removeEventListener\('touchmove',\s*(\w+)""")

        for (name in playgrounds) {
            val html = read(name)
            val added = add.findAll(html).map { it.groupValues[1] }.toList().sorted()
            val removed = remove.findAll(html).map { it.groupValues[1] }.toList().sorted()
            assertEquals(
                "$name: every page-wide touchmove listener must be removed when its drag ends, " +
                    "or every touch on the scene waits for the script",
                added,
                removed
            )
        }
    }

    @Test
    fun `demo dropdown resets after each pick so the same demo can be reloaded`() {
        for (name in playgrounds) {
            val html = read(name)
            assertTrue(
                "$name: demo select must clear its value after loading",
                html.contains("onchange=\"loadExample(this.value); this.value='';\"")
            )
        }
    }

    @Test
    fun `nova64 hands keyboard focus to the console when the scene shows`() {
        val html = read("playground-nova64.html")
        assertTrue("focusScene must exist", html.contains("function focusScene()"))

        // Bodies end at the next top-level function; inner callbacks are indented deeper.
        fun body(name: String) = html.substringAfter("function $name() {").substringBefore("\n        function ")

        val runCode = body("runCode")
        assertTrue("runCode must focus the scene", runCode.contains("focusScene()"))

        val toggle = body("toggleEditor")
        assertTrue("returning to the scene must focus it", toggle.contains("focusScene()"))
    }
}
