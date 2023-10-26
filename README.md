## Android Auto Wirelesss Gateway
Complete solution for using an old phone (Fire Stick TV) as a Android Auto Wireless Dongle.
Three apps:
- **Main GW** app, minimal, minimal intervention, using https://github.com/nisargjhaveri/AAWirelessGateway an app with no configuration that will listen on a socket for AA compatible stream when android auto is detected
  - HotSpot details preconfigured via `adb shell`, and auto start? if not, hook on boot on an activity to launch it.
- **Launcher** App, for the client phone. This app will make use of the `Request API Wifi` protocol, to connect to the hotspot (without routing the traffic through it), and will launch android auto pointing to the Wifi GW
  - Automation can be created if:
    - Wifi is present && car Bluetooth is connected
    - Wifi network is known and stored in preferences (not connected to manually)
- **Starter** App, present on the gateway, and exposing an intent action to be executed from other app. This app will launch on Bluetooth connection attempt, and will approve the incoming bluetooth connection, and send over bluetooth the Request API Wifi info, so that Android Auto can automatically start.
- Again see: https://github.com/nisargjhaveri/AAWirelessGateway
- Auto approve bluetooth connection?
  - https://www.londatiga.net/it/programming/android/how-to-programmatically-pair-or-unpair-android-bluetooth-device/
  - https://stackoverflow.com/questions/35519321/android-bluetooth-pairing-without-user-enter-pin-and-confirmation-using-android
