package com.xraiassistant.data.repositories

import android.util.Log
import com.xraiassistant.data.local.dao.RAGDao
import com.xraiassistant.data.local.entities.RAGDocumentEntity
import com.xraiassistant.data.models.RAGDocument
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * Vector Search Service
 *
 * Performs semantic search using vector embeddings and cosine similarity
 * Supports pure semantic search and hybrid FTS4+vector search
 */
@Singleton
class VectorSearchService @Inject constructor(
    private val ragDao: RAGDao,
    private val embeddingRepository: EmbeddingRepository
) {
    companion object {
        private const val TAG = "VectorSearchService"

        // Hybrid search weights
        private const val SEMANTIC_WEIGHT = 0.6f
        private const val KEYWORD_WEIGHT = 0.4f
    }

    /**
     * Calculate cosine similarity between two vectors
     *
     * @param a First vector
     * @param b Second vector
     * @return Similarity score between 0 and 1 (1 = identical)
     */
    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size || a.isEmpty()) {
            return 0f
        }

        // Calculate dot product
        var dotProduct = 0f
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
        }

        // Calculate magnitudes
        var magnitudeA = 0f
        var magnitudeB = 0f
        for (i in a.indices) {
            magnitudeA += a[i] * a[i]
            magnitudeB += b[i] * b[i]
        }
        magnitudeA = sqrt(magnitudeA)
        magnitudeB = sqrt(magnitudeB)

        if (magnitudeA == 0f || magnitudeB == 0f) {
            return 0f
        }

        return dotProduct / (magnitudeA * magnitudeB)
    }

    /**
     * Pure vector-based semantic search
     * Loads all embeddings and finds most similar
     *
     * @param query Search query text
     * @param topK Number of results to return
     * @param sourceType Optional filter by source type
     * @return List of documents ranked by semantic similarity
     */
    suspend fun semanticSearch(
        query: String,
        topK: Int = 5,
        sourceType: String? = null
    ): List<RAGDocument> {
        Log.d(TAG, "🔍 Semantic search for: ${query.take(50)}...")

        // 1. Generate query embedding
        val queryEmbedding = embeddingRepository.generateEmbedding(query)

        // 2. Load all embeddings from database
        val allEmbeddings = if (sourceType != null) {
            ragDao.loadEmbeddingsByType(sourceType)
        } else {
            ragDao.loadAllEmbeddings()
        }

        Log.d(TAG, "  📊 Comparing against ${allEmbeddings.count()} embeddings...")

        // 3. Calculate similarities
        val results = allEmbeddings.map { embeddingData ->
            val score = cosineSimilarity(queryEmbedding, embeddingData.embedding)
            embeddingData.document.toDomain(score)
        }

        // 4. Sort by similarity and return top-K
        val topResults = results
            .sortedByDescending { it.relevanceScore }
            .take(topK)

        val topScore = topResults.firstOrNull()?.relevanceScore ?: 0f
        Log.d(TAG, "  ✅ Found ${topResults.size} relevant documents (top score: %.3f)".format(topScore))

        return topResults
    }

    /**
     * Hybrid search: FTS4 keyword + vector semantic
     * Best results - combines fast keyword filtering with semantic understanding
     *
     * @param query Search query text
     * @param topK Number of results to return
     * @param sourceType Optional filter by source type
     * @return List of documents ranked by combined score
     */
    suspend fun hybridSearch(
        query: String,
        topK: Int = 10,
        sourceType: String? = null
    ): List<RAGDocument> {
        Log.d(TAG, "🔍 Hybrid search for: ${query.take(50)}...")

        // 1. FTS4 keyword search (fast pre-filter)
        val keywordResults = if (sourceType != null) {
            ragDao.fullTextSearchByType(query, sourceType, limit = 50)
        } else {
            ragDao.fullTextSearch(query, limit = 50)
        }

        if (keywordResults.isEmpty()) {
            Log.d(TAG, "  ℹ️ No keyword matches, falling back to pure semantic search")
            return semanticSearch(query, topK, sourceType)
        }

        Log.d(TAG, "  📊 FTS4 found ${keywordResults.size} keyword matches")

        // 2. Generate query embedding
        val queryEmbedding = embeddingRepository.generateEmbedding(query)

        // 3. Score keyword results with semantic similarity
        val scored = keywordResults.mapIndexedNotNull { index, doc ->
            val embedding = ragDao.loadEmbedding(doc.id) ?: return@mapIndexedNotNull null

            val semanticScore = cosineSimilarity(queryEmbedding, embedding)

            // FTS4 rank score (higher index = lower rank)
            val keywordScore = (keywordResults.size - index).toFloat() / keywordResults.size

            // Combine scores: 60% semantic + 40% keyword
            val finalScore = SEMANTIC_WEIGHT * semanticScore + KEYWORD_WEIGHT * keywordScore

            doc.toDomain(finalScore)
        }

        // 4. Return top-K by combined score
        val topResults = scored
            .sortedByDescending { it.relevanceScore }
            .take(topK)

        val topScore = topResults.firstOrNull()?.relevanceScore ?: 0f
        Log.d(TAG, "  ✅ Hybrid search returned ${topResults.size} documents (top score: %.3f)".format(topScore))

        return topResults
    }

    /**
     * Find conversations similar to a given conversation
     * Averages embeddings per conversation and compares
     *
     * @param conversationId ID of conversation to find similar to
     * @param topK Number of similar conversations to return
     * @return List of similar conversation IDs with scores
     */
    suspend fun findSimilarConversations(
        conversationId: String,
        topK: Int = 5
    ): List<Pair<String, Float>> {
        Log.d(TAG, "🔍 Finding conversations similar to: ${conversationId.take(8)}...")

        // 1. Get embeddings for the conversation
        val embeddings = ragDao.loadEmbeddingsForSource(conversationId, "conversation")

        if (embeddings.isEmpty()) {
            Log.d(TAG, "  ⚠️ No embeddings found for conversation")
            return emptyList()
        }

        // 2. Calculate average embedding for the conversation
        val avgEmbedding = averageEmbedding(embeddings)

        // 3. Load all other conversations' embeddings
        val allEmbeddings = ragDao.loadEmbeddingsByType("conversation")

        // 4. Group by conversation and calculate similarity
        val conversationScores = mutableMapOf<String, MutableList<FloatArray>>()

        for (embeddingData in allEmbeddings) {
            val sourceId = embeddingData.document.sourceId
            if (sourceId != conversationId) {
                conversationScores
                    .getOrPut(sourceId) { mutableListOf() }
                    .add(embeddingData.embedding)
            }
        }

        // 5. Calculate average embeddings and similarities
        val similarities = conversationScores.map { (sourceId, embeddings) ->
            val avgOtherEmbedding = averageEmbedding(embeddings)
            val similarity = cosineSimilarity(avgEmbedding, avgOtherEmbedding)
            sourceId to similarity
        }

        // 6. Sort and get top-K
        val topSimilar = similarities
            .sortedByDescending { it.second }
            .take(topK)

        Log.d(TAG, "  ✅ Found ${topSimilar.size} similar conversations")

        return topSimilar
    }

    /**
     * Calculate average of multiple embeddings
     * Used for conversation-level similarity
     *
     * @param embeddings List of embeddings to average
     * @return Average embedding vector
     */
    private fun averageEmbedding(embeddings: List<FloatArray>): FloatArray {
        if (embeddings.isEmpty()) {
            return FloatArray(0)
        }

        val dimension = embeddings.first().size
        val avg = FloatArray(dimension) { 0f }

        for (embedding in embeddings) {
            for (i in embedding.indices) {
                avg[i] += embedding[i]
            }
        }

        val count = embeddings.size.toFloat()
        for (i in avg.indices) {
            avg[i] /= count
        }

        return avg
    }
}

/**
 * Extension function to convert RAGDocumentEntity to domain model
 */
private fun RAGDocumentEntity.toDomain(relevanceScore: Float = 0f): RAGDocument {
    val metadata = try {
        if (this.metadata != null) {
            val moshi = com.squareup.moshi.Moshi.Builder().build()
            val adapter = moshi.adapter<Map<*, *>>(Map::class.java)
            @Suppress("UNCHECKED_CAST")
            adapter.fromJson(this.metadata) as? Map<String, String> ?: emptyMap()
        } else {
            emptyMap()
        }
    } catch (e: Exception) {
        emptyMap()
    }

    return RAGDocument(
        id = this.id,
        sourceType = this.sourceType,
        sourceId = this.sourceId,
        chunkText = this.chunkText,
        chunkIndex = this.chunkIndex,
        metadata = metadata,
        relevanceScore = relevanceScore
    )
}
