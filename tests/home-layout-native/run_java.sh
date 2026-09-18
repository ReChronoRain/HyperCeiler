#!/usr/bin/env bash
# Host test for the authenticated preference endpoint.
#
# The endpoint is compiled against minimal android.os / android.util /
# android.content.res stubs so its access checks and range validation can be
# exercised without a device.
set -euo pipefail
project_dir="$(cd "$(dirname "$0")/../.." && pwd)"
classes_dir="$(mktemp -d -t home-layout-endpoint)"
trap 'rm -r "$classes_dir"' EXIT
stubs="$project_dir/tests/home-layout-native/stubs"
javac -d "$classes_dir" \
    "$project_dir/tests/home-dock-window/stubs/android/os/Binder.java" \
    "$project_dir/tests/home-dock-window/stubs/android/os/IBinder.java" \
    "$project_dir/tests/home-dock-window/stubs/android/os/Parcel.java" \
    "$stubs/android/util/DisplayMetrics.java" \
    "$stubs/android/util/Log.java" \
    "$stubs/android/content/res/Resources.java" \
    "$stubs/android/content/Context.java" \
    "$stubs/android/content/ContentResolver.java" \
    "$stubs/android/net/Uri.java" \
    "$stubs/android/database/Cursor.java" \
    "$stubs/com/sevtinge/hyperceiler/common/utils/PrefsBridge.java" \
    "$stubs/com/sevtinge/hyperceiler/libhook/utils/api/ContextUtils.java" \
    "$project_dir/library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/dock/HomeLayoutNativeEndpointOS4.java" \
    "$project_dir/tests/home-layout-native/HomeLayoutNativeEndpointOS4Test.java"
java -cp "$classes_dir" com.sevtinge.hyperceiler.libhook.rules.home.dock.HomeLayoutNativeEndpointOS4Test
