package com.xraiassistant.data.models

/**
 * Names a favorite after the scene, not after its first line of code.
 *
 * Titles used to be code.lines().first(), which for a cart reads like "let S;".
 * The AI almost always names the scene in the prose before the code block, as a
 * markdown heading or a bold lead sentence, so that is tried first. Only when the
 * message has neither does it fall back to code: the first line that is not
 * blank or a comment, which is the rule the iOS app uses.
 */
object FavoriteTitle {
    const val MAX_LENGTH = 50
    const val UNTITLED = "Untitled Scene"

    private val heading = Regex("""^\s{0,3}#{1,6}\s+(.+?)\s*#*\s*$""", RegexOption.MULTILINE)
    private val boldLead = Regex("""^\s*\*\*(.+?)\*\*""")

    fun from(messageContent: String?, code: String): String {
        val prose = messageContent
            ?.substringBefore("```")
            ?.replace("[INSERT_CODE]", "")
            .orEmpty()

        heading.find(prose)?.groupValues?.get(1)?.let(::clean)?.takeIf { it.isNotEmpty() }
            ?.let { return clamp(it) }

        val firstLine = prose.lineSequence().firstOrNull { it.isNotBlank() }
        firstLine?.let { boldLead.find(it) }?.groupValues?.get(1)?.let(::clean)?.takeIf { it.isNotEmpty() }
            ?.let { return clamp(it) }

        return fromCode(code)
    }

    /** First line of code that is not blank or a comment; matches iOS generateFavoriteTitle. */
    fun fromCode(code: String): String =
        code.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() && !it.startsWith("//") && !it.startsWith("/*") && !it.startsWith("*") }
            ?.let(::clamp)
            ?: UNTITLED

    /** The title the old rule produced, used to find favorites saved before this fix. */
    fun legacy(code: String): String = code.lines().firstOrNull()?.take(MAX_LENGTH) ?: "Untitled"

    /** Drops inline markdown so the title reads as plain text. */
    private fun clean(text: String): String =
        text.replace(Regex("""[*_`~]"""), "")
            .replace(Regex("""\[([^\]]*)]\([^)]*\)"""), "$1")
            .trim()
            .trimEnd('.', ':', '!', ';', ',')
            .trim()

    /** Fits MAX_LENGTH, cutting at a word boundary where one is near. */
    private fun clamp(text: String): String {
        if (text.length <= MAX_LENGTH) return text
        val cut = text.take(MAX_LENGTH - 1)
        val space = cut.lastIndexOf(' ')
        return (if (space >= MAX_LENGTH / 2) cut.take(space) else cut).trimEnd() + "…"
    }
}
