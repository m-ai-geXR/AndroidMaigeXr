package com.xraiassistant.data.background

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONObject

/**
 * Requests handed to the background and their finished replies. Kept in
 * preferences so a reply that finishes after the app was closed is still
 * there at the next launch.
 */
class BackgroundReplyStore(private val prefs: SharedPreferences) {

    data class Job(
        val id: String,
        val prompt: String,
        val systemPrompt: String,
        val model: String,
        val temperature: Double,
        val topP: Double,
        val effort: String
    )

    data class Outcome(val jobId: String, val text: String?, val error: String?, val model: String? = null)

    fun saveJob(job: Job) {
        val json = JSONObject()
            .put("prompt", job.prompt).put("systemPrompt", job.systemPrompt).put("model", job.model)
            .put("temperature", job.temperature).put("topP", job.topP).put("effort", job.effort)
        prefs.edit().putString(JOB + job.id, json.toString()).apply()
    }

    fun job(id: String): Job? {
        val raw = prefs.getString(JOB + id, null) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            Job(id, json.getString("prompt"), json.getString("systemPrompt"), json.getString("model"),
                json.getDouble("temperature"), json.getDouble("topP"), json.getString("effort"))
        }.getOrNull()
    }

    fun saveOutcome(outcome: Outcome) {
        val json = JSONObject().put("text", outcome.text ?: JSONObject.NULL).put("error", outcome.error ?: JSONObject.NULL)
            .put("model", outcome.model ?: JSONObject.NULL)
        prefs.edit().putString(OUTCOME + outcome.jobId, json.toString()).remove(JOB + outcome.jobId).apply()
    }

    fun outcome(jobId: String): Outcome? {
        val raw = prefs.getString(OUTCOME + jobId, null) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            Outcome(
                jobId,
                json.optString("text").takeIf { !json.isNull("text") },
                json.optString("error").takeIf { !json.isNull("error") },
                json.optString("model").takeIf { json.has("model") && !json.isNull("model") }
            )
        }.getOrNull()
    }

    /** Finished replies nobody collected (the app was closed when they arrived). */
    fun uncollectedOutcomes(): List<Outcome> =
        prefs.all.keys.filter { it.startsWith(OUTCOME) }.mapNotNull { outcome(it.removePrefix(OUTCOME)) }

    fun remove(jobId: String) {
        prefs.edit().remove(JOB + jobId).remove(OUTCOME + jobId).apply()
    }

    companion object {
        private const val JOB = "job_"
        private const val OUTCOME = "outcome_"

        fun from(context: Context) = BackgroundReplyStore(
            context.applicationContext.getSharedPreferences("background_replies", Context.MODE_PRIVATE)
        )

        /** Job ids as they finish, for a chat that is still open. */
        private val _finished = MutableSharedFlow<String>(extraBufferCapacity = 8)
        val finished: SharedFlow<String> = _finished.asSharedFlow()
        internal fun announce(jobId: String) { _finished.tryEmit(jobId) }
    }
}
