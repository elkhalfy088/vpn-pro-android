package akh.vpn.dd.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Auto-connect on boot can be implemented here
            // For now, just log; user can enable from settings
        }
    }
}
