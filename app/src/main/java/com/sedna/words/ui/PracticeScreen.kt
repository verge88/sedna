package com.sedna.words.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardVoice
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.sedna.words.SednaViewModel
import com.sedna.words.data.Recall
import com.sedna.words.data.Word
import com.sedna.words.data.normalizeAnswer

private enum class Exercise(val label: String) {
    CARDS("Карточки"), WRITE("Написание"), QUIZ("Тест")
}

@Composable
fun PracticeScreen(model: SednaViewModel, speaker: Pronouncer, onFind: () -> Unit) {
    var mode by rememberSaveable { mutableStateOf(Exercise.CARDS) }
    var reviewAll by rememberSaveable { mutableStateOf(false) }
    val completed = remember { mutableStateListOf<Long>() }
    val eligible = model.words.filter { reviewAll || it.due() }
    val card = eligible.firstOrNull { it.id !in completed }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(19.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 22.dp, end = 22.dp, top = 25.dp, bottom = 35.dp
        )
    ) {
        item {
            Eyebrow("ПРАКТИКА / ИНТЕРВАЛЬНОЕ ПОВТОРЕНИЕ")
            Spacer(Modifier.height(10.dp))
            Text("Запомни\nнавсегда.", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(8.dp))
            Text("Ответы определяют дату следующего повторения.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Exercise.entries.forEach { option ->
                    FilterChip(
                        selected = mode == option,
                        onClick = { mode = option },
                        label = { Text(option.label) }
                    )
                }
            }
        }
        if (model.words.isEmpty()) {
            item {
                EmptyState("Сначала добавим слова",
                    "Выбери тематический набор или найди слово в онлайн-словаре.")
                Button(onClick = onFind, modifier = Modifier.fillMaxWidth()) {
                    Text("Открыть словарь")
                }
            }
        } else if (card == null) {
            item {
                EmptyState(
                    if (completed.isNotEmpty()) "Тренировка завершена!" else "Сегодня всё повторено",
                    if (completed.isNotEmpty()) "Сделано карточек: ${completed.size}."
                    else "Новые карточки появятся по расписанию повторений."
                )
                Button(
                    onClick = { reviewAll = true; completed.clear() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (reviewAll) "Начать заново" else "Тренировать все слова") }
            }
        } else {
            item {
                Row(
                    Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Eyebrow(if (reviewAll) "СВОБОДНАЯ ТРЕНИРОВКА" else "ПОВТОРЕНИЯ СЕГОДНЯ")
                    Eyebrow("ГОТОВО: ${completed.size}")
                }
            }
            item(key = "card-${card.id}-${mode.name}") {
                // Reinitialize exercise input on each new card or switched exercise.
                key(card.id, mode) {
                    val next: (Recall) -> Unit = { result ->
                        completed.add(card.id)
                        model.grade(card.id, result)
                    }
                    when (mode) {
                        Exercise.CARDS -> Flashcard(card, speaker, next)
                        Exercise.WRITE -> SpellingExercise(card, next)
                        Exercise.QUIZ -> QuizExercise(card, model.words, next)
                    }
                }
            }
        }
        item {
            Text(
                "Повторение: «Снова» — через 10 мин, «Помню» — по растущему интервалу. " +
                    "Все данные хранятся только на устройстве.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Flashcard(card: Word, speaker: Pronouncer, next: (Recall) -> Unit) {
    var revealed by remember { mutableStateOf(false) }
    OutlinePanel(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Eyebrow("ВСПОМНИ ПЕРЕВОД")
            Text(card.term, style = MaterialTheme.typography.headlineLarge)
            if (card.phonetic.isNotBlank()) Text(card.phonetic)
            IconButton(onClick = { speaker.speak(card.term) }) {
                Icon(Icons.Rounded.KeyboardVoice, "Произнести")
            }
            if (!revealed) {
                Spacer(Modifier.height(28.dp))
                Button(onClick = { revealed = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Показать ответ")
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(15.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Eyebrow("ПЕРЕВОД")
                        Text(card.translation.ifBlank { card.definition },
                            style = MaterialTheme.typography.headlineSmall)
                        if (card.example.isNotBlank()) {
                            Text(card.example, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                Eyebrow("НАСКОЛЬКО ЛЕГКО ВСПОМНИЛ?")
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf(Recall.AGAIN, Recall.HARD).forEach { quality ->
                        OutlinedButton(onClick = { next(quality) },
                            modifier = Modifier.weight(1f)) {
                            Text(quality.caption)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf(Recall.GOOD, Recall.EASY).forEach { quality ->
                        Button(onClick = { next(quality) },
                            modifier = Modifier.weight(1f)) {
                            Text(quality.caption)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpellingExercise(card: Word, next: (Recall) -> Unit) {
    var answer by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    val correct = normalizeAnswer(answer) == normalizeAnswer(card.term)
    OutlinePanel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Eyebrow("НАПИШИ ПО-АНГЛИЙСКИ")
            Text(card.translation.ifBlank { card.definition },
                style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(
                value = answer, onValueChange = { if (!submitted) answer = it },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                label = { Text("Английское слово") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (answer.isNotBlank()) submitted = true
                })
            )
            if (submitted) {
                Eyebrow(if (correct) "ПРАВИЛЬНО" else "ПРАВИЛЬНЫЙ ОТВЕТ")
                Text(card.term, style = MaterialTheme.typography.headlineSmall)
                if (card.example.isNotBlank()) Text(card.example)
                Button(
                    onClick = { next(if (correct) Recall.GOOD else Recall.AGAIN) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Следующее слово") }
            } else {
                Button(
                    onClick = { submitted = true }, enabled = answer.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Проверить") }
            }
        }
    }
}

@Composable
private fun QuizExercise(card: Word, cards: List<Word>, next: (Recall) -> Unit) {
    val choices = remember(card.id, cards.size) {
        (cards.filter { it.id != card.id && it.term != card.term }
            .shuffled().take(3).map { it.term } + card.term).shuffled()
    }
    var selection by remember { mutableStateOf<String?>(null) }
    val correct = selection == card.term
    OutlinePanel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Eyebrow("ВЫБЕРИ АНГЛИЙСКОЕ СЛОВО")
            Text(card.translation.ifBlank { card.definition },
                style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(5.dp))
            choices.forEach { option ->
                OutlinedButton(
                    onClick = { selection = option },
                    enabled = selection == null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(option, modifier = Modifier.padding(vertical = 5.dp))
                }
            }
            if (selection != null) {
                Eyebrow(if (correct) "ВЕРНО!" else "ПОПРОБУЙ ЗАПОМНИТЬ")
                Text(if (correct) "Отличная работа." else "Правильный ответ: ${card.term}")
                Button(
                    onClick = { next(if (correct) Recall.GOOD else Recall.AGAIN) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Продолжить") }
            }
        }
    }
}
