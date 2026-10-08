package com.xraiassistant.data.remote

import com.xraiassistant.data.models.TogetherAIResponse.Delta
import com.xraiassistant.ui.viewmodels.MAX_REPLY_DURATION_MS
import com.xraiassistant.ui.viewmodels.hasRunTooLong
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Together reasoning must count as progress, and no reply may run forever. */
class ReasoningStreamTest {

    @Test
    fun reasoningIsWrappedThenAnswerFollows() {
        val stream = ReasoningStream()
        val out = listOf(
            Delta(role = "assistant", reasoning = "Plan "),
            Delta(reasoning = "the cube"),
            Delta(content = "const cube"),
            Delta(content = " = 1")
        ).mapNotNull { stream.text(it) }.joinToString("") + (stream.finish() ?: "")
        assertEquals("<think>Plan the cube</think>const cube = 1", out)
    }

    @Test
    fun unfinishedThinkingIsClosed() {
        val stream = ReasoningStream()
        stream.text(Delta(reasoning = "hmm"))
        assertEquals("</think>", stream.finish())
        assertNull(stream.finish())
    }

    @Test
    fun repliesAreCappedOverall() {
        assertFalse(hasRunTooLong(0, 60_000))
        assertTrue(hasRunTooLong(0, MAX_REPLY_DURATION_MS + 1))
    }

    @Test
    fun anEmptyReasoningFieldDoesNotHideTheOther() {
        val stream = ReasoningStream()
        assertEquals("<think>thinking", stream.text(Delta(reasoning = "", reasoningContent = "thinking")))
    }
}
