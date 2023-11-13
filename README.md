# Android Auto Wirelesss Gateway
Complete solution for using an old phone as a Android Auto Wireless Dongle.

## Configuration
Android Auto uses bluetooth and wifi to comunicate wirelessly. First a bluetooth connection is established where the wifi details are shared between the car and the phone. We need to prepare the device for both things separately. _Its a one time setup, no worries..._

### Bluetooth
You will need to pair the two devices toghether first, as upon car detected, the app will go throught all the bonded devices trying to initiate the connection. Open Bluetooth settings in both devices an pair them toghether.

If using a TV box (FireStick in my case), the Bluetooth settings panel may filter out anything NOT a controller/headset. Use third party apps, I used these ones: 
- https://play.google.com/store/apps/details?id=com.zdworksinc.bluetoothtvscanner
- https://play.google.com/store/apps/details?id=com.waylonhuang.bluetoothpair

### Wifi HotSpot
The only parameter I was not able to get automatically was the BSSID/MAC of the interface that will serve the Wifi hotspot, so it has to be setup manually. To do so you would require another computer, here are the steps for linux, but for other systems google how to get the BSSID of a wifi network:
1. Start the hotspot from the app. A dialog will appear with the name of the wifi network something like `DIRECT-...`
2. On a Linux pc run the command `sudo iwlist scanning` and search for the wifi network, in this example the _**BSSID**_ is: `BA:73:9C:75:2B:62`
   ```
   Cell 11 - Address: BA:73:9C:75:2B:62
                   Channel:1
                   Frequency:2.412 GHz (Channel 1)
                   Quality=70/70  Signal level=-37 dBm  
                   Encryption key:on
                   ESSID:"DIRECT-V6-Android_bb59"
                   Bit Rates:6 Mb/s; 9 Mb/s; 12 Mb/s; 18 Mb/s; 24 Mb/s
                             36 Mb/s; 48 Mb/s; 54 Mb/s
   ...
   ```
3. Enter the `BSSID` in the field called _**P2P0 MAC Address (BSSID)**_, Alternatively you can enter this data from the `adb shell`:

      ```shell
      adb shell 'am start -n net.mfuertes.aagw.gateway/.USBReceiverActivity --es MAC_ADDRESS <MAC>'
      ```
## Usage
Once everything is setup, just plug the device onto the car and wait for Android Auto apear on the dashboard.

**How does it work?** Once the USB is detected, it will try to initiate the Android Auto wired connection. If Google Apps are instaled (and therefore regular Android Auto) or if it is not installed as system app, It will ask you wich application has to handle the usb device (the car), you need to manually select this app and set it as allways. Then the process will start. It will bring up the AP and will try to connect to the bonded Bluetooth devices. It will try to connect as a Bluetooth audio sink. Then the master phone will react and try to reach back to the gateway looking for the specific Service UUID to ask the wifi credentials (SSID, PSK, BSSID, IP). Once this credentials are exchanged, the bluetooth part is done, and the master phone will try to connect to that wifi, and to that ip. Then the gateway just forwards everything that comes from the network to the usb and viceversa.

## Installing as `priv-app`
If the app is installed as system app `priv-app` it gains additional features, such us USB auto accept for Android Auto or restart MTP on failure.
```shell
# Install from TWRP
adb push gateway.apk /sdcard
adb push privapp-permissions-net.mfuertes.aagw.gateway.xml /sdcard

adb shell twrp mount /system
adb shell twrp remountrw /system
adb shell mkdir -p /system/priv-app/net.mfuertes.aagw.gateway/
adb shell chmod 0755 /system/priv-app/net.mfuertes.aagw.gateway
adb shell cp /sdcard/gateway.apk /system/priv-app/net.mfuertes.aagw.gateway/base.apk
adb shell chmod 0644 /system/priv-app/net.mfuertes.aagw.gateway/base.apk
adb shell cp /sdcard/privapp-permissions-net.mfuertes.aagw.gateway.xml /system/etc/permissions/
adb shell chmod 0644 /system/etc/permissions/privapp-permissions-net.mfuertes.aagw.gateway.xml
```
> This MTP restart is useful if the device does not have baterry, and re-plug means restart, ie: TV Sticks...

## Tested on:
- **OnePlus X, _running LineageOS 18.1 (Android 11)_**
- **FireStickTV Lite (Sheldon),  _running LineageOS 18.1 (Android 11)_**
- **Pixel 2XL,  _running LineageOS 20 (Android 13)_**

# Credit where credit is due..
- Big thanks to **[nisargjhaveri](https://github.com/nisargjhaveri/AAWirelessGateway)** for almost all the code, this is a minor refactor of his work with a minimal P2P AP & USB gadget restart addition.
- Thank you also to **[north3221](https://github.com/north3221/AAGateWayWiFi)** for the idea of restarting mtp (usb gadget). I have done it with the Android hidden API (so that root is not required, only system app) but the idea is the same.
