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

import android.annotation.*
import android.content.*
import android.content.pm.*
import android.content.res.*
import android.graphics.*
import android.text.*
import android.util.*
import android.util.Log
import com.github.kyuubiran.ezxhelper.*
import com.sevtinge.hyperceiler.utils.*
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils.*
import moralnorm.internal.utils.*
import java.util.*

fun dp2px(dpValue: Float): Int = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP, dpValue, EzXHelper.appContext.resources.displayMetrics
).toInt()

fun px2dp(pxValue: Int): Int = (pxValue / EzXHelper.appContext.resources.displayMetrics.density + 0.5f).toInt()

fun getDensityDpi(): Int =
    (EzXHelper.appContext.resources.displayMetrics.widthPixels / EzXHelper.appContext.resources.displayMetrics.density).toInt()

fun isDarkMode(): Boolean =
    EzXHelper.appContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

fun colorFilter(colorInt: Int) = BlendModeColorFilter(colorInt, BlendMode.SRC_IN)

fun getPackageInfoCompat(packageName: String, flags: Int = 0): PackageInfo =
    EzXHelper.appContext.packageManager.getPackageInfo(
        packageName,
        PackageManager.PackageInfoFlags.of(flags.toLong())
    )

fun checkVersionName(): String = getPackageInfoCompat(EzXHelper.appContext.packageName).versionName

fun isAlpha(): Boolean =
    getPackageInfoCompat(EzXHelper.appContext.packageName).versionName.contains("ALPHA", ignoreCase = true)

fun isTablet(): Boolean = Resources.getSystem().configuration.smallestScreenWidthDp >= 600

fun isPadDevice(): Boolean = isTablet() || DeviceHelper.isFoldDevice()

fun checkVersionCode(): Long = getPackageInfoCompat(EzXHelper.appContext.packageName).longVersionCode

fun checkAndroidVersion(): String = PropUtils.getProp("ro.build.version.release")

@SuppressLint("DiscouragedApi")
fun getCornerRadiusTop(): Int {
    val resourceId = EzXHelper.appContext.resources.getIdentifier(
        "rounded_corner_radius_top", "dimen", "android"
    )
    return if (resourceId > 0) {
        EzXHelper.appContext.resources.getDimensionPixelSize(resourceId)
    } else 100
}

fun setLocale(context: Context, locale: Locale): Context {
    var tmpLocale: Locale = locale
    if ("und" == locale.toLanguageTag() || "system" == locale.toLanguageTag()) {
        tmpLocale = Resources.getSystem().configuration.locales[0]
    }
    val configuration = context.resources.configuration
    configuration.setLocale(tmpLocale)
    Log.d("AppUtil", "setLocale: ${tmpLocale.toLanguageTag()}")
    // if (atLeastAndroidT()) {
    //     AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tmpLocale.toLanguageTag()))
    // }
    return context.createConfigurationContext(configuration)
}

fun getLocale(context: Context): Locale {
    val pref = getSharedPrefs(context, true)
    val tag: String? = pref.getString("prefs_key_settings_language", "SYSTEM")
    Log.d("AppUtil", "getLocale: tag=$tag")
    return if (tag == null || TextUtils.isEmpty(tag) || "SYSTEM" == tag) {
        val sysLang = Resources.getSystem().configuration.locales[0].toLanguageTag().trim()
        Log.d("AppUtil", "getLocale: sysLang=$sysLang")
        Locale.forLanguageTag(sysLang)
    } else Locale.forLanguageTag(tag)
}
