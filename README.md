## Android Auto Wirelesss Gateway
Complete solution for using an old phone (Fire Stick TV) as a Android Auto Wireless Dongle.

## Operation Modes
App needs to be in `/system/priv-app` for auto connection (headless, omit first time aproval in gui) and UsbDevice gadget mode reset.
- **Client**: The device connects to master wifi where HeadUnit Runs as server.
  - If headless use: `adb shell cmd -w add-network ssid wpa2 psk`
- **Server**:
  - The device has **AP** capabilities:
    - It will go through bonded Bluetooth devices trying to initiate Native Android Auto connection.
    - Regular (non root) `adb` has to be able to execute `ip -o link`
  - The device does not have **AP** capabilities:
    - It will need to be connected to an external Wifi (only device, no master)
    - `SSID, PSK, BSSID` have to be specified in the config file (TODO).
    - It will go through bonded Bluetooth devices trying to initiate Native Android Auto connection.

