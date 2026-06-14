package xyz.crearts.notekeeper.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import xyz.crearts.notekeeper.data.model.Note
import xyz.crearts.notekeeper.data.repository.NoteRepository
import xyz.crearts.notekeeper.data.sync.NetworkConnectivityObserver

class NoteListViewModel(
    private val repository: NoteRepository,
    private val networkObserver: NetworkConnectivityObserver
) : ViewModel() {

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncStatus = MutableStateFlow<String>("")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllActiveNotes().collect { list ->
                _notes.value = list
            }
        }
        viewModelScope.launch {
            networkObserver.observe().collect { status ->
                if (status == NetworkConnectivityObserver.Status.Available) {
                    syncNotes()
                }
            }
        }
    }

    fun syncNotes() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncStatus.value = "Syncing..."
            val success = repository.syncNotes()
            _syncStatus.value = if (success) "Synced" else "Sync failed"
            _isSyncing.value = false
        }
    }

    fun refreshNotes() {
        viewModelScope.launch {
            _isSyncing.value = true
            repository.refreshNotes()
            _isSyncing.value = false
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    fun archiveNote(note: Note) {
        viewModelScope.launch {
            repository.archiveNote(note)
        }
    }

    class Factory(
        private val repository: NoteRepository,
        private val networkObserver: NetworkConnectivityObserver
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NoteListViewModel(repository, networkObserver) as T
        }
    }
}
