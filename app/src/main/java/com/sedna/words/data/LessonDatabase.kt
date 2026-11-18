package com.sedna.words.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Separate versioned database; the existing vocabulary DB remains unchanged. */
class LessonDatabase(context: Context) : SQLiteOpenHelper(context, "sedna_lessons.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE generated_lessons (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                payload TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                completed_at INTEGER,
                best_score INTEGER
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX lessons_created_idx ON generated_lessons(created_at DESC)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Use explicit, non-destructive migrations when new columns are introduced.
    }

    fun all(): List<SavedLesson> = readableDatabase.query(
        "generated_lessons", null, null, null, null, null, "created_at DESC"
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                runCatching { add(cursor.toSavedLesson()) }
            }
        }
    }

    fun save(lesson: GeneratedLesson): Long {
        val values = ContentValues().apply {
            put("payload", LessonCodec.encode(lesson))
            put("created_at", System.currentTimeMillis())
        }
        return writableDatabase.insertOrThrow("generated_lessons", null, values)
    }

    fun complete(id: Long, correct: Int): Boolean {
        val db = writableDatabase
        val previous = db.query(
            "generated_lessons", arrayOf("best_score"), "id = ?",
            arrayOf(id.toString()), null, null, null
        ).use { cursor ->
            if (!cursor.moveToFirst()) return false
            if (cursor.isNull(0)) null else cursor.getInt(0)
        }
        val values = ContentValues().apply {
            put("completed_at", System.currentTimeMillis())
            put("best_score", maxOf(correct, previous ?: 0))
        }
        return db.update("generated_lessons", values, "id = ?", arrayOf(id.toString())) > 0
    }

    fun delete(id: Long) {
        writableDatabase.delete("generated_lessons", "id = ?", arrayOf(id.toString()))
    }

    private fun Cursor.toSavedLesson(): SavedLesson = SavedLesson(
        id = getLong(getColumnIndexOrThrow("id")),
        lesson = LessonCodec.decode(getString(getColumnIndexOrThrow("payload"))),
        createdAt = getLong(getColumnIndexOrThrow("created_at")),
        completedAt = getColumnIndexOrThrow("completed_at").let {
            if (isNull(it)) null else getLong(it)
        },
        bestScore = getColumnIndexOrThrow("best_score").let {
            if (isNull(it)) null else getInt(it)
        }
    )
}
