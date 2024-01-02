package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.strongtoast

import android.widget.FrameLayout
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.api.LazyClass.StrongToast


object HideStrongToast : BaseHook() {
    override fun init() {
        StrongToast.methodFinder().first {
            name == "onAttachedToWindow"
        }.createHook {
            after {
                val strongToastLayout = it.thisObject as FrameLayout
                strongToastLayout.viewTreeObserver.addOnPreDrawListener {
                    return@addOnPreDrawListener false
                }
            }
        }
    }
}
