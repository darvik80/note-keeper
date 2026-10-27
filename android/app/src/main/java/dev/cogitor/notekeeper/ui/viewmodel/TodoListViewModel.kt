package dev.cogitor.notekeeper.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.cogitor.notekeeper.data.model.Todo
import dev.cogitor.notekeeper.data.repository.TodoRepository
import dev.cogitor.notekeeper.data.sync.NetworkConnectivityObserver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

enum class TodoListFilter { Active, Done, Archive, Trash }

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

    private val _filter = MutableStateFlow(TodoListFilter.Active)
    val filter: StateFlow<TodoListFilter> = _filter.asStateFlow()

    init {
        viewModelScope.launch {
            _filter.flatMapLatest { current ->
                when (current) {
                    TodoListFilter.Active -> repository.getTodosByCompletion(completed = false)
                    TodoListFilter.Done -> repository.getTodosByCompletion(completed = true)
                    TodoListFilter.Archive -> repository.getArchivedTodos()
                    TodoListFilter.Trash -> repository.getDeletedTodos()
                }
            }.collect { list ->
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

    fun setFilter(filter: TodoListFilter) {
        _filter.value = filter
    }

    fun deleteTodo(todo: Todo) {
        viewModelScope.launch {
            repository.deleteTodo(todo)
        }
    }

    fun archiveTodo(todo: Todo) {
        viewModelScope.launch {
            repository.archiveTodo(todo)
        }
    }

    fun restoreTodo(todo: Todo) {
        viewModelScope.launch {
            repository.restoreTodo(todo)
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
