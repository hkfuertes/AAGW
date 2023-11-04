package net.mfuertes.aagw.gateway.connectivity

import WifiInfoRequestOuterClass
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.system.Os
import android.util.Log
import java.io.File
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections
import java.util.Locale


object WifiHelper {
    /**
     * Get IP address from first non-localhost interface
     * @param useIPv4   true=return ipv4, false=return ipv6
     * @return  address or empty string
     */
    fun getIPAddress(useIPv4: Boolean = true): String? {
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

    /**
     * Start LocalOnlyHotSpot
     * Permission:
     * - android.permission.CHANGE_WIFI_STATE
     * - android.permission.ACCESS_FINE_LOCATION
     */
    @SuppressLint("MissingPermission")
    fun startAp(
        context: Context,
        bssid: String,
        callback: (wifiHotspotInfo: WifiHotspotInfo?) -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

            wifiManager.startLocalOnlyHotspot(
                object : WifiManager.LocalOnlyHotspotCallback() {

                    override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation) {
                        super.onStarted(reservation)

                        val wifiHotspotInfo = WifiHotspotInfo(
                            reservation,
                            reservation.softApConfiguration.ssid!!,
                            reservation.softApConfiguration.passphrase!!,
                            (reservation.softApConfiguration.bssid ?: bssid).toString(),
                            getIPAddress(true)!!
                        )
                        callback(wifiHotspotInfo)
                    }

                    override fun onStopped() {
                        super.onStopped()
                        callback(null)
                    }

                    override fun onFailed(reason: Int) {
                        super.onFailed(reason)
                        callback(null)
                    }
                }, Handler()
            )
        }
    }

    private val registrationListener = object : NsdManager.RegistrationListener {
        override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {}
        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
        override fun onServiceUnregistered(arg0: NsdServiceInfo) {}
        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
    }


    fun registerService(context: Context, port: Int) {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "aawireless"
            serviceType = "_aawireless._tcp." //To make it work with Wifi Launcher!
            setPort(port)
        }

        (context.getSystemService(Context.NSD_SERVICE) as NsdManager).apply {
            registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        }
    }

    fun unRegisterService(context: Context) {
        try {
            (context.getSystemService(Context.NSD_SERVICE) as NsdManager).apply {
                unregisterService(registrationListener)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    data class WifiHotspotInfo(
        val reservation: WifiManager.LocalOnlyHotspotReservation?,
        val ssid: String,
        val psk: String,
        val bssid: String,
        val ipAddress: String,
        val securityMode: WifiInfoRequestOuterClass.SecurityMode = WifiInfoRequestOuterClass.SecurityMode.UNKNOWN_SECURITY_MODE,
        val accessPointType: WifiInfoRequestOuterClass.AccessPointType = WifiInfoRequestOuterClass.AccessPointType.DYNAMIC
    )

}