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
import java.io.FileInputStream
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Local VPN session that stays connected WITHOUT hijacking all app traffic.
 *
 * Why: a full userspace TCP stack is required to route 0.0.0.0/0 safely.
 * Routing everything without a complete return path breaks other apps.
 *
 * This service:
 * - Uses real Android VpnService (status bar key, Settings → VPN)
 * - Does NOT decrypt traffic
 * - Does NOT send traffic to servers
 * - Does NOT capture the default route, so Chrome/WhatsApp/etc keep working
 *
 * Future: optional advanced capture mode can be added behind a clear warning.
 */
class GDataVpnService : VpnService() {

    private var tunInterface: ParcelFileDescriptor? = null
    private val running = AtomicBoolean(false)
    private var readerThread: Thread? = null

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
                .setSession("G Data Local VPN")
                .setMtu(1500)
                .addAddress("10.10.10.2", 32)
                // Local subnet only — do NOT addRoute(0.0.0.0, 0)
                // so other apps keep normal internet access.
                .addRoute("10.10.10.0", 24)
                .addDnsServer("1.1.1.1")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setMetered(false)
            }

            try {
                builder.addDisallowedApplication(packageName)
            } catch (_: Exception) {
            }

            tunInterface = builder.establish()
            if (tunInterface == null) {
                stopSelf()
                return
            }

            running.set(true)
            isRunning = true

            // Drain any packets on the local interface so the FD stays healthy
            readerThread = Thread({
                val input = FileInputStream(tunInterface!!.fileDescriptor)
                val buffer = ByteArray(32767)
                try {
                    while (running.get()) {
                        val n = input.read(buffer)
                        if (n <= 0) Thread.sleep(100)
                    }
                } catch (_: Exception) {
                }
            }, "GDataVpnReader").also { it.start() }
        } catch (_: Exception) {
            stopVpn()
            stopSelf()
        }
    }

    private fun stopVpn() {
        running.set(false)
        isRunning = false
        try {
            readerThread?.interrupt()
            readerThread = null
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
            .setContentText("Connected · apps keep normal internet · no decryption")
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
