package com.xraiassistant.data.remote

import com.xraiassistant.data.models.AnthropicRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming

/**
 * Anthropic API Service
 *
 * Endpoint: https://api.anthropic.com
 * Documentation: https://docs.claude.com/en/api/messages
 *
 * Supported Models:
 * - Claude Fable 5.1 (claude-fable-5-1) - Most capable, 1M context, effort-based
 * - Claude Opus 5 (claude-opus-5) - Frontier coding and agents, 1M context, effort-based
 * - Claude Sonnet 5 (claude-sonnet-5) - Best speed/cost balance, 1M context, effort-based
 * - Claude Haiku 4.5 (claude-haiku-4-5) - Fast and cheap, 200K context, sampling-based
 * - Claude Opus 4.6 / Sonnet 4.6 - Previous generation, sampling-based
 */
interface AnthropicService {

    /**
     * Messages API with streaming support
     *
     * Supports the Claude 5 series (effort-based) and Claude 4.x (sampling-based).
     *
     * Example curl (Claude 4.x - sampling):
     * ```
     * curl -X POST "https://api.anthropic.com/v1/messages" \
     *   -H "x-api-key: YOUR_API_KEY" \
     *   -H "anthropic-version: 2023-06-01" \
     *   -H "Content-Type: application/json" \
     *   -d '{
     *     "model": "claude-sonnet-4-6",
     *     "messages": [{"role": "user", "content": "Hello"}],
     *     "stream": true,
     *     "max_tokens": 4096,
     *     "temperature": 0.7
     *   }'
     * ```
     *
     * Example curl (Claude 5 series - adaptive thinking plus effort):
     * ```
     * curl -X POST "https://api.anthropic.com/v1/messages" \
     *   -H "x-api-key: YOUR_API_KEY" \
     *   -H "anthropic-version: 2023-06-01" \
     *   -H "Content-Type: application/json" \
     *   -d '{
     *     "model": "claude-opus-5",
     *     "messages": [{"role": "user", "content": "Solve this complex problem..."}],
     *     "stream": true,
     *     "max_tokens": 64000,
     *     "thinking": {"type": "adaptive"},
     *     "output_config": {"effort": "high"}
     *   }'
     * ```
     *
     * IMPORTANT: the Claude 5 series removed temperature and top_p, and rejects the
     * older thinking shape {"type": "enabled", "budget_tokens": N}. Both return 400.
     */
    @POST("v1/messages")
    @Streaming
    suspend fun messages(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") version: String = "2023-06-01",
        @Body request: AnthropicRequest
    ): Response<ResponseBody>
}
