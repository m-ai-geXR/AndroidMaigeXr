package com.xraiassistant.data.remote

import com.xraiassistant.data.models.OpenAIRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming

/**
 * xAI (Grok) API Service
 *
 * xAI uses an OpenAI-compatible API format — same request/response structure
 * and SSE streaming format. Reuses OpenAIRequest data class.
 *
 * Endpoint: https://api.x.ai
 * Documentation: https://docs.x.ai/docs
 */
interface XAIService {

    /**
     * Chat completions with streaming support
     *
     * Example curl:
     * ```
     * curl -X POST "https://api.x.ai/v1/chat/completions" \
     *   -H "Authorization: Bearer YOUR_XAI_API_KEY" \
     *   -H "Content-Type: application/json" \
     *   -d '{
     *     "model": "grok-4-0709",
     *     "messages": [{"role": "user", "content": "Hello"}],
     *     "stream": true
     *   }'
     * ```
     */
    @POST("v1/chat/completions")
    @Streaming
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: OpenAIRequest
    ): Response<ResponseBody>
}
