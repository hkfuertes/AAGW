## FireStick Lite (sheldon/p) installation guide
```shell
# From TWRP!
# Ennable insecure ADB
twrp mount /system
adb shell "sed -i 's|ro.secure=1|ro.secure=0|g' /system/build.prop"
adb shell "sed -i 's|ro.adb.secure=1|ro.adb.secure=0|g' /system/build.prop"
adb shell "echo 'persist.sys.usb.config=adb ' >> /system/build.prop"
# Maybe they where not there in the first place, add them!

# Data related changes
twrp mount data
mkdir /data/property
echo -n 'mtp,adb' > /data/property/persist.sys.usb.config

# Skip setup wizard
adb shell settings put secure user_setup_complete 1
adb shell settings put global device_provisioned 1

# Install APP fom System
adb install Gateway.apk

# Convert from TWRP
twrp mount /system
adb shell mkdir -p /system/priv-app/net.mfuertes.aagw.gateway/
adb shell chmod 0755 /system/priv-app/net.mfuertes.aagw.gateway/
adb push Gateway.apk /system/priv-app/net.mfuertes.aagw.gateway/base.apk
adb shell chmod 0644 /system/priv-app/net.mfuertes.aagw.gateway/base.apk
adb push privapp-permissions-net.mfuertes.aagw.gateway.xml /system/etc/permissions/
```

> **Download LOS18**: https://github.com/mt8695/android_device_amazon_sheldon/releases <br/> 
> **TWRP CLI**: https://twrp.me/faq/openrecoveryscript.html
