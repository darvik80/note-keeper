package xyz.crearts.notekeeper.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings_prefs")

class SettingsDataStore(private val context: Context) {
    private val SERVER_URL_KEY = stringPreferencesKey("server_url")
    private val NOTIFICATION_CHANNEL_KEY = stringPreferencesKey("default_notification_channel")

    val serverUrl: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[SERVER_URL_KEY] ?: DEFAULT_URL
    }

    val defaultNotificationChannel: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[NOTIFICATION_CHANNEL_KEY] ?: DEFAULT_CHANNEL
    }

    suspend fun saveServerUrl(url: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[SERVER_URL_KEY] = url
        }
    }

    suspend fun saveDefaultNotificationChannel(channelId: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[NOTIFICATION_CHANNEL_KEY] = channelId
        }
    }

    companion object {
        const val DEFAULT_URL = "https://note.darvik.synology.me:8443/api/v1/"
        const val DEFAULT_CHANNEL = "reminders"
    }
}
