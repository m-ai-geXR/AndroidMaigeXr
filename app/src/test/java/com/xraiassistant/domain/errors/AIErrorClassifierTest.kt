package com.xraiassistant.domain.errors

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * The point of these is the guarantees, not the individual mappings: the user
 * always gets a cause and a next step, and never a response body.
 */
class AIErrorClassifierTest {

    // MARK: by status

    @Test
    fun `maps http statuses to categories`() {
        val cases = mapOf(
            401 to AIErrorCategory.INVALID_API_KEY,
            402 to AIErrorCategory.QUOTA_EXCEEDED,
            403 to AIErrorCategory.ACCESS_DENIED,
            404 to AIErrorCategory.MODEL_UNAVAILABLE,
            429 to AIErrorCategory.RATE_LIMITED,
            500 to AIErrorCategory.SERVER_ERROR,
            503 to AIErrorCategory.SERVER_ERROR
        )
        for ((status, expected) in cases) {
            val info = AIErrorClassifier.classify(AIProviderException("Anthropic", status))
            assertEquals("status $status", expected, info.category)
        }
    }

    @Test
    fun `names the provider so the user knows which key to check`() {
        val info = AIErrorClassifier.classify(AIProviderException("Together.ai", 401))
        assertTrue(info.message.contains("Together.ai"))
    }

    @Test
    fun `transient failures are retryable and permanent ones are not`() {
        assertTrue(AIErrorClassifier.classify(AIProviderException("x", 429)).retryable)
        assertTrue(AIErrorClassifier.classify(AIProviderException("x", 503)).retryable)
        assertFalse(AIErrorClassifier.classify(AIProviderException("x", 401)).retryable)
        assertFalse(AIErrorClassifier.classify(AIProviderException("x", 402)).retryable)
    }

    // MARK: by message

    @Test
    fun `detects a missing key however it is spelled`() {
        for (text in listOf(
            "API key not configured",
            "apiKey is required",
            "api_key still set to changeMe"
        )) {
            assertEquals(text, AIErrorCategory.MISSING_API_KEY,
                AIErrorClassifier.classify(IllegalStateException(text)).category)
        }
    }

    @Test
    fun `detects real android network failures`() {
        assertEquals(AIErrorCategory.OFFLINE,
            AIErrorClassifier.classify(UnknownHostException("Unable to resolve host \"api.together.xyz\"")).category)
        assertEquals(AIErrorCategory.OFFLINE,
            AIErrorClassifier.classify(IOException("Failed to connect to api.openai.com")).category)
    }

    @Test
    fun `detects a socket timeout`() {
        assertEquals(AIErrorCategory.TIMEOUT,
            AIErrorClassifier.classify(SocketTimeoutException("timeout")).category)
    }

    @Test
    fun `detects a context overflow`() {
        assertEquals(AIErrorCategory.CONTEXT_TOO_LONG,
            AIErrorClassifier.classify(Exception("maximum context length is 8192 tokens")).category)
    }

    @Test
    fun `reads a status embedded in older prose throws`() {
        val info = AIErrorClassifier.classify(Exception("HTTP 429 Too Many Requests"))
        assertEquals(AIErrorCategory.RATE_LIMITED, info.category)
    }

    // MARK: the guarantees

    @Test
    fun `never leaks a provider response body`() {
        val body = """{"error":{"message":"invalid x-api-key","type":"authentication_error"}}"""
        val info = AIErrorClassifier.classify(AIProviderException("Anthropic", 401, body))

        assertFalse(info.message.contains("x-api-key"))
        assertFalse(info.message.contains("{"))
        assertFalse(info.asMessage().contains(body))
    }

    @Test
    fun `does not show a bare status code`() {
        val info = AIErrorClassifier.classify(AIProviderException("OpenAI", 401))
        assertFalse(info.message.contains("401"))
    }

    @Test
    fun `always gives the user something to do`() {
        val errors = listOf<Throwable?>(
            AIProviderException("x", 401),
            AIProviderException("x", 429),
            AIProviderException("x", 500),
            UnknownHostException("Unable to resolve host"),
            Exception("something nobody anticipated"),
            Exception(),
            null
        )
        for (e in errors) {
            val info = AIErrorClassifier.classify(e)
            assertTrue("title for $e", info.title.isNotBlank())
            assertTrue("message for $e", info.message.isNotBlank())
            assertTrue("action for $e", info.action.isNotBlank())
        }
    }

    @Test
    fun `unrecognised failures still produce a usable message`() {
        val info = AIErrorClassifier.classify(Exception("¯\\_(ツ)_/¯"))
        assertEquals(AIErrorCategory.UNKNOWN, info.category)
        assertTrue(info.action.contains("Settings"))
    }

    @Test
    fun `null throwable does not crash`() {
        val info = AIErrorClassifier.classify(null)
        assertEquals(AIErrorCategory.UNKNOWN, info.category)
    }

    // MARK: formatting

    @Test
    fun `line form is single line and message form carries all three parts`() {
        val info = AIErrorClassifier.classify(AIProviderException("OpenAI", 429))
        assertFalse(info.asLine().contains("\n"))

        val body = info.asMessage()
        assertTrue(body.contains(info.title))
        assertTrue(body.contains(info.message))
        assertTrue(body.contains(info.action))
    }
}
