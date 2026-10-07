package com.example.zerogrid.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class ThemeMode { LIGHT, DARK, SYSTEM }

private val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")

class ThemePreferenceManager private constructor(private val context: Context) {

    private val THEME_KEY = stringPreferencesKey("theme_mode")
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(getThemeModeSync())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == KEY_THEME_MODE) {
            _themeMode.value = getThemeModeSync()
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)

        // If SharedPreferences has no value yet, migrate asynchronously from DataStore
        if (!prefs.contains(KEY_THEME_MODE)) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val dsPrefs = context.themeDataStore.data.first()
                    val dsMode = dsPrefs[THEME_KEY]
                    if (dsMode != null) {
                        prefs.edit().putString(KEY_THEME_MODE, dsMode).apply()
                        val mode = when (dsMode) {
                            "LIGHT" -> ThemeMode.LIGHT
                            "DARK" -> ThemeMode.DARK
                            else -> ThemeMode.SYSTEM
                        }
                        _themeMode.value = mode
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun getThemeModeSync(): ThemeMode {
        val saved = prefs.getString(KEY_THEME_MODE, null)
        return when (saved) {
            "LIGHT" -> ThemeMode.LIGHT
            "DARK" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
        try {
            context.themeDataStore.edit { prefs ->
                prefs[THEME_KEY] = mode.name
            }
        } catch (_: Exception) {}
    }

    companion object {
        private const val PREFS_NAME = "zerogrid_theme_prefs"
        private const val KEY_THEME_MODE = "theme_mode"

        @Volatile
        private var instance: ThemePreferenceManager? = null

        fun getInstance(context: Context): ThemePreferenceManager {
            return instance ?: synchronized(this) {
                instance ?: ThemePreferenceManager(context.applicationContext).also { instance = it }
            }
        }

        operator fun invoke(context: Context): ThemePreferenceManager = getInstance(context)
    }
}