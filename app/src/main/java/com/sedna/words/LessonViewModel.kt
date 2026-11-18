package com.sedna.words

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sedna.words.data.GeneratedLesson
import com.sedna.words.data.LessonDatabase
import com.sedna.words.data.LessonRequest
import com.sedna.words.data.OllamaLessonGenerator
import com.sedna.words.data.SavedLesson
import com.sedna.words.data.StudyPreferences
import com.sedna.words.data.WordDraft
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LessonViewModel(application: Application) : AndroidViewModel(application) {
    private val database = LessonDatabase(application)
    private val prefs = StudyPreferences(application)
    private val generator = OllamaLessonGenerator()
    private var generationJob: Job? = null

    var lessons by mutableStateOf<List<SavedLesson>>(emptyList())
        private set
    var activeLessonId by mutableStateOf<Long?>(null)
        private set
    var endpoint by mutableStateOf(prefs.ollamaEndpoint)
        private set
    var modelName by mutableStateOf(prefs.ollamaModel)
        private set
    var generating by mutableStateOf(false)
        private set
    var testing by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var connectionMessage by mutableStateOf<String?>(null)
        private set

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            lessons = withContext(Dispatchers.IO) { database.all() }
        }
    }

    fun changeEndpoint(value: String) {
        endpoint = value
        prefs.ollamaEndpoint = value
        connectionMessage = null
        error = null
    }

    fun changeModel(value: String) {
        modelName = value
        prefs.ollamaModel = value
        connectionMessage = null
        error = null
    }

    fun checkConnection() {
        if (testing || generating) return
        viewModelScope.launch {
            testing = true
            error = null
            connectionMessage = null
            try {
                connectionMessage = generator.checkConnection(endpoint, modelName)
            } catch (c: CancellationException) {
                throw c
            } catch (e: Exception) {
                error = e.message ?: "Не получилось подключиться к Ollama."
            } finally {
                testing = false
            }
        }
    }

    fun generate(request: LessonRequest, candidates: List<WordDraft>) {
        if (generating || testing) return
        generationJob = viewModelScope.launch {
            generating = true
            error = null
            connectionMessage = null
            try {
                val lesson = generator.generate(request, endpoint, modelName, candidates)
                val id = withContext(Dispatchers.IO) { database.save(lesson) }
                refresh()
                activeLessonId = id
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.message ?: "Не удалось создать урок. Повтори попытку."
            } finally {
                generating = false
                generationJob = null
            }
        }
    }

    fun cancelGeneration() {
        generationJob?.cancel()
        generationJob = null
        generating = false
    }

    fun openLesson(id: Long?) {
        activeLessonId = id
        error = null
    }

    fun complete(id: Long, score: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { database.complete(id, score) }
            refresh()
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { database.delete(id) }
            if (activeLessonId == id) activeLessonId = null
            refresh()
        }
    }

    override fun onCleared() {
        generationJob?.cancel()
        database.close()
        super.onCleared()
    }
}
