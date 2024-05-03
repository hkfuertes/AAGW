package net.mfuertes.aagw.gateway.connectivity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbManager
import android.preference.PreferenceManager
import android.util.Log
import net.mfuertes.aagw.gateway.GatewayService
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException


object Coordinator {

    private const val DEFAULT_HANDSHAKE_TIMEOUT = 15
    private const val DEFAULT_CONNECTION_TIMEOUT = 60 // 1min
    const val MESSAGE_KEY = "MESSAGE_KEY"
    const val ACTION_START_WITH_SOCKET = "start_socket"
    const val ACTION_START_WITH_USB = "start_usb"
    const val ACTION_STOP_WITH_MESSAGE = "stop_message"
    const val LOG_TAG = "COORDINATOR"

    fun initNativeFlow(context: Context, callback: (() -> Unit)? = null) {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        sharedPref.getString(GatewayService.MAC_ADDRESS_KEY, null)?.let { mac ->
            WifiHelper.startP2pAp(context, mac) { wifiHotspotInfo ->
                Log.d("NATIVE_FLOW", wifiHotspotInfo.toString())

                val pairedDevices = BluetoothProfileHandler.getBondedDevices()

                var connected = false
                while (!connected)
                    for (device in pairedDevices) {
                        BluetoothProfileHandler(context).connectDevice(
                            device,
                            DEFAULT_HANDSHAKE_TIMEOUT * 1000L,
                            wifiHotspotInfo
                        ) {
                            connected = it;
                        }
                    }
                callback?.invoke()
            }
        }
    }

    fun startServiceWithSocket(context: Context, socket: Socket) {
        SocketHandler.socket = socket
        val i = Intent(context, GatewayService::class.java)
        i.action = ACTION_START_WITH_SOCKET
        context.startForegroundService(i)
    }

    fun startServiceWithUsb(context: Context, usbAccessory: UsbAccessory) {
        val i = Intent(context, GatewayService::class.java)
        i.putExtra(UsbManager.EXTRA_ACCESSORY, usbAccessory)
        i.action = ACTION_START_WITH_USB
        context.startForegroundService(i)
    }

    fun stopServiceWithMessage(context: Context, message: String) {
        val i = Intent(context, GatewayService::class.java)
        i.putExtra(MESSAGE_KEY, message)
        i.action = ACTION_STOP_WITH_MESSAGE
        context.startForegroundService(i)
    }

    object SocketHandler {
        @get:Synchronized
        @set:Synchronized
        var socket: Socket? = null
    }

    fun listenForSocket(context: Context, callback: (socket: Socket) -> Unit) {
        try {
            val mServerSocket = ServerSocket(5288, 5).apply {
                // soTimeout = DEFAULT_CONNECTION_TIMEOUT * 1000
                reuseAddress = true
            }

            callback.invoke(mServerSocket.accept())

            mServerSocket.runCatching {
                close()
            }

            Log.i(LOG_TAG, "TCP connected")
        } catch (e: SocketTimeoutException) {
            stopServiceWithMessage(context,"Wireless connection did not happen")
        } catch (e: IOException) {
            Log.e(LOG_TAG, "tcp - error initializing: ${e.message}")
            stopServiceWithMessage(context,"Error initializing TCP")
        }
    }

    class BootCompletedReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            initNativeFlow(context) {
                listenForSocket(context) {
                    startServiceWithSocket(context, it)
                }
            }
        }
    }

}