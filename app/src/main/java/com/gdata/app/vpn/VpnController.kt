package com.gdata.app.vpn

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VpnController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun isRunning(): Boolean = GDataVpnService.isRunning

    /** Returns prepare Intent if user must approve VPN; null if already approved. */
    fun prepareIntent(activity: Activity): Intent? = VpnService.prepare(activity)

    fun start() {
        val intent = Intent(context, GDataVpnService::class.java).apply {
            action = GDataVpnService.ACTION_START
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun stop() {
        val intent = Intent(context, GDataVpnService::class.java).apply {
            action = GDataVpnService.ACTION_STOP
        }
        context.startService(intent)
    }
}
