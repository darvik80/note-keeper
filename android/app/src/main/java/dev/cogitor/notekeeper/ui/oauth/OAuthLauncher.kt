package dev.cogitor.notekeeper.ui.oauth

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

object OAuthLauncher {
    const val REDIRECT = "notekeeper://oauth"

    fun start(context: Context, apiBaseUrl: String, provider: String) {
        val api = apiBaseUrl.trimEnd('/')
        val url = "$api/auth/oauth/$provider?redirect=${Uri.encode(REDIRECT)}"
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
            .launchUrl(context, Uri.parse(url))
    }
}
