package com.xraiassistant.domain.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Reasoning models put their thinking in <think>…</think>; only the answer is shown. */
class ReplyTextTest {
    @Test fun finishedThinkingIsRemoved() {
        val shown = ReplyText.visible("<think>plan the cube</think>\nHere is your cube.")
        assertEquals("Here is your cube.", shown.text)
        assertFalse(shown.isThinking)
    }

    @Test fun unfinishedThinkingShowsThinking() {
        val shown = ReplyText.visible("<think>still planning the")
        assertEquals("", shown.text)
        assertTrue(shown.isThinking)
    }

    @Test fun closingTagWithoutOpeningIsHandled() {
        assertEquals("Answer", ReplyText.visible("planning</think>Answer").text)
    }

    @Test fun plainRepliesAreUntouched() {
        val reply = "Here is code:\n```js\nconst a = 1 < 2;\n```"
        assertEquals(reply, ReplyText.visible(reply).text)
    }
}
