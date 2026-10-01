package com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.island

import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClassOrNull
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook

class UnlockFocus : BaseHook() {

    private val mPkg by lazy {
        PrefsBridge.getStringSet("system_ui_focus_notification_list")
    }

    override fun init() {

        XposedLog.d(TAG, "UnlockFocus init")
        val NotificationSettingsManager =
            loadClassOrNull("com.miui.systemui.notification.NotificationSettingsManager")

        NotificationSettingsManager?.findMethod {
            name("canShowFocusState")
        }
            ?.createAfterHook {
                val pkg = it.args[1].toString()
                if (mPkg.contains(pkg)) {
                    it.result = 1
                }
            }

        NotificationSettingsManager?.findMethod {
            name("canShowFocusStateApp")
        }
            ?.createAfterHook {
                val pkg = it.args[1].toString()
                if (mPkg.contains(pkg)) {
                    it.result = 1
                }
            }
    }
}
