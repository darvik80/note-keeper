package dev.cogitor.notekeeper.data.remote.api

import dev.cogitor.notekeeper.data.model.ApiToken
import dev.cogitor.notekeeper.data.model.ApiTokenCreateRequest
import dev.cogitor.notekeeper.data.model.IntegrationRequest
import dev.cogitor.notekeeper.data.model.IntegrationResponse
import dev.cogitor.notekeeper.data.model.User
import dev.cogitor.notekeeper.data.model.UserSettings
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AccountApiService {
    @GET("users/search")
    suspend fun searchUsers(@Query("query") query: String): Response<List<User>>

    @POST("users/batch")
    suspend fun usersByIds(@Body ids: List<String>): Response<List<User>>

    @GET("settings")
    suspend fun getSettings(): Response<UserSettings>

    @POST("settings")
    suspend fun saveSettings(@Body settings: UserSettings): Response<Void>

    @GET("settings/daily-report/preview")
    suspend fun previewDailyReport(): Response<ResponseBody>

    @POST("settings/daily-report/test")
    suspend fun testDailyReport(): Response<Void>

    @POST("integrations/telegram")
    suspend fun sendTelegram(@Body request: IntegrationRequest): Response<IntegrationResponse>

    @POST("integrations/dingtalk")
    suspend fun sendDingtalk(@Body request: IntegrationRequest): Response<IntegrationResponse>

    @POST("integrations/telegram/test-todo")
    suspend fun testTelegramTodo(): Response<IntegrationResponse>

    @GET("tokens")
    suspend fun listTokens(): Response<List<ApiToken>>

    @POST("tokens")
    suspend fun createToken(@Body request: ApiTokenCreateRequest): Response<ApiToken>

    @DELETE("tokens/{id}")
    suspend fun revokeToken(@Path("id") id: String): Response<Void>
}
