package com.sevtinge.hyperceiler.utils.devicesdk

import com.sevtinge.hyperceiler.utils.api.LazyClass.clazzMiuiBuild

/**
 * 函数调用，适用于其他一些需要判断的情况，仅支持小米设备的判断
 * 2024-04-20 更新对非小米设备的判断方式，仅防止闪退
 * @return 一个 Boolean 值，true 代表是平板，false 代表不是平板
 */
fun isPad(): Boolean {
    return try {
        clazzMiuiBuild.getField("IS_TABLET").getBoolean(null)
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
        clazzMiuiBuild.getField("IS_INTERNATIONAL_BUILD").getBoolean(null)
    } catch(_: Throwable) {
        false
    }
}