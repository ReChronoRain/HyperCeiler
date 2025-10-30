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
package com.sevtinge.hyperceiler.hook.utils.pkg

import android.content.Context
import android.content.pm.PackageManager
import com.sevtinge.hyperceiler.hook.utils.prefs.PrefsUtils
import java.security.MessageDigest

object CheckModifyUtils {

    /**
     * 获取指定包名的检查结果
     * 非 hook 请使用带 context 的方法
     * 返回 true 表示被修改
     */
    fun getCheckResult(context: Context, pkg: String): Boolean {
        return PrefsUtils.getSharedBoolPrefs(context, "prefs_key_debug_mode_$pkg", false)
    }

    fun getCheckResult(pkg: String): Boolean {
        return PrefsUtils.mPrefsMap.getBoolean("debug_mode_$pkg")
    }

    /**
     * 手动设置指定包名的检查结果
     */
    fun setCheckResult(pkg: String, isModified: Boolean) {
        clearCheckResult(pkg)
        PrefsUtils.editor().putBoolean("prefs_key_debug_mode_$pkg", isModified).commit()
    }

    /**
     * 清除指定包名的检查结果
     */
    fun clearCheckResult(pkg: String) {
        PrefsUtils.editor().remove("prefs_key_debug_mode_$pkg").commit()
    }

    /**
     * 检查第三方 APK 文件（本地路径）签名是否在允许列表中。
     * 传入的 `context` 仅用于获取 `PackageManager`
     * 返回 true 表示被修改或签名不匹配/无法获取签名。
     */
    fun isApkModified(context: Context, apkFilePath: String, acceptedSignatures: List<String>): Boolean {
        val normalizedAccepted = acceptedSignatures
            .map { normalizeHexSignature(it) }
            .filter { it.isNotEmpty() }
            .toSet()
        if (normalizedAccepted.isEmpty()) return true

        val signatures = try {
            val pm = context.packageManager
            val pi = try {
                pm.getPackageArchiveInfo(apkFilePath, PackageManager.GET_SIGNING_CERTIFICATES)
            } catch (_: ClassCastException) {
                // 忽略某些 APK 解析时的系统内部错误
                null
            }
            // 某些 Android 版本需要设置以下两项以正确解析签名
            pi?.applicationInfo?.sourceDir = apkFilePath
            pi?.applicationInfo?.publicSourceDir = apkFilePath
            val signingInfo = pi?.signingInfo
            val certs = signingInfo?.let {
                if (it.hasMultipleSigners()) it.apkContentsSigners else it.signingCertificateHistory
            }
            certs?.map { it.toByteArray() } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        if (signatures.isEmpty()) return true

        for (sig in signatures) {
            val hex = sha256Hex(sig)
            if (hex in normalizedAccepted) return false
        }
        return true
    }

    fun isApkModified(context: Context, pkg: String, vararg acceptedSignatures: String): Boolean {
        return isApkModified(context, getApkFilePath(context, pkg), acceptedSignatures.asList())
    }

    private fun getApkFilePath(context: Context, pkg: String): String {
        return try {
            val pm = context.packageManager
            val ai = pm.getApplicationInfo(pkg, 0)
            ai.sourceDir ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    private fun sha256Hex(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02X".format(it) }
    }

    private fun normalizeHexSignature(s: String): String {
        return s.replace(":", "")
            .replace("\\s+".toRegex(), "")
            .uppercase()
    }

    const val XIAOMI_SIGNATURE = "C9:00:9D:01:EB:F9:F5:D0:30:2B:C7:1B:2F:E9:AA:9A:47:A4:32:BB:A1:73:08:A3:11:1B:75:D7:B2:14:90:25"
}
