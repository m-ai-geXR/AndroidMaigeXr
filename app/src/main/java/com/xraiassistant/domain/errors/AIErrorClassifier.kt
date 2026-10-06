package com.xraiassistant.domain.errors

/**
 * Turning provider failures into messages a person can act on.
 *
 * Errors used to be matched loosely against the exception text and, when
 * nothing matched, shown raw: "Failed to get response: <stack message>". That
 * tells the user nothing actionable and can surface a response body.
 *
 * The taxonomy here matches the web and iOS clients, so all three say the same
 * thing about the same failure.
 */
enum class AIErrorCategory {
    MISSING_API_KEY,
    INVALID_API_KEY,
    ACCESS_DENIED,
    RATE_LIMITED,
    QUOTA_EXCEEDED,
    MODEL_UNAVAILABLE,
    CONTEXT_TOO_LONG,
    SERVER_ERROR,
    TIMEOUT,
    OFFLINE,
    EMPTY_RESPONSE,
    UNKNOWN
}

/**
 * @property title short, for the heading
 * @property message what happened, with no status codes or response bodies
 * @property action the next thing to try; never empty, because a dead end is
 *   not a useful error
 * @property retryable whether repeating the same request could plausibly work
 */
data class AIErrorInfo(
    val category: AIErrorCategory,
    val title: String,
    val message: String,
    val action: String,
    val retryable: Boolean
) {
    /** One line, for a snackbar. */
    fun asLine(): String = "$title. $action"

    /** Chat bubble: what happened, then what to do. */
    fun asMessage(): String = "$title\n\n$message\n\n$action"
}

/** Carries the HTTP status so classification reads a number, not prose. */
class AIProviderException(
    val provider: String,
    val status: Int? = null,
    val providerMessage: String? = null,
    cause: Throwable? = null
) : Exception("$provider request failed${status?.let { " ($it)" } ?: ""}", cause)

object AIErrorClassifier {

    private const val SETTINGS_HINT = "Open Settings to check your API key."
    private const val DEFAULT_PROVIDER = "The AI provider"

    fun classify(error: Throwable?, providerName: String = DEFAULT_PROVIDER): AIErrorInfo {
        if (error is AIProviderException) {
            val provider = error.provider.ifBlank { providerName }
            error.status?.let { status -> byStatus(status, provider)?.let { return it } }
            error.providerMessage?.let { text -> byMessage(text, provider)?.let { return it } }
            return unknown(provider, error.status)
        }

        val text = error?.message.orEmpty()
        if (text.isNotBlank()) {
            byMessage(text, providerName)?.let { return it }
            // A status embedded in prose, from throw sites that predate the
            // typed exception.
            Regex("\\b([45]\\d{2})\\b").find(text)?.let { match ->
                byStatus(match.groupValues[1].toInt(), providerName)?.let { return it }
            }
        }
        return unknown(providerName, null)
    }

    private fun byStatus(status: Int, provider: String): AIErrorInfo? = when {
        status == 401 -> AIErrorInfo(
            AIErrorCategory.INVALID_API_KEY,
            "API key rejected",
            "$provider did not accept your API key. It may be mistyped, revoked, or from a different provider.",
            "$SETTINGS_HINT Keys often pick up a stray space when copied.",
            retryable = false
        )
        status == 403 -> AIErrorInfo(
            AIErrorCategory.ACCESS_DENIED,
            "Access denied",
            "Your $provider key is valid but is not allowed to use this model.",
            "Pick a different model, or enable access for this one in your provider dashboard.",
            retryable = false
        )
        status == 402 -> AIErrorInfo(
            AIErrorCategory.QUOTA_EXCEEDED,
            "Out of credit",
            "Your $provider account has no remaining balance for this request.",
            "Add credit in your provider dashboard, or switch to a free model.",
            retryable = false
        )
        status == 404 -> AIErrorInfo(
            AIErrorCategory.MODEL_UNAVAILABLE,
            "Model unavailable",
            "$provider does not currently offer the selected model.",
            "Choose another model in Settings.",
            retryable = false
        )
        status == 429 -> AIErrorInfo(
            AIErrorCategory.RATE_LIMITED,
            "Too many requests",
            "$provider is rate limiting you, usually from sending several requests in quick succession.",
            "Wait a few seconds and try again. Free tiers have tighter limits.",
            retryable = true
        )
        status >= 500 -> AIErrorInfo(
            AIErrorCategory.SERVER_ERROR,
            "$provider is having trouble",
            "The provider returned a server error. This is on their side, not yours.",
            "Try again shortly, or switch provider in Settings.",
            retryable = true
        )
        else -> null
    }

    private fun byMessage(text: String, provider: String): AIErrorInfo? {
        val t = text.lowercase()

        // "api key", "apikey" and "api_key" all appear across the clients.
        val mentionsKey = t.contains("api key") || t.contains("apikey") || t.contains("api_key")
        if (mentionsKey && (t.contains("not configured") || t.contains("required") || t.contains("changeme"))) {
            return AIErrorInfo(
                AIErrorCategory.MISSING_API_KEY,
                "API key needed",
                "$provider needs an API key before it can answer.",
                SETTINGS_HINT,
                retryable = false
            )
        }
        if (t.contains("context length") || t.contains("too many tokens") || t.contains("maximum context")) {
            return AIErrorInfo(
                AIErrorCategory.CONTEXT_TOO_LONG,
                "Conversation too long",
                "This conversation has outgrown what the model can read at once.",
                "Start a new conversation, or switch to a model with a larger context window.",
                retryable = false
            )
        }
        if (t.contains("timed out") || t.contains("timeout")) {
            return AIErrorInfo(
                AIErrorCategory.TIMEOUT,
                "Request timed out",
                "The model took too long to respond. Reasoning models can think for over a minute on hard scenes.",
                "Try again, or lower the reasoning effort in Settings for a faster answer.",
                retryable = true
            )
        }
        if (
            t.contains("unable to resolve host") || t.contains("unknownhost") ||
            t.contains("failed to connect") || t.contains("cannot reach") ||
            t.contains("no address associated") || t.contains("network is unreachable") ||
            t.contains("ssl")
        ) {
            return AIErrorInfo(
                AIErrorCategory.OFFLINE,
                "No connection",
                "Could not reach $provider.",
                "Check your internet connection and try again.",
                retryable = true
            )
        }
        if (t.contains("empty response") || t.contains("no response body")) {
            return AIErrorInfo(
                AIErrorCategory.EMPTY_RESPONSE,
                "Empty response",
                "$provider accepted the request but returned nothing.",
                "Try again. If it keeps happening, switch model or provider.",
                retryable = true
            )
        }
        return null
    }

    private fun unknown(provider: String, status: Int?): AIErrorInfo = AIErrorInfo(
        AIErrorCategory.UNKNOWN,
        "Something went wrong",
        if (status != null) "$provider returned an unexpected error (status $status)."
        else "$provider returned an unexpected error.",
        "Try again. If it keeps happening, switch model or provider in Settings.",
        retryable = true
    )
}
