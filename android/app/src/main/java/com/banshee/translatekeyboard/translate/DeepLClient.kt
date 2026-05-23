package com.banshee.translatekeyboard.translate

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class DeepLUsage(
    val charactersUsed: Long,
    val charactersLimit: Long,
) {
    val percentUsed: Float
        get() = if (charactersLimit <= 0) 0f else (charactersUsed.toFloat() / charactersLimit).coerceIn(0f, 1f)
    val charactersRemaining: Long
        get() = (charactersLimit - charactersUsed).coerceAtLeast(0L)
}

class DeepLClient(
    private val apiKeyProvider: () -> String,
) {
    private val http = OkHttpClient.Builder()
        .callTimeout(8, TimeUnit.SECONDS)
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    suspend fun getUsage(): Result<DeepLUsage> = withContext(Dispatchers.IO) {
        val key = apiKeyProvider().trim()
        if (key.isBlank()) return@withContext Result.failure(IllegalStateException("Missing DeepL API key"))
        val host = if (key.endsWith(":fx")) "api-free.deepl.com" else "api.deepl.com"
        val req = Request.Builder()
            .url("https://$host/v2/usage")
            .header("Authorization", "DeepL-Auth-Key $key")
            .get()
            .build()
        runCatching {
            http.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) error("HTTP ${resp.code}: $raw")
                val json = JSONObject(raw)
                DeepLUsage(
                    charactersUsed = json.optLong("character_count", 0L),
                    charactersLimit = json.optLong("character_limit", 0L),
                )
            }
        }
    }

    suspend fun translate(text: String, targetLang: String): Result<String> = withContext(Dispatchers.IO) {
        val key = apiKeyProvider().trim()
        if (key.isBlank()) return@withContext Result.failure(IllegalStateException("Missing DeepL API key"))
        if (text.isBlank()) return@withContext Result.success("")

        val host = if (key.endsWith(":fx")) "api-free.deepl.com" else "api.deepl.com"
        val body = FormBody.Builder()
            .add("text", text)
            .add("target_lang", targetLang)
            .build()
        val req = Request.Builder()
            .url("https://$host/v2/translate")
            .header("Authorization", "DeepL-Auth-Key $key")
            .post(body)
            .build()

        runCatching {
            http.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) error("HTTP ${resp.code}: $raw")
                val translations = JSONObject(raw).getJSONArray("translations")
                translations.getJSONObject(0).getString("text")
            }
        }
    }
}
