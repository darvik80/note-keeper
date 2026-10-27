package dev.cogitor.notekeeper.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.cogitor.notekeeper.data.local.SettingsDataStore
import dev.cogitor.notekeeper.data.notification.NotificationHelper
import dev.cogitor.notekeeper.ui.components.MarkdownText
import dev.cogitor.notekeeper.ui.components.NkEditPreviewToggle
import dev.cogitor.notekeeper.ui.components.NkFab
import dev.cogitor.notekeeper.ui.components.NkShareDialog
import dev.cogitor.notekeeper.ui.components.NkTagInput
import dev.cogitor.notekeeper.ui.components.nkTopBarColors
import dev.cogitor.notekeeper.ui.viewmodel.TodoDetailViewModel
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
fun TodoDetailScreen(
    viewModel: TodoDetailViewModel,
    todoId: String?,
    onBack: () -> Unit
) {
    val todo by viewModel.todo.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val saveLabel by viewModel.saveLabel.collectAsState()
    val attachments by viewModel.attachments.collectAsState()
    val shareResult by viewModel.shareResult.collectAsState()
    val completions by viewModel.completions.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var tagList by remember { mutableStateOf(listOf<String>()) }
    var tagDraft by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("medium") }
    var completed by remember { mutableStateOf(false) }
    var dueDate by remember { mutableStateOf<String?>(null) }
    var reminder by remember { mutableStateOf<String?>(null) }
    var scheduleRepeat by remember { mutableStateOf("none") }
    var serverNotificationChannels by remember { mutableStateOf("") }
    var isNewTodo by remember { mutableStateOf(todoId == null) }
    var isEditing by remember { mutableStateOf(todoId == null) }
    var showShareDialog by remember { mutableStateOf(false) }
    var confirmForever by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }
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

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(todo) {
        todo?.let {
            title = it.title
            description = it.description ?: ""
            tagList = it.tags.orEmpty()
            tagDraft = ""
            priority = it.priority ?: "medium"
            completed = it.completed
            dueDate = it.dueDate
            reminder = it.reminder
            scheduleRepeat = it.scheduleRepeat ?: "none"
            serverNotificationChannels = it.notificationChannels ?: ""
            isNewTodo = false
        }
    }

    fun commitTagDraft() {
        val next = tagDraft.trim().trimStart('#')
        if (next.isNotBlank() && next !in tagList) tagList = tagList + next
        tagDraft = ""
    }

    suspend fun persist(announce: Boolean = false) {
        commitTagDraft()
        val hasBody = description.isNotBlank() || tagList.isNotEmpty() || dueDate != null || reminder != null
        val resolvedTitle = title.trim().ifBlank {
            if (!hasBody) {
                if (announce) snackbarHostState.showSnackbar("Title required")
                return
            }
            "Untitled"
        }
        viewModel.persist(
            isNew = isNewTodo,
            title = resolvedTitle,
            description = description,
            tags = tagList,
            priority = priority,
            completed = completed,
            dueDate = dueDate,
            reminder = reminder,
            scheduleRepeat = scheduleRepeat,
            notificationChannels = serverNotificationChannels.ifBlank { null },
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

    LaunchedEffect(title, description, tagList, priority, completed, dueDate, reminder, scheduleRepeat, serverNotificationChannels) {
        if (isNewTodo || todo == null || title.isBlank()) return@LaunchedEffect
        delay(800)
        persist(announce = false)
    }

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
                    if (todo?.isDeleted == true || todo?.isArchived == true) {
                        TextButton(onClick = { viewModel.restoreTodo() }) { Text("Restore") }
                        if (todo?.isDeleted == true) {
                            TextButton(onClick = { confirmForever = true }) { Text("Delete forever") }
                        }
                    } else if (!isNewTodo) {
                        IconButton(onClick = { viewModel.toggleComplete() }) {
                            Icon(
                                if (todo?.completed == true) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                contentDescription = "Complete"
                            )
                        }
                        IconButton(onClick = { viewModel.toggleFavorite() }) {
                            Icon(
                                if (todo?.isFavorite == true) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorite"
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
                                text = { Text("Send to Telegram") },
                                onClick = {
                                    showMore = false
                                    viewModel.sendToChannel(telegram = true)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Send to DingTalk") },
                                onClick = {
                                    showMore = false
                                    viewModel.sendToChannel(telegram = false)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Move to Trash") },
                                onClick = {
                                    showMore = false
                                    viewModel.deleteTodo()
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
                        CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                    } else {
                        Icon(Icons.Outlined.Check, contentDescription = "Save")
                    }
                }
            }
        }
    ) { padding ->
        if (isEditing) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
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
                                { Icon(Icons.Outlined.Check, contentDescription = null) }
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
                    leadingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
                    trailingIcon = if (dueDate != null) {
                        {
                            IconButton(
                                onClick = { dueDate = null },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(Icons.Outlined.Close, contentDescription = "Clear")
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
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Notifications,
                                contentDescription = null
                            )
                        },
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
                                        Text(
                                            ch.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                },
                                onClick = {
                                    notificationChannel = ch.id
                                    channelDropdownExpanded = false
                                },
                                leadingIcon = if (notificationChannel == ch.id) {
                                    { Icon(Icons.Outlined.Check, contentDescription = null) }
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
                    val channels = serverNotificationChannels.split(",").map { it.trim() }
                        .filter { it.isNotBlank() }
                    listOf("telegram", "dingtalk").forEach { ch ->
                        FilterChip(
                            selected = ch in channels,
                            onClick = {
                                val current =
                                    serverNotificationChannels.split(",").map { it.trim() }
                                        .filter { it.isNotBlank() }.toMutableList()
                                if (ch in current) current.remove(ch) else current.add(ch)
                                serverNotificationChannels = current.joinToString(",")
                            },
                            label = { Text(ch.replaceFirstChar { it.uppercase() }) },
                            leadingIcon = if (ch in channels) {
                                { Icon(Icons.Outlined.Check, contentDescription = null) }
                            } else null
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Repeat schedule
                Text("Repeat", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "none",
                        "daily",
                        "weekdays",
                        "weekly",
                        "monthly",
                        "custom"
                    ).forEach { r ->
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

                NkTagInput(
                    tags = tagList,
                    onTagsChange = { tagList = it },
                    draft = tagDraft,
                    onDraftChange = { tagDraft = it }
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

                    if (tagList.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = tagList.joinToString(" ") { "#$it" },
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

                    if (completions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Completions", style = MaterialTheme.typography.titleMedium)
                        completions.take(12).forEach { log ->
                            Text(
                                log.completedAt?.let { formatDisplayDateTime(it) } ?: "done",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Server notification channels
                    if (serverNotificationChannels.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        val channels = serverNotificationChannels.split(",").map { it.trim() }
                            .filter { it.isNotBlank() }
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
                        MarkdownText(
                            markdown = description,
                            modifier = Modifier.fillMaxWidth(),
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
                                    Icon(Icons.Outlined.Delete, contentDescription = "Remove")
                                }
                            }
                        }
                    }
                    if (!isNewTodo) {
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

    // Due Date Picker Dialog
    if (showDueDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueDate?.let {
                try {
                    LocalDate.parse(it.take(10)).atStartOfDay(ZoneId.systemDefault()).toInstant()
                        .toEpochMilli()
                } catch (_: Exception) {
                    null
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDueDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
                            .toLocalDate()
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
                try {
                    LocalDate.parse(it.take(10)).atStartOfDay(ZoneId.systemDefault()).toInstant()
                        .toEpochMilli()
                } catch (_: Exception) {
                    null
                }
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

    if (showShareDialog) {
        NkShareDialog(
            title = "Share todo",
            result = shareResult,
            onShare = { viewModel.shareTodo(it) },
            onDismiss = { showShareDialog = false }
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
}

private fun formatDisplayDate(iso: String): String {
    return try {
        val date = LocalDate.parse(iso.take(10))
        date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    } catch (_: Exception) {
        iso
    }
}

private fun formatDisplayDateTime(iso: String): String {
    return try {
        val instant = Instant.parse(iso)
        val dt = instant.atZone(ZoneId.systemDefault()).toLocalDateTime()
        dt.format(DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm"))
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
