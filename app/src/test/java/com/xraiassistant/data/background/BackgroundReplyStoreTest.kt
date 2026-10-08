package com.xraiassistant.data.background

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards replies that must arrive after the user leaves the app: a handed-off
 * request must be readable by the background job, and its finished reply must
 * survive until the chat collects it, across a relaunch.
 */
class BackgroundReplyStoreTest {

    private class MemoryPrefs : SharedPreferences {
        val map = mutableMapOf<String, Any?>()
        override fun getAll(): MutableMap<String, *> = map.toMutableMap()
        override fun getString(key: String?, defValue: String?) = map[key] as String? ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?) = defValues
        override fun getInt(key: String?, defValue: Int) = defValue
        override fun getLong(key: String?, defValue: Long) = defValue
        override fun getFloat(key: String?, defValue: Float) = defValue
        override fun getBoolean(key: String?, defValue: Boolean) = defValue
        override fun contains(key: String?) = map.containsKey(key)
        override fun registerOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
            val pending = mutableListOf<() -> Unit>()
            override fun putString(k: String?, v: String?) = apply { pending += { map[k!!] = v } }
            override fun putStringSet(k: String?, v: MutableSet<String>?) = this
            override fun putInt(k: String?, v: Int) = this
            override fun putLong(k: String?, v: Long) = this
            override fun putFloat(k: String?, v: Float) = this
            override fun putBoolean(k: String?, v: Boolean) = this
            override fun remove(k: String?) = apply { pending += { map.remove(k) } }
            override fun clear() = apply { pending += { map.clear() } }
            override fun commit(): Boolean { pending.forEach { it() }; return true }
            override fun apply() { commit() }
        }
    }

    private val job = BackgroundReplyStore.Job("j1", "make a cube", "SYS", "local:qwen", 0.7, 0.9, "high")

    @Test
    fun handedOffJobCanBeReadBack() {
        val store = BackgroundReplyStore(MemoryPrefs())
        store.saveJob(job)
        assertEquals(job, store.job("j1"))
    }

    @Test
    fun finishedReplySurvivesARelaunchAndReplacesTheJob() {
        val prefs = MemoryPrefs()
        BackgroundReplyStore(prefs).apply {
            saveJob(job)
            saveOutcome(BackgroundReplyStore.Outcome("j1", "here is your cube", null, "local:qwen"))
        }
        val relaunched = BackgroundReplyStore(prefs)
        assertEquals("here is your cube", relaunched.outcome("j1")?.text)
        assertEquals("the reply keeps the model that wrote it", "local:qwen", relaunched.outcome("j1")?.model)
        assertNull("the job is done", relaunched.job("j1"))
        assertEquals(listOf("j1"), relaunched.uncollectedOutcomes().map { it.jobId })
    }

    @Test
    fun failedReplyKeepsItsError() {
        val store = BackgroundReplyStore(MemoryPrefs())
        store.saveOutcome(BackgroundReplyStore.Outcome("j2", null, "HTTP 500"))
        val outcome = store.outcome("j2")
        assertNull(outcome?.text)
        assertEquals("HTTP 500", outcome?.error)
    }

    @Test
    fun collectedReplyIsGone() {
        val store = BackgroundReplyStore(MemoryPrefs())
        store.saveOutcome(BackgroundReplyStore.Outcome("j3", "x", null))
        store.remove("j3")
        assertNull(store.outcome("j3"))
        assertTrue(store.uncollectedOutcomes().isEmpty())
    }
}
