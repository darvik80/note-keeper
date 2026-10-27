package dev.cogitor.notekeeper.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.cogitor.notekeeper.data.model.Todo
import dev.cogitor.notekeeper.ui.components.NkCard
import dev.cogitor.notekeeper.ui.components.NkEmptyState
import dev.cogitor.notekeeper.ui.components.NkFab
import dev.cogitor.notekeeper.ui.components.NkScreenHeader
import dev.cogitor.notekeeper.ui.components.PriorityDot
import dev.cogitor.notekeeper.ui.components.SwipeHint
import dev.cogitor.notekeeper.ui.viewmodel.TodoListFilter
import dev.cogitor.notekeeper.ui.viewmodel.TodoListViewModel
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
    val filter by viewModel.filter.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            NkFab(onClick = onAddClick) {
                Icon(Icons.Outlined.Add, contentDescription = "New todo")
            }
        }
    ) { padding ->
        Column(Modifier
            .fillMaxSize()
            .padding(padding)) {
            NkScreenHeader(
                title = "Todos",
                subtitle = when {
                    todos.isEmpty() -> null
                    filter == TodoListFilter.Done -> "${todos.size} done"
                    else -> "${todos.size} open"
                }
            ) {
                IconButton(onClick = { viewModel.syncTodos() }) {
                    if (isSyncing) CircularProgressIndicator(
                        Modifier.padding(8.dp),
                        strokeWidth = 2.dp
                    )
                    else Icon(Icons.Outlined.Sync, contentDescription = "Sync")
                }
            }
            if (syncStatus.isNotEmpty()) {
                Text(
                    syncStatus,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp)
                )
            }
            Row(
                Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filter == TodoListFilter.Active,
                    onClick = { viewModel.setFilter(TodoListFilter.Active) },
                    label = { Text("Active") }
                )
                FilterChip(
                    selected = filter == TodoListFilter.Done,
                    onClick = { viewModel.setFilter(TodoListFilter.Done) },
                    label = { Text("Done") }
                )
            }
            PullToRefreshBox(
                isRefreshing = isSyncing,
                onRefresh = { viewModel.syncTodos() },
                modifier = Modifier.fillMaxSize()
            ) {
                if (todos.isEmpty()) {
                    if (filter == TodoListFilter.Done) {
                        NkEmptyState(
                            Icons.Outlined.TaskAlt,
                            "No completed tasks",
                            "Mark a todo done to see it here"
                        )
                    } else {
                        NkEmptyState(Icons.Outlined.TaskAlt, "No open tasks", "Tap + to add a task")
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(todos, key = { it.id }) { todo ->
                            val dismissState = rememberSwipeToDismissBoxState()
                            SwipeToDismissBox(
                                state = dismissState,
                                modifier = Modifier.clip(MaterialTheme.shapes.medium),
                                onDismiss = { value ->
                                    when (value) {
                                        SwipeToDismissBoxValue.EndToStart -> viewModel.archiveTodo(todo)
                                        SwipeToDismissBoxValue.StartToEnd -> viewModel.toggleComplete(todo)
                                        else -> Unit
                                    }
                                },
                                backgroundContent = {
                                    if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                                        val toStart =
                                            dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
                                        SwipeHint(
                                            if (toStart) "Archive" else if (todo.completed) "Open" else "Done",
                                            end = toStart,
                                            color = if (toStart) MaterialTheme.colorScheme.secondaryContainer
                                            else MaterialTheme.colorScheme.tertiaryContainer
                                        )
                                    }
                                }
                            ) {
                                TodoRow(
                                    todo = todo,
                                    onClick = { onTodoClick(todo.id) },
                                    onToggle = { viewModel.toggleComplete(todo) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TodoRow(todo: Todo, onClick: () -> Unit, onToggle: () -> Unit) {
    val overdue = todo.dueDate?.let {
        try {
            LocalDate.parse(it.take(10)).isBefore(LocalDate.now()) && !todo.completed
        } catch (_: Exception) {
            false
        }
    } ?: false
    NkCard(
        onClick = onClick,
        color = if (overdue) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
        else MaterialTheme.colorScheme.surface
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggle) {
                Icon(
                    if (todo.completed) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = "Complete",
                    tint = if (todo.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
            Column(Modifier
                .weight(1f)
                .padding(end = 8.dp)) {
                Text(
                    todo.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (todo.completed) TextDecoration.LineThrough else TextDecoration.None
                )
                val meta = buildList {
                    todo.dueDate?.let { add(formatShortDate(it)) }
                    val tags = todo.tags.orEmpty()
                    if (tags.isNotEmpty()) add(tags.take(2).joinToString(" ") { "#$it" })
                }
                if (meta.isNotEmpty()) {
                    Text(
                        meta.joinToString("  ·  "),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            PriorityDot(todo.priority)
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
    } catch (_: Exception) {
        iso
    }
}
