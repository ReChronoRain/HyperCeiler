package com.sevtinge.cemiuiler.utils.devicesdk

import com.github.kyuubiran.ezxhelper.ClassUtils.getStaticObjectOrNullAs
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass

object Build {
    private val clazzMiuiBuild by lazy {
        loadClass("miui.os.Build")
    }
    val IS_TABLET by lazy {
        getStaticObjectOrNullAs<Boolean>(clazzMiuiBuild, "IS_TABLET") ?: false
    }
    val IS_INTERNATIONAL_BUILD by lazy {
        getStaticObjectOrNullAs<Boolean>(clazzMiuiBuild, "IS_INTERNATIONAL_BUILD") ?: false
    }
}
