## Android Auto Wirelesss Gateway
Complete solution for using an old phone (Fire Stick TV) as a Android Auto Wireless Dongle.

- **Gateway** app in `client mode`, needs:
  - App needs to be in `/system/priv-app` if auto connection (headless, omit first time aproval in gui) and UsbDevice gadget mode reset is wanted.
    - Provided the priv-app permissions.
  - Set some wifi (Master's hostpot) if headless (TV dongle) set wifi via adb:
    ```shell
    adb shell cmd -w add-network ssid wpa2 psk
    ```
  - HeadUnit server running on Master
  - Master's hostpot running
- **Gatway** app in `server mode`:
  - [X] Auto Pair
  - [X] LocalOnlyHotspot
  - [ ] Integrate everything 
