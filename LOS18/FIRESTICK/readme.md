To use kamakiri:
```shell
sudo apt install python3 python3-serial python3-usb adb fastboot dos2unix
sudo systemctl stop ModemManager
sudo systemctl disable ModemManager
```

```shell
# /data/vendor/wifi/hostapd
# /vendor/bin/hw/hostapd
find /system /vendor /odm /product -name hostapd 2>/dev/null
```