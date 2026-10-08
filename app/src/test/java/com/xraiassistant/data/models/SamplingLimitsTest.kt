package com.xraiassistant.data.models

import org.junit.Assert.assertEquals
import org.junit.Test

/** GLM and Kimi reject top_p below 0.95; the app default of 0.9 must not reach them. */
class SamplingLimitsTest {

    @Test
    fun defaultsAreRaisedForGlmAndKimi() {
        for (model in listOf("zai-org/GLM-5.3", "zai-org/GLM-5.3-Flash", "moonshotai/Kimi-K3")) {
            assertEquals(model, 0.95, SamplingLimits.topP(0.9, model), 0.0)
            assertEquals(model, 1.0, SamplingLimits.temperature(1.5, model), 0.0)
        }
    }

    @Test
    fun otherModelsKeepTheUsersValues() {
        assertEquals(0.9, SamplingLimits.topP(0.9, "meta-llama/Meta-Llama-3.1-8B-Instruct-Turbo"), 0.0)
        assertEquals(1.5, SamplingLimits.temperature(1.5, "Qwen/Qwen3.8-Flash"), 0.0)
    }
}
