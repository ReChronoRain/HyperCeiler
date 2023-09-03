package com.sevtinge.cemiuiler.module.hook.creation

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.ClassUtils.setStaticObject
import com.sevtinge.cemiuiler.module.base.BaseHook

object UnlockCreation : BaseHook() {
    override fun init() {
        setStaticObject(loadClass("miui.os.Build"), "IS_TABLET", true)
    }
}
