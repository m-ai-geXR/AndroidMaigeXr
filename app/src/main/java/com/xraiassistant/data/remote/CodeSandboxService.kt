package com.xraiassistant.data.remote

import com.xraiassistant.data.models.CodeSandboxDefineRequest
import com.xraiassistant.data.models.CodeSandboxDefineResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * CodeSandbox Define API Service
 *
 * Programmatically create sandboxes using the CodeSandbox Define API.
 * Documentation: https://codesandbox.io/docs/api
 *
 * Base URL: https://codesandbox.io/api/v1/
 */
interface CodeSandboxService {

    /**
     * Create a new sandbox using the Define API
     *
     * POST https://codesandbox.io/api/v1/sandboxes/define?json=1
     *
     * @param json Set to 1 to return JSON response (instead of redirect)
     * @param request The sandbox definition with files
     * @return Sandbox ID to construct preview URL
     */
    @POST("sandboxes/define")
    suspend fun createSandbox(
        @Query("json") json: Int = 1,
        @Body request: CodeSandboxDefineRequest
    ): CodeSandboxDefineResponse
}
