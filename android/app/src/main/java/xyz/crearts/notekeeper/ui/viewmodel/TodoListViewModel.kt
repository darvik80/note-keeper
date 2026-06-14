package xyz.crearts.notekeeper.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import xyz.crearts.notekeeper.data.model.Todo
import xyz.crearts.notekeeper.data.repository.TodoRepository
import xyz.crearts.notekeeper.data.sync.NetworkConnectivityObserver

class TodoListViewModel(
    private val repository: TodoRepository,
    private val networkObserver: NetworkConnectivityObserver
) : ViewModel() {

    private val _todos = MutableStateFlow<List<Todo>>(emptyList())
    val todos: StateFlow<List<Todo>> = _todos.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncStatus = MutableStateFlow<String>("")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _showCompleted = MutableStateFlow(false)
    val showCompleted: StateFlow<Boolean> = _showCompleted.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllActiveTodos().collect { list ->
                _todos.value = list
            }
        }
        viewModelScope.launch {
            networkObserver.observe().collect { status ->
                if (status == NetworkConnectivityObserver.Status.Available) {
                    syncTodos()
                }
            }
        }
    }

    fun syncTodos() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncStatus.value = "Syncing..."
            val result = repository.syncTodos()
            _syncStatus.value = result.fold(
                onSuccess = { "Synced" },
                onFailure = { e -> "Sync failed: ${e.message?.take(80)}" }
            )
            _isSyncing.value = false
        }
    }

    fun toggleShowCompleted() {
        _showCompleted.value = !_showCompleted.value
        viewModelScope.launch {
            if (_showCompleted.value) {
                repository.getTodosByCompletion(true).collect { list ->
                    _todos.value = list
                }
            } else {
                repository.getAllActiveTodos().collect { list ->
                    _todos.value = list
                }
            }
        }
    }

    fun deleteTodo(todo: Todo) {
        viewModelScope.launch {
            repository.deleteTodo(todo)
        }
    }

    fun toggleComplete(todo: Todo) {
        viewModelScope.launch {
            repository.toggleComplete(todo)
        }
    }

    class Factory(
        private val repository: TodoRepository,
        private val networkObserver: NetworkConnectivityObserver
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TodoListViewModel(repository, networkObserver) as T
        }
    }
}
