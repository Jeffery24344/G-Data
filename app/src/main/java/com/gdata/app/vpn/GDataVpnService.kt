package com.gdata.app.vpn

import android.content.Intent
import android.net.VpnService

/**
 * Stub local VPN service.
 * Full implementation (non-decrypting pass-through + notification) can be expanded later.
 * Declared in the manifest so the project builds cleanly.
 */
class GDataVpnService : VpnService() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Intentionally minimal for first buildable release
        return START_NOT_STICKY
    }

    companion object {
        const val ACTION_START = "com.gdata.app.vpn.START"
        const val ACTION_STOP = "com.gdata.app.vpn.STOP"
    }
}
