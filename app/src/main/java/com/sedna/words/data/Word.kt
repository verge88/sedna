package com.sedna.words.data

import java.util.Locale
import kotlin.math.roundToInt

data class WordDraft(
    val term: String,
    val translation: String = "",
    val definition: String = "",
    val example: String = "",
    val phonetic: String = "",
    val level: String = "A1",
    val topic: String = "Из словаря",
    val audioUrl: String = ""
)

data class Word(
    val id: Long,
    val term: String,
    val translation: String,
    val definition: String,
    val example: String,
    val phonetic: String,
    val level: String,
    val topic: String,
    val audioUrl: String,
    val repetitions: Int,
    val intervalDays: Int,
    val ease: Double,
    val dueAt: Long,
    val createdAt: Long,
    val lastReviewedAt: Long,
    val correct: Int,
    val attempts: Int,
    val lapses: Int
) {
    val learned: Boolean get() = repetitions >= 3
    fun due(now: Long = System.currentTimeMillis()): Boolean = dueAt <= now
}

enum class Recall(val caption: String) {
    AGAIN("Снова"), HARD("Трудно"), GOOD("Помню"), EASY("Легко")
}

object Scheduler {
    private const val DAY = 86_400_000L
    private const val TEN_MINUTES = 600_000L

    fun review(card: Word, quality: Recall, now: Long): Word {
        val nextEase = (card.ease + when (quality) {
            Recall.AGAIN -> -0.20
            Recall.HARD -> -0.10
            Recall.GOOD -> 0.0
            Recall.EASY -> 0.15
        }).coerceIn(1.3, 3.2)
        val nextInterval = when (quality) {
            Recall.AGAIN -> 0
            Recall.HARD -> maxOf(1, (card.intervalDays * 1.2).roundToInt())
            Recall.GOOD -> when (card.repetitions) {
                0 -> 1
                1 -> 3
                else -> maxOf(1, (card.intervalDays * nextEase).roundToInt())
            }
            Recall.EASY -> if (card.repetitions == 0) 4
                else maxOf(2, (card.intervalDays * nextEase * 1.3).roundToInt())
        }
        return card.copy(
            repetitions = if (quality == Recall.AGAIN) 0 else card.repetitions + 1,
            intervalDays = nextInterval,
            ease = nextEase,
            dueAt = now + if (quality == Recall.AGAIN) TEN_MINUTES else nextInterval * DAY,
            lastReviewedAt = now,
            attempts = card.attempts + 1,
            correct = card.correct + if (quality == Recall.AGAIN) 0 else 1,
            lapses = card.lapses + if (quality == Recall.AGAIN) 1 else 0
        )
    }
}

fun normalizeAnswer(text: String): String = text.lowercase(Locale.ROOT)
    .trim().replace(Regex("[^\\p{L}\\p{N}]+"), "")
