package com.tanjim.guardianlock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val enabled = SecurePrefs.get(context).getBoolean(SecurePrefs.KEY_ENABLED, false)
        if (enabled) {
            ContextCompat.startForegroundService(context, Intent(context, GuardianService::class.java))
        }
    }
}
