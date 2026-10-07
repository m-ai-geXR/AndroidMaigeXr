package com.xraiassistant.data.local

import android.content.Context
import com.xraiassistant.data.models.AICapability
import com.xraiassistant.data.models.AIModel
import com.xraiassistant.domain.local.LocalServerConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Address and model name of the user's local model server. Not secret, so kept
 * in plain preferences; the optional key is stored with the other API keys.
 */
@Singleton
class LocalServerSettings @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("local_model_server", Context.MODE_PRIVATE)

    var baseUrl: String
        get() = prefs.getString(KEY_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_URL, value.trim()).apply()

    var modelName: String
        get() = prefs.getString(KEY_MODEL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_MODEL, value.trim()).apply()

    val chatCompletionsUrl: String? get() = LocalServerConfig.chatCompletionsUrl(baseUrl)

    val isConfigured: Boolean get() = chatCompletionsUrl != null && modelName.isNotBlank()

    /** The model to list under Local in the picker, once set up. */
    fun model(): AIModel? = if (!isConfigured) null else AIModel(
        id = LocalServerConfig.MODEL_PREFIX + modelName,
        displayName = modelName,
        description = "Your own server at $baseUrl",
        provider = LocalServerConfig.PROVIDER,
        pricing = "Runs on your server",
        capabilities = setOf(AICapability.TEXT_GENERATION, AICapability.CODE_GENERATION, AICapability.STREAMING)
    )

    private companion object {
        const val KEY_URL = "base_url"
        const val KEY_MODEL = "model_name"
    }
}
