## To make SoftAP auto start on boot
```shell
# Reboot into TWRP
adb reboot recovery

# Auto start AP
adb shell twrp mount /system
adb shell twrp remountrw /system
adb push softap.rc /system/etc/init/
adb push softap_start.sh /system/bin/
```