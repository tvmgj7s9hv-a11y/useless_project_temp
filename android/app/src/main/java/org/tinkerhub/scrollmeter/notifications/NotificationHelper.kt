package org.tinkerhub.scrollmeter.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.tinkerhub.scrollmeter.core.Milestone
import org.tinkerhub.scrollmeter.ui.MainActivity

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_MILESTONES_ID = "channel_milestones"
        const val CHANNEL_USAGE_ID = "channel_usage_reminders"

        const val NOTIFICATION_ID_MILESTONE = 1001
        const val NOTIFICATION_ID_USAGE = 1002
    }

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val milestoneChannel = NotificationChannel(
                CHANNEL_MILESTONES_ID,
                "Milestones & Landmarks",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Humorous milestone alerts when reaching famous distances on Instagram"
                enableVibration(true)
            }

            val usageChannel = NotificationChannel(
                CHANNEL_USAGE_ID,
                "Usage & Distance Reminders",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Periodic reminders about scrolling distance and duration"
            }

            notificationManager.createNotificationChannel(milestoneChannel)
            notificationManager.createNotificationChannel(usageChannel)
        }
    }

    fun showMilestoneNotification(milestone: Milestone) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_MILESTONES_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("${milestone.emoji} Milestone: ${milestone.title}")
            .setContentText(milestone.description)
            .setStyle(NotificationCompat.BigTextStyle().bigText(milestone.description))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_MILESTONE, notification)
        } catch (e: SecurityException) {
            // Android 13+ POST_NOTIFICATIONS permission not yet granted by user
        }
    }

    fun showUsageReminder(formattedDistance: String, wittyComment: String) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_USAGE_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("SAHAL'S SCROLL: $formattedDistance scrolled")
            .setContentText(wittyComment)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_USAGE, notification)
        } catch (e: SecurityException) {
            // Permission catch
        }
    }
}
