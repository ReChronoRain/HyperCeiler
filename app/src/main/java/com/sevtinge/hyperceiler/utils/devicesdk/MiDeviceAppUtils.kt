package com.sevtinge.hyperceiler.utils.devicesdk

import com.github.kyuubiran.ezxhelper.ClassUtils.getStaticObjectOrNullAs
import com.sevtinge.hyperceiler.utils.api.LazyClass.clazzMiuiBuild

val IS_TABLET by lazy {
    getStaticObjectOrNullAs<Boolean>(clazzMiuiBuild, "IS_TABLET") ?: false
}
val IS_INTERNATIONAL_BUILD by lazy {
    getStaticObjectOrNullAs<Boolean>(clazzMiuiBuild, "IS_INTERNATIONAL_BUILD") ?: false
}

/**
 * 函数调用，适用于其他一些需要判断的情况，仅支持小米设备的判断
 * 2024-04-20 更新对非小米设备的判断方式，仅防止闪退
 * @return 一个 Boolean 值，true 代表是平板，false 代表不是平板
 */
fun isPad(): Boolean {
    return try {
        IS_TABLET
    } catch(_: Throwable) {
        isPadDevice()
    }
}

/**
 * 函数调用，适用于其他一些需要判断的情况，仅支持小米设备的判断
 * @return 一个 Boolean 值，true 代表是国际版系统，false 代表不是国际版系统
 */
fun isInternational(): Boolean {
    return  try {
        IS_INTERNATIONAL_BUILD
    } catch(_: Throwable) {
        false
    }
}