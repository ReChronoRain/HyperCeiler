package com.sevtinge.hyperceiler.libhook.rules.systemframework.display

import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.hookAllMethods

object FuckSubScreenWhiteList : BaseHook() {
    override fun init() {
        val asI = findClass("com.android.server.wm.ActivityStarterImpl")
        asI.hookAllMethods("isShouldShowOnRearDisplay") {
            before {
                returnConstant(true)
            }
        }
        asI.hookAllMethods("isAllowedToStartOnRearDisplay") {
            before {
                returnConstant(true)
            }
        }

    }
}
