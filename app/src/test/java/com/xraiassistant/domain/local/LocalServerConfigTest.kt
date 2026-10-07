package com.xraiassistant.domain.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The Local model server: address handling and the plain-HTTP host rule. */
class LocalServerConfigTest {

    @Test
    fun addressFormsBecomeChatCompletionsUrl() {
        val expected = "http://192.168.1.20:11434/v1/chat/completions"
        listOf(
            "192.168.1.20:11434", "http://192.168.1.20:11434", "http://192.168.1.20:11434/",
            "http://192.168.1.20:11434/v1", "http://192.168.1.20:11434/v1/chat/completions",
            "  http://192.168.1.20:11434/v1/  "
        ).forEach { assertEquals(it, expected, LocalServerConfig.chatCompletionsUrl(it)) }
    }

    @Test
    fun httpsAndHostnamesAreKept() {
        assertEquals("https://llm.example.com/v1/chat/completions", LocalServerConfig.chatCompletionsUrl("https://llm.example.com"))
        assertEquals("http://mac-studio.local:1234/v1/chat/completions", LocalServerConfig.chatCompletionsUrl("mac-studio.local:1234"))
    }

    @Test
    fun badAddressesAreRejected() {
        assertNull(LocalServerConfig.chatCompletionsUrl(""))
        assertNull(LocalServerConfig.chatCompletionsUrl("   "))
        assertNull(LocalServerConfig.chatCompletionsUrl("ftp://server:21"))
        assertNull(LocalServerConfig.chatCompletionsUrl("file:///etc/passwd"))
    }

    @Test
    fun modelPrefixIsStripped() {
        assertEquals("qwen2.5-coder:7b", LocalServerConfig.serverModelName("local:qwen2.5-coder:7b"))
        assertEquals("llama3", LocalServerConfig.serverModelName("llama3"))
    }

    @Test
    fun localAndPrivateHostsMayUsePlainHttp() {
        listOf("localhost", "127.0.0.1", "10.0.2.2", "10.1.2.3", "172.16.0.5", "172.31.255.1",
            "192.168.1.20", "169.254.10.10", "mac-studio.local", "::1").forEach {
            assertTrue(it, LocalServerConfig.isLocalHost(it))
        }
    }

    @Test
    fun publicHostsMustUseHttps() {
        listOf("api.openai.com", "api.together.xyz", "8.8.8.8", "172.32.0.1", "192.169.1.1",
            "evil.local.example.com", "1.2.3", "300.1.1.1").forEach {
            assertFalse(it, LocalServerConfig.isLocalHost(it))
        }
    }
}
