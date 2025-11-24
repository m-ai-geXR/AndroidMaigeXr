package com.xraiassistant.data.models

import android.util.Base64
import java.util.*

/**
 * Message Content Types for Multimodal AI
 *
 * Equivalent to iOS AIMessageContentType
 * Supports text and image content in a single message
 */
sealed class AIMessageContentType {
    data class Text(val text: String) : AIMessageContentType()
    data class Image(val imageContent: AIImageContent) : AIMessageContentType()
}

/**
 * AI Image Content - Represents an image attachment
 *
 * Equivalent to iOS AIImageContent
 */
data class AIImageContent(
    val data: ByteArray,
    val mimeType: String,  // "image/jpeg", "image/png", "image/webp"
    val filename: String? = null
) {
    val base64String: String
        get() = Base64.encodeToString(data, Base64.NO_WRAP)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AIImageContent

        if (!data.contentEquals(other.data)) return false
        if (mimeType != other.mimeType) return false
        if (filename != other.filename) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + (filename?.hashCode() ?: 0)
        return result
    }
}

/**
 * Universal Message Format for AI Providers
 *
 * Equivalent to iOS AIMessage
 * Supports multimodal content (text + images)
 */
data class AIMessage(
    val role: AIMessageRole,
    val content: List<AIMessageContentType>,
    val timestamp: Date = Date(),
    val id: String = UUID.randomUUID().toString()
) {
    companion object {
        // Convenience constructor for text-only messages
        fun text(role: AIMessageRole, text: String): AIMessage {
            return AIMessage(
                role = role,
                content = listOf(AIMessageContentType.Text(text))
            )
        }

        // Convenience constructor for messages with images
        fun withImages(
            role: AIMessageRole,
            text: String,
            images: List<AIImageContent>
        ): AIMessage {
            val content = mutableListOf<AIMessageContentType>(
                AIMessageContentType.Text(text)
            )
            content.addAll(images.map { AIMessageContentType.Image(it) })

            return AIMessage(
                role = role,
                content = content
            )
        }
    }

    // Helper to get text-only content
    val textContent: String
        get() = content
            .filterIsInstance<AIMessageContentType.Text>()
            .joinToString("\n") { it.text }

    // Check if message has images
    val hasImages: Boolean
        get() = content.any { it is AIMessageContentType.Image }

    // Get all images from message
    val images: List<AIImageContent>
        get() = content
            .filterIsInstance<AIMessageContentType.Image>()
            .map { it.imageContent }
}

/**
 * Message Role - System, User, or Assistant
 *
 * Equivalent to iOS AIMessageRole
 */
enum class AIMessageRole {
    SYSTEM,
    USER,
    ASSISTANT
}

/**
 * Provider Capabilities - Defines what each AI provider supports
 *
 * Equivalent to iOS AIProviderCapabilities
 */
data class AIProviderCapabilities(
    val supportsVision: Boolean,
    val supportsStreaming: Boolean,
    val supportedImageFormats: List<String>,
    val maxImageSize: Int,  // in bytes
    val maxImagesPerMessage: Int,
    val maxTokens: Int
) {
    companion object {
        // Default capabilities for non-vision providers
        val DEFAULT = AIProviderCapabilities(
            supportsVision = false,
            supportsStreaming = true,
            supportedImageFormats = emptyList(),
            maxImageSize = 0,
            maxImagesPerMessage = 0,
            maxTokens = 4096
        )

        // OpenAI GPT-4 Vision capabilities
        val OPENAI_VISION = AIProviderCapabilities(
            supportsVision = true,
            supportsStreaming = true,
            supportedImageFormats = listOf("image/jpeg", "image/png", "image/webp", "image/gif"),
            maxImageSize = 20 * 1024 * 1024,  // 20MB
            maxImagesPerMessage = 10,
            maxTokens = 128000
        )

        // Anthropic Claude Vision capabilities
        val ANTHROPIC_VISION = AIProviderCapabilities(
            supportsVision = true,
            supportsStreaming = true,
            supportedImageFormats = listOf("image/jpeg", "image/png", "image/webp", "image/gif"),
            maxImageSize = 5 * 1024 * 1024,  // 5MB
            maxImagesPerMessage = 20,
            maxTokens = 200000
        )

        // Google Gemini Vision capabilities
        val GEMINI_VISION = AIProviderCapabilities(
            supportsVision = true,
            supportsStreaming = true,
            supportedImageFormats = listOf("image/jpeg", "image/png", "image/webp"),
            maxImageSize = 20 * 1024 * 1024,  // 20MB
            maxImagesPerMessage = 16,
            maxTokens = 1048576  // 1M tokens
        )

        // Together.ai Llama Vision capabilities
        val TOGETHER_VISION = AIProviderCapabilities(
            supportsVision = true,
            supportsStreaming = true,
            supportedImageFormats = listOf("image/jpeg", "image/png", "image/webp"),
            maxImageSize = 10 * 1024 * 1024,  // 10MB
            maxImagesPerMessage = 5,
            maxTokens = 8192
        )
    }
}
