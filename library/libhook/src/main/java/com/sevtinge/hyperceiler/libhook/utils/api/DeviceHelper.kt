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

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.utils.api

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.os.Build
import android.os.Process
import com.sevtinge.hyperceiler.expansion.utils.TokenUtils.getDeviceToken
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Hardware.isPadDevice
import com.sevtinge.hyperceiler.libhook.utils.api.PropUtils.getProp
import com.sevtinge.hyperceiler.libhook.utils.hookapi.LazyClass.clazzMiuiBuild
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getStaticObjectFieldAsOrNull
import com.sevtinge.hyperceiler.libhook.utils.shell.ShellUtils.checkRootPermission
import com.sevtinge.hyperceiler.libhook.utils.shell.ShellUtils.rootExecCmd
import io.github.kyuubiran.ezxhelper.xposed.EzXposed.appContext
import java.io.File
import java.nio.charset.Charset
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

val IS_TABLET by lazy {
    clazzMiuiBuild.getStaticObjectFieldAsOrNull<Boolean>("IS_TABLET") ?: false
}

val IS_INTERNATIONAL_BUILD by lazy {
    clazzMiuiBuild.getStaticObjectFieldAsOrNull<Boolean>("IS_INTERNATIONAL_BUILD") ?: false
}

/**
 * 设备信息工具类
 */
object DeviceHelper {

    // ==================== Hardware 硬件信息 ====================

    /**
     * 硬件信息相关工具
     */
    object Hardware {
        @JvmStatic
        fun getFingerPrint(): String = Build.FINGERPRINT

        @JvmStatic
        fun getLocale(): String = getProp("ro.product.locale")

        @JvmStatic
        fun getLanguage(): String = Locale.getDefault().toString()

        @JvmStatic
        fun getBoard(): String = Build.BOARD

        @JvmStatic
        fun getSoc(): String = getProp("ro.soc.model")

        @JvmStatic
        fun getDeviceName(): String = Build.DEVICE

        @JvmStatic
        fun getMarketName(): String = getProp("ro.product.marketname")

        @JvmStatic
        fun getModelName(): String = Build.MODEL

        @JvmStatic
        fun getBrand(): String = Build.BRAND

        @JvmStatic
        fun getManufacturer(): String = Build.MANUFACTURER

        @JvmStatic
        fun getModDevice(): String = getProp("ro.product.mod_device")

        @JvmStatic
        fun getCharacteristics(): String = getProp("ro.build.characteristics")

        @JvmStatic
        fun getSerial(): String = rootExecCmd("getprop ro.serialno").replace("\n", "")

        @JvmStatic
        fun getCpuId(): String = removeLeadingZeros(rootExecCmd("getprop ro.boot.cpuid"))

        @JvmStatic
        fun getDensityDpi(): Int =
            (appContext.resources.displayMetrics.widthPixels / appContext.resources.displayMetrics.density).toInt()

        @SuppressLint("DiscouragedApi")
        @JvmStatic
        fun getCornerRadiusTop(): Int {
            val resourceId = appContext.resources.getIdentifier(
                "rounded_corner_radius_top", "dimen", "android"
            )
            return if (resourceId > 0) {
                appContext.resources.getDimensionPixelSize(resourceId)
            } else 100
        }

        @JvmStatic
        fun isTablet(): Boolean = Resources.getSystem().configuration.smallestScreenWidthDp >= 600

        @JvmStatic
        fun isPadDevice(): Boolean = isTablet() || DeviceTypeUtils.isFoldable()

        @JvmStatic
        fun isDarkMode(): Boolean =
            appContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

        @JvmStatic
        fun colorFilter(colorInt: Int) = BlendModeColorFilter(colorInt, BlendMode.SRC_IN)

        @JvmStatic
        fun getDeviceToken(androidId: String): String {
            val modelName = getModelName()
            val cpuId = getCpuId()
            val serial = getSerial()
            return getDeviceToken(modelName, serial, androidId, cpuId)
        }

        @JvmStatic
        fun removeLeadingZeros(input: String): String {
            var result = input
            while (result.startsWith("0") || result.startsWith("x")) {
                result = result.drop(1)
            }
            return result
        }
    }

    // ==================== Miui 小米设备判断 ====================

    /**
     * 小米设备特有的判断工具
     */
    object Miui {
        private const val CLASS_MIUI_BUILD: String = "miui.os.Build"

        private val isTablet: Boolean by lazy {
            InvokeUtils.getStaticField(CLASS_MIUI_BUILD, "IS_TABLET") as Boolean
        }

        private val isInternationalBuild: Boolean by lazy {
            InvokeUtils.getStaticField(CLASS_MIUI_BUILD, "IS_INTERNATIONAL_BUILD") as Boolean
        }

        /**
         * 判断是否为平板，仅支持小米设备
         * @return true 代表是平板，false 代表不是平板
         */
        @JvmStatic
        fun isPad(): Boolean {
            return runCatching {
                isTablet
            }.getOrElse {
                isPadDevice()
            }
        }

        /**
         * 判断是否为国际版系统，仅支持小米设备
         * @return true 代表是国际版系统，false 代表不是国际版系统
         */
        @JvmStatic
        fun isInternational(): Boolean {
            return runCatching {
                isInternationalBuild
            }.getOrElse {
                false
            }
        }
    }

    // ==================== System 系统版本判断 ====================

    /**
     * 系统版本相关工具
     */
    object System {
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
                0
            }
        }

        const val SUPPORT_NOT = 0
        const val SUPPORT_PARTIAL = 1
        const val SUPPORT_FULL = 2

        data class VersionInfo(
            val androidVersion: Int,
            val hyperOSVersion: Float,
            val smallVersion: Float,
            val supportStatus: Int
        ) {

            fun getAndroidVersion(): String {
                return when (androidVersion) {
                    35 -> "15"
                    36 -> "16"
                    37 -> "17"
                    else -> androidVersion.toString()
                }
            }

            @SuppressLint("DefaultLocale")
            fun getDisplayVersion(): String {
                val version = smallVersion
                val major = version.roundToInt()
                val decimal = version - major

                return if (decimal < 0.001f) {
                    String.format("%d.0", major)
                } else {
                    val patch = (decimal * 1000).roundToInt()
                    String.format("%d.0.%d", major, patch)
                }
            }

            fun matchesSmallVersion(other: Float): Boolean {
                return abs(smallVersion - other) < 0.001f
            }
        }

        private val versionList: List<VersionInfo> by lazy {
            listOf(
                // 已完全适配
                VersionInfo(35, 2.0f, 2.0f, SUPPORT_FULL),
                VersionInfo(35, 2.0f, 2.1f, SUPPORT_FULL),
                VersionInfo(35, 2.0f, 2.2f, SUPPORT_FULL),
                VersionInfo(36, 3.0f, 3.0f, SUPPORT_FULL),
                VersionInfo(36, 3.0f, 3.3f, SUPPORT_FULL),

                // 部分功能未适配
                VersionInfo(35, 3.0f, 3.0f, SUPPORT_PARTIAL),

                // 未适配
                VersionInfo(36, 2.0f, 2.23f, SUPPORT_NOT)
            )
        }

        private val fullSupportVersions: List<VersionInfo> by lazy {
            versionList.filter { it.supportStatus == SUPPORT_FULL }
        }

        private val partialSupportVersions: List<VersionInfo> by lazy {
            versionList.filter { it.supportStatus == SUPPORT_PARTIAL }
        }

        private val notSupportVersions: List<VersionInfo> by lazy {
            versionList.filter { it.supportStatus == SUPPORT_NOT }
        }

        @JvmStatic
        fun getSystemVersionIncremental(): String =
            getProp("ro.mi.os.version.incremental").ifEmpty { getProp("ro.system.build.version.incremental") }

        @JvmStatic
        fun getBuildDate(): String = getProp("ro.system.build.date")

        @JvmStatic
        fun getHost(): String = Build.HOST

        @JvmStatic
        fun getBuilder(): String = getProp("ro.build.user")

        @JvmStatic
        fun getBaseOs(): String = getProp("ro.build.version.base_os").ifEmpty { "null" }

        @JvmStatic
        fun getXmsVersion(): String = runCatching {
                getProp("persist.sys.xms.version")
            }.recoverCatching {
                getProp("ro.mi.xms.version.incremental")
            }.getOrElse { "null" }

        @JvmStatic
        fun getRomAuthor(): String = getProp("ro.rom.author") + getProp("ro.romid")

        @JvmStatic
        fun getWhoAmI(): String = rootExecCmd("whoami") ?: "unknown"

        @JvmStatic
        fun getRootGroupsInfo(): String = rootExecCmd("id") ?: "unknown"

        @JvmStatic
        fun getCurrentUserId(): Int = Process.myUserHandle().hashCode()

        @JvmStatic
        fun getAndroidVersion(): Int = androidSDK

        @JvmStatic
        fun getHyperOSVersion(): Float = hyperOSSDK

        @SuppressLint("DefaultLocale")
        @JvmStatic
        fun getSmallVersion(): Float =
            String.format("%.1f", hyperOSSDK + smallVersion * 0.001f).toFloatOrNull() ?: -1f

        @JvmStatic
        fun isSupportTelephony(context: Context): Boolean =
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)

        @JvmStatic
        fun isSupportWifi(context: Context): Boolean =
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI)

        /**
         * 判断是否为指定某个 Android 版本
         * @param code 传入的 Android SDK Int 数值
         * @return 一个 Boolean 值
         */
        @JvmStatic
        fun isAndroidVersion(code: Int): Boolean = androidSDK == code

        /**
         * 判断是否大于某个 Android 版本
         * @param code 传入的 Android SDK Int 数值
         * @return 一个 Boolean 值
         */
        @JvmStatic
        fun isMoreAndroidVersion(code: Int): Boolean = androidSDK >= code

        /**
         * 判断是否为指定某个 HyperOS 版本
         * @param code 传入的 HyperOS 版本 Float 数值
         * @return 一个 Boolean 值
         */
        @JvmStatic
        fun isHyperOSVersion(code: Float): Boolean = hyperOSSDK == code

        /**
         * 判断是否大于某个 HyperOS 版本
         * @param code 传入的 HyperOS 版本 Float 数值
         * @return 一个 Boolean 值
         */
        @JvmStatic
        fun isMoreHyperOSVersion(code: Float): Boolean = hyperOSSDK >= code

        /**
         * 判断是否为指定某个小版本
         * @param code 传入的小版本 Int 数值
         * @param osVersion 传入的 HyperOS 版本值
         * @return 一个 Boolean 值
         */
        @JvmStatic
        fun isMoreSmallVersion(code: Int, osVersion: Float): Boolean {
            return if (hyperOSSDK == osVersion) {
                smallVersion >= code
            } else {
                hyperOSSDK > osVersion
            }
        }

        @SuppressLint("DefaultLocale")
        @JvmStatic
        fun getVersionListText(status: Int): String {
            return getVersionsByStatus(status).joinToString("\n") { info ->
                "  • Android ${info.getAndroidVersion()} - HyperOS ${info.getDisplayVersion()}"
            }
        }

        @JvmStatic
        fun getVersionsByStatus(status: Int): List<VersionInfo> {
            return when (status) {
                SUPPORT_FULL -> fullSupportVersions
                SUPPORT_PARTIAL -> partialSupportVersions
                SUPPORT_NOT -> notSupportVersions
                else -> emptyList()
            }
        }

        @JvmStatic
        fun getSupportStatus(): Int {
            val currentAndroid = androidSDK
            val currentSmall = getSmallVersion()

            for (info in versionList) {
                if (info.androidVersion != currentAndroid) {
                    continue
                }
                if (info.matchesSmallVersion(currentSmall)) {
                    return info.supportStatus
                }
            }

            return SUPPORT_NOT
        }

    }

    // ==================== Module 模块扫描 ====================

    /**
     * Magisk/KernelSU 模块扫描工具
     */
    object Module {
        data class ModuleInfo(
            val moduleDir: String,
            val name: String,
            val version: String,
            val versionCode: String
        ) {
            fun extractName(): String = name

            fun formattedVersion(): String {
                val v = version.trim()
                val vc = versionCode.trim()

                if (v.isEmpty() && vc.isEmpty()) return ""
                if (v.isEmpty()) return vc
                if (vc.isEmpty()) return v

                return if (v.contains(vc)) v else "$v ($vc)"
            }
        }

        @JvmStatic
        fun scanModules(basePath: String = "/data/adb/modules", charset: Charset = Charsets.UTF_8): List<ModuleInfo> {
            if (checkRootPermission() != 0) return emptyList()

            val moduleDirs = rootExecCmd("sh -c 'ls -1 -- \"$basePath\"'")?.lineSequence()
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.toList() ?: return emptyList()

            return buildList {
                for (dirName in moduleDirs) {
                    val dirPath = "$basePath/$dirName"
                    val checkCmd = "sh -c '[ -f \"$dirPath/module.prop\" ] && [ -f \"$dirPath/daemon.apk\" ] && echo 1 || echo 0'"
                    if (rootExecCmd(checkCmd)?.trim() != "1") continue

                    val content = rootExecCmd("sh -c 'cat -- \"$dirPath/module.prop\"'") ?: continue

                    val tempFile = kotlin.io.path.createTempFile("module_prop_", ".tmp").toFile()
                    try {
                        tempFile.writeText(content, charset)
                        parseModuleProp(tempFile, dirPath, charset)?.let { add(it) }
                    } finally {
                        tempFile.delete()
                    }
                }
            }
        }

        @JvmStatic
        fun parseModuleProp(propFile: File, moduleDirPath: String, charset: Charset = Charsets.UTF_8): ModuleInfo? {
            if (!propFile.exists() || !propFile.isFile) return null
            val map = mutableMapOf<String, String>()

            try {
                propFile.useLines(charset) { lines ->
                    lines.forEach { line ->
                        val trimmed = line.trim()
                        if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEach
                        val idx = trimmed.indexOf('=')
                        if (idx <= 0) return@forEach
                        val key = trimmed.take(idx).trim()
                        val value = trimmed.substring(idx + 1).trim()
                        map[key] = value
                    }
                }
            } catch (_: Exception) {
                return null
            }

            fun getIgnoreCase(k: String): String =
                map.entries.firstOrNull { it.key.equals(k, ignoreCase = true) }?.value ?: ""

            val name = getIgnoreCase("name")
            val version = getIgnoreCase("version")
            val versionCode = getIgnoreCase("versionCode")

            return ModuleInfo(moduleDirPath, name, version, versionCode)
        }
    }
}
