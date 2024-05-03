package net.mfuertes.aagw.gateway.connectivity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager
import android.os.Build
import android.preference.PreferenceManager
import net.mfuertes.aagw.gateway.GatewayService
import org.lsposed.hiddenapibypass.HiddenApiBypass
import kotlin.concurrent.thread


object UsbHelper {
    /**
     * From UsbManager class
     * https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/core/java/android/hardware/usb/UsbManager.java
     * Permissions:
     * - android.permission.MANAGE_USB (restricted --> `priv-app` required!)
     */
    const val FUNCTION_NONE: Long = 0
    const val FUNCTION_MTP = (1 shl 2).toLong()
    const val FUNCTION_PTP = (1 shl 4).toLong()
    const val FUNCTION_RNDIS = (1 shl 5).toLong()
    const val FUNCTION_MIDI = (1 shl 3).toLong()
    const val FUNCTION_ACCESSORY = (1 shl 1).toLong()
    const val FUNCTION_AUDIO_SOURCE = (1 shl 6).toLong()
    const val FUNCTION_ADB: Long = 1;
    const val FUNCTION_NCM = (1 shl 10).toLong()
    const val FUNCTION_UVC = (1 shl 7).toLong()

    private fun getCurrentFunctions(usbManager: UsbManager) : Long?{
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            return HiddenApiBypass.invoke(UsbManager::class.java, usbManager, "getCurrentFunctions") as Long
        return null;
    }

    private fun setCurrentFunctions(usbManager: UsbManager, function: Long){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            HiddenApiBypass.invoke(UsbManager::class.java, usbManager, "setCurrentFunctions", function)
    }
    @JvmStatic
    fun setMode(usbManager: UsbManager, function: Long, reset: Boolean = false){
        thread {
            try{
                if(reset) setCurrentFunctions(usbManager, FUNCTION_ADB)
                if(getCurrentFunctions(usbManager) != function){
                    setCurrentFunctions(usbManager, function)
                }
            }catch (ex: Exception){
                ex.printStackTrace()
            }
        }
    }
}