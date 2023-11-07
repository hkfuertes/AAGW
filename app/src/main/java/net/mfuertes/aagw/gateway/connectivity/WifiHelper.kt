package net.mfuertes.aagw.gateway.connectivity

import WifiInfoRequestOuterClass
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.ActionListener
import android.net.wifi.p2p.WifiP2pManager.ChannelListener
import android.os.Build
import android.os.Handler
import android.os.Looper.getMainLooper
import android.util.Log
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

    private val channelListener = ChannelListener { TODO("Not yet implemented") }

    @SuppressLint("MissingPermission")
    fun startP2pAp(
        context: Context,
        mac: String?,
        callback: ((wifiHotspotInfo: WifiHotspotInfo) -> Unit)?
    ) {
        val manager = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        val channel: WifiP2pManager.Channel = manager.initialize(context, getMainLooper(), channelListener)

        val receiver = object: BroadcastReceiver() {
            override fun onReceive(receiverContext :Context, intent: Intent) {
                if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION == intent.action) {
                    val networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO) as NetworkInfo?
                    if (networkInfo!!.isConnected) {
                        manager.requestGroupInfo(channel) { group ->
                            if (group != null && group.isGroupOwner) {
                                manager.requestConnectionInfo(channel) {info ->
                                    if(info?.groupOwnerAddress != null){
                                        val wifiHotspotInfo = WifiHotspotInfo(
                                            null,
                                            group.networkName,
                                            group.passphrase,
                                            mac, //sudo iwlist scanning on linux!
                                            info.groupOwnerAddress.hostAddress!!
                                        )
                                        Log.d("WIFI_P2P","Info: $wifiHotspotInfo")
                                        callback?.invoke(wifiHotspotInfo)
                                    }
                                }


                            }
                        }
                        //Self unregister!
                        context.unregisterReceiver(this)
                    }
                }
            }

        }

        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        context.registerReceiver(receiver, intentFilter);

        manager.removeGroup(channel,null)
        manager.createGroup(channel, object :ActionListener {
            override fun onSuccess() {
                Log.d("WIFI_P2P", "Succeeded!")
            }

            override fun onFailure(error: Int) {
                Log.d("WIFI_P2P", "Error: $error")
                startP2pAp(context, mac, callback)
            }

        })
    }

    fun stopP2pAp(
        context: Context ) {
        val manager = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        val channel: WifiP2pManager.Channel = manager.initialize(context, getMainLooper(), channelListener)

        manager.removeGroup(channel,null)
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
        val bssid: String?,
        val ipAddress: String,
        val securityMode: WifiInfoRequestOuterClass.SecurityMode = WifiInfoRequestOuterClass.SecurityMode.UNKNOWN_SECURITY_MODE,
        val accessPointType: WifiInfoRequestOuterClass.AccessPointType = WifiInfoRequestOuterClass.AccessPointType.DYNAMIC
    )

}