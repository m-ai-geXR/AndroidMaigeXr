package com.xraiassistant.data.models

/**
 * One line of plain text from an AI reply, for list rows: the prose before any
 * code, without markdown, collapsed to a single line.
 */
object MessagePreview {
    const val MAX_LENGTH = 160

    /** A preview that does not repeat [droppingTitle] as its opening words. */
    fun from(content: String?, droppingTitle: String?): String? {
        val preview = from(content) ?: return null
        if (droppingTitle.isNullOrEmpty() || !preview.startsWith(droppingTitle, ignoreCase = true)) return preview
        return preview.drop(droppingTitle.length)
            .dropWhile { it.isWhitespace() || it in ".:!—-–" }
            .ifEmpty { null }
    }

    fun from(content: String?): String? {
        val prose = content
            ?.substringBefore("```")
            ?.replace("[INSERT_CODE]", "")
            ?.replace(Regex("""\[([^\]]*)]\([^)]*\)"""), "$1")
            ?.replace(Regex("""(?m)^\s{0,3}#{1,6}\s+"""), "")
            ?.replace(Regex("""[*_`~>]"""), "")
            ?.replace(Regex("""\s+"""), " ")
            ?.trim()
            .orEmpty()
        if (prose.isEmpty()) return null
        return if (prose.length <= MAX_LENGTH) prose else prose.take(MAX_LENGTH - 1).trimEnd() + "…"
    }
}
