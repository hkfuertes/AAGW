package net.mfuertes.aagw.gateway

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import net.mfuertes.aagw.gateway.connectivity.WifiHelper

class USBReceiverActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usbreceiver)
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

        WifiHelper.getMacAddress("wlan0")?.let {
            Log.d("MAC_ADDRESS", it)
        }
        if (intent.action?.equals(UsbManager.ACTION_USB_ACCESSORY_ATTACHED) == true) {
            val accessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY) as UsbAccessory?
            accessory?.also { usbAccessory ->
                val i = Intent(this, GatewayService::class.java)
                i.putExtra(UsbManager.EXTRA_ACCESSORY, usbAccessory)
                this.startForegroundService(i)
            }
            finish()
        }
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

}