package com.xraiassistant.ui.viewmodels

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** A reply with nothing but reasoning must end in a message, not an empty bubble. */
class EmptyReplyTest {

    @Test
    fun reasoningOnlyRepliesAreEmpty() {
        assertTrue(isEmptyReply(""))
        assertTrue(isEmptyReply("<think>planning the island</think>"))
        assertTrue(isEmptyReply("<think>still thinking when the budget ran out"))
        assertTrue(isEmptyReply("   \n"))
    }

    @Test
    fun answersAreNotEmpty() {
        assertFalse(isEmptyReply("<think>plan</think>Here is your scene"))
        assertFalse(isEmptyReply("const cube = 1"))
    }
}
