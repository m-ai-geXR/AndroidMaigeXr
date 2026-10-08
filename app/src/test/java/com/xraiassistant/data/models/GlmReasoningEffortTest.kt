package com.xraiassistant.data.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** GLM thinks for minutes at its default depth; requests must carry a lighter effort. */
class GlmReasoningEffortTest {

    @Test
    fun glmGetsALighterEffort() {
        assertEquals("medium", TogetherReasoning.effort("zai-org/GLM-5.3", AIEffort.HIGH))
        assertEquals("low", TogetherReasoning.effort("zai-org/GLM-5.3-Flash", AIEffort.MEDIUM))
        assertEquals("max", TogetherReasoning.effort("zai-org/GLM-5.3", AIEffort.MAX))
        assertNull(TogetherReasoning.effort("moonshotai/Kimi-K3", AIEffort.HIGH))
    }

    @Test
    fun glmModelsUseTheEffortControl() {
        for (id in listOf("zai-org/GLM-5.3", "zai-org/GLM-5.3-Flash")) {
            assertEquals(id, AIModelControl.EFFORT, AIModels.ALL_MODELS.first { it.id == id }.control)
        }
    }
}
