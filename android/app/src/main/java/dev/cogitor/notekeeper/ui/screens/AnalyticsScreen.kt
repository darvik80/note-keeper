package dev.cogitor.notekeeper.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api
import dev.cogitor.notekeeper.data.local.NoteDatabase
import dev.cogitor.notekeeper.data.local.entity.NoteEntity
import dev.cogitor.notekeeper.data.local.entity.TodoEntity
import dev.cogitor.notekeeper.ui.components.nkTopBarColors
import org.json.JSONArray
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.max

private enum class AnalyticsRange(val label: String, val days: Long) {
    Week("Week", 7),
    Month("Month", 30),
    Year("Year", 365)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = remember { NoteDatabase.getDatabase(context) }
    val notes by db.noteDao().getNotesForAnalytics().collectAsState(initial = emptyList())
    val todos by db.todoDao().getTodosForAnalytics().collectAsState(initial = emptyList())
    var range by remember { mutableStateOf(AnalyticsRange.Week) }

    val end = LocalDate.now()
    val start = end.minusDays(range.days - 1)
    val notesInRange = notes.filter { createdOn(it.createdAt)?.let { d -> !d.isBefore(start) && !d.isAfter(end) } == true }
    val todosInRange = todos.filter { createdOn(it.createdAt)?.let { d -> !d.isBefore(start) && !d.isAfter(end) } == true }
    val completed = todosInRange.count { it.completed }
    val completionRate = if (todosInRange.isEmpty()) 0.0 else completed * 100.0 / todosInRange.size

    val tagCounts = linkedMapOf<String, Int>()
    notes.forEach { note ->
        parseTags(note.tags).forEach { tag ->
            tagCounts[tag] = (tagCounts[tag] ?: 0) + 1
        }
    }
    todos.forEach { todo ->
        parseTags(todo.tags).forEach { tag ->
            tagCounts[tag] = (tagCounts[tag] ?: 0) + 1
        }
    }
    val topTags = tagCounts.entries.sortedByDescending { it.value }.take(10)
    val maxTag = topTags.firstOrNull()?.value ?: 1

    fun priorityCount(p: String) =
        notes.count { it.priority == p } + todos.count { it.priority == p }
    val priorityCounts = mapOf(
        "high" to priorityCount("high"),
        "medium" to priorityCount("medium"),
        "low" to priorityCount("low")
    )
    val priorityTotal = max(1, priorityCounts.values.sum())

    val activity = IntArray(range.days.toInt())
    (notesInRange.map(NoteEntity::createdAt) + todosInRange.map(TodoEntity::createdAt)).forEach { createdAt ->
        val day = createdOn(createdAt) ?: return@forEach
        val idx = ChronoUnit.DAYS.between(start, day).toInt()
        if (idx in activity.indices) activity[idx]++
    }
    val maxActivity = max(1, activity.maxOrNull() ?: 1)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Analytics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = nkTopBarColors()
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                "Activity over the selected range",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnalyticsRange.entries.forEach { option ->
                    FilterChip(
                        selected = range == option,
                        onClick = { range = option },
                        label = { Text(option.label) }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard("Notes", notesInRange.size.toString(), Modifier.weight(1f))
                StatCard("Todos", todosInRange.size.toString(), Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard("Completed", completed.toString(), Modifier.weight(1f))
                StatCard("Completion", String.format("%.1f%%", completionRate), Modifier.weight(1f))
            }

            Spacer(Modifier.height(18.dp))
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Activity", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.background)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        activity.forEach { count ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight(fraction = (count.toFloat() / maxActivity).coerceAtLeast(if (count > 0) 0.04f else 0f))
                                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Top tags", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    if (topTags.isEmpty()) {
                        Text(
                            "No tags yet",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        topTags.forEach { (tag, count) ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("#$tag", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                Box(
                                    Modifier
                                        .weight(1.2f)
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth(fraction = count.toFloat() / maxTag)
                                            .fillMaxHeight()
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    count.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Priority", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    PriorityBar("High", priorityCounts["high"] ?: 0, priorityTotal, MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(10.dp))
                    PriorityBar("Medium", priorityCounts["medium"] ?: 0, priorityTotal, MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(10.dp))
                    PriorityBar("Low", priorityCounts["low"] ?: 0, priorityTotal, MaterialTheme.colorScheme.outline)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PriorityBar(label: String, count: Int, total: Int, color: androidx.compose.ui.graphics.Color) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = color)
            Text(
                count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction = count.toFloat() / total)
                    .fillMaxHeight()
                    .background(color)
            )
        }
    }
}

private fun createdOn(raw: String?): LocalDate? {
    if (raw.isNullOrBlank()) return null
    return runCatching {
        when {
            raw.length >= 10 && raw[4] == '-' -> LocalDate.parse(raw.take(10))
            else -> Instant.parse(raw).atZone(ZoneId.systemDefault()).toLocalDate()
        }
    }.getOrNull()
}

private fun parseTags(raw: String): List<String> {
    return runCatching {
        val arr = JSONArray(raw)
        buildList {
            for (i in 0 until arr.length()) {
                val v = arr.optString(i).trim()
                if (v.isNotEmpty()) add(v)
            }
        }
    }.getOrElse { emptyList() }
}
