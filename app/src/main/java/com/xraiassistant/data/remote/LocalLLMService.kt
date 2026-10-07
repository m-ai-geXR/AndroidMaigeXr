package com.xraiassistant.data.remote

import com.xraiassistant.data.models.OpenAIRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming
import retrofit2.http.Url

/**
 * The user's own OpenAI-compatible server. The address comes from Settings, so
 * the full URL is passed on each call.
 */
interface LocalLLMService {
    @POST
    @Streaming
    suspend fun chatCompletion(
        @Url url: String,
        @Header("Authorization") authorization: String?,
        @Body request: OpenAIRequest
    ): Response<ResponseBody>
}
