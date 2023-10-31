package net.mfuertes.aagw.bluetoothconnect

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections
import java.util.Locale

object APHelper {


    /**
     * Get IP address from first non-localhost interface
     * @param useIPv4   true=return ipv4, false=return ipv6
     * @return  address or empty string
     */
    fun getIPAddress(useIPv4: Boolean): String? {
        try {
            val interfaces: List<NetworkInterface> =
                Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs: List<InetAddress> = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress) {
                        val sAddr = addr.hostAddress
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        val isIPv4 = sAddr.indexOf(':') < 0
                        if (useIPv4) {
                            if (isIPv4) return sAddr
                        } else {
                            if (!isIPv4) {
                                val delim = sAddr.indexOf('%') // drop ip6 zone suffix
                                return if (delim < 0) sAddr.uppercase(Locale.getDefault()) else sAddr.substring(
                                    0,
                                    delim
                                ).uppercase(
                                    Locale.getDefault()
                                )
                            }
                        }
                    }
                }
            }
        } catch (ignored: java.lang.Exception) {
        } // for now eat exceptions
        return ""
    }

    @SuppressLint("MissingPermission")
    fun startAp(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

            wifiManager.startLocalOnlyHotspot(
                object : WifiManager.LocalOnlyHotspotCallback() {

                    override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation) {

                        val ssid = reservation.softApConfiguration.ssid
                        val psk = reservation.softApConfiguration.passphrase
                        val bssid = reservation.softApConfiguration.bssid

                        if (ssid != null) {
                            Log.d("DANG", ssid)
                        }

                        if (psk != null) {
                            Log.d("DANG", psk)
                        }

                        Log.d("DANG", bssid.toString())

                        Thread.sleep(30*1000)



                        getIPAddress(true)?.let { Log.v("DANG", it) }
                    }

                    override fun onStopped() {
                        super.onStopped()
                        Log.v("DANG", "Local Hotspot Stopped")
                    }

                    override fun onFailed(reason: Int) {
                        super.onFailed(reason)
                        Log.v("DANG", "Local Hotspot failed to start")
                    }
                }, Handler()
            )
        }
    }

}