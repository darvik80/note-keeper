package xyz.crearts.notekeeper.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import xyz.crearts.notekeeper.data.model.Todo
import xyz.crearts.notekeeper.ui.viewmodel.TodoListViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoListScreen(
    viewModel: TodoListViewModel,
    onTodoClick: (String) -> Unit,
    onAddClick: () -> Unit,
    onNotesClick: () -> Unit
) {
    val todos by viewModel.todos.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val showCompleted by viewModel.showCompleted.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    SingleChoiceSegmentedButtonRow {
                        SegmentedButton(
                            selected = false,
                            onClick = onNotesClick,
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            icon = { Text("📝") }
                        ) { Text("Notes") }
                        SegmentedButton(
                            selected = true,
                            onClick = { },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            icon = { Text("✅") }
                        ) { Text("Todos") }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.syncTodos() }) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Text("🔄")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Text("➕")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (syncStatus.isNotEmpty()) {
                Text(
                    text = syncStatus,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showCompleted) "Completed" else "Active",
                    style = MaterialTheme.typography.titleSmall
                )
                TextButton(onClick = { viewModel.toggleShowCompleted() }) {
                    Text(if (showCompleted) "Show Active" else "Show Completed")
                }
            }
            if (todos.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No todos yet.\nTap + to create one.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(todos, key = { it.id }) { todo ->
                        TodoItem(
                            todo = todo,
                            onClick = { onTodoClick(todo.id) },
                            onToggleComplete = { viewModel.toggleComplete(todo) },
                            onDelete = { viewModel.deleteTodo(todo) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TodoItem(
    todo: Todo,
    onClick: () -> Unit,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit
) {
    val isOverdue = todo.dueDate?.let {
        try { LocalDate.parse(it.take(10)).isBefore(LocalDate.now()) && !todo.completed } catch (_: Exception) { false }
    } ?: false

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOverdue) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            IconButton(onClick = onToggleComplete, modifier = Modifier.size(32.dp)) {
                Text(if (todo.completed) "☑️" else "⬜")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = todo.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (todo.completed) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (todo.completed) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                )
                if (!todo.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = todo.description!!.take(80),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                // Due date & reminder row
                val tags = todo.tags ?: emptyList()
                val hasMetadata = todo.dueDate != null || todo.reminder != null || tags.isNotEmpty()
                if (hasMetadata) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        todo.dueDate?.let { date ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isOverdue) MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📅",
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = formatShortDate(date),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        if (todo.reminder != null) {
                            Text(
                                text = "🔔",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        if (tags.isNotEmpty()) {
                            Text(
                                text = tags.take(2).joinToString(" ") { "#$it" }
                                    + if (tags.size > 2) " +${tags.size - 2}" else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            // Priority indicator
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = when (todo.priority) {
                    "high" -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                    "medium" -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                },
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = when (todo.priority) {
                        "high" -> "🔴"
                        "medium" -> "🟡"
                        else -> "🟢"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    color = when (todo.priority) {
                        "high" -> MaterialTheme.colorScheme.error
                        "medium" -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.outline
                    }
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Text("🗑️")
            }
        }
    }
}

private fun formatShortDate(iso: String): String {
    return try {
        val date = LocalDate.parse(iso.take(10))
        val today = LocalDate.now()
        when {
            date == today -> "Today"
            date == today.plusDays(1) -> "Tomorrow"
            date == today.minusDays(1) -> "Yesterday"
            else -> date.format(DateTimeFormatter.ofPattern("MMM d"))
        }
    } catch (_: Exception) { iso }
}

@Composable
private fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    androidx.compose.material3.TextButton(onClick = onClick, modifier = modifier) {
        content()
    }
}
