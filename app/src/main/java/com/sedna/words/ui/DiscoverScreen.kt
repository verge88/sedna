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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.KeyboardVoice
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.sedna.words.SednaViewModel

@Composable
fun DiscoverScreen(model: SednaViewModel, speaker: Pronouncer) {
    var query by rememberSaveable { mutableStateOf("") }
    var expandedTopic by rememberSaveable { mutableStateOf<String?>(null) }
    val state = model.search
    val packs = model.catalog.groupBy { it.topic }.entries.toList()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 22.dp, end = 22.dp, top = 25.dp, bottom = 30.dp
        )
    ) {
        item {
            Eyebrow("ПОИСК / СЛОВАРЬ")
            Spacer(Modifier.height(10.dp))
            Text("Открой новое\nслово.", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(8.dp))
            Text("Определения и транскрипция — Free Dictionary API. Подсказки — Datamuse.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Слово на английском") },
                    placeholder = { Text("например: discover") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { model.find(query) }) {
                            Icon(Icons.Rounded.Search, "Найти")
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { model.find(query) })
                )
                Button(
                    onClick = { model.find(query) },
                    enabled = query.isNotBlank() && !state.loading,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Найти в онлайн-словаре") }
            }
        }
        if (state.loading) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(modifier = Modifier.height(22.dp))
                    Text("Загружаем словарную статью…")
                }
            }
        }
        if (state.result != null) {
            item(key = "result-${state.result.term}") {
                val draft = state.result
                var translation by remember(draft.term, draft.definition) {
                    mutableStateOf(draft.translation)
                }
                val saved = model.words.any { it.term.equals(draft.term, ignoreCase = true) }
                OutlinePanel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Eyebrow("${draft.level} • СЛОВАРНАЯ СТАТЬЯ")
                                Text(draft.term, style = MaterialTheme.typography.headlineMedium)
                                if (draft.phonetic.isNotBlank()) {
                                    Text(draft.phonetic, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(onClick = { speaker.speak(draft.term) }) {
                                Icon(Icons.Rounded.KeyboardVoice, "Произнести")
                            }
                        }
                        if (draft.definition.isNotBlank()) {
                            Eyebrow("ОПРЕДЕЛЕНИЕ")
                            Text(draft.definition, style = MaterialTheme.typography.bodyLarge)
                        }
                        if (draft.example.isNotBlank()) {
                            Eyebrow("ПРИМЕР")
                            Text(draft.example, style = MaterialTheme.typography.bodyMedium)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        OutlinedTextField(
                            value = translation,
                            onValueChange = { translation = it },
                            label = { Text("Перевод на русский (можно редактировать)") },
                            placeholder = { Text("Введите перевод") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = { model.save(draft.copy(translation = translation)) },
                            enabled = !saved && translation.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Add, null)
                            Text(if (saved) "Уже в моём словаре" else "Добавить в мой словарь",
                                modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        }
        if (state.error != null) {
            item {
                Text(state.error, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (state.suggestions.isNotEmpty()) {
            item {
                Eyebrow("ПОХОЖИЕ СЛОВА")
                Spacer(Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.suggestions.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            pair.forEach { suggestion ->
                                AssistChip(
                                    onClick = { query = suggestion; model.find(suggestion) },
                                    label = { Text(suggestion) }
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(5.dp))
            SectionHeading("01 / ОФЛАЙН", "Учебные наборы")
            Spacer(Modifier.height(6.dp))
            Text("Выбирай тему, просматривай слова или добавляй весь набор одной кнопкой.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(packs, key = { it.key }) { pack ->
            OutlinePanel(Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            expandedTopic = if (expandedTopic == pack.key) null else pack.key
                        }.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Eyebrow("${pack.value.first().level} / ${pack.value.size} СЛОВ")
                            Text(pack.key, style = MaterialTheme.typography.titleMedium)
                        }
                        OutlinedButton(onClick = { model.savePack(pack.key) }) {
                            Icon(Icons.Rounded.Add, "Добавить всё")
                            Text("Все")
                        }
                    }
                    if (expandedTopic == pack.key) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        pack.value.forEach { item ->
                            val saved = model.words.any { it.term.equals(item.term, true) }
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(item.term, style = MaterialTheme.typography.titleMedium)
                                    Text(item.translation, style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { model.save(item) }, enabled = !saved) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Добавить ${item.term}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
