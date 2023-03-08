package com.sevtinge.cemiuiler.module.securitycenter

import android.widget.TextView
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.sevtinge.cemiuiler.module.base.BaseHook

class RemoveOpenAppConfirmationPopup : BaseHook() {
    override fun init() {
        findMethod("android.widget.TextView") {
            name == "setText" && parameterTypes[0] == CharSequence::class.java
        }.hookAfter {
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