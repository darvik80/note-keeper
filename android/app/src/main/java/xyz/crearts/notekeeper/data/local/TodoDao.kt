package xyz.crearts.notekeeper.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import xyz.crearts.notekeeper.data.local.entity.TodoEntity

@Dao
interface TodoDao {

    @Query("SELECT * FROM todos WHERE isDeleted = 0 AND isArchived = 0 ORDER BY updatedAt DESC, createdAt DESC")
    fun getAllActiveTodos(): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE id = :id LIMIT 1")
    suspend fun getTodoById(id: String): TodoEntity?

    @Query("SELECT * FROM todos WHERE localId = :localId LIMIT 1")
    suspend fun getTodoByLocalId(localId: Long): TodoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: TodoEntity): Long

    @Update
    suspend fun updateTodo(todo: TodoEntity)

    @Query("DELETE FROM todos WHERE localId = :localId")
    suspend fun deleteTodoByLocalId(localId: Long)

    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun deleteTodoById(id: String)

    @Query("SELECT * FROM todos WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSyncTodos(): List<TodoEntity>

    @Query("SELECT * FROM todos WHERE syncStatus = 'PENDING_DELETE'")
    suspend fun getPendingDeleteTodos(): List<TodoEntity>

    @Query("UPDATE todos SET syncStatus = 'SYNCED' WHERE localId = :localId")
    suspend fun markAsSynced(localId: Long)

    @Query("UPDATE todos SET id = :newId, syncStatus = 'SYNCED' WHERE localId = :localId")
    suspend fun updateServerId(localId: Long, newId: String)

    @Query("SELECT * FROM todos WHERE isFavorite = 1 AND isDeleted = 0 AND isArchived = 0 ORDER BY updatedAt DESC")
    fun getFavoriteTodos(): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE isArchived = 1 AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getArchivedTodos(): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedTodos(): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE completed = :completed AND isDeleted = 0 AND isArchived = 0 ORDER BY updatedAt DESC")
    fun getTodosByCompletion(completed: Boolean): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchTodos(query: String): Flow<List<TodoEntity>>

    @Query("DELETE FROM todos WHERE isDeleted = 1 AND syncStatus = 'SYNCED'")
    suspend fun clearSyncedDeletedTodos()
}
