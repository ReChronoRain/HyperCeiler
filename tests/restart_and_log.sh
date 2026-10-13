#!/bin/bash
ADB="/d/build-tools/android-sdk/platform-tools/adb.exe"
"$ADB" logcat -c
"$ADB" shell am force-stop com.miui.home
sleep 3
"$ADB" shell input keyevent KEYCODE_HOME
sleep 12
"$ADB" logcat -d -s HyperCeiler.HomeLayout:I | grep -E "dart container|hook hits|caller va|slot 5" | head -40
