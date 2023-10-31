package net.mfuertes.aagw.gateway;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;

public class BootCompletedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        /*
        TBR!!!
             if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
                val intentFilter = IntentFilter()
                intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
                context.applicationContext.registerReceiver(this, intentFilter)
                toggleUSB()
            } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION == intent.action) {
                if (isNetworkAvailable(context)) {
                    toggleUSB()
                }
            }
         */
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            UsbHelper.setMode(context.getSystemService(UsbManager.class), UsbHelper.FUNCTION_MTP);
        }
    }
}