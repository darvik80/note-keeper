package dev.cogitor.notekeeper.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import dev.cogitor.notekeeper.ui.components.MarkdownText
import dev.cogitor.notekeeper.ui.components.NkEditPreviewToggle
import dev.cogitor.notekeeper.ui.components.NkFab
import dev.cogitor.notekeeper.data.model.NoteFolders
import dev.cogitor.notekeeper.ui.components.NkFolderField
import dev.cogitor.notekeeper.ui.components.NkGhostField
import dev.cogitor.notekeeper.ui.components.NkShareDialog
import dev.cogitor.notekeeper.ui.components.NkMarkdownBar
import dev.cogitor.notekeeper.ui.components.NkTagInput
import dev.cogitor.notekeeper.ui.components.insertMarkdown
import dev.cogitor.notekeeper.ui.components.nkTopBarColors
import dev.cogitor.notekeeper.ui.components.wrapMarkdown
import dev.cogitor.notekeeper.ui.viewmodel.NoteDetailViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteDetailScreen(
    viewModel: NoteDetailViewModel,
    noteId: String?,
    onBack: () -> Unit,
    initialFolder: String = "default",
    initialSubfolder: String? = null
) {
    val note by viewModel.note.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val saveLabel by viewModel.saveLabel.collectAsState()
    val attachments by viewModel.attachments.collectAsState()
    val shareResult by viewModel.shareResult.collectAsState()
    val knownFolders by viewModel.knownFolders.collectAsState()
    val history by viewModel.history.collectAsState()
    val templates by viewModel.templates.collectAsState()
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
    var showMore by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var showTemplates by remember { mutableStateOf(false) }
    var confirmForever by remember { mutableStateOf(false) }
    var showSaveTemplate by remember { mutableStateOf(false) }
    var templateName by remember { mutableStateOf("") }
    var reminder by remember { mutableStateOf<String?>(null) }
    var showReminderDatePicker by remember { mutableStateOf(false) }
    var showReminderTimePicker by remember { mutableStateOf(false) }
    var reminderDate by remember { mutableStateOf<LocalDate?>(null) }
    var folderPath by remember {
        mutableStateOf(NoteFolders.path(initialFolder, initialSubfolder).let {
            if (it == "default") "" else it
        })
    }

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
            folderPath = NoteFolders.path(it.folder, it.subfolder).let { path ->
                if (path == "default") "" else path
            }
            reminder = it.reminder
            isNewNote = false
        }
    }

    LaunchedEffect(title.text, content.text, tagList, priority, folderPath, reminder) {
        if (isNewNote || note == null || title.text.isBlank()) return@LaunchedEffect
        delay(800)
        val (folder, sub) = NoteFolders.parse(folderPath.ifBlank { NoteFolders.ALL })
        viewModel.updateNote(title.text, content.text, tagList, priority, folder, sub, reminder)
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
        val (folder, sub) = NoteFolders.parse(folderPath.ifBlank { NoteFolders.ALL })
        viewModel.persist(
            isNew = isNewNote,
            title = resolvedTitle,
            content = content.text,
            tags = tagList,
            priority = priority,
            folder = folder,
            subfolder = sub,
            reminder = reminder,
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
                    NkEditPreviewToggle(isEditing = isEditing, onChange = { isEditing = it })
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
                        if (note?.isDeleted == true) {
                            TextButton(onClick = { confirmForever = true }) { Text("Delete forever") }
                        }
                    } else if (isNewNote && templates.isNotEmpty()) {
                        TextButton(onClick = { showTemplates = true }) { Text("Template") }
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
                                text = { Text("Save as template") },
                                onClick = {
                                    showMore = false
                                    templateName = title.text.trim().ifBlank { "Untitled" }
                                    showSaveTemplate = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("History") },
                                onClick = {
                                    showMore = false
                                    viewModel.loadHistory()
                                    showHistory = true
                                },
                                leadingIcon = { Icon(Icons.Outlined.History, contentDescription = null) }
                            )
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
                NkTagInput(
                    tags = tagList,
                    onTagsChange = { tagList = it },
                    draft = tagDraft,
                    onDraftChange = { tagDraft = it }
                )
                NkFolderField(
                    path = folderPath,
                    onPathChange = { folderPath = it },
                    knownPaths = knownFolders,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                AssistChip(
                    onClick = { showReminderDatePicker = true },
                    label = { Text(reminder?.let { formatNoteDateTime(it) } ?: "Reminder") },
                    leadingIcon = { Icon(Icons.Outlined.Notifications, contentDescription = null) },
                    trailingIcon = if (reminder != null) {
                        {
                            IconButton(
                                onClick = { reminder = null },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(Icons.Outlined.Close, contentDescription = "Clear")
                            }
                        }
                    } else null
                )
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
                    val folderLabel = folderPath.trim().takeIf { it.isNotBlank() && it != "default" }
                    if (tagList.isNotEmpty() || folderLabel != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            buildList {
                                folderLabel?.let { add(it) }
                                addAll(tagList.map { "#$it" })
                            }.joinToString("  "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    reminder?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Reminder  ${formatNoteDateTime(it)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            scope.launch {
                                                val dest = java.io.File(context.cacheDir, attachment.name)
                                                val file = viewModel.downloadAttachment(attachment, dest)
                                                if (file != null) {
                                                    snackbarHostState.showSnackbar("Saved ${file.name}")
                                                }
                                            }
                                        }
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
        NkShareDialog(
            title = "Share note",
            result = shareResult,
            onShare = { viewModel.shareNote(it) },
            onDismiss = { showShareDialog = false }
        )
    }
    if (showHistory) {
        AlertDialog(
            onDismissRequest = { showHistory = false },
            title = { Text("History") },
            text = {
                Column {
                    if (history.isEmpty()) Text("No versions yet")
                    history.forEach { entry ->
                        TextButton(onClick = {
                            viewModel.restoreVersion(entry.id)
                            showHistory = false
                        }) {
                            Text("${entry.action ?: "edit"}  ${entry.timestamp.orEmpty()}")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistory = false }) { Text("Close") }
            }
        )
    }
    if (showTemplates) {
        AlertDialog(
            onDismissRequest = { showTemplates = false },
            title = { Text("Templates") },
            text = {
                Column {
                    templates.forEach { template ->
                        TextButton(onClick = {
                            title = TextFieldValue(template.name)
                            content = TextFieldValue(template.content.orEmpty())
                            tagList = template.tags.orEmpty()
                            showTemplates = false
                            isEditing = true
                        }) { Text(template.name) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTemplates = false }) { Text("Close") }
            }
        )
    }
    if (confirmForever) {
        AlertDialog(
            onDismissRequest = { confirmForever = false },
            title = { Text("Delete forever?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmForever = false
                    viewModel.deleteForever()
                    onBack()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmForever = false }) { Text("Cancel") }
            }
        )
    }
    if (showSaveTemplate) {
        AlertDialog(
            onDismissRequest = { showSaveTemplate = false },
            title = { Text("Save as template") },
            text = {
                OutlinedTextField(
                    value = templateName,
                    onValueChange = { templateName = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.saveAsTemplate(templateName.trim(), content.text, tagList)
                        showSaveTemplate = false
                    },
                    enabled = templateName.isNotBlank()
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveTemplate = false }) { Text("Cancel") }
            }
        )
    }
    if (showReminderDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = reminder?.let {
                runCatching {
                    Instant.parse(it).toEpochMilli()
                }.getOrNull()
            }
        )
        DatePickerDialog(
            onDismissRequest = { showReminderDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        reminderDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        showReminderDatePicker = false
                        showReminderTimePicker = true
                    }
                }) { Text("Next") }
            },
            dismissButton = {
                TextButton(onClick = { showReminderDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    if (showReminderTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = 9, initialMinute = 0)
        AlertDialog(
            onDismissRequest = { showReminderTimePicker = false },
            title = { Text("Reminder time") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    reminderDate?.let { date ->
                        val time = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        reminder = LocalDateTime.of(date, time)
                            .atZone(ZoneId.systemDefault()).toInstant().toString()
                    }
                    showReminderTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showReminderTimePicker = false }) { Text("Cancel") }
            }
        )
    }
}

private fun formatNoteDateTime(iso: String): String {
    return try {
        Instant.parse(iso).atZone(ZoneId.systemDefault()).toLocalDateTime()
            .format(DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm"))
    } catch (_: Exception) {
        iso
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
