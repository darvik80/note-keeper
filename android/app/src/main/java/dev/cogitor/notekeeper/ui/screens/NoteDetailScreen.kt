package dev.cogitor.notekeeper.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import dev.cogitor.notekeeper.ui.components.MarkdownText
import dev.cogitor.notekeeper.ui.components.NkFab
import dev.cogitor.notekeeper.ui.components.NkGhostField
import dev.cogitor.notekeeper.ui.components.NkMarkdownBar
import dev.cogitor.notekeeper.ui.components.insertMarkdown
import dev.cogitor.notekeeper.ui.components.nkTopBarColors
import dev.cogitor.notekeeper.ui.components.wrapMarkdown
import dev.cogitor.notekeeper.ui.viewmodel.NoteDetailViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteDetailScreen(
    viewModel: NoteDetailViewModel,
    noteId: String?,
    onBack: () -> Unit
) {
    val note by viewModel.note.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val saveLabel by viewModel.saveLabel.collectAsState()
    val attachments by viewModel.attachments.collectAsState()
    val shareResult by viewModel.shareResult.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf(TextFieldValue("")) }
    var content by remember { mutableStateOf(TextFieldValue("")) }
    var tagDraft by remember { mutableStateOf("") }
    var tagList by remember { mutableStateOf(listOf<String>()) }
    var priority by remember { mutableStateOf("medium") }
    var isNewNote by remember { mutableStateOf(noteId == null) }
    var isEditing by remember { mutableStateOf(noteId == null) }
    var showShareDialog by remember { mutableStateOf(false) }
    var shareUserId by remember { mutableStateOf("") }
    var showMore by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val path = getPathFromUri(context, it)
            path?.let { filePath -> viewModel.uploadAttachment(filePath) }
        }
    }

    LaunchedEffect(noteId) {
        if (noteId != null) viewModel.loadNote(noteId)
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(note) {
        note?.let {
            title = TextFieldValue(it.title, TextRange(it.title.length))
            val body = it.content.orEmpty()
            content = TextFieldValue(body, TextRange(body.length))
            tagList = it.tags.orEmpty()
            priority = it.priority ?: "medium"
            isNewNote = false
        }
    }

    LaunchedEffect(title.text, content.text, tagList, priority) {
        if (isNewNote || note == null || title.text.isBlank()) return@LaunchedEffect
        delay(800)
        viewModel.updateNote(title.text, content.text, tagList, priority)
    }

    fun commitTagDraft() {
        val next = tagDraft.trim().trimStart('#')
        if (next.isNotBlank() && next !in tagList) tagList = tagList + next
        tagDraft = ""
    }

    suspend fun persist(announce: Boolean = false) {
        commitTagDraft()
        val resolvedTitle = title.text.trim().ifBlank {
            if (content.text.isBlank() && tagList.isEmpty()) {
                if (announce) snackbarHostState.showSnackbar("Title required")
                return
            }
            "Untitled"
        }
        viewModel.persist(
            isNew = isNewNote,
            title = resolvedTitle,
            content = content.text,
            tags = tagList,
            priority = priority,
            announce = announce
        )
    }

    fun goBack() {
        scope.launch {
            persist(announce = false)
            onBack()
        }
    }

    BackHandler { goBack() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = isEditing,
                            onClick = { isEditing = true },
                            label = { Text("Edit") }
                        )
                        FilterChip(
                            selected = !isEditing,
                            onClick = { isEditing = false },
                            label = { Text("Preview") }
                        )
                    }
                },
                colors = nkTopBarColors(),
                navigationIcon = {
                    IconButton(onClick = { goBack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (saveLabel.isNotBlank()) {
                        Text(
                            saveLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                    if (note?.isDeleted == true || note?.isArchived == true) {
                        TextButton(onClick = { viewModel.restoreNote() }) { Text("Restore") }
                    } else if (!isNewNote) {
                        IconButton(onClick = { viewModel.toggleFavorite() }) {
                            Icon(
                                if (note?.isFavorite == true) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Favorite",
                                tint = if (note?.isFavorite == true) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onBackground
                            )
                        }
                        IconButton(onClick = { showMore = true }) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = showMore, onDismissRequest = { showMore = false }) {
                            DropdownMenuItem(
                                text = { Text("Share") },
                                onClick = {
                                    showMore = false
                                    showShareDialog = true
                                },
                                leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Attach file") },
                                onClick = {
                                    showMore = false
                                    filePicker.launch(arrayOf("*/*"))
                                },
                                leadingIcon = { Icon(Icons.Outlined.AttachFile, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Move to Trash") },
                                onClick = {
                                    showMore = false
                                    viewModel.deleteNote()
                                    onBack()
                                },
                                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (isEditing) {
                NkFab(
                    onClick = {
                        if (!isSaving) scope.launch { persist(announce = true) }
                    }
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(8.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Outlined.Check, contentDescription = "Save")
                    }
                }
            }
        }
    ) { padding ->
        if (isEditing) {
            val imeVisible = WindowInsets.isImeVisible ||
                ViewCompat.getRootWindowInsets(LocalView.current)
                    ?.isVisible(WindowInsetsCompat.Type.ime()) == true
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
            ) {
                NkGhostField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = "Title",
                    textStyle = MaterialTheme.typography.displaySmall,
                    singleLine = true,
                    modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tagList.forEach { tag ->
                        InputChip(
                            selected = false,
                            onClick = { tagList = tagList.filterNot { it == tag } },
                            label = { Text("#$tag") },
                            trailingIcon = {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = "Remove",
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        )
                    }
                    BasicTextField(
                        value = tagDraft,
                        onValueChange = { value ->
                            if (value.endsWith(",") || value.endsWith(" ") || value.endsWith("\n")) {
                                tagDraft = value
                                commitTagDraft()
                            } else {
                                tagDraft = value
                            }
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .weight(1f, fill = false),
                        decorationBox = { inner ->
                            Box {
                                if (tagDraft.isEmpty() && tagList.isEmpty()) {
                                    Text(
                                        "tags",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.4f
                                        )
                                    )
                                }
                                inner()
                            }
                        }
                    )
                }
                Row(
                    modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("low", "medium", "high").forEach { level ->
                        FilterChip(
                            selected = priority == level,
                            onClick = { priority = level },
                            label = { Text(level.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
                NkMarkdownBar(
                    onWrap = { before, after -> content = wrapMarkdown(content, before, after) },
                    onInsert = { snippet -> content = insertMarkdown(content, snippet) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                )
                NkGhostField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = "Write…",
                    textStyle = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = if (imeVisible) 8.dp else 72.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                item {
                    Text(
                        text = title.text.ifBlank { "Untitled" },
                        style = MaterialTheme.typography.displaySmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (tagList.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            tagList.joinToString("  ") { "#$it" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    if (content.text.isBlank()) {
                        Text(
                            "Nothing to preview. Tap to edit.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isEditing = true }
                                .padding(vertical = 8.dp)
                        )
                    } else {
                        MarkdownText(
                            markdown = content.text,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    if (attachments.isNotEmpty()) {
                        Spacer(Modifier.height(24.dp))
                        Text("Attachments", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        attachments.forEach { attachment ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    attachment.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.removeAttachment(attachment) }) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "Remove")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showShareDialog) {
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = { Text("Share Note") },
            text = {
                Column {
                    OutlinedTextField(
                        value = shareUserId,
                        onValueChange = { shareUserId = it },
                        label = { Text("User ID") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (shareResult.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(shareResult, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.shareNote(shareUserId) }) {
                    Text("Share")
                }
            },
            dismissButton = {
                TextButton(onClick = { showShareDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun getPathFromUri(context: android.content.Context, uri: Uri): String? {
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
