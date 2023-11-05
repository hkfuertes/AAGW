# Android Auto Wirelesss Gateway
Complete solution for using an old phone as a Android Auto Wireless Dongle.

## Operation Modes
It will go through bonded Bluetooth devices trying to initiate Native Android Auto connection. Two modes can be used, internal `LocalOnlyHotSpot` or external AP.
  - Internal AP _a.k.a. `LocalOnlyHotSpot`_:
    - The `BSSID` is the main attribute needed for Android Auto to connect. _On tested device LOHS will use WLAN0 MAC address as BSSID._
  - External AP:
    - Gateway has to be connected to this AP, even when no internet is detected (see below).
    - `SSID, PSK, BSSID` have to be specified. Again BSSID is the main attribute for Android Auto to connect.

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
## Configuring the MAC_ADDRESS from `adb shell`
If the main activity is started with an extra string `MAC_ADDRESS`, the mac address field will be updated:
```shell
# Get the WLAN0 mac address with
adb shell 'ip -o link | grep wlan0' #... or by any other mean ...
adb shell 'am start -n net.mfuertes.aagw.gateway/.USBReceiverActivity --es MAC_ADDRESS <MAC>'
```

## Allow _no internet_ connection
Android will ping Google servers to check if the connection has internet. If the connection has no internet it will prompt or not connect. With this commands, google wont ask, it will connect.
```shell
adb shell 'settings put global captive_portal_detection_enabled 0'
adb shell 'settings put global captive_portal_mode 0'
```

## ESP32 SoftAP sketch
If for some reason your device doesnt support Tethering, you can use this sketch to program a cheap ESP device to act as an access point _(...like the ones used to 'hack' the PS4)_:
```c++
#include <WiFi.h>
#include "esp_wifi.h"

const char* ssid           = "AndroidAuto";   // SSID Name
const char* password       = "1234567890";    // SSID Password - Set to NULL to have an open AP
const int   channel        = 10;              // WiFi Channel number between 1 and 13
const bool  hide_SSID      = false;           // To disable SSID broadcast -> SSID will not appear in a basic WiFi scan
const int   max_connection = 2;               // Maximum simultaneous connected clients on the AP

void setup()
{
    Serial.begin(115200);
    WiFi.mode(WIFI_AP);
    WiFi.softAP(ssid, password, channel, hide_SSID, max_connection);
    Serial.print("[+] AP Created with IP Gateway ");
    Serial.println(WiFi.softAPIP());
    Serial.printf("[+] MAC address = %s\n", WiFi.softAPmacAddress().c_str());
}

void loop(){}
```

## Tested on:
- **OnePlus X, _running LineageOS 18.1 (Android 11)_**: working in all modes.
  - The `LocalOnlyHotSpot` uses `wlan0` MAC as BSSID.

# Credit where credit is due..
- Big thanks to **[nisargjhaveri](https://github.com/nisargjhaveri/AAWirelessGateway)** for almost all the code, this is a minor refactor of his work with a minimal `LocalOnlyHotSpot` & usb gadget restart addition.
- Thank you also to **[north3221](https://github.com/north3221/AAGateWayWiFi)** for the idea of restarting mtp (usb gadget). I have done it with the Android hidden API (so that root is not required, only system app) but the idea is the same.