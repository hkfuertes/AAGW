package net.mfuertes.aagw.bluetoothconnect

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class MainActivity : Activity() {
    companion object{
        const val START_ANDROID_AUTO_FLOW = "net.mfuertes.aagw.bluetoothconnect.START_ANDROID_AUTO_FLOW"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()
        APHelper.startAp(this)
        if(intent.action.equals(Intent.ACTION_BOOT_COMPLETED)){
            PairingReceiver.makeDiscoverable()
        }else if(intent.action.equals(START_ANDROID_AUTO_FLOW)){
            //If mac saved start flow...
        }
        //finish()
    }
}