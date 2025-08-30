package com.sevtinge.hyperceiler.hook.safe

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.sevtinge.hyperceiler.hook.module.HostConstant.HOST_HOME
import com.sevtinge.hyperceiler.hook.module.HostConstant.HOST_SECURITY_CENTER
import com.sevtinge.hyperceiler.hook.module.HostConstant.HOST_SYSTEM_UI
import com.sevtinge.hyperceiler.hook.utils.PropUtils
import com.sevtinge.hyperceiler.hook.utils.prefs.PrefsUtils

object SafeMode : RescuePartyPlus.CrashHandler {
    const val PROP_REPORT_PACKAGE = "persist.hyperceiler.crash.report"

    private fun onSafeMode(context: Context, pkgName: String): Boolean {
        if (isInSafeModeOnHook(pkgName) || isInSafeModeByProp(pkgName)) {
            return false
        }

        context.startActivity(Intent().apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            component = ComponentName(
                "com.sevtinge.hyperceiler",
                "com.sevtinge.hyperceiler.safemode.CrashActivity"
            )

            putExtra("key_is_need_set_prop", true)
            putExtra("key_pkg", pkgName)
        })

        return true
    }

    override fun onHandleCrash(context: Context, pkgName: String, mitigationCount: Int): Boolean {
        if (pkgName != HOST_SYSTEM_UI && pkgName != HOST_HOME) {
            return false
        }

        if (mitigationCount < 1) {
            return false
        }

        return onSafeMode(context, pkgName)
    }

    @JvmStatic
    fun isInSafeModeOnHook(pkgName: String): Boolean = when (pkgName) {
        HOST_SYSTEM_UI -> PrefsUtils.mPrefsMap.getBoolean("system_ui_safe_mode_enable")
        HOST_HOME -> PrefsUtils.mPrefsMap.getBoolean("home_safe_mode_enable")
        HOST_SECURITY_CENTER -> PrefsUtils.mPrefsMap.getBoolean("security_center_safe_mode_enable")
        else -> false
    }

    @JvmStatic
    fun isInSafeModeByProp(pkgName: String): Boolean {
        val data = PropUtils.getProp(PROP_REPORT_PACKAGE, "")
        if (pkgName.isEmpty()) {
            return false
        }

        return data.split(",").toHashSet().contains(pkgName)
    }
}
