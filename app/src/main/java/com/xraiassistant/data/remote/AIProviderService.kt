package com.xraiassistant.data.remote

import com.xraiassistant.data.models.AIEffort
import com.xraiassistant.data.models.AIImageContent
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI Provider Service
 *
 * Facade/wrapper for RealAIProviderService that maintains backward compatibility
 * with the existing interface while providing real HTTP calls to AI providers.
 *
 * ✅ UPDATED: Now uses real API calls instead of stub implementation
 *
 * NOTE: Every delegation below uses named arguments. RealAIProviderService has
 * several trailing parameters with defaults (effort, images), so positional
 * delegation silently shifts arguments into the wrong slots whenever a new
 * parameter is inserted. See AIProviderServiceTest for the regression guard.
 */
@Singleton
class AIProviderService @Inject constructor(
    private val realAIProviderService: RealAIProviderService
) {

    /**
     * Generate AI response (non-streaming)
     *
     * Collects all streaming chunks into a single string.
     * For real-time updates, use generateResponseStream() instead.
     */
    suspend fun generateResponse(
        provider: String,
        apiKey: String,
        model: String,
        prompt: String,
        systemPrompt: String,
        temperature: Double,
        topP: Double,
        effort: AIEffort = AIEffort.HIGH
    ): String {
        return realAIProviderService.generateResponse(
            provider = provider,
            apiKey = apiKey,
            model = model,
            prompt = prompt,
            systemPrompt = systemPrompt,
            temperature = temperature,
            topP = topP,
            effort = effort
        )
    }

    /**
     * Generate AI response with streaming
     *
     * Returns a Flow that emits response chunks in real-time.
     * Use this for better UX with long responses.
     *
     * Supports multimodal input with images for vision-capable models.
     *
     * Example:
     * ```kotlin
     * service.generateResponseStream(...).collect { chunk ->
     *     // Update UI with each chunk
     *     println(chunk)
     * }
     * ```
     */
    suspend fun generateResponseStream(
        provider: String,
        apiKey: String,
        model: String,
        prompt: String,
        systemPrompt: String,
        temperature: Double,
        topP: Double,
        effort: AIEffort = AIEffort.HIGH,
        images: List<AIImageContent> = emptyList()
    ): Flow<String> {
        return realAIProviderService.generateResponseStream(
            provider = provider,
            apiKey = apiKey,
            model = model,
            prompt = prompt,
            systemPrompt = systemPrompt,
            temperature = temperature,
            topP = topP,
            effort = effort,
            images = images
        )
    }
}
