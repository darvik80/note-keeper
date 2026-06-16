package xyz.crearts.notekeeper.data.remote.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST

interface TagApiService {

    @GET("tags")
    suspend fun getAllTags(): Response<List<String>>

    @POST("tags/rebuild")
    suspend fun rebuildTags(): Response<Unit>
}
