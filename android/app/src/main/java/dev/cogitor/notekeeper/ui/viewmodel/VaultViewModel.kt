package dev.cogitor.notekeeper.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.cogitor.notekeeper.data.model.VaultFolder
import dev.cogitor.notekeeper.data.model.VaultFolderInput
import dev.cogitor.notekeeper.data.model.VaultItem
import dev.cogitor.notekeeper.data.model.VaultItemInput
import dev.cogitor.notekeeper.data.remote.api.VaultApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VaultViewModel(private val api: VaultApiService) : ViewModel() {
    private val _folders = MutableStateFlow<List<VaultFolder>>(emptyList())
    val folders = _folders.asStateFlow()
    private val _items = MutableStateFlow<List<VaultItem>>(emptyList())
    val items = _items.asStateFlow()
    private val _item = MutableStateFlow<VaultItem?>(null)
    val item = _item.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    private val _saving = MutableStateFlow(false)
    val saving = _saving.asStateFlow()

    private val _folderId = MutableStateFlow<String?>(null)
    val folderId = _folderId.asStateFlow()

    private var lastQuery: String? = null

    fun selectFolder(id: String?) {
        _folderId.value = id
        load()
    }

    fun load(folderId: String? = _folderId.value, q: String? = lastQuery) {
        lastQuery = q
        viewModelScope.launch {
            try {
                _folders.value = api.listFolders().body().orEmpty()
                _items.value = api.listItems(folderId, q).body().orEmpty()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun loadFolders() {
        viewModelScope.launch {
            try {
                _folders.value = api.listFolders().body().orEmpty()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun deleteFolder(id: String) {
        viewModelScope.launch {
            try {
                api.deleteFolder(id)
                if (_folderId.value == id) _folderId.value = null
                load()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun createFolder(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            try {
                api.createFolder(VaultFolderInput(trimmed, _folderId.value))
                load()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun loadItem(id: String) {
        viewModelScope.launch {
            try {
                _item.value = api.getItem(id).body()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun save(input: VaultItemInput, id: String?, revision: Int?) {
        viewModelScope.launch {
            _saving.value = true
            try {
                if (id == null) api.createItem(input)
                else api.updateItem(id, input, revision?.toString())
                load()
            } catch (e: Exception) {
                _error.value = e.message
            }
            _saving.value = false
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            api.deleteItem(id)
            load()
        }
    }

    class Factory(private val api: VaultApiService) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = VaultViewModel(api) as T
    }
}
