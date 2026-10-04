package com.xraiassistant.ui.components

import com.xraiassistant.data.models.ChatMessage

/**
 * Code extraction and Run Scene gating for chat messages.
 *
 * Both lived as private copies inside ChatMessageCard and ThreadedMessageView,
 * which let them drift. They are pure string logic, so keeping them here makes
 * them unit testable without Compose.
 */

/** Fence languages we accept, longest first so "```js" cannot shadow "```jsx". */
private val FENCE_LANGUAGES = listOf(
    "javascript", "typescript", "jsx", "tsx", "html", "js", "ts", ""
)

/** Trailing markers the models sometimes leave inside the fence. */
private val TRAILING_ARTIFACTS = listOf("[/INSERT_CODE]", "[RUN_SCENE]", "```")

/** Shortest block we treat as runnable. Below this it is a fragment, not a scene. */
private const val MIN_RUNNABLE_LENGTH = 10

/**
 * Extracts the runnable code block from a message, or null if there is none.
 *
 * Only *closed* fences count. A fence that is still open is a response mid
 * flight, not a block to run.
 *
 * When a message holds several blocks — models often show a small illustrative
 * snippet before the real scene — the longest one wins. The previous
 * implementation returned the first match, so Run Scene could run a two line
 * example instead of the scene underneath it.
 */
fun extractCodeFromMessage(content: String): String? =
    closedFencedBlocks(content)
        .map(::stripTrailingArtifacts)
        .filter { it.length >= MIN_RUNNABLE_LENGTH }
        .maxByOrNull { it.length }

/**
 * Whether a message should offer Run Scene.
 *
 * The button used to render for every assistant message, with [extractCodeFromMessage]
 * only choosing its colour, so an empty streaming placeholder showed a play
 * button before a single token had arrived. It now requires finished, runnable
 * content.
 */
fun shouldShowRunScene(message: ChatMessage, hasRunSceneHandler: Boolean): Boolean {
    if (!hasRunSceneHandler) return false
    if (message.isUser) return false
    if (message.isWelcomeMessage) return false
    if (message.isStreaming) return false
    return extractCodeFromMessage(message.content) != null
}

/** Every closed ``` fence in order, with its language tag removed. */
private fun closedFencedBlocks(content: String): List<String> {
    val blocks = mutableListOf<String>()
    var searchFrom = 0

    while (true) {
        val open = content.indexOf("```", searchFrom)
        if (open == -1) break

        val afterOpen = open + 3
        val close = content.indexOf("```", afterOpen)
        // No closing fence: the response is still streaming. Stop, do not guess.
        if (close == -1) break

        blocks += content.substring(afterOpen, close).stripLanguageTag().trim()
        searchFrom = close + 3
    }

    return blocks
}

/**
 * Drops a leading language tag. The tag is only ever the remainder of the
 * opening fence line, so anything after the first newline is code regardless.
 */
private fun String.stripLanguageTag(): String {
    val firstLineEnd = indexOf('\n')
    val firstLine = if (firstLineEnd == -1) this else substring(0, firstLineEnd)
    val tag = firstLine.trim().lowercase()

    val isLanguageTag = tag.isNotEmpty() &&
        FENCE_LANGUAGES.contains(tag) &&
        firstLineEnd != -1

    return if (isLanguageTag) substring(firstLineEnd + 1) else this
}

private fun stripTrailingArtifacts(code: String): String {
    var result = code.trim()
    var changed = true
    while (changed) {
        changed = false
        for (artifact in TRAILING_ARTIFACTS) {
            if (result.endsWith(artifact)) {
                result = result.dropLast(artifact.length).trim()
                changed = true
            }
        }
    }
    return result
}
