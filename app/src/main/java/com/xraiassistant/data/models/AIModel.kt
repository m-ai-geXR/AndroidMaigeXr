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

    val GEMINI_2_0_FLASH_EXP = AIModel(
        id = "gemini-2.0-flash-exp",
        displayName = "Gemini 2.0 Flash (Experimental)",
        description = "Latest experimental model - Fast & multimodal",
        provider = "Google",
        pricing = "Free (experimental)",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING
        )
    )

    val GEMINI_1_5_PRO = AIModel(
        id = "gemini-1.5-pro",
        displayName = "Gemini 1.5 Pro",
        description = "Advanced reasoning & 2M context window",
        provider = "Google",
        pricing = "$3.50/$10.50 per 1M tokens",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING,
            AICapability.FUNCTION_CALLING
        )
    )

    val GEMINI_1_5_FLASH = AIModel(
        id = "gemini-1.5-flash",
        displayName = "Gemini 1.5 Flash",
        description = "Fast & cost-effective - 1M context",
        provider = "Google",
        pricing = "$0.075/$0.30 per 1M tokens",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING
        )
    )

    val GEMINI_1_5_FLASH_8B = AIModel(
        id = "gemini-1.5-flash-8b",
        displayName = "Gemini 1.5 Flash 8B",
        description = "Ultra-fast lightweight model",
        provider = "Google",
        pricing = "$0.0375/$0.15 per 1M tokens",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING
        )
    )

    // ============= GOOGLE GEMINI 3 MODELS (2025) =============

    val GEMINI_3_PRO = AIModel(
        id = "gemini-3-pro-preview",
        displayName = "Gemini 3 Pro (Preview)",
        description = "Latest model - 1M context, advanced reasoning & agentic workflows",
        provider = "Google",
        pricing = "$2/$12 per 1M tokens",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING,
            AICapability.FUNCTION_CALLING
        )
    )

    val GEMINI_3_PRO_IMAGE = AIModel(
        id = "gemini-3-pro-image-preview",
        displayName = "Gemini 3 Pro Image (Preview)",
        description = "Image generation with reasoning - 65K context",
        provider = "Google",
        pricing = "$2 text / $0.134 image per 1M tokens",
        capabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CODE_GENERATION,
            AICapability.STREAMING
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
        GEMINI_3_PRO,             // Latest Gemini 3 with advanced reasoning
        GEMINI_3_PRO_IMAGE,       // Gemini 3 with image generation
        GEMINI_2_0_FLASH_EXP,     // Latest Gemini experimental
        GEMINI_1_5_PRO,           // Best Gemini for complex tasks
        GEMINI_1_5_FLASH,         // Fast Gemini
        GEMINI_1_5_FLASH_8B       // Fastest/cheapest Gemini
    )
    
    val MODELS_BY_PROVIDER = ALL_MODELS.groupBy { it.provider }
}