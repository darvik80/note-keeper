package dev.cogitor.notekeeper.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.cogitor.notekeeper.data.model.SavedQuery
import dev.cogitor.notekeeper.data.model.SavedQueryFilters
import dev.cogitor.notekeeper.data.model.SavedQueryInput
import dev.cogitor.notekeeper.data.model.SearchResult
import dev.cogitor.notekeeper.data.remote.RetrofitClient
import dev.cogitor.notekeeper.ui.components.NkCard
import dev.cogitor.notekeeper.ui.components.NkEmptyState
import dev.cogitor.notekeeper.ui.components.NkScreenHeader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    onNoteClick: (String) -> Unit,
    onTodoClick: (String) -> Unit,
    onSettingsClick: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var searchType by remember { mutableStateOf("all") }
    var result by remember { mutableStateOf<SearchResult?>(null) }
    var saved by remember { mutableStateOf(listOf<SavedQuery>()) }
    var showSave by remember { mutableStateOf(false) }
    var saveName by remember { mutableStateOf("") }
    val notes = result?.notes.orEmpty()
    val todos = result?.todos.orEmpty()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        saved = runCatching {
            RetrofitClient.searchApiService.getSavedQueries().body().orEmpty()
        }.getOrElse { emptyList() }
    }

    LaunchedEffect(query, searchType) {
        if (query.length < 2) {
            result = null
            return@LaunchedEffect
        }
        delay(300)
        val typeParam = searchType.takeIf { it != "all" }
        result = runCatching {
            RetrofitClient.searchApiService.search(query, typeParam).body()
        }.getOrNull()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0)
    ) { padding ->
        Column(Modifier
            .fillMaxSize()
            .padding(padding)
            .imePadding()
            .padding(horizontal = 20.dp)) {
            NkScreenHeader(title = "Search") {
                IconButton(
                    onClick = { showSave = true },
                    enabled = query.isNotBlank()
                ) {
                    Icon(Icons.Outlined.BookmarkAdd, contentDescription = "Save query")
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Notes and todos") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )
            Row(
                Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("all", "notes", "todos").forEach { type ->
                    FilterChip(
                        selected = searchType == type,
                        onClick = { searchType = type },
                        label = { Text(type.replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
            if (saved.isNotEmpty()) {
                Row(
                    Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    saved.forEach { item ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                query = item.query
                                searchType = item.filters?.type ?: "all"
                            },
                            label = { Text(item.name) },
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            runCatching {
                                                RetrofitClient.searchApiService.deleteQuery(item.id)
                                            }
                                            saved = saved.filter { it.id != item.id }
                                        }
                                    },
                                    modifier = Modifier.height(20.dp)
                                ) {
                                    Icon(Icons.Outlined.Close, contentDescription = "Delete saved query")
                                }
                            }
                        )
                    }
                }
            }
            if (query.length >= 2 && notes.isEmpty() && todos.isEmpty()) {
                NkEmptyState(Icons.Outlined.Search, "No matches", "Try a different phrase")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(notes, key = { "n-${it.id}" }) { note ->
                        SearchRow(note.title, "Note", Icons.AutoMirrored.Outlined.Notes) { onNoteClick(note.id) }
                    }
                    items(todos, key = { "t-${it.id}" }) { todo ->
                        SearchRow(
                            todo.title,
                            "Todo",
                            Icons.Outlined.TaskAlt
                        ) { onTodoClick(todo.id) }
                    }
                }
            }
        }
    }

    if (showSave) {
        AlertDialog(
            onDismissRequest = { showSave = false },
            title = { Text("Save query") },
            text = {
                OutlinedTextField(
                    value = saveName,
                    onValueChange = { saveName = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = saveName.trim()
                        if (name.isBlank() || query.isBlank()) return@TextButton
                        scope.launch {
                            val created = runCatching {
                                RetrofitClient.searchApiService.saveQuery(
                                    SavedQueryInput(
                                        name = name,
                                        query = query,
                                        filters = SavedQueryFilters(type = searchType)
                                    )
                                ).body()
                            }.getOrNull()
                            if (created != null) saved = listOf(created) + saved
                            saveName = ""
                            showSave = false
                        }
                    },
                    enabled = saveName.isNotBlank()
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showSave = false }) { Text("Cancel") }
            }
        )
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
                Text(
                    kind,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
