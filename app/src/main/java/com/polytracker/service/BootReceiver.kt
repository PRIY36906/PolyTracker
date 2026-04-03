package com.polytracker.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.polytracker.util.Prefs

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Only restart if a wallet was previously configured
            if (Prefs.getWallet(context) != null) {
                TrackingService.start(context)
            }
        }
    }
}
