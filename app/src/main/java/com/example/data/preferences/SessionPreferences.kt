package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.savDataStore by preferencesDataStore(name = "sav_family_prefs")

class SessionPreferences(private val context: Context) {
    private val rememberMeKey = booleanPreferencesKey("remember_me")
    private val themeModeKey = stringPreferencesKey("theme_mode") // SYSTEM, LIGHT, DARK

    val rememberMe: Flow<Boolean> = context.savDataStore.data.map { prefs ->
        prefs[rememberMeKey] ?: true
    }

    val themeMode: Flow<String> = context.savDataStore.data.map { prefs ->
        prefs[themeModeKey] ?: "SYSTEM"
    }

    suspend fun setRememberMe(enabled: Boolean) {
        context.savDataStore.edit { prefs ->
            prefs[rememberMeKey] = enabled
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.savDataStore.edit { prefs ->
            prefs[themeModeKey] = mode
        }
    }
}
