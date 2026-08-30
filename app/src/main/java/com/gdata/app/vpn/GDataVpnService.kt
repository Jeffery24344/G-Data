package com.gdata.app.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.gdata.app.MainActivity
import com.gdata.app.R
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Minimal local VPN session.
 *
 * Critical design choice: do NOT add DNS servers and do NOT add public routes.
 * Adding DNS or 0.0.0.0/0 without a complete userspace stack breaks Chrome,
 * WhatsApp, and other apps (no working return path).
 *
 * This still uses real VpnService so Android can show the VPN key, while
 * leaving normal app networking on the device's default network.
 */
class GDataVpnService : VpnService() {

    private var tunInterface: ParcelFileDescriptor? = null
    private val running = AtomicBoolean(false)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopVpn()
                stopSelf()
                return START_NOT_STICKY
            }
            else -> startVpn()
        }
        return START_STICKY
    }

    private fun startVpn() {
        if (running.get()) return

        startForeground(NOTIFICATION_ID, buildNotification())

        try {
            val builder = Builder()
                .setSession("G Data")
                .setMtu(1500)
                // Link-local style address only
                .addAddress("10.255.255.2", 32)

            // Intentionally NO addDnsServer(...) — DNS hijack was killing apps.
            // Intentionally NO addRoute("0.0.0.0", 0) — full capture needs a real stack.
            // Optional tiny private route only (never public internet ranges).
            builder.addRoute("10.255.255.0", 24)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setMetered(false)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.allowFamily(android.system.OsConstants.AF_INET)
            }

            try {
                builder.addDisallowedApplication(packageName)
            } catch (_: Exception) {
            }

            // Prefer non-blocking so we never stall the system network path
            builder.setBlocking(false)

            tunInterface = builder.establish()
            if (tunInterface == null) {
                stopSelf()
                return
            }

            running.set(true)
            isRunning = true
        } catch (_: Exception) {
            stopVpn()
            stopSelf()
        }
    }

    private fun stopVpn() {
        running.set(false)
        isRunning = false
        try {
            tunInterface?.close()
        } catch (_: Exception) {
        }
        tunInterface = null
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    override fun onRevoke() {
        stopVpn()
        stopSelf()
        super.onRevoke()
    }

    private fun buildNotification(): Notification {
        val channelId = "gdata_vpn"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "Local VPN",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
        val pending = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("G Data VPN on")
            .setContentText("Session active · normal apps use system network · no decryption")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pending)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val ACTION_START = "com.gdata.app.vpn.START"
        const val ACTION_STOP = "com.gdata.app.vpn.STOP"
        const val NOTIFICATION_ID = 2001

        @Volatile
        var isRunning: Boolean = false
            private set
    }
}
