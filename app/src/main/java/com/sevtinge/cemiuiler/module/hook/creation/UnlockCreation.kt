package com.sevtinge.cemiuiler.module.hook.creation

import com.github.kyuubiran.ezxhelper.ClassUtils.setStaticObject
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.api.LazyClass.clazzMiuiBuild

object UnlockCreation : BaseHook() {
    override fun init() {
        setStaticObject(clazzMiuiBuild, "IS_TABLET", true)
    }
}
