#!/bin/sh
# Permissions on this file: 755, chown 0.0

SSID=AAWG
PASS_KEY=1234567890

cmd -w wifi set-wifi-enabled disabled
# cmd -w wifi start-softap $SSID wpa2 $PASS_KEY 5|bridged
cmd -w wifi start-softap $SSID wpa2 $PASS_KEY
