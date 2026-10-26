#!/bin/bash
# Control experiment: the hotseat-margin knob is the known-good lever that moves both the dock and
# the assistant capsule. If the screenshot diff cannot see THAT move, the A/B method is the problem,
# not the indicator knob. $1 = knob key stem, $2 = value, $3 = output png.
ADB="/d/build-tools/android-sdk/platform-tools/adb.exe"
URI="content://com.sevtinge.hyperceiler.provider.sharedprefs"
KEY="$1"; VALUE="$2"; OUT="$3"
"$ADB" shell setprop debug.hyperceiler.prefs_write 1
"$ADB" shell content call --uri "$URI" --method hc_debug_put --arg boolean:${KEY}_enable:true
"$ADB" shell content call --uri "$URI" --method hc_debug_put --arg integer:$KEY:"$VALUE"
"$ADB" logcat -c
"$ADB" shell am force-stop com.miui.home
sleep 3
"$ADB" shell input keyevent KEYCODE_HOME
sleep 12
"$ADB" exec-out screencap -p > "$OUT"
"$ADB" logcat -d -s HyperCeiler.HomeLayout:I | grep -E "hook hits" | tail -1
