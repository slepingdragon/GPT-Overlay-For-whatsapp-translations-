package com.banshee.translatekeyboard.settings

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class HistoryEntry(
    val id: String,
    val source: String,
    val translation: String,
    val targetLang: String,
    val timestamp: Long,
    val favorite: Boolean = false,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("source", source)
        put("translation", translation)
        put("targetLang", targetLang)
        put("timestamp", timestamp)
        put("favorite", favorite)
    }

    companion object {
        fun fromJson(o: JSONObject): HistoryEntry = HistoryEntry(
            id = o.optString("id", UUID.randomUUID().toString()),
            source = o.optString("source"),
            translation = o.optString("translation"),
            targetLang = o.optString("targetLang", "ID"),
            timestamp = o.optLong("timestamp", System.currentTimeMillis()),
            favorite = o.optBoolean("favorite", false),
        )

        fun create(source: String, translation: String, targetLang: TargetLang): HistoryEntry = HistoryEntry(
            id = UUID.randomUUID().toString(),
            source = source,
            translation = translation,
            targetLang = targetLang.name,
            timestamp = System.currentTimeMillis(),
            favorite = false,
        )
    }
}

class Prefs(context: Context) {
    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences("translate_kb", Context.MODE_PRIVATE)

    var deeplApiKey: String
        get() = sp.getString(KEY_API, "") ?: ""
        set(value) = sp.edit().putString(KEY_API, value.trim()).apply()

    var targetLang: TargetLang
        get() = TargetLang.valueOf(sp.getString(KEY_TARGET, TargetLang.ID.name) ?: TargetLang.ID.name)
        set(value) = sp.edit().putString(KEY_TARGET, value.name).apply()

    var defaultContactNumber: String
        get() = sp.getString(KEY_CONTACT, "") ?: ""
        set(value) = sp.edit().putString(KEY_CONTACT, value.filter { it.isDigit() }).apply()

    var backgroundImageUri: String
        get() = sp.getString(KEY_BG_URI, "") ?: ""
        set(value) = sp.edit().putString(KEY_BG_URI, value).apply()

    var copyButtonColor: Int
        get() = sp.getInt(KEY_COPY_COLOR, DEFAULT_COPY_COLOR)
        set(value) = sp.edit().putInt(KEY_COPY_COLOR, value).apply()

    var sendButtonColor: Int
        get() = sp.getInt(KEY_SEND_COLOR, DEFAULT_SEND_COLOR)
        set(value) = sp.edit().putInt(KEY_SEND_COLOR, value).apply()

    var inputBoxColor: Int
        get() = sp.getInt(KEY_INPUT_COLOR, DEFAULT_INPUT_COLOR)
        set(value) = sp.edit().putInt(KEY_INPUT_COLOR, value).apply()

    var accentColor: Int
        get() = sp.getInt(KEY_ACCENT_COLOR, DEFAULT_ACCENT_COLOR)
        set(value) = sp.edit().putInt(KEY_ACCENT_COLOR, value).apply()

    var customColors: List<Int>
        get() = (sp.getString(KEY_CUSTOM_COLORS, "") ?: "")
            .split(",")
            .mapNotNull { it.trim().takeIf { it.isNotEmpty() }?.let { hex ->
                runCatching { hex.toLong(16).toInt() }.getOrNull()
            } }
        set(value) = sp.edit().putString(
            KEY_CUSTOM_COLORS,
            value.joinToString(",") { (it.toLong() and 0xFFFFFFFFL).toString(16).padStart(8, '0') },
        ).apply()

    fun addCustomColor(c: Int) {
        val list = customColors.toMutableList()
        list.remove(c)
        list.add(0, c)
        customColors = list.take(24)
    }

    fun removeCustomColor(c: Int) {
        customColors = customColors.filter { it != c }
    }

    var autoTranslate: Boolean
        get() = sp.getBoolean(KEY_AUTO_TRANSLATE, true)
        set(value) = sp.edit().putBoolean(KEY_AUTO_TRANSLATE, value).apply()

    var uiOpacity: Float
        get() = sp.getFloat(KEY_UI_OPACITY, 1.0f)
        set(value) = sp.edit().putFloat(KEY_UI_OPACITY, value.coerceIn(0f, 1f)).apply()

    var favoriteLanguages: List<TargetLang>
        get() {
            val raw = sp.getString(KEY_ENABLED_LANGS, null)
            if (raw.isNullOrBlank()) return listOf(TargetLang.EN, TargetLang.ID, TargetLang.SU)
            return raw.split(",").mapNotNull { name ->
                runCatching { TargetLang.valueOf(name.trim()) }.getOrNull()
            }.ifEmpty { listOf(TargetLang.EN, TargetLang.ID, TargetLang.SU) }
        }
        set(value) {
            val sanitized = if (value.isEmpty()) listOf(TargetLang.EN) else value
            sp.edit().putString(KEY_ENABLED_LANGS, sanitized.joinToString(",") { it.name }).apply()
        }

    fun toggleFavoriteLanguage(lang: TargetLang) {
        val current = favoriteLanguages.toMutableList()
        if (lang in current) current.remove(lang) else current.add(lang)
        favoriteLanguages = current
    }

    var savedSource: String
        get() = sp.getString(KEY_SAVED_SOURCE, "") ?: ""
        set(value) = sp.edit().putString(KEY_SAVED_SOURCE, value).apply()

    var savedTranslation: String
        get() = sp.getString(KEY_SAVED_TRANSLATION, "") ?: ""
        set(value) = sp.edit().putString(KEY_SAVED_TRANSLATION, value).apply()

    fun loadHistory(): List<HistoryEntry> {
        val raw = sp.getString(KEY_HISTORY, "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { HistoryEntry.fromJson(arr.getJSONObject(it)) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveHistory(entries: List<HistoryEntry>) {
        val arr = JSONArray()
        entries.take(HISTORY_LIMIT).forEach { arr.put(it.toJson()) }
        sp.edit().putString(KEY_HISTORY, arr.toString()).apply()
    }

    fun addHistoryEntry(source: String, translation: String, targetLang: TargetLang) {
        if (source.isBlank() || translation.isBlank()) return
        val existing = loadHistory().toMutableList()
        val dupeIdx = existing.indexOfFirst {
            it.source.trim() == source.trim() && it.targetLang == targetLang.name
        }
        val now = System.currentTimeMillis()
        if (dupeIdx >= 0) {
            val merged = existing[dupeIdx].copy(timestamp = now, translation = translation)
            existing.removeAt(dupeIdx)
            existing.add(0, merged)
        } else {
            existing.add(0, HistoryEntry.create(source, translation, targetLang))
        }
        saveHistory(existing)
    }

    fun deleteHistoryEntry(id: String) {
        saveHistory(loadHistory().filter { it.id != id })
    }

    fun toggleFavorite(id: String) {
        saveHistory(loadHistory().map { if (it.id == id) it.copy(favorite = !it.favorite) else it })
    }

    fun clearAllHistory() {
        saveHistory(emptyList())
    }

    companion object {
        private const val KEY_API = "deepl_api_key"
        private const val KEY_TARGET = "target_lang"
        private const val KEY_CONTACT = "default_contact_number"
        private const val KEY_BG_URI = "background_image_uri"
        private const val KEY_COPY_COLOR = "copy_button_color"
        private const val KEY_SEND_COLOR = "send_button_color"
        private const val KEY_INPUT_COLOR = "input_box_color"
        private const val KEY_ACCENT_COLOR = "accent_color"
        private const val KEY_CUSTOM_COLORS = "custom_colors"
        private const val KEY_AUTO_TRANSLATE = "auto_translate"
        private const val KEY_UI_OPACITY = "ui_opacity"
        private const val KEY_ENABLED_LANGS = "enabled_languages"
        private const val KEY_SAVED_SOURCE = "saved_source"
        private const val KEY_SAVED_TRANSLATION = "saved_translation"
        private const val KEY_HISTORY = "history_v1"
        private const val HISTORY_LIMIT = 500
        const val DEFAULT_COPY_COLOR: Int = 0xFF00E5FF.toInt()
        const val DEFAULT_SEND_COLOR: Int = 0xFFFFFFFF.toInt()
        const val DEFAULT_INPUT_COLOR: Int = 0x8C1A1D24.toInt() // ~55% alpha dark surface
        const val DEFAULT_ACCENT_COLOR: Int = 0xFFFFFFFF.toInt()

        val COLOR_PRESETS: List<Int> = listOf(
            0xFFFFFFFF.toInt(), // white
            0xFF000000.toInt(), // black
            0xFF00E5FF.toInt(), // cyan
            0xFF1E88E5.toInt(), // blue
            0xFF7C4DFF.toInt(), // indigo
            0xFFBA68C8.toInt(), // purple
            0xFFFF4081.toInt(), // pink
            0xFFEF5350.toInt(), // red
            0xFFFF7043.toInt(), // orange
            0xFFFFB300.toInt(), // amber
            0xFFFFD600.toInt(), // yellow
            0xFF66BB6A.toInt(), // green
            0xFF26A69A.toInt(), // teal
            0xFF00ACC1.toInt(), // cyan-deep
            0xFFB0BEC5.toInt(), // light grey
            0xFF607D8B.toInt(), // blue-grey
        )
    }
}

enum class TargetLang(
    val deeplCode: String?,
    val myMemoryCode: String,
    val flag: String,
    val label: String,
    val fullName: String,
) {
    EN("EN-US", "en", "🇬🇧", "EN", "English"),
    ID("ID", "id", "🇮🇩", "ID", "Indonesian"),
    SU(null, "su", "🏝", "SU", "Sundanese"),
    ES("ES", "es", "🇪🇸", "ES", "Spanish"),
    FR("FR", "fr", "🇫🇷", "FR", "French"),
    DE("DE", "de", "🇩🇪", "DE", "German"),
    IT("IT", "it", "🇮🇹", "IT", "Italian"),
    PT("PT-BR", "pt", "🇧🇷", "PT", "Portuguese"),
    NL("NL", "nl", "🇳🇱", "NL", "Dutch"),
    PL("PL", "pl", "🇵🇱", "PL", "Polish"),
    RU("RU", "ru", "🇷🇺", "RU", "Russian"),
    JA("JA", "ja", "🇯🇵", "JA", "Japanese"),
    KO("KO", "ko", "🇰🇷", "KO", "Korean"),
    ZH("ZH", "zh", "🇨🇳", "ZH", "Chinese"),
    AR("AR", "ar", "🇸🇦", "AR", "Arabic"),
    TR("TR", "tr", "🇹🇷", "TR", "Turkish"),
}
