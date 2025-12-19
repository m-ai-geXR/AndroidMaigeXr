package com.xraiassistant.data.repositories

import android.util.Log
import com.xraiassistant.data.local.SettingsDataStore
import com.xraiassistant.data.models.EmbeddingRequest
import com.xraiassistant.data.remote.EmbeddingService
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Embedding Repository
 *
 * Handles generation of vector embeddings using Together AI's m2-bert-80M-8k-retrieval model
 * Provides single and batch embedding generation with rate limiting
 */
@Singleton
class EmbeddingRepository @Inject constructor(
    private val embeddingService: EmbeddingService,
    private val settingsDataStore: SettingsDataStore
) {
    companion object {
        private const val TAG = "EmbeddingRepository"
        private const val EMBEDDING_MODEL = "togethercomputer/m2-bert-80M-8k-retrieval"
        private const val EMBEDDING_DIMENSION = 768
        private const val DEFAULT_BATCH_SIZE = 20
        private const val RATE_LIMIT_DELAY_MS = 100L // 0.1 second between batches
        private const val PROVIDER_TOGETHER_AI = "Together.ai"
        private const val DEFAULT_API_KEY = "changeMe"
    }

    /**
     * Check if RAG is available (Together.ai API key is configured)
     */
    fun isRAGAvailable(): Boolean {
        val apiKey = getAPIKey()
        return apiKey != DEFAULT_API_KEY && apiKey.isNotBlank()
    }

    /**
     * Get Together.ai API key from settings
     */
    private fun getAPIKey(): String {
        return try {
            settingsDataStore.getAPIKeySync(PROVIDER_TOGETHER_AI)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get Together.ai API key: ${e.message}")
            DEFAULT_API_KEY
        }
    }

    /**
     * Generate embedding for a single text
     *
     * @param text Text to embed (up to 8k tokens)
     * @return 768-dimensional embedding vector
     * @throws Exception if API call fails, text is empty, or API key not configured
     */
    suspend fun generateEmbedding(text: String): FloatArray {
        if (text.isBlank()) {
            throw IllegalArgumentException("Cannot generate embedding for empty text")
        }

        // Check if Together.ai API key is configured
        val apiKey = getAPIKey()
        if (apiKey == DEFAULT_API_KEY || apiKey.isBlank()) {
            throw IllegalStateException("Together.ai API key not configured. RAG features require a Together.ai API key.")
        }

        Log.d(TAG, "🧠 Generating embedding for text (${text.length} chars)...")

        val request = EmbeddingRequest(
            model = EMBEDDING_MODEL,
            input = text
        )

        val response = embeddingService.generateEmbedding(
            authorization = "Bearer $apiKey",
            request = request
        )

        if (response.data.isEmpty()) {
            throw IllegalStateException("Empty embedding response from API")
        }

        val embedding = response.data.first().embedding.map { it.toFloat() }.toFloatArray()

        Log.d(TAG, "✅ Generated ${embedding.size}-dimensional embedding")

        return embedding
    }

    /**
     * Generate embeddings for multiple texts in a single batch request
     *
     * @param texts List of texts to embed
     * @return List of 768-dimensional embeddings (same order as input)
     * @throws Exception if API call fails or API key not configured
     */
    suspend fun batchGenerateEmbeddings(texts: List<String>): List<FloatArray> {
        if (texts.isEmpty()) {
            return emptyList()
        }

        // Check if Together.ai API key is configured
        val apiKey = getAPIKey()
        if (apiKey == DEFAULT_API_KEY || apiKey.isBlank()) {
            throw IllegalStateException("Together.ai API key not configured. RAG features require a Together.ai API key.")
        }

        Log.d(TAG, "🧠 Generating batch embeddings for ${texts.size} texts...")

        val request = EmbeddingRequest(
            model = EMBEDDING_MODEL,
            input = texts
        )

        val response = embeddingService.generateEmbedding(
            authorization = "Bearer $apiKey",
            request = request
        )

        val embeddings = response.data
            .sortedBy { it.index } // Ensure correct order
            .map { it.embedding.map { d -> d.toFloat() }.toFloatArray() }

        Log.d(TAG, "✅ Generated ${embeddings.size} embeddings")

        return embeddings
    }

    /**
     * Generate embeddings for large number of texts with chunked processing
     * Automatically splits into smaller batches to respect API limits and rate limiting
     *
     * @param texts List of texts to embed (can be large)
     * @param batchSize Number of texts per batch (default 20)
     * @return List of 768-dimensional embeddings (same order as input)
     * @throws Exception if any batch fails
     */
    suspend fun batchGenerateEmbeddingsChunked(
        texts: List<String>,
        batchSize: Int = DEFAULT_BATCH_SIZE
    ): List<FloatArray> {
        if (texts.isEmpty()) {
            return emptyList()
        }

        Log.d(TAG, "🧠 Generating embeddings for ${texts.size} texts in batches of $batchSize...")

        val allEmbeddings = mutableListOf<FloatArray>()
        val totalBatches = (texts.size + batchSize - 1) / batchSize

        for (i in texts.indices step batchSize) {
            val endIndex = minOf(i + batchSize, texts.size)
            val batch = texts.subList(i, endIndex)
            val batchNumber = (i / batchSize) + 1

            Log.d(TAG, "  📦 Processing batch $batchNumber/$totalBatches...")

            val batchEmbeddings = batchGenerateEmbeddings(batch)
            allEmbeddings.addAll(batchEmbeddings)

            // Add small delay between batches to respect rate limits
            if (endIndex < texts.size) {
                delay(RATE_LIMIT_DELAY_MS)
            }
        }

        Log.d(TAG, "✅ Generated ${allEmbeddings.size} total embeddings")

        return allEmbeddings
    }

    /**
     * Check if a text is suitable for embedding
     * (not too short, not too long)
     *
     * @param text Text to check
     * @return true if text can be embedded
     */
    fun isTextEmbeddable(text: String): Boolean {
        val trimmed = text.trim()
        return trimmed.isNotEmpty() && trimmed.length >= 10 // Minimum 10 chars
    }

    /**
     * Estimate token count for a text (rough approximation)
     * Used to check if text fits within 8k token limit
     *
     * @param text Text to estimate
     * @return Approximate token count
     */
    fun estimateTokenCount(text: String): Int {
        // Rough estimation: 4 characters ≈ 1 token
        return text.length / 4
    }

    /**
     * Truncate text to fit within token limit
     *
     * @param text Text to truncate
     * @param maxTokens Maximum tokens (default 8000 for this model)
     * @return Truncated text
     */
    fun truncateToTokenLimit(text: String, maxTokens: Int = 8000): String {
        val estimatedTokens = estimateTokenCount(text)

        if (estimatedTokens <= maxTokens) {
            return text
        }

        // Truncate by character count (rough approximation)
        val maxChars = maxTokens * 4
        return text.take(maxChars)
    }
}
