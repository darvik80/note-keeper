package dev.cogitor.notekeeper.data.remote

import dev.cogitor.notekeeper.BuildConfig
import dev.cogitor.notekeeper.data.local.SettingsDataStore
import dev.cogitor.notekeeper.data.local.TokenDataStore
import dev.cogitor.notekeeper.data.remote.api.AccountApiService
import dev.cogitor.notekeeper.data.remote.api.AttachmentApiService
import dev.cogitor.notekeeper.data.remote.api.AuthApiService
import dev.cogitor.notekeeper.data.remote.api.BackupApiService
import dev.cogitor.notekeeper.data.remote.api.NoteApiService
import dev.cogitor.notekeeper.data.remote.api.SearchApiService
import dev.cogitor.notekeeper.data.remote.api.TagApiService
import dev.cogitor.notekeeper.data.remote.api.TemplateApiService
import dev.cogitor.notekeeper.data.remote.api.TodoApiService
import dev.cogitor.notekeeper.data.remote.api.VaultApiService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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

    fun currentBaseUrl(): String = baseUrl

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BASIC
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
        redactHeader("Authorization")
        redactHeader("Cookie")
        redactHeader("Set-Cookie")
    }

    private val authInterceptor = okhttp3.Interceptor { chain ->
        val requestBuilder = chain.request().newBuilder()
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
    val vaultApiService: VaultApiService get() = retrofit.create(VaultApiService::class.java)
    val accountApiService: AccountApiService get() = retrofit.create(AccountApiService::class.java)
    val templateApiService: TemplateApiService get() = retrofit.create(TemplateApiService::class.java)
    val backupApiService: BackupApiService get() = retrofit.create(BackupApiService::class.java)
}
