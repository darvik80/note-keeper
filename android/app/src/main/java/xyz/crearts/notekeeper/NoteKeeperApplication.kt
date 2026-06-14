package xyz.crearts.notekeeper

import android.app.Application
import xyz.crearts.notekeeper.data.notification.NotificationHelper
import xyz.crearts.notekeeper.data.sync.SyncWorker

class NoteKeeperApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
        SyncWorker.schedule(this)
    }
}
