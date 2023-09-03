package com.sevtinge.cemiuiler.module.hook.systemui.lockscreen

import android.view.View
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.Helpers
import com.sevtinge.cemiuiler.utils.devicesdk.isMoreAndroidVersion
import de.robv.android.xposed.XposedHelpers

object HideLockScreenStatusBar : BaseHook() {
    override fun init() {
        val statusBarClass = if (isMoreAndroidVersion(33))
            "com.android.systemui.statusbar.phone.CentralSurfacesImpl"
        else
            "com.android.systemui.statusbar.phone.StatusBar"

        Helpers.hookAllMethods(statusBarClass, lpparam.classLoader, "makeStatusBarView",
            object : MethodHook() {
                override fun after(param: MethodHookParam) {
                    val mKeyguardStatusBar = XposedHelpers.getObjectField(
                        XposedHelpers.getObjectField(
                            param.thisObject,
                            "mNotificationPanelViewController"
                        ), "mKeyguardStatusBar"
                    ) as View
                    mKeyguardStatusBar.translationY = -999f
                }
            }
        )
    }
}
