package net.mfuertes.aagw.helper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        PairingReceiver.makeDiscoverable()
        UsbHelper.setMode(context.getSystemService(UsbManager::class.java) as UsbManager, UsbHelper.FUNCTION_MTP)
    }
}