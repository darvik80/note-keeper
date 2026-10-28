package dev.cogitor.notekeeper.data.model

import com.google.gson.annotations.SerializedName

/**
 * Mirrors the server's Todo JSON structure (nested location/schedule).
 * Used for Retrofit deserialization, then converted to flat Todo domain model.
 */
data class TodoResponse(
    val id: String,
    val title: String,
    val description: String? = null,
    val completed: Boolean = false,
    val tags: List<String>? = emptyList(),
    val priority: String? = "medium",
    @SerializedName(value = "isFavorite", alternate = ["favorite"]) val isFavorite: Boolean = false,
    @SerializedName(value = "isArchived", alternate = ["archived"]) val isArchived: Boolean = false,
    @SerializedName(value = "isDeleted", alternate = ["deleted"]) val isDeleted: Boolean = false,
    val deletedAt: String? = null,
    val dueDate: String? = null,
    val reminder: String? = null,
    val notifiedAt: String? = null,
    val notificationChannels: String? = null,
    val ownerId: String? = null,
    val sharedWith: String? = null,
    val location: LocationDto? = null,
    val schedule: ScheduleDto? = null,
    val attachments: List<Attachment>? = emptyList(),
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class LocationDto(
    val lat: Double? = null,
    val lng: Double? = null,
    val address: String? = null
)

data class ScheduleDto(
    val repeat: String? = null,
    val endDate: String? = null
)

data class NoteResponse(
    val id: String,
    val title: String,
    val content: String? = "",
    val tags: List<String>? = emptyList(),
    val folder: String? = "default",
    val subfolder: String? = null,
    val priority: String? = "medium",
    @SerializedName(value = "isFavorite", alternate = ["favorite"]) val isFavorite: Boolean = false,
    @SerializedName(value = "isEncrypted", alternate = ["encrypted"]) val isEncrypted: Boolean = false,
    @SerializedName(value = "isArchived", alternate = ["archived"]) val isArchived: Boolean = false,
    @SerializedName(value = "isDeleted", alternate = ["deleted"]) val isDeleted: Boolean = false,
    val deletedAt: String? = null,
    val reminder: String? = null,
    val templateId: String? = null,
    val ownerId: String? = null,
    val sharedWith: String? = null,
    val attachments: List<Attachment>? = emptyList(),
    val createdAt: String? = null,
    val updatedAt: String? = null
)

