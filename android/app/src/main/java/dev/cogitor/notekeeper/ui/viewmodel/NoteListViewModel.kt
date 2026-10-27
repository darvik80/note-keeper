package dev.cogitor.notekeeper.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.cogitor.notekeeper.data.model.Note
import dev.cogitor.notekeeper.data.model.NoteFolders
import dev.cogitor.notekeeper.data.repository.NoteRepository
import dev.cogitor.notekeeper.data.sync.NetworkConnectivityObserver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.io.File

enum class NoteListFilter { Active, Favorite, Shared, Archive, Trash }

@OptIn(ExperimentalCoroutinesApi::class)
class NoteListViewModel(
    private val repository: NoteRepository,
    private val networkObserver: NetworkConnectivityObserver
) : ViewModel() {

    private val _allNotes = MutableStateFlow<List<Note>>(emptyList())

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncStatus = MutableStateFlow<String>("")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _filter = MutableStateFlow(NoteListFilter.Active)
    val filter: StateFlow<NoteListFilter> = _filter.asStateFlow()

    private val _folderPath = MutableStateFlow(NoteFolders.ALL)
    val folderPath: StateFlow<String> = _folderPath.asStateFlow()

    private val _folders = MutableStateFlow<List<String>>(emptyList())
    val folders: StateFlow<List<String>> = _folders.asStateFlow()

    init {
        viewModelScope.launch {
            _filter.flatMapLatest { current ->
                when (current) {
                    NoteListFilter.Active -> repository.getAllActiveNotes()
                    NoteListFilter.Favorite -> repository.getFavoriteNotes()
                    NoteListFilter.Shared -> flow { emit(repository.getSharedWithMe()) }
                    NoteListFilter.Archive -> repository.getArchivedNotes()
                    NoteListFilter.Trash -> repository.getDeletedNotes()
                }
            }.collect { list ->
                _allNotes.value = list
                _folders.value = NoteFolders.knownPaths(list)
                applyFolder(list)
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
            if (success) {
                delay(1600)
                if (_syncStatus.value == "Synced") _syncStatus.value = ""
            }
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

    fun restoreNote(note: Note) {
        viewModelScope.launch {
            repository.restoreNote(note)
        }
    }

    fun deleteForever(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note, permanent = true)
        }
    }

    fun importNote(file: File, folder: String?, subfolder: String?) {
        viewModelScope.launch {
            repository.importNote(file, folder, subfolder)
            syncNotes()
        }
    }

    fun setFilter(filter: NoteListFilter) {
        _filter.value = filter
    }

    fun setFolderPath(path: String) {
        _folderPath.value = path
        applyFolder(_allNotes.value)
    }

    private fun applyFolder(list: List<Note>) {
        val selected = _folderPath.value
        _notes.value = list.filter { NoteFolders.matches(it, selected) }
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
