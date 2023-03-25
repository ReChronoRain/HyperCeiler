package com.sevtinge.cemiuiler.utils.woobox

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import com.github.kyuubiran.ezxhelper.init.InitFields
import com.sevtinge.cemiuiler.utils.PrefsUtils.getSharedPrefs
import moralnorm.internal.utils.DeviceHelper
import java.io.DataOutputStream
import java.util.*

fun dp2px(dpValue: Float): Int = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP, dpValue, InitFields.appContext.resources.displayMetrics
).toInt()

fun px2dp(pxValue: Int): Int = (pxValue / InitFields.appContext.resources.displayMetrics.density + 0.5f).toInt()

fun getDensityDpi(): Int =
    (InitFields.appContext.resources.displayMetrics.widthPixels / InitFields.appContext.resources.displayMetrics.density).toInt()

fun isDarkMode(): Boolean =
    InitFields.appContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

@SuppressLint("PrivateApi")
@Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
fun getProp(mKey: String): String =
    Class.forName("android.os.SystemProperties").getMethod("get", String::class.java).invoke(Class.forName("android.os.SystemProperties"), mKey)
        .toString()

@SuppressLint("PrivateApi")
fun getProp(mKey: String, defaultValue: Boolean): Boolean =
    Class.forName("android.os.SystemProperties").getMethod("getBoolean", String::class.java, Boolean::class.javaPrimitiveType)
        .invoke(Class.forName("android.os.SystemProperties"), mKey, defaultValue) as Boolean

fun checkVersionName(): String = InitFields.appContext.packageManager.getPackageInfo(
    InitFields.appContext.packageName, 0
).versionName

fun isAlpha(): Boolean = InitFields.appContext.packageManager.getPackageInfo(
    InitFields.appContext.packageName, 0
).versionName.contains("ALPHA", ignoreCase = true)

fun isPadDevice(): Boolean = DeviceHelper.isTablet() || DeviceHelper.isFoldDevice()

fun atLeastAndroidS(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

fun atLeastAndroidT(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

fun checkVersionCode(): Long = InitFields.appContext.packageManager.getPackageInfo(
    InitFields.appContext.packageName, 0
).longVersionCode

fun checkMiuiVersion(): Float = when (getProp("ro.miui.ui.version.name")) {
    "V140" -> 14f
    "V130" -> 13f
    "V125" -> 12.5f
    "V12" -> 12f
    "V11" -> 11f
    "V10" -> 10f
    else -> 0f
}

fun checkAndroidVersion(): String = getProp("ro.build.version.release")

/**
 * 执行 Shell 命令
 * @param command Shell 命令
 */
fun execShell(command: String) {
    try {
        val p = Runtime.getRuntime().exec("su")
        val outputStream = p.outputStream
        val dataOutputStream = DataOutputStream(outputStream)
        dataOutputStream.writeBytes(command)
        dataOutputStream.flush()
        dataOutputStream.close()
        outputStream.close()
    } catch (t: Throwable) {
        t.printStackTrace()
    }
}

@SuppressLint("DiscouragedApi")
fun getCornerRadiusTop(): Int {
    val resourceId = InitFields.appContext.resources.getIdentifier(
        "rounded_corner_radius_top", "dimen", "android"
    )
    return if (resourceId > 0) {
        InitFields.appContext.resources.getDimensionPixelSize(resourceId)
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