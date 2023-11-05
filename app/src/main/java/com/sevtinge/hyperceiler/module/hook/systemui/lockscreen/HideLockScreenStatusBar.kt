package com.sevtinge.hyperceiler.module.hook.systemui.lockscreen

import android.view.View
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.devicesdk.isMoreAndroidVersion
import de.robv.android.xposed.XposedHelpers

object HideLockScreenStatusBar : BaseHook() {
    override fun init() {
        val statusBarClass = if (isMoreAndroidVersion(33))
            "com.android.systemui.statusbar.phone.CentralSurfacesImpl"
        else
            "com.android.systemui.statusbar.phone.StatusBar"

        hookAllMethods(
            statusBarClass, lpparam.classLoader, "makeStatusBarView",
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
