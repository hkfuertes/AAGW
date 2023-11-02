## Android Auto Wirelesss Gateway
Complete solution for using an old phone (Fire Stick TV) as a Android Auto Wireless Dongle.

## Installing as `priv-app`
```shell
# Install from TWRP
adb push Gateway.apk /sdcard
adb push privapp-permissions-net.mfuertes.aagw.gateway.xml /sdcard

adb shell twrp mount /system
adb shell twrp remountrw /system
adb shell mkdir -p /system/priv-app/net.mfuertes.aagw.gateway/
adb shell chmod 0755 /system/priv-app/net.mfuertes.aagw.gateway
adb shell cp /sdcard/Gateway.apk /system/priv-app/net.mfuertes.aagw.gateway/base.apk
adb shell chmod 0644 /system/priv-app/net.mfuertes.aagw.gateway/base.apk
adb shell cp /sdcard/privapp-permissions-net.mfuertes.aagw.gateway.xml /system/etc/permissions/
adb shell chmod 0644 /system/etc/permissions/privapp-permissions-net.mfuertes.aagw.gateway.xml
```


## Operation Modes
App needs to be in `/system/priv-app` for auto connection (headless, omit first time aproval in gui) and UsbDevice gadget mode reset.
- **Client**: The device connects to master wifi where HeadUnit Runs as server.
  - If headless use: `adb shell cmd -w add-network ssid wpa2 psk`
- **Server**:
  - The device has **AP** capabilities:
    - It will go through bonded Bluetooth devices trying to initiate Native Android Auto connection.
    - Regular (non root) `adb` has to be able to execute `ip -o link`
    - Location permission has to be manually enabled via UI or: 
      
      ```shell
      abd shell pm grant net.mfuertes.aagw.gateway android.permission.ACCESS_FINE_LOCATION
      ```
      > LOCATION (GPS) has to be enabled for the HotSpot to be brought up!
  - The device does not have **AP** capabilities:
    - It will need to be connected to an external Wifi (only device, no master)
    - `SSID, PSK, BSSID` have to be specified in the config file (TODO).
    - It will go through bonded Bluetooth devices trying to initiate Native Android Auto connection.
