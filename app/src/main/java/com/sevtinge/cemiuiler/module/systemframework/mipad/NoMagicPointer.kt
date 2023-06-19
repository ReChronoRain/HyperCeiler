package com.sevtinge.cemiuiler.module.systemframework.mipad

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook

object NoMagicPointer : BaseHook() {
    override fun init() {
        loadClassOrNull("android.magicpointer.util.MiuiMagicPointerUtils")?.methodFinder()?.first {
            name == "isEnable"
        }?.createHook {
            returnConstant(false)
        }

        loadClass("com.android.server.SystemServerImpl").methodFinder().first {
            name == "addMagicPointerManagerService"
        }.createHook {
            returnConstant(null)
        }
    }
}
