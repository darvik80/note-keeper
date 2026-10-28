package dev.cogitor.notekeeper.data.repository

import dev.cogitor.notekeeper.data.local.NoteDao
import dev.cogitor.notekeeper.data.model.Attachment
import dev.cogitor.notekeeper.data.model.Note
import dev.cogitor.notekeeper.data.model.NoteFolders
import dev.cogitor.notekeeper.data.model.SyncStatus
import dev.cogitor.notekeeper.data.remote.api.AttachmentApiService
import dev.cogitor.notekeeper.data.remote.api.NoteApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.UUID

class NoteRepository(
    private val noteDao: NoteDao,
    private val apiService: NoteApiService,
    private val attachmentApiService: AttachmentApiService
) {

    fun getAllActiveNotes(): Flow<List<Note>> {
        return noteDao.getAllActiveNotes().map { entities ->
            entities.map { NoteMapper.toDomain(it) }
        }
    }

    fun getFavoriteNotes(): Flow<List<Note>> {
        return noteDao.getFavoriteNotes().map { entities ->
            entities.map { NoteMapper.toDomain(it) }
        }
    }

    fun getArchivedNotes(): Flow<List<Note>> {
        return noteDao.getArchivedNotes().map { entities ->
            entities.map { NoteMapper.toDomain(it) }
        }
    }

    fun getDeletedNotes(): Flow<List<Note>> {
        return noteDao.getDeletedNotes().map { entities ->
            entities.map { NoteMapper.toDomain(it) }
        }
    }

    fun searchNotes(query: String): Flow<List<Note>> {
        return noteDao.searchNotes(query).map { entities ->
            entities.map { NoteMapper.toDomain(it) }
        }
    }

    suspend fun getNoteById(id: String): Note? {
        return noteDao.getNoteById(id)?.let { NoteMapper.toDomain(it) }
    }

    suspend fun createNote(
        title: String,
        content: String,
        folder: String = "default",
        tags: List<String> = emptyList(),
        priority: String = "medium",
        subfolder: String? = null,
        reminder: String? = null
    ): Note {
        val localId = UUID.randomUUID().toString()
        val note = Note(
            id = localId,
            title = title,
            content = content,
            tags = tags,
            folder = folder,
            subfolder = subfolder?.trim()?.takeIf { it.isNotBlank() },
            priority = priority,
            reminder = reminder,
            syncStatus = SyncStatus.PENDING_CREATE
        )
        noteDao.insertNote(NoteMapper.toEntity(note))
        return note
    }

    suspend fun folderPaths(): List<String> {
        return noteDao.listFolderRows()
            .map { NoteFolders.path(it.folder, it.subfolder) }
            .filter { it.isNotBlank() && it != "default" }
            .distinct()
            .sorted()
    }

    suspend fun updateNote(note: Note) {
        val entity = NoteMapper.toEntity(note.copy(syncStatus = SyncStatus.PENDING_UPDATE))
        noteDao.updateNote(entity)
    }

    suspend fun deleteNote(note: Note, permanent: Boolean = false) {
        if (permanent) {
            runCatching { apiService.deleteNote(note.id, permanent = true) }
        }
        if (permanent || note.syncStatus == SyncStatus.PENDING_CREATE) {
            if (note.localId != 0L) {
                noteDao.deleteNoteByLocalId(note.localId)
            } else {
                val entity = noteDao.getNoteById(note.id)
                entity?.let { noteDao.deleteNoteByLocalId(it.localId) }
            }
        } else {
            val entity = NoteMapper.toEntity(
                note.copy(
                    isDeleted = true,
                    syncStatus = SyncStatus.PENDING_DELETE
                )
            )
            noteDao.updateNote(entity)
        }
    }

    suspend fun archiveNote(note: Note) {
        persistArchiveFlag(note, archived = true)
    }

    suspend fun restoreNote(note: Note) {
        persistArchiveFlag(note, archived = false)
    }

    private suspend fun persistArchiveFlag(note: Note, archived: Boolean) {
        val local = note.copy(
            isArchived = archived,
            isDeleted = if (archived) note.isDeleted else false,
            syncStatus = SyncStatus.PENDING_UPDATE
        )
        noteDao.updateNote(NoteMapper.toEntity(local))
        val response = runCatching {
            if (archived) apiService.archiveNote(note.id) else apiService.restoreNote(note.id)
        }.getOrNull()
        if (response?.isSuccessful == true) {
            val synced = (response.body()?.let { NoteMapper.fromResponse(it) } ?: local).copy(
                localId = note.localId,
                syncStatus = SyncStatus.SYNCED
            )
            noteDao.updateNote(NoteMapper.toEntity(synced))
        }
    }

    suspend fun shareNote(note: Note, userId: String): String {
        return try {
            val response = apiService.shareNote(note.id, userId)
            if (response.isSuccessful) {
                response.body()?.let { serverNote ->
                    val domainNote = NoteMapper.fromResponse(serverNote)
                    noteDao.updateNote(
                        NoteMapper.toEntity(
                            domainNote.copy(
                                syncStatus = SyncStatus.SYNCED,
                                localId = note.localId
                            )
                        )
                    )
                }
                "Shared successfully"
            } else {
                "Share failed: ${response.code()}"
            }
        } catch (e: Exception) {
            "Share error: ${e.message}"
        }
    }

    suspend fun getSharedWithMe(): List<Note> {
        return try {
            val response = apiService.getSharedWithMe()
            if (response.isSuccessful) {
                response.body()?.map { NoteMapper.fromResponse(it) } ?: emptyList()
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun uploadAttachment(
        parentId: String,
        filePath: String,
        parentType: String
    ): Attachment? {
        return try {
            val file = File(filePath)
            val requestFile = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
            val parentIdBody = parentId.toRequestBody("text/plain".toMediaTypeOrNull())
            val parentTypeBody = parentType.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = attachmentApiService.uploadAttachment(body, parentIdBody, parentTypeBody)
            if (response.isSuccessful) {
                response.body()
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteAttachment(attachmentId: String): Boolean {
        return try {
            val response = attachmentApiService.deleteAttachment(attachmentId)
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun downloadAttachment(attachmentId: String, dest: File): File? {
        return try {
            val response = attachmentApiService.downloadAttachment(attachmentId)
            val body = response.body() ?: return null
            dest.outputStream().use { out -> body.byteStream().copyTo(out) }
            dest
        } catch (e: Exception) {
            null
        }
    }

    suspend fun history(id: String) = runCatching {
        apiService.getNoteHistory(id).body().orEmpty()
    }.getOrElse { emptyList() }

    suspend fun restoreVersion(note: Note, historyId: String): Note? {
        return try {
            val response = apiService.restoreVersion(note.id, historyId)
            val body = response.body() ?: return null
            val mapped = NoteMapper.fromResponse(body).copy(
                localId = note.localId,
                syncStatus = SyncStatus.SYNCED
            )
            noteDao.updateNote(NoteMapper.toEntity(mapped))
            mapped
        } catch (e: Exception) {
            null
        }
    }

    suspend fun importNote(file: File, folder: String?, subfolder: String?): Note? {
        return try {
            val part = MultipartBody.Part.createFormData(
                "file",
                file.name,
                file.asRequestBody("text/markdown".toMediaTypeOrNull())
            )
            val folderBody = folder?.toRequestBody("text/plain".toMediaTypeOrNull())
            val subBody = subfolder?.toRequestBody("text/plain".toMediaTypeOrNull())
            val response = apiService.importNote(part, folderBody, subBody)
            response.body()?.let { NoteMapper.fromResponse(it) }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun syncNotes(): Boolean {
        return try {
            // 1. Push pending creates
            val pendingCreates = noteDao.getPendingSyncNotes()
                .filter { it.syncStatus == SyncStatus.PENDING_CREATE.name }
            for (entity in pendingCreates) {
                val note = NoteMapper.toDomain(entity)
                val input = NoteMapper.toInput(note)
                val response = apiService.createNote(input)
                if (response.isSuccessful) {
                    response.body()?.let { serverNote ->
                        noteDao.updateServerId(entity.localId, serverNote.id)
                    }
                }
            }

            // 2. Push pending updates
            val pendingUpdates = noteDao.getPendingSyncNotes()
                .filter { it.syncStatus == SyncStatus.PENDING_UPDATE.name }
            for (entity in pendingUpdates) {
                val note = NoteMapper.toDomain(entity)
                val response = if (note.isArchived) {
                    apiService.archiveNote(note.id)
                } else {
                    val restored = runCatching { apiService.restoreNote(note.id) }.getOrNull()
                    if (restored != null && !restored.isSuccessful) restored
                    else apiService.updateNote(note.id, NoteMapper.toInput(note))
                }
                if (response.isSuccessful) {
                    noteDao.markAsSynced(entity.localId)
                }
            }

            // 3. Push pending deletes
            val pendingDeletes = noteDao.getPendingDeleteNotes()
            for (entity in pendingDeletes) {
                val response = apiService.deleteNote(entity.id, permanent = false)
                if (response.isSuccessful) {
                    noteDao.deleteNoteByLocalId(entity.localId)
                }
            }

            // 4. Pull from server
            val response = apiService.getNotes()
            if (response.isSuccessful) {
                response.body()?.let { serverNotes ->
                    for (serverNote in serverNotes) {
                        val domainNote = NoteMapper.fromResponse(serverNote)
                        val existing = noteDao.getNoteById(domainNote.id)
                        if (existing == null) {
                            noteDao.insertNote(NoteMapper.toEntity(domainNote.copy(syncStatus = SyncStatus.SYNCED)))
                        } else if (existing.syncStatus == SyncStatus.SYNCED.name) {
                            noteDao.updateNote(
                                NoteMapper.toEntity(
                                    domainNote.copy(
                                        syncStatus = SyncStatus.SYNCED,
                                        localId = existing.localId
                                    )
                                )
                            )
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun refreshNotes(): Boolean {
        return try {
            val response = apiService.getNotes()
            if (response.isSuccessful) {
                response.body()?.let { serverNotes ->
                    for (serverNote in serverNotes) {
                        val domainNote = NoteMapper.fromResponse(serverNote)
                        val existing = noteDao.getNoteById(domainNote.id)
                        if (existing == null) {
                            noteDao.insertNote(NoteMapper.toEntity(domainNote.copy(syncStatus = SyncStatus.SYNCED)))
                        } else if (existing.syncStatus == SyncStatus.SYNCED.name) {
                            noteDao.updateNote(
                                NoteMapper.toEntity(
                                    domainNote.copy(
                                        syncStatus = SyncStatus.SYNCED,
                                        localId = existing.localId
                                    )
                                )
                            )
                        }
                    }
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}
