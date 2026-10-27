package dev.cogitor.notekeeper.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.cogitor.notekeeper.data.model.VaultFolder
import dev.cogitor.notekeeper.data.model.displayPath
import dev.cogitor.notekeeper.ui.components.NkCard
import dev.cogitor.notekeeper.ui.components.NkEmptyState
import dev.cogitor.notekeeper.ui.components.NkFab
import dev.cogitor.notekeeper.ui.components.NkScreenHeader
import dev.cogitor.notekeeper.ui.viewmodel.VaultViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultListScreen(
    viewModel: VaultViewModel,
    onItemClick: (String) -> Unit,
    onAddClick: (folderId: String?) -> Unit,
    onSettingsClick: () -> Unit
) {
    val items by viewModel.items.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val folderId by viewModel.folderId.collectAsState()
    var query by remember { mutableStateOf("") }
    var newFolder by remember { mutableStateOf("") }
    var folderToDelete by remember { mutableStateOf<VaultFolder?>(null) }
    LaunchedEffect(query) {
        if (query.isNotEmpty()) delay(300)
        viewModel.load(q = query.ifBlank { null })
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            NkFab(onClick = { onAddClick(folderId) }) {
                Icon(Icons.Outlined.Add, contentDescription = "New item")
            }
        }
    ) { padding ->
        Column(Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 20.dp)) {
            NkScreenHeader(
                title = "Vault",
                subtitle = if (items.isEmpty()) null else "${items.size}"
            ) {
                IconButton(onClick = { viewModel.load(q = query.ifBlank { null }) }) {
                    Icon(Icons.Outlined.Sync, contentDescription = "Refresh")
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Settings")
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
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = folderId == null,
                    onClick = { viewModel.selectFolder(null) },
                    label = { Text("All") }
                )
                folders.forEach { folder ->
                    FilterChip(
                        selected = folderId == folder.id,
                        onClick = { viewModel.selectFolder(folder.id) },
                        label = { Text(folder.displayPath(folders)) },
                        trailingIcon = {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = "Delete folder",
                                modifier = Modifier.clickable { folderToDelete = folder }
                            )
                        }
                    )
                }
                OutlinedTextField(
                    value = newFolder,
                    onValueChange = { newFolder = it },
                    placeholder = { Text("New folder") },
                    singleLine = true,
                    modifier = Modifier.width(140.dp),
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        viewModel.createFolder(newFolder)
                        newFolder = ""
                    })
                )
                IconButton(
                    onClick = {
                        viewModel.createFolder(newFolder)
                        newFolder = ""
                    },
                    enabled = newFolder.isNotBlank()
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Create folder")
                }
            }
            if (items.isEmpty()) {
                NkEmptyState(
                    Icons.Outlined.Lock,
                    if (folderId == null) "Vault is empty" else "Folder is empty",
                    if (folderId == null) "Store passwords and keys" else "Tap + to add a secret here"
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        val extra = listOfNotNull(item.username, item.host).joinToString(" @ ")
                        val folderName = folders.find { it.id == item.folderId }?.displayPath(folders)
                        NkCard(onClick = { onItemClick(item.id) }) {
                            Row(
                                Modifier.padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (item.type == "ssh_key") Icons.Outlined.Key else Icons.Outlined.Password,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .padding(end = 4.dp)
                                )
                                Column(Modifier.padding(start = 12.dp)) {
                                    Text(item.title, style = MaterialTheme.typography.titleLarge)
                                    if (extra.isNotBlank() || folderName != null) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            listOfNotNull(folderName, extra.takeIf { it.isNotBlank() })
                                                .joinToString("  ·  "),
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
    folderToDelete?.let { folder ->
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            title = { Text("Delete folder?") },
            text = { Text("Folder ${folder.displayPath(folders)} gone. Secrets inside stay until you delete them.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFolder(folder.id)
                    folderToDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { folderToDelete = null }) { Text("Cancel") }
            }
        )
    }
}
