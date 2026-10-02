package com.xraiassistant.data.remote

import com.xraiassistant.data.models.AIEffort
import com.xraiassistant.data.models.AIImageContent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Regression tests for the AIProviderService facade.
 *
 * The facade exists only to forward calls to RealAIProviderService, whose
 * signature ends in two defaulted parameters (effort, images). Forwarding
 * positionally once put the image list into the effort slot, so these tests
 * pin down that every argument lands in the parameter it belongs to.
 */
class AIProviderServiceTest {

    private val realService: RealAIProviderService = mockk()
    private val service = AIProviderService(realService)

    private val image = AIImageContent(
        data = byteArrayOf(1, 2, 3),
        mimeType = "image/png",
        filename = "cube.png"
    )

    @Test
    fun `generateResponseStream forwards every argument to the matching parameter`() = runTest {
        coEvery {
            realService.generateResponseStream(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns flowOf("chunk-1", "chunk-2")

        val chunks = service.generateResponseStream(
            provider = "Anthropic",
            apiKey = "sk-ant-test",
            model = "claude-opus-5",
            prompt = "Create a spinning cube",
            systemPrompt = "You are a Babylon.js expert",
            temperature = 0.4,
            topP = 0.8,
            effort = AIEffort.MAX,
            images = listOf(image)
        ).toList()

        assertEquals(listOf("chunk-1", "chunk-2"), chunks)

        coVerify(exactly = 1) {
            realService.generateResponseStream(
                provider = "Anthropic",
                apiKey = "sk-ant-test",
                model = "claude-opus-5",
                prompt = "Create a spinning cube",
                systemPrompt = "You are a Babylon.js expert",
                temperature = 0.4,
                topP = 0.8,
                effort = AIEffort.MAX,
                images = listOf(image)
            )
        }
    }

    @Test
    fun `generateResponseStream defaults to high effort and no images`() = runTest {
        coEvery {
            realService.generateResponseStream(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns flowOf("ok")

        service.generateResponseStream(
            provider = "Together.ai",
            apiKey = "together-test",
            model = "deepseek-ai/DeepSeek-R1",
            prompt = "prompt",
            systemPrompt = "system",
            temperature = 0.7,
            topP = 0.9
        ).toList()

        coVerify(exactly = 1) {
            realService.generateResponseStream(
                provider = "Together.ai",
                apiKey = "together-test",
                model = "deepseek-ai/DeepSeek-R1",
                prompt = "prompt",
                systemPrompt = "system",
                temperature = 0.7,
                topP = 0.9,
                effort = AIEffort.HIGH,
                images = emptyList()
            )
        }
    }

    @Test
    fun `generateResponse forwards every argument to the matching parameter`() = runTest {
        coEvery {
            realService.generateResponse(any(), any(), any(), any(), any(), any(), any(), any())
        } returns "full response"

        val response = service.generateResponse(
            provider = "OpenAI",
            apiKey = "sk-openai-test",
            model = "gpt-5",
            prompt = "Create a torus",
            systemPrompt = "You are a Three.js expert",
            temperature = 0.2,
            topP = 0.6,
            effort = AIEffort.LOW
        )

        assertEquals("full response", response)

        coVerify(exactly = 1) {
            realService.generateResponse(
                provider = "OpenAI",
                apiKey = "sk-openai-test",
                model = "gpt-5",
                prompt = "Create a torus",
                systemPrompt = "You are a Three.js expert",
                temperature = 0.2,
                topP = 0.6,
                effort = AIEffort.LOW
            )
        }
    }

    @Test
    fun `generateResponse defaults to high effort`() = runTest {
        coEvery {
            realService.generateResponse(any(), any(), any(), any(), any(), any(), any(), any())
        } returns "full response"

        service.generateResponse(
            provider = "Together.ai",
            apiKey = "together-test",
            model = "deepseek-ai/DeepSeek-R1",
            prompt = "prompt",
            systemPrompt = "system",
            temperature = 0.7,
            topP = 0.9
        )

        coVerify(exactly = 1) {
            realService.generateResponse(
                provider = any(),
                apiKey = any(),
                model = any(),
                prompt = any(),
                systemPrompt = any(),
                temperature = any(),
                topP = any(),
                effort = AIEffort.HIGH
            )
        }
    }
}
