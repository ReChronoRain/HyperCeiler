#!/bin/bash
# Turn every layout knob off and take the control frame.
ADB="/d/build-tools/android-sdk/platform-tools/adb.exe"
URI="content://com.sevtinge.hyperceiler.provider.sharedprefs"
OUT="$1"
"$ADB" shell setprop debug.hyperceiler.prefs_write 1
"$ADB" shell setprop debug.hyperceiler.layout.override 0
"$ADB" shell setprop debug.hyperceiler.layout.hook5 off
"$ADB" shell content call --uri "$URI" --method hc_debug_put --arg boolean:home_layout_indicator_margin_bottom_enable:false
"$ADB" shell content call --uri "$URI" --method hc_debug_put --arg boolean:home_layout_hotseats_margin_bottom_enable:false
"$ADB" logcat -c
"$ADB" shell am force-stop com.miui.home
sleep 3
"$ADB" shell input keyevent KEYCODE_HOME
sleep 12
"$ADB" exec-out screencap -p > "$OUT"
echo "(control frame captured)"
