package com.sevtinge.cemiuiler.module.systemui.lockscreen

import com.sevtinge.cemiuiler.module.base.BaseHook
import android.view.View
import android.widget.LinearLayout
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.getObject
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.hookBefore

object RemoveCamera : BaseHook() {
    override fun init() {
        //屏蔽右下角组件显示
        findMethod("com.android.systemui.statusbar.phone.KeyguardBottomAreaView") {
            name == "onFinishInflate"
        }.hookAfter {
            (it.thisObject.getObject("mRightAffordanceViewLayout") as LinearLayout).visibility =
                View.GONE
        }

        //屏蔽滑动撞墙动画
        findMethod("com.android.keyguard.KeyguardMoveRightController") {
            name == "onTouchMove" && parameterCount == 2
        }.hookBefore {
            it.result = false
        }
        findMethod("com.android.keyguard.KeyguardMoveRightController") {
            name == "reset"
        }.hookBefore {
            it.result = null
        }

    }
}