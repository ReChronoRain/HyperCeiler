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
import com.sevtinge.hyperceiler.utils.shell.ShellUtils.*
import java.util.*


// 设备信息相关
fun getSystemVersionIncremental(): String = if (getProp("ro.mi.os.version.incremental") != "") getProp("ro.mi.os.version.incremental") else getProp("ro.system.build.version.incremental")
fun getBuildDate(): String = getProp("ro.system.build.date")
fun getHost(): String = Build.HOST
fun getBuilder(): String = getProp("ro.build.user")
fun getBaseOs(): String = if (getProp("ro.build.version.base_os") != "") getProp("ro.build.version.base_os") else "null"
fun getRomAuthor(): String = getProp("ro.rom.author") + getProp("ro.romid")
fun getWhoAmI(): String = rootExecCmd("whoami")
fun getCurrentUserId(): Int = Process.myUserHandle().hashCode()

/**
 * 获取设备 Android 版本
 * @return 一个 Int 值
 */
fun getAndroidVersion(): Int = Build.VERSION.SDK_INT

/**
 * 获取小米设备 MIUI 版本
 * 将获取到的字符串转换为浮点，以提供判断
 * @return 一个 Float 值
 * HyperOS 2.0 已弃用
 */
fun getMiuiVersion(): Float = if (getProp("ro.miui.ui.version.name") == "V125") 12.5f else try { getProp("ro.miui.ui.version.code").toFloat() } catch (_: Exception) { -1f }

/**
 * 获取小米设备 HyperOS 版本
 * 将获取到的字符串转换为浮点，以提供判断
 * @return 一个 Float 值
 */
fun getHyperOSVersion(): Float = if (getProp("ro.mi.os.version.code") != null) try { getProp("ro.mi.os.version.code").toFloat() } catch (_: Exception) { -1f } else 0f

/**
 * 判断是否为指定某个 Android 版本
 * @param code 传入的 Android SDK Int 数值
 * @return 一个 Boolean 值
 */
fun isAndroidVersion(code: Int): Boolean = getAndroidVersion() == code

/**
 * 判断是否大于某个 Android 版本
 * @param code 传入的 Android SDK Int 数值
 * @return 一个 Boolean 值
 */
fun isMoreAndroidVersion(code: Int): Boolean = getAndroidVersion() >= code

/**
 * 判断是否为指定某个 MIUI 版本
 * @param code 传入的 MIUI 版本 Float 数值
 * @return 一个 Boolean 值
 * HyperOS 2.0 已弃用
 */
fun isMiuiVersion(code: Float): Boolean = getMiuiVersion() == code

/**
 * 判断是否大于某个 MIUI 版本
 * @param code 传入的 MIUI 版本 Float 数值
 * @return 一个 Boolean 值
 * HyperOS 2.0 已弃用
 */
fun isMoreMiuiVersion(code: Float): Boolean = getMiuiVersion() >= code

/**
 * 判断是否为指定某个 HyperOS 版本
 * @param code 传入的 HyperOS 版本 Float 数值
 * @return 一个 Boolean 值
 */
fun isHyperOSVersion(code: Float): Boolean = getHyperOSVersion() == code


/**
 * 判断是否大于某个 HyperOS 版本
 * @param code 传入的 HyperOS 版本 Float 数值
 * @return 一个 Boolean 值
 */
fun isMoreHyperOSVersion(code: Float): Boolean = getHyperOSVersion() >= code


private val mSupportMiuiVersion: List<Float> = mutableListOf(13.0f, 14.0f, 816.0f, 818.0f)
private val mSupportHyperOsVersion: List<Float> = mutableListOf(-1.0f, 1.0f, 2.0f)
private val mSupportAndroidVersion: List<Int> = mutableListOf(33, 34, 35)


fun isFullSupport(): Boolean {
    val isMiuiVersionSupport = mSupportMiuiVersion.contains(getMiuiVersion())
    val isHyperOsVersionSupport = mSupportHyperOsVersion.contains(getHyperOSVersion())
    val isAndroidVersionSupport = mSupportAndroidVersion.contains(getAndroidVersion())
    return isMiuiVersionSupport && isHyperOsVersionSupport && isAndroidVersionSupport
}