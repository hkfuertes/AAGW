package net.mfuertes.aagw.gateway

import android.app.Activity
import android.content.Intent
import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbManager
import android.os.Bundle

class USBReceiverActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usbreceiver)
    }

    override fun onResume() {
        super.onResume()
        if (intent.action?.equals(UsbManager.ACTION_USB_ACCESSORY_ATTACHED) == true) {
            val accessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY) as UsbAccessory?
            accessory?.also { usbAccessory ->
                val i = Intent(this, AAGatewayService::class.java)
                i.putExtra(UsbManager.EXTRA_ACCESSORY, usbAccessory)
                this.startForegroundService(i)
            }
        }
        finish()
    }
}