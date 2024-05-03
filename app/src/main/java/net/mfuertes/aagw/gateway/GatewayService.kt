package net.mfuertes.aagw.gateway

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.util.Log
import net.mfuertes.aagw.gateway.connectivity.UsbHelper
import net.mfuertes.aagw.gateway.connectivity.WifiHelper
import net.mfuertes.aagw.gateway.connectivity.BluetoothProfileHandler
import java.io.*
import java.net.*


class GatewayService : Service() {
    companion object {
        private const val LOG_TAG = "AAService"

        private const val NOTIFICATION_CHANNEL_ID = "default"
        private const val NOTIFICATION_ID = 1

        private const val DEFAULT_HANDSHAKE_TIMEOUT = 15
        private const val DEFAULT_CONNECTION_TIMEOUT = 60 // 1min

        const val MAC_ADDRESS_KEY = "MAC_ADDRESS_KEY"
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
    private val ALREADY_STARTED = true;
    private var mMacAddress: String? = null

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

    /**
     * La idea aqui seria:
     * Dos formas de iniciar:
     * - Desde usb accesory:
     *   - Nos guardamos el acesorio
     *   - si tenemos tcp, adelante, sino esperamos
     * - Desde manual/flow (desde tcp)
     *   - guardamos el socket
     *   - si tenemos accesorio, adelante, sino esperamos
     */
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

        mMacAddress = intent?.getStringExtra(MAC_ADDRESS_KEY)
        if (mMacAddress == null) {
            Log.e(LOG_TAG, "No MAC Address found")
            stopService()
            return START_REDELIVER_INTENT
        }

        if(!ALREADY_STARTED)
            WifiHelper.initNativeFlow(this, mMacAddress!!)
        //Manually start AA.
        mRunning = true
        mUsbComplete = false
        mLocalComplete = false

        mMainHandlerThread.start()

        return START_REDELIVER_INTENT
    }

    private fun onMainHandlerThreadStopped() {
        stopService()
    }

    private fun stopService() {
        WifiHelper.stopP2pAp(this)
        //UsbHelper.setMode(mUsbManager, UsbHelper.FUNCTION_MTP)
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
                    // At this point TCP is ok!
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

    private inner class TCPPollThread() : Thread() {

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

                mServerSocket = ServerSocket(5288, 5).apply {
                    soTimeout = mClientConnectionTimeout * 1000
                    reuseAddress = true
                }

                mServerSocket?.let {
                    mSocket = it.accept().apply {
                        // soTimeout = 10000
                    }
                }

                mServerSocket?.runCatching {
                    close()
                }
                mServerSocket = null


                mSocket?.also {
                    mSocketOutputStream = it.getOutputStream()
                    mSocketInputStream = DataInputStream(it.getInputStream())
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