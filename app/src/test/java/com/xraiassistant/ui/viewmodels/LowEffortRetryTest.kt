package com.xraiassistant.ui.viewmodels

import com.xraiassistant.data.models.AIEffort
import com.xraiassistant.data.models.AIModels
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** GLM thinks and answers from one budget; an empty GLM reply gets one retry at low effort. */
class LowEffortRetryTest {

    @Test
    fun glmEmptyRepliesRetryOnceAtLowEffort() {
        assertTrue(shouldRetryWithLowEffort("zai-org/GLM-5.3", AIEffort.HIGH, alreadyRetried = false))
        assertFalse("only once", shouldRetryWithLowEffort("zai-org/GLM-5.3", AIEffort.HIGH, alreadyRetried = true))
        assertFalse("medium already maps to GLM low", shouldRetryWithLowEffort("zai-org/GLM-5.3", AIEffort.MEDIUM, false))
        assertFalse("only GLM", shouldRetryWithLowEffort("moonshotai/Kimi-K3", AIEffort.HIGH, false))
    }

    @Test
    fun glmHasRoomToThinkAndAnswer() {
        val glm = AIModels.ALL_MODELS.filter { it.id.startsWith("zai-org/GLM-5.3") }
        assertEquals(2, glm.size)
        assertTrue(glm.all { it.maxOutputTokens >= 65_536 })
    }
}
