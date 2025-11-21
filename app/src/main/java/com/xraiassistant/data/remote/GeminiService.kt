package com.xraiassistant.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

/**
 * Google Gemini API Service
 *
 * Endpoint: https://generativelanguage.googleapis.com
 * Documentation: https://ai.google.dev/gemini-api/docs
 *
 * Note: Gemini uses API key as query parameter instead of header
 */
interface GeminiService {

    /**
     * Generate content with streaming support
     *
     * Example curl:
     * ```
     * curl -X POST \
     *   "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-exp:streamGenerateContent?key=YOUR_API_KEY" \
     *   -H "Content-Type: application/json" \
     *   -d '{
     *     "contents": [{
     *       "parts": [{"text": "Write a story about a magic backpack"}]
     *     }]
     *   }'
     * ```
     */
    @POST("v1beta/models/{model}:streamGenerateContent")
    @Streaming
    suspend fun streamGenerateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: com.xraiassistant.data.models.GeminiRequest
    ): Response<ResponseBody>

    /**
     * Generate content (non-streaming)
     */
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: com.xraiassistant.data.models.GeminiRequest
    ): Response<ResponseBody>
}
