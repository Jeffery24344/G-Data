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
 * Optional local VPN.
 *
 * - Does NOT decrypt HTTPS
 * - Does NOT send traffic to external servers
 * - Establishes a local tunnel interface for policy / future optimization hooks
 * - Does not claim free data or speed boosts
 *
 * Default route is intentionally NOT forced for all traffic in this version so
 * normal internet keeps working while the VPN permission/session is active.
 * Full packet-level optimization can be added later on top of this service.
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
                .setSession("G Data")
                .setMtu(1500)
                .addAddress("10.10.10.2", 32)
                .addDnsServer("1.1.1.1")
                .addDnsServer("8.8.8.8")

            // Keep internet working: do not capture 0.0.0.0/0 yet.
            // Session still shows as VPN active for the user-approved local service.
            builder.addRoute("10.10.10.0", 24)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setMetered(false)
            }

            tunInterface = builder.establish()
            if (tunInterface == null) {
                stopSelf()
                return
            }

            running.set(true)
            isRunning = true

            readerThread = Thread({
                val input = FileInputStream(tunInterface!!.fileDescriptor)
                val buffer = ByteArray(32767)
                try {
                    while (running.get()) {
                        val length = input.read(buffer)
                        if (length <= 0) {
                            Thread.sleep(50)
                        }
                        // Pass-through expansion point: inspect / count / shape packets here.
                        // No decryption. No upload of payload to servers.
                    }
                } catch (_: Exception) {
                    // Interface closed
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
            .setContentTitle("G Data local VPN")
            .setContentText("Active · no decryption · traffic stays on device")
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
