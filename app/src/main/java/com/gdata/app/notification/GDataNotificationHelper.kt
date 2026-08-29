package com.gdata.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.gdata.app.MainActivity
import com.gdata.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GDataNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_USAGE = "usage_alerts"
        const val CHANNEL_BUNDLE = "bundle"
        const val CHANNEL_SYSTEM = "system"
    }

    init {
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_USAGE, "Usage Alerts", NotificationManager.IMPORTANCE_DEFAULT)
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_BUNDLE, "Bundle", NotificationManager.IMPORTANCE_HIGH)
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_SYSTEM, "System", NotificationManager.IMPORTANCE_LOW)
        )
    }

    private fun pendingMain(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun notifyUsage(title: String, text: String) {
        val n = NotificationCompat.Builder(context, CHANNEL_USAGE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingMain())
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(1001, n)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted
        }
    }

    fun notifyBundle(title: String, text: String) {
        val n = NotificationCompat.Builder(context, CHANNEL_BUNDLE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingMain())
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(1002, n)
        } catch (_: SecurityException) {
        }
    }
}
