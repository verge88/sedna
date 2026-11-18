package com.sedna.words.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.KeyboardVoice
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sedna.words.LessonViewModel
import com.sedna.words.SednaViewModel
import com.sedna.words.data.LessonRequest
import com.sedna.words.data.SavedLesson
import com.sedna.words.data.WordDraft
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LessonsScreen(
    model: LessonViewModel,
    wordsModel: SednaViewModel,
    speaker: Pronouncer,
    onLibrary: () -> Unit
) {
    val active = model.lessons.firstOrNull { it.id == model.activeLessonId }
    if (active == null) {
        LessonsHome(model, wordsModel)
    } else {
        LessonPlayer(
            active,
            onExit = { model.openLesson(null) },
            onComplete = { score -> model.complete(active.id, score) },
            onSaveWords = { wordsModel.saveLessonWords(active.lesson) },
            speaker = speaker,
            onLibrary = onLibrary
        )
    }
}

@Composable
private fun LessonsHome(model: LessonViewModel, wordsModel: SednaViewModel) {
    var topic by rememberSaveable { mutableStateOf("Путешествия") }
    var level by rememberSaveable { mutableStateOf("A2") }
    var count by rememberSaveable { mutableIntStateOf(4) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<SavedLesson?>(null) }
    val topics = listOf(
        "Путешествия", "Каждый день", "Работа и карьера",
        "Еда и напитки", "Технологии и интернет", "Природа и мир"
    )
    if (pendingDelete != null) {
        val record = pendingDelete!!
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Удалить урок?") },
            text = { Text("«${record.lesson.title}» исчезнет из сохранённых уроков.") },
            confirmButton = {
                TextButton(onClick = { model.delete(record.id); pendingDelete = null }) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Отмена") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 22.dp, end = 22.dp, top = 22.dp, bottom = 34.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Eyebrow("AI / СЛОВАРНЫЕ УРОКИ")
            Spacer(Modifier.height(9.dp))
            Text("Урок, созданный\nдля тебя.", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(9.dp))
            Text(
                "Открытая языковая модель подберёт слова, примеры и проверочные задания " +
                    "по твоей теме и уровню. Сохранённые уроки доступны офлайн.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            OutlinePanel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(19.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Eyebrow("01 / ТЕМА")
                    OutlinedTextField(
                        value = topic, onValueChange = { topic = it.take(60) },
                        label = { Text("Какие слова изучаем?") },
                        placeholder = { Text("например: В ресторане") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        topics.forEach { preset ->
                            FilterChip(
                                selected = topic == preset,
                                onClick = { topic = preset },
                                label = { Text(preset) }
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Eyebrow("02 / УРОВЕНЬ ENGLISH")
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("A1", "A2", "B1", "B2", "C1").forEach { option ->
                            FilterChip(
                                selected = level == option,
                                onClick = { level = option },
                                label = { Text(option) }
                            )
                        }
                    }
                    Eyebrow("03 / СЛОВ В УРОКЕ")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(4, 6, 8).forEach { option ->
                            FilterChip(
                                selected = count == option,
                                onClick = { count = option },
                                label = { Text("$option слов") }
                            )
                        }
                    }
                }
            }
        }
        item {
            val candidateWords = remember(topic, level, wordsModel.words) {
                val saved = wordsModel.words.map {
                    WordDraft(
                        term = it.term, translation = it.translation, level = it.level,
                        topic = it.topic, definition = it.definition, example = it.example
                    )
                }
                (saved + wordsModel.catalog).filter { it.level == level }
                    .distinctBy { it.term.lowercase(Locale.ROOT) }
                    .sortedByDescending {
                        it.topic.contains(topic.trim(), ignoreCase = true) ||
                            topic.contains(it.topic, ignoreCase = true)
                    }.take(20)
            }
            Button(
                onClick = { model.generate(LessonRequest(topic.trim(), level, count), candidateWords) },
                enabled = !model.generating && !model.testing && topic.trim().length in 3..60,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Сгенерировать урок · $count слов") }
            if (model.generating) {
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.height(24.dp))
                    Text("Модель составляет урок. Первый запуск может быть долгим.",
                        style = MaterialTheme.typography.bodyMedium)
                }
                TextButton(onClick = { model.cancelGeneration() }) { Text("Отменить генерацию") }
            }
            if (model.error != null) {
                Spacer(Modifier.height(10.dp))
                Text(model.error.orEmpty(), style = MaterialTheme.typography.bodyMedium)
            }
            if (model.connectionMessage != null) {
                Spacer(Modifier.height(10.dp))
                Text(model.connectionMessage.orEmpty(), style = MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            OutlinePanel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Eyebrow("ЛОКАЛЬНАЯ OPEN-SOURCE LLM")
                            Text("Ollama · ${model.modelName}", style = MaterialTheme.typography.titleMedium)
                        }
                        TextButton(onClick = { settingsOpen = !settingsOpen }) {
                            Text(if (settingsOpen) "Скрыть" else "Настроить")
                        }
                    }
                    Text(
                        "Модель работает на компьютере или своём сервере. Платный ключ не нужен.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (settingsOpen) {
                        OutlinedTextField(
                            value = model.endpoint,
                            onValueChange = model::changeEndpoint,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Адрес Ollama") },
                            singleLine = true,
                            placeholder = { Text("http://192.168.1.5:11434") }
                        )
                        OutlinedTextField(
                            value = model.modelName,
                            onValueChange = model::changeModel,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Модель в Ollama") },
                            singleLine = true
                        )
                        Text(
                            "Эмулятор: http://10.0.2.2:11434. Телефон: локальный IP компьютера. " +
                                "Для доступа из интернета нужен HTTPS. Не открывай Ollama в публичную сеть.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = { model.checkConnection() },
                            enabled = !model.testing && !model.generating,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (model.testing) "Проверяем подключение…" else "Проверить Ollama и модель")
                        }
                    }
                }
            }
        }
        item {
            SectionHeading("04 / ЛОКАЛЬНО", "Сохранённые уроки")
            Spacer(Modifier.height(5.dp))
            Text(
                "Упражнения остаются доступными без Ollama и без интернета.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (model.lessons.isEmpty()) {
            item {
                EmptyState("Уроков пока нет", "Создай первый урок и начни расширять словарный запас.")
            }
        }
        items(model.lessons, key = { it.id }) { item ->
            OutlinePanel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Eyebrow(
                        "${item.lesson.level} · ${item.lesson.words.size} СЛОВ · " +
                            SimpleDateFormat("dd.MM.yyyy", Locale("ru")).format(Date(item.createdAt))
                    )
                    Text(item.lesson.title, style = MaterialTheme.typography.titleLarge)
                    Text(item.lesson.summary, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (item.bestScore != null) {
                        Eyebrow("ЛУЧШИЙ РЕЗУЛЬТАТ: ${item.bestScore}/${item.lesson.questions.size}")
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        IconButton(onClick = { pendingDelete = item }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Удалить урок")
                        }
                        Button(onClick = { model.openLesson(item.id) }) {
                            Text(if (item.completedAt == null) "Открыть урок" else "Пройти снова")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonPlayer(
    record: SavedLesson,
    speaker: Pronouncer,
    onExit: () -> Unit,
    onComplete: (Int) -> Unit,
    onSaveWords: () -> Unit,
    onLibrary: () -> Unit
) {
    val lesson = record.lesson
    var phase by rememberSaveable(record.id) { mutableIntStateOf(0) }
    var index by rememberSaveable(record.id) { mutableIntStateOf(0) }
    var score by rememberSaveable(record.id) { mutableIntStateOf(0) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(17.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 22.dp, end = 22.dp, top = 18.dp, bottom = 36.dp
        )
    ) {
        item {
            TextButton(onClick = onExit) {
                Icon(Icons.Rounded.ArrowBack, null)
                Text("Все уроки", modifier = Modifier.padding(start = 8.dp))
            }
            Eyebrow("${lesson.level} · ${lesson.topic} · ${lesson.model}")
            Spacer(Modifier.height(10.dp))
            Text(lesson.title, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(lesson.summary, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        when (phase) {
            0 -> {
                item {
                    OutlinePanel(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Eyebrow("ТВОЙ ПЕРСОНАЛЬНЫЙ УРОК")
                            Text("${lesson.words.size} слов · ${lesson.questions.size} заданий",
                                style = MaterialTheme.typography.headlineSmall)
                            Text("Сначала разберём слова с примерами, потом проверим знания.",
                                style = MaterialTheme.typography.bodyMedium)
                            Button(onClick = { phase = 1 }, modifier = Modifier.fillMaxWidth()) {
                                Text("Начать изучение")
                            }
                        }
                    }
                }
            }
            1 -> {
                item {
                    SectionHeading("01 / СЛОВА", "Разбери слова")
                    Spacer(Modifier.height(3.dp))
                    Text("Нажимай на значок произношения, слушай и повторяй.",
                        style = MaterialTheme.typography.bodyMedium)
                }
                items(lesson.words, key = { it.term }) { word ->
                    OutlinePanel(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Text(word.term, style = MaterialTheme.typography.headlineSmall)
                                IconButton(onClick = { speaker.speak(word.term) }) {
                                    Icon(Icons.Rounded.KeyboardVoice, "Произнести ${word.term}")
                                }
                            }
                            Text(word.translation, style = MaterialTheme.typography.titleMedium)
                            Text(word.explanation, style = MaterialTheme.typography.bodyMedium)
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Eyebrow("В КОНТЕКСТЕ")
                            Text(word.example, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                item {
                    Button(
                        onClick = { phase = 2; index = 0; score = 0 },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Проверить знания") }
                }
            }
            2 -> {
                val task = lesson.questions[index]
                item(key = "task-${record.id}-$index") {
                    var selected by rememberSaveable(record.id, index) { mutableIntStateOf(-1) }
                    OutlinePanel(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(13.dp)
                        ) {
                            Row(Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                Eyebrow("02 / ПРОВЕРКА")
                                Eyebrow("${index + 1} / ${lesson.questions.size}")
                            }
                            LinearProgressIndicator(
                                progress = { index.toFloat() / lesson.questions.size },
                                modifier = Modifier.fillMaxWidth().height(6.dp)
                            )
                            Text(task.prompt, style = MaterialTheme.typography.headlineSmall)
                            task.options.forEachIndexed { i, option ->
                                OutlinedButton(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { selected = i },
                                    enabled = selected < 0
                                ) {
                                    Text("${i + 1}. $option",
                                        modifier = Modifier.padding(vertical = 6.dp))
                                }
                            }
                            if (selected >= 0) {
                                Eyebrow(if (selected == task.correctIndex)
                                    "ВЕРНО" else "ОБРАТИ ВНИМАНИЕ")
                                Text("Правильно: ${task.options[task.correctIndex]}",
                                    style = MaterialTheme.typography.titleMedium)
                                Text(task.explanation, style = MaterialTheme.typography.bodyMedium)
                                Button(onClick = {
                                    val newScore = score + if (selected == task.correctIndex) 1 else 0
                                    score = newScore
                                    if (index == lesson.questions.lastIndex) {
                                        phase = 3
                                        onComplete(newScore)
                                    } else index++
                                }, modifier = Modifier.fillMaxWidth()) {
                                    Text(if (index == lesson.questions.lastIndex)
                                        "Завершить урок" else "Следующий вопрос")
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                item {
                    OutlinePanel(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(23.dp), verticalArrangement = Arrangement.spacedBy(15.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Eyebrow("03 / УРОК ПРОЙДЕН")
                            Text("$score / ${lesson.questions.size}",
                                style = MaterialTheme.typography.headlineLarge)
                            Text(
                                if (score == lesson.questions.size) "Все ответы верные!"
                                else "Результат сохранён. Ошибки помогут запомнить слова.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Button(onClick = onSaveWords, modifier = Modifier.fillMaxWidth()) {
                                Text("Добавить слова урока в словарь")
                            }
                            OutlinedButton(
                                onClick = { phase = 1; index = 0; score = 0 },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Повторить урок") }
                            TextButton(onClick = onLibrary) { Text("Перейти в мой словарь") }
                        }
                    }
                }
            }
        }
        item {
            Text(
                "Урок создан языковой моделью. Возможны неточности в переводе и заданиях; " +
                    "проверяй незнакомые выражения в словаре.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
