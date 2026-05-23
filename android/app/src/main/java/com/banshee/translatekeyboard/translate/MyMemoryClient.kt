package com.banshee.translatekeyboard.translate

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class MyMemoryClient {
    private val http = OkHttpClient.Builder()
        .callTimeout(8, TimeUnit.SECONDS)
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    suspend fun translate(text: String, sourceLang: String, targetLang: String): Result<String> = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext Result.success("")

        val url = "https://api.mymemory.translated.net/get".toHttpUrl().newBuilder()
            .addQueryParameter("q", text)
            .addQueryParameter("langpair", "$sourceLang|$targetLang")
            .build()

        val req = Request.Builder().url(url).get().build()
        runCatching {
            http.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) error("HTTP ${resp.code}: $raw")
                val data = JSONObject(raw).getJSONObject("responseData")
                data.getString("translatedText")
            }
        }
    }
}

object SundaneseHeuristics {
    private val indonesianHints = listOf(
        " yang ", " saya ", " kamu ", " tidak ", " sudah ", " kalau ",
        " saja ", " juga ", " ini ", " itu ", " bisa ", " akan ", " dan ",
    )

    fun guessSourceFor(text: String): String {
        val padded = " ${text.lowercase()} "
        val hits = indonesianHints.count { padded.contains(it) }
        return if (hits >= 1) "id" else "en"
    }
}
