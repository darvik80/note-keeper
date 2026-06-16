package xyz.crearts.notekeeper.data.remote

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import xyz.crearts.notekeeper.data.local.SettingsDataStore
import xyz.crearts.notekeeper.data.local.TokenDataStore
import xyz.crearts.notekeeper.data.remote.api.AttachmentApiService
import xyz.crearts.notekeeper.data.remote.api.AuthApiService
import xyz.crearts.notekeeper.data.remote.api.NoteApiService
import xyz.crearts.notekeeper.data.remote.api.SearchApiService
import xyz.crearts.notekeeper.data.remote.api.TagApiService
import xyz.crearts.notekeeper.data.remote.api.TodoApiService

object RetrofitClient {

    private var baseUrl: String = SettingsDataStore.DEFAULT_URL
    private var tokenDataStore: TokenDataStore? = null
    private var _retrofit: Retrofit? = null

    fun initTokenStore(store: TokenDataStore) {
        tokenDataStore = store
    }

    fun setBaseUrl(url: String) {
        if (!url.endsWith("/")) {
            baseUrl = "$url/"
        } else {
            baseUrl = url
        }
        _retrofit = null
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor = okhttp3.Interceptor { chain ->
        val requestBuilder = chain.request().newBuilder()
            .addHeader("Content-Type", "application/json")
        val token = runBlocking {
            tokenDataStore?.token?.first()
        }
        if (token != null) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        chain.proceed(requestBuilder.build())
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor)
        .build()

    private val retrofit: Retrofit
        get() {
            return _retrofit ?: Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build().also { _retrofit = it }
        }

    val noteApiService: NoteApiService get() = retrofit.create(NoteApiService::class.java)
    val authApiService: AuthApiService get() = retrofit.create(AuthApiService::class.java)
    val attachmentApiService: AttachmentApiService get() = retrofit.create(AttachmentApiService::class.java)
    val todoApiService: TodoApiService get() = retrofit.create(TodoApiService::class.java)
    val tagApiService: TagApiService get() = retrofit.create(TagApiService::class.java)
    val searchApiService: SearchApiService get() = retrofit.create(SearchApiService::class.java)
}
