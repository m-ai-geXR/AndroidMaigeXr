package com.xraiassistant.data.repositories

import com.xraiassistant.data.local.SettingsDataStore
import com.xraiassistant.data.models.AIEffort
import com.xraiassistant.data.models.AIImageContent
import com.xraiassistant.data.remote.AIProviderService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests the repository half of the AI call chain: ChatViewModel hands the
 * repository an effort level plus an optional image list, and both have to
 * reach AIProviderService in their own parameters.
 */
class AIProviderRepositoryTest {

    private val service: AIProviderService = mockk()
    private val settingsDataStore: SettingsDataStore = mockk()
    private val repository = AIProviderRepository(service, settingsDataStore)

    private val image = AIImageContent(
        data = byteArrayOf(9, 8, 7),
        mimeType = "image/jpeg",
        filename = "scene.jpg"
    )

    private fun givenApiKey(key: String) {
        every { settingsDataStore.getAPIKeySync(any()) } returns key
    }

    @Test
    fun `generateResponseStream passes effort and images through to the service`() = runTest {
        givenApiKey("sk-ant-test")
        coEvery {
            service.generateResponseStream(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns flowOf("chunk")

        val chunks = repository.generateResponseStream(
            prompt = "Create a spinning cube",
            model = "claude-opus-5",
            temperature = 0.4,
            topP = 0.8,
            systemPrompt = "You are a Babylon.js expert",
            effort = AIEffort.XHIGH,
            images = listOf(image)
        ).toList()

        assertEquals(listOf("chunk"), chunks)

        coVerify(exactly = 1) {
            service.generateResponseStream(
                provider = "Anthropic",
                apiKey = "sk-ant-test",
                model = "claude-opus-5",
                prompt = "Create a spinning cube",
                systemPrompt = "You are a Babylon.js expert",
                temperature = 0.4,
                topP = 0.8,
                effort = AIEffort.XHIGH,
                images = listOf(image)
            )
        }
    }

    @Test
    fun `generateResponse passes effort through to the service`() = runTest {
        givenApiKey("sk-openai-test")
        coEvery {
            service.generateResponse(any(), any(), any(), any(), any(), any(), any(), any())
        } returns "done"

        val response = repository.generateResponse(
            prompt = "Create a torus",
            model = "gpt-5",
            temperature = 0.2,
            topP = 0.6,
            systemPrompt = "You are a Three.js expert",
            effort = AIEffort.MEDIUM
        )

        assertEquals("done", response)

        coVerify(exactly = 1) {
            service.generateResponse(
                provider = "OpenAI",
                apiKey = "sk-openai-test",
                model = "gpt-5",
                prompt = "Create a torus",
                systemPrompt = "You are a Three.js expert",
                temperature = 0.2,
                topP = 0.6,
                effort = AIEffort.MEDIUM
            )
        }
    }

    @Test
    fun `streaming is refused when the provider has no api key`() = runTest {
        givenApiKey("changeMe")

        val error = runCatching {
            repository.generateResponseStream(
                prompt = "prompt",
                model = "gpt-5",
                temperature = 0.7,
                topP = 0.9,
                systemPrompt = "system"
            )
        }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertEquals("API key not configured for OpenAI", error?.message)
    }

    @Test
    fun `model ids route to the expected provider`() = runTest {
        givenApiKey("test-key")
        coEvery {
            service.generateResponseStream(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns flowOf("chunk")

        val expectedProviders = mapOf(
            "gpt-5" to "OpenAI",
            "claude-opus-5" to "Anthropic",
            "gemini-2.5-pro" to "Google AI",
            "grok-4" to "xAI",
            "deepseek-ai/DeepSeek-R1" to "Together.ai",
            "meta-llama/Llama-3.3-70B-Instruct-Turbo" to "Together.ai",
            "Qwen/Qwen2.5-7B-Instruct-Turbo" to "Together.ai",
            "some-unknown-model" to "Together.ai"
        )

        expectedProviders.forEach { (model, provider) ->
            repository.generateResponseStream(
                prompt = "prompt",
                model = model,
                temperature = 0.7,
                topP = 0.9,
                systemPrompt = "system"
            ).toList()

            coVerify(exactly = 1) {
                service.generateResponseStream(
                    provider = provider,
                    apiKey = "test-key",
                    model = model,
                    prompt = "prompt",
                    systemPrompt = "system",
                    temperature = 0.7,
                    topP = 0.9,
                    effort = AIEffort.HIGH,
                    images = emptyList()
                )
            }
        }
    }
}
