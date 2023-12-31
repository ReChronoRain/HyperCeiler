package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.strongtoast

import android.widget.FrameLayout
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook


object HideStrongToast : BaseHook() {
    private val StrongToast by lazy {
        loadClassOrNull("com.android.systemui.toast.MIUIStrongToast", lpparam.classLoader)
    }

    override fun init() {
        StrongToast!!.methodFinder().first {
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
