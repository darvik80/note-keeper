package xyz.crearts.notekeeper.data.remote.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import xyz.crearts.notekeeper.data.model.NoteHistory
import xyz.crearts.notekeeper.data.model.NoteInput
import xyz.crearts.notekeeper.data.model.NoteResponse

interface NoteApiService {

    @GET("notes")
    suspend fun getNotes(
        @Query("folder") folder: String? = null,
        @Query("tag") tag: String? = null,
        @Query("priority") priority: String? = null,
        @Query("isFavorite") isFavorite: Boolean? = null,
        @Query("isEncrypted") isEncrypted: Boolean? = null,
        @Query("isArchived") isArchived: Boolean? = null,
        @Query("isDeleted") isDeleted: Boolean? = null
    ): Response<List<NoteResponse>>

    @GET("notes/{id}")
    suspend fun getNoteById(@Path("id") id: String): Response<NoteResponse>

    @POST("notes")
    suspend fun createNote(@Body input: NoteInput): Response<NoteResponse>

    @PUT("notes/{id}")
    suspend fun updateNote(@Path("id") id: String, @Body input: NoteInput): Response<NoteResponse>

    @DELETE("notes/{id}")
    suspend fun deleteNote(
        @Path("id") id: String,
        @Query("permanent") permanent: Boolean = false
    ): Response<Unit>

    @POST("notes/{id}/archive")
    suspend fun archiveNote(@Path("id") id: String): Response<NoteResponse>

    @POST("notes/{id}/restore")
    suspend fun restoreNote(@Path("id") id: String): Response<NoteResponse>

    @GET("notes/shared-with-me")
    suspend fun getSharedWithMe(): Response<List<NoteResponse>>

    @POST("notes/{id}/share")
    suspend fun shareNote(
        @Path("id") id: String,
        @Query("userId") userId: String
    ): Response<NoteResponse>

    @DELETE("notes/{id}/share")
    suspend fun unshareNote(
        @Path("id") id: String,
        @Query("userId") userId: String
    ): Response<NoteResponse>

    @GET("notes/{id}/history")
    suspend fun getNoteHistory(@Path("id") id: String): Response<List<NoteHistory>>
}

