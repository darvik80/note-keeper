package xyz.crearts.notekeeper.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import xyz.crearts.notekeeper.data.local.entity.TodoEntity
import xyz.crearts.notekeeper.data.model.Attachment
import xyz.crearts.notekeeper.data.model.AttachmentInput
import xyz.crearts.notekeeper.data.model.LocationInput
import xyz.crearts.notekeeper.data.model.ScheduleInput
import xyz.crearts.notekeeper.data.model.SyncStatus
import xyz.crearts.notekeeper.data.model.Todo
import xyz.crearts.notekeeper.data.model.TodoInput
import xyz.crearts.notekeeper.data.model.TodoResponse

object TodoMapper {
    private val gson = Gson()

    fun toDomain(entity: TodoEntity): Todo {
        return Todo(
            localId = entity.localId,
            id = entity.id,
            title = entity.title,
            description = entity.description,
            completed = entity.completed,
            tags = parseTags(entity.tags),
            priority = entity.priority,
            isFavorite = entity.isFavorite,
            isArchived = entity.isArchived,
            isDeleted = entity.isDeleted,
            deletedAt = entity.deletedAt,
            dueDate = entity.dueDate,
            reminder = entity.reminder,
            notifiedAt = entity.notifiedAt,
            notificationChannels = entity.notificationChannels,
            locationLat = entity.locationLat,
            locationLng = entity.locationLng,
            locationAddress = entity.locationAddress,
            scheduleRepeat = entity.scheduleRepeat,
            scheduleEndDate = entity.scheduleEndDate,
            ownerId = entity.ownerId,
            sharedWith = entity.sharedWith,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            attachments = parseAttachments(entity.attachments),
            syncStatus = SyncStatus.valueOf(entity.syncStatus)
        )
    }

    fun toEntity(todo: Todo): TodoEntity {
        return TodoEntity(
            localId = todo.localId,
            id = todo.id,
            title = todo.title,
            description = todo.description ?: "",
            completed = todo.completed,
            tags = gson.toJson(todo.tags ?: emptyList<String>()),
            priority = todo.priority ?: "medium",
            isFavorite = todo.isFavorite,
            isArchived = todo.isArchived,
            isDeleted = todo.isDeleted,
            deletedAt = todo.deletedAt,
            dueDate = todo.dueDate,
            reminder = todo.reminder,
            notifiedAt = todo.notifiedAt,
            notificationChannels = todo.notificationChannels,
            locationLat = todo.locationLat,
            locationLng = todo.locationLng,
            locationAddress = todo.locationAddress,
            scheduleRepeat = todo.scheduleRepeat ?: "none",
            scheduleEndDate = todo.scheduleEndDate,
            ownerId = todo.ownerId,
            sharedWith = todo.sharedWith,
            createdAt = todo.createdAt,
            updatedAt = todo.updatedAt,
            attachments = gson.toJson(todo.attachments ?: emptyList<Attachment>()),
            syncStatus = todo.syncStatus.name
        )
    }

    fun toInput(todo: Todo): TodoInput {
        val locationInput = if (todo.locationLat != null || todo.locationLng != null || todo.locationAddress != null) {
            LocationInput(lat = todo.locationLat, lng = todo.locationLng, address = todo.locationAddress)
        } else null

        return TodoInput(
            title = todo.title,
            description = todo.description,
            tags = todo.tags,
            priority = todo.priority,
            isFavorite = todo.isFavorite,
            completed = todo.completed,
            dueDate = todo.dueDate,
            reminder = todo.reminder,
            notificationChannels = todo.notificationChannels,
            location = locationInput,
            schedule = if ((todo.scheduleRepeat ?: "none") != "none") {
                ScheduleInput(repeat = todo.scheduleRepeat ?: "none", endDate = todo.scheduleEndDate)
            } else null,
            attachments = todo.attachments?.map { att ->
                AttachmentInput(
                    id = att.id,
                    name = att.name,
                    url = att.url,
                    size = att.size,
                    mimeType = att.type,
                    uploadedAt = att.uploadedAt
                )
            }
        )
    }

    fun fromResponse(response: TodoResponse, syncStatus: SyncStatus = SyncStatus.SYNCED): Todo {
        return Todo(
            id = response.id,
            title = response.title,
            description = response.description,
            completed = response.completed,
            tags = response.tags,
            priority = response.priority,
            isFavorite = response.isFavorite,
            isArchived = response.isArchived,
            isDeleted = response.isDeleted,
            deletedAt = response.deletedAt,
            dueDate = response.dueDate,
            reminder = response.reminder,
            notifiedAt = response.notifiedAt,
            notificationChannels = response.notificationChannels,
            locationLat = response.location?.lat,
            locationLng = response.location?.lng,
            locationAddress = response.location?.address,
            scheduleRepeat = response.schedule?.repeat ?: "none",
            scheduleEndDate = response.schedule?.endDate,
            ownerId = response.ownerId,
            sharedWith = response.sharedWith,
            createdAt = response.createdAt,
            updatedAt = response.updatedAt,
            attachments = response.attachments,
            syncStatus = syncStatus
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
