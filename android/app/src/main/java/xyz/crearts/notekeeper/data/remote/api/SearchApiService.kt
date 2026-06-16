package xyz.crearts.notekeeper.data.remote.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import xyz.crearts.notekeeper.data.model.SavedQuery
import xyz.crearts.notekeeper.data.model.SavedQueryInput
import xyz.crearts.notekeeper.data.model.SearchResult

interface SearchApiService {

    @GET("search")
    suspend fun search(
        @Query("query") query: String,
        @Query("type") type: String? = null,
        @Query("tags") tags: String? = null,
        @Query("priority") priority: String? = null
    ): Response<SearchResult>

    @GET("search/queries")
    suspend fun getSavedQueries(): Response<List<SavedQuery>>

    @POST("search/queries")
    suspend fun saveQuery(@Body input: SavedQueryInput): Response<SavedQuery>

    @DELETE("search/queries/{id}")
    suspend fun deleteQuery(@Path("id") id: String): Response<Unit>
}
