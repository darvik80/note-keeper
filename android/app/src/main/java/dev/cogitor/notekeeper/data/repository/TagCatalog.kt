package dev.cogitor.notekeeper.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.cogitor.notekeeper.data.local.NoteDatabase
import dev.cogitor.notekeeper.data.remote.RetrofitClient

object TagCatalog {
    private val gson = Gson()
    private val listType = object : TypeToken<List<String>>() {}.type

    suspend fun load(context: Context): List<String> {
        val remote = runCatching {
            RetrofitClient.tagApiService.getAllTags().body().orEmpty()
        }.getOrElse { emptyList() }
        val db = NoteDatabase.getDatabase(context)
        val local = (db.noteDao().allTagJson() + db.todoDao().allTagJson())
            .flatMap(::parse)
        return (remote + local)
            .map { it.trim().trimStart('#') }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }
    }

    private fun parse(raw: String): List<String> = runCatching {
        gson.fromJson<List<String>>(raw, listType).orEmpty()
    }.getOrElse { emptyList() }
}
