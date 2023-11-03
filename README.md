# Android Auto Wirelesss Gateway
Complete solution for using an old phone as a Android Auto Wireless Dongle.

Tested on:
- **OnePlus X, _running LineageOS 18.1 (Android 11)_**: working in all modes.
  - The `LocalOnlyHotSpot` uses `wlan0` MAC as BSSID. It can be automatically detected if app is in `priv-app`, if wifi is enabled.
  - If the app is `priv-app` it can also auto accept Android Auto connection and reset MTP mode when finished/errored.
- **FireStickTV, _running LineageOS 18.1 (Android 11)_**: working on Client Mode, _Maybe Server Mode using external AP (needs test)_
  - It gets `wlan0` without `priv-app`.
  - The `LocalOnlyHotSpot` errores with reason `2`. My bet is that AP is not implemented on TV Roms.
  - If the app is `priv-app` it can also auto accept Android Auto connection and reset MTP mode when finished/errored.

## Operation Modes
- **Client**: The gateway connects to master's wifi where HeadUnit runs as server.
  - If headless use: `adb shell cmd -w add-network ssid wpa2 psk`
- **Server**: It will go through bonded Bluetooth devices trying to initiate Native Android Auto connection.
  - Internal AP _a.k.a. `LocalOnlyHotSpot`_:
    - It will go through bonded Bluetooth devices trying to initiate Native Android Auto connection.
    - The BSSID is the main attribute needed for Android Auto to connect. _On tested device LOHS will use WLAN0 MAC address as BSSID. A method is provided to auto discover it, but it will work depending on the device and if the app is system or not. It executes `ip -o link` to auto discover._
  - External AP:
    - Gateway has to be connected to this AP, even when no internet is detected.
    - `SSID, PSK, BSSID` have to be specified. Again BSSID is the main attribute for Android Auto to connect.

## Installing as `priv-app`
If the app is installed as system app `priv-app` it gains additional features, such us USB auto accept for Android Auto and (in some cases) WLAN0 MAC discovery.
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
