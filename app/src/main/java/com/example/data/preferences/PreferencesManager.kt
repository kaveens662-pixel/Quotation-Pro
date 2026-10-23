package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "quotation_pro_preferences")

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
    ENGLISH("en", "English", "English"),
    SINHALA("si", "Sinhala", "සිංහල")
}

data class UserPreferences(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val defaultCurrency: String = "USD"
)

class PreferencesManager(private val context: Context) {
    companion object {
        val KEY_THEME = stringPreferencesKey("app_theme")
        val KEY_LANGUAGE = stringPreferencesKey("app_language")
        val KEY_DEFAULT_CURRENCY = stringPreferencesKey("default_currency")
    }

    val userPreferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val themeStr = prefs[KEY_THEME] ?: AppThemeMode.SYSTEM.name
        val langStr = prefs[KEY_LANGUAGE] ?: AppLanguage.ENGLISH.code
        val currStr = prefs[KEY_DEFAULT_CURRENCY] ?: "USD"

        val theme = try {
            AppThemeMode.valueOf(themeStr)
        } catch (_: Exception) {
            AppThemeMode.SYSTEM
        }

        val lang = if (langStr == "si") AppLanguage.SINHALA else AppLanguage.ENGLISH

        UserPreferences(
            themeMode = theme,
            language = lang,
            defaultCurrency = currStr
        )
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME] = mode.name
        }
    }

    suspend fun setLanguage(lang: AppLanguage) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LANGUAGE] = lang.code
        }
    }

    suspend fun setDefaultCurrency(currency: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEFAULT_CURRENCY] = currency
        }
    }
}
