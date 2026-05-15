package xyz.crearts.notekeeper.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import xyz.crearts.notekeeper.data.model.Attachment
import xyz.crearts.notekeeper.data.model.Note
import xyz.crearts.notekeeper.data.model.SyncStatus
import xyz.crearts.notekeeper.data.repository.NoteRepository

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
            repository.createNote(title, content, folder, tags, priority)
            _isSaving.value = false
        }
    }

    fun updateNote(
        title: String,
        content: String,
        tags: List<String> = emptyList(),
        priority: String = "medium"
    ) {
        viewModelScope.launch {
            _note.value?.let { existing ->
                _isSaving.value = true
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
            }
        }
    }

    fun deleteNote() {
        viewModelScope.launch {
            _note.value?.let { note ->
                repository.deleteNote(note)
                _note.value = null
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
