package com.sevtinge.hyperceiler.module.hook.systemui.lockscreen

import android.view.View
import com.sevtinge.hyperceiler.module.base.BaseHook
import de.robv.android.xposed.XposedHelpers

object HideLockScreenStatusBar : BaseHook() {
    override fun init() {
        hookAllMethods(
            "com.android.systemui.statusbar.phone.CentralSurfacesImpl", lpparam.classLoader,
            "makeStatusBarView",
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
