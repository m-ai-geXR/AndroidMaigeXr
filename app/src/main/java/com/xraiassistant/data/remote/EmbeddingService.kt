package com.xraiassistant.data.remote

import com.xraiassistant.data.models.EmbeddingRequest
import com.xraiassistant.data.models.EmbeddingResponse
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Together AI Embedding Service
 *
 * Retrofit interface for generating vector embeddings
 * Uses togethercomputer/m2-bert-80M-8k-retrieval model (768 dimensions)
 *
 * API Documentation: https://docs.together.ai/reference/embeddings
 */
interface EmbeddingService {

    /**
     * Generate embeddings for text(s)
     *
     * @param authorization Bearer token for Together AI API ("Bearer YOUR_API_KEY")
     * @param request Embedding request with text(s) to embed
     * @return Embedding response with 768-dimensional vectors
     */
    @POST("v1/embeddings")
    suspend fun generateEmbedding(
        @retrofit2.http.Header("Authorization") authorization: String,
        @Body request: EmbeddingRequest
    ): EmbeddingResponse
}
