package com.sedna.words

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sedna.words.data.DictionaryGateway
import com.sedna.words.data.GeneratedLesson
import com.sedna.words.data.Recall
import com.sedna.words.data.SeedCatalog
import com.sedna.words.data.StudyPreferences
import com.sedna.words.data.Word
import com.sedna.words.data.WordDatabase
import com.sedna.words.data.WordDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class SearchState(
    val query: String = "",
    val loading: Boolean = false,
    val result: WordDraft? = null,
    val suggestions: List<String> = emptyList(),
    val error: String? = null
)

class SednaViewModel(application: Application) : AndroidViewModel(application) {
    private val database = WordDatabase(application)
    private val preferences = StudyPreferences(application)
    private val dictionary = DictionaryGateway()
    private var searchJob: Job? = null

    val catalog: List<WordDraft> = SeedCatalog.load(application)
    var words by mutableStateOf<List<Word>>(emptyList())
        private set
    var search by mutableStateOf(SearchState())
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var darkMode by mutableStateOf(preferences.darkMode)
        private set
    var dailyGoal by mutableIntStateOf(preferences.dailyGoal)
        private set
    var todayCount by mutableIntStateOf(preferences.todayCount)
        private set
    var streak by mutableIntStateOf(preferences.streak)
        private set

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            words = withContext(Dispatchers.IO) { database.all() }
            todayCount = preferences.todayCount
            streak = preferences.streak
        }
    }

    fun clearMessage() { message = null }

    fun toggleTheme() {
        darkMode = !darkMode
        preferences.darkMode = darkMode
    }

    fun setGoal(goal: Int) {
        dailyGoal = goal
        preferences.dailyGoal = goal
    }

    fun find(input: String) {
        val term = input.trim()
        searchJob?.cancel()
        if (term.isBlank()) {
            search = SearchState()
            return
        }
        val fallback = catalog.firstOrNull { it.term.equals(term, true) }
        search = SearchState(query = term, loading = true, result = fallback)
        searchJob = viewModelScope.launch {
            val remote = dictionary.lookup(term)
            val enriched = remote.word?.copy(
                translation = fallback?.translation.orEmpty(),
                topic = fallback?.topic ?: "Из API",
                level = fallback?.level ?: "A1",
                example = remote.word.example.ifBlank { fallback?.example.orEmpty() }
            ) ?: fallback
            search = SearchState(
                query = term, loading = false, result = enriched,
                suggestions = remote.suggestions,
                error = if (enriched == null) remote.error else null
            )
        }
    }

    fun save(draft: WordDraft) {
        if (draft.term.isBlank()) return
        viewModelScope.launch {
            val added = withContext(Dispatchers.IO) { database.add(draft) }
            message = if (added) "Слово добавлено в словарь" else "Слово уже сохранено"
            refresh()
        }
    }

    fun savePack(topic: String) {
        val entries = catalog.filter { it.topic == topic }
        viewModelScope.launch {
            val count = withContext(Dispatchers.IO) {
                entries.count { database.add(it) }
            }
            message = "Добавлено слов: $count"
            refresh()
        }
    }

    fun saveLessonWords(lesson: GeneratedLesson) {
        viewModelScope.launch {
            val added = withContext(Dispatchers.IO) {
                lesson.words.count { word ->
                    database.add(WordDraft(
                        term = word.term,
                        translation = word.translation,
                        definition = word.explanation,
                        example = word.example,
                        level = lesson.level,
                        topic = lesson.topic
                    ))
                }
            }
            message = if (added > 0) "Добавлено слов из урока: $added"
                else "Все слова этого урока уже есть в словаре"
            refresh()
        }
    }

    fun remove(id: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { database.remove(id) }
            message = "Слово удалено"
            refresh()
        }
    }

    fun changeTranslation(id: Long, value: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { database.updateTranslation(id, value) }
            message = "Перевод сохранён"
            refresh()
        }
    }

    fun grade(id: Long, answer: Recall) {
        viewModelScope.launch {
            val changed = withContext(Dispatchers.IO) {
                database.review(id, answer, System.currentTimeMillis())
            }
            if (changed) preferences.recordReview()
            refresh()
        }
    }

    /** Portable JSON vocabulary backup. Existing words are never overwritten on import. */
    fun exportWords(uri: Uri) {
        val snapshot = words
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val json = JSONArray()
                    for (word in snapshot) {
                        json.put(JSONObject().apply {
                            put("term", word.term)
                            put("translation", word.translation)
                            put("definition", word.definition)
                            put("example", word.example)
                            put("phonetic", word.phonetic)
                            put("level", word.level)
                            put("topic", word.topic)
                        })
                    }
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                        out.writer(Charsets.UTF_8).use { it.write(json.toString(2)) }
                    } ?: error("Нельзя открыть файл")
                }
                message = "Словарь экспортирован"
            } catch (_: Exception) { message = "Не удалось экспортировать словарь" }
        }
    }

    fun importWords(uri: Uri) {
        viewModelScope.launch {
            try {
                val count = withContext(Dispatchers.IO) {
                    val text = getApplication<Application>().contentResolver.openInputStream(uri)
                        ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                        ?: error("Нельзя прочитать файл")
                    val json = JSONArray(text)
                    require(json.length() <= 20_000) { "Файл слишком большой" }
                    var added = 0
                    for (i in 0 until json.length()) {
                        val item = json.getJSONObject(i)
                        val term = item.optString("term").trim()
                        if (term.isBlank() || term.length > 60) continue
                        if (database.add(WordDraft(
                            term = term,
                            translation = item.optString("translation").take(500),
                            definition = item.optString("definition").take(1500),
                            example = item.optString("example").take(1500),
                            phonetic = item.optString("phonetic").take(100),
                            level = item.optString("level", "A1").take(12),
                            topic = item.optString("topic", "Импорт").take(80)
                        ))) added++
                    }
                    added
                }
                message = "Импортировано слов: $count"
                refresh()
            } catch (_: Exception) { message = "Файл не подходит для импорта" }
        }
    }

    override fun onCleared() {
        database.close()
        super.onCleared()
    }
}
