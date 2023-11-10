package com.sevtinge.hyperceiler.utils.devicesdk

import android.os.Build

/**
获取设备 Android 版本 、MIUI 版本 、HyperOS 版本
并判断设备指定类型
 */

// ----- Android ----------------------------------------------------------------------------------

fun getAndroidVersion(): Int = Build.VERSION.SDK_INT

fun isAndroidVersion(versioncode: Int): Boolean{
    val result: Boolean = when (versioncode) {
        33 -> (getAndroidVersion() == Build.VERSION_CODES.TIRAMISU)
        34 -> (getAndroidVersion() == Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        else -> false
    };
    return result
}

fun isMoreAndroidVersion(version: Int): Boolean {
    return getAndroidVersion() >= version
}

// ----- MIUI -------------------------------------------------------------------------------------

fun getMiuiVersion(): Float = when (getProp("ro.miui.ui.version.name")) {
    "V150" -> 15f
    "V140" -> 14f
    "V130" -> 13f
    else -> 0f
}

fun isMiuiVersion(versioncode: Float): Boolean{
    val result: Boolean = when (versioncode) {
        13f -> (getProp("ro.miui.ui.version.name") == "V130")
        14f -> (getProp("ro.miui.ui.version.name") == "V140")
        15f -> (getProp("ro.miui.ui.version.name") == "V150")
        else -> false
    };
    return result
}

fun isMoreMiuiVersion(version: Float): Boolean {
    return getMiuiVersion() >= version
}

// ----- HyperOS ----------------------------------------------------------------------------------

fun getHyperOSVersion(): Float = when (getProp("ro.mi.os.version.name")) {
    "OS1.0" -> 1f
    else -> 0f
}

fun isHyperOSVersion(versioncode: Float): Boolean{
    val result: Boolean = when (versioncode) {
        1f -> (getProp("ro.mi.os.version.name") == "OS1.0")
        else -> false
    };
    return result
}

fun isMoreHyperOSVersion(version: Float): Boolean {
    return getHyperOSVersion() >= version
}
