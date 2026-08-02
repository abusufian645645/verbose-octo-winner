package com.tanjim.guardianlock

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.telephony.SmsManager
import androidx.core.content.ContextCompat

object AlertSender {

    @SuppressLint("MissingPermission")
    fun sendAlert(context: Context, message: String) {
        val prefs = SecurePrefs.get(context)
        val contact = prefs.getString(SecurePrefs.KEY_CONTACT, null) ?: return

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val fullMessage = buildString {
            append(message)
            append(lastKnownLocationText(context))
        }

        try {
            val smsManager = context.getSystemService(SmsManager::class.java)
            val parts = smsManager.divideMessage(fullMessage)
            smsManager.sendMultipartTextMessage(contact, null, parts, null, null)
        } catch (_: Exception) {
        }
    }

    @SuppressLint("MissingPermission")
    private fun lastKnownLocationText(context: Context): String {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return ""

        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = lm.getProviders(true)
            for (provider in providers) {
                val loc = lm.getLastKnownLocation(provider) ?: continue
                return " Approx. location: https://maps.google.com/?q=${loc.latitude},${loc.longitude}"
            }
            ""
        } catch (_: Exception) {
            ""
        }
    }
}
