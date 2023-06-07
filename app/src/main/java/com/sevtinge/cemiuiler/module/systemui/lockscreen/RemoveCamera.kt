package com.sevtinge.cemiuiler.module.systemui.lockscreen

import android.view.View
import android.widget.LinearLayout
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.getObjectField

object RemoveCamera : BaseHook() {
    override fun init() {
        // 屏蔽右下角组件显示
        loadClass("com.android.systemui.statusbar.phone.KeyguardBottomAreaView").methodFinder().first {
            name == "onFinishInflate"
        }.createHook {
            after {
                (it.thisObject.getObjectField("mRightAffordanceViewLayout") as LinearLayout).visibility =
                    View.GONE
            }
        }

        // 屏蔽滑动撞墙动画
        loadClass("com.android.keyguard.KeyguardMoveRightController").methodFinder().first {
            name == "onTouchMove" && parameterCount == 2
        }.createHook {
            before {
                it.result = false
            }
        }
        loadClass("com.android.keyguard.KeyguardMoveRightController").methodFinder().first {
            name == "reset"
        }.createHook {
            before {
                it.result = null
            }
        }

    }
}
