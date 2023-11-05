package net.mfuertes.aagw.gateway

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceActivity
import android.preference.PreferenceManager


@SuppressLint("ExportedPreferenceActivity")
class USBReceiverActivity : PreferenceActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preferences)

        findPreference("bluetooth_permissions")?.apply {
            isEnabled = (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                            && !isPermissionAccepted(
                        arrayOf(
                            Manifest.permission.BLUETOOTH_CONNECT
                        ).asList()
                    )
                    )
            setOnPreferenceClickListener {
                checkPermission(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_CONNECT
                    ).asList(), 0
                )
                true
            }
        }
        findPreference("location_permissions")?.apply {
            isEnabled =
                !isPermissionAccepted(
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ).asList()
                )
            setOnPreferenceClickListener {
                checkPermission(
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    ).asList(), 0
                )
                true
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // am start -n net.mfuertes.aagw.gateway/.USBReceiverActivity --es MAC_ADDRESS <MAC>
        intent.getStringExtra("MAC_ADDRESS")?.let {
            PreferenceManager.getDefaultSharedPreferences(this).edit().apply{
                putString(GatewayService.MAC_ADDRESS_KEY, it).apply()
            }
            finish()
        }

        if (intent.action?.equals(UsbManager.ACTION_USB_ACCESSORY_ATTACHED) == true) {
            val accessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY) as UsbAccessory?
            accessory?.also { usbAccessory ->
                val i = Intent(this, GatewayService::class.java)
                i.putExtra(UsbManager.EXTRA_ACCESSORY, usbAccessory)
                fillIntent(i).also {
                    this.startForegroundService(it)
                }
            }
            finish()
        }
    }

    private fun fillIntent(intent: Intent): Intent {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        intent.putExtra(
            GatewayService.SELF_EXT_KEY,
            sharedPref.getBoolean(GatewayService.SELF_EXT_KEY, false)
        )
        intent.putExtra(
            GatewayService.SSID_KEY,
            sharedPref.getString(GatewayService.SSID_KEY, null)
        )
        intent.putExtra(GatewayService.PSK_KEY, sharedPref.getString(GatewayService.PSK_KEY, null))
        intent.putExtra(
            GatewayService.BSSID_KEY,
            sharedPref.getString(GatewayService.BSSID_KEY, null)
        )
        intent.putExtra(
            GatewayService.MAC_ADDRESS_KEY,
            sharedPref.getString(GatewayService.MAC_ADDRESS_KEY, null)
        )
        return intent
    }

    // Function to check and request permission.
    private fun checkPermission(permissions: List<String>, requestCode: Int) {
        for (permission in permissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                // Requesting the permission
                requestPermissions(arrayOf(permission), requestCode)
            }
        }
    }

    private fun isPermissionAccepted(permissions: List<String>): Boolean {
        return permissions.all { applicationContext.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
    }

}