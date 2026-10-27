package dev.cogitor.notekeeper.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import dev.cogitor.notekeeper.ui.components.MarkdownText
import dev.cogitor.notekeeper.ui.viewmodel.NoteDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    viewModel: NoteDetailViewModel,
    noteId: String?,
    onBack: () -> Unit
) {
    val note by viewModel.note.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val attachments by viewModel.attachments.collectAsState()
    val shareResult by viewModel.shareResult.collectAsState()

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("medium") }
    var isNewNote by remember { mutableStateOf(noteId == null) }
    var isEditing by remember { mutableStateOf(true) }
    var showShareDialog by remember { mutableStateOf(false) }
    var shareUserId by remember { mutableStateOf("") }

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
        if (noteId != null) {
            viewModel.loadNote(noteId)
        }
    }

    LaunchedEffect(note) {
        note?.let {
            title = it.title
            content = it.content ?: ""
            tags = (it.tags ?: emptyList()).joinToString(", ")
            priority = it.priority ?: "medium"
            isNewNote = false
        }
    }

    LaunchedEffect(title, content, tags, priority) {
        if (isNewNote || note == null || title.isBlank()) return@LaunchedEffect
        delay(800)
        val tagList = tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
        viewModel.updateNote(title, content, tagList, priority)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNewNote) "New Note" else "Edit Note") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { isEditing = !isEditing }) {
                        Icon(
                            if (isEditing) Icons.Outlined.Visibility else Icons.Outlined.Edit,
                            contentDescription = if (isEditing) "Preview" else "Edit"
                        )
                    }
                    if (!isNewNote) {
                        IconButton(onClick = { viewModel.toggleFavorite() }) {
                            Icon(
                                if (note?.isFavorite == true) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Favorite"
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (title.isNotBlank() && !isSaving) {
                        val tagList = tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        if (isNewNote) {
                            viewModel.createNote(title, content, tagList, priority)
                        } else {
                            viewModel.updateNote(title, content, tagList, priority)
                        }
                    }
                }
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                } else {
                    Icon(Icons.Outlined.Check, contentDescription = "Save")
                }
            }
        }
    ) { padding ->
        if (isEditing) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tags (comma separated)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(Modifier.fillMaxWidth()) {
                    TextButton(onClick = { content += "**bold**" }) { Text("B") }
                    TextButton(onClick = { content += "\n- item" }) { Text("List") }
                    TextButton(onClick = { content += "\n- [ ] task" }) { Text("Task") }
                    TextButton(onClick = { content += "`code`" }) { Text("Code") }
                }
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content (Markdown)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    minLines = 10
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                item {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (tags.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                .joinToString(" ") { "#$it" },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    MarkdownText(
                        markdown = content,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (attachments.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Attachments",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        attachments.forEach { attachment ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = attachment.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.removeAttachment(attachment) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "Remove")
                                }
                            }
                        }
                    }
                    if (!isNewNote) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { filePicker.launch(arrayOf("*/*")) }) {
                            Icon(Icons.Outlined.AttachFile, contentDescription = null)
                            Text("Add attachment")
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
