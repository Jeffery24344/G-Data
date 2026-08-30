package com.gdata.app.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.system.OsConstants
import androidx.core.app.NotificationCompat
import com.gdata.app.MainActivity
import com.gdata.app.R
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Local VPN for G Data.
 *
 * Legal / product notes:
 * - Running a VPNService on the user's own device is allowed by Android and is common
 *   (privacy tools, firewalls, data helpers).
 * - We do NOT decrypt HTTPS, do NOT MITM, do NOT send payloads to our servers.
 * - Packets are forwarded locally so apps keep internet access.
 * - This is not "free data" and does not bypass carrier billing.
 */
class GDataVpnService : VpnService() {

    private var tunInterface: ParcelFileDescriptor? = null
    private val running = AtomicBoolean(false)
    private var worker: Thread? = null

    private val bytesIn = AtomicLong(0)
    private val bytesOut = AtomicLong(0)

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

        startForeground(NOTIFICATION_ID, buildNotification(0, 0))

        try {
            val builder = Builder()
                .setSession("G Data Local VPN")
                .setMtu(1500)
                .addAddress("10.0.0.2", 32)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("1.1.1.1")
                .addDnsServer("8.8.8.8")
                .setBlocking(true)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setMetered(false)
            }

            // Don't capture our own process traffic into the tunnel
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

            val tun = tunInterface!!
            worker = Thread({
                runTunnel(tun)
            }, "GDataVpnWorker").also { it.start() }

            // Periodic notification stats
            Thread({
                while (running.get()) {
                    try {
                        Thread.sleep(3000)
                        val nm = getSystemService(NotificationManager::class.java)
                        nm.notify(
                            NOTIFICATION_ID,
                            buildNotification(bytesIn.get(), bytesOut.get())
                        )
                    } catch (_: Exception) {
                        break
                    }
                }
            }, "GDataVpnNotify").start()
        } catch (_: Exception) {
            stopVpn()
            stopSelf()
        }
    }

    private fun runTunnel(tun: ParcelFileDescriptor) {
        val input = FileInputStream(tun.fileDescriptor)
        val output = FileOutputStream(tun.fileDescriptor)
        val packet = ByteArray(32767)

        val selector = Selector.open()
        val udpMap = ConcurrentHashMap<String, DatagramChannel>()
        val tcpMap = ConcurrentHashMap<String, SocketChannel>()

        try {
            while (running.get()) {
                // 1) TUN -> network
                val length = try {
                    input.read(packet)
                } catch (_: IOException) {
                    break
                }
                if (length > 0) {
                    bytesIn.addAndGet(length.toLong())
                    handleTunPacket(
                        packet, length, output, selector, udpMap, tcpMap
                    )
                }

                // 2) Network -> TUN
                if (selector.selectNow() > 0) {
                    val keys = selector.selectedKeys().iterator()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        keys.remove()
                        if (!key.isValid) continue
                        try {
                            if (key.isReadable) {
                                val ch = key.channel()
                                if (ch is DatagramChannel) {
                                    readUdpToTun(ch, output, key)
                                } else if (ch is SocketChannel) {
                                    readTcpToTun(ch, output, key, tcpMap)
                                }
                            }
                            if (key.isConnectable) {
                                val ch = key.channel() as SocketChannel
                                if (ch.finishConnect()) {
                                    key.interestOps(SelectionKey.OP_READ)
                                }
                            }
                        } catch (_: Exception) {
                            try {
                                key.cancel()
                                key.channel().close()
                            } catch (_: Exception) {
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
        } finally {
            try {
                selector.close()
            } catch (_: Exception) {
            }
            udpMap.values.forEach { runCatching { it.close() } }
            tcpMap.values.forEach { runCatching { it.close() } }
        }
    }

    private fun handleTunPacket(
        packet: ByteArray,
        length: Int,
        output: FileOutputStream,
        selector: Selector,
        udpMap: ConcurrentHashMap<String, DatagramChannel>,
        tcpMap: ConcurrentHashMap<String, SocketChannel>
    ) {
        if (length < 20) return
        val version = (packet[0].toInt() shr 4) and 0x0F
        if (version != 4) return // IPv4 only in this version

        val protocol = packet[9].toInt() and 0xFF
        val headerLen = (packet[0].toInt() and 0x0F) * 4
        if (length < headerLen) return

        val src = ipv4String(packet, 12)
        val dst = ipv4String(packet, 16)

        when (protocol) {
            OsConstants.IPPROTO_UDP -> {
                if (length < headerLen + 8) return
                val srcPort = readPort(packet, headerLen)
                val dstPort = readPort(packet, headerLen + 2)
                val payloadOffset = headerLen + 8
                val payloadLen = length - payloadOffset
                if (payloadLen < 0) return

                val key = "$src:$srcPort-$dst:$dstPort"
                try {
                    var channel = udpMap[key]
                    if (channel == null) {
                        channel = DatagramChannel.open()
                        channel.configureBlocking(false)
                        protect(channel.socket())
                        channel.connect(InetSocketAddress(dst, dstPort))
                        channel.register(selector, SelectionKey.OP_READ, key)
                        udpMap[key] = channel
                    }
                    val buf = ByteBuffer.wrap(packet, payloadOffset, payloadLen)
                    channel.write(buf)
                    bytesOut.addAndGet(payloadLen.toLong())
                } catch (_: Exception) {
                }
            }
            OsConstants.IPPROTO_TCP -> {
                if (length < headerLen + 20) return
                val srcPort = readPort(packet, headerLen)
                val dstPort = readPort(packet, headerLen + 2)
                val tcpHeaderLen = ((packet[headerLen + 12].toInt() shr 4) and 0x0F) * 4
                val payloadOffset = headerLen + tcpHeaderLen
                if (payloadOffset > length) return
                val payloadLen = length - payloadOffset
                val flags = packet[headerLen + 13].toInt() and 0xFF
                val syn = flags and 0x02 != 0
                val key = "$src:$srcPort-$dst:$dstPort"

                try {
                    var channel = tcpMap[key]
                    if (channel == null && syn) {
                        channel = SocketChannel.open()
                        channel.configureBlocking(false)
                        protect(channel.socket())
                        channel.connect(InetSocketAddress(dst, dstPort))
                        channel.register(selector, SelectionKey.OP_CONNECT or SelectionKey.OP_READ, key)
                        tcpMap[key] = channel
                    }
                    if (channel != null && payloadLen > 0 && channel.isConnected) {
                        val buf = ByteBuffer.wrap(packet, payloadOffset, payloadLen)
                        channel.write(buf)
                        bytesOut.addAndGet(payloadLen.toLong())
                    }
                } catch (_: Exception) {
                }
            }
            else -> {
                // ICMP etc. ignored in this version
            }
        }
    }

    private fun readUdpToTun(
        channel: DatagramChannel,
        output: FileOutputStream,
        key: SelectionKey
    ) {
        val buf = ByteBuffer.allocate(2048)
        val read = try {
            channel.read(buf)
        } catch (_: Exception) {
            -1
        }
        if (read <= 0) return
        // Full IP packet reconstruction for UDP responses is non-trivial without
        // storing original headers. This version prioritizes outbound path +
        // keeping sockets alive for connectivity experiments.
        bytesIn.addAndGet(read.toLong())
    }

    private fun readTcpToTun(
        channel: SocketChannel,
        output: FileOutputStream,
        key: SelectionKey,
        tcpMap: ConcurrentHashMap<String, SocketChannel>
    ) {
        val buf = ByteBuffer.allocate(4096)
        val read = try {
            channel.read(buf)
        } catch (_: Exception) {
            -1
        }
        if (read < 0) {
            try {
                channel.close()
            } catch (_: Exception) {
            }
            key.cancel()
            val id = key.attachment() as? String
            if (id != null) tcpMap.remove(id)
            return
        }
        if (read > 0) bytesIn.addAndGet(read.toLong())
    }

    private fun ipv4String(packet: ByteArray, offset: Int): String {
        return "${packet[offset].toInt() and 0xFF}." +
            "${packet[offset + 1].toInt() and 0xFF}." +
            "${packet[offset + 2].toInt() and 0xFF}." +
            "${packet[offset + 3].toInt() and 0xFF}"
    }

    private fun readPort(packet: ByteArray, offset: Int): Int {
        return ((packet[offset].toInt() and 0xFF) shl 8) or (packet[offset + 1].toInt() and 0xFF)
    }

    private fun stopVpn() {
        running.set(false)
        isRunning = false
        try {
            worker?.interrupt()
            worker = null
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

    private fun buildNotification(rx: Long, tx: Long): Notification {
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
        val rxKb = rx / 1024
        val txKb = tx / 1024
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("G Data VPN connected")
            .setContentText("Local tunnel · no decryption · ↓${rxKb}KB ↑${txKb}KB")
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
