package com.sevtinge.hyperceiler.module.hook.home.recent

import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.utils.*

object HideRecentCard : BaseHook() {
    override fun init() {
        findAndHookMethod(
            "com.android.systemui.shared.recents.system.ActivityManagerWrapper",
            "needRemoveTask",
            "com.android.systemui.shared.recents.model.GroupedRecentTaskInfoCompat",
            object : MethodHook() {
                override fun after(param: MethodHookParam) {
                    val pkgName = param.args[0]
                        ?.getObjectField("mMainTaskInfo")
                        ?.getObjectField("topActivity")
                        ?.callMethod("getPackageName")
                    val selectedApps = mPrefsMap.getStringSet("home_recent_hide_card")
                    if (selectedApps.contains(pkgName)) {
                        param.result = true
                    }
                }
            })
    }
}