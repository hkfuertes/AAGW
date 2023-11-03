## Android Auto Wirelesss Gateway
Complete solution for using an old phone as a Android Auto Wireless Dongle.
Tested on:
- **OnePlus X, _running LineageOS 18.1 (Android 11)_**: working in all modes.
  - The `LocalOnlyHotSpot` uses `wlan0` MAC as BSSID. It can be automatically detected if app is in `priv-app`.
  - If the app is `priv-app` it can also auto accept Android Auto connection and reset MTP mode when finished/errored.
- **FireStickTV, _running LineageOS 18.1 (Android 11)_**: working on Client Mode, _Maybe Server Mode using external AP (needs test)_
  - It gets `wlan0` without `priv-app`.
  - The `LocalOnlyHotSpot` errores with reason `2`. My bet is that AP is not implemented on TV Roms.
  - If the app is `priv-app` it can also auto accept Android Auto connection and reset MTP mode when finished/errored.

## Installing as `priv-app`
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

## Operation Modes
App needs to be in `/system/priv-app` for auto connection (headless, omit first time aproval in gui) and UsbDevice gadget mode reset.
- **Client**: The device connects to master wifi where HeadUnit Runs as server.
  - If headless use: `adb shell cmd -w add-network ssid wpa2 psk`
- **Server**:
  - The device has **AP** capabilities:
    - It will go through bonded Bluetooth devices trying to initiate Native Android Auto connection.
    - Regular (non root) `adb` has to be able to execute `ip -o link` (Has to be `priv-app`)
  - The device does not have **AP** capabilities:
    - It will need to be connected to an external Wifi (only device, no master)
    - `SSID, PSK, BSSID` have to be specified.
    - It will go through bonded Bluetooth devices trying to initiate Native Android Auto connection.
