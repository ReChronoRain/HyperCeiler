package com.sevtinge.cemiuiler.module.hook.systemframework

import com.sevtinge.cemiuiler.module.base.BaseHook

object DeleteOnPostNotification : BaseHook() {

    override fun init() {
        findAndHookMethod("com.android.server.wm.AlertWindowNotification", "onPostNotification",
            object : MethodHook() {
                override fun before(param: MethodHookParam?) {
                    param?.result = null
                }
            }
        )
    }
}
