package com.xraiassistant.domain.local

import java.net.URI

/**
 * The user's own model server: Ollama, LM Studio, llama.cpp, vLLM or any server
 * that speaks the OpenAI chat completions API. The user enters its address and
 * model name in Settings; an API key is optional.
 */
object LocalServerConfig {
    const val PROVIDER = "Local"
    const val MODEL_PREFIX = "local:"

    /**
     * The chat completions endpoint for what the user typed. Accepts a bare host
     * ("192.168.1.20:11434"), a server root ("http://mac.local:1234") or a URL that
     * already ends in /v1 or /v1/chat/completions. Only http and https.
     */
    fun chatCompletionsUrl(input: String): String? {
        var text = input.trim()
        if (text.isEmpty()) return null
        if (!text.contains("://")) text = "http://$text"
        text = text.trimEnd('/')
        text = text.removeSuffix("/chat/completions")
        if (!text.endsWith("/v1")) text += "/v1"
        val url = "$text/chat/completions"
        val uri = runCatching { URI(url) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") return null
        if (uri.host.isNullOrBlank()) return null
        return url
    }

    /** The model name sent to the server, from the app's prefixed model id. */
    fun serverModelName(modelId: String): String = modelId.removePrefix(MODEL_PREFIX)

    /**
     * Hosts allowed over plain HTTP: this device, the emulator's host, mDNS names
     * and private network ranges. Everything else must use HTTPS.
     */
    fun isLocalHost(host: String): Boolean {
        val h = host.lowercase().trim('[', ']')
        if (h == "localhost" || h.endsWith(".local") || h == "10.0.2.2" || h == "::1") return true
        val parts = h.split('.').map { it.toIntOrNull() ?: return false }
        if (parts.size != 4 || parts.any { it !in 0..255 }) return false
        val (a, b) = parts[0] to parts[1]
        return a == 127 || a == 10 || (a == 172 && b in 16..31) || (a == 192 && b == 168) || (a == 169 && b == 254)
    }
}
