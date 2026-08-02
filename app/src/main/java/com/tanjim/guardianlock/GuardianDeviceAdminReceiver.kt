package com.tanjim.guardianlock

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

class GuardianDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onPasswordFailed(context: Context, intent: Intent) {
        super.onPasswordFailed(context, intent)

        val prefs = SecurePrefs.get(context)
        if (!prefs.getBoolean(SecurePrefs.KEY_ENABLED, false)) return

        val attempts = prefs.getInt(SecurePrefs.KEY_FAILED_ATTEMPTS, 0) + 1
        prefs.edit().putInt(SecurePrefs.KEY_FAILED_ATTEMPTS, attempts).apply()

        AlertSender.sendAlert(
            context,
            "GuardianLock: $attempts failed unlock attempt(s) detected on this device."
        )
    }

    override fun onPasswordSucceeded(context: Context, intent: Intent) {
        super.onPasswordSucceeded(context, intent)
        SecurePrefs.get(context).edit().putInt(SecurePrefs.KEY_FAILED_ATTEMPTS, 0).apply()
    }
}
