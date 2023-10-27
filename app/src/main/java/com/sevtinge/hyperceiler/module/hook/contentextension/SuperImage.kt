package com.sevtinge.hyperceiler.module.hook.contentextension

import android.graphics.Bitmap
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook

class SuperImage : BaseHook() {
    override fun init() {
        loadClass("com.miui.contentextension.utils.SuperImageUtils").methodFinder().first {
            name == "isSupportSuperImage"
        }.createHook {
            returnConstant(true)
        }

        loadClass("com.miui.contentextension.utils.SuperImageUtils").methodFinder().first {
            name == "isBitmapSupportSuperImage" &&
                parameterTypes[0] == Bitmap::class.java
        }.createHook {
            returnConstant(true)
        }
    }
}
