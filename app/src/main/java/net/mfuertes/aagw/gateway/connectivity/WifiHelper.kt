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
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.ActionListener
import android.net.wifi.p2p.WifiP2pManager.ChannelListener
import android.os.Looper.getMainLooper
import android.util.Log
import net.mfuertes.aagw.gateway.GatewayService

object WifiHelper {

    private const val DEFAULT_HANDSHAKE_TIMEOUT = 15
    private const val DEFAULT_CONNECTION_TIMEOUT = 60 // 1min
    private val channelListener = ChannelListener { TODO("Not yet implemented") }

    @SuppressLint("MissingPermission")
    fun startP2pAp(
        context: Context,
        mac: String?,
        callback: ((wifiHotspotInfo: WifiHotspotInfo) -> Unit)?
    ) {
        val manager = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        val channel: WifiP2pManager.Channel =
            manager.initialize(context, getMainLooper(), channelListener)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION == intent.action) {
                    val networkInfo =
                        intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO) as NetworkInfo?
                    if (networkInfo!!.isConnected) {
                        manager.requestGroupInfo(channel) { group ->
                            if (group != null && group.isGroupOwner) {
                                manager.requestConnectionInfo(channel) { info ->
                                    if (info?.groupOwnerAddress != null) {
                                        val wifiHotspotInfo = WifiHotspotInfo(
                                            group.networkName,
                                            group.passphrase,
                                            mac, //sudo iwlist scanning on linux!
                                            info.groupOwnerAddress.hostAddress!!
                                        )
                                        Log.d("WIFI_P2P", "Info: $wifiHotspotInfo")
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

        manager.removeGroup(channel, null)
        manager.createGroup(channel, object : ActionListener {
            override fun onSuccess() {
                Log.d("WIFI_P2P", "Succeeded!")
            }

            override fun onFailure(error: Int) {
                Log.d("WIFI_P2P", "Error: $error")
                context.unregisterReceiver(receiver)
                startP2pAp(context, mac, callback)
            }

        })
    }

    fun stopP2pAp(
        context: Context
    ) {
        val manager = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        val channel: WifiP2pManager.Channel =
            manager.initialize(context, getMainLooper(), channelListener)

        manager.removeGroup(channel, null)
    }

    data class WifiHotspotInfo(
        val ssid: String,
        val psk: String,
        val bssid: String?,
        val ipAddress: String,
        val securityMode: WifiInfoRequestOuterClass.SecurityMode = WifiInfoRequestOuterClass.SecurityMode.UNKNOWN_SECURITY_MODE,
        val accessPointType: WifiInfoRequestOuterClass.AccessPointType = WifiInfoRequestOuterClass.AccessPointType.DYNAMIC
    )

}