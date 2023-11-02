package net.mfuertes.aagw.gateway

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
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
import java.util.prefs.PreferenceChangeEvent
import java.util.prefs.PreferenceChangeListener

@SuppressLint("ExportedPreferenceActivity")
class USBReceiverActivity : PreferenceActivity(), OnSharedPreferenceChangeListener {
    companion object{
        const val NAME = "GATEWAY"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preferences)
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        val serverModeconfig = findPreference("server_mode_config").also {
            it.isEnabled = prefs.getBoolean(GatewayService.SERVER_MODE_KEY, false)
        }

        val ssid = findPreference(GatewayService.SSID_KEY).also {
            it.isEnabled = !prefs.getBoolean(GatewayService.SELF_AP_KEY, false)
        }

        val psk = findPreference(GatewayService.PSK_KEY).also {
            it.isEnabled = !prefs.getBoolean(GatewayService.SELF_AP_KEY, false)
        }

        findPreference(GatewayService.SERVER_MODE_KEY).apply {
            setOnPreferenceChangeListener{ _, value ->
                serverModeconfig.isEnabled = (value == true)
                return@setOnPreferenceChangeListener true
            }
        }

        findPreference(GatewayService.SELF_AP_KEY).apply {
            setOnPreferenceChangeListener{ _, value ->
                ssid.isEnabled = (value == false)
                psk.isEnabled = (value == false)
                return@setOnPreferenceChangeListener true
            }
        }

    }

    override fun onResume() {
        super.onResume()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkPermission(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_CONNECT
            ).asList(),0)
        }else{
            checkPermission(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            ).asList(),0)
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

    private fun fillIntent(intent: Intent): Intent{
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        intent.putExtra(GatewayService.SERVER_MODE_KEY, sharedPref.getBoolean(GatewayService.SERVER_MODE_KEY, false))
        intent.putExtra(GatewayService.SELF_AP_KEY, sharedPref.getBoolean(GatewayService.SELF_AP_KEY, false))
        intent.putExtra(GatewayService.SSID_KEY, sharedPref.getString(GatewayService.SSID_KEY, null))
        intent.putExtra(GatewayService.PSK_KEY, sharedPref.getString(GatewayService.PSK_KEY, null))
        intent.putExtra(GatewayService.BSSID_KEY, sharedPref.getString(GatewayService.BSSID_KEY, null))
        return intent
    }

    // Function to check and request permission.
    private fun checkPermission(permissions: List<String>, requestCode: Int) {
        for(permission in permissions){
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                // Requesting the permission
                requestPermissions( arrayOf(permission), requestCode)
            }
        }
    }

    override fun onSharedPreferenceChanged(sp: SharedPreferences?, key: String?) {
        TODO("Not yet implemented")
    }

}