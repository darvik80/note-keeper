package dev.cogitor.notekeeper.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.cogitor.notekeeper.data.model.Attachment
import dev.cogitor.notekeeper.data.model.Note
import dev.cogitor.notekeeper.data.model.NoteHistory
import dev.cogitor.notekeeper.data.model.NoteTemplate
import dev.cogitor.notekeeper.data.model.NoteTemplateInput
import dev.cogitor.notekeeper.data.model.SyncStatus
import dev.cogitor.notekeeper.data.remote.RetrofitClient
import java.io.File
import dev.cogitor.notekeeper.data.repository.NoteRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NoteDetailViewModel(
    private val repository: NoteRepository
) : ViewModel() {

    private val _note = MutableStateFlow<Note?>(null)
    val note: StateFlow<Note?> = _note.asStateFlow()

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

    private val _knownFolders = MutableStateFlow<List<String>>(emptyList())
    val knownFolders: StateFlow<List<String>> = _knownFolders.asStateFlow()

    private val _history = MutableStateFlow<List<NoteHistory>>(emptyList())
    val history: StateFlow<List<NoteHistory>> = _history.asStateFlow()

    private val _historyLoading = MutableStateFlow(false)
    val historyLoading: StateFlow<Boolean> = _historyLoading.asStateFlow()

    private val _historyError = MutableStateFlow<String?>(null)
    val historyError: StateFlow<String?> = _historyError.asStateFlow()

    private val _templates = MutableStateFlow<List<NoteTemplate>>(emptyList())
    val templates: StateFlow<List<NoteTemplate>> = _templates.asStateFlow()

    init {
        viewModelScope.launch {
            _knownFolders.value = repository.folderPaths()
        }
        viewModelScope.launch {
            _templates.value = runCatching {
                RetrofitClient.templateApiService.list().body().orEmpty()
            }.getOrElse { emptyList() }
        }
    }

    fun loadNote(id: String) {
        viewModelScope.launch {
            val note = repository.getNoteById(id)
            _note.value = note
            _attachments.value = note?.attachments ?: emptyList()
        }
    }

    fun createNote(
        title: String,
        content: String,
        tags: List<String> = emptyList(),
        priority: String = "medium",
        folder: String = "default",
        subfolder: String? = null,
        reminder: String? = null
    ) {
        viewModelScope.launch {
            persist(true, title, content, tags, priority, folder, subfolder, reminder, announce = true)
        }
    }

    fun updateNote(
        title: String,
        content: String,
        tags: List<String> = emptyList(),
        priority: String = "medium",
        folder: String = "default",
        subfolder: String? = null,
        reminder: String? = null,
        announce: Boolean = false
    ) {
        viewModelScope.launch {
            persist(false, title, content, tags, priority, folder, subfolder, reminder, announce = announce)
        }
    }

    suspend fun persist(
        isNew: Boolean,
        title: String,
        content: String,
        tags: List<String> = emptyList(),
        priority: String = "medium",
        folder: String = "default",
        subfolder: String? = null,
        reminder: String? = null,
        announce: Boolean = false
    ) {
        if (title.isBlank()) return
        _isSaving.value = true
        _saveLabel.value = "Saving…"
        val resolvedFolder = folder.trim().ifBlank { "default" }
        val resolvedSub = subfolder?.trim()?.takeIf { it.isNotBlank() }
        if (isNew && _note.value == null) {
            val created = repository.createNote(
                title, content, resolvedFolder, tags, priority, resolvedSub, reminder
            )
            _note.value = created
        } else {
            _note.value?.let { existing ->
                val updated = existing.copy(
                    title = title,
                    content = content,
                    tags = tags,
                    priority = priority,
                    folder = resolvedFolder,
                    subfolder = resolvedSub,
                    reminder = reminder,
                    updatedAt = java.time.Instant.now().toString()
                )
                repository.updateNote(updated)
                _note.value = updated.copy(syncStatus = SyncStatus.PENDING_UPDATE)
            }
        }
        _isSaving.value = false
        _saveLabel.value = "Saved"
        if (announce) _messages.emit("Saved")
    }

    fun deleteNote() {
        viewModelScope.launch {
            _note.value?.let { note ->
                repository.deleteNote(note)
                _note.value = null
                _messages.emit("Moved to Trash")
            }
        }
    }

    fun deleteForever() {
        viewModelScope.launch {
            _note.value?.let { note ->
                repository.deleteNote(note, permanent = true)
                _note.value = null
                _messages.emit("Deleted forever")
            }
        }
    }

    fun loadHistory() {
        viewModelScope.launch {
            val note = _note.value ?: return@launch
            _history.value = emptyList()
            _historyError.value = null
            if (note.syncStatus == SyncStatus.PENDING_CREATE) {
                _historyError.value = "Note not on server yet. Wait for sync, then open History."
                return@launch
            }
            _historyLoading.value = true
            repository.history(note.id)
                .onSuccess { _history.value = it }
                .onFailure { _historyError.value = it.message ?: "Could not load history" }
            _historyLoading.value = false
        }
    }

    fun restoreVersion(historyId: String) {
        viewModelScope.launch {
            val current = _note.value ?: return@launch
            val restored = repository.restoreVersion(current, historyId)
            if (restored == null) {
                _messages.emit("Restore failed")
                return@launch
            }
            _note.value = restored
            _attachments.value = restored.attachments.orEmpty()
            _messages.emit("Version restored")
            loadHistory()
        }
    }

    suspend fun downloadAttachment(attachment: Attachment, dest: File): File? {
        return repository.downloadAttachment(attachment.id, dest)
    }

    fun restoreNote() {
        viewModelScope.launch {
            _note.value?.let { note ->
                repository.restoreNote(note)
                _note.value = note.copy(isArchived = false, isDeleted = false, syncStatus = SyncStatus.PENDING_UPDATE)
                _saveLabel.value = "Restored"
                _messages.emit("Restored")
            }
        }
    }

    fun shareNote(userId: String) {
        viewModelScope.launch {
            _note.value?.let { note ->
                _shareResult.value = repository.shareNote(note, userId)
            }
        }
    }

    fun uploadAttachment(filePath: String) {
        viewModelScope.launch {
            _note.value?.let { note ->
                _isSaving.value = true
                val result = repository.uploadAttachment(note.id, filePath, "note")
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
            _note.value?.let { note ->
                val updated = note.copy(attachments = _attachments.value)
                repository.updateNote(updated)
                _note.value = updated
            }
        }
    }

    fun saveAsTemplate(name: String, content: String, tags: List<String>) {
        viewModelScope.launch {
            runCatching {
                RetrofitClient.templateApiService.create(
                    NoteTemplateInput(name, content, tags)
                )
            }.onSuccess { response ->
                if (response.isSuccessful) {
                    _templates.value = _templates.value + listOfNotNull(response.body())
                    _messages.emit("Template saved")
                } else {
                    _messages.emit("Template save failed")
                }
            }.onFailure { _messages.emit(it.message ?: "Template save failed") }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            _note.value?.let { note ->
                val updated = note.copy(isFavorite = !note.isFavorite)
                repository.updateNote(updated)
                _note.value = updated
            }
        }
    }

    class Factory(private val repository: NoteRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NoteDetailViewModel(repository) as T
        }
    }
}
