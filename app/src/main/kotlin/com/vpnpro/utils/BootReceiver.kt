package com.vpnpro.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Placeholder — extend to auto-reconnect on boot if desired */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Optionally reconnect to last server on boot
        }
    }
}
