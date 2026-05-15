package xyz.crearts.notekeeper.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import xyz.crearts.notekeeper.data.local.TodoDao
import xyz.crearts.notekeeper.data.model.Attachment
import xyz.crearts.notekeeper.data.model.SyncStatus
import xyz.crearts.notekeeper.data.model.Todo
import xyz.crearts.notekeeper.data.remote.api.AttachmentApiService
import xyz.crearts.notekeeper.data.remote.api.TodoApiService
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
        if (permanent || todo.syncStatus == SyncStatus.PENDING_CREATE) {
            if (todo.localId != 0L) {
                todoDao.deleteTodoByLocalId(todo.localId)
            } else {
                val entity = todoDao.getTodoById(todo.id)
                entity?.let { todoDao.deleteTodoByLocalId(it.localId) }
            }
        } else {
            val entity = TodoMapper.toEntity(todo.copy(isDeleted = true, syncStatus = SyncStatus.PENDING_DELETE))
            todoDao.updateTodo(entity)
        }
    }

    suspend fun archiveTodo(todo: Todo) {
        val entity = TodoMapper.toEntity(todo.copy(isArchived = true, syncStatus = SyncStatus.PENDING_UPDATE))
        todoDao.updateTodo(entity)
    }

    suspend fun restoreTodo(todo: Todo) {
        val entity = TodoMapper.toEntity(
            todo.copy(
                isArchived = false,
                isDeleted = false,
                syncStatus = SyncStatus.PENDING_UPDATE
            )
        )
        todoDao.updateTodo(entity)
    }

    suspend fun toggleComplete(todo: Todo) {
        val updated = todo.copy(completed = !todo.completed, syncStatus = SyncStatus.PENDING_UPDATE)
        todoDao.updateTodo(TodoMapper.toEntity(updated))
    }

    suspend fun shareTodo(todo: Todo, userId: String): String {
        return try {
            val response = apiService.shareTodo(todo.id, userId)
            if (response.isSuccessful) {
                response.body()?.let { serverTodo ->
                    todoDao.updateTodo(TodoMapper.toEntity(serverTodo.copy(syncStatus = SyncStatus.SYNCED, localId = todo.localId)))
                }
                "Shared successfully"
            } else {
                "Share failed: ${response.code()}"
            }
        } catch (e: Exception) {
            "Share error: ${e.message}"
        }
    }

    suspend fun uploadAttachment(parentId: String, filePath: String, parentType: String): Attachment? {
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
            val pendingCreates = todoDao.getPendingSyncTodos().filter { it.syncStatus == SyncStatus.PENDING_CREATE.name }
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

            val pendingUpdates = todoDao.getPendingSyncTodos().filter { it.syncStatus == SyncStatus.PENDING_UPDATE.name }
            for (entity in pendingUpdates) {
                val todo = TodoMapper.toDomain(entity)
                val input = TodoMapper.toInput(todo)
                val response = apiService.updateTodo(todo.id, input)
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

            val response = apiService.getTodos()
            if (response.isSuccessful) {
                response.body()?.let { serverTodos ->
                    for (serverTodo in serverTodos) {
                        val existing = todoDao.getTodoById(serverTodo.id)
                        if (existing == null) {
                            todoDao.insertTodo(TodoMapper.toEntity(serverTodo.copy(syncStatus = SyncStatus.SYNCED)))
                        } else if (existing.syncStatus == SyncStatus.SYNCED.name) {
                            todoDao.updateTodo(TodoMapper.toEntity(serverTodo.copy(syncStatus = SyncStatus.SYNCED, localId = existing.localId)))
                        }
                    }
                }
                Result.success(true)
            } else {
                Result.failure(Exception("Server ${response.code()}: ${response.errorBody()?.string()?.take(100)}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
