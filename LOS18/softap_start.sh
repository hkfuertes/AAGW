#!/bin/sh
# Permissions on this file: 755, chown 0.0

export SSID=AAWG
export PASS_KEY=12334567890

cmd -w wifi start-softap $SSID wpa2 $PASS_KEY bridge
