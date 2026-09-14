package com.xraiassistant.data.remote

import com.xraiassistant.data.models.OpenAIRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming

/**
 * OpenAI API Service
 *
 * Endpoint: https://api.openai.com
 * Documentation: https://platform.openai.com/docs/api-reference/chat
 *
 * Supported Models:
 * - GPT-6 Astra (gpt-6-astra) - Most capable, 1.05M context, effort-based
 * - GPT-5.6 Sol / Terra / Luna - Current generation, 1.05M context, effort-based
 * - GPT-5.2 (gpt-5.2) - Previous generation, sampling-based
 *
 * IMPORTANT: GPT-5.6 and GPT-6 accept only the default temperature and top_p and
 * return 400 on any other value. They take reasoning_effort and
 * max_completion_tokens instead of temperature/top_p and max_tokens.
 */
interface OpenAIService {

    /**
     * Chat completions with streaming support
     *
     * Example curl:
     * ```
     * curl -X POST "https://api.openai.com/v1/chat/completions" \
     *   -H "Authorization: Bearer YOUR_API_KEY" \
     *   -H "Content-Type: application/json" \
     *   -d '{
     *     "model": "gpt-5.6-sol",
     *     "messages": [{"role": "user", "content": "Hello"}],
     *     "stream": true,
     *     "reasoning_effort": "high",
     *     "max_completion_tokens": 64000
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
