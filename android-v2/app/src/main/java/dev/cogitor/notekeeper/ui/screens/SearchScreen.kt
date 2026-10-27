package dev.cogitor.notekeeper.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import dev.cogitor.notekeeper.data.model.SearchResult
import dev.cogitor.notekeeper.data.remote.RetrofitClient
import dev.cogitor.notekeeper.ui.components.NkEmptyState

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(topBar = { TopAppBar(title = { Text("Search") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
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
                singleLine = true
            )
            if (query.length >= 2 && notes.isEmpty() && todos.isEmpty()) {
                NkEmptyState(Icons.Outlined.Search, "No matches", "Try a different phrase")
            } else {
                LazyColumn(Modifier.padding(top = 8.dp)) {
                    items(notes, key = { "n-${it.id}" }) { note ->
                        ListItem(
                            headlineContent = { Text(note.title) },
                            supportingContent = { Text("Note") },
                            leadingContent = { Icon(Icons.Outlined.Notes, contentDescription = null) },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.clickable { onNoteClick(note.id) }
                        )
                    }
                    items(todos, key = { "t-${it.id}" }) { todo ->
                        ListItem(
                            headlineContent = { Text(todo.title) },
                            supportingContent = { Text("Todo") },
                            leadingContent = { Icon(Icons.Outlined.TaskAlt, contentDescription = null) },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.clickable { onTodoClick(todo.id) }
                        )
                    }
                }
            }
        }
    }
}
