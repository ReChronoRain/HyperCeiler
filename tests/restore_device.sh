#!/bin/bash
# Restore the device to the state the previous handover documented: indicator knob back to
# value=121 / disabled, the hotseat knob I touched during the control run back to disabled,
# and the debug write property off.
ADB="/d/build-tools/android-sdk/platform-tools/adb.exe"
URI="content://com.sevtinge.hyperceiler.provider.sharedprefs"
set -x
"$ADB" shell setprop debug.hyperceiler.prefs_write 1
"$ADB" shell content call --uri "$URI" --method hc_debug_put --arg integer:home_layout_indicator_margin_bottom:121
"$ADB" shell content call --uri "$URI" --method hc_debug_put --arg boolean:home_layout_indicator_margin_bottom_enable:false
"$ADB" shell content call --uri "$URI" --method hc_debug_put --arg boolean:home_layout_hotseats_margin_bottom_enable:false
"$ADB" shell content call --uri "$URI" --method hc_debug_put --arg integer:home_layout_hotseats_margin_bottom:70
"$ADB" shell setprop debug.hyperceiler.prefs_write 0
"$ADB" shell content query --uri "$URI/pref/integer/prefs_key_home_layout_indicator_margin_bottom"
"$ADB" shell content query --uri "$URI/pref/boolean/prefs_key_home_layout_indicator_margin_bottom_enable"
"$ADB" shell content query --uri "$URI/pref/boolean/prefs_key_home_layout_hotseats_margin_bottom_enable"
"$ADB" shell am force-stop com.miui.home
sleep 2
"$ADB" shell input keyevent KEYCODE_HOME
