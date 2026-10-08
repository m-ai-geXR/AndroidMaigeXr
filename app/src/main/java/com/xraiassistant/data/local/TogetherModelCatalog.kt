package com.xraiassistant.data.local

import android.content.Context
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.xraiassistant.data.models.AICapability
import com.xraiassistant.data.models.AIModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The chat models the user Together key can use right now, from GET /v1/models.
 * Together retires models (DeepSeek R1 went without notice), so a live list keeps
 * the picker honest: curated models the key cannot use drop out, and the rest of
 * what the key can use is offered too. With no key, no network or no cached list,
 * the built-in list is used unchanged. Matches iOS TogetherModelCatalog.
 */
@Singleton
class TogetherModelCatalog @Inject constructor(
    @ApplicationContext context: Context
) {
    data class LiveModel(
        val id: String,
        val displayName: String,
        val organization: String?,
        val contextLength: Int?,
        val inputPricePerMillion: Double
    )

    private val prefs = context.getSharedPreferences("together_models", Context.MODE_PRIVATE)

    /** The last list fetched for the key, if any. */
    val cached: List<LiveModel>?
        get() = prefs.getString(KEY_JSON, null)?.let { parse(it) }?.takeIf { it.isNotEmpty() }

    val isStale: Boolean
        get() = System.currentTimeMillis() - prefs.getLong(KEY_FETCHED_AT, 0L) > MAX_AGE_MS

    /** Fetches the list for [apiKey] and caches it. Keeps the old list on failure. */
    suspend fun refresh(apiKey: String): Boolean = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "changeMe") return@withContext false
        try {
            val connection = (URL("https://api.together.xyz/v1/models").openConnection() as HttpURLConnection).apply {
                setRequestProperty("Authorization", "Bearer $apiKey")
                connectTimeout = 20_000
                readTimeout = 20_000
            }
            try {
                if (connection.responseCode != 200) return@withContext false
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val models = parse(body)
                if (models.isEmpty()) return@withContext false
                prefs.edit().putString(KEY_JSON, body).putLong(KEY_FETCHED_AT, System.currentTimeMillis()).apply()
                Log.d(TAG, "Together models for this key: ${models.size}")
                true
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not fetch Together models: ${e.message}")
            false
        }
    }

    companion object {
        private const val TAG = "TogetherModelCatalog"
        private const val KEY_JSON = "models_json"
        private const val KEY_FETCHED_AT = "fetched_at"
        private const val MAX_AGE_MS = 24L * 60 * 60 * 1000

        /** The group the extra models appear under in the picker. */
        const val MORE_GROUP = "Together.ai · More"

        private val listAdapter = Moshi.Builder().build().adapter<Any>(Any::class.java)

        /**
         * Chat models billed per token. Together has no serverless flag; models
         * only on dedicated endpoints are billed hourly with no per-token price.
         */
        fun parse(json: String): List<LiveModel> {
            val root = try { listAdapter.fromJson(json) } catch (e: Exception) { null }
            @Suppress("UNCHECKED_CAST")
            val items = (root as? List<Map<String, Any?>>)
                ?: ((root as? Map<String, Any?>)?.get("data") as? List<Map<String, Any?>>)
                ?: return emptyList()
            return items.mapNotNull { item ->
                val id = item["id"] as? String ?: return@mapNotNull null
                if (item["type"] != "chat") return@mapNotNull null
                val pricing = item["pricing"] as? Map<*, *> ?: emptyMap<String, Any>()
                val input = (pricing["input"] as? Number)?.toDouble() ?: 0.0
                val output = (pricing["output"] as? Number)?.toDouble() ?: 0.0
                if (input <= 0 && output <= 0) return@mapNotNull null
                LiveModel(
                    id = id,
                    displayName = (item["display_name"] as? String)?.takeIf { it.isNotBlank() } ?: id,
                    organization = item["organization"] as? String,
                    contextLength = (item["context_length"] as? Number)?.toInt(),
                    inputPricePerMillion = input
                )
            }
        }

        /**
         * The models to offer: curated ones the key can use, in their order, then
         * the rest of the key chat models by name. Without a live list, curated.
         */
        fun merge(curated: List<AIModel>, live: List<LiveModel>?): List<AIModel> {
            if (live.isNullOrEmpty()) return curated
            val liveIds = live.map { it.id }.toSet()
            val kept = curated.filter { it.id in liveIds }
            val curatedIds = curated.map { it.id }.toSet()
            val extras = live
                .filter { it.id !in curatedIds }
                .sortedBy { it.displayName.lowercase() }
                .map { model ->
                    val details = listOfNotNull(
                        model.organization?.takeIf { it.isNotBlank() },
                        model.contextLength?.takeIf { it > 0 }?.let { "${it / 1000}K context" }
                    )
                    AIModel(
                        id = model.id,
                        displayName = model.displayName,
                        description = details.joinToString(" · ").ifEmpty { "Together model" },
                        provider = MORE_GROUP,
                        pricing = "$%.2f/1M input tokens".format(model.inputPricePerMillion),
                        capabilities = setOf(AICapability.TEXT_GENERATION, AICapability.CODE_GENERATION, AICapability.STREAMING)
                    )
                }
            // Never leave the key with nothing: if no curated model survived, keep them.
            return (kept.ifEmpty { curated }) + extras
        }
    }
}
