package com.xraiassistant.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Playground options the user toggles in Settings.
 *
 * A plain object over SharedPreferences, like AppearanceStore: the scene reads it
 * while composing and needs the value synchronously, not after a ViewModel loads.
 */
object PlaygroundPreferences {
    private const val PREFS = "maigexr_playground"
    private const val KEY_COMMAND_LINE = "command_line_enabled"

    private val _commandLineEnabled = MutableStateFlow(true)

    /** The one-line JavaScript console at the bottom of the scene. On by default. */
    val commandLineEnabled: StateFlow<Boolean> = _commandLineEnabled.asStateFlow()

    fun load(context: Context) {
        _commandLineEnabled.value = prefs(context).getBoolean(KEY_COMMAND_LINE, true)
    }

    fun setCommandLineEnabled(context: Context, enabled: Boolean) {
        if (_commandLineEnabled.value == enabled) return
        _commandLineEnabled.value = enabled
        prefs(context).edit().putBoolean(KEY_COMMAND_LINE, enabled).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
