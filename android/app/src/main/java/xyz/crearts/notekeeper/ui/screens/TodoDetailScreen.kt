package xyz.crearts.notekeeper.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import xyz.crearts.notekeeper.data.local.SettingsDataStore
import xyz.crearts.notekeeper.data.notification.NotificationHelper
import xyz.crearts.notekeeper.ui.viewmodel.TodoDetailViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TodoDetailScreen(
    viewModel: TodoDetailViewModel,
    todoId: String?,
    onBack: () -> Unit
) {
    val todo by viewModel.todo.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val attachments by viewModel.attachments.collectAsState()
    val shareResult by viewModel.shareResult.collectAsState()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("medium") }
    var completed by remember { mutableStateOf(false) }
    var dueDate by remember { mutableStateOf<String?>(null) }
    var reminder by remember { mutableStateOf<String?>(null) }
    var scheduleRepeat by remember { mutableStateOf("none") }
    var serverNotificationChannels by remember { mutableStateOf("") }
    var isNewTodo by remember { mutableStateOf(todoId == null) }
    var isEditing by remember { mutableStateOf(todoId == null) }
    var showShareDialog by remember { mutableStateOf(false) }
    var shareUserId by remember { mutableStateOf("") }
    var showDueDatePicker by remember { mutableStateOf(false) }
    var showReminderDatePicker by remember { mutableStateOf(false) }
    var showReminderTimePicker by remember { mutableStateOf(false) }
    var reminderDate by remember { mutableStateOf<LocalDate?>(null) }
    var notificationChannel by remember { mutableStateOf(NotificationHelper.CHANNEL_REMINDERS) }
    var channelDropdownExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val settingsDataStore = remember { SettingsDataStore(context) }
    val defaultChannel by settingsDataStore.defaultNotificationChannel.collectAsState(initial = SettingsDataStore.DEFAULT_CHANNEL)

    LaunchedEffect(defaultChannel) {
        if (notificationChannel == NotificationHelper.CHANNEL_REMINDERS) {
            notificationChannel = defaultChannel
        }
    }
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val path = getPathFromUri(context, it)
            path?.let { filePath -> viewModel.uploadAttachment(filePath) }
        }
    }

    LaunchedEffect(todoId) {
        if (todoId != null) {
            viewModel.loadTodo(todoId)
        }
    }

    LaunchedEffect(todo) {
        todo?.let {
            title = it.title
            description = it.description ?: ""
            tags = (it.tags ?: emptyList()).joinToString(", ")
            priority = it.priority ?: "medium"
            completed = it.completed
            dueDate = it.dueDate
            reminder = it.reminder
            scheduleRepeat = it.scheduleRepeat ?: "none"
            serverNotificationChannels = it.notificationChannels ?: ""
            isNewTodo = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNewTodo) "New Todo" else "Todo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("⬅️")
                    }
                },
                actions = {
                    if (!isNewTodo) {
                        if (!isEditing) {
                            IconButton(onClick = { isEditing = true }) {
                                Text("✏️")
                            }
                        }
                        IconButton(onClick = { viewModel.toggleComplete() }) {
                            Text(if (todo?.completed == true) "☑️" else "⬜")
                        }
                        IconButton(onClick = { viewModel.toggleFavorite() }) {
                            Text(if (todo?.isFavorite == true) "❤️" else "🤍")
                        }
                        IconButton(onClick = { showShareDialog = true }) {
                            Text("📤")
                        }
                        IconButton(onClick = {
                            viewModel.deleteTodo()
                            onBack()
                        }) {
                            Text("🗑️")
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
                        if (isNewTodo) {
                            viewModel.createTodo(title, description, tagList, priority, dueDate, reminder, scheduleRepeat, serverNotificationChannels.ifBlank { null })
                            onBack()
                        } else {
                            viewModel.updateTodo(title, description, tagList, priority, completed, dueDate, reminder, scheduleRepeat, serverNotificationChannels.ifBlank { null })
                            onBack()
                        }
                    }
                }
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                } else {
                    Text("✅")
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
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Priority chips
                Text("Priority", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("low", "medium", "high").forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p.replaceFirstChar { it.uppercase() }) },
                            leadingIcon = if (priority == p) {
                                { Text("✅") }
                            } else null
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Due Date
                Text("Due Date", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                AssistChip(
                    onClick = { showDueDatePicker = true },
                    label = { Text(dueDate?.let { formatDisplayDate(it) } ?: "Set due date") },
                    leadingIcon = { Text("📅") },
                    trailingIcon = if (dueDate != null) {
                        {
                            IconButton(onClick = { dueDate = null }, modifier = Modifier.size(18.dp)) {
                                Text("❌")
                            }
                        }
                    } else null
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Reminder
                Text("Reminder", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                AssistChip(
                    onClick = { showReminderDatePicker = true },
                    label = { Text(reminder?.let { formatDisplayDateTime(it) } ?: "Set reminder") },
                    leadingIcon = { Text("🔔") },
                    trailingIcon = if (reminder != null) {
                        {
                            IconButton(onClick = { reminder = null }, modifier = Modifier.size(18.dp)) {
                                Text("❌")
                            }
                        }
                    } else null
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Notification Channel
                Text("Notification Channel", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                ExposedDropdownMenuBox(
                    expanded = channelDropdownExpanded,
                    onExpandedChange = { channelDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = NotificationHelper.getChannelName(notificationChannel),
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        leadingIcon = { Text("🔔") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = channelDropdownExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = channelDropdownExpanded,
                        onDismissRequest = { channelDropdownExpanded = false }
                    ) {
                        NotificationHelper.channels.forEach { ch ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(ch.name, style = MaterialTheme.typography.bodyMedium)
                                        Text(ch.description, style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline)
                                    }
                                },
                                onClick = {
                                    notificationChannel = ch.id
                                    channelDropdownExpanded = false
                                },
                                leadingIcon = if (notificationChannel == ch.id) {
                                    { Text("✅") }
                                } else null
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Server Notification Channels (Telegram/DingTalk)
                Text("Server Notifications", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val channels = serverNotificationChannels.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    listOf("telegram", "dingtalk").forEach { ch ->
                        FilterChip(
                            selected = ch in channels,
                            onClick = {
                                val current = serverNotificationChannels.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
                                if (ch in current) current.remove(ch) else current.add(ch)
                                serverNotificationChannels = current.joinToString(",")
                            },
                            label = { Text(ch.replaceFirstChar { it.uppercase() }) },
                            leadingIcon = if (ch in channels) {
                                { Text("✅") }
                            } else null
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Repeat schedule
                Text("Repeat", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("none", "daily", "weekly", "monthly").forEach { r ->
                        FilterChip(
                            selected = scheduleRepeat == r,
                            onClick = { scheduleRepeat = r },
                            label = { Text(r.replaceFirstChar { it.uppercase() }) },
                            leadingIcon = if (scheduleRepeat == r) {
                                { Text("🔁") }
                            } else null
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tags (comma separated)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 6
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.weight(1f),
                            color = if (completed) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                        )
                        if (completed) {
                            Text(
                                text = "✅ Done",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Priority badge
                    Spacer(modifier = Modifier.height(8.dp))
                    val priorityEmoji = when (priority) {
                        "high" -> "🔴"
                        "medium" -> "🟡"
                        else -> "🟢"
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (priority) {
                            "high" -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                            "medium" -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        }
                    ) {
                        Text(
                            text = "$priorityEmoji ${priority.replaceFirstChar { it.uppercase() }} priority",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = when (priority) {
                                "high" -> MaterialTheme.colorScheme.error
                                "medium" -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.outline
                            }
                        )
                    }

                    if (tags.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                .joinToString(" ") { "#$it" },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Due date
                    dueDate?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "📅 Due: ${formatDisplayDate(it)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // Reminder
                    reminder?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "⏰ Reminder: ${formatDisplayDateTime(it)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // Repeat schedule
                    if (scheduleRepeat != "none") {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "🔁 Repeat: ${scheduleRepeat.replaceFirstChar { it.uppercase() }}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // Server notification channels
                    if (serverNotificationChannels.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        val channels = serverNotificationChannels.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        Text(
                            text = "Notify via: ${channels.joinToString(", ") { it.replaceFirstChar { c -> c.uppercase() } }}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // Description
                    if (description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    // Timestamps
                    todo?.let { t ->
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        t.createdAt?.let { ca ->
                            Text(
                                text = "Created: ${formatDisplayDateTime(ca)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        t.updatedAt?.let { ua ->
                            Text(
                                text = "Updated: ${formatDisplayDateTime(ua)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

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
                                    Text("🗑️")
                                }
                            }
                        }
                    }
                    if (!isNewTodo) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { filePicker.launch(arrayOf("*/*")) }) {
                            Text("📎 Add attachment")
                        }
                    }
                }
            }
        }
    }

    // Due Date Picker Dialog
    if (showDueDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueDate?.let {
                try { LocalDate.parse(it.take(10)).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() } catch (_: Exception) { null }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDueDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        dueDate = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    }
                    showDueDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDueDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Reminder Date Picker
    if (showReminderDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = reminder?.let {
                try { LocalDate.parse(it.take(10)).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() } catch (_: Exception) { null }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showReminderDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        reminderDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        showReminderDatePicker = false
                        showReminderTimePicker = true
                    }
                }) { Text("Next: Set Time") }
            },
            dismissButton = {
                TextButton(onClick = { showReminderDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Reminder Time Picker
    if (showReminderTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = 9, initialMinute = 0)
        AlertDialog(
            onDismissRequest = { showReminderTimePicker = false },
            title = { Text("Set reminder time") },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(onClick = {
                    reminderDate?.let { date ->
                        val time = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        val dateTime = LocalDateTime.of(date, time)
                        reminder = dateTime.atZone(ZoneId.systemDefault()).toInstant().toString()
                    }
                    showReminderTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showReminderTimePicker = false }) { Text("Cancel") }
            }
        )
    }

    // Share Dialog
    if (showShareDialog) {
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = { Text("Share Todo") },
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
                TextButton(onClick = { viewModel.shareTodo(shareUserId) }) {
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

private fun formatDisplayDate(iso: String): String {
    return try {
        val date = LocalDate.parse(iso.take(10))
        date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    } catch (_: Exception) { iso }
}

private fun formatDisplayDateTime(iso: String): String {
    return try {
        val instant = Instant.parse(iso)
        val dt = instant.atZone(ZoneId.systemDefault()).toLocalDateTime()
        dt.format(DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm"))
    } catch (_: Exception) { iso }
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
