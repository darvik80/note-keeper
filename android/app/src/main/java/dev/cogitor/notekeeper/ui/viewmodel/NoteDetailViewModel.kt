package dev.cogitor.notekeeper.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.cogitor.notekeeper.data.model.Attachment
import dev.cogitor.notekeeper.data.model.Note
import dev.cogitor.notekeeper.data.model.SyncStatus
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
        folder: String = "default"
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            _saveLabel.value = "Saving…"
            val created = repository.createNote(title, content, folder, tags, priority)
            _note.value = created
            _isSaving.value = false
            _saveLabel.value = "Saved"
            _messages.emit("Saved")
        }
    }

    fun updateNote(
        title: String,
        content: String,
        tags: List<String> = emptyList(),
        priority: String = "medium",
        announce: Boolean = false
    ) {
        viewModelScope.launch {
            _note.value?.let { existing ->
                _isSaving.value = true
                _saveLabel.value = "Saving…"
                val updated = existing.copy(
                    title = title,
                    content = content,
                    tags = tags,
                    priority = priority,
                    updatedAt = java.time.Instant.now().toString()
                )
                repository.updateNote(updated)
                _note.value = updated.copy(syncStatus = SyncStatus.PENDING_UPDATE)
                _isSaving.value = false
                _saveLabel.value = "Saved"
                if (announce) _messages.emit("Saved")
            }
        }
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
