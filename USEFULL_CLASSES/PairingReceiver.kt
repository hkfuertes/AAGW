/*

<receiver
    android:name=".connectivity.bluetooth.PairingReceiver"
    android:enabled="true"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
        <action android:name="android.bluetooth.device.action.PAIRING_REQUEST"/>
    </intent-filter>
</receiver>

*/

package net.mfuertes.aagw.gateway.connectivity.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import org.lsposed.hiddenapibypass.HiddenApiBypass


class PairingReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        pairDevice(context, intent)
        abortBroadcast()
    }

    companion object {

        /**
         * BluetoothAdapter.setScanMode()
         * https://cs.android.com/android/platform/superproject/main/+/main:packages/modules/Bluetooth/framework/java/android/bluetooth/BluetoothAdapter.java
         *
         * Permissions:
         * - android.Manifest.permission.BLUETOOTH_SCAN
         * - android.Manifest.permission.BLUETOOTH_PRIVILEGED
         */
        fun makeDiscoverable() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                HiddenApiBypass.invoke(
                    BluetoothAdapter::class.java,
                    BluetoothAdapter.getDefaultAdapter(),
                    "setScanMode",
                    BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE
                )
        }

        /**
         * Permissions:
         * - android.Manifest.permission.BLUETOOTH_CONNECT
         */
        private fun pairDevice(context: Context, intent: Intent) {
            if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return
            }
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