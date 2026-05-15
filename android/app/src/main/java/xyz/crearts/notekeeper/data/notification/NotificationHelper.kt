package xyz.crearts.notekeeper.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationHelper {

    const val CHANNEL_REMINDERS = "reminders"
    const val CHANNEL_DUE_SOON = "due_soon"
    const val CHANNEL_OVERDUE = "overdue"

    data class ChannelInfo(
        val id: String,
        val name: String,
        val description: String,
        val importance: Int
    )

    val channels = listOf(
        ChannelInfo(
            id = CHANNEL_REMINDERS,
            name = "Reminders",
            description = "Scheduled reminder notifications for todos",
            importance = NotificationManager.IMPORTANCE_HIGH
        ),
        ChannelInfo(
            id = CHANNEL_DUE_SOON,
            name = "Due Soon",
            description = "Notifications for todos approaching their due date",
            importance = NotificationManager.IMPORTANCE_DEFAULT
        ),
        ChannelInfo(
            id = CHANNEL_OVERDUE,
            name = "Overdue",
            description = "Notifications for overdue todos",
            importance = NotificationManager.IMPORTANCE_HIGH
        )
    )

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            channels.forEach { info ->
                val channel = NotificationChannel(info.id, info.name, info.importance).apply {
                    description = info.description
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun getChannelName(channelId: String): String {
        return channels.find { it.id == channelId }?.name ?: "Reminders"
    }
}
