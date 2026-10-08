package com.xraiassistant.data.models

/**
 * Some models accept only part of the temperature / top_p range the Settings
 * sliders offer and answer anything else with a 400. GLM and Kimi on Together
 * require top_p between 0.95 and 1, while the app default is 0.9. Values are
 * pulled into each model's range just before sending.
 */
object SamplingLimits {
    data class Range(
        val temperature: ClosedFloatingPointRange<Double>,
        val topP: ClosedFloatingPointRange<Double>
    )

    val standard = Range(0.0..2.0, 0.01..1.0)

    fun rangeFor(model: String): Range {
        val id = model.lowercase()
        val strict = id.startsWith("zai-org/") || id.contains("glm-") ||
            id.startsWith("moonshotai/") || id.contains("kimi-")
        return if (strict) Range(0.0..1.0, 0.95..1.0) else standard
    }

    fun temperature(value: Double, model: String) = value.coerceIn(rangeFor(model).temperature)

    fun topP(value: Double, model: String) = value.coerceIn(rangeFor(model).topP)
}
