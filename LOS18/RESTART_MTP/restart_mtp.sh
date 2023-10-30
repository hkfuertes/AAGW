#!/bin/sh

# Permissions on this file: 755, chown 0.0
svc usb resetUsbGadget
svc usb setFunctions
svc usb setFunctions mtp true