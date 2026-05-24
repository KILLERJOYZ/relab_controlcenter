package com.example.relab_tool.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.relab_tool.ui.theme.DarkModeOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val SEED_COLOR = longPreferencesKey("seed_color")
    }

    val themeMode: Flow<DarkModeOption> = dataStore.data.map { preferences ->
        val modeName = preferences[PreferencesKeys.THEME_MODE] ?: DarkModeOption.FOLLOW_SYSTEM.name
        try {
            DarkModeOption.valueOf(modeName)
        } catch (e: IllegalArgumentException) {
            DarkModeOption.FOLLOW_SYSTEM
        }
    }

    val useDynamicColor: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DYNAMIC_COLOR] ?: true
    }

    val seedColor: Flow<Long> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SEED_COLOR] ?: 0xFF6750A4 // Default M3 Primary
    }

    suspend fun setThemeMode(mode: DarkModeOption) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DYNAMIC_COLOR] = enabled
        }
    }

    suspend fun setSeedColor(color: Long) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SEED_COLOR] = color
        }
    }
}
