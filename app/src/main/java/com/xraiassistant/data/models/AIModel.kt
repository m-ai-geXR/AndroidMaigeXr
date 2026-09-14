package com.xraiassistant.data.models

/**
 * AI Model data classes
 * Equivalent to the model management system in iOS ChatViewModel.swift
 */
data class AIModel(
    val id: String,
    val displayName: String,
    val description: String,
    val provider: String,
    val pricing: String = "",
    val capabilities: Set<AICapability> = emptySet(),
    val control: AIModelControl = AIModelControl.SAMPLING,
    val maxOutputTokens: Int = 16_000
)

enum class AICapability {
    TEXT_GENERATION,
    CODE_GENERATION,
    STREAMING,
    FUNCTION_CALLING
}

/**
 * Which generation controls a model actually accepts.
 *
 * Frontier models (Claude 5 series, GPT-5.6 / GPT-6) removed temperature and
 * top_p from their APIs and reject any request carrying custom values with a
 * 400. They expose a discrete reasoning-effort level instead.
 */
enum class AIModelControl {
    /** Legacy sampling knobs: temperature + top_p. */
    SAMPLING,

    /** Discrete reasoning effort. Sending temperature/top_p to these models is a 400. */
    EFFORT
}

/**
 * Reasoning depth for models using EFFORT control.
 *
 * Anthropic (output_config.effort) and OpenAI (reasoning_effort) share the same
 * five level names, so one type covers both providers.
 */
enum class AIEffort(val apiValue: String, val displayName: String, val summary: String) {
    LOW("low", "Low", "Fastest and cheapest - simple scenes"),
    MEDIUM("medium", "Medium", "Light reasoning for routine edits"),
    HIGH("high", "High", "Balanced depth and cost (recommended)"),
    XHIGH("xhigh", "Extra High", "Deeper reasoning for complex scenes"),
    MAX("max", "Maximum", "Maximum depth - highest cost and latency");

    companion object {
        fun fromApiValue(value: String?): AIEffort =
            entries.firstOrNull { it.apiValue == value } ?: HIGH
    }
}

/**
 * Predefined AI models matching iOS implementation
 */
object AIModels {
    // ============= TOGETHER.AI MODELS =============

    val DEEPSEEK_R1_70B = AIModel(
        id = "deepseek-ai/DeepSeek-R1",
        displayName = "DeepSeek R1",
        description = "Advanced reasoning & coding",
        provider = "Together.ai",
        pricing = "Serverless",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING
        )
    )

    val LLAMA_3_3_70B = AIModel(
        id = "meta-llama/Llama-3.3-70B-Instruct-Turbo",
        displayName = "Llama 3.3 70B Turbo",
        description = "Latest large model",
        provider = "Together.ai",
        pricing = "Serverless",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING
        )
    )

    val LLAMA_3_8B_LITE = AIModel(
        id = "meta-llama/Meta-Llama-3-8B-Instruct-Lite",
        displayName = "Llama 3 8B Lite",
        description = "Cost-effective option",
        provider = "Together.ai",
        pricing = "$0.10/1M tokens",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING
        )
    )

    val LLAMA_3_1_8B_TURBO = AIModel(
        id = "meta-llama/Meta-Llama-3.1-8B-Instruct-Turbo",
        displayName = "Llama 3.1 8B Turbo",
        description = "Good balance",
        provider = "Together.ai",
        pricing = "$0.18/1M tokens",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING
        )
    )

    val QWEN_2_5_7B_TURBO = AIModel(
        id = "Qwen/Qwen2.5-7B-Instruct-Turbo",
        displayName = "Qwen 2.5 7B Turbo",
        description = "Fast coding specialist",
        provider = "Together.ai",
        pricing = "$0.30/1M tokens",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING
        )
    )

    // ============= OPENAI MODELS =============

    // GPT-6 / GPT-5.6 Series (current generation)
    val GPT_6_ASTRA = AIModel(
        id = "gpt-6-astra",
        displayName = "GPT-6 Astra",
        description = "Most capable model, built for the hardest end-to-end work - 1.05M context",
        provider = "OpenAI",
        pricing = "$10.00/$50.00 per 1M tokens",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING,
            AICapability.FUNCTION_CALLING
        ),
        control = AIModelControl.EFFORT,
        maxOutputTokens = 64_000
    )

    val GPT_5_6_SOL = AIModel(
        id = "gpt-5.6-sol",
        displayName = "GPT-5.6 Sol",
        description = "Flagship for complex professional work - 1.05M context",
        provider = "OpenAI",
        pricing = "$4.00/$20.00 per 1M tokens",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING,
            AICapability.FUNCTION_CALLING
        ),
        control = AIModelControl.EFFORT,
        maxOutputTokens = 64_000
    )

    val GPT_5_6_TERRA = AIModel(
        id = "gpt-5.6-terra",
        displayName = "GPT-5.6 Terra",
        description = "Balances intelligence and cost - 1.05M context",
        provider = "OpenAI",
        pricing = "$2.00/$12.00 per 1M tokens",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING,
            AICapability.FUNCTION_CALLING
        ),
        control = AIModelControl.EFFORT,
        maxOutputTokens = 64_000
    )

    val GPT_5_6_LUNA = AIModel(
        id = "gpt-5.6-luna",
        displayName = "GPT-5.6 Luna",
        description = "Optimized for cost-sensitive workloads - 1.05M context",
        provider = "OpenAI",
        pricing = "$0.20/$1.20 per 1M tokens",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING,
            AICapability.FUNCTION_CALLING
        ),
        control = AIModelControl.EFFORT,
        maxOutputTokens = 64_000
    )

    // GPT-5.2 (previous generation - kept as fallback)
    val GPT_5_2 = AIModel(
        id = "gpt-5.2",
        displayName = "GPT-5.2",
        description = "Previous-generation coding and agentic model - 400K context",
        provider = "OpenAI",
        pricing = "$1.75/$14.00 per 1M tokens",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING,
            AICapability.FUNCTION_CALLING
        ),
        control = AIModelControl.SAMPLING,
        maxOutputTokens = 64_000
    )

    // ============= ANTHROPIC MODELS =============

    // Claude 5 Series (current generation)
    val CLAUDE_FABLE_5_1 = AIModel(
        id = "claude-fable-5-1",
        displayName = "Claude Fable 5.1",
        description = "Most capable model for the hardest reasoning and agentic work - 1M context",
        provider = "Anthropic",
        pricing = "$10.00/$50.00 per 1M tokens",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING,
            AICapability.FUNCTION_CALLING
        ),
        control = AIModelControl.EFFORT,
        maxOutputTokens = 64_000
    )

    val CLAUDE_OPUS_5 = AIModel(
        id = "claude-opus-5",
        displayName = "Claude Opus 5",
        description = "Frontier intelligence for agents and coding - 1M context",
        provider = "Anthropic",
        pricing = "$5.00/$25.00 per 1M tokens",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING,
            AICapability.FUNCTION_CALLING
        ),
        control = AIModelControl.EFFORT,
        maxOutputTokens = 64_000
    )

    val CLAUDE_SONNET_5 = AIModel(
        id = "claude-sonnet-5",
        displayName = "Claude Sonnet 5",
        description = "Best combination of speed, cost and intelligence - 1M context",
        provider = "Anthropic",
        pricing = "$2.00/$10.00 per 1M tokens",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING,
            AICapability.FUNCTION_CALLING
        ),
        control = AIModelControl.EFFORT,
        maxOutputTokens = 64_000
    )

    val CLAUDE_HAIKU_4_5 = AIModel(
        id = "claude-haiku-4-5",
        displayName = "Claude Haiku 4.5",
        description = "Fastest model with near-frontier intelligence - 200K context",
        provider = "Anthropic",
        pricing = "$1.00/$5.00 per 1M tokens",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING,
            AICapability.FUNCTION_CALLING
        ),
        control = AIModelControl.SAMPLING,
        maxOutputTokens = 32_000
    )

    // Claude 4.6 Series (previous generation - kept as fallback)
    val CLAUDE_OPUS_4_6 = AIModel(
        id = "claude-opus-4-6",
        displayName = "Claude Opus 4.6",
        description = "Previous-generation flagship - 200K/1M context",
        provider = "Anthropic",
        pricing = "$5.00/$25.00 per 1M tokens",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING,
            AICapability.FUNCTION_CALLING
        ),
        control = AIModelControl.SAMPLING,
        maxOutputTokens = 64_000
    )

    val CLAUDE_SONNET_4_6 = AIModel(
        id = "claude-sonnet-4-6",
        displayName = "Claude Sonnet 4.6",
        description = "Previous-generation balanced model - 200K/1M context",
        provider = "Anthropic",
        pricing = "$3.00/$15.00 per 1M tokens",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING,
            AICapability.FUNCTION_CALLING
        ),
        control = AIModelControl.SAMPLING,
        maxOutputTokens = 64_000
    )

    // ============= XAI (GROK) MODELS =============

    val GROK_4 = AIModel(
        id = "grok-4-0709",
        displayName = "Grok 4",
        description = "xAI flagship model - advanced reasoning, coding, functions - 256K context",
        provider = "xAI",
        pricing = "Pay per token",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING,
            AICapability.FUNCTION_CALLING
        )
    )

    val GROK_4_FAST_REASONING = AIModel(
        id = "grok-4-fast-reasoning",
        displayName = "Grok 4 Fast Reasoning",
        description = "Fast reasoning with massive 2M token context window",
        provider = "xAI",
        pricing = "Pay per token",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING,
            AICapability.FUNCTION_CALLING
        )
    )

    val GROK_3 = AIModel(
        id = "grok-3",
        displayName = "Grok 3",
        description = "General-purpose model - 131K context",
        provider = "xAI",
        pricing = "Pay per token",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING,
            AICapability.FUNCTION_CALLING
        )
    )

    val GROK_3_MINI = AIModel(
        id = "grok-3-mini",
        displayName = "Grok 3 Mini",
        description = "Lightweight reasoning model - fast and cost-effective - 131K context",
        provider = "xAI",
        pricing = "Pay per token",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING,
            AICapability.FUNCTION_CALLING
        )
    )

    val GROK_CODE_FAST = AIModel(
        id = "grok-code-fast-1",
        displayName = "Grok Code Fast",
        description = "Coding specialist with reasoning - optimized for code generation - 256K context",
        provider = "xAI",
        pricing = "Pay per token",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING,
            AICapability.FUNCTION_CALLING
        )
    )

    // ============= GOOGLE GEMINI MODELS =============

    // Gemini 3.1 Series (Latest - February 2026)
    val GEMINI_3_PRO_PREVIEW = AIModel(
        id = "gemini-3.1-pro-preview",
        displayName = "Gemini 3.1 Pro",
        description = "Latest and most powerful Gemini model - top-tier reasoning, writing, planning, coding, multimodal understanding",
        provider = "Google AI",
        pricing = "FREE tier available",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING,
            AICapability.FUNCTION_CALLING
        )
    )

    // Gemini 2.5 Series (Latest Stable)
    val GEMINI_2_5_PRO = AIModel(
        id = "gemini-2.5-pro",
        displayName = "Gemini 2.5 Pro",
        description = "High-capability reasoning & coding - strong for complex codebases, algorithmic tasks, data/maths logic",
        provider = "Google AI",
        pricing = "FREE tier available",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING,
            AICapability.FUNCTION_CALLING
        )
    )

    val GEMINI_2_5_FLASH = AIModel(
        id = "gemini-2.5-flash",
        displayName = "Gemini 2.5 Flash",
        description = "Balanced - lower latency & snappier, supports coding & writing - good for general use and prototyping",
        provider = "Google AI",
        pricing = "FREE tier available",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING,
            AICapability.FUNCTION_CALLING
        )
    )

    val GEMINI_2_5_FLASH_LITE = AIModel(
        id = "gemini-2.5-flash-lite",
        displayName = "Gemini 2.5 Flash-Lite",
        description = "Lightweight and fastest - optimized for speed and shorter tasks",
        provider = "Google AI",
        pricing = "FREE tier available",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING,
            AICapability.FUNCTION_CALLING
        )
    )

    val ALL_MODELS = listOf(
        // Together.ai models (5 models)
        DEEPSEEK_R1_70B,
        LLAMA_3_3_70B,
        LLAMA_3_8B_LITE,
        LLAMA_3_1_8B_TURBO,
        QWEN_2_5_7B_TURBO,

        // OpenAI models (5 models)
        GPT_6_ASTRA,
        GPT_5_6_SOL,
        GPT_5_6_TERRA,
        GPT_5_6_LUNA,
        GPT_5_2,

        // Anthropic models (6 models)
        CLAUDE_FABLE_5_1,
        CLAUDE_OPUS_5,
        CLAUDE_SONNET_5,
        CLAUDE_HAIKU_4_5,
        CLAUDE_OPUS_4_6,
        CLAUDE_SONNET_4_6,

        // xAI (Grok) models (5 models)
        GROK_4,
        GROK_4_FAST_REASONING,
        GROK_3,
        GROK_3_MINI,
        GROK_CODE_FAST,

        // Google AI models (4 models)
        GEMINI_3_PRO_PREVIEW,
        GEMINI_2_5_PRO,
        GEMINI_2_5_FLASH,
        GEMINI_2_5_FLASH_LITE
    )
    
    val MODELS_BY_PROVIDER = ALL_MODELS.groupBy { it.provider }
}