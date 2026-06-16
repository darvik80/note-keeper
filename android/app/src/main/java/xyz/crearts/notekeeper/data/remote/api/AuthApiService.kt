package xyz.crearts.notekeeper.data.remote.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import xyz.crearts.notekeeper.data.model.AuthRequest
import xyz.crearts.notekeeper.data.model.AuthResponse
import xyz.crearts.notekeeper.data.model.User

interface AuthApiService {

    @POST("auth/login")
    suspend fun login(@Body request: AuthRequest): Response<AuthResponse>

    @POST("auth/register")
    suspend fun register(@Body request: AuthRequest): Response<AuthResponse>

    @GET("auth/me")
    suspend fun getCurrentUser(@Header("Authorization") authHeader: String): Response<User>
}
