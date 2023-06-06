package com.sevtinge.cemiuiler.module.systemui.statusbar.network

import android.view.View
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import de.robv.android.xposed.XposedHelpers


object StatusBarNoNetSpeedSep : BaseHook() {
    override fun init() {
        loadClass("com.android.systemui.statusbar.views.NetworkSpeedSplitter", lpparam.classLoader).methodFinder().first {
            name == "updateVisibility"
        }.createHook {
            before {
                XposedHelpers.setObjectField(it.thisObject, "mNetworkSpeedVisibility", View.GONE)
            }
        }
    }
}