package tv.zelta.app.settings

import android.content.Context
import org.json.JSONObject
import java.util.Locale

/**
 * Data-driven translations copied from the desktop app (assets/i18n).
 * `translate` looks a string up by its English text, so the existing
 * `zt(turkish, english)` call sites keep working and gain 18 languages.
 */
object I18n {
    private val cache = HashMap<String, Map<String, String>>()

    fun forLanguage(context: Context, language: String): Map<String, String> =
        cache.getOrPut(language) { build(context, language) }

    fun translate(context: Context, language: String, english: String): String? {
        if (language == "en" || english.isBlank()) return null
        return forLanguage(context, language)[normalize(english)]
    }

    private fun build(context: Context, language: String): Map<String, String> {
        val english = parse(context, "en")
        val target = parse(context, language)
        val out = HashMap<String, String>()
        for ((key, value) in target) {
            val source = english[key] ?: continue
            if (value.isBlank() || source.isBlank()) continue
            out[normalize(source)] = value
        }
        // Android-only copy that has no desktop counterpart
        for ((englishText, translated) in parse(context, "android-$language")) {
            if (translated.isNotBlank()) out[normalize(englishText)] = translated
        }
        return out
    }

    private fun parse(context: Context, code: String): Map<String, String> {
        val result = HashMap<String, String>()
        runCatching {
            val text = context.assets.open("i18n/$code.json").bufferedReader().use { it.readText() }
            flatten(JSONObject(text), "", result)
        }
        return result
    }

    private fun flatten(obj: JSONObject, prefix: String, out: MutableMap<String, String>) {
        for (key in obj.keys()) {
            val value = obj.get(key)
            val full = if (prefix.isEmpty()) key else "$prefix.$key"
            if (value is JSONObject) flatten(value, full, out) else out[full] = value.toString()
        }
    }

    /** Case, ellipsis and British/US spelling insensitive key. */
    fun normalize(text: String): String = text
        .lowercase(Locale.ROOT)
        .replace('\u2026', '.')
        .replace("favourit", "favorit")
        .replace("licence", "license")
        .replace("colour", "color")
        .replace(Regex("\\s+"), " ")
        .trim()
        .trimEnd('.', '!', '?', ':')

    /** Native language names shown in the pickers. */
    val LANGUAGES: List<Pair<String, String>> = listOf(
        "en" to "English",
        "tr" to "Türkçe",
        "es" to "Español",
        "de" to "Deutsch",
        "fr" to "Français",
        "it" to "Italiano",
        "pt" to "Português",
        "nl" to "Nederlands",
        "pl" to "Polski",
        "ru" to "Русский",
        "uk" to "Українська",
        "az" to "Azərbaycanca",
        "ka" to "ქართული",
        "ar" to "العربية",
        "hi" to "हिन्दी",
        "ja" to "日本語",
        "ko" to "한국어",
        "zh" to "中文",
    )
}
