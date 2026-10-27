package dev.cogitor.notekeeper.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNoteClick: (String) -> Unit,
    onTodoClick: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<SearchResult?>(null) }
    val scope = rememberCoroutineScope()

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
                    }
                },
                label = { Text("Search notes and todos") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            LazyColumn(Modifier.padding(top = 12.dp)) {
                items(result?.notes.orEmpty()) { note ->
                    Text(
                        "Note · ${note.title}",
                        modifier = Modifier.fillMaxWidth().clickable { onNoteClick(note.id) }.padding(12.dp)
                    )
                }
                items(result?.todos.orEmpty()) { todo ->
                    Text(
                        "Todo · ${todo.title}",
                        modifier = Modifier.fillMaxWidth().clickable { onTodoClick(todo.id) }.padding(12.dp)
                    )
                }
            }
        }
    }
}
