package net.mfuertes.aagw.gateway

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.util.Log
import net.mfuertes.aagw.gateway.connectivity.UsbHelper
import net.mfuertes.aagw.gateway.connectivity.WifiHelper
import net.mfuertes.aagw.gateway.connectivity.bluetooth.BluetoothProfileHandler
import java.io.*
import java.net.*


class GatewayService : Service() {
    companion object {
        private const val LOG_TAG = "AAService"

        private const val NOTIFICATION_CHANNEL_ID = "default"
        private const val NOTIFICATION_ID = 1

        private const val DEFAULT_HANDSHAKE_TIMEOUT = 15
        private const val DEFAULT_CONNECTION_TIMEOUT = 60 // 1min
    }

    private var mLogCommunication = false

    private var mRunning = false

    private var mAccessory: UsbAccessory? = null

    private var mPhoneInputStream: FileInputStream? = null
    private var mPhoneOutputStream: FileOutputStream? = null

    private var mSocketInputStream: DataInputStream? = null
    private var mSocketOutputStream: OutputStream? = null

    private var mUsbComplete = false
    private var mLocalComplete = false

    private var mClientConnectionTimeout = DEFAULT_CONNECTION_TIMEOUT

    private val mMainHandlerThread = MainHandlerThread()

    private val mUsbManager: UsbManager by lazy { getSystemService(UsbManager::class.java) }
    private val mBluetoothProfileHandler: BluetoothProfileHandler by lazy { BluetoothProfileHandler(this) }

    override fun onCreate() {
        super.onCreate()

        val notificationManager = getSystemService(NotificationManager::class.java)

        val notificationChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "General",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationChannel.setSound(null, null)
        notificationChannel.enableVibration(false)
        notificationChannel.enableLights(false)
        notificationManager.createNotificationChannel(notificationChannel)
    }

    private fun updateNotification(msg: String) {
        Log.i(LOG_TAG, "Notification updated: $msg")
        val notificationBuilder = Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(msg)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            notificationBuilder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }

        startForeground(NOTIFICATION_ID, notificationBuilder.build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (isRunning()) return START_REDELIVER_INTENT

        updateNotification("Started")

        mAccessory = intent?.getParcelableExtra(UsbManager.EXTRA_ACCESSORY) as UsbAccessory?
        if (mAccessory == null) {
            Log.e(LOG_TAG, "No USB accessory found")
            stopService()
            return START_REDELIVER_INTENT
        }
        //NSD discovery!
        WifiHelper.registerService(this, 5288)

        WifiHelper.startAp(this){ wifiHotspotInfo ->
            Log.d("NATIVE_FLOW", wifiHotspotInfo.toString())

            /**
             * Permissions:
             * - android.permission.BLUETOOTH_CONNECT
             */
            if(
                checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                ){
                Log.e(LOG_TAG, "Permission BLUETOOTH_CONNECT not granted")
                stopService()
                return@startAp
            }
            val pairedDevices: Set<BluetoothDevice> = BluetoothAdapter.getDefaultAdapter().bondedDevices

            for(device in pairedDevices){
                //Start Native Flow
                mBluetoothProfileHandler.connectDevice(device, DEFAULT_HANDSHAKE_TIMEOUT * 1000L, wifiHotspotInfo!!){
                    Log.d("NATIVE_FLOW", "Bluetooth: $it")
                }
            }

            //Manually start AA.
            mRunning = true
            mUsbComplete = false
            mLocalComplete = false

            mMainHandlerThread.start()
        }

        return START_REDELIVER_INTENT
    }

    private fun onMainHandlerThreadStopped() {
        stopService()
    }

    private fun stopService() {
        WifiHelper.unRegisterService(this)
        UsbHelper.setMode(mUsbManager, UsbHelper.FUNCTION_MTP)
        stopForeground(true)
        stopSelf()
    }

    private fun stopRunning(msg: String) {
        Log.i(LOG_TAG, msg)

        if (mRunning) {
            mRunning = false
            updateNotification("Stopping wireless connection")
        }

        mMainHandlerThread.cancel()
    }

    private fun isRunning() = mRunning

    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRunning("Service onDestroy")
    }

    private inner class MainHandlerThread : Thread() {
        private val mUsbThread = USBPollThread()
        private val mTcpThread = TCPPollThread()

        fun cancel() {
            mUsbThread.cancel()
            mTcpThread.cancel()
        }

        override fun run() {
            super.run()

            mUsbThread.start()
            mTcpThread.start()

            mUsbThread.join()
            mTcpThread.join()

            Handler(mainLooper).post {
                onMainHandlerThreadStopped()
            }
        }
    }

    private inner class USBPollThread : Thread() {
        var mUsbFileDescriptor: ParcelFileDescriptor? = null

        fun cancel() {
            mUsbFileDescriptor?.runCatching {
                close()
            }
        }

        override fun run() {
            super.run()

            if (isRunning() && !mLocalComplete) {
                updateNotification("Waiting for TCP")
            }

            while (isRunning() && !mLocalComplete) {
                try {
                    sleep(100)
                } catch (e: InterruptedException) {
                    Log.e(LOG_TAG, "usb - error sleeping: ${e.message}")
                }
            }

            if (isRunning()) {
                mUsbFileDescriptor = mUsbManager.openAccessory(mAccessory)?.also {
                    val fd = it.fileDescriptor
                    mPhoneInputStream = FileInputStream(fd)
                    mPhoneOutputStream = FileOutputStream(fd)

                    // Fake HandShake?
                    // mPhoneInputStream!!.read(ByteArray(16384))
                    // mPhoneOutputStream!!.write(byteArrayOf(0, 3, 0, 8, 0, 2, 0, 1, 0, 4, 0, 0))
                }

                if (mUsbFileDescriptor == null || mPhoneInputStream == null || mPhoneOutputStream == null) {
                    stopRunning("Error initializing USB")
                }
            }

            if (isRunning()) {
                mUsbComplete = true
            }

            if (isRunning()) {
                val phoneInputStream = mPhoneInputStream!!

                val buffer = ByteArray(16384)
                while (isRunning()) {
                    try {
                        val len: Int
                        try {
                            len = phoneInputStream.read(buffer)
                            if (mLogCommunication) Log.v(
                                LOG_TAG,
                                "USB read: ${buffer.copyOf(len).toHex()}"
                            )
                        } catch (e: Exception) {
                            Log.e(LOG_TAG, "usb main loop - usb read error: ${e.message}")
                            throw e
                        }

                        try {
                            mSocketOutputStream?.write(buffer.copyOf(len))
                        } catch (e: Exception) {
                            Log.e(LOG_TAG, "usb main loop - tcp write error: ${e.message}")
                            throw e
                        }
                    } catch (e: Exception) {
                        stopRunning("Error in USB main loop")
                    }
                }
            }

            mUsbFileDescriptor?.apply {
                try {
                    close()
                } catch (e: IOException) {
                    Log.e(LOG_TAG, "usb - error closing file descriptor: ${e.message}")
                }
            }

            stopRunning("USB main loop stopped")
        }

    }

    private inner class TCPPollThread(server: Boolean = true) : Thread() {

        val serverMode = server

        var mServerSocket: ServerSocket? = null
        var mSocket: Socket? = null

        fun cancel() {
            mServerSocket?.runCatching {
                close()
            }
            mServerSocket = null

            mSocket?.runCatching {
                close()
            }
            mSocket = null
        }

        override fun run() {
            super.run()

            try {
                if(serverMode){
                    mServerSocket = ServerSocket(5288, 5).apply {
                        soTimeout = mClientConnectionTimeout * 1000
                        reuseAddress = true
                    }

                    mServerSocket?.let {
                        mSocket = it.accept().apply {
                            soTimeout = 10000
                        }
                    }

                    mServerSocket?.runCatching {
                        close()
                    }
                    mServerSocket = null
                }else{
                    var addressInt: Int
                    var address: String

                    do {
                        addressInt = getSystemService(WifiManager::class.java).dhcpInfo.gateway
                        address = "%d.%d.%d.%d".format(
                            null,
                            addressInt and 0xff,
                            addressInt shr 8 and 0xff,
                            addressInt shr 16 and 0xff,
                            addressInt shr 24 and 0xff
                        )
                        sleep(100)
                        Log.d(LOG_TAG, address)
                    } while (address == "0.0.0.0" && isRunning())

                    // If we got an interrupt (is not running) we end the thread...
                    if(!isRunning()) return;

                    Log.d(LOG_TAG, address)
                    mSocket = Socket().apply {
                        connect(InetSocketAddress(address, 5277), mClientConnectionTimeout * 1000)
                    }
                }

                mSocket?.also {
                    mSocketOutputStream = it.getOutputStream()
                    mSocketInputStream = DataInputStream(it.getInputStream())

                    //Fake Handshake?
                    // mSocketOutputStream!!.write(byteArrayOf(0, 3, 0, 6, 0, 1, 0, 1, 0, 2))
                    // mSocketOutputStream!!.flush()
                    // mSocketInputStream!!.read(ByteArray(12))
                }

                Log.i(LOG_TAG, "TCP connected")
            } catch (e: SocketTimeoutException) {
                stopRunning("Wireless connection did not happen")
            } catch (e: IOException) {
                Log.e(LOG_TAG, "tcp - error initializing: ${e.message}")
                stopRunning("Error initializing TCP")
            }

            if (isRunning() && mSocket == null) {
                stopRunning("Error connecting to wireless client")
            }

            if (isRunning()) {
                mLocalComplete = true
            }

            if (isRunning() && !mUsbComplete) {
                updateNotification("Waiting for USB")
            }

            while (isRunning() && !mUsbComplete) {
                try {
                    sleep(10)
                } catch (e: InterruptedException) {
                    Log.e(LOG_TAG, "tcp - error sleeping ${e.message}")
                }
            }

            if (isRunning()) {
                updateNotification("Connected!")
            }

            val buffer = ByteArray(16384)
            while (isRunning()) {
                try {
                    var pos = 4
                    val encLen: Int
                    try {
                        mSocketInputStream?.readFully(buffer, 0, 4)
                        if (buffer[1].toInt() == 9) //Flag 9 means the header is 8 bytes long (read four more bytes separately)
                        {
                            pos += 4
                            mSocketInputStream?.readFully(buffer, 4, 4)
                        }

                        encLen =
                            ((buffer[2].toInt() and 0xFF) shl 8) or (buffer[3].toInt() and 0xFF)

                        mSocketInputStream?.readFully(buffer, pos, encLen)

                        if (mLogCommunication) Log.v(
                            LOG_TAG,
                            "TCP read: ${buffer.copyOf(encLen + pos).toHex()}"
                        )
                    } catch (e: Exception) {
                        Log.e(LOG_TAG, "tcp main loop - tcp read error: ${e.message}")
                        throw e
                    }

                    try {
                        mPhoneOutputStream?.write(buffer.copyOf(encLen + pos))
                    } catch (e: Exception) {
                        Log.e(LOG_TAG, "tcp main loop - usb write error: ${e.message}")
                        throw e
                    }
                } catch (e: Exception) {
                    stopRunning("Error in TCP main loop")
                }
            }

            mSocket?.apply {
                try {
                    close()
                } catch (e: IOException) {
                    Log.e(LOG_TAG, "tcp - error closing socket: ${e.message}")
                }
            }

            stopRunning("TCP main loop stopped")
        }
    }

    private val hexArray = "0123456789ABCDEF".toCharArray()
    fun ByteArray.toHex(): String {
        val hexChars = CharArray(size * 2)

        var i = 0

        forEach {
            val octet = it.toInt()
            hexChars[i] = hexArray[(octet and 0xF0).ushr(4)]
            hexChars[i + 1] = hexArray[(octet and 0x0F)]
            i += 2
        }

        return String(hexChars)
    }

}