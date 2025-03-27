/*
  * This file is part of HyperCeiler.

  * HyperCeiler is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation, either version 3 of the
  * License.

  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.

  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <https://www.gnu.org/licenses/>.

  * Copyright (C) 2023-2025 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.utils.devicesdk

import android.os.Build
import android.os.Process
import com.sevtinge.hyperceiler.utils.PropUtils.getProp
import com.sevtinge.hyperceiler.utils.shell.ShellUtils.rootExecCmd


// 设备信息相关
fun getSystemVersionIncremental(): String = getProp("ro.mi.os.version.incremental").ifEmpty { getProp("ro.system.build.version.incremental") }
fun getBuildDate(): String = getProp("ro.system.build.date")
fun getHost(): String = Build.HOST
fun getBuilder(): String = getProp("ro.build.user")
fun getBaseOs(): String = getProp("ro.build.version.base_os").ifEmpty { "null" }
fun getRomAuthor(): String = getProp("ro.rom.author") + getProp("ro.romid")
fun getWhoAmI(): String = rootExecCmd("whoami")
fun getCurrentUserId(): Int = Process.myUserHandle().hashCode()
// 仅获取设备信息，不要用于判断
fun getAndroidVersion(): Int = androidSDK
fun getHyperOSVersion(): Float = hyperOSSDK

private val androidSDK: Int by lazy {
    Build.VERSION.SDK_INT
}
private val hyperOSSDK: Float by lazy {
    getProp("ro.mi.os.version.code").toFloatOrNull() ?: 0f
}
private val smallVersion: Int by lazy {
    val versionParts = getSystemVersionIncremental().split(".")
    if (versionParts.size >= 3) {
        versionParts[2].toIntOrNull() ?: 0
    } else {
        0 // 当格式不匹配预期时返回默认值
    }
}
private val mSupportHyperOsVersion: List<Float> by lazy {
    mutableListOf(-1.0f, 1.0f, 2.0f)
}
private val mSupportAndroidVersion: List<Int> by lazy {
    mutableListOf(33, 34, 35)
}

/**
 * 判断是否为指定某个 Android 版本
 * @param code 传入的 Android SDK Int 数值
 * @return 一个 Boolean 值
 */
fun isAndroidVersion(code: Int): Boolean = androidSDK == code

/**
 * 判断是否大于某个 Android 版本
 * @param code 传入的 Android SDK Int 数值
 * @return 一个 Boolean 值
 */
fun isMoreAndroidVersion(code: Int): Boolean = androidSDK >= code


/**
 * 判断是否为指定某个 HyperOS 版本
 * @param code 传入的 HyperOS 版本 Float 数值
 * @return 一个 Boolean 值
 */
fun isHyperOSVersion(code: Float): Boolean = hyperOSSDK == code


/**
 * 判断是否大于某个 HyperOS 版本
 * @param code 传入的 HyperOS 版本 Float 数值
 * @return 一个 Boolean 值
 */
fun isMoreHyperOSVersion(code: Float): Boolean = hyperOSSDK >= code


/**
 * 判断是否为指定某个小版本
 * @param code 传入的小版本 Int 数值
 * @param osVersion 传入的 HyperOS 版本值
 * @return 一个 Boolean 值
 */
fun isMoreSmallVersion(code: Int, osVersion: Float): Boolean {
    return if (isMoreHyperOSVersion(osVersion)) {
        smallVersion >= code
    } else {
        false
    }
}

fun isFullSupport(): Boolean {
    val isHyperOsVersionSupport = mSupportHyperOsVersion.contains(hyperOSSDK)
    val isAndroidVersionSupport = mSupportAndroidVersion.contains(androidSDK)
    return isHyperOsVersionSupport && isAndroidVersionSupport
}