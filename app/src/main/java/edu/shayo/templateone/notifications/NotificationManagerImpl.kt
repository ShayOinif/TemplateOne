package edu.shayo.templateone.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import edu.shayo.templateone.R
import javax.inject.Inject

class NotificationManagerImpl @Inject constructor(
    @ApplicationContext private val appContext: Context
) : NotificationManager {

    private val systemNotificationManager =
        appContext.getSystemService(android.app.NotificationManager::class.java) as android.app.NotificationManager

    override fun generateNotification(
        mainNotificationText: CharSequence,
        titleText: CharSequence,
        actions: List<NotificationCompat.Action>,
        notificationChannelId: String,
    ): Notification {

        if (needNotificationChannel())
            createNotificationChannel(
                titleText,
                notificationChannelId,
            )

        val bigTextStyle = setStyle(
            mainNotificationText,
            titleText,
        )

        val notificationCompatBuilder =
            NotificationCompat.Builder(appContext, notificationChannelId)

        return notificationCompatBuilder.apply {
            setStyle(bigTextStyle)
            setContentTitle(titleText)
            setContentText(mainNotificationText)
            setSmallIcon(R.mipmap.ic_launcher)
            setDefaults(NotificationCompat.DEFAULT_ALL)
            setOngoing(true)
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

            actions.forEach {
                addAction(it)
            }
        }.build()
    }

    override fun notify(
        notificationId: Int,
        notification: Notification,
    ) =
        systemNotificationManager.notify(
            notificationId,
            notification
        )

    private fun createNotificationChannel(
        titleText: CharSequence,
        notificationChannelId: String,
    ) =
        systemNotificationManager.createNotificationChannel(
            NotificationChannel(
                notificationChannelId,
                titleText,
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            )
        )
}

private fun setStyle(
    mainNotificationText: CharSequence,
    titleText: CharSequence,
) =
    NotificationCompat.BigTextStyle()
        .bigText(mainNotificationText)
        .setBigContentTitle(titleText)

private fun needNotificationChannel() =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O