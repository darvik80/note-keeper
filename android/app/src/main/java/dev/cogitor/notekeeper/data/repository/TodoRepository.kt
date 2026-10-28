package dev.cogitor.notekeeper.data.repository

import dev.cogitor.notekeeper.data.local.TodoDao
import dev.cogitor.notekeeper.data.model.Attachment
import dev.cogitor.notekeeper.data.model.SyncStatus
import dev.cogitor.notekeeper.data.model.Todo
import dev.cogitor.notekeeper.data.remote.api.AttachmentApiService
import dev.cogitor.notekeeper.data.remote.api.TodoApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.UUID

class TodoRepository(
    private val todoDao: TodoDao,
    private val apiService: TodoApiService,
    private val attachmentApiService: AttachmentApiService
) {

    fun getAllActiveTodos(): Flow<List<Todo>> {
        return todoDao.getAllActiveTodos().map { entities ->
            entities.map { TodoMapper.toDomain(it) }
        }
    }

    fun getTodosByCompletion(completed: Boolean): Flow<List<Todo>> {
        return todoDao.getTodosByCompletion(completed).map { entities ->
            entities.map { TodoMapper.toDomain(it) }
        }
    }

    fun getFavoriteTodos(): Flow<List<Todo>> {
        return todoDao.getFavoriteTodos().map { entities ->
            entities.map { TodoMapper.toDomain(it) }
        }
    }

    fun getArchivedTodos(): Flow<List<Todo>> {
        return todoDao.getArchivedTodos().map { entities ->
            entities.map { TodoMapper.toDomain(it) }
        }
    }

    fun getDeletedTodos(): Flow<List<Todo>> {
        return todoDao.getDeletedTodos().map { entities ->
            entities.map { TodoMapper.toDomain(it) }
        }
    }

    fun searchTodos(query: String): Flow<List<Todo>> {
        return todoDao.searchTodos(query).map { entities ->
            entities.map { TodoMapper.toDomain(it) }
        }
    }

    suspend fun getTodoById(id: String): Todo? {
        return todoDao.getTodoById(id)?.let { TodoMapper.toDomain(it) }
    }

    suspend fun createTodo(
        title: String,
        description: String = "",
        tags: List<String> = emptyList(),
        priority: String = "medium",
        dueDate: String? = null,
        reminder: String? = null,
        scheduleRepeat: String = "none",
        notificationChannels: String? = null
    ): Todo {
        val localId = UUID.randomUUID().toString()
        val todo = Todo(
            id = localId,
            title = title,
            description = description,
            tags = tags,
            priority = priority,
            dueDate = dueDate,
            reminder = reminder,
            scheduleRepeat = scheduleRepeat,
            notificationChannels = notificationChannels,
            syncStatus = SyncStatus.PENDING_CREATE
        )
        todoDao.insertTodo(TodoMapper.toEntity(todo))
        return todo
    }

    suspend fun updateTodo(todo: Todo) {
        val entity = TodoMapper.toEntity(todo.copy(syncStatus = SyncStatus.PENDING_UPDATE))
        todoDao.updateTodo(entity)
    }

    suspend fun deleteTodo(todo: Todo, permanent: Boolean = false) {
        if (permanent) {
            runCatching { apiService.deleteTodo(todo.id, permanent = true) }
        }
        if (permanent || todo.syncStatus == SyncStatus.PENDING_CREATE) {
            if (todo.localId != 0L) {
                todoDao.deleteTodoByLocalId(todo.localId)
            } else {
                val entity = todoDao.getTodoById(todo.id)
                entity?.let { todoDao.deleteTodoByLocalId(it.localId) }
            }
        } else {
            val entity = TodoMapper.toEntity(
                todo.copy(
                    isDeleted = true,
                    syncStatus = SyncStatus.PENDING_DELETE
                )
            )
            todoDao.updateTodo(entity)
        }
    }

    suspend fun archiveTodo(todo: Todo) {
        persistArchiveFlag(todo, archived = true)
    }

    suspend fun restoreTodo(todo: Todo) {
        persistArchiveFlag(todo, archived = false)
    }

    private suspend fun persistArchiveFlag(todo: Todo, archived: Boolean) {
        todoDao.setArchivedFlag(todo.id, archived, SyncStatus.PENDING_UPDATE.name)
        val ok = runCatching {
            val response = if (archived) apiService.archiveTodo(todo.id) else apiService.restoreTodo(todo.id)
            response.isSuccessful
        }.getOrDefault(false)
        if (ok) {
            todoDao.setArchivedFlag(todo.id, archived, SyncStatus.SYNCED.name)
        }
    }

    suspend fun toggleComplete(todo: Todo): Todo {
        return try {
            val response = apiService.toggleComplete(todo.id)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                val mapped = TodoMapper.fromResponse(body).copy(
                    localId = todo.localId,
                    syncStatus = SyncStatus.SYNCED
                )
                todoDao.updateTodo(TodoMapper.toEntity(mapped))
                mapped
            } else {
                toggleCompleteLocal(todo)
            }
        } catch (_: Exception) {
            toggleCompleteLocal(todo)
        }
    }

    private suspend fun toggleCompleteLocal(todo: Todo): Todo {
        val updated = todo.copy(completed = !todo.completed, syncStatus = SyncStatus.PENDING_UPDATE)
        todoDao.updateTodo(TodoMapper.toEntity(updated))
        return updated
    }

    suspend fun completions(id: String) = runCatching {
        apiService.getCompletions(id).body().orEmpty()
    }.getOrElse { emptyList() }

    suspend fun shareTodo(todo: Todo, userId: String): String {
        return try {
            val response = apiService.shareTodo(todo.id, userId)
            if (response.isSuccessful) {
                response.body()?.let { serverTodo ->
                    val domainTodo = TodoMapper.fromResponse(serverTodo)
                    todoDao.updateTodo(
                        TodoMapper.toEntity(
                            domainTodo.copy(
                                syncStatus = SyncStatus.SYNCED,
                                localId = todo.localId
                            )
                        )
                    )
                }
                "Shared successfully"
            } else {
                "Share failed: ${response.code()}"
            }
        } catch (e: Exception) {
            "Share error: ${e.message}"
        }
    }

    suspend fun getSharedWithMe(): List<Todo> {
        return try {
            val response = apiService.getSharedWithMe()
            if (response.isSuccessful) {
                response.body()?.map { TodoMapper.fromResponse(it) } ?: emptyList()
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun uploadAttachment(
        parentId: String,
        filePath: String,
        parentType: String
    ): Attachment? {
        return try {
            val file = File(filePath)
            val requestFile = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
            val parentIdBody = parentId.toRequestBody("text/plain".toMediaTypeOrNull())
            val parentTypeBody = parentType.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = attachmentApiService.uploadAttachment(body, parentIdBody, parentTypeBody)
            if (response.isSuccessful) {
                response.body()
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteAttachment(attachmentId: String): Boolean {
        return try {
            val response = attachmentApiService.deleteAttachment(attachmentId)
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun syncTodos(): Result<Boolean> {
        return try {
            val pendingCreates = todoDao.getPendingSyncTodos()
                .filter { it.syncStatus == SyncStatus.PENDING_CREATE.name }
            for (entity in pendingCreates) {
                val todo = TodoMapper.toDomain(entity)
                val input = TodoMapper.toInput(todo)
                val response = apiService.createTodo(input)
                if (response.isSuccessful) {
                    response.body()?.let { serverTodo ->
                        todoDao.updateServerId(entity.localId, serverTodo.id)
                    }
                }
            }

            val pendingUpdates = todoDao.getPendingSyncTodos()
                .filter { it.syncStatus == SyncStatus.PENDING_UPDATE.name }
            for (entity in pendingUpdates) {
                val todo = TodoMapper.toDomain(entity)
                val response = if (todo.isArchived) {
                    apiService.archiveTodo(todo.id)
                } else {
                    runCatching { apiService.restoreTodo(todo.id) }
                    apiService.updateTodo(todo.id, TodoMapper.toInput(todo))
                }
                if (response.isSuccessful) {
                    todoDao.markAsSynced(entity.localId)
                }
            }

            val pendingDeletes = todoDao.getPendingDeleteTodos()
            for (entity in pendingDeletes) {
                val response = apiService.deleteTodo(entity.id, permanent = false)
                if (response.isSuccessful) {
                    todoDao.deleteTodoByLocalId(entity.localId)
                }
            }

            pullTodosFromServer()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun pullTodosFromServer() {
        val batches = listOf(
            apiService.getTodos(isArchived = false, isDeleted = false),
            apiService.getTodos(isArchived = true, isDeleted = false)
        )
        for (response in batches) {
            if (!response.isSuccessful) continue
            for (serverTodo in response.body().orEmpty()) {
                val domainTodo = TodoMapper.fromResponse(serverTodo)
                val existing = todoDao.getTodoById(domainTodo.id)
                if (existing == null) {
                    todoDao.insertTodo(TodoMapper.toEntity(domainTodo.copy(syncStatus = SyncStatus.SYNCED)))
                } else if (existing.syncStatus == SyncStatus.SYNCED.name) {
                    todoDao.updateTodo(
                        TodoMapper.toEntity(
                            domainTodo.copy(
                                syncStatus = SyncStatus.SYNCED,
                                localId = existing.localId
                            )
                        )
                    )
                }
            }
        }
    }
}
