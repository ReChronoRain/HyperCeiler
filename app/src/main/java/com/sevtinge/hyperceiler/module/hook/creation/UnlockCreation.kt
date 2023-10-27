package com.sevtinge.hyperceiler.module.hook.creation

import com.github.kyuubiran.ezxhelper.ClassUtils.setStaticObject
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.api.LazyClass.clazzMiuiBuild

object UnlockCreation : BaseHook() {
    override fun init() {
        setStaticObject(clazzMiuiBuild, "IS_TABLET", true)
    }
}
