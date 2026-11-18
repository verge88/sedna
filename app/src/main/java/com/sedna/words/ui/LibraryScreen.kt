package com.sedna.words.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.KeyboardVoice
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sedna.words.SednaViewModel
import com.sedna.words.data.Word

private enum class DictionaryFilter(val text: String) {
    ALL("Все"), DUE("Повторить"), LEARNED("Выучено")
}

@Composable
fun LibraryScreen(model: SednaViewModel, speaker: Pronouncer) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(DictionaryFilter.ALL) }
    var expanded by rememberSaveable { mutableLongStateOf(-1L) }
    var toDelete by remember { mutableStateOf<Word?>(null) }
    val visible = model.words.filter {
        (it.term.contains(query, true) || it.translation.contains(query, true)) &&
            when (filter) {
                DictionaryFilter.ALL -> true
                DictionaryFilter.DUE -> it.due()
                DictionaryFilter.LEARNED -> it.learned
            }
    }

    if (toDelete != null) {
        val card = toDelete!!
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Удалить слово?") },
            text = { Text("«${card.term}» исчезнет из словаря и истории повторений.") },
            confirmButton = {
                TextButton(onClick = { model.remove(card.id); toDelete = null }) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) { Text("Отмена") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 22.dp, end = 22.dp, top = 25.dp, bottom = 30.dp
        )
    ) {
        item {
            Eyebrow("МОЙ СЛОВАРЬ / КОЛЛЕКЦИЯ")
            Spacer(Modifier.height(10.dp))
            Text("Твоя личная\nколлекция.", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(6.dp))
            Text("${model.words.size} слов сохранено на устройстве.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text("Поиск по слову или переводу") },
                label = { Text("Фильтр словаря") }
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DictionaryFilter.entries.forEach { option ->
                    FilterChip(
                        selected = filter == option,
                        onClick = { filter = option },
                        label = { Text(option.text) }
                    )
                }
            }
        }
        if (visible.isEmpty()) {
            item {
                EmptyState(
                    if (model.words.isEmpty()) "Словарь пока пуст" else "Ничего не найдено",
                    if (model.words.isEmpty()) "Добавь набор или найди первое слово в поиске."
                        else "Измени фильтры, чтобы увидеть другие карточки."
                )
            }
        }
        items(visible, key = { it.id }) { word ->
            OutlinePanel(Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable { expanded = if (expanded == word.id) -1L else word.id }
                            .padding(17.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Eyebrow("${word.level} · ${word.topic}")
                            Text(word.term, style = MaterialTheme.typography.titleLarge)
                            Text(word.translation.ifBlank { "Перевод не указан" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { speaker.speak(word.term) }) {
                            Icon(Icons.Rounded.KeyboardVoice, contentDescription = "Произнести")
                        }
                    }
                    if (expanded == word.id) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Column(
                            Modifier.padding(17.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (word.phonetic.isNotBlank()) Text(word.phonetic)
                            if (word.definition.isNotBlank()) {
                                Eyebrow("ЗНАЧЕНИЕ")
                                Text(word.definition, style = MaterialTheme.typography.bodyMedium)
                            }
                            if (word.example.isNotBlank()) {
                                Eyebrow("В КОНТЕКСТЕ")
                                Text(word.example, style = MaterialTheme.typography.bodyMedium)
                            }
                            Eyebrow("ПОВТОРЕНИЙ: ${word.attempts} · ${if (word.learned) "ИЗУЧЕНО" else "В ПРОЦЕССЕ"}")
                            var edited by remember(word.id, word.translation) {
                                mutableStateOf(word.translation)
                            }
                            OutlinedTextField(
                                value = edited,
                                onValueChange = { edited = it },
                                label = { Text("Мой перевод") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                OutlinedButton(onClick = { toDelete = word }) {
                                    Icon(Icons.Rounded.DeleteOutline, null)
                                    Text("Удалить")
                                }
                                Button(
                                    onClick = { model.changeTranslation(word.id, edited) },
                                    enabled = edited.isNotBlank() && edited != word.translation
                                ) { Text("Сохранить") }
                            }
                        }
                    }
                }
            }
        }
    }
}
