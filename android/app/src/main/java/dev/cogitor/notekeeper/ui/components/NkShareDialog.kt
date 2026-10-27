package dev.cogitor.notekeeper.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.cogitor.notekeeper.data.model.User
import dev.cogitor.notekeeper.data.remote.RetrofitClient
import kotlinx.coroutines.delay

@Composable
fun NkShareDialog(
    title: String,
    result: String,
    onShare: (userId: String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var users by remember { mutableStateOf(emptyList<User>()) }
    LaunchedEffect(query) {
        if (query.length < 2) {
            users = emptyList()
            return@LaunchedEffect
        }
        delay(300)
        users = runCatching {
            RetrofitClient.accountApiService.searchUsers(query).body().orEmpty()
        }.getOrElse { emptyList() }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search name or email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (result.isNotBlank()) {
                    Text(
                        result,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                LazyColumn {
                    items(users, key = { it.id }) { user ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onShare(user.id) }
                                .padding(vertical = 10.dp)
                        ) {
                            Text(user.name ?: user.email, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                user.email,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
