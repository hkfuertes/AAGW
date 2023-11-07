# Android Auto Wirelesss Gateway
Complete solution for using an old phone as a Android Auto Wireless Dongle.

## Configuration
The only parameter I was not able to get automatically was the BSSID/MAC of the interface that will serve the Wifi hotspot, so it has to be setup manually.To do so you would require another computer, here are the steps for linux, but for other systems google how to get the BSSID of a wifi network:
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

## Tested on:
- **OnePlus X, _running LineageOS 18.1 (Android 11)_**
- **FireStickTV Lite (Sheldon),  _running LineageOS 18.1 (Android 11)_**

# Credit where credit is due..
- Big thanks to **[nisargjhaveri](https://github.com/nisargjhaveri/AAWirelessGateway)** for almost all the code, this is a minor refactor of his work with a minimal `LocalOnlyHotSpot` & usb gadget restart addition.
- Thank you also to **[north3221](https://github.com/north3221/AAGateWayWiFi)** for the idea of restarting mtp (usb gadget). I have done it with the Android hidden API (so that root is not required, only system app) but the idea is the same.