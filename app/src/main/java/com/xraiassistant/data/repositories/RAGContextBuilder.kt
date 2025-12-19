package com.xraiassistant.data.repositories

import android.util.Log
import com.xraiassistant.data.models.RAGDocument
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RAG Context Builder
 *
 * Assembles context from RAG search results for AI prompts
 * Token-aware context building with relevance scoring
 */
@Singleton
class RAGContextBuilder @Inject constructor(
    private val vectorSearchService: VectorSearchService
) {
    companion object {
        private const val TAG = "RAGContextBuilder"
        private const val MAX_CONTEXT_TOKENS = 3000 // Reserve ~1000 tokens for user query + AI response
        private const val CHARS_PER_TOKEN = 4 // Rough estimation: 4 chars ≈ 1 token
    }

    /**
     * Build RAG context from conversation history for a user query
     *
     * @param userQuery User's question/prompt
     * @param libraryId Optional filter by 3D library
     * @param topK Number of relevant chunks to consider
     * @return Formatted context string for AI prompt
     */
    suspend fun buildContext(
        userQuery: String,
        libraryId: String? = null,
        topK: Int = 10
    ): String {
        Log.d(TAG, "🔨 Building RAG context for query: ${userQuery.take(50)}...")

        // 1. Hybrid search for relevant chunks (keyword + semantic)
        var relevantDocs = vectorSearchService.hybridSearch(userQuery, topK * 2) // Get extra for filtering

        // 2. Filter by library if specified
        if (libraryId != null) {
            val beforeCount = relevantDocs.size
            relevantDocs = relevantDocs.filter { doc ->
                doc.metadata["library_id"] == libraryId
            }
            Log.d(TAG, "  📚 Filtered to library: $libraryId (${relevantDocs.size}/$beforeCount docs)")
        }

        // 3. Take top-K after filtering
        relevantDocs = relevantDocs.take(topK)

        if (relevantDocs.isEmpty()) {
            Log.d(TAG, "  ℹ️ No relevant context found")
            return ""
        }

        // 4. Build context string (token-aware)
        val context = StringBuilder("# Relevant Context from Previous Conversations:\n\n")
        var tokenCount = 0
        var chunksIncluded = 0

        for (doc in relevantDocs) {
            val relevancePercent = (doc.relevanceScore * 100).toInt()
            val chunk = """
---
**Relevance**: $relevancePercent% | **Source**: ${doc.sourceType}
${doc.chunkText}


""".trimIndent()

            val chunkTokens = estimateTokens(chunk)

            // Check if adding this chunk exceeds token limit
            if (tokenCount + chunkTokens > MAX_CONTEXT_TOKENS) {
                Log.d(TAG, "  ⚠️ Reached token limit ($MAX_CONTEXT_TOKENS), stopping at $chunksIncluded chunks")
                break
            }

            context.append(chunk)
            context.append("\n")
            tokenCount += chunkTokens
            chunksIncluded++
        }

        if (chunksIncluded == 0) {
            Log.d(TAG, "  ℹ️ No chunks fit within token limit")
            return ""
        }

        Log.d(TAG, "  ✅ Built context with $chunksIncluded chunks (~$tokenCount tokens)")
        return context.toString()
    }

    /**
     * Build context for continuing a specific conversation
     * Searches within the conversation history
     *
     * @param conversationId Conversation ID
     * @param userQuery User's question
     * @return Formatted context string
     */
    suspend fun buildConversationContext(
        conversationId: String,
        userQuery: String
    ): String {
        Log.d(TAG, "🔨 Building conversation context for: ${conversationId.take(8)}...")

        // Search within the specific conversation
        val relevantDocs = vectorSearchService.hybridSearch(
            query = userQuery,
            topK = 5,
            sourceType = "conversation"
        ).filter { it.sourceId == conversationId }

        if (relevantDocs.isEmpty()) {
            Log.d(TAG, "  ℹ️ No relevant context in this conversation")
            return ""
        }

        val context = StringBuilder("# Relevant Context from This Conversation:\n\n")
        var tokenCount = 0

        for (doc in relevantDocs) {
            val chunk = "${doc.chunkText}\n\n"
            val chunkTokens = estimateTokens(chunk)

            if (tokenCount + chunkTokens > MAX_CONTEXT_TOKENS) {
                break
            }

            context.append(chunk)
            tokenCount += chunkTokens
        }

        Log.d(TAG, "  ✅ Built conversation context (~$tokenCount tokens)")
        return context.toString()
    }

    /**
     * Build context focused on code examples and patterns
     *
     * @param query Search query
     * @param language Optional programming language filter
     * @return Formatted context with code examples
     */
    suspend fun buildCodeContext(
        query: String,
        language: String? = null
    ): String {
        Log.d(TAG, "🔨 Building code context for: ${query.take(50)}...")

        // Search for code-related content
        var searchQuery = query
        if (language != null) {
            searchQuery += " $language code example"
        }

        val relevantDocs = vectorSearchService.hybridSearch(searchQuery, topK = 8)

        // Filter for docs that likely contain code (heuristic)
        val codeDocs = relevantDocs.filter { doc ->
            val text = doc.chunkText.lowercase()
            text.contains("function") ||
            text.contains("const") ||
            text.contains("class") ||
            text.contains("import") ||
            text.contains("```") ||
            text.contains("{") ||
            text.contains("=>")
        }

        if (codeDocs.isEmpty()) {
            Log.d(TAG, "  ℹ️ No code examples found")
            return ""
        }

        val context = StringBuilder("# Relevant Code Examples:\n\n")
        var tokenCount = 0

        for (doc in codeDocs.take(5)) {
            val chunk = "```\n${doc.chunkText}\n```\n\n"
            val chunkTokens = estimateTokens(chunk)

            if (tokenCount + chunkTokens > MAX_CONTEXT_TOKENS) {
                break
            }

            context.append(chunk)
            tokenCount += chunkTokens
        }

        Log.d(TAG, "  ✅ Built code context with ${codeDocs.size} examples (~$tokenCount tokens)")
        return context.toString()
    }

    /**
     * Build context that considers multiple recent user messages
     * Used for multi-turn conversations
     *
     * @param recentMessages List of recent user messages
     * @param libraryId Optional library filter
     * @return Formatted context string
     */
    suspend fun buildMultiTurnContext(
        recentMessages: List<String>,
        libraryId: String? = null
    ): String {
        Log.d(TAG, "🔨 Building multi-turn context from ${recentMessages.size} messages...")

        // Combine recent messages into a compound query
        val compoundQuery = recentMessages.joinToString(" ")

        // Perform search with compound query
        var relevantDocs = vectorSearchService.hybridSearch(compoundQuery, topK = 15)

        // Filter by library if specified
        if (libraryId != null) {
            relevantDocs = relevantDocs.filter { it.metadata["library_id"] == libraryId }
        }

        // Group by conversation to avoid redundancy
        val seenConversations = mutableSetOf<String>()
        val uniqueDocs = mutableListOf<RAGDocument>()

        for (doc in relevantDocs) {
            if (!seenConversations.contains(doc.sourceId)) {
                uniqueDocs.add(doc)
                seenConversations.add(doc.sourceId)

                if (uniqueDocs.size >= 8) {
                    break
                }
            }
        }

        val context = StringBuilder("# Relevant Context (Multi-Turn):\n\n")
        var tokenCount = 0

        for (doc in uniqueDocs) {
            val chunk = "---\n${doc.chunkText}\n\n"
            val chunkTokens = estimateTokens(chunk)

            if (tokenCount + chunkTokens > MAX_CONTEXT_TOKENS) {
                break
            }

            context.append(chunk)
            tokenCount += chunkTokens
        }

        Log.d(TAG, "  ✅ Built multi-turn context with ${uniqueDocs.size} unique conversations (~$tokenCount tokens)")
        return context.toString()
    }

    /**
     * Rough token estimation (4 chars ≈ 1 token)
     *
     * @param text Text to estimate
     * @return Approximate token count
     */
    private fun estimateTokens(text: String): Int {
        return text.length / CHARS_PER_TOKEN
    }

    /**
     * Truncate context to fit within token limit
     *
     * @param context Context string
     * @param maxTokens Maximum tokens allowed
     * @return Truncated context
     */
    fun truncateContext(context: String, maxTokens: Int): String {
        val estimatedTokens = estimateTokens(context)

        if (estimatedTokens <= maxTokens) {
            return context
        }

        // Truncate by character count (rough approximation)
        val maxChars = maxTokens * CHARS_PER_TOKEN
        if (context.length > maxChars) {
            val truncated = context.substring(0, maxChars)
            return "$truncated\n\n...(context truncated)"
        }

        return context
    }

    /**
     * Get max context tokens limit
     */
    fun getMaxContextTokens(): Int = MAX_CONTEXT_TOKENS
}
