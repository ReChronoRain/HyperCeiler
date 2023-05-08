package com.sevtinge.cemiuiler.module.securitycenter

import com.github.kyuubiran.ezxhelper.utils.*
import com.sevtinge.cemiuiler.module.base.BaseHook
import de.robv.android.xposed.XposedBridge


object GetBubbleAppString : BaseHook() {
    override fun init() {
        try {
            val classBubble = loadClass("com.miui.bubbles.Bubble")
            findMethod("com.miui.bubbles.settings.BubblesSettings") {
                name == "getBubbleAppString"
            }.hookBefore {
                val stringBuilder = StringBuilder()
                val mActiveBubbles = it.thisObject.getObject("mActiveBubbles")
                for (bubble in mActiveBubbles as HashSet<*>) {
                    stringBuilder.append(
                        classBubble.getMethod("getPackageName").invokeAs<String>(bubble)
                    )
                    stringBuilder.append(":")
                    stringBuilder.append(bubble.getObject("userId"))
                    stringBuilder.append(",")
                }
                // XposedBridge.log("MaxFreeFormTest: getBubbleAppString called! Result:$stringBuilder")
                it.result = stringBuilder.toString()
            }
        } catch (e: Throwable) {
            log("Hook failed by: $e");
        }
    }

}
