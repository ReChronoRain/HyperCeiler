package com.sevtinge.hyperceiler.hook.module.hook.home.recent

import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.base.tool.HookTool
import com.sevtinge.hyperceiler.hook.utils.callMethod
import com.sevtinge.hyperceiler.hook.utils.getObjectField

object HideRecentCard : BaseHook() {
    override fun init() {
        findAndHookMethod(
            "com.android.systemui.shared.recents.system.ActivityManagerWrapper",
            "needRemoveTask",
            "com.android.systemui.shared.recents.model.GroupedRecentTaskInfoCompat",
            object : HookTool.MethodHook() {
                override fun after(param: MethodHookParam) {
                    val pkgName = param.args[0]
                        ?.getObjectField("mMainTaskInfo")
                        ?.getObjectField("realActivity")
                        ?.callMethod("getPackageName")
                    val selectedApps = mPrefsMap.getStringSet("home_recent_hide_card")
                    if (selectedApps.contains(pkgName)) {
                        param.result = true
                    }
                }
            })
    }
}
