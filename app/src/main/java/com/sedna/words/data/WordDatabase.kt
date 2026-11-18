package com.sedna.words.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class WordDatabase(context: Context) : SQLiteOpenHelper(context, "sedna.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE cards (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                term TEXT NOT NULL COLLATE NOCASE UNIQUE,
                translation TEXT NOT NULL,
                definition TEXT NOT NULL,
                example TEXT NOT NULL,
                phonetic TEXT NOT NULL,
                level TEXT NOT NULL,
                topic TEXT NOT NULL,
                audio_url TEXT NOT NULL,
                repetitions INTEGER NOT NULL DEFAULT 0,
                interval_days INTEGER NOT NULL DEFAULT 0,
                ease REAL NOT NULL DEFAULT 2.5,
                due_at INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL,
                last_reviewed_at INTEGER NOT NULL DEFAULT 0,
                correct INTEGER NOT NULL DEFAULT 0,
                attempts INTEGER NOT NULL DEFAULT 0,
                lapses INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX ix_cards_due ON cards(due_at)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Deliberately no destructive migration: add versioned ALTER TABLE changes here.
    }

    fun all(): List<Word> = readableDatabase.query(
        "cards", null, null, null, null, null, "created_at DESC"
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) add(cursor.toWord())
        }
    }

    fun add(draft: WordDraft): Boolean {
        val values = ContentValues().apply {
            put("term", draft.term.trim().lowercase())
            put("translation", draft.translation.trim())
            put("definition", draft.definition.trim())
            put("example", draft.example.trim())
            put("phonetic", draft.phonetic.trim())
            put("level", draft.level)
            put("topic", draft.topic)
            put("audio_url", draft.audioUrl)
            put("created_at", System.currentTimeMillis())
        }
        return writableDatabase.insertWithOnConflict(
            "cards", null, values, SQLiteDatabase.CONFLICT_IGNORE
        ) != -1L
    }

    fun remove(id: Long) {
        writableDatabase.delete("cards", "id = ?", arrayOf(id.toString()))
    }

    fun updateTranslation(id: Long, translation: String) {
        val values = ContentValues().apply { put("translation", translation.trim()) }
        writableDatabase.update("cards", values, "id = ?", arrayOf(id.toString()))
    }

    fun review(id: Long, quality: Recall, now: Long): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val original = db.query(
                "cards", null, "id = ?", arrayOf(id.toString()), null, null, null
            ).use { cursor -> if (cursor.moveToFirst()) cursor.toWord() else null }
                ?: return false
            val next = Scheduler.review(original, quality, now)
            val values = ContentValues().apply {
                put("repetitions", next.repetitions)
                put("interval_days", next.intervalDays)
                put("ease", next.ease)
                put("due_at", next.dueAt)
                put("last_reviewed_at", next.lastReviewedAt)
                put("correct", next.correct)
                put("attempts", next.attempts)
                put("lapses", next.lapses)
            }
            db.update("cards", values, "id = ?", arrayOf(id.toString()))
            db.setTransactionSuccessful()
            return true
        } finally {
            db.endTransaction()
        }
    }

    private fun Cursor.toWord(): Word {
        fun str(name: String) = getString(getColumnIndexOrThrow(name))
        fun int(name: String) = getInt(getColumnIndexOrThrow(name))
        fun long(name: String) = getLong(getColumnIndexOrThrow(name))
        return Word(
            id = long("id"), term = str("term"), translation = str("translation"),
            definition = str("definition"), example = str("example"),
            phonetic = str("phonetic"), level = str("level"), topic = str("topic"),
            audioUrl = str("audio_url"), repetitions = int("repetitions"),
            intervalDays = int("interval_days"), ease = getDouble(getColumnIndexOrThrow("ease")),
            dueAt = long("due_at"), createdAt = long("created_at"),
            lastReviewedAt = long("last_reviewed_at"), correct = int("correct"),
            attempts = int("attempts"), lapses = int("lapses")
        )
    }
}
