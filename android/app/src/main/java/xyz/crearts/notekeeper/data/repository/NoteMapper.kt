package xyz.crearts.notekeeper.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import xyz.crearts.notekeeper.data.local.entity.NoteEntity
import xyz.crearts.notekeeper.data.model.Attachment
import xyz.crearts.notekeeper.data.model.Note
import xyz.crearts.notekeeper.data.model.NoteInput
import xyz.crearts.notekeeper.data.model.SyncStatus

object NoteMapper {
    private val gson = Gson()

    fun toDomain(entity: NoteEntity): Note {
        return Note(
            localId = entity.localId,
            id = entity.id,
            title = entity.title,
            content = entity.content,
            tags = parseTags(entity.tags),
            folder = entity.folder,
            subfolder = entity.subfolder,
            priority = entity.priority,
            isFavorite = entity.isFavorite,
            isEncrypted = entity.isEncrypted,
            isArchived = entity.isArchived,
            isDeleted = entity.isDeleted,
            deletedAt = entity.deletedAt,
            reminder = entity.reminder,
            templateId = entity.templateId,
            ownerId = entity.ownerId,
            sharedWith = entity.sharedWith,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            attachments = parseAttachments(entity.attachments),
            syncStatus = SyncStatus.valueOf(entity.syncStatus)
        )
    }

    fun toEntity(note: Note): NoteEntity {
        return NoteEntity(
            localId = note.localId,
            id = note.id,
            title = note.title,
            content = note.content ?: "",
            tags = gson.toJson(note.tags ?: emptyList<String>()),
            folder = note.folder ?: "default",
            subfolder = note.subfolder,
            priority = note.priority ?: "medium",
            isFavorite = note.isFavorite,
            isEncrypted = note.isEncrypted,
            isArchived = note.isArchived,
            isDeleted = note.isDeleted,
            deletedAt = note.deletedAt,
            reminder = note.reminder,
            templateId = note.templateId,
            ownerId = note.ownerId,
            sharedWith = note.sharedWith,
            createdAt = note.createdAt,
            updatedAt = note.updatedAt,
            attachments = gson.toJson(note.attachments ?: emptyList<Attachment>()),
            syncStatus = note.syncStatus.name
        )
    }

    fun toInput(note: Note): NoteInput {
        return NoteInput(
            title = note.title,
            content = note.content,
            tags = note.tags,
            folder = note.folder,
            subfolder = note.subfolder,
            priority = note.priority,
            isFavorite = note.isFavorite,
            isEncrypted = note.isEncrypted,
            reminder = note.reminder,
            templateId = note.templateId
        )
    }

    private fun parseTags(tagsJson: String): List<String> {
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson<List<String>>(tagsJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseAttachments(attachmentsJson: String): List<Attachment> {
        return try {
            val type = object : TypeToken<List<Attachment>>() {}.type
            gson.fromJson<List<Attachment>>(attachmentsJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
