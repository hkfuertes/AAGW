package net.mfuertes.aagw.gateway

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.util.Log


class MtpReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            val intentFilter = IntentFilter()
            intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            context.applicationContext.registerReceiver(this, intentFilter)
            toggleUSB()
        } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION == intent.action) {
            if (isNetworkAvailable(context)) {
                toggleUSB()
            }
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val nw = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
        return actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    companion object{
        fun toggleUSB() {
            //val cmdOne = arrayOf("su", "-c", "svc", "usb", "resetUsbGadget")
            val cmdTwo = arrayOf("su", "-c", "svc", "usb", "setFunctions")
            val cmdThree = arrayOf("su", "-c", "svc", "usb", "setFunctions", "mtp", "true")
            try {
                Log.d("USB CONN", "Restarting MTP")
                //var p = Runtime.getRuntime().exec(cmdOne)
                //p.waitFor()
                var p = Runtime.getRuntime().exec(cmdTwo)
                p.waitFor()
                p = Runtime.getRuntime().exec(cmdThree)
                p.waitFor()
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("USB CONN", "ERROR", e)
            }
        }
    }

}