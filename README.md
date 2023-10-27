## Android Auto Wirelesss Gateway
Complete solution for using an old phone (Fire Stick TV) as a Android Auto Wireless Dongle.

---
### Main App
This is the main GW App. It will be in server mode.
- Take https://github.com/nisargjhaveri/AAWirelessGateway as base.
- Enable HotSpot on Boot
  -  Configure it via `adb shell cmd -w wifi <command>`
    - https://xdaforums.com/t/enable-init-d.4351731/
  -  Auto enable it on boot via `BroadcastReceiver`
    - https://stackoverflow.com/questions/5290141/android-broadcastreceiver-on-startup-keep-running-when-activity-is-in-backgrou
    - https://github.com/borconi/WifiLauncherforHUR/tree/master/app/src/main/java/com/borconi/emil/wifilauncherforhur/tethering

#### Steps
- [ ] Clean and minimize base app (nisargjhaveri/AAWirelessGateway)
  - At this stage, the app has to work with WifiLauncher in Server mode (Client connects to GW' Wifi)
- [ ] Button to enable hotspot
- [ ] `BroadcastReceiver` with a notification.
- [ ] `BroadcastReceiver` brings up AP
---
### Launcher App
This app will be on client phone and will make use of the `Request API Wifi` protocol, to connect to the hotspot (without routing the traffic through it), and will launch android auto pointing to the Wifi's GW.
- https://developer.android.com/develop/connectivity/wifi/wifi-bootstrap#java
- Wifi Credentials have to be in `SharedPreferences`
- QuickTile?
- Automation can be created if:
  - Credentials are present
  - Wifi is accesible/visible
  - Car Bluetooth is connected
   
#### Steps
- [ ] Android Preference Activity, with minimal framework (Basic `PreferenceActivity`)
- [ ] `SharedPreference` to store Wifi 'credentials'
- [ ] Request API WIFI connection and `AALaunch`
  - https://github.com/borconi/WifiLauncherforHUR/blob/master/app/src/main/java/com/borconi/emil/wifilauncherforhur/connectivity/Connector.java
- [ ] QuickTile Service
---
### Starter App
This app will live in GW. Via pair request or via intent will start the Bluetooth Native flow to launch AA on client.
- Take https://github.com/nisargjhaveri/AAWirelessGateway as base.
- Store in `SharedPreferences` last connected device (to wifi via Request Api)
- System App?
- Auto approve pairing
  - https://www.londatiga.net/it/programming/android/how-to-programmatically-pair-or-unpair-android-bluetooth-device/
  - https://stackoverflow.com/questions/35519321/android-bluetooth-pairing-without-user-enter-pin-and-confirmation-using-android

#### Steps
- [ ] Clean and minimize base app (nisargjhaveri/AAWirelessGateway)
- [ ] Launch Flow on already paired device
  - [ ] Save `WifiRequestInfo` recipient's bt address in `SharedPreferences`
- [ ] `BroadcastReceiver` To auto accept pairing.
- [ ] Initiate flow when pairing.
- [ ] Expose intent/action to initiate flow on last stored bt address
