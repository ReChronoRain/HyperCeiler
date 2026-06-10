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
package com.sevtinge.hyperceiler.libhook.rules.securitycenter.app

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.UserHandle
import com.sevtinge.hyperceiler.libhook.R
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.MiuixPreferenceUtils
import io.github.lingqiqi5211.ezhooktool.core.callStaticMethod
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook

@SuppressLint("DiscouragedApi")
object AddAppInfoEntry : BaseHook() {
    private const val KEY_AOSP_APP_INFO = "hyperceiler_aosp_app_info_pref"
    private const val KEY_OPEN_BY_DEFAULT = "app_default_pref"

    override fun init() {
        val detailsFragmentClass =
            findClassIfExists("com.miui.appmanager.fragment.ApplicationsDetailsFragment") ?: return

        detailsFragmentClass.findMethod {
            name("onCreatePreferences")
            parameterTypes(Bundle::class.java, String::class.java)
        }.createAfterHook {
            addAppInfoPreference(it.thisObject)
        }
    }

    private fun addAppInfoPreference(fragment: Any) {
        if (callMethod(fragment, "findPreference", KEY_AOSP_APP_INFO) != null) return

        val openByDefaultPref =
            callMethod(fragment, "findPreference", KEY_OPEN_BY_DEFAULT) ?: return
        val parent = callMethod(openByDefaultPref, "getParent") ?: return
        val activity = callMethod(fragment, "requireActivity") as Activity
        val insertOrder = (callMethod(openByDefaultPref, "getOrder") as Int) + 1

        movePreferencesAfter(parent, insertOrder)

        val pref = MiuixPreferenceUtils.createTextPreference(activity).apply {
            callMethod(this, "setKey", KEY_AOSP_APP_INFO)
            callMethod(
                this,
                "setTitle",
                getModuleString(activity, R.string.security_center_aosp_app_info_label)
            )
            callMethod(this, "setVisible", true)
            callMethod(this, "setPersistent", false)
            callMethod(this, "setOrder", insertOrder)
            callMethod(this, "setIntent", createAppInfoIntent(activity))
        }
        callMethod(parent, "addPreference", pref)
    }

    private fun movePreferencesAfter(parent: Any, insertOrder: Int) {
        val count = callMethod(parent, "getPreferenceCount") as Int
        for (index in 0 until count) {
            val pref = callMethod(parent, "getPreference", index) ?: continue
            val order = callMethod(pref, "getOrder") as Int
            if (order >= insertOrder) {
                callMethod(pref, "setOrder", order + 1)
            }
        }
    }

    private fun getModuleString(context: Context, id: Int): String =
        AppsTool.getModuleRes(context).getString(id)

    private fun createAppInfoIntent(activity: Activity): Intent {
        val pkgName = activity.intent.getStringExtra("package_name")!!
        // UserHandle.myUserId() / UserHandle.getUserId(uid) 标记 @hide，
        // 不在 public SDK 中，需走反射。
        val myUserId = UserHandle::class.java.callStaticMethod("myUserId") as Int
        val uid = activity.intent.getIntExtra("miui.intent.extra.USER_ID", myUserId)
        val userId = UserHandle::class.java.callStaticMethod("getUserId", uid) as Int
        // SPA 路由：AppInfoSettings/{packageName}/{userId}
        // 对应 Provider：com.android.settings.spa.app.appinfo.AppInfoSettingsProvider
        val destination = "AppInfoSettings/$pkgName/$userId"
        return Intent(Intent.ACTION_MAIN).apply {
            setClassName("com.android.settings", "com.android.settings.spa.SpaActivity")
            putExtra("spaActivityDestination", destination)
            putExtra("sessionSource", "browse")
        }
    }
}
