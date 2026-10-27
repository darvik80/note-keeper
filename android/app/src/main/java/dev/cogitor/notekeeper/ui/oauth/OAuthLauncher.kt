package dev.cogitor.notekeeper.ui.oauth

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import java.security.SecureRandom

object OAuthLauncher {
    const val REDIRECT = "notekeeper://oauth"

    @Volatile
    private var pendingState: String? = null
    @Volatile
    private var pendingAt: Long = 0
    private val random = SecureRandom()

    private val CHROME_PACKAGES = listOf(
        "com.android.chrome",
        "com.chrome.beta",
        "com.chrome.dev",
        "com.google.android.apps.chrome"
    )

    fun start(context: Context, apiBaseUrl: String, provider: String) {
        val state = newState()
        pendingState = state
        pendingAt = System.currentTimeMillis()
        val api = apiBaseUrl.trimEnd('/')
        val url = "$api/auth/oauth/$provider?redirect=${Uri.encode(REDIRECT)}&state=${Uri.encode(state)}"
        val tabs = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
            .build()
        // Edge Custom Tabs intercepts notekeeper:// as "open Edge". Prefer Chrome.
        val chrome = CustomTabsClient.getPackageName(context, CHROME_PACKAGES, true)
        if (chrome != null) {
            tabs.intent.setPackage(chrome)
        }
        tabs.launchUrl(context, Uri.parse(url))
    }

    fun takePendingState(state: String?): Boolean {
        val expected = pendingState
        val started = pendingAt
        pendingState = null
        pendingAt = 0
        if (expected.isNullOrBlank() || state.isNullOrBlank()) return false
        if (System.currentTimeMillis() - started > 10 * 60 * 1000) return false
        return expected == state
    }

    private fun newState(): String {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
}
