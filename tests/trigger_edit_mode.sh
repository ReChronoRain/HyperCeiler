#!/bin/bash
ADB="/d/build-tools/android-sdk/platform-tools/adb.exe"
# Long-press on an empty desktop area enters edit mode; both transitions call
# WorkspaceGetxController.startIndictorAnimation -> indicatorOffsetBottomPortrait.
"$ADB" shell input swipe 600 1150 600 1150 1200
sleep 4
"$ADB" shell input keyevent KEYCODE_BACK
sleep 4
"$ADB" shell input keyevent KEYCODE_HOME
sleep 4
echo "=== hook hits ==="
"$ADB" logcat -d -s HyperCeiler.HomeLayout:I | grep -E "hook hits" | tail -3
