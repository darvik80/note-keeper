package dev.cogitor.notekeeper.data.remote.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import dev.cogitor.notekeeper.data.model.VaultFolder
import dev.cogitor.notekeeper.data.model.VaultFolderInput
import dev.cogitor.notekeeper.data.model.VaultItem
import dev.cogitor.notekeeper.data.model.VaultItemInput

interface VaultApiService {
    @GET("vault/folders")
    suspend fun listFolders(): Response<List<VaultFolder>>

    @POST("vault/folders")
    suspend fun createFolder(@Body input: VaultFolderInput): Response<VaultFolder>

    @GET("vault/items")
    suspend fun listItems(
        @Query("folder") folder: String? = null,
        @Query("q") q: String? = null
    ): Response<List<VaultItem>>

    @GET("vault/items/{id}")
    suspend fun getItem(@Path("id") id: String): Response<VaultItem>

    @POST("vault/items")
    suspend fun createItem(@Body input: VaultItemInput): Response<VaultItem>

    @PUT("vault/items/{id}")
    suspend fun updateItem(
        @Path("id") id: String,
        @Body input: VaultItemInput,
        @Header("If-Match") revision: String? = null
    ): Response<VaultItem>

    @DELETE("vault/items/{id}")
    suspend fun deleteItem(@Path("id") id: String): Response<Unit>
}
