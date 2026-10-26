#!/bin/bash
ADB="/d/build-tools/android-sdk/platform-tools/adb.exe"
URI="content://com.sevtinge.hyperceiler.provider.sharedprefs"
set -x
"$ADB" shell setprop debug.hyperceiler.prefs_write 1
"$ADB" shell content call --uri "$URI" --method hc_debug_put --arg integer:home_layout_indicator_margin_bottom:100
"$ADB" shell content call --uri "$URI" --method hc_debug_put --arg boolean:home_layout_indicator_margin_bottom_enable:true
"$ADB" shell content query --uri "$URI/pref/integer/prefs_key_home_layout_indicator_margin_bottom"
"$ADB" shell content query --uri "$URI/pref/boolean/prefs_key_home_layout_indicator_margin_bottom_enable"
