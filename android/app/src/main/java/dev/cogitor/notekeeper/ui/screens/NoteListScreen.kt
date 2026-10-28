package dev.cogitor.notekeeper.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.cogitor.notekeeper.data.model.Note
import dev.cogitor.notekeeper.data.model.NoteFolders
import dev.cogitor.notekeeper.data.model.SyncStatus
import dev.cogitor.notekeeper.ui.components.MarkdownText
import dev.cogitor.notekeeper.ui.components.NkCard
import dev.cogitor.notekeeper.ui.components.NkEmptyState
import dev.cogitor.notekeeper.ui.components.NkFab
import dev.cogitor.notekeeper.ui.components.NkScreenHeader
import dev.cogitor.notekeeper.ui.components.PriorityDot
import dev.cogitor.notekeeper.ui.components.SwipeHint
import dev.cogitor.notekeeper.ui.viewmodel.NoteListFilter
import dev.cogitor.notekeeper.ui.viewmodel.NoteListViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(
    viewModel: NoteListViewModel,
    onNoteClick: (String) -> Unit,
    onAddClick: (folder: String, subfolder: String?) -> Unit,
    onSettingsClick: () -> Unit
) {
    val notes by viewModel.notes.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val folderPath by viewModel.folderPath.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val importPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val path = copyUriToCache(context, it) ?: return@let
            val name = java.io.File(path).name.lowercase()
            if (!name.endsWith(".txt") && !name.endsWith(".md") && !name.endsWith(".markdown")) {
                scope.launch { snackbarHostState.showSnackbar("Only .txt and .md") }
                return@let
            }
            val (folder, sub) = NoteFolders.parse(folderPath)
            viewModel.importNote(java.io.File(path), folder, sub)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (filter == NoteListFilter.Active) {
                NkFab(onClick = {
                    val (folder, sub) = NoteFolders.parse(folderPath)
                    onAddClick(folder, sub)
                }) {
                    Icon(Icons.Outlined.Add, contentDescription = "New note")
                }
            }
        }
    ) { padding ->
        Column(Modifier
            .fillMaxSize()
            .padding(padding)) {
            NkScreenHeader(
                title = "Notes",
                subtitle = when {
                    notes.isEmpty() -> null
                    filter == NoteListFilter.Archive -> "${notes.size} archived"
                    filter == NoteListFilter.Trash -> "${notes.size} in trash"
                    else -> "${notes.size}"
                }
            ) {
                IconButton(onClick = { importPicker.launch(arrayOf("text/plain", "text/markdown")) }) {
                    Icon(Icons.Outlined.FileOpen, contentDescription = "Import markdown")
                }
                IconButton(onClick = { viewModel.syncNotes() }) {
                    if (isSyncing) CircularProgressIndicator(
                        Modifier.padding(8.dp),
                        strokeWidth = 2.dp
                    )
                    else Icon(Icons.Outlined.Sync, contentDescription = "Sync")
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Settings")
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
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filter == NoteListFilter.Active,
                    onClick = { viewModel.setFilter(NoteListFilter.Active) },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = filter == NoteListFilter.Favorite,
                    onClick = { viewModel.setFilter(NoteListFilter.Favorite) },
                    label = { Text("Starred") }
                )
                FilterChip(
                    selected = filter == NoteListFilter.Shared,
                    onClick = { viewModel.setFilter(NoteListFilter.Shared) },
                    label = { Text("Shared") }
                )
                FilterChip(
                    selected = filter == NoteListFilter.Archive,
                    onClick = { viewModel.setFilter(NoteListFilter.Archive) },
                    label = { Text("Archive") }
                )
                FilterChip(
                    selected = filter == NoteListFilter.Trash,
                    onClick = { viewModel.setFilter(NoteListFilter.Trash) },
                    label = { Text("Trash") }
                )
            }
            if (folders.isNotEmpty()) {
                Row(
                    Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = folderPath == NoteFolders.ALL,
                        onClick = { viewModel.setFolderPath(NoteFolders.ALL) },
                        label = { Text("All folders") }
                    )
                    folders.forEach { path ->
                        FilterChip(
                            selected = folderPath == path,
                            onClick = { viewModel.setFolderPath(path) },
                            label = { Text(NoteFolders.label(path)) }
                        )
                    }
                }
            }
            PullToRefreshBox(
                isRefreshing = isSyncing,
                onRefresh = { viewModel.syncNotes() },
                modifier = Modifier.fillMaxSize()
            ) {
                if (notes.isEmpty()) {
                    when (filter) {
                        NoteListFilter.Archive -> NkEmptyState(
                            Icons.Outlined.Inventory2,
                            "Archive is empty",
                            "Swipe a note far left to archive it"
                        )
                        NoteListFilter.Trash -> NkEmptyState(
                            Icons.Outlined.DeleteOutline,
                            "Trash is empty",
                            "Delete a note to find it here. Swipe to restore."
                        )
                        NoteListFilter.Favorite -> NkEmptyState(
                            Icons.Outlined.Inventory2,
                            "No starred notes",
                            "Star a note to pin it here"
                        )
                        NoteListFilter.Shared -> NkEmptyState(
                            Icons.AutoMirrored.Outlined.Notes,
                            "Nothing shared with you",
                            "Notes others share land here"
                        )
                        else -> NkEmptyState(
                            Icons.AutoMirrored.Outlined.Notes,
                            if (folderPath == NoteFolders.ALL) "No notes yet" else "Folder is empty",
                            if (folderPath == NoteFolders.ALL) "Tap + to write one"
                            else "Tap + to add a note here"
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(notes, key = { it.id }) { note ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                positionalThreshold = { it * 0.72f }
                            )
                            SwipeToDismissBox(
                                state = dismissState,
                                modifier = Modifier.clip(MaterialTheme.shapes.medium),
                                enableDismissFromStartToEnd = false,
                                onDismiss = {
                                    if (filter == NoteListFilter.Active) {
                                        viewModel.archiveNote(note)
                                        scope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = "Archived",
                                                actionLabel = "Undo",
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.restoreNote(note)
                                            }
                                        }
                                    } else {
                                        viewModel.restoreNote(note)
                                    }
                                },
                                backgroundContent = {
                                    if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                                        SwipeHint(
                                            if (filter == NoteListFilter.Active) "Archive" else "Restore",
                                            end = true,
                                            color = MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    }
                                }
                            ) {
                                NoteRow(note = note, onClick = { onNoteClick(note.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteRow(note: Note, onClick: () -> Unit) {
    NkCard(onClick = onClick) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    note.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (note.syncStatus != SyncStatus.SYNCED) {
                    Text(
                        when (note.syncStatus) {
                            SyncStatus.PENDING_CREATE -> "+"
                            SyncStatus.PENDING_UPDATE -> "•"
                            SyncStatus.PENDING_DELETE -> "–"
                            else -> ""
                        },
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
                PriorityDot(note.priority)
            }
            val body = note.content.orEmpty()
            Spacer(Modifier.height(8.dp))
            if (body.isBlank()) {
                Text(
                    "Empty note",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                MarkdownText(
                    markdown = body,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    compact = true
                )
            }
            val tags = note.tags.orEmpty()
            val folderLabel = if (NoteFolders.isDefault(note.folder, note.subfolder)) null
            else NoteFolders.path(note.folder, note.subfolder)
            if (tags.isNotEmpty() || folderLabel != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    buildList {
                        folderLabel?.let { add(it) }
                        addAll(tags.take(3).map { "#$it" })
                    }.joinToString("  "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun copyUriToCache(context: android.content.Context, uri: Uri): String? {
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    return cursor?.use {
        val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        it.moveToFirst()
        val displayName = it.getString(nameIndex)
        val file = java.io.File(context.cacheDir, displayName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        file.absolutePath
    }
}
