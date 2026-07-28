package com.vpnpro.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Receives BOOT_COMPLETED and restarts the VPN if auto-connect is enabled.
 * Full auto-connect implementation requires reading DataStore prefs; wired here for future use.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Boot completed — auto-connect hook (configure in Settings)")
            // TODO: read DataStore autoConnect pref and start VpnProService if enabled
        }
    }
}
