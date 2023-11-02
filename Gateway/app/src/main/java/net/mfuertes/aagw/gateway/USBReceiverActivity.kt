package net.mfuertes.aagw.gateway

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager
import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceActivity
import android.preference.PreferenceManager
import android.util.Log
import net.mfuertes.aagw.gateway.connectivity.WifiHelper

@SuppressLint("ExportedPreferenceActivity")
class USBReceiverActivity : PreferenceActivity(), OnSharedPreferenceChangeListener {
    companion object {
        const val NAME = "GATEWAY"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preferences)
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        // Setting default device MacAddress if found...
        WifiHelper.getMacAddress("wlan0")?.let {
            Log.d("MAC_ADDRESS", it)
            prefs.edit().putString(GatewayService.MAC_ADDRESS_KEY, it).commit()
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

        findPreference("MAC_ADDRESS_KEY")?.apply {
            setEnabled(WifiHelper.getMacAddress("wlan0") == null)
        }

    }

    override fun onResume() {
        super.onResume()

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
            GatewayService.SERVER_MODE_KEY,
            sharedPref.getBoolean(GatewayService.SERVER_MODE_KEY, false)
        )
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
            sharedPref.getString(GatewayService.MAC_ADDRESS_KEY, WifiHelper.getMacAddress("wlan0"))
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

    override fun onSharedPreferenceChanged(sp: SharedPreferences?, key: String?) {
        TODO("Not yet implemented")
    }

}