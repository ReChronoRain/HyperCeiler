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

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.utils.devicesdk

import android.os.*
import com.sevtinge.hyperceiler.utils.PropUtils.*

// 设备信息相关
fun getSystemVersionIncremental(): String = getProp("ro.system.build.version.incremental")
fun getBuildDate(): String = getProp("ro.system.build.date")
fun getHost(): String = Build.HOST
fun getBuilder(): String = getProp("ro.build.user")
fun getBaseOs(): String = getProp("ro.build.version.base_os")
fun getRomAuthor(): String = getProp("ro.rom.author") + getProp("ro.romid")

/**
 * 获取设备 Android 版本
 * @return 一个 Int 值
 */
fun getAndroidVersion(): Int = Build.VERSION.SDK_INT

/**
 * 获取小米设备 MIUI 版本
 * 将获取到的字符串转换为浮点，以提供判断
 * @return 一个 Float 值
 */
fun getMiuiVersion(): Float {
    return when (getProp("ro.miui.ui.version.name")) {
        "V150" -> 15f
        "V140" -> 14f
        "V130" -> 13f
        "V125" -> 12.5f
        "V12" -> 12f
        "V11" -> 11f
        "V10" -> 10f
        else -> 0f
    }
}

/**
 * 获取小米设备 HyperOS 版本
 * 将获取到的字符串转换为浮点，以提供判断
 * @return 一个 Float 值
 */
fun getHyperOSVersion(): Float {
    return when (getProp("ro.mi.os.version.name")) {
        "OS2.0" -> 2f
        "OS1.0" -> 1f
        else -> 0f
    }
}

/**
 * 判断是否为指定某个 Android 版本
 * @param code 传入的 Android SDK Int 数值
 * @return 一个 Boolean 值
 */
fun isAndroidVersion(code: Int): Boolean {
    return getAndroidVersion() == code
}

/**
 * 判断是否大于某个 Android 版本
 * @param code 传入的 Android SDK Int 数值
 * @return 一个 Boolean 值
 */
fun isMoreAndroidVersion(code: Int): Boolean {
    return getAndroidVersion() >= code
}

/**
 * 判断是否为指定某个 MIUI 版本
 * @param code 传入的 MIUI 版本 Float 数值
 * @return 一个 Boolean 值
 */
fun isMiuiVersion(code: Float): Boolean {
    return getMiuiVersion() == code
}

/**
 * 判断是否大于某个 MIUI 版本
 * @param code 传入的 MIUI 版本 Float 数值
 * @return 一个 Boolean 值
 */
fun isMoreMiuiVersion(code: Float): Boolean {
    return getMiuiVersion() >= code
}

/**
 * 判断是否为指定某个 HyperOS 版本
 * @param code 传入的 HyperOS 版本 Float 数值
 * @return 一个 Boolean 值
 */
fun isHyperOSVersion(code: Float): Boolean{
    return getHyperOSVersion() == code
}

/**
 * 判断是否大于某个 HyperOS 版本
 * @param code 传入的 HyperOS 版本 Float 数值
 * @return 一个 Boolean 值
 */
fun isMoreHyperOSVersion(code: Float): Boolean {
    return getHyperOSVersion() >= code
}
