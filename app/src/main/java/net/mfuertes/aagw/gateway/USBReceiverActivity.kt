package net.mfuertes.aagw.gateway

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceActivity
import android.preference.PreferenceManager
import net.mfuertes.aagw.gateway.connectivity.WifiHelper


@SuppressLint("ExportedPreferenceActivity")
class USBReceiverActivity : PreferenceActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preferences)

        findPreference("start_stop_ap")?.apply {
            setOnPreferenceClickListener {
                WifiHelper.startP2pAp(this@USBReceiverActivity, null){
                    AlertDialog.Builder(this@USBReceiverActivity)
                        .setTitle("HotSpot Started")
                        .setMessage("Network name: ${it.ssid}\nPassword: ${it.psk}")
                        .setNegativeButton("Stop"){ _, _ ->
                            WifiHelper.stopP2pAp(this@USBReceiverActivity)
                        }
                        .setOnDismissListener {
                            WifiHelper.stopP2pAp(this@USBReceiverActivity)
                        }
                        .show()
                }
                true
            }
        }

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
                    ).asList()
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
                    ).asList()
                )
                true
            }
        }
    }

    override fun onResume() {
        super.onResume()

        WifiHelper.stopP2pAp(this)

        // am start -n net.mfuertes.aagw.gateway/.USBReceiverActivity --es MAC_ADDRESS <MAC>
        intent.getStringExtra("MAC_ADDRESS")?.let {
            PreferenceManager.getDefaultSharedPreferences(this).edit().apply{
                putString(GatewayService.MAC_ADDRESS_KEY, it).apply()
            }
            finish()
        }

        if (intent.action?.equals(UsbManager.ACTION_USB_ACCESSORY_ATTACHED) == true) {
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
            val accessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY) as UsbAccessory?
            accessory?.also { usbAccessory ->
                val i = Intent(this, GatewayService::class.java)
                i.putExtra(UsbManager.EXTRA_ACCESSORY, usbAccessory)
                i.putExtra(
                    GatewayService.MAC_ADDRESS_KEY,
                    sharedPref.getString(GatewayService.MAC_ADDRESS_KEY, null)
                )
                startForegroundService(i)
            }
            finish()
        }
    }

    // Function to check and request permission.
    private fun checkPermission(permissions: List<String>) {
        for (permission in permissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                // Requesting the permission
                requestPermissions(arrayOf(permission), 0)
            }
        }
    }

    private fun isPermissionAccepted(permissions: List<String>): Boolean {
        return permissions.all { applicationContext.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
    }

}