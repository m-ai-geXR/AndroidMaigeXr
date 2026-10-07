package com.xraiassistant.di

import android.util.Log
import com.squareup.moshi.Moshi
import com.xraiassistant.BuildConfig
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.xraiassistant.data.remote.AnthropicService
import com.xraiassistant.data.remote.CodeSandboxService
import com.xraiassistant.data.remote.EmbeddingService
import com.xraiassistant.data.remote.GeminiService
import com.xraiassistant.data.remote.OpenAIService
import com.xraiassistant.data.remote.TogetherAIService
import com.xraiassistant.data.remote.XAIService
import com.xraiassistant.data.remote.LocalLLMService
import com.xraiassistant.domain.local.LocalServerConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Network Module
 *
 * Provides Retrofit instances for different AI providers:
 * - Together.ai: https://api.together.xyz
 * - OpenAI: https://api.openai.com
 * - Anthropic: https://api.anthropic.com
 * - Google: https://generativelanguage.googleapis.com
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // Qualifiers for different Retrofit instances
    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class TogetherAI

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class OpenAI

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class Anthropic

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class Google

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class CodeSandbox

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class XAI

    /**
     * Moshi for JSON serialization
     */
    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    /**
     * OkHttpClient shared by every provider.
     *
     * TLS uses the platform's normal certificate and hostname checks; nothing is
     * overridden. An earlier version installed a trust-all TrustManager and an
     * always-true HostnameVerifier "for the emulator" in every build, which let
     * anyone on the network read API keys and prompts, and is grounds for Play
     * rejection.
     *
     * Logging is off in release. Debug builds log request lines and status only,
     * with credentials removed: auth headers are redacted and the Gemini key,
     * which travels as a ?key= query parameter, is masked in the logged URL.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            // Reasoning models can think for minutes before emitting a first token,
            // and readTimeout measures the gap between reads, so 120s trips during
            // that silence.
            .readTimeout(600, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            // Plain HTTP is allowed by the network security config only so the
            // user's own model server on their network works (see Local in
            // Settings). This keeps every other host on HTTPS.
            .addInterceptor { chain ->
                val url = chain.request().url
                if (!url.isHttps && !LocalServerConfig.isLocalHost(url.host)) {
                    throw java.io.IOException("Plain HTTP is only allowed to local network addresses: ${url.host}")
                }
                chain.proceed(chain.request())
            }

        if (BuildConfig.DEBUG) {
            val keyInUrl = Regex("""([?&]key=)[^&\s]+""")
            val logging = HttpLoggingInterceptor { message ->
                Log.d("OkHttp", message.replace(keyInUrl, "$1██"))
            }.apply {
                level = HttpLoggingInterceptor.Level.BASIC
                redactHeader("Authorization")
                redactHeader("x-api-key")
                redactHeader("x-goog-api-key")
            }
            builder.addInterceptor(logging)
        }

        return builder.build()
    }

    /**
     * Retrofit for Together.ai
     * Base URL: https://api.together.xyz
     */
    @Provides
    @Singleton
    @TogetherAI
    fun provideTogetherAIRetrofit(
        okHttpClient: OkHttpClient,
        moshi: Moshi
    ): Retrofit = Retrofit.Builder()
        .baseUrl("https://api.together.xyz/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    /**
     * Retrofit for OpenAI
     * Base URL: https://api.openai.com
     */
    @Provides
    @Singleton
    @OpenAI
    fun provideOpenAIRetrofit(
        okHttpClient: OkHttpClient,
        moshi: Moshi
    ): Retrofit = Retrofit.Builder()
        .baseUrl("https://api.openai.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    /**
     * Retrofit for Anthropic
     * Base URL: https://api.anthropic.com
     */
    @Provides
    @Singleton
    @Anthropic
    fun provideAnthropicRetrofit(
        okHttpClient: OkHttpClient,
        moshi: Moshi
    ): Retrofit = Retrofit.Builder()
        .baseUrl("https://api.anthropic.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    /**
     * Together.ai Service
     */
    @Provides
    @Singleton
    fun provideTogetherAIService(
        @TogetherAI retrofit: Retrofit
    ): TogetherAIService = retrofit.create(TogetherAIService::class.java)

    /**
     * OpenAI Service
     */
    @Provides
    @Singleton
    fun provideOpenAIService(
        @OpenAI retrofit: Retrofit
    ): OpenAIService = retrofit.create(OpenAIService::class.java)

    /**
     * Anthropic Service
     */
    @Provides
    @Singleton
    fun provideAnthropicService(
        @Anthropic retrofit: Retrofit
    ): AnthropicService = retrofit.create(AnthropicService::class.java)

    /**
     * Retrofit for Google Gemini
     * Base URL: https://generativelanguage.googleapis.com
     */
    @Provides
    @Singleton
    @Google
    fun provideGeminiRetrofit(
        okHttpClient: OkHttpClient,
        moshi: Moshi
    ): Retrofit = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    /**
     * Google Gemini Service
     */
    @Provides
    @Singleton
    fun provideGeminiService(
        @Google retrofit: Retrofit
    ): GeminiService = retrofit.create(GeminiService::class.java)

    /**
     * Retrofit for xAI (Grok)
     * Base URL: https://api.x.ai
     * Uses OpenAI-compatible API format
     */
    @Provides
    @Singleton
    @XAI
    fun provideXAIRetrofit(
        okHttpClient: OkHttpClient,
        moshi: Moshi
    ): Retrofit = Retrofit.Builder()
        .baseUrl("https://api.x.ai/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    /**
     * xAI (Grok) Service
     */
    @Provides
    @Singleton
    fun provideXAIService(
        @XAI retrofit: Retrofit
    ): XAIService = retrofit.create(XAIService::class.java)

    /**
     * The user's own model server. Calls pass a full URL, so any base works;
     * the xAI Retrofit is reused for its client and converters.
     */
    @Provides
    @Singleton
    fun provideLocalLLMService(
        @XAI retrofit: Retrofit
    ): LocalLLMService = retrofit.create(LocalLLMService::class.java)

    /**
     * Retrofit for CodeSandbox
     * Base URL: https://codesandbox.io/api/v1/
     */
    @Provides
    @Singleton
    @CodeSandbox
    fun provideCodeSandboxRetrofit(
        okHttpClient: OkHttpClient,
        moshi: Moshi
    ): Retrofit = Retrofit.Builder()
        .baseUrl("https://codesandbox.io/api/v1/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    /**
     * CodeSandbox Service
     */
    @Provides
    @Singleton
    fun provideCodeSandboxService(
        @CodeSandbox retrofit: Retrofit
    ): CodeSandboxService = retrofit.create(CodeSandboxService::class.java)

    /**
     * Embedding Service (Together.ai)
     * Uses the same base URL as TogetherAIService for generating embeddings
     */
    @Provides
    @Singleton
    fun provideEmbeddingService(
        @TogetherAI retrofit: Retrofit
    ): EmbeddingService = retrofit.create(EmbeddingService::class.java)
}
