package dev.cogitor.notekeeper

import android.app.Application
import dev.cogitor.notekeeper.data.notification.NotificationHelper
import dev.cogitor.notekeeper.data.sync.SyncWorker

class NoteKeeperApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
        SyncWorker.schedule(this)
    }
}
