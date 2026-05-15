package xyz.crearts.notekeeper.data.model

data class AuthRequest(
    val email: String,
    val password: String,
    val name: String? = null
)

data class AuthResponse(
    val token: String,
    val user: User? = null
)

data class User(
    val id: String,
    val email: String,
    val name: String? = null,
    val picture: String? = null
)
