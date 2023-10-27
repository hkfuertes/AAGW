## FireStick Lite (sheldon/p) installation guide
```shell
# From TWRP!
# Ennable insecure ADB
adb shell mount /system_root
adb shell "sed -i 's|ro.secure=1|ro.secure=0|g' /system_root/system/build.prop"
adb shell "sed -i 's|ro.adb.secure=1|ro.adb.secure=0|g' /system_root/system/build.prop"
adb shell "echo 'persist.sys.usb.config=mtp,adb ' >> /system_root/system/build.prop"
# Maybe they where not there in the first place, add them!

# Data related changes
twrp mount data
mkdir /data/property
echo -n 'mtp,adb' > /data/property/persist.sys.usb.config

# Install systemizer and aapt

# Skip setup wizard
adb shell settings put secure user_setup_complete 1
adb shell settings put global device_provisioned 1

# Install APP
adb install Gateway.apk
```

> **Download LOS18**: https://github.com/mt8695/android_device_amazon_sheldon/releases <br/> 
> **TWRP CLI**: https://twrp.me/faq/openrecoveryscript.html
