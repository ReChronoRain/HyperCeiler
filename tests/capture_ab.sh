#!/bin/bash
# Turn the indicator knob off, relaunch the desktop, force one edit-mode cycle so the
# indicator offset is recomputed, then screenshot. That is the control frame for the A/B.
ADB="/d/build-tools/android-sdk/platform-tools/adb.exe"
URI="content://com.sevtinge.hyperceiler.provider.sharedprefs"
OUT="$1"
"$ADB" shell setprop debug.hyperceiler.prefs_write 1
"$ADB" shell content call --uri "$URI" --method hc_debug_put --arg boolean:home_layout_indicator_margin_bottom_enable:false
"$ADB" logcat -c
"$ADB" shell am force-stop com.miui.home
sleep 3
"$ADB" shell input keyevent KEYCODE_HOME
sleep 10
"$ADB" shell input swipe 600 1150 600 1150 1200
sleep 4
"$ADB" shell input keyevent KEYCODE_BACK
sleep 4
"$ADB" shell input keyevent KEYCODE_HOME
sleep 5
"$ADB" exec-out screencap -p > "$OUT"
echo "=== hits ==="
"$ADB" logcat -d -s HyperCeiler.HomeLayout:I | grep -E "hook hits" | tail -1 | grep -o "indicatorOffsetBottomPortrait=[0-9]* last=[0-9.-]* caller=0x[0-9a-f]* d=[0-9-]*"
