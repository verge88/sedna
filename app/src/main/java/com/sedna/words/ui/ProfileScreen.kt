package com.sedna.words.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sedna.words.SednaViewModel

@Composable
fun ProfileScreen(model: SednaViewModel) {
    val export = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) model.exportWords(uri) }
    val import = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) model.importWords(uri) }
    val words = model.words
    val totalAttempts = words.sumOf { it.attempts }
    val correct = words.sumOf { it.correct }
    val accuracy = if (totalAttempts == 0) 0 else (correct * 100 / totalAttempts)

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 22.dp, end = 22.dp, top = 25.dp, bottom = 35.dp
        )
    ) {
        item {
            Eyebrow("ПРОФИЛЬ / РЕЗУЛЬТАТЫ")
            Spacer(Modifier.height(10.dp))
            Text("Маленькие шаги.\nБольшой словарь.",
                style = MaterialTheme.typography.headlineLarge)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("СЛОВ", "${words.size}", Modifier.weight(1f))
                StatTile("ТОЧНОСТЬ", "$accuracy%", Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("ДНЕЙ ПОДРЯД", "${model.streak}", Modifier.weight(1f))
                StatTile("ОТВЕТОВ", "$totalAttempts", Modifier.weight(1f))
            }
        }
        item {
            SectionHeading("01 / УРОВНИ", "Изучаемые уровни")
            Spacer(Modifier.height(14.dp))
            OutlinePanel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
                    listOf("A1", "A2", "B1", "B2").forEach { level ->
                        val count = words.count { it.level == level }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(level, style = MaterialTheme.typography.titleMedium)
                            Text("$count", style = MaterialTheme.typography.bodyMedium)
                        }
                        LinearProgressIndicator(
                            progress = { if (words.isEmpty()) 0f else count.toFloat() / words.size },
                            modifier = Modifier.fillMaxWidth().height(5.dp),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
        item {
            SectionHeading("02 / НАСТРОЙКИ", "Мой ритм")
            Spacer(Modifier.height(12.dp))
            OutlinePanel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
                    Eyebrow("ЦЕЛЬ ПОВТОРЕНИЙ НА ДЕНЬ")
                    Row(
                        Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = { model.setGoal(model.dailyGoal - 5) },
                            enabled = model.dailyGoal > 5) {
                            Icon(Icons.Rounded.Remove, "Уменьшить")
                        }
                        Text("${model.dailyGoal} слов",
                            style = MaterialTheme.typography.headlineSmall)
                        IconButton(onClick = { model.setGoal(model.dailyGoal + 5) },
                            enabled = model.dailyGoal < 50) {
                            Icon(Icons.Rounded.Add, "Увеличить")
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Тёмная тема", style = MaterialTheme.typography.titleMedium)
                            Text("Только чёрный, белый и серый",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = model.darkMode, onCheckedChange = { model.toggleTheme() })
                    }
                }
            }
        }
        item {
            SectionHeading("03 / ДАННЫЕ", "Мой словарь")
            Spacer(Modifier.height(12.dp))
            Text("Экспорт и импорт в JSON. Данные не отправляются на сервер Sedna.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            Button(onClick = { export.launch("sedna-words.json") },
                modifier = Modifier.fillMaxWidth()) { Text("Экспортировать слова") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { import.launch("application/json") },
                modifier = Modifier.fillMaxWidth()) { Text("Импортировать слова") }
        }
        item {
            SectionHeading("04 / ИСТОЧНИКИ", "О приложении")
            Spacer(Modifier.height(10.dp))
            Text(
                "Sedna — офлайн-словарь и интервальное повторение. " +
                    "Онлайн-определения: Free Dictionary API (dictionaryapi.dev). " +
                    "Подсказки: Datamuse (datamuse.com). " +
                    "Публичные API имеют правила использования и могут ограничивать запросы. " +
                    "Datamuse требует ключ с 1 февраля 2027 года.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Eyebrow("SEDNA / VERSION 1.0")
        }
    }
}

@Composable
private fun StatTile(name: String, value: String, modifier: Modifier = Modifier) {
    OutlinePanel(modifier) {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Eyebrow(name)
            Text(value, style = MaterialTheme.typography.headlineMedium)
        }
    }
}
