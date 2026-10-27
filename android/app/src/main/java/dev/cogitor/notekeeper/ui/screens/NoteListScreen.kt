package dev.cogitor.notekeeper.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.cogitor.notekeeper.data.model.Note
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(
    viewModel: NoteListViewModel,
    onNoteClick: (String) -> Unit,
    onAddClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onTodosClick: () -> Unit,
    onLogout: () -> Unit
) {
    val notes by viewModel.notes.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val filter by viewModel.filter.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            NkFab(onClick = onAddClick) {
                Icon(Icons.Outlined.Add, contentDescription = "New note")
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
                IconButton(onClick = onLogout) {
                    Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = "Log out")
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
                            "Swipe a note left to archive it"
                        )
                        NoteListFilter.Trash -> NkEmptyState(
                            Icons.Outlined.DeleteOutline,
                            "Trash is empty",
                            "Delete a note to find it here. Swipe to restore."
                        )
                        else -> NkEmptyState(Icons.AutoMirrored.Outlined.Notes, "No notes yet", "Tap + to write one")
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(notes, key = { it.id }) { note ->
                            val dismissState = rememberSwipeToDismissBoxState()
                            SwipeToDismissBox(
                                state = dismissState,
                                modifier = Modifier.clip(MaterialTheme.shapes.medium),
                                enableDismissFromStartToEnd = false,
                                onDismiss = {
                                    if (filter == NoteListFilter.Active) viewModel.archiveNote(note)
                                    else viewModel.restoreNote(note)
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
            if (tags.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    tags.take(3).joinToString("  ") { "#$it" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
