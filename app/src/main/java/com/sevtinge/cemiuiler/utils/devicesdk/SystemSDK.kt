package com.sevtinge.cemiuiler.utils.devicesdk

import android.os.Build

/**
获取设备 Android 版本 、MIUI 版本
并判断设备指定类型
 */
fun getAndroidVersion(): Int = Build.VERSION.SDK_INT

fun isAndroidR(): Boolean = getAndroidVersion() == Build.VERSION_CODES.R
fun isAndroidS(): Boolean = getAndroidVersion() == Build.VERSION_CODES.S
fun isAndroidSv2(): Boolean = getAndroidVersion() == Build.VERSION_CODES.S_V2
fun isAndroidT(): Boolean = getAndroidVersion() == Build.VERSION_CODES.TIRAMISU
fun isAndroidU(): Boolean = getAndroidVersion() == Build.VERSION_CODES.UPSIDE_DOWN_CAKE

fun isMoreAndroidVersion(version: Int): Boolean {
    return getAndroidVersion() >= version
}

fun getMiuiVersion(): Float = when (getProp("ro.miui.ui.version.name")) {
    "V150" -> 15f
    "V140" -> 14f
    "V130" -> 13f
    "V125" -> 12.5f
    "V12" -> 12f
    "V11" -> 11f
    "V10" -> 10f
    else -> 0f
}

fun isMoreMiuiVersion(version: Float): Boolean {
    return getMiuiVersion() >= version
}
