package com.xraiassistant.data.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Together retires models; saved choices and the default must always be ones it still serves. */
class TogetherLineupTest {

    @Test
    fun defaultIsOffered() {
        assertTrue(AIModels.ALL_MODELS.any { it.id == ModelMigrations.DEFAULT_MODEL })
    }

    @Test
    fun retiredModelsAreGoneAndMigrated() {
        for ((old, new) in ModelMigrations.retired) {
            assertFalse(old, AIModels.ALL_MODELS.any { it.id == old })
            assertTrue(new, AIModels.ALL_MODELS.any { it.id == new })
            assertEquals(new, ModelMigrations.resolve(old))
        }
    }

    @Test
    fun currentAndLocalModelsAreKept() {
        assertEquals("zai-org/GLM-5.3", ModelMigrations.resolve("zai-org/GLM-5.3"))
        assertEquals("local:qwen", ModelMigrations.resolve("local:qwen"))
        assertEquals(ModelMigrations.DEFAULT_MODEL, ModelMigrations.resolve("something/unknown"))
    }
}
