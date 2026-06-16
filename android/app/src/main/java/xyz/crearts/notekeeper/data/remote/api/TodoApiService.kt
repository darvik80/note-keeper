package xyz.crearts.notekeeper.data.remote.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import xyz.crearts.notekeeper.data.model.TodoInput
import xyz.crearts.notekeeper.data.model.TodoResponse

interface TodoApiService {

    @GET("todos")
    suspend fun getTodos(
        @Query("completed") completed: Boolean? = null,
        @Query("tag") tag: String? = null,
        @Query("priority") priority: String? = null,
        @Query("isFavorite") isFavorite: Boolean? = null,
        @Query("isArchived") isArchived: Boolean? = null,
        @Query("isDeleted") isDeleted: Boolean? = null
    ): Response<List<TodoResponse>>

    @GET("todos/{id}")
    suspend fun getTodoById(@Path("id") id: String): Response<TodoResponse>

    @POST("todos")
    suspend fun createTodo(@Body input: TodoInput): Response<TodoResponse>

    @PUT("todos/{id}")
    suspend fun updateTodo(@Path("id") id: String, @Body input: TodoInput): Response<TodoResponse>

    @DELETE("todos/{id}")
    suspend fun deleteTodo(
        @Path("id") id: String,
        @Query("permanent") permanent: Boolean = false
    ): Response<Unit>

    @POST("todos/{id}/archive")
    suspend fun archiveTodo(@Path("id") id: String): Response<TodoResponse>

    @POST("todos/{id}/restore")
    suspend fun restoreTodo(@Path("id") id: String): Response<TodoResponse>

    @GET("todos/shared-with-me")
    suspend fun getSharedWithMe(): Response<List<TodoResponse>>

    @POST("todos/{id}/share")
    suspend fun shareTodo(
        @Path("id") id: String,
        @Query("userId") userId: String
    ): Response<TodoResponse>

    @DELETE("todos/{id}/share")
    suspend fun unshareTodo(
        @Path("id") id: String,
        @Query("userId") userId: String
    ): Response<TodoResponse>
}

