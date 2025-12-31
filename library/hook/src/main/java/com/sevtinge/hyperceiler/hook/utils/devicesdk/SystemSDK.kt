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
package com.sevtinge.hyperceiler.hook.utils.devicesdk

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import com.sevtinge.hyperceiler.hook.utils.PropUtils.getProp
import com.sevtinge.hyperceiler.hook.utils.shell.ShellUtils.checkRootPermission
import com.sevtinge.hyperceiler.hook.utils.shell.ShellUtils.rootExecCmd
import java.io.File
import java.nio.charset.Charset

// 设备信息相关
fun getSystemVersionIncremental(): String = getProp("ro.mi.os.version.incremental").ifEmpty { getProp("ro.system.build.version.incremental") }
fun getBuildDate(): String = getProp("ro.system.build.date")
fun getHost(): String = Build.HOST
fun getBuilder(): String = getProp("ro.build.user")
fun getBaseOs(): String = getProp("ro.build.version.base_os").ifEmpty { "null" }
fun getRomAuthor(): String = getProp("ro.rom.author") + getProp("ro.romid")
fun getWhoAmI(): String = rootExecCmd("whoami") ?: "unknown"
fun getRootGroupsInfo(): String = rootExecCmd("id") ?: "unknown"
fun getCurrentUserId(): Int = Process.myUserHandle().hashCode()
// 仅获取设备信息，不要用于判断
fun getAndroidVersion(): Int = androidSDK
fun getHyperOSVersion(): Float = hyperOSSDK
@SuppressLint("DefaultLocale")
fun getSmallVersion(): Float = String.format("%.1f", hyperOSSDK + smallVersion * 0.001f).toFloatOrNull() ?: -1f
fun isSupportTelephony(context: Context): Boolean = context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
fun isSupportWifi(context: Context): Boolean = context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI)

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
    mutableListOf(1.0f)
}
private val mSupportSmallVersion: List<Float> by lazy {
    mutableListOf(2.0f, 2.1f, 2.2f)
}
private val mSupportAndroidVersion: List<Int> by lazy {
    mutableListOf(34, 35)
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
    return if (hyperOSSDK > osVersion) {
        true
    } else if (isMoreHyperOSVersion(osVersion)) {
        smallVersion >= code
    } else {
        false
    }
}

@SuppressLint("DefaultLocale")
fun isFullSupport(): Boolean {
    val isBigVersionSupport = mSupportHyperOsVersion.contains(hyperOSSDK)
    val isSmallVersionSupport = mSupportSmallVersion.contains(getSmallVersion())
    val isAndroidVersionSupport = mSupportAndroidVersion.contains(androidSDK)

    val isHyperOsVersionSupport = if (hyperOSSDK >= 2f) {
        isSmallVersionSupport
    } else {
        isBigVersionSupport
    }
    return isHyperOsVersionSupport && isAndroidVersionSupport
}

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
            /*val checkPkgCmd = "sh -c 'aapt dump badging \"$dirPath/daemon.apk\" 2>/dev/null | grep -q \"package: name=\\'org.lsposed.daemon\\'\" && echo 1 || echo 0'"
            if (rootExecCmd(checkPkgCmd)?.trim() != "1") continue*/

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
