package com.sevtinge.cemiuiler.module.systemui.statusbar.network

import android.view.View
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.Helpers
import de.robv.android.xposed.XposedHelpers


object StatusBarNoNetSpeedSep : BaseHook() {
    override fun init() {
        Helpers.findAndHookMethod("com.android.systemui.statusbar.views.NetworkSpeedSplitter", lpparam.classLoader, "updateVisibility",
            object : MethodHook() {
                @Throws(Throwable::class)
                override fun before(param: MethodHookParam) {
                    XposedHelpers.setObjectField(
                        param.thisObject,
                        "mNetworkSpeedVisibility",
                        View.GONE
                    )
                }
            }
        )
    }
}