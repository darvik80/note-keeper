package dev.cogitor.notekeeper.data.model

data class AppConfig(
    val status: String? = null,
    val oauth: OAuthConfig? = null
)

data class OAuthConfig(
    val google: Boolean = false,
    val yandex: Boolean = false
)
