package com.sedna.words.data

import org.json.JSONArray
import org.json.JSONObject

data class LessonRequest(val topic: String, val level: String, val count: Int) {
    init {
        require(topic.trim().length in 3..60) { "Укажи тему от 3 до 60 символов." }
        require(level in listOf("A1", "A2", "B1", "B2", "C1")) { "Неизвестный уровень." }
        require(count in listOf(4, 6, 8)) { "Можно выбрать 4, 6 или 8 слов." }
    }
}

data class LessonWord(
    val term: String,
    val translation: String,
    val explanation: String,
    val example: String
)

data class LessonQuestion(
    val term: String,
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class GeneratedLesson(
    val title: String,
    val summary: String,
    val topic: String,
    val level: String,
    val model: String,
    val words: List<LessonWord>,
    val questions: List<LessonQuestion>
)

data class SavedLesson(
    val id: Long,
    val lesson: GeneratedLesson,
    val createdAt: Long,
    val completedAt: Long?,
    val bestScore: Int?
)

class InvalidLessonException(message: String) : IllegalArgumentException(message)

/** Whitelisted, bounded, deterministic validation. LLM output is not executable code. */
object LessonCodec {
    const val MAX_RESPONSE = 96_000

    fun parse(response: String, request: LessonRequest, model: String): GeneratedLesson {
        if (response.length > MAX_RESPONSE) throw InvalidLessonException("Ответ модели слишком большой.")
        val root = try { JSONObject(response.trim()) } catch (_: Exception) {
            throw InvalidLessonException("Модель вернула не JSON. Повтори генерацию.")
        }
        val wordsJson = root.optJSONArray("words")
            ?: throw InvalidLessonException("В ответе отсутствует список слов.")
        val tasksJson = root.optJSONArray("questions")
            ?: throw InvalidLessonException("В ответе отсутствуют упражнения.")
        if (wordsJson.length() != request.count || tasksJson.length() != request.count) {
            throw InvalidLessonException("Модель вернула неверное число слов или заданий.")
        }
        fun text(obj: JSONObject, field: String, max: Int): String {
            val value = obj.optString(field).trim()
            if (value.isBlank() || value.length > max) {
                throw InvalidLessonException("Некорректное поле: $field.")
            }
            return value
        }
        val words = (0 until wordsJson.length()).map { i ->
            val obj = wordsJson.optJSONObject(i)
                ?: throw InvalidLessonException("Некорректная словарная карточка.")
            val term = text(obj, "term", 50)
            if (!term.matches(Regex("[a-zA-Z][a-zA-Z '-]*"))) {
                throw InvalidLessonException("Модель вернула некорректное английское слово.")
            }
            LessonWord(
                term = term,
                translation = text(obj, "translation", 120),
                explanation = text(obj, "explanation", 450),
                example = text(obj, "example", 220)
            )
        }
        val terms = words.map { it.term.lowercase() }
        if (terms.distinct().size != request.count) {
            throw InvalidLessonException("Модель повторила одно и то же слово.")
        }
        val questions = (0 until tasksJson.length()).map { i ->
            val obj = tasksJson.optJSONObject(i)
                ?: throw InvalidLessonException("Некорректное упражнение.")
            val term = text(obj, "term", 50)
            if (term.lowercase() !in terms) {
                throw InvalidLessonException("Упражнение не соответствует словам урока.")
            }
            val optionsJson = obj.optJSONArray("options")
                ?: throw InvalidLessonException("Нет вариантов ответа.")
            if (optionsJson.length() != 4) {
                throw InvalidLessonException("В тесте должно быть четыре варианта.")
            }
            val options = (0 until 4).map { j ->
                val option = optionsJson.optString(j).trim()
                if (option.isEmpty() || option.length > 50) {
                    throw InvalidLessonException("Некорректный вариант ответа.")
                }
                option
            }
            if (options.map { it.lowercase() }.distinct().size != 4 ||
                options.any { it.lowercase() !in terms }) {
                throw InvalidLessonException("Варианты должны быть уникальными словами урока.")
            }
            val index = obj.optInt("correctIndex", -1)
            if (index !in 0..3 || !options[index].equals(term, true)) {
                throw InvalidLessonException("Упражнение содержит неправильный ключ ответа.")
            }
            LessonQuestion(
                term = term,
                prompt = text(obj, "prompt", 350),
                options = options,
                correctIndex = index,
                explanation = text(obj, "explanation", 450)
            )
        }
        if (questions.map { it.term.lowercase() }.distinct().size != request.count) {
            throw InvalidLessonException("Для каждого слова требуется своё упражнение.")
        }
        return GeneratedLesson(
            title = text(root, "title", 100),
            summary = text(root, "summary", 450),
            topic = request.topic.trim(),
            level = request.level,
            model = model,
            words = words,
            questions = questions
        )
    }

    fun encode(lesson: GeneratedLesson): String = JSONObject().apply {
        put("title", lesson.title)
        put("summary", lesson.summary)
        put("topic", lesson.topic)
        put("level", lesson.level)
        put("model", lesson.model)
        put("words", JSONArray().apply {
            lesson.words.forEach { item ->
                put(JSONObject().apply {
                    put("term", item.term)
                    put("translation", item.translation)
                    put("explanation", item.explanation)
                    put("example", item.example)
                })
            }
        })
        put("questions", JSONArray().apply {
            lesson.questions.forEach { item ->
                put(JSONObject().apply {
                    put("term", item.term)
                    put("prompt", item.prompt)
                    put("options", JSONArray(item.options))
                    put("correctIndex", item.correctIndex)
                    put("explanation", item.explanation)
                })
            }
        })
    }.toString()

    fun decode(saved: String): GeneratedLesson {
        val json = JSONObject(saved)
        val request = LessonRequest(
            json.getString("topic"), json.getString("level"),
            json.getJSONArray("words").length()
        )
        return parse(json.toString(), request, json.getString("model"))
    }
}
