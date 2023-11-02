package net.mfuertes.aagw.gateway.connectivity

import WifiInfoRequestOuterClass
import android.annotation.SuppressLint
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
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
                            getMacAddress("wlan0")?: "c0:ee:fb:9a:00:76",
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
        (context.getSystemService(Context.NSD_SERVICE) as NsdManager).apply {
            unregisterService(registrationListener)
        }
    }
    // Theoretically no root required...
    fun getMacAddress(iface: String): String? {
        // 30: wlan0: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc mq state UP mode DEFAULT group default qlen 1000\    link/ether c0:ee:fb:9a:00:76 brd ff:ff:ff:ff:ff:ff
        val cmd = "ip -o link"
        val retVal = runCommand(cmd) ?: return null
        retVal.split("\n").also { list ->
            list.filter {it.contains(iface)}.also {
                if(it.isEmpty()) return null
                return it.first().split("link/ether").last().trim().split(" ").first()
            }
        }
    }

    private fun runCommand(cmd: String): String? {
        return try {
            val process = Runtime.getRuntime().exec(cmd)
            val reader = BufferedReader(
                InputStreamReader(process.inputStream)
            )
            var read: Int
            val buffer = CharArray(4096)
            val output = StringBuffer()
            while (reader.read(buffer).also { read = it } > 0) {
                output.append(buffer, 0, read)
            }
            reader.close()
            process.waitFor()
            output.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    data class WifiHotspotInfo(
        val reservation: WifiManager.LocalOnlyHotspotReservation,
        val ssid: String,
        val psk: String,
        val bssid: String,
        val ipAddress: String,
        val securityMode: WifiInfoRequestOuterClass.SecurityMode = WifiInfoRequestOuterClass.SecurityMode.UNKNOWN_SECURITY_MODE,
        val accessPointType: WifiInfoRequestOuterClass.AccessPointType = WifiInfoRequestOuterClass.AccessPointType.DYNAMIC
    )

}