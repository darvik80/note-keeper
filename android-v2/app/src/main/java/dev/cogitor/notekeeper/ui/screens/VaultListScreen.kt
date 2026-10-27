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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.cogitor.notekeeper.ui.components.NkCard
import dev.cogitor.notekeeper.ui.components.NkEmptyState
import dev.cogitor.notekeeper.ui.components.NkFab
import dev.cogitor.notekeeper.ui.components.NkScreenHeader
import dev.cogitor.notekeeper.ui.viewmodel.VaultViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultListScreen(
    viewModel: VaultViewModel,
    onItemClick: (String) -> Unit,
    onAddClick: () -> Unit
) {
    val items by viewModel.items.collectAsState()
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            NkFab(onClick = onAddClick) {
                Icon(Icons.Outlined.Add, contentDescription = "New item")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            NkScreenHeader(title = "Vault", subtitle = if (items.isEmpty()) null else "${items.size}") {
                IconButton(onClick = { viewModel.load(q = query.ifBlank { null }) }) {
                    Icon(Icons.Outlined.Sync, contentDescription = "Refresh")
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search vault") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )
            if (items.isEmpty()) {
                NkEmptyState(Icons.Outlined.Lock, "Vault is empty", "Store passwords and keys")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        val extra = listOfNotNull(item.username, item.host).joinToString(" @ ")
                        NkCard(onClick = { onItemClick(item.id) }) {
                            Row(
                                Modifier.padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (item.type == "ssh_key") Icons.Outlined.Key else Icons.Outlined.Password,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp).padding(end = 4.dp)
                                )
                                Column(Modifier.padding(start = 12.dp)) {
                                    Text(item.title, style = MaterialTheme.typography.titleLarge)
                                    if (extra.isNotBlank()) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            extra,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
