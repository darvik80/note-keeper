package dev.cogitor.notekeeper.data.remote.api

import dev.cogitor.notekeeper.data.model.AppConfig
import dev.cogitor.notekeeper.data.model.AuthRequest
import dev.cogitor.notekeeper.data.model.AuthResponse
import dev.cogitor.notekeeper.data.model.OAuthExchangeRequest
import dev.cogitor.notekeeper.data.model.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApiService {

    @GET("config")
    suspend fun getConfig(): Response<AppConfig>


    @POST("auth/login")
    suspend fun login(@Body request: AuthRequest): Response<AuthResponse>

    @POST("auth/register")
    suspend fun register(@Body request: AuthRequest): Response<AuthResponse>

    @POST("auth/oauth/exchange")
    suspend fun exchangeOAuth(@Body request: OAuthExchangeRequest): Response<AuthResponse>

    @GET("auth/me")
    suspend fun getCurrentUser(@Header("Authorization") authHeader: String): Response<User>
}
