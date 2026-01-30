/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.safecrash

import android.app.ApplicationErrorReport
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemProperties
import com.sevtinge.hyperceiler.libhook.callback.ICrashHandler
import com.sevtinge.hyperceiler.libhook.utils.api.PropUtils
import com.sevtinge.hyperceiler.libhook.utils.api.PropUtils.setProp
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsUtils

object SafeModeHandler : ICrashHandler {
    private const val TAG = "SafeModeHandler"
    private const val PROP_REPORT = "persist.service.hyperceiler.crash.report"

    override fun onCrashDetected(
        context: Context,
        pkgName: String,
        crashInfo: ApplicationErrorReport.CrashInfo?,
        longMsg: String?,
        stackTrace: String?
    ): Boolean {
        if (isInSafeModeByConfig(pkgName) || isInSafeModeByProp(pkgName)) {
            XposedLog.d(TAG, "$pkgName is already in safe mode, skipping UI launch.")
            return false
        }

        val alias = CrashScope.getAlias(pkgName) ?: return false

        return runCatching {
            val intent = Intent().apply {
                component = ComponentName("com.sevtinge.hyperceiler", "com.sevtinge.hyperceiler.safemode.CrashActivity")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP

                putExtra("key_is_need_set_prop", true)
                putExtra("key_pkg", alias)

                if (longMsg != null) putExtra("key_longMsg", longMsg)
                if (stackTrace != null) putExtra("key_stackTrace", stackTrace)

                crashInfo?.let {
                    putExtra("key_throwClassName", it.throwClassName)
                    putExtra("key_throwFileName", it.throwFileName)
                    putExtra("key_throwLineNumber", it.throwLineNumber)
                    putExtra("key_throwMethodName", it.throwMethodName)
                }
            }
            context.startActivity(intent)
            true
        }.getOrElse {
            XposedLog.e(TAG, "Failed to start CrashActivity, fallback to setprop", it)
            updateCrashProp(alias)
            true
        }
    }

    private fun isInSafeModeByConfig(pkgName: String): Boolean {
        return when (pkgName) {
            "com.android.systemui" -> PrefsUtils.mPrefsMap.getBoolean("system_ui_safe_mode_enable")
            "com.miui.home" -> PrefsUtils.mPrefsMap.getBoolean("home_safe_mode_enable")
            "com.android.settings" -> PrefsUtils.mPrefsMap.getBoolean("settings_safe_mode_enable")
            "com.miui.securitycenter" -> PrefsUtils.mPrefsMap.getBoolean("security_center_safe_mode_enable")
            else -> false
        }
    }

    private fun isInSafeModeByProp(pkgName: String): Boolean {
        val currentProp = PropUtils.getProp(PROP_REPORT, "")
        val alias = CrashScope.getAlias(pkgName) ?: return false
        return currentProp.split(",").contains(alias)
    }

    fun updateCrashProp(alias: String) {
        val current = PropUtils.getProp(PROP_REPORT, "")
        val newProp = if (current.isEmpty()) alias else "$current,$alias"

        val uniqueProp = newProp.split(",").distinct().joinToString(",")

        try {
            setProp(PROP_REPORT, uniqueProp)
        } catch (_: Throwable) {
            try {
                SystemProperties.set(PROP_REPORT, uniqueProp)
            } catch (ex: Exception) {
                XposedLog.e(TAG, "Failed to set prop", ex)
            }
        }
    }
}
