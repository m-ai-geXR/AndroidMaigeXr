package com.xraiassistant.data.local

import com.xraiassistant.data.models.AIModels
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The picker should offer what the user Together key can actually use. */
class TogetherModelCatalogTest {

    private val sample = """
    [
      {"id":"zai-org/GLM-5.3-Flash","type":"chat","display_name":"GLM-5.3 Flash","organization":"Z.ai","context_length":1000000,"pricing":{"input":0.15,"output":0.6,"hourly":0}},
      {"id":"moonshotai/Kimi-K3","type":"chat","display_name":"Kimi K3","pricing":{"input":3,"output":9}},
      {"id":"Qwen/Qwen3.7-Max","type":"chat","display_name":"Qwen3.7 Max","pricing":{"input":1.2,"output":4}},
      {"id":"acme/New-Model","type":"chat","display_name":"New Model","organization":"Acme","context_length":131072,"pricing":{"input":0.5,"output":1}},
      {"id":"acme/Dedicated-Only","type":"chat","pricing":{"input":0,"output":0,"hourly":4.5}},
      {"id":"BAAI/bge-base-en-v1.5","type":"embedding","pricing":{"input":0.01,"output":0}}
    ]
    """

    private val curated = AIModels.ALL_MODELS.filter { it.provider == "Together.ai" }

    @Test
    fun parseKeepsOnlyOnDemandChatModels() {
        assertEquals(
            setOf("zai-org/GLM-5.3-Flash", "moonshotai/Kimi-K3", "Qwen/Qwen3.7-Max", "acme/New-Model"),
            TogetherModelCatalog.parse(sample).map { it.id }.toSet()
        )
    }

    @Test
    fun mergeDropsUnavailableCuratedAndAddsTheRest() {
        val merged = TogetherModelCatalog.merge(curated, TogetherModelCatalog.parse(sample))
        val ids = merged.map { it.id }
        assertTrue(ids.contains("zai-org/GLM-5.3-Flash"))
        assertFalse("curated but not available to this key", ids.contains("zai-org/GLM-5.3"))
        assertEquals(TogetherModelCatalog.MORE_GROUP, merged.first { it.id == "acme/New-Model" }.provider)
        assertEquals("Together.ai", merged.first { it.id == "Qwen/Qwen3.7-Max" }.provider)
    }

    @Test
    fun withoutALiveListTheBuiltInListIsUsed() {
        assertEquals(curated, TogetherModelCatalog.merge(curated, null))
    }
}
