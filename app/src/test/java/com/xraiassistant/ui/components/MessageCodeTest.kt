package com.xraiassistant.ui.components

import com.xraiassistant.data.models.ChatMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the Run Scene button appearing before any code did.
 *
 * The button rendered for every assistant message and used the extracted code
 * only to pick a colour, so the empty placeholder created at the start of a
 * stream showed a play button immediately.
 */
class MessageCodeTest {

    private fun ai(content: String, streaming: Boolean = false) =
        ChatMessage.aiMessage(content = content, isStreaming = streaming)

    private val scene = """
        ```javascript
        function init() {
          nova64.scene.createCube(1, 0xff0000, [0, 0, 0]);
          nova64.camera.setCameraPosition(0, 5, 11);
        }
        ```
    """.trimIndent()

    // MARK: the reported bug

    @Test
    fun `empty placeholder does not offer Run Scene`() {
        assertFalse(shouldShowRunScene(ai("", streaming = true), hasRunSceneHandler = true))
    }

    @Test
    fun `message that is still streaming does not offer Run Scene`() {
        // A closed block exists, but more may still be coming.
        assertTrue(extractCodeFromMessage(scene) != null)
        assertFalse(shouldShowRunScene(ai(scene, streaming = true), hasRunSceneHandler = true))
    }

    @Test
    fun `finished message with code offers Run Scene`() {
        assertTrue(shouldShowRunScene(ai(scene), hasRunSceneHandler = true))
    }

    @Test
    fun `finished prose without code does not offer Run Scene`() {
        val prose = "Sure. A spinning cube needs a mesh, a light and a camera. Want me to write it?"
        assertFalse(shouldShowRunScene(ai(prose), hasRunSceneHandler = true))
    }

    @Test
    fun `user message never offers Run Scene even when it contains code`() {
        val message = ChatMessage.userMessage(scene)
        assertFalse(shouldShowRunScene(message, hasRunSceneHandler = true))
    }

    @Test
    fun `welcome message is left to the Run Demo button`() {
        val welcome = ChatMessage(content = scene, isUser = false, isWelcomeMessage = true)
        assertFalse(shouldShowRunScene(welcome, hasRunSceneHandler = true))
    }

    @Test
    fun `no handler means no button`() {
        assertFalse(shouldShowRunScene(ai(scene), hasRunSceneHandler = false))
    }

    // MARK: open fences

    @Test
    fun `unclosed fence yields no code`() {
        val midStream = "Here you go:\n```javascript\nfunction init() {\n  nova64.scene.createCube("
        assertNull(extractCodeFromMessage(midStream))
    }

    @Test
    fun `fence that closes later is picked up once complete`() {
        val partial = "Here:\n```javascript\nconst a = 1; const b = 2;"
        assertNull(extractCodeFromMessage(partial))
        assertTrue(extractCodeFromMessage("$partial\n```") != null)
    }

    // MARK: choosing the right block

    @Test
    fun `longest block wins when a snippet precedes the scene`() {
        // Models often show a tiny example first. Running that instead of the
        // scene underneath it was the old behaviour.
        val content = """
            First the shape:

            ```javascript
            const size = 1;
            ```

            Now the scene:

            ```javascript
            function init() {
              nova64.scene.createCube(1, 0xff0000, [0, 0, 0]);
              nova64.camera.setCameraPosition(0, 5, 11);
              nova64.light.setAmbientLight(0x404060, 0.8);
            }
            ```
        """.trimIndent()

        val code = extractCodeFromMessage(content)!!
        assertTrue("should pick the scene", code.contains("setAmbientLight"))
        assertFalse("should not pick the snippet", code.trim() == "const size = 1;")
    }

    @Test
    fun `language tag is stripped from the extracted code`() {
        val code = extractCodeFromMessage(scene)!!
        assertFalse(code.startsWith("javascript"))
        assertTrue(code.startsWith("function init()"))
    }

    @Test
    fun `fence with no language tag still extracts`() {
        val content = "```\nfunction init() { nova64.scene.createCube(1); }\n```"
        val code = extractCodeFromMessage(content)!!
        assertTrue(code.startsWith("function init()"))
    }

    @Test
    fun `very short block is not runnable`() {
        assertNull(extractCodeFromMessage("```js\nx = 1\n```"))
    }

    @Test
    fun `trailing artifacts are stripped`() {
        val content = "```javascript\nfunction init() { nova64.scene.createCube(1); }\n[RUN_SCENE]\n```"
        val code = extractCodeFromMessage(content)!!
        assertFalse(code.contains("[RUN_SCENE]"))
        assertTrue(code.endsWith("}"))
    }

    @Test
    fun `message with no fences at all yields null`() {
        assertNull(extractCodeFromMessage("No code here, just an explanation of how meshes work."))
    }

    @Test
    fun `empty content yields null`() {
        assertNull(extractCodeFromMessage(""))
    }

    // MARK: the click payload

    @Test
    fun `extracted code is what runs, never the surrounding prose`() {
        val content = "Here is the scene you asked for:\n\n$scene\n\nTweak the colour if you like."
        val code = extractCodeFromMessage(content)!!
        assertFalse(code.contains("Tweak the colour"))
        assertFalse(code.contains("Here is the scene"))
        assertEquals(code, code.trim())
    }
}
