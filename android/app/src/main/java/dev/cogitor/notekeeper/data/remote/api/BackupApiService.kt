package dev.cogitor.notekeeper.data.remote.api

import dev.cogitor.notekeeper.data.model.BackupSettings
import dev.cogitor.notekeeper.data.model.StatusMessage
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Streaming

interface BackupApiService {
    @Streaming
    @GET("backup/export")
    suspend fun export(): Response<ResponseBody>

    @Multipart
    @POST("backup/import")
    suspend fun importData(@Part file: MultipartBody.Part): Response<StatusMessage>

    @GET("backup/list")
    suspend fun list(): Response<List<Map<String, String>>>

    @DELETE("backup/delete/{filename}")
    suspend fun delete(@Path("filename") filename: String): Response<StatusMessage>

    @Streaming
    @GET("backup/download/{filename}")
    suspend fun download(@Path("filename") filename: String): Response<ResponseBody>

    @GET("backup/settings")
    suspend fun getSettings(): Response<BackupSettings>

    @POST("backup/settings")
    suspend fun saveSettings(@Body settings: BackupSettings): Response<StatusMessage>
}
