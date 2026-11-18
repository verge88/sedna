package com.sedna.words.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sedna.words.SednaViewModel

@Composable
fun HomeScreen(model: SednaViewModel, onStart: () -> Unit, onExplore: () -> Unit) {
    val cards = model.words
    val due = cards.count { it.due() }
    val learned = cards.count { it.learned }
    val progress = (model.todayCount.toFloat() / model.dailyGoal).coerceIn(0f, 1f)
    val packs = model.catalog.groupBy { it.topic }.entries.toList()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(21.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 22.dp, end = 22.dp, top = 28.dp, bottom = 28.dp
        )
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Eyebrow("01 — ТВОЙ ЕЖЕДНЕВНЫЙ ПРОГРЕСС")
                Text("Слова, которые\nостаются с тобой.",
                    style = MaterialTheme.typography.headlineLarge)
                Text("Учи новое, повторяй вовремя и замечай, как растёт твой словарный запас.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Metric("В СЛОВАРЕ", "${cards.size}", Modifier.weight(1f))
                Metric("ПОВТОРИТЬ", "$due", Modifier.weight(1f))
                Metric("ВЫУЧЕНО", "$learned", Modifier.weight(1f))
            }
        }
        item {
            OutlinePanel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Eyebrow("ЦЕЛЬ НА ДЕНЬ")
                        Text("${model.todayCount} / ${model.dailyGoal}",
                            style = MaterialTheme.typography.labelLarge)
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(7.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text("Серия занятий: ${model.streak} дн.",
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            val foreground = if (model.darkMode) Color.Black else Color.White
            val background = if (model.darkMode) Color.White else Color.Black
            Surface(
                color = background,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("ЕСТЬ ${due} СЛОВ К ПОВТОРЕНИЮ",
                        style = MaterialTheme.typography.labelSmall, color = foreground)
                    Text("Пять минут сегодня —\nпамять надолго.",
                        style = MaterialTheme.typography.headlineSmall, color = foreground)
                    Button(
                        onClick = onStart,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = foreground, contentColor = background
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (due > 0) "Начать повторение" else "Перейти к тренировке")
                        Icon(Icons.Rounded.ArrowForward, null, Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
        item {
            SectionHeading("02 / УРОКИ", "Наборы по темам")
            Spacer(Modifier.height(8.dp))
            Text("Готовые тематические подборки работают даже без интернета.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(packs.take(4), key = { it.key }) { pack ->
            OutlinePanel(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(17.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.weight(1f)) {
                        Eyebrow("${pack.value.first().level} • ${pack.value.size} СЛОВ")
                        Text(pack.key, style = MaterialTheme.typography.titleMedium)
                    }
                    IconButton(onClick = { model.savePack(pack.key) }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Добавить набор ${pack.key}")
                    }
                }
            }
        }
        item {
            Button(onClick = onExplore, modifier = Modifier.fillMaxWidth()) {
                Text("Все наборы и поиск слов")
                Icon(Icons.Rounded.ArrowForward, null, Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun Metric(label: String, amount: String, modifier: Modifier = Modifier) {
    OutlinePanel(modifier) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Eyebrow(label)
            Text(amount, style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black)
        }
    }
}
