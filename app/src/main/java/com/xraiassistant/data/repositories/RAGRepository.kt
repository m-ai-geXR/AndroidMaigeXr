package com.xraiassistant.data.repositories

import android.util.Log
import com.squareup.moshi.Moshi
import com.xraiassistant.data.local.dao.RAGDao
import com.xraiassistant.data.local.entities.RAGDocumentEntity
import com.xraiassistant.data.local.entities.RAGEmbeddingEntity
import com.xraiassistant.data.models.ChatMessage
import com.xraiassistant.data.models.RAGDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RAG Repository
 *
 * High-level API for RAG (Retrieval-Augmented Generation) system
 * Coordinates embedding generation, document storage, and vector search
 */
@Singleton
class RAGRepository @Inject constructor(
    private val ragDao: RAGDao,
    private val embeddingRepository: EmbeddingRepository,
    private val vectorSearchService: VectorSearchService,
    private val ragContextBuilder: RAGContextBuilder
) {
    companion object {
        private const val TAG = "RAGRepository"
    }

    // Moshi for JSON serialization
    private val moshi = Moshi.Builder().build()

    // Helper to serialize metadata map to JSON
    private fun serializeMetadata(metadata: Map<String, String>): String {
        return try {
            val jsonAdapter = moshi.adapter<Map<*, *>>(Map::class.java)
            jsonAdapter.toJson(metadata) ?: "{}"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to serialize metadata: ${e.message}", e)
            "{}"
        }
    }

    /**
     * Index a single message for RAG search
     * Generates embedding and stores document
     *
     * @param message Chat message to index
     * @param hadImages Whether this message was sent with images (multimodal)
     */
    suspend fun indexMessage(
        message: ChatMessage,
        hadImages: Boolean = false
    ) = withContext(Dispatchers.IO) {
        try {
            // Check if RAG is available (Together.ai API key configured)
            if (!embeddingRepository.isRAGAvailable()) {
                Log.d(TAG, "⚠️ Skipping message - RAG not available (Together.ai API key not configured)")
                return@withContext
            }

            // Skip multimodal messages (messages with images)
            // This prevents token limit issues with base64 image data
            if (hadImages) {
                Log.d(TAG, "⚠️ Skipping multimodal message - was sent with images")
                return@withContext
            }

            // Skip if text is too short
            if (!embeddingRepository.isTextEmbeddable(message.content)) {
                Log.d(TAG, "⚠️ Skipping message - text too short")
                return@withContext
            }

            // Truncate if needed
            val truncatedText = embeddingRepository.truncateToTokenLimit(message.content)

            // Create metadata
            val metadata = buildMap {
                message.libraryId?.let { put("library_id", it) }
                put("timestamp", message.timestamp.toString())
                put("is_user", message.isUser.toString())
            }

            // Create RAG document
            val document = RAGDocumentEntity(
                id = UUID.randomUUID().toString(),
                sourceType = "message",
                sourceId = message.id,
                chunkText = truncatedText,
                chunkIndex = 0,
                metadata = serializeMetadata(metadata)
            )

            // Generate embedding
            val embedding = embeddingRepository.generateEmbedding(truncatedText)

            // Create embedding entity
            val embeddingEntity = RAGEmbeddingEntity(
                id = UUID.randomUUID().toString(),
                documentId = document.id,
                embedding = embedding
            )

            // Save to database
            ragDao.insertDocumentWithEmbedding(document, embeddingEntity)

            Log.d(TAG, "✅ Indexed message: ${message.id.take(8)}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to index message: ${e.message}", e)
        }
    }

    /**
     * Index multiple messages in batch
     * More efficient than indexing one at a time
     *
     * @param messages List of messages to index
     */
    suspend fun indexMessages(messages: List<ChatMessage>) = withContext(Dispatchers.IO) {
        Log.d(TAG, "🔄 Batch indexing ${messages.size} messages...")

        var successCount = 0
        for (message in messages) {
            try {
                indexMessage(message)
                successCount++
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to index message ${message.id}: ${e.message}")
            }
        }

        Log.d(TAG, "✅ Batch indexing complete: $successCount/${messages.size} successful")
    }

    /**
     * Index a conversation with chunking for long conversations
     *
     * @param conversationId Conversation ID
     * @param messages List of messages in the conversation
     */
    suspend fun indexConversation(
        conversationId: String,
        messages: List<ChatMessage>
    ) = withContext(Dispatchers.IO) {
        Log.d(TAG, "🔄 Indexing conversation ${conversationId.take(8)} with ${messages.size} messages...")

        val chunks = createConversationChunks(messages)

        for ((index, chunk) in chunks.withIndex()) {
            try {
                // Create metadata
                val metadata = mapOf(
                    "conversation_id" to conversationId,
                    "chunk_index" to index.toString(),
                    "message_count" to chunk.messageCount.toString()
                )

                // Create RAG document
                val document = RAGDocumentEntity(
                    id = UUID.randomUUID().toString(),
                    sourceType = "conversation",
                    sourceId = conversationId,
                    chunkText = chunk.text,
                    chunkIndex = index,
                    metadata = serializeMetadata(metadata)
                )

                // Generate embedding
                val embedding = embeddingRepository.generateEmbedding(chunk.text)

                // Create embedding entity
                val embeddingEntity = RAGEmbeddingEntity(
                    id = UUID.randomUUID().toString(),
                    documentId = document.id,
                    embedding = embedding
                )

                // Save to database
                ragDao.insertDocumentWithEmbedding(document, embeddingEntity)

                Log.d(TAG, "  ✅ Indexed chunk $index (${chunk.text.length} chars)")
            } catch (e: Exception) {
                Log.e(TAG, "  ❌ Failed to index chunk $index: ${e.message}")
            }
        }

        Log.d(TAG, "✅ Conversation indexing complete")
    }

    /**
     * Build RAG-enhanced context for a user query
     *
     * @param userQuery User's question
     * @param libraryId Optional library filter
     * @param topK Number of results
     * @return Context string to add to system prompt
     */
    suspend fun buildContextForQuery(
        userQuery: String,
        libraryId: String? = null,
        topK: Int = 10
    ): String {
        return ragContextBuilder.buildContext(userQuery, libraryId, topK)
    }

    /**
     * Search messages semantically
     *
     * @param query Search query
     * @param limit Maximum results
     * @return List of relevant RAG documents
     */
    suspend fun searchMessages(
        query: String,
        limit: Int = 10
    ): List<RAGDocument> {
        return vectorSearchService.hybridSearch(query, limit, sourceType = "message")
    }

    /**
     * Get statistics about indexed content
     *
     * @return Map of statistics
     */
    suspend fun getStatistics(): Map<String, Int> = withContext(Dispatchers.IO) {
        mapOf(
            "total_documents" to ragDao.getDocumentCount(),
            "total_embeddings" to ragDao.getEmbeddingCount(),
            "message_documents" to ragDao.getDocumentCountByType("message"),
            "conversation_documents" to ragDao.getDocumentCountByType("conversation")
        )
    }

    /**
     * Check if a message is already indexed
     *
     * @param messageId Message ID
     * @return True if indexed
     */
    suspend fun isMessageIndexed(messageId: String): Boolean = withContext(Dispatchers.IO) {
        val indexed = ragDao.getIndexedSourceIds("message")
        indexed.contains(messageId)
    }

    /**
     * Delete all RAG data (for testing/reset)
     */
    suspend fun clearAllRAGData() = withContext(Dispatchers.IO) {
        Log.d(TAG, "🗑️ Clearing all RAG data...")
        ragDao.deleteAllRAGData()
        Log.d(TAG, "✅ All RAG data cleared")
    }

    /**
     * Delete RAG data for a specific conversation
     *
     * @param conversationId Conversation ID
     */
    suspend fun deleteConversationRAGData(conversationId: String) = withContext(Dispatchers.IO) {
        ragDao.deleteDocumentsBySource("conversation", conversationId)
        Log.d(TAG, "✅ Deleted RAG data for conversation ${conversationId.take(8)}")
    }

    // MARK: - Private Helpers

    /**
     * Create text chunks from conversation messages
     * Groups messages into coherent chunks for embedding
     */
    private fun createConversationChunks(messages: List<ChatMessage>): List<ConversationChunk> {
        val chunks = mutableListOf<ConversationChunk>()
        val maxChunkSize = 6000 // Max characters per chunk (~1500 tokens)

        var currentChunk = StringBuilder()
        var currentMessageCount = 0

        for (message in messages) {
            val messageText = """
                ${if (message.isUser) "User" else "Assistant"}: ${message.content}

            """.trimIndent()

            // Check if adding this message would exceed chunk size
            if (currentChunk.length + messageText.length > maxChunkSize && currentChunk.isNotEmpty()) {
                // Save current chunk
                chunks.add(ConversationChunk(currentChunk.toString(), currentMessageCount))

                // Start new chunk
                currentChunk = StringBuilder()
                currentMessageCount = 0
            }

            currentChunk.append(messageText)
            currentChunk.append("\n")
            currentMessageCount++
        }

        // Add final chunk if not empty
        if (currentChunk.isNotEmpty()) {
            chunks.add(ConversationChunk(currentChunk.toString(), currentMessageCount))
        }

        return chunks
    }

    /**
     * Data class for conversation chunks
     */
    private data class ConversationChunk(
        val text: String,
        val messageCount: Int
    )
}
