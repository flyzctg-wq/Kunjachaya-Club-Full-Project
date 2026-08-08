package com.example.data.preferences

import android.content.Context
import com.example.ui.language.Language

class LanguagePreferences(context: Context) {
    private val prefs = context.getSharedPreferences("kunjachaya_app_preferences", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LANGUAGE = "app_language"
        private const val KEY_DARK_THEME = "app_dark_theme"
    }

    /**
     * Get persisted language preference, defaulting to Bengali (BN).
     */
    fun getLanguage(): Language {
        val langStr = prefs.getString(KEY_LANGUAGE, Language.BN.name) ?: Language.BN.name
        return try {
            Language.valueOf(langStr)
        } catch (e: Exception) {
            Language.BN
        }
    }

    /**
     * Persist selected language preference to SharedPreferences.
     */
    fun setLanguage(language: Language) {
        prefs.edit().putString(KEY_LANGUAGE, language.name).apply()
    }

    /**
     * Get persisted dark theme preference.
     */
    fun isDarkTheme(): Boolean {
        return prefs.getBoolean(KEY_DARK_THEME, false)
    }

    /**
     * Persist dark theme preference.
     */
    fun setDarkTheme(isDark: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_THEME, isDark).apply()
    }
}
