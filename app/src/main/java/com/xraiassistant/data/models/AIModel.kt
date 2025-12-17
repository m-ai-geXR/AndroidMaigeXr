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
    val capabilities: Set<AICapability> = emptySet()
)

enum class AICapability {
    TEXT_GENERATION,
    CODE_GENERATION,
    STREAMING,
    FUNCTION_CALLING
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

    // GPT-5.2 Series (Latest - December 2025)
    val GPT_5_2 = AIModel(
        id = "gpt-5.2",
        displayName = "GPT-5.2",
        description = "Best model for coding and agentic tasks - 400K context",
        provider = "OpenAI",
        pricing = "$1.75/$14.00 per 1M tokens",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING,
            AICapability.FUNCTION_CALLING
        )
    )

    val GPT_5_2_PRO = AIModel(
        id = "gpt-5.2-pro",
        displayName = "GPT-5.2 Pro",
        description = "Smartest and most trustworthy - highest accuracy - 400K context",
        provider = "OpenAI",
        pricing = "Premium tier",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING,
            AICapability.FUNCTION_CALLING
        )
    )

    val GPT_5_2_CHAT_LATEST = AIModel(
        id = "gpt-5.2-chat-latest",
        displayName = "GPT-5.2 Chat (Latest)",
        description = "Latest ChatGPT model - automatically updates - 128K context",
        provider = "OpenAI",
        pricing = "$1.75/$14.00 per 1M tokens",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING,
            AICapability.FUNCTION_CALLING
        )
    )

    // o-series Reasoning Models (2024-2025)
    val O1 = AIModel(
        id = "o1-2024-12-17",
        displayName = "o1",
        description = "Advanced reasoning model for complex problems",
        provider = "OpenAI",
        pricing = "Premium tier",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING,
            AICapability.FUNCTION_CALLING
        )
    )

    val O3_MINI = AIModel(
        id = "o3-mini-2025-01-31",
        displayName = "o3-mini",
        description = "Latest reasoning model with enhanced reasoning abilities",
        provider = "OpenAI",
        pricing = "Economy tier",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING,
            AICapability.FUNCTION_CALLING
        )
    )

    // GPT-4o Series (Still Supported)
    val GPT_4O = AIModel(
        id = "gpt-4o",
        displayName = "GPT-4o",
        description = "Versatile high-intelligence flagship model - text and image inputs",
        provider = "OpenAI",
        pricing = "$2.50/$10.00 per 1M tokens",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING,
            AICapability.FUNCTION_CALLING
        )
    )

    val GPT_4O_MINI = AIModel(
        id = "gpt-4o-mini",
        displayName = "GPT-4o Mini",
        description = "Fast and affordable small model for focused tasks",
        provider = "OpenAI",
        pricing = "$0.15/$0.60 per 1M tokens",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING,
            AICapability.FUNCTION_CALLING
        )
    )
    
    // ============= ANTHROPIC CLAUDE MODELS =============

    // Claude 4.5 Series (Latest - 2025)
    val CLAUDE_SONNET_4_5 = AIModel(
        id = "claude-sonnet-4-5-20250929",
        displayName = "Claude Sonnet 4.5",
        description = "Smartest model for complex agents and coding - 200K context",
        provider = "Anthropic",
        pricing = "$3.00/$15.00 per 1M tokens",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING,
            AICapability.FUNCTION_CALLING
        )
    )

    val CLAUDE_HAIKU_4_5 = AIModel(
        id = "claude-haiku-4-5-20251001",
        displayName = "Claude Haiku 4.5",
        description = "Fastest model with near-frontier intelligence - 200K context",
        provider = "Anthropic",
        pricing = "$0.25/$1.25 per 1M tokens",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING,
            AICapability.FUNCTION_CALLING
        )
    )

    // Claude 4.1 Series
    val CLAUDE_OPUS_4_1 = AIModel(
        id = "claude-opus-4-1-20250805",
        displayName = "Claude Opus 4.1",
        description = "Exceptional model for specialized reasoning tasks - 200K context",
        provider = "Anthropic",
        pricing = "$15.00/$75.00 per 1M tokens",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING,
            AICapability.FUNCTION_CALLING
        )
    )

    // Claude 4 Series (May 2025)
    val CLAUDE_SONNET_4 = AIModel(
        id = "claude-sonnet-4-20250514",
        displayName = "Claude Sonnet 4",
        description = "Previous Sonnet 4 version - 200K context",
        provider = "Anthropic",
        pricing = "$3.00/$15.00 per 1M tokens",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING,
            AICapability.FUNCTION_CALLING
        )
    )

    val CLAUDE_OPUS_4 = AIModel(
        id = "claude-opus-4-20250514",
        displayName = "Claude Opus 4",
        description = "Previous Opus 4 version - 200K context",
        provider = "Anthropic",
        pricing = "$15.00/$75.00 per 1M tokens",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING,
            AICapability.FUNCTION_CALLING
        )
    )

    // Claude 3.5 Series (Legacy - 2024)
    val CLAUDE_3_5_SONNET_OCT = AIModel(
        id = "claude-3-5-sonnet-20241022",
        displayName = "Claude 3.5 Sonnet (Oct 2024)",
        description = "Previous generation high-performance model",
        provider = "Anthropic",
        pricing = "$3.00/$15.00 per 1M tokens",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING,
            AICapability.FUNCTION_CALLING
        )
    )

    val CLAUDE_3_5_SONNET_JUNE = AIModel(
        id = "claude-3-5-sonnet-20240620",
        displayName = "Claude 3.5 Sonnet (June 2024)",
        description = "Earlier 3.5 Sonnet version",
        provider = "Anthropic",
        pricing = "$3.00/$15.00 per 1M tokens",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING,
            AICapability.FUNCTION_CALLING
        )
    )

    val CLAUDE_3_5_HAIKU = AIModel(
        id = "claude-3-5-haiku-20241022",
        displayName = "Claude 3.5 Haiku",
        description = "Fast and affordable legacy model",
        provider = "Anthropic",
        pricing = "$0.25/$1.25 per 1M tokens",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING,
            AICapability.FUNCTION_CALLING
        )
    )

    // Claude 3 Series (Legacy - Early 2024)
    val CLAUDE_3_OPUS = AIModel(
        id = "claude-3-opus-20240229",
        displayName = "Claude 3 Opus",
        description = "Original powerful model",
        provider = "Anthropic",
        pricing = "$15.00/$75.00 per 1M tokens",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING,
            AICapability.FUNCTION_CALLING
        )
    )

    val CLAUDE_3_SONNET = AIModel(
        id = "claude-3-sonnet-20240229",
        displayName = "Claude 3 Sonnet",
        description = "Balanced legacy model",
        provider = "Anthropic",
        pricing = "$3.00/$15.00 per 1M tokens",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING,
            AICapability.FUNCTION_CALLING
        )
    )

    val CLAUDE_3_HAIKU = AIModel(
        id = "claude-3-haiku-20240307",
        displayName = "Claude 3 Haiku",
        description = "Fastest legacy model",
        provider = "Anthropic",
        pricing = "$0.25/$1.25 per 1M tokens",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING,
            AICapability.FUNCTION_CALLING
        )
    )

    // ============= GOOGLE GEMINI MODELS =============

    // Gemini 3 Series (Newest & Most Powerful)
    val GEMINI_3_PRO_PREVIEW = AIModel(
        id = "gemini-3-pro-preview",
        displayName = "Gemini 3 Pro",
        description = "Newest and most powerful general-purpose model - top-tier reasoning, writing, planning, coding, multimodal understanding",
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

        // OpenAI models (8 models - Updated December 2025)
        GPT_5_2,
        GPT_5_2_PRO,
        GPT_5_2_CHAT_LATEST,
        O1,
        O3_MINI,
        GPT_4O,
        GPT_4O_MINI,

        // Anthropic models (12 models)
        CLAUDE_SONNET_4_5,
        CLAUDE_HAIKU_4_5,
        CLAUDE_OPUS_4_1,
        CLAUDE_SONNET_4,
        CLAUDE_OPUS_4,
        CLAUDE_3_5_SONNET_OCT,
        CLAUDE_3_5_SONNET_JUNE,
        CLAUDE_3_5_HAIKU,
        CLAUDE_3_OPUS,
        CLAUDE_3_SONNET,
        CLAUDE_3_HAIKU,

        // Google AI models (4 models)
        GEMINI_3_PRO_PREVIEW,
        GEMINI_2_5_PRO,
        GEMINI_2_5_FLASH,
        GEMINI_2_5_FLASH_LITE
    )
    
    val MODELS_BY_PROVIDER = ALL_MODELS.groupBy { it.provider }
}