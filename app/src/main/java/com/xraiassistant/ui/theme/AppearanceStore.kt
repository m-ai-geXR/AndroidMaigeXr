package com.xraiassistant.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * How the app picks light or dark.
 *
 * The brand palette is adaptive, so following the system needs no setting. This
 * exists because following the system is not always what a person wants: the
 * desktop client has offered System, Light and Dark since the brand work, and
 * the native clients had no way to override at all.
 */
enum class ThemeMode(val storageValue: String, val displayName: String) {
    SYSTEM("system", "System"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark");

    companion object {
        fun from(value: String?): ThemeMode =
            entries.firstOrNull { it.storageValue == value } ?: SYSTEM
    }
}

/**
 * Holds the chosen mode.
 *
 * Deliberately a plain object over SharedPreferences rather than DataStore: the
 * theme is read by MainActivity before any ViewModel exists, and it has to be
 * available synchronously at first composition or the app flashes the wrong
 * theme on launch.
 */
object AppearanceStore {
    private const val PREFS = "maigexr_appearance"
    private const val KEY = "theme_mode"

    private val _mode = MutableStateFlow(ThemeMode.SYSTEM)
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    fun load(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _mode.value = ThemeMode.from(prefs.getString(KEY, null))
    }

    fun set(context: Context, mode: ThemeMode) {
        if (_mode.value == mode) return
        _mode.value = mode
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, mode.storageValue)
            .apply()
    }
}
