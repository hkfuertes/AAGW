package net.mfuertes.aagw.bluetoothconnect

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log


class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("RECEIVER", "Making Discoverable!")
        if(intent.action.equals(Intent.ACTION_BOOT_COMPLETED)){
            Log.d("RECEIVER", "Making Discoverable!")
            makeDiscoverable(context, 300)
        }else if (intent.action.equals("android.bluetooth.device.action.PAIRING_REQUEST")) {
            pairDevice(intent)
            abortBroadcast()
        }
    }

    @SuppressLint("MissingPermission")
    private fun makeDiscoverable(context: Context, duration: Int) {
        val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, duration)
        context.startActivity(discoverableIntent)
        Log.i("Log", "Discoverable ")
    }

    @SuppressLint("MissingPermission")
    private fun pairDevice(intent: Intent){
        try {
            val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            val pin = intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", 0)
            //the pin in case you need to accept for an specific pin
            Log.d("PIN", pin.toString())
            //maybe you look for a name or address
            Log.d("Bonded", device!!.name)
            val pinBytes = ("" + pin).toByteArray(charset("UTF-8"))
            device!!.setPin(pinBytes)
            //setPairing confirmation if needed
            device!!.setPairingConfirmation(true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}