package com.sevtinge.hyperceiler.libhook.rules.systemframework.display

import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.hookAllMethods

/** 绕过背屏白名单*/
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
        /** 禁止锁屏返回桌面 */
        val notGotoHome = PrefsBridge.getBoolean("system_framework_fuck_subscreen_not_go_to_home")

        if (notGotoHome) {
            asI.hookAllMethods("handlerTransitionFinished") {
                before { param ->
                    param.args[3] = false
                }
            }

        }

    }
}
