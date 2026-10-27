package dev.cogitor.notekeeper.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import dev.cogitor.notekeeper.data.model.SearchResult
import dev.cogitor.notekeeper.data.remote.RetrofitClient
import dev.cogitor.notekeeper.ui.components.NkCard
import dev.cogitor.notekeeper.ui.components.NkEmptyState
import dev.cogitor.notekeeper.ui.components.NkScreenHeader

@Composable
fun SearchScreen(
    onNoteClick: (String) -> Unit,
    onTodoClick: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<SearchResult?>(null) }
    val scope = rememberCoroutineScope()
    val notes = result?.notes.orEmpty()
    val todos = result?.todos.orEmpty()

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            NkScreenHeader(title = "Search")
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    if (it.length >= 2) {
                        scope.launch {
                            result = RetrofitClient.searchApiService.search(it).body()
                        }
                    } else {
                        result = null
                    }
                },
                label = { Text("Notes and todos") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )
            if (query.length >= 2 && notes.isEmpty() && todos.isEmpty()) {
                NkEmptyState(Icons.Outlined.Search, "No matches", "Try a different phrase")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(notes, key = { "n-${it.id}" }) { note ->
                        SearchRow(note.title, "Note", Icons.Outlined.Notes) { onNoteClick(note.id) }
                    }
                    items(todos, key = { "t-${it.id}" }) { todo ->
                        SearchRow(todo.title, "Todo", Icons.Outlined.TaskAlt) { onTodoClick(todo.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchRow(title: String, kind: String, icon: ImageVector, onClick: () -> Unit) {
    NkCard(onClick = onClick) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.padding(start = 14.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(kind, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
