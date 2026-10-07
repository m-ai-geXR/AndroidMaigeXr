package com.xraiassistant.domain.errors

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Guards the fix for replies failing when the app is backgrounded or the device
 * sleeps mid-request: dropped connections must be recognised as interruptions
 * and the request must be sent again exactly once.
 */
class NetworkInterruptionTest {

    @Test
    fun connectionDropsAreInterruptions() {
        assertTrue(NetworkInterruption.isInterruption(SocketException("Software caused connection abort")))
        assertTrue(NetworkInterruption.isInterruption(SocketTimeoutException("timeout")))
        assertTrue(NetworkInterruption.isInterruption(UnknownHostException("api.together.xyz")))
        assertTrue(NetworkInterruption.isInterruption(IOException("unexpected end of stream on https://api.openai.com/")))
    }

    @Test
    fun wrappedConnectionDropIsInterruption() {
        val wrapped = RuntimeException("stream failed", IOException("Connection reset by peer"))
        assertTrue(NetworkInterruption.isInterruption(wrapped))
    }

    @Test
    fun realErrorsAreNotInterruptions() {
        assertFalse(NetworkInterruption.isInterruption(IllegalStateException("401 Unauthorized")))
        assertFalse(NetworkInterruption.isInterruption(IOException("HTTP 400 Bad Request")))
        assertFalse(NetworkInterruption.isInterruption(null))
    }

    @Test
    fun heldRequestRunsOnceOnResume() {
        val queue = InterruptedRequestQueue()
        var runs = 0
        queue.hold { runs++ }
        assertTrue(queue.hasPending)
        queue.resume()
        queue.resume()
        assertEquals(1, runs)
        assertFalse(queue.hasPending)
    }

    @Test
    fun onlyLatestRequestIsKept() {
        val queue = InterruptedRequestQueue()
        val sent = mutableListOf<String>()
        queue.hold { sent += "first" }
        queue.hold { sent += "second" }
        queue.resume()
        assertEquals(listOf("second"), sent)
    }
}
