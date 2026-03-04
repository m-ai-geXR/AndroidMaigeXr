package com.xraiassistant.data.remote

import android.util.Log
import com.squareup.moshi.Moshi
import com.xraiassistant.data.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real AI Provider Service
 *
 * Implements actual HTTP calls to Together.ai, OpenAI, and Anthropic APIs
 * with streaming support for real-time responses.
 *
 * Includes retry logic with exponential backoff for transient errors.
 */
@Singleton
class RealAIProviderService @Inject constructor(
    private val togetherAIService: TogetherAIService,
    private val openAIService: OpenAIService,
    private val anthropicService: AnthropicService,
    private val geminiService: GeminiService,
    private val xaiService: XAIService,
    private val moshi: Moshi
) {

    companion object {
        private const val TAG = "RealAIProviderService"
        private const val MAX_RETRIES = 3
        private const val INITIAL_RETRY_DELAY_MS = 1000L // 1 second
        private const val MAX_RETRY_DELAY_MS = 30000L // 30 seconds
    }

    /**
     * Generate AI response with streaming
     *
     * Routes to appropriate provider based on provider name:
     * - "Together.ai" → Together.ai API
     * - "OpenAI" → OpenAI API
     * - "Anthropic" → Anthropic API
     * - "Google AI" → Google Gemini API
     *
     * Supports multimodal input with images for vision-capable models.
     *
     * @return Flow<String> emitting response chunks in real-time
     */
    suspend fun generateResponseStream(
        provider: String,
        apiKey: String,
        model: String,
        prompt: String,
        systemPrompt: String,
        temperature: Double,
        topP: Double,
        images: List<AIImageContent> = emptyList()
    ): Flow<String> = flow {
        Log.d(TAG, "🚀 Generating streaming response")
        Log.d(TAG, "Provider: $provider")
        Log.d(TAG, "Model: $model")
        Log.d(TAG, "Temperature: $temperature, Top-P: $topP")
        if (images.isNotEmpty()) {
            Log.d(TAG, "📷 Images: ${images.size} attached")
        }

        when (provider) {
            "Together.ai" -> {
                streamTogetherAI(apiKey, model, prompt, systemPrompt, temperature, topP, images)
                    .collect { chunk -> emit(chunk) }
            }
            "OpenAI" -> {
                streamOpenAI(apiKey, model, prompt, systemPrompt, temperature, topP, images)
                    .collect { chunk -> emit(chunk) }
            }
            "Anthropic" -> {
                streamAnthropic(apiKey, model, prompt, systemPrompt, temperature, topP, images)
                    .collect { chunk -> emit(chunk) }
            }
            "Google AI" -> {
                streamGemini(apiKey, model, prompt, systemPrompt, temperature, topP, images)
                    .collect { chunk -> emit(chunk) }
            }
            "xAI" -> {
                streamXAI(apiKey, model, prompt, systemPrompt, temperature, topP, images)
                    .collect { chunk -> emit(chunk) }
            }
            else -> {
                Log.e(TAG, "❌ Unknown provider: $provider")
                throw IllegalArgumentException("Unknown provider: $provider")
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Stream response from Together.ai with automatic retry logic
     * Supports multimodal input with images for vision models
     */
    private suspend fun streamTogetherAI(
        apiKey: String,
        model: String,
        prompt: String,
        systemPrompt: String,
        temperature: Double,
        topP: Double,
        images: List<AIImageContent> = emptyList()
    ): Flow<String> = flow {
        val messages = buildList<APIChatMessage> {
            if (systemPrompt.isNotEmpty()) {
                add(APIChatMessage(role = "system", content = systemPrompt))
            }

            // For now, only text is supported (vision API requires different structure)
            // TODO: Implement multimodal content when vision models are confirmed supported
            add(APIChatMessage(role = "user", content = prompt))

            if (images.isNotEmpty()) {
                Log.w(TAG, "⚠️ Together.ai vision support not yet implemented, ignoring ${images.size} images")
            }
        }

        val request = TogetherAIRequest(
            model = model,
            messages = messages,
            temperature = temperature,
            topP = topP,
            stream = true,
            maxTokens = 8192
        )

        var lastException: Exception? = null
        var retryCount = 0

        while (retryCount <= MAX_RETRIES) {
            try {
                Log.d(TAG, "📡 Calling Together.ai API... (attempt ${retryCount + 1}/${MAX_RETRIES + 1})")

                val response = togetherAIService.chatCompletion(
                    authorization = "Bearer $apiKey",
                    request = request
                )

                // Handle successful response
                if (response.isSuccessful) {
                    Log.d(TAG, "✅ Together.ai API response received, streaming chunks...")
                    parseServerSentEvents(response.body()!!).collect { chunk ->
                        emit(chunk)
                    }
                    return@flow // Success - exit retry loop
                }

                // Handle error responses
                val errorBody = response.errorBody()?.string()
                val errorCode = response.code()

                Log.e(TAG, "❌ Together.ai API error: $errorCode - $errorBody")

                // Check if this is a retryable error
                if (isRetryableError(errorCode) && retryCount < MAX_RETRIES) {
                    val retryAfterSeconds = response.headers()["retry-after"]?.toIntOrNull() ?: 0
                    val delayMs = calculateRetryDelay(retryCount, retryAfterSeconds)

                    Log.w(TAG, "⏳ Service temporarily unavailable. Retrying in ${delayMs}ms...")
                    lastException = Exception("Together.ai API error $errorCode: Service temporarily unavailable")

                    delay(delayMs)
                    retryCount++
                    continue
                }

                // Non-retryable error or max retries reached
                throw Exception("Together.ai API error: $errorCode - $errorBody")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error calling Together.ai API (attempt ${retryCount + 1})", e)

                // Check if this is a network error that should be retried
                val isRetryableNetworkError = when (e) {
                    is java.net.SocketTimeoutException,
                    is java.net.SocketException,
                    is java.io.IOException -> retryCount < MAX_RETRIES
                    else -> false
                }

                if (isRetryableNetworkError) {
                    val delayMs = calculateRetryDelay(retryCount, 0)
                    Log.w(TAG, "⏳ Network error. Retrying in ${delayMs}ms...")
                    lastException = e
                    delay(delayMs)
                    retryCount++
                    continue
                }

                // Provide user-friendly error message for non-retryable errors
                when (e) {
                    is javax.net.ssl.SSLHandshakeException -> {
                        throw Exception("SSL connection failed. This may be due to emulator certificate issues. Error: ${e.message}")
                    }
                    is java.net.UnknownHostException -> {
                        throw Exception("Cannot reach api.together.xyz. Please check your internet connection.")
                    }
                    else -> throw Exception("Network error: ${e.message}")
                }
            }
        }

        // Max retries exceeded
        throw Exception("Together.ai service unavailable after $MAX_RETRIES retries. Please try again later.", lastException)
    }.flowOn(Dispatchers.IO)

    /**
     * Check if an HTTP error code is retryable
     */
    private fun isRetryableError(code: Int): Boolean {
        return code == 503 || // Service Unavailable
               code == 429 || // Too Many Requests
               code == 408 || // Request Timeout
               code >= 500    // Server errors
    }

    /**
     * Calculate retry delay using exponential backoff
     *
     * @param retryCount Current retry attempt (0-based)
     * @param retryAfterSeconds Server-provided retry-after value (0 if not provided)
     * @return Delay in milliseconds
     */
    private fun calculateRetryDelay(retryCount: Int, retryAfterSeconds: Int): Long {
        // If server provides retry-after, use it
        if (retryAfterSeconds > 0) {
            return (retryAfterSeconds * 1000L).coerceAtMost(MAX_RETRY_DELAY_MS)
        }

        // Otherwise use exponential backoff: 1s, 2s, 4s, 8s, ...
        val exponentialDelay = INITIAL_RETRY_DELAY_MS * (1L shl retryCount)
        return exponentialDelay.coerceAtMost(MAX_RETRY_DELAY_MS)
    }

    /**
     * Stream response from OpenAI
     */
    private suspend fun streamOpenAI(
        apiKey: String,
        model: String,
        prompt: String,
        systemPrompt: String,
        temperature: Double,
        topP: Double,
        images: List<AIImageContent> = emptyList()
    ): Flow<String> = flow {
        Log.d(TAG, "📡 Calling OpenAI API...")
        Log.d(TAG, "   Model: $model")
        Log.d(TAG, "   Prompt length: ${prompt.length} chars")
        if (images.isNotEmpty()) {
            Log.d(TAG, "📷 OpenAI: ${images.size} images attached")
            images.forEachIndexed { index, img ->
                Log.d(TAG, "   Image $index: ${img.mimeType}, ${img.data.size / 1024}KB")
            }
        }

        // Build OpenAI messages with multimodal support (matching iOS OpenAIProvider.swift lines 138-172)
        val userMessageContent: Any = if (images.isEmpty()) {
            // Simple text-only message
            Log.d(TAG, "   Content type: text-only")
            prompt
        } else {
            // Multimodal message (text + images)
            Log.d(TAG, "   Content type: multimodal (text + ${images.size} images)")
            buildList<Map<String, Any>> {
                add(mapOf("type" to "text", "text" to prompt))
                images.forEachIndexed { index, imageContent ->
                    val base64Length = imageContent.base64String.length
                    Log.d(TAG, "   Adding image $index: base64 length = $base64Length chars")
                    add(mapOf(
                        "type" to "image_url",
                        "image_url" to mapOf(
                            "url" to "data:${imageContent.mimeType};base64,${imageContent.base64String}"
                        )
                    ))
                }
            }
        }

        val messages = buildList<APIChatMessage> {
            if (systemPrompt.isNotEmpty()) {
                add(APIChatMessage(role = "system", content = systemPrompt))
            }
            add(APIChatMessage(role = "user", content = userMessageContent))
        }

        Log.d(TAG, "   Total messages: ${messages.size}")

        val request = OpenAIRequest(
            model = model,
            messages = messages,
            temperature = temperature,
            topP = topP,
            stream = true,
            maxTokens = 8192
        )

        // Log the serialized request for debugging (only first 500 chars to avoid huge logs)
        try {
            val requestJson = moshi.adapter(OpenAIRequest::class.java).toJson(request)
            Log.d(TAG, "   Request JSON (preview): ${requestJson.take(500)}...")
        } catch (e: Exception) {
            Log.w(TAG, "   Could not serialize request for logging: ${e.message}")
        }

        val response = openAIService.chatCompletion(
            authorization = "Bearer $apiKey",
            request = request
        )

        if (!response.isSuccessful) {
            val errorBody = response.errorBody()?.string()
            Log.e(TAG, "❌ OpenAI API error: ${response.code()} - $errorBody")
            throw Exception("OpenAI API error: ${response.code()} - $errorBody")
        }

        Log.d(TAG, "✅ OpenAI API response received, streaming chunks...")
        parseServerSentEvents(response.body()!!).collect { chunk ->
            emit(chunk)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Stream response from xAI (Grok)
     *
     * xAI uses an OpenAI-compatible API — same request format and SSE streaming.
     * Reuses OpenAIRequest and parseServerSentEvents().
     */
    private suspend fun streamXAI(
        apiKey: String,
        model: String,
        prompt: String,
        systemPrompt: String,
        temperature: Double,
        topP: Double,
        images: List<AIImageContent> = emptyList()
    ): Flow<String> = flow {
        Log.d(TAG, "📡 Calling xAI (Grok) API...")
        Log.d(TAG, "   Model: $model")
        Log.d(TAG, "   Prompt length: ${prompt.length} chars")

        val userMessageContent: Any = if (images.isEmpty()) {
            prompt
        } else {
            // xAI (Grok 4) supports vision - same format as OpenAI
            Log.d(TAG, "📷 xAI: ${images.size} images attached")
            buildList<Map<String, Any>> {
                add(mapOf("type" to "text", "text" to prompt))
                images.forEachIndexed { index, imageContent ->
                    Log.d(TAG, "   Adding image $index: ${imageContent.mimeType}")
                    add(mapOf(
                        "type" to "image_url",
                        "image_url" to mapOf(
                            "url" to "data:${imageContent.mimeType};base64,${imageContent.base64String}"
                        )
                    ))
                }
            }
        }

        val messages = buildList<APIChatMessage> {
            if (systemPrompt.isNotEmpty()) {
                add(APIChatMessage(role = "system", content = systemPrompt))
            }
            add(APIChatMessage(role = "user", content = userMessageContent))
        }

        val request = OpenAIRequest(
            model = model,
            messages = messages,
            temperature = temperature,
            topP = topP,
            stream = true,
            maxTokens = 8192
        )

        val response = xaiService.chatCompletion(
            authorization = "Bearer $apiKey",
            request = request
        )

        if (!response.isSuccessful) {
            val errorBody = response.errorBody()?.string()
            Log.e(TAG, "❌ xAI API error: ${response.code()} - $errorBody")
            throw Exception("xAI API error: ${response.code()} - $errorBody")
        }

        Log.d(TAG, "✅ xAI API response received, streaming chunks...")
        parseServerSentEvents(response.body()!!).collect { chunk ->
            emit(chunk)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Stream response from Anthropic
     *
     * NOTE: Claude 4.5+ models only accept temperature OR top_p, not both.
     * We use temperature only (top_p is ignored for Anthropic).
     * Supports multimodal input with images for vision models.
     */
    private suspend fun streamAnthropic(
        apiKey: String,
        model: String,
        prompt: String,
        systemPrompt: String,
        temperature: Double,
        topP: Double,  // Ignored for Anthropic - Claude 4.5+ only accepts temperature
        images: List<AIImageContent> = emptyList()
    ): Flow<String> = flow {
        Log.d(TAG, "📡 Calling Anthropic API...")
        Log.d(TAG, "   Model: $model")
        Log.d(TAG, "   Temperature: $temperature (top_p ignored for Claude 4.5+)")
        if (images.isNotEmpty()) {
            Log.d(TAG, "   📷 Images: ${images.size} attached")
        }

        // Build Anthropic message with multimodal support (matching iOS AnthropicProvider.swift lines 250-286)
        val messageContent: Any = if (images.isEmpty()) {
            // Simple text-only message
            prompt
        } else {
            // Multimodal message (text + images)
            buildList<Map<String, Any>> {
                add(mapOf("type" to "text", "text" to prompt))
                images.forEach { imageContent ->
                    add(mapOf(
                        "type" to "image",
                        "source" to mapOf(
                            "type" to "base64",
                            "media_type" to imageContent.mimeType,
                            "data" to imageContent.base64String
                        )
                    ))
                }
            }
        }

        val messages = listOf(
            APIChatMessage(role = "user", content = messageContent)
        )

        val request = AnthropicRequest(
            model = model,
            messages = messages,
            temperature = temperature,
            topP = null,  // CRITICAL: Claude 4.5+ doesn't allow both temperature and top_p
            stream = true,
            maxTokens = 8192,
            system = systemPrompt.takeIf { it.isNotEmpty() }
        )

        val response = anthropicService.messages(
            apiKey = apiKey,
            request = request
        )

        if (!response.isSuccessful) {
            val errorBody = response.errorBody()?.string()
            Log.e(TAG, "❌ Anthropic API error: ${response.code()} - $errorBody")
            throw Exception("Anthropic API error: ${response.code()} - $errorBody")
        }

        Log.d(TAG, "✅ Anthropic API response received, streaming chunks...")
        parseAnthropicServerSentEvents(response.body()!!).collect { chunk ->
            emit(chunk)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Parse Server-Sent Events (SSE) from Together.ai and OpenAI
     *
     * Format:
     * ```
     * data: {"choices":[{"delta":{"content":"Hello"}}]}
     * data: {"choices":[{"delta":{"content":" world"}}]}
     * data: [DONE]
     * ```
     */
    private suspend fun parseServerSentEvents(responseBody: ResponseBody): Flow<String> = flow {
        val source = responseBody.source()
        val adapter = moshi.adapter(TogetherAIResponse::class.java)

        try {
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break

                // SSE format: "data: {json}" or "data: [DONE]"
                if (line.startsWith("data: ")) {
                    val jsonData = line.substring(6).trim()

                    // Check for end marker
                    if (jsonData == "[DONE]") {
                        Log.d(TAG, "🏁 Stream complete")
                        break
                    }

                    // Parse JSON chunk - skip errors but keep emitting
                    val chunk = try {
                        adapter.fromJson(jsonData)
                    } catch (e: Exception) {
                        Log.w(TAG, "⚠️ Failed to parse chunk: $jsonData", e)
                        null
                    }

                    val content = chunk?.choices?.firstOrNull()?.delta?.content
                    if (!content.isNullOrEmpty()) {
                        emit(content)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error reading stream", e)
            throw e
        } finally {
            responseBody.close()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Parse Server-Sent Events from Anthropic
     *
     * Anthropic uses slightly different format:
     * ```
     * data: {"type":"content_block_start",...}
     * data: {"type":"content_block_delta","delta":{"type":"text_delta","text":"Hello"}}
     * data: {"type":"content_block_delta","delta":{"type":"text_delta","text":" world"}}
     * data: {"type":"message_stop"}
     * ```
     */
    private suspend fun parseAnthropicServerSentEvents(responseBody: ResponseBody): Flow<String> = flow {
        val source = responseBody.source()
        val adapter = moshi.adapter(AnthropicResponse::class.java)

        try {
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break

                if (line.startsWith("data: ")) {
                    val jsonData = line.substring(6).trim()

                    // Parse JSON chunk - skip errors but keep emitting
                    val chunk = try {
                        adapter.fromJson(jsonData)
                    } catch (e: Exception) {
                        Log.w(TAG, "⚠️ Failed to parse Anthropic chunk: $jsonData", e)
                        null
                    }

                    // Check for text delta
                    if (chunk?.type == "content_block_delta") {
                        val text = chunk.delta?.text
                        if (!text.isNullOrEmpty()) {
                            emit(text)
                        }
                    }

                    // Check for completion
                    if (chunk?.type == "message_stop") {
                        Log.d(TAG, "🏁 Anthropic stream complete")
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error reading Anthropic stream", e)
            throw e
        } finally {
            responseBody.close()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Stream response from Google Gemini with automatic retry logic
     * Supports multimodal input with images for vision models.
     */
    private suspend fun streamGemini(
        apiKey: String,
        model: String,
        prompt: String,
        systemPrompt: String,
        temperature: Double,
        topP: Double,
        images: List<AIImageContent> = emptyList()
    ): Flow<String> = flow {
        // Build the Gemini request with multimodal support (matching iOS implementation)
        val contentParts = mutableListOf<GeminiRequest.Part>()

        // Add text part
        contentParts.add(GeminiRequest.Part(text = prompt))

        // Add image parts if present (matching iOS GoogleAIProvider.swift lines 190-195)
        if (images.isNotEmpty()) {
            Log.d(TAG, "📷 Gemini: ${images.size} images attached")
            images.forEach { imageContent ->
                contentParts.add(
                    GeminiRequest.Part(
                        inlineData = GeminiRequest.InlineData(
                            mimeType = imageContent.mimeType,
                            data = imageContent.base64String
                        )
                    )
                )
            }
        }

        val content = GeminiRequest.GeminiContent(parts = contentParts, role = "user")

        val systemInstruction = if (systemPrompt.isNotEmpty()) {
            GeminiRequest.GeminiContent(
                parts = listOf(GeminiRequest.Part(text = systemPrompt)),
                role = null
            )
        } else null

        val generationConfig = GeminiRequest.GenerationConfig(
            temperature = temperature,
            topP = topP,
            maxOutputTokens = 65536
        )

        val request = GeminiRequest(
            contents = listOf(content),
            generationConfig = generationConfig,
            systemInstruction = systemInstruction
        )

        var lastException: Exception? = null
        var retryCount = 0

        while (retryCount <= MAX_RETRIES) {
            try {
                Log.d(TAG, "📡 Calling Google Gemini API... (attempt ${retryCount + 1}/${MAX_RETRIES + 1})")

                val response = geminiService.streamGenerateContent(
                    model = model,
                    apiKey = apiKey,
                    request = request
                )

                // Handle successful response
                if (response.isSuccessful) {
                    Log.d(TAG, "✅ Gemini API response received, streaming chunks...")
                    parseGeminiServerSentEvents(response.body()!!).collect { chunk ->
                        emit(chunk)
                    }
                    return@flow // Success - exit retry loop
                }

                // Handle error responses
                val errorBody = response.errorBody()?.string()
                val errorCode = response.code()

                Log.e(TAG, "❌ Gemini API error: $errorCode - $errorBody")

                // Handle quota/rate limit errors (429) with user-friendly messages
                if (errorCode == 429) {
                    val errorMessage = when {
                        errorBody?.contains("quota", ignoreCase = true) == true ->
                            "Google API quota exceeded. Please check your API usage at https://ai.dev/usage or try a different Gemini model (e.g., Gemini 1.5 Flash or Gemini 1.5 Pro)."
                        errorBody?.contains("rate limit", ignoreCase = true) == true ->
                            "Rate limit exceeded. Please wait a few moments and try again, or select a different model."
                        else ->
                            "Too many requests. Please wait and try again with a different model."
                    }
                    throw Exception(errorMessage)
                }

                // Check if this is a retryable error
                if (isRetryableError(errorCode) && retryCount < MAX_RETRIES) {
                    val retryAfterSeconds = response.headers()["retry-after"]?.toIntOrNull() ?: 0
                    val delayMs = calculateRetryDelay(retryCount, retryAfterSeconds)

                    Log.w(TAG, "⏳ Service temporarily unavailable. Retrying in ${delayMs}ms...")
                    lastException = Exception("Gemini API error $errorCode: Service temporarily unavailable")

                    delay(delayMs)
                    retryCount++
                    continue
                }

                // Non-retryable error or max retries reached
                throw Exception("Gemini API error: $errorCode - $errorBody")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error calling Gemini API (attempt ${retryCount + 1})", e)

                // Check if this is a network error that should be retried
                val isRetryableNetworkError = when (e) {
                    is java.net.SocketTimeoutException,
                    is java.net.SocketException,
                    is java.io.IOException -> retryCount < MAX_RETRIES
                    else -> false
                }

                if (isRetryableNetworkError) {
                    val delayMs = calculateRetryDelay(retryCount, 0)
                    Log.w(TAG, "⏳ Network error. Retrying in ${delayMs}ms...")
                    lastException = e
                    delay(delayMs)
                    retryCount++
                    continue
                }

                // Provide user-friendly error message for non-retryable errors
                when (e) {
                    is javax.net.ssl.SSLHandshakeException -> {
                        throw Exception("SSL connection failed. This may be due to emulator certificate issues. Error: ${e.message}")
                    }
                    is java.net.UnknownHostException -> {
                        throw Exception("Cannot reach generativelanguage.googleapis.com. Please check your internet connection.")
                    }
                    else -> throw Exception("Network error: ${e.message}")
                }
            }
        }

        // Max retries exceeded
        throw Exception("Gemini service unavailable after $MAX_RETRIES retries. Please try again later.", lastException)
    }.flowOn(Dispatchers.IO)

    /**
     * Parse Gemini streaming response
     *
     * CRITICAL: Gemini returns the ENTIRE response as ONE JSON array: [{...}, {...}, ...]
     * This matches the iOS implementation approach (GoogleAIProvider.swift lines 129-159)
     *
     * iOS approach:
     * 1. Read ALL bytes into fullResponse string
     * 2. Parse each SSE line as it arrives
     * 3. Extract text from each GeminiResponse chunk
     *
     * Using alt=sse for true line-by-line streaming (avoids blocking body.string() on long responses).
     */
    private fun parseGeminiServerSentEvents(body: ResponseBody): Flow<String> = flow {
        Log.d(TAG, "✅ Gemini SSE stream started, parsing line by line...")

        val source = body.source()
        val adapter = moshi.adapter(GeminiResponse::class.java)

        try {
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break

                // SSE format: "data: {json}"
                if (!line.startsWith("data: ")) continue
                val jsonData = line.substring(6).trim()
                if (jsonData.isEmpty()) continue

                val chunk = try {
                    adapter.fromJson(jsonData)
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Failed to parse Gemini chunk: ${jsonData.take(100)}")
                    null
                }

                chunk?.candidates?.firstOrNull()?.content?.parts?.forEach { part ->
                    val text = part.text
                    if (!text.isNullOrEmpty()) {
                        emit(text)
                    }
                }
            }
            Log.d(TAG, "🏁 Gemini stream complete")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Gemini parsing error: ${e.message}")
            throw e
        } finally {
            body.close()
        }
    }

    /**
     * Non-streaming version (collects all chunks into a single string)
     *
     * This is the interface used by the existing AIProviderService.kt stub.
     * We implement it by collecting the streaming response.
     */
    suspend fun generateResponse(
        provider: String,
        apiKey: String,
        model: String,
        prompt: String,
        systemPrompt: String,
        temperature: Double,
        topP: Double
    ): String {
        val fullResponse = StringBuilder()

        generateResponseStream(
            provider, apiKey, model, prompt, systemPrompt, temperature, topP
        ).collect { chunk ->
            fullResponse.append(chunk)
        }

        return fullResponse.toString()
    }
}
