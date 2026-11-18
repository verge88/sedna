package com.sedna.words.data

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

data class DictionaryResponse(
    val word: WordDraft?,
    val suggestions: List<String>,
    val error: String? = null
)

/**
 * Network access only after an explicit search. No API key, tracking SDK or proxy.
 * Search cache avoids repeated requests in a session; saved cards live in SQLite.
 * Providers can throttle traffic, so no claim of limitless public API availability is made.
 */
class DictionaryGateway {
    private val cache = ConcurrentHashMap<String, DictionaryResponse>()

    suspend fun lookup(input: String): DictionaryResponse = withContext(Dispatchers.IO) {
        val term = input.trim().lowercase(Locale.ROOT)
        if (term.isBlank() || term.length > 60 || !term.matches(Regex("[a-z][a-z '-]*"))) {
            return@withContext DictionaryResponse(null, emptyList(), "Введите английское слово.")
        }
        cache[term]?.let { return@withContext it }

        var word: WordDraft? = null
        var error: String? = null
        try {
            val data = getJson("https://api.dictionaryapi.dev/api/v2/entries/en/${Uri.encode(term)}")
            val entries = JSONArray(data)
            if (entries.length() > 0) word = decode(entries.getJSONObject(0))
        } catch (e: HttpError) {
            error = if (e.status == 404) "Слово не найдено в онлайн-словаре."
                else if (e.status == 429) "Сервис ограничил частоту запросов. Попробуйте позже."
                else "Ошибка словаря: HTTP ${e.status}"
        } catch (e: Exception) {
            error = "Нет ответа от словаря. Проверьте интернет."
        }

        val suggestions = try {
            val data = getJson("https://api.datamuse.com/sug?s=${Uri.encode(term)}&max=8")
            val items = JSONArray(data)
            buildList {
                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i).optString("word")
                    if (item.isNotBlank() && item != term) add(item)
                }
            }
        } catch (_: Exception) { emptyList() }

        DictionaryResponse(word, suggestions, error).also {
            // Cache confirmed entries only. Transient failures should be retryable.
            if (it.word != null) cache[term] = it
        }
    }

    private fun decode(item: org.json.JSONObject): WordDraft {
        val phonetics = item.optJSONArray("phonetics") ?: JSONArray()
        var phonetic = item.optString("phonetic")
        var audio = ""
        for (i in 0 until phonetics.length()) {
            val p = phonetics.optJSONObject(i) ?: continue
            if (phonetic.isBlank()) phonetic = p.optString("text")
            val candidate = p.optString("audio")
            if (audio.isBlank() && candidate.isNotBlank()) {
                audio = if (candidate.startsWith("//")) "https:$candidate" else candidate
            }
        }
        val meanings = item.optJSONArray("meanings") ?: JSONArray()
        var definition = ""
        var example = ""
        for (i in 0 until meanings.length()) {
            val meaning = meanings.optJSONObject(i) ?: continue
            val definitions = meaning.optJSONArray("definitions") ?: continue
            for (j in 0 until definitions.length()) {
                val d = definitions.optJSONObject(j) ?: continue
                if (definition.isBlank()) definition = d.optString("definition")
                if (example.isBlank()) example = d.optString("example")
            }
        }
        return WordDraft(
            term = item.optString("word"),
            definition = definition,
            example = example,
            phonetic = phonetic,
            audioUrl = if (audio.startsWith("https://")) audio else ""
        )
    }

    private fun getJson(address: String): String {
        val connection = (URL(address).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 8_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "SednaAndroid/1.0 (vocabulary study)")
        }
        try {
            val status = connection.responseCode
            if (status !in 200..299) throw HttpError(status)
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private class HttpError(val status: Int) : IOException("HTTP $status")
}
