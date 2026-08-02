package com.tanjim.guardianlock

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlin.math.abs
import kotlin.math.sqrt

class GuardianService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var keyguardManager: KeyguardManager

    private var lastAlertTime = 0L
    private val alertCooldownMs = 30_000L

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

        startForeground(NOTIF_ID, buildNotification())
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!keyguardManager.isDeviceLocked) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val magnitude = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH

        if (abs(magnitude) > MOTION_THRESHOLD) {
            val now = System.currentTimeMillis()
            if (now - lastAlertTime > alertCooldownMs) {
                lastAlertTime = now
                AlertSender.sendAlert(
                    this,
                    "GuardianLock: Movement/tampering detected on your locked device."
                )
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val channelId = "guardian_channel"
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(channelId) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    getString(R.string.notif_channel_name),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.notif_active_title))
            .setContentText(getString(R.string.notif_active_text))
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val NOTIF_ID = 1001
        private const val MOTION_THRESHOLD = 4.0f
    }
}
