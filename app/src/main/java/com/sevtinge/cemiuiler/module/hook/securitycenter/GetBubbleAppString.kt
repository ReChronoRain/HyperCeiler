package com.sevtinge.cemiuiler.module.hook.securitycenter

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.getObjectField
import com.sevtinge.cemiuiler.utils.log.XposedLogUtils

object GetBubbleAppString : BaseHook() {
    override fun init() {
        try {
            val classBubble = loadClass("com.miui.bubbles.Bubble")
            loadClass("com.miui.bubbles.settings.BubblesSettings").methodFinder().first {
                name == "getBubbleAppString"
            }.createHook {
                before {
                    val stringBuilder = StringBuilder()
                    val mActiveBubbles = it.thisObject.getObjectField("mActiveBubbles")
                    for (bubble in mActiveBubbles as HashSet<*>) {
                        stringBuilder.append(
                            classBubble.getMethod("getPackageName").invoke(bubble)
                        )
                        stringBuilder.append(":")
                        stringBuilder.append(bubble.getObjectField("userId"))
                        stringBuilder.append(",")
                    }
                    // XposedBridge.log("MaxFreeFormTest: getBubbleAppString called! Result:$stringBuilder")
                    it.result = stringBuilder.toString()
                }
            }
        } catch (e: Throwable) {
            XposedLogUtils.logE(TAG, e)
        }
    }

}
