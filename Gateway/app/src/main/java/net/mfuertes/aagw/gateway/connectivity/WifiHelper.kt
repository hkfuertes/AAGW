package net.mfuertes.aagw.gateway.connectivity

import WifiInfoRequestOuterClass
import android.annotation.SuppressLint
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
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
    fun startAp(
        context: Context,
        callback: (success: Boolean, reservation: WifiManager.LocalOnlyHotspotReservation?, ipAddress: String?) -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

            wifiManager.startLocalOnlyHotspot(
                object : WifiManager.LocalOnlyHotspotCallback() {

                    override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation) {
                        super.onStarted(reservation)
                        callback(true, reservation, getIPAddress(true))
                    }

                    override fun onStopped() {
                        super.onStopped()
                        callback(false, null, null)
                    }

                    override fun onFailed(reason: Int) {
                        super.onFailed(reason)
                        callback(false, null, null)
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
        (context.getSystemService(Context.NSD_SERVICE) as NsdManager).apply {
            unregisterService(registrationListener)
        }
    }

    fun getMacAddr(): String? {
        try {
            val all: List<NetworkInterface> =
                Collections.list(NetworkInterface.getNetworkInterfaces())
            for (nif in all) {
                if (!nif.name.equals("wlan0", ignoreCase = true)) continue
                val macBytes = nif.hardwareAddress ?: return ""
                val res1 = StringBuilder()
                for (b in macBytes) {
                    // res1.append(Integer.toHexString(b & 0xFF) + ":");
                    res1.append(String.format("%02X:", b))
                }
                if (res1.length > 0) {
                    res1.deleteCharAt(res1.length - 1)
                }
                return res1.toString()
            }
        } catch (ex: Exception) {
            //handle exception
        }
        return ""
    }


    data class WifiHotspotInfo(
        val ssid: String,
        val psk: String,
        val bssid: String = "*", //Any?
        val ipAddress: String,
        val securityMode: WifiInfoRequestOuterClass.SecurityMode = WifiInfoRequestOuterClass.SecurityMode.UNKNOWN_SECURITY_MODE,
        val accessPointType: WifiInfoRequestOuterClass.AccessPointType = WifiInfoRequestOuterClass.AccessPointType.DYNAMIC
    )

    fun disableMACRandomization(){

    }

}