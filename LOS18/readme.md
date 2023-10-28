## LOS18 headless installation guide
```shell
# From TWRP!
# Ennable insecure ADB
twrp mount /system
adb shell "sed -i 's|ro.secure=1|ro.secure=0|g' /system/build.prop"
adb shell "sed -i 's|ro.adb.secure=1|ro.adb.secure=0|g' /system/build.prop"
adb shell "echo 'persist.sys.usb.config=adb ' >> /system/build.prop"

adb shell twrp remountrw /system
# Maybe they where not there in the first place, add them!
adb shell "echo 'ro.secure=0 ' >> /system/build.prop"
adb shell "echo 'ro.adb.secure=0 ' >> /system/build.prop"
adb shell "echo 'persist.sys.usb.config=adb ' >> /system/build.prop"

# Data related changes
adb shell twrp mount data
adb shell "mkdir -p /data/property"
adb shell "echo -n 'mtp,adb' > /data/property/persist.sys.usb.config"

# Reboot into system
adb reboot

# Skip setup wizard
adb shell settings put secure user_setup_complete 1
adb shell settings put global device_provisioned 1

# Install APP fom System
adb install Gateway.apk

# Reboot into TWRP
adb reboot recovery

# Convert from TWRP
adb push Gateway.apk /sdcard
adb push privapp-permissions-net.mfuertes.aagw.gateway.xml /sdcard

adb shell twrp mount /system
adb shell twrp remountrw /system
adb shell mkdir -p /system/priv-app/net.mfuertes.aagw.gateway/
adb shell chmod 0755 /system/priv-app/net.mfuertes.aagw.gateway
adb shell cp /sdcard/Gateway.apk /system/priv-app/net.mfuertes.aagw.gateway/base.apk
adb shell chmod 0644 /system/priv-app/net.mfuertes.aagw.gateway/base.apk
adb shell cp /sdcard/privapp-permissions-net.mfuertes.aagw.gateway.xml /system/etc/permissions/

# Auto start AP
adb shell twrp mount /system
adb shell twrp remountrw /system
adb push softap.rc /system/etc/init/
adb push softap_start.sh /system/bin/
```

> **Download LOS18**: https://github.com/mt8695/android_device_amazon_sheldon/releases <br/> 
> **TWRP CLI**: https://twrp.me/faq/openrecoveryscript.html
