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
package com.sevtinge.hyperceiler.hook.utils.devicesdk

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.os.Build
import com.sevtinge.hyperceiler.expansion.utils.TokenUtils.getDeviceToken
import com.sevtinge.hyperceiler.hook.utils.PropUtils.getProp
import com.sevtinge.hyperceiler.hook.utils.shell.ShellUtils.rootExecCmd
import io.github.kyuubiran.ezxhelper.xposed.EzXposed.appContext
import java.util.Locale

fun getFingerPrint(): String = Build.FINGERPRINT
fun getLocale(): String = getProp("ro.product.locale")
fun getLanguage(): String = Locale.getDefault().toString()
fun getBoard(): String = Build.BOARD
fun getSoc(): String = getProp("ro.soc.model")
fun getDeviceName(): String = Build.DEVICE
fun getMarketName(): String = getProp("ro.product.marketname")
fun getModelName(): String = Build.MODEL
fun getBrand(): String = Build.BRAND
fun getManufacturer(): String = Build.MANUFACTURER
fun getModDevice(): String = getProp("ro.product.mod_device")
fun getCharacteristics(): String = getProp("ro.build.characteristics")
fun getSerial(): String = rootExecCmd("getprop ro.serialno").replace("\n", "")
fun getCpuId(): String = removeLeadingZeros(rootExecCmd("getprop ro.boot.cpuid"))

fun getDensityDpi(): Int =
    (appContext.resources.displayMetrics.widthPixels / appContext.resources.displayMetrics.density).toInt()

@SuppressLint("DiscouragedApi")
fun getCornerRadiusTop(): Int {
    val resourceId = appContext.resources.getIdentifier(
        "rounded_corner_radius_top", "dimen", "android"
    )
    return if (resourceId > 0) {
        appContext.resources.getDimensionPixelSize(resourceId)
    } else 100
}

fun isTablet(): Boolean = Resources.getSystem().configuration.smallestScreenWidthDp >= 600
fun isPadDevice(): Boolean = isTablet() || DeviceType.isFoldable()
fun isDarkMode(): Boolean =
    appContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
fun colorFilter(colorInt: Int) = BlendModeColorFilter(colorInt, BlendMode.SRC_IN)

fun getDeviceToken(androidId : String): String {
    val modelName = getModelName()
    val cpuId = getCpuId()
    val serial = getSerial()

    return getDeviceToken(modelName, serial, androidId, cpuId)
}

fun removeLeadingZeros(input: String): String {
    var result = input
    while (result.startsWith("0") || result.startsWith("x")) {
        result = result.drop(1)
    }
    return result
}
