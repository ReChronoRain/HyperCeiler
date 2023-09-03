package com.sevtinge.cemiuiler.module.hook.securitycenter

import android.annotation.SuppressLint
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook

class RemoveOpenAppConfirmationPopup : BaseHook() {
    @SuppressLint("DiscouragedApi")
    override fun init() {
        loadClass("android.widget.TextView").methodFinder().first {
            name == "setText" && parameterTypes[0] == CharSequence::class.java
        }.createHook {
            after {
                val textView = it.thisObject as TextView
                if (it.args.isNotEmpty() && it.args[0]?.toString().equals(
                        textView.context.resources.getString(
                            textView.context.resources.getIdentifier(
                                "button_text_accept",
                                "string",
                                textView.context.packageName
                            )
                        )
                    )
                ) {
                    textView.performClick()
                }
            }
        }
    }
}
