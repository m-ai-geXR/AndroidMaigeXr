package com.xraiassistant.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Together AI Embedding API Models
 *
 * For generating vector embeddings using the m2-bert-80M-8k-retrieval model
 * Documentation: https://docs.together.ai/reference/embeddings
 */

/**
 * Request to generate embeddings
 * Supports single string or array of strings
 */
@JsonClass(generateAdapter = true)
data class EmbeddingRequest(
    @Json(name = "model") val model: String = "togethercomputer/m2-bert-80M-8k-retrieval",
    @Json(name = "input") val input: Any  // String or List<String>
)

/**
 * Response from embedding API
 */
@JsonClass(generateAdapter = true)
data class EmbeddingResponse(
    @Json(name = "data") val data: List<EmbeddingDataItem>,
    @Json(name = "model") val model: String,
    @Json(name = "object") val objectType: String = "list"
)

/**
 * Individual embedding data item
 */
@JsonClass(generateAdapter = true)
data class EmbeddingDataItem(
    @Json(name = "embedding") val embedding: List<Double>,
    @Json(name = "index") val index: Int,
    @Json(name = "object") val objectType: String = "embedding"
)

/**
 * Domain model for RAG document
 */
data class RAGDocument(
    val id: String,
    val sourceType: String,  // "conversation", "message", "code"
    val sourceId: String,
    val chunkText: String,
    val chunkIndex: Int,
    val metadata: Map<String, String> = emptyMap(),
    val relevanceScore: Float = 0f
)

/**
 * Embedding data with associated document
 */
data class EmbeddingData(
    val document: RAGDocument,
    val embedding: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EmbeddingData

        if (document != other.document) return false
        if (!embedding.contentEquals(other.embedding)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = document.hashCode()
        result = 31 * result + embedding.contentHashCode()
        return result
    }
}
