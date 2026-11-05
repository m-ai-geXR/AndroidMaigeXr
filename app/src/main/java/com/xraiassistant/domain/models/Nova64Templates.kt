package com.xraiassistant.domain.models

/** A tested Nova64 game engine the AI customises by rewriting its CONFIG. */
data class Nova64TemplateInfo(
    val id: String,
    val family: String,
    val genre: String,
    val style: String,
    val description: String,
    val guide: String,
    /** Example CONFIG, starting with its `// NOVA64 TEMPLATE: <id>` line. */
    val config: String
)

/**
 * Prompt side of the shared Nova64 cart kit (WebMaigeXr/nova64-kit).
 *
 * Picks a game template for a chat request and builds the GAME TEMPLATE MODE block.
 * The engines live in the kit inlined in playground-nova64.html, which splices the
 * AI's CONFIG in when the cart runs. Mirrors `chooseTemplate` in kit.src.js;
 * [Nova64TemplatesData.testVectors] (generated from the JS kit) keeps them in step.
 */
object Nova64Templates {
    private val optOut = Regex("""\b(from scratch|free-?form|without (a |the )?template|no template|custom engine)\b""")
    private val newThing = Regex("""\b(new|another|different)\b.*\b(scene|game|cart|demo)\b""")
    private val families = listOf(
        "dungeon" to Regex("""\b(dungeons?|crawlers?|rpg|wizardry|labyrinths?|mazes?|dungeon-crawler)\b"""),
        "runner" to Regex("""\b(runners?|endless run\w*|lanes?|subway|temple run)\b"""),
        "arena" to Regex("""\b(arenas?|dodgers?|dodging|dodge|bullet hell|top-down|survive waves|waves of)\b""")
    )
    private val neon = Regex("""\b(neon|cyber\w*|synth\w*|grid|digital|hacker|tron|data|electric|vapor\w*|laser|sci-?fi|space|holo\w*)\b""")
    private val classic = Regex("""\b(stone|torch\w*|caves?|cavern|crypts?|tomb|mines?|lava|medieval|castle|forest|corridor|ice|frost|crystal|fantasy)\b""")
    private val variants = mapOf(
        "dungeon" to mapOf("neon" to "fp-dungeon-odyssey", "classic" to "fp-dungeon-wizardry"),
        "runner" to mapOf("neon" to "lane-runner-grid", "classic" to "lane-runner-corridor"),
        "arena" to mapOf("neon" to "arena-dodger-neon", "classic" to "arena-dodger-cave")
    )
    private val defaultStyle = mapOf("dungeon" to "classic", "runner" to "neon", "arena" to "neon")
    const val CONFIG_MARK = "// ============================== CONFIG =============================="
    const val ENGINE_MARK = "// ============================== ENGINE =============================="
    private val templateLine = Regex("""//\s*NOVA64 TEMPLATE:\s*([a-z0-9-]+)""")

    val all: List<Nova64TemplateInfo> get() = Nova64TemplatesData.templates

    fun template(id: String): Nova64TemplateInfo? = all.firstOrNull { it.id == id }

    /** The template id named by a cart's `// NOVA64 TEMPLATE: <id>` line. */
    fun templateIdOf(code: String?): String? {
        val id = code?.let { templateLine.find(it)?.groupValues?.get(1) } ?: return null
        return if (template(id) != null) id else null
    }

    /** Which template a chat request should use, or null for free-form generation. */
    fun choose(message: String, currentTemplateId: String?): String? {
        val m = message.lowercase()
        if (optOut.containsMatchIn(m)) return null
        val family = families.firstOrNull { it.second.containsMatchIn(m) }?.first
        if (family == null) {
            return if (currentTemplateId != null && !newThing.containsMatchIn(m)) currentTemplateId else null
        }
        val isNeon = neon.containsMatchIn(m)
        val isClassic = classic.containsMatchIn(m)
        if (currentTemplateId != null && template(currentTemplateId)?.family == family && !isNeon && !isClassic) {
            return currentTemplateId
        }
        val style = when {
            isNeon && !isClassic -> "neon"
            isClassic && !isNeon -> "classic"
            else -> defaultStyle.getValue(family)
        }
        return variants.getValue(family).getValue(style)
    }

    /** The CONFIG section of a cart, from its NOVA64 TEMPLATE line to the ENGINE marker. */
    fun configOf(code: String): String? {
        var start = Regex("""//\s*NOVA64 TEMPLATE:""").find(code)?.range?.first ?: code.indexOf(CONFIG_MARK)
        if (start < 0) return null
        val end = code.indexOf(ENGINE_MARK, start).let { if (it < 0) code.length else it }
        return code.substring(start, end).trim() + "\n"
    }

    /** The GAME TEMPLATE MODE block for a request, or null when it should be free-form. */
    fun promptBlock(message: String, currentCode: String?): String? {
        val current = templateIdOf(currentCode)
        val id = choose(message, current) ?: return null
        val t = template(id) ?: return null
        val config = (if (current == id) currentCode?.let { configOf(it) } else null) ?: t.config
        return Nova64TemplatesData.promptTemplate
            .replace("{genre}", t.genre)
            .replace("{style}", t.style)
            .replace("{description}", t.description)
            .replace("{guide}", t.guide)
            .replace("{id}", id)
            .replace("{config}", config.trim())
    }

    /** Each example is only a CONFIG; the playground kit adds the tested engine. */
    val examples: List<CodeExample>
        get() = all.map { t ->
            CodeExample(
                id = "nova64-template-${t.id}",
                title = "${t.genre}: ${t.style}",
                description = "Game template. ${t.description} Describe your own version in the chat to restyle it.",
                code = t.config,
                category = ExampleCategory.ADVANCED,
                difficulty = ExampleDifficulty.INTERMEDIATE,
                keywords = listOf("game", "template", t.family, t.genre.lowercase(), t.style.lowercase()),
                aiPromptHints = "This is a game template: rewrite only its CONFIG; the engine is added automatically."
            )
        }
}
