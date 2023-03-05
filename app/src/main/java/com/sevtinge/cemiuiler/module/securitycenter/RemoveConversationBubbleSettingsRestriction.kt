package com.sevtinge.cemiuiler.module.securitycenter

import com.sevtinge.cemiuiler.module.base.BaseHook
import android.annotation.SuppressLint
import android.content.Context
import android.util.ArrayMap
import com.github.kyuubiran.ezxhelper.utils.*
import org.lsposed.hiddenapibypass.HiddenApiBypass

class RemoveConversationBubbleSettingsRestriction : BaseHook(){
    @SuppressLint("PrivateApi")
    override fun init() {
        findMethod("com.miui.bubbles.settings.BubblesSettings") {
            name == "getDefaultBubbles"
        }.hookBefore { param ->
            val classBubbleApp = loadClass("com.miui.bubbles.settings.BubbleApp")
            val arrayMap = ArrayMap<String, Any>()
            val mContext = param.thisObject.getObject("mContext") as Context
            val mCurrentUserId = param.thisObject.getObject("mCurrentUserId") as Int
            val freeformSuggestionList = HiddenApiBypass.invoke(
                Class.forName("android.util.MiuiMultiWindowUtils"),
                null,
                "getFreeformSuggestionList",
                mContext
            ) as List<*>
            if (freeformSuggestionList.isNotEmpty()) {
                for (str in freeformSuggestionList) {
                    val bubbleApp = classBubbleApp.getConstructor(
                        String::class.java, Int::class.java
                    ).newInstance(str, mCurrentUserId)
                    classBubbleApp.getMethod("setChecked", Boolean::class.java)
                        .invoke(bubbleApp, true)
                    arrayMap[str as String] = bubbleApp
                }
            }
            param.result = arrayMap
        }
    }
}