package dev.cogitor.notekeeper.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.cogitor.notekeeper.data.model.Attachment
import dev.cogitor.notekeeper.data.model.SyncStatus
import dev.cogitor.notekeeper.data.model.Todo
import dev.cogitor.notekeeper.data.repository.TodoRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TodoDetailViewModel(
    private val repository: TodoRepository
) : ViewModel() {

    private val _todo = MutableStateFlow<Todo?>(null)
    val todo: StateFlow<Todo?> = _todo.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _attachments = MutableStateFlow<List<Attachment>>(emptyList())
    val attachments: StateFlow<List<Attachment>> = _attachments.asStateFlow()

    private val _shareResult = MutableStateFlow<String>("")
    val shareResult: StateFlow<String> = _shareResult.asStateFlow()

    private val _saveLabel = MutableStateFlow("")
    val saveLabel: StateFlow<String> = _saveLabel.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun loadTodo(id: String) {
        viewModelScope.launch {
            val todo = repository.getTodoById(id)
            _todo.value = todo
            _attachments.value = todo?.attachments ?: emptyList()
        }
    }

    fun createTodo(
        title: String,
        description: String = "",
        tags: List<String> = emptyList(),
        priority: String = "medium",
        dueDate: String? = null,
        reminder: String? = null,
        scheduleRepeat: String = "none",
        notificationChannels: String? = null
    ) {
        viewModelScope.launch {
            persist(true, title, description, tags, priority, false, dueDate, reminder, scheduleRepeat, notificationChannels, announce = true)
        }
    }

    fun updateTodo(
        title: String,
        description: String,
        tags: List<String> = emptyList(),
        priority: String = "medium",
        completed: Boolean = false,
        dueDate: String? = null,
        reminder: String? = null,
        scheduleRepeat: String = "none",
        notificationChannels: String? = null,
        announce: Boolean = false
    ) {
        viewModelScope.launch {
            persist(false, title, description, tags, priority, completed, dueDate, reminder, scheduleRepeat, notificationChannels, announce)
        }
    }

    suspend fun persist(
        isNew: Boolean,
        title: String,
        description: String,
        tags: List<String> = emptyList(),
        priority: String = "medium",
        completed: Boolean = false,
        dueDate: String? = null,
        reminder: String? = null,
        scheduleRepeat: String = "none",
        notificationChannels: String? = null,
        announce: Boolean = false
    ) {
        if (title.isBlank()) return
        _isSaving.value = true
        _saveLabel.value = "Saving…"
        if (isNew && _todo.value == null) {
            val created = repository.createTodo(
                title,
                description,
                tags,
                priority,
                dueDate,
                reminder,
                scheduleRepeat,
                notificationChannels
            )
            _todo.value = created
        } else {
            _todo.value?.let { existing ->
                val updated = existing.copy(
                    title = title,
                    description = description,
                    tags = tags,
                    priority = priority,
                    completed = completed,
                    dueDate = dueDate,
                    reminder = reminder,
                    scheduleRepeat = scheduleRepeat,
                    notificationChannels = notificationChannels,
                    updatedAt = java.time.Instant.now().toString()
                )
                repository.updateTodo(updated)
                _todo.value = updated.copy(syncStatus = SyncStatus.PENDING_UPDATE)
            }
        }
        _isSaving.value = false
        _saveLabel.value = "Saved"
        if (announce) _messages.emit("Saved")
    }

    fun deleteTodo() {
        viewModelScope.launch {
            _todo.value?.let { todo ->
                repository.deleteTodo(todo)
                _todo.value = null
                _messages.emit("Moved to Trash")
            }
        }
    }

    fun restoreTodo() {
        viewModelScope.launch {
            _todo.value?.let { todo ->
                repository.restoreTodo(todo)
                _todo.value = todo.copy(isArchived = false, isDeleted = false, syncStatus = SyncStatus.PENDING_UPDATE)
                _saveLabel.value = "Restored"
                _messages.emit("Restored")
            }
        }
    }

    fun toggleComplete() {
        viewModelScope.launch {
            _todo.value?.let { todo ->
                repository.toggleComplete(todo)
                _todo.value = todo.copy(completed = !todo.completed)
            }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            _todo.value?.let { todo ->
                val updated = todo.copy(isFavorite = !todo.isFavorite)
                repository.updateTodo(updated)
                _todo.value = updated
            }
        }
    }

    fun shareTodo(userId: String) {
        viewModelScope.launch {
            _todo.value?.let { todo ->
                _shareResult.value = repository.shareTodo(todo, userId)
            }
        }
    }

    fun uploadAttachment(filePath: String) {
        viewModelScope.launch {
            _todo.value?.let { todo ->
                _isSaving.value = true
                val result = repository.uploadAttachment(todo.id, filePath, "todo")
                result?.let { attachment ->
                    _attachments.value = _attachments.value + attachment
                }
                _isSaving.value = false
            }
        }
    }

    fun removeAttachment(attachment: Attachment) {
        viewModelScope.launch {
            repository.deleteAttachment(attachment.id)
            _attachments.value = _attachments.value.filter { it.id != attachment.id }
            _todo.value?.let { todo ->
                val updated = todo.copy(attachments = _attachments.value)
                repository.updateTodo(updated)
                _todo.value = updated
            }
        }
    }

    class Factory(private val repository: TodoRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TodoDetailViewModel(repository) as T
        }
    }
}
