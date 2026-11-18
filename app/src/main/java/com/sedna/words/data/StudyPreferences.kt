package com.sedna.words.data

import android.content.Context
import java.time.LocalDate

class StudyPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("sedna_prefs", Context.MODE_PRIVATE)

    var darkMode: Boolean
        get() = prefs.getBoolean("dark_mode", false)
        set(value) { prefs.edit().putBoolean("dark_mode", value).apply() }

    var dailyGoal: Int
        get() = prefs.getInt("daily_goal", 15)
        set(value) { prefs.edit().putInt("daily_goal", value.coerceIn(5, 50)).apply() }

    var ollamaEndpoint: String
        get() = prefs.getString("ollama_endpoint", OllamaLessonGenerator.DEFAULT_ENDPOINT)
            ?: OllamaLessonGenerator.DEFAULT_ENDPOINT
        set(value) { prefs.edit().putString("ollama_endpoint", value.take(260)).apply() }

    var ollamaModel: String
        get() = prefs.getString("ollama_model", OllamaLessonGenerator.DEFAULT_MODEL)
            ?: OllamaLessonGenerator.DEFAULT_MODEL
        set(value) { prefs.edit().putString("ollama_model", value.take(100)).apply() }

    val todayCount: Int
        get() = if (prefs.getString("today", null) == LocalDate.now().toString())
            prefs.getInt("today_count", 0) else 0

    val streak: Int
        get() {
            val last = prefs.getString("today", null)
            val today = LocalDate.now()
            return if (last == today.toString() || last == today.minusDays(1).toString())
                prefs.getInt("streak", 0) else 0
        }

    fun recordReview() {
        val today = LocalDate.now()
        val last = prefs.getString("today", null)
        if (last == today.toString()) {
            prefs.edit().putInt("today_count", todayCount + 1).apply()
        } else {
            val streak = if (last == today.minusDays(1).toString())
                prefs.getInt("streak", 0) + 1 else 1
            prefs.edit().putString("today", today.toString())
                .putInt("today_count", 1).putInt("streak", streak).apply()
        }
    }
}
