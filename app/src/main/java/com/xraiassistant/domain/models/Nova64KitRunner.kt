package com.xraiassistant.domain.models

import android.content.Context
import android.util.Log
import androidx.concurrent.futures.await
import androidx.javascriptengine.JavaScriptIsolate
import androidx.javascriptengine.JavaScriptSandbox
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The shared Nova64 cart kit (WebMaigeXr/nova64-kit, bundled as assets/nova64-kit.js),
 * run in an AndroidX JavaScriptSandbox so the chat can check a reply as soon as it
 * arrives and ask the model for a repair. The playground page runs the same kit
 * again when the cart is injected.
 *
 * Unavailable (returns null) on devices whose WebView does not support the sandbox;
 * replies are then simply not checked.
 */
@Singleton
class Nova64KitRunner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class Result(val code: String, val problems: List<String>)

    private val mutex = Mutex()
    private var sandbox: JavaScriptSandbox? = null
    private var isolate: JavaScriptIsolate? = null
    private var failed = false

    /** Bounded number of repair attempts per user message (MAX_REPAIRS in the kit). */
    var maxRepairs = 2
        private set

    private suspend fun kit(): JavaScriptIsolate? {
        isolate?.let { return it }
        if (failed || !JavaScriptSandbox.isSupported()) return null
        return try {
            val box = JavaScriptSandbox.createConnectedInstanceAsync(context).await()
            val iso = box.createIsolate()
            val source = context.assets.open("nova64-kit.js").bufferedReader().use { it.readText() }
            iso.evaluateJavaScriptAsync(source).await()
            maxRepairs = iso.evaluateJavaScriptAsync("String(Nova64Kit.MAX_REPAIRS)").await().toIntOrNull() ?: 2
            sandbox = box
            isolate = iso
            iso
        } catch (e: Exception) {
            Log.w(TAG, "Nova64 kit not available; replies are not checked for repair", e)
            failed = true
            null
        }
    }

    suspend fun prepare(code: String): Result? = mutex.withLock {
        val iso = kit() ?: return null
        try {
            val json = iso.evaluateJavaScriptAsync(
                "(function(){var r=Nova64Kit.prepare(${JSONObject.quote(code)});" +
                    "return JSON.stringify({code:r.code,problems:r.problems});})()"
            ).await()
            val obj = JSONObject(json)
            val problems = obj.getJSONArray("problems").let { a -> List(a.length()) { a.getString(it) } }
            Result(obj.getString("code"), problems)
        } catch (e: Exception) {
            Log.w(TAG, "Nova64 kit prepare failed", e)
            null
        }
    }

    suspend fun repairPrompt(code: String, problems: List<String>): String? = mutex.withLock {
        val iso = kit() ?: return null
        try {
            iso.evaluateJavaScriptAsync(
                "Nova64Kit.repairPrompt(${JSONObject.quote(code)}, ${JSONArray(problems)})"
            ).await()
        } catch (e: Exception) {
            Log.w(TAG, "Nova64 kit repairPrompt failed", e)
            null
        }
    }

    private companion object { const val TAG = "Nova64KitRunner" }
}
