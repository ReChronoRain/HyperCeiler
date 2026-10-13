#!/bin/bash
# A/B in edit mode: startIndictorAnimation is one of the two consumers, and edit mode is where the
# page indicator is actually drawn. $1 = enable(0/1), $2 = value, $3 = output png.
ADB="/d/build-tools/android-sdk/platform-tools/adb.exe"
URI="content://com.sevtinge.hyperceiler.provider.sharedprefs"
ENABLE="$1"; VALUE="$2"; OUT="$3"
"$ADB" shell setprop debug.hyperceiler.prefs_write 1
if [ "$ENABLE" = "1" ]; then
  "$ADB" shell content call --uri "$URI" --method hc_debug_put --arg integer:home_layout_indicator_margin_bottom:"$VALUE"
fi
"$ADB" shell content call --uri "$URI" --method hc_debug_put --arg boolean:home_layout_indicator_margin_bottom_enable:"$ENABLE"
"$ADB" logcat -c
"$ADB" shell am force-stop com.miui.home
sleep 3
"$ADB" shell input keyevent KEYCODE_HOME
sleep 10
# enter edit mode and stay there
"$ADB" shell input swipe 600 1150 600 1150 1200
sleep 6
"$ADB" exec-out screencap -p > "$OUT"
echo "=== last hits line ==="
"$ADB" logcat -d -s HyperCeiler.HomeLayout:I | grep -E "hook hits" | tail -1 | tr ' ' '\n' | grep -A0 -E "indicatorOffsetBottomPortrait" -A4 | tr '\n' ' '
echo
