package dev.cogitor.notekeeper.data.remote.api

import dev.cogitor.notekeeper.data.model.NoteTemplate
import dev.cogitor.notekeeper.data.model.NoteTemplateInput
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface TemplateApiService {
    @GET("templates")
    suspend fun list(@Query("category") category: String? = null): Response<List<NoteTemplate>>

    @POST("templates")
    suspend fun create(@Body input: NoteTemplateInput): Response<NoteTemplate>

    @GET("templates/{id}")
    suspend fun get(@Path("id") id: String): Response<NoteTemplate>

    @DELETE("templates/{id}")
    suspend fun delete(@Path("id") id: String): Response<Void>
}
