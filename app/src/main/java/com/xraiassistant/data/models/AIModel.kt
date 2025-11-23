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
    val DEEPSEEK_R1 = AIModel(
        id = "deepseek-ai/DeepSeek-R1",
        displayName = "DeepSeek R1",
        description = "Advanced reasoning & coding (serverless)",
        provider = "Together.ai",
        pricing = "Serverless pricing",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING
        )
    )
    
    val LLAMA_3_3_70B = AIModel(
        id = "meta-llama/Llama-3.3-70B-Instruct-Turbo",
        displayName = "Llama 3.3 70B",
        description = "Latest Meta large model",
        provider = "Together.ai",
        pricing = "FREE",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING
        )
    )
    
    val LLAMA_3_8B_LITE = AIModel(
        id = "meta-llama/Llama-3-8b-chat-hf",
        displayName = "Llama 3 8B Lite",
        description = "Cost-effective option",
        provider = "Together.ai",
        pricing = "$0.10/1M",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION
        )
    )
    
    val QWEN_2_5_7B_TURBO = AIModel(
        id = "Qwen/Qwen2.5-7B-Instruct-Turbo",
        displayName = "Qwen 2.5 7B Turbo",
        description = "Fast coding specialist",
        provider = "Together.ai",
        pricing = "$0.30/1M",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING
        )
    )
    
    // REPLACED: Qwen 2.5 Coder 32B is NOT serverless (requires dedicated endpoint)
    // Using serverless alternative: Qwen3 Coder 480B
    val QWEN_3_CODER_480B = AIModel(
        id = "Qwen/Qwen3-Coder-480B-A35B-Instruct-FP8",
        displayName = "Qwen3 Coder 480B",
        description = "Advanced serverless coding model",
        provider = "Together.ai",
        pricing = "Serverless pricing",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING
        )
    )

    // Alternative: Arcee Coder (serverless)
    val ARCEE_CODER = AIModel(
        id = "arcee-ai/coder-large",
        displayName = "Arcee Coder Large",
        description = "Serverless code generation specialist",
        provider = "Together.ai",
        pricing = "Serverless pricing",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING
        )
    )
    
    val GPT_4O = AIModel(
        id = "gpt-4o",
        displayName = "GPT-4o",
        description = "OpenAI's latest model",
        provider = "OpenAI",
        pricing = "$5.00/1M",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING,
            AICapability.FUNCTION_CALLING
        )
    )
    
    // ============= ANTHROPIC CLAUDE MODELS (2025) =============

    val CLAUDE_SONNET_4_5 = AIModel(
        id = "claude-sonnet-4-5-20250929",
        displayName = "Claude Sonnet 4.5",
        description = "Latest Anthropic model - 200K context, extended thinking",
        provider = "Anthropic",
        pricing = "$3/$15 per 1M tokens",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING,
            AICapability.FUNCTION_CALLING
        )
    )

    val CLAUDE_OPUS_4_1 = AIModel(
        id = "claude-opus-4-1-20250805",
        displayName = "Claude Opus 4.1",
        description = "Most powerful Claude model - Complex reasoning & analysis",
        provider = "Anthropic",
        pricing = "$15/$75 per 1M tokens",
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
        description = "Fast & cost-effective - Great for quick tasks",
        provider = "Anthropic",
        pricing = "$1/$5 per 1M tokens",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING
        )
    )

    // Legacy model (kept for backward compatibility)
    @Deprecated("Use CLAUDE_SONNET_4_5 instead", ReplaceWith("CLAUDE_SONNET_4_5"))
    val CLAUDE_3_5_SONNET = AIModel(
        id = "claude-3-5-sonnet-20241022",
        displayName = "Claude 3.5 Sonnet (Legacy)",
        description = "Previous generation model",
        provider = "Anthropic",
        pricing = "$3.00/1M",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING
        )
    )

    // ============= GOOGLE GEMINI MODELS (2025) =============

    // Gemini 3.0 Models (Latest Generation)
    val GEMINI_3_0_PRO = AIModel(
        id = "gemini-3.0-pro",
        displayName = "Gemini 3.0 Pro",
        description = "Next-gen flagship model - Advanced reasoning, 2M context",
        provider = "Google",
        pricing = "Premium",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING,
            AICapability.FUNCTION_CALLING
        )
    )

    val GEMINI_3_0_FLASH = AIModel(
        id = "gemini-3.0-flash",
        displayName = "Gemini 3.0 Flash",
        description = "Next-gen fast model - 2M context, optimized for speed",
        provider = "Google",
        pricing = "Standard",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING,
            AICapability.FUNCTION_CALLING
        )
    )

    // Gemini 2.5 Models (Current Generation)
    val GEMINI_2_5_PRO = AIModel(
        id = "gemini-2.5-pro",
        displayName = "Gemini 2.5 Pro",
        description = "State-of-the-art thinking model - 1M context, reasoning over code, math & STEM",
        provider = "Google",
        pricing = "Check pricing",
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
        description = "Best price-performance - 1M context, large-scale processing",
        provider = "Google",
        pricing = "Best value",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING,
            AICapability.FUNCTION_CALLING
        )
    )

    val GEMINI_2_5_FLASH_LITE = AIModel(
        id = "gemini-2.5-flash-lite",
        displayName = "Gemini 2.5 Flash Lite",
        description = "Fastest flash model - 1M context, optimized for cost-efficiency & high throughput",
        provider = "Google",
        pricing = "Most affordable",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING,
            AICapability.FUNCTION_CALLING
        )
    )

    val ALL_MODELS = listOf(
        DEEPSEEK_R1,              // Serverless reasoning model
        LLAMA_3_3_70B,            // Serverless, recommended for chat
        LLAMA_3_8B_LITE,
        QWEN_2_5_7B_TURBO,        // Serverless Turbo model
        QWEN_3_CODER_480B,        // Serverless coding model (replaces 32B)
        ARCEE_CODER,              // Serverless coding specialist
        GPT_4O,
        CLAUDE_SONNET_4_5,        // Latest Claude model (recommended)
        CLAUDE_OPUS_4_1,          // Most powerful Claude
        CLAUDE_HAIKU_4_5,         // Fastest/cheapest Claude
        GEMINI_3_0_PRO,           // Next-gen Gemini Pro
        GEMINI_3_0_FLASH,         // Next-gen Gemini Flash
        GEMINI_2_5_PRO,           // Latest Gemini 2.5 - state-of-the-art thinking
        GEMINI_2_5_FLASH,         // Best Gemini 2.5 price-performance
        GEMINI_2_5_FLASH_LITE     // Fastest & most affordable Gemini 2.5
    )
    
    val MODELS_BY_PROVIDER = ALL_MODELS.groupBy { it.provider }
}