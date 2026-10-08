package com.xraiassistant.domain.text

/**
 * What a reply shows: reasoning models such as DeepSeek R1 wrap their thinking
 * in <think>…</think>, which is not part of the answer.
 */
object ReplyText {
    data class Visible(val text: String, val isThinking: Boolean)

    /** The answer without thinking, and whether a <think> block is still open. */
    fun visible(raw: String): Visible {
        var text = raw
        var thinking = false
        while (true) {
            val open = text.indexOf("<think>")
            if (open < 0) break
            val close = text.indexOf("</think>", open)
            if (close < 0) {
                text = text.substring(0, open)
                thinking = true
                break
            }
            text = text.removeRange(open, close + "</think>".length)
        }
        // Some servers omit the opening tag and send only the closing one.
        val strayClose = text.indexOf("</think>")
        if (strayClose >= 0) text = text.substring(strayClose + "</think>".length)
        return Visible(text.trim(), thinking)
    }
}
