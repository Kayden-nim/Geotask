package com.example.geotask.pages


import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val DATASTORE_NAME = "theme_preferences"
private val Context.dataStore by preferencesDataStore(name = DATASTORE_NAME)

class ThemePreferences(context: Context) {
    private val appContext = context.applicationContext

    companion object {
        private val THEME_KEY = booleanPreferencesKey("dark_mode")
    }

    val isDarkTheme: Flow<Boolean> = appContext.dataStore.data
        .map { preferences ->
            preferences[THEME_KEY] ?: false
        }

    suspend fun saveTheme(isDark: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[THEME_KEY] = isDark
        }
    }
}
