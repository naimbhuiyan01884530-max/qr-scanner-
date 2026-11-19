package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.i18n.AppLanguage
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "qrlens_prefs")

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val language: AppLanguage = AppLanguage.BN,
    val vibrateOnScan: Boolean = true
)

class PreferencesManager(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val VIBRATE_ON_SCAN = booleanPreferencesKey("vibrate_on_scan")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val theme = prefs[Keys.THEME_MODE]?.let { name ->
            ThemeMode.entries.firstOrNull { it.name == name }
        } ?: ThemeMode.DARK

        val lang = prefs[Keys.LANGUAGE]?.let { name ->
            AppLanguage.entries.firstOrNull { it.name == name }
        } ?: AppLanguage.BN

        val vibrate = prefs[Keys.VIBRATE_ON_SCAN] ?: true
        UserPreferences(themeMode = theme, language = lang, vibrateOnScan = vibrate)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun setVibrateOnScan(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VIBRATE_ON_SCAN] = enabled }
    }
}
