## To use kamakiri:
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

## FireStick TV LineageOS headless Installation
```shell
# From TWRP!
# Ennable insecure ADB
adb shell twrp mount /system
adb shell twrp remountrw /system
# Maybe they where not there in the first place, add them!
adb shell "echo 'ro.secure=0 ' >> /system/build.prop"
adb shell "echo 'ro.adb.secure=0 ' >> /system/build.prop"
adb shell "echo 'persist.sys.usb.config=adb ' >> /system/build.prop"

# Reboot into system
adb reboot

# Skip setup wizard
adb shell settings put secure user_setup_complete 1
adb shell settings put global device_provisioned 1
adb shell cmd -w wifi add-network <SSID> wpa2 <PSK>
```

> **Download LOS18**: https://github.com/mt8695/android_device_amazon_sheldon/releases <br/> 
> **TWRP CLI**: https://twrp.me/faq/openrecoveryscript.html
