package net.mfuertes.aagw.gateway.connectivity

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import org.lsposed.hiddenapibypass.HiddenApiBypass


class PairingReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        pairDevice(intent)
        abortBroadcast()
    }

    companion object {
        fun makeDiscoverable() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                HiddenApiBypass.invoke(
                    BluetoothAdapter::class.java,
                    BluetoothAdapter.getDefaultAdapter(),
                    "setScanMode",
                    BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE
                )
        }

        @SuppressLint("MissingPermission")
        private fun pairDevice(intent: Intent) {
            try {
                val device: BluetoothDevice? =
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                val pin = intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", 0)
                Log.d("PIN", pin.toString())
                Log.d("Bonded", device!!.name)
                val pinBytes = ("" + pin).toByteArray(charset("UTF-8"))
                device!!.setPin(pinBytes)
                device!!.setPairingConfirmation(true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}