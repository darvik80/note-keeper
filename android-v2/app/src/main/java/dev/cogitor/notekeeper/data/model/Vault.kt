package dev.cogitor.notekeeper.data.model

data class VaultFolder(
    val id: String,
    val parentId: String? = null,
    val ownerId: String? = null,
    val name: String,
    val updatedAt: String? = null
)

data class VaultItem(
    val id: String,
    val folderId: String? = null,
    val ownerId: String? = null,
    val type: String = "password",
    val title: String,
    val username: String? = null,
    val host: String? = null,
    val url: String? = null,
    val tags: List<String>? = emptyList(),
    val revision: Int = 1,
    val updatedAt: String? = null,
    val deletedAt: String? = null,
    val password: String? = null,
    val privateKey: String? = null,
    val secret: String? = null,
    val extraJson: String? = null
)

data class VaultFolderInput(
    val name: String,
    val parentId: String? = null
)

data class VaultItemInput(
    val id: String? = null,
    val title: String,
    val type: String? = "password",
    val folderId: String? = null,
    val username: String? = null,
    val host: String? = null,
    val url: String? = null,
    val tags: List<String>? = emptyList(),
    val password: String? = null,
    val privateKey: String? = null,
    val secret: String? = null,
    val extraJson: String? = null
)
