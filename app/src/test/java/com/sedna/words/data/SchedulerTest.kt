package com.sedna.words.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SchedulerTest {
    private val time = 1_800_000_000_000L

    private fun newCard() = Word(
        id = 1, term = "learn", translation = "учить",
        definition = "To acquire knowledge", example = "I learn daily.",
        phonetic = "", level = "A1", topic = "Study", audioUrl = "",
        repetitions = 0, intervalDays = 0, ease = 2.5, dueAt = 0,
        createdAt = time, lastReviewedAt = 0, correct = 0, attempts = 0, lapses = 0
    )

    @Test fun newCardIsDueImmediately() {
        assertTrue(newCard().due(time))
    }

    @Test fun goodSchedulesNextDay() {
        val next = Scheduler.review(newCard(), Recall.GOOD, time)
        assertEquals(1, next.repetitions)
        assertEquals(1, next.intervalDays)
        assertEquals(time + 86_400_000, next.dueAt)
        assertEquals(1, next.correct)
        assertEquals(1, next.attempts)
    }

    @Test fun repeatedGoodReviewsSpaceOut() {
        val first = Scheduler.review(newCard(), Recall.GOOD, time)
        val second = Scheduler.review(first, Recall.GOOD, time + 86_400_000)
        val third = Scheduler.review(second, Recall.GOOD, time + 4 * 86_400_000)
        assertEquals(3, second.intervalDays)
        assertTrue(third.intervalDays > second.intervalDays)
        assertTrue(third.learned)
    }

    @Test fun againResetsStreakOfCorrectAnswers() {
        val good = Scheduler.review(newCard(), Recall.GOOD, time)
        val again = Scheduler.review(good, Recall.AGAIN, time + 1000)
        assertEquals(0, again.repetitions)
        assertEquals(time + 601_000, again.dueAt)
        assertEquals(1, again.lapses)
        assertEquals(1, again.correct)
        assertEquals(2, again.attempts)
    }

    @Test fun easyStartsAtFourDays() {
        val next = Scheduler.review(newCard(), Recall.EASY, time)
        assertEquals(4, next.intervalDays)
        assertEquals(time + 4 * 86_400_000L, next.dueAt)
    }

    @Test fun hardNeverSchedulesZeroDays() {
        val next = Scheduler.review(newCard(), Recall.HARD, time)
        assertEquals(1, next.intervalDays)
    }

    @Test fun answerNormalizationIgnoresCaseAndPunctuation() {
        assertEquals(normalizeAnswer("  HELLO!  "), normalizeAnswer("hello"))
        assertEquals(normalizeAnswer("По-русски"), normalizeAnswer("по русски"))
    }
}
