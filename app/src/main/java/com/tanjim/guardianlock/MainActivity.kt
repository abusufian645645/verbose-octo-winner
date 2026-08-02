package com.tanjim.guardianlock

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var contactInput: EditText
    private lateinit var statusText: TextView

    private val requiredPermissions = mutableListOf(
        Manifest.permission.SEND_SMS,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ).apply {
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
    }.toTypedArray()

    private val permissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            requestDeviceAdmin()
        } else {
            Toast.makeText(this, "All permissions are required for protection to work", Toast.LENGTH_LONG).show()
        }
    }

    private val deviceAdminLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) {
        startProtection()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        contactInput = findViewById(R.id.contactInput)
        statusText = findViewById(R.id.statusText)

        val prefs = SecurePrefs.get(this)
        contactInput.setText(prefs.getString(SecurePrefs.KEY_CONTACT, ""))
        updateStatus()

        findViewById<Button>(R.id.enableButton).setOnClickListener {
            val number = contactInput.text.toString().trim()
            if (number.isEmpty()) {
                Toast.makeText(this, "Enter a contact number first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            prefs.edit().putString(SecurePrefs.KEY_CONTACT, number).apply()
            requestPermissionsThenAdmin()
        }

        findViewById<Button>(R.id.disableButton).setOnClickListener {
            stopService(Intent(this, GuardianService::class.java))
            SecurePrefs.get(this).edit().putBoolean(SecurePrefs.KEY_ENABLED, false).apply()
            updateStatus()
        }
    }

    private fun requestPermissionsThenAdmin() {
        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            requestDeviceAdmin()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun requestDeviceAdmin() {
        val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(this, GuardianDeviceAdminReceiver::class.java)
        if (dpm.isAdminActive(adminComponent)) {
            startProtection()
        } else {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "GuardianLock needs this to notice failed unlock attempts on your device."
                )
            }
            deviceAdminLauncher.launch(intent)
        }
    }

    private fun startProtection() {
        ContextCompat.startForegroundService(this, Intent(this, GuardianService::class.java))
        SecurePrefs.get(this).edit().putBoolean(SecurePrefs.KEY_ENABLED, true).apply()
        updateStatus()
    }

    private fun updateStatus() {
        val enabled = SecurePrefs.get(this).getBoolean(SecurePrefs.KEY_ENABLED, false)
        statusText.text = if (enabled) "Status: Protection ACTIVE" else "Status: Protection OFF"
    }
}
