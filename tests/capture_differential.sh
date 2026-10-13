#!/bin/bash
# Differential experiment.
#   slot 0 (hotseat margin)  = +delta   -> moves dock icons + capsule + dock window
#   slot 5 retargeted via debug.hyperceiler.layout.hook5 to a dock-window-only accessor with
#   -delta              -> moves only whatever that accessor feeds
# If the capsule snaps back to the baseline while the dock stays moved, the capsule's Y comes from
# the dock-window side, and the inverse combination is the independent capsule lever.
#   $1 = slot5 symbol ("off" = no retarget), $2 = slot5 value, $3 = hotseat value, $4 = output png
ADB="/d/build-tools/android-sdk/platform-tools/adb.exe"
URI="content://com.sevtinge.hyperceiler.provider.sharedprefs"
SYMBOL="$1"; IND_VALUE="$2"; HS_VALUE="$3"; OUT="$4"
"$ADB" shell setprop debug.hyperceiler.prefs_write 1
"$ADB" shell setprop debug.hyperceiler.layout.override 1
if [ "$SYMBOL" = "off" ]; then
  "$ADB" shell setprop debug.hyperceiler.layout.hook5 off
else
  "$ADB" shell setprop debug.hyperceiler.layout.hook5 "$SYMBOL"
fi
"$ADB" shell content call --uri "$URI" --method hc_debug_put --arg boolean:home_layout_indicator_margin_bottom_enable:true
"$ADB" shell content call --uri "$URI" --method hc_debug_put --arg integer:home_layout_indicator_margin_bottom:"$IND_VALUE"
if [ "$HS_VALUE" = "off" ]; then
  "$ADB" shell content call --uri "$URI" --method hc_debug_put --arg boolean:home_layout_hotseats_margin_bottom_enable:false
else
  "$ADB" shell content call --uri "$URI" --method hc_debug_put --arg boolean:home_layout_hotseats_margin_bottom_enable:true
  "$ADB" shell content call --uri "$URI" --method hc_debug_put --arg integer:home_layout_hotseats_margin_bottom:"$HS_VALUE"
fi
"$ADB" logcat -c
"$ADB" shell am force-stop com.miui.home
sleep 3
"$ADB" shell input keyevent KEYCODE_HOME
sleep 12
"$ADB" shell input swipe 600 1150 600 1150 1200
sleep 4
"$ADB" shell input keyevent KEYCODE_BACK
sleep 4
"$ADB" shell input keyevent KEYCODE_HOME
sleep 5
"$ADB" exec-out screencap -p > "$OUT"
"$ADB" logcat -d -s HyperCeiler.HomeLayout:I | grep -E "hook hits" | tail -1
