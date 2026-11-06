package tv.zelta.app.settings

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Compose-observable settings backed by SharedPreferences. */
class AppSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var theme by mutableStateOf(prefs.getString(KEY_THEME, "default") ?: "default")
        private set
    var language by mutableStateOf(prefs.getString(KEY_LANGUAGE, "en") ?: "en")
        private set
    var highestQuality by mutableStateOf(prefs.getBoolean(KEY_QUALITY, true))
        private set
    var setupDone by mutableStateOf(prefs.getBoolean(KEY_SETUP_DONE, false))
        private set

    fun completeSetup() {
        setupDone = true
        prefs.edit().putBoolean(KEY_SETUP_DONE, true).apply()
    }

    fun resetAll() {
        theme = "default"
        language = "en"
        highestQuality = true
        setupDone = false
        prefs.edit().clear().apply()
    }

    fun updateTheme(value: String) {
        theme = value
        prefs.edit().putString(KEY_THEME, value).apply()
    }

    fun updateLanguage(value: String) {
        language = value
        prefs.edit().putString(KEY_LANGUAGE, value).apply()
    }

    fun updateHighestQuality(value: Boolean) {
        highestQuality = value
        prefs.edit().putBoolean(KEY_QUALITY, value).apply()
    }

    companion object {
        private const val PREFS = "zeltatv-settings"
        private const val KEY_THEME = "theme"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_QUALITY = "highest_quality"
        private const val KEY_SETUP_DONE = "setup_done"

        fun languageOf(context: Context): String =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_LANGUAGE, "en") ?: "en"
    }
}
