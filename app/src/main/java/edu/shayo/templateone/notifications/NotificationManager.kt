package edu.shayo.templateone.notifications

import android.app.Notification
import androidx.core.app.NotificationCompat

interface NotificationManager {
    fun generateNotification(
        mainNotificationText: CharSequence,
        titleText: CharSequence,
        actions: List<NotificationCompat.Action>,
        notificationChannelId: String,
    ): Notification

    fun notify(
    notificationId: Int,
    notification: Notification,
    )
}