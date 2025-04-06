/*
  * This file is part of HyperCeiler.

  * HyperCeiler is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation, either version 3 of the
  * License.

  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.

  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <https://www.gnu.org/licenses/>.

  * Copyright (C) 2023-2025 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.hook.utils.devicesdk

import com.github.kyuubiran.ezxhelper.ClassUtils.getStaticObjectOrNullAs
import com.sevtinge.hyperceiler.hook.utils.api.LazyClass.clazzMiuiBuild

val IS_TABLET by lazy {
    getStaticObjectOrNullAs<Boolean>(clazzMiuiBuild, "IS_TABLET") ?: false
}
val IS_PAD by lazy {
    getStaticObjectOrNullAs<Boolean>(clazzMiuiBuild, "IS_PAD") ?: false
}
val IS_FOLD by lazy {
    getStaticObjectOrNullAs<Boolean>(clazzMiuiBuild, "IS_FOLD") ?: false
}
val IS_INTERNATIONAL_BUILD by lazy {
    getStaticObjectOrNullAs<Boolean>(clazzMiuiBuild, "IS_INTERNATIONAL_BUILD") ?: false
}

/**
 * 函数调用，适用于其他一些需要更高精度判断大屏设备的情况，仅支持小米设备的判断
 * @return 一个 Boolean 值，true 代表是大屏设备，false 代表不是大屏设备
 */
fun isLargeUI(): Boolean {
    return runCatching {
        !(!IS_PAD && (!IS_FOLD || !isTablet()))
    }.getOrElse {
        isPad()
    }
}

/**
 * 函数调用，适用于其他一些需要判断的情况，仅支持小米设备的判断
 * 2025-04-20 更新对非小米设备的判断方式，仅防止闪退
 * @return 一个 Boolean 值，true 代表是平板，false 代表不是平板
 */
fun isPad(): Boolean {
    return runCatching {
        IS_TABLET
    }.getOrElse {
        isPadDevice()
    }
}

/**
 * 函数调用，适用于其他一些需要判断的情况，仅支持小米设备的判断
 * @return 一个 Boolean 值，true 代表是国际版系统，false 代表不是国际版系统
 */
fun isInternational(): Boolean {
    return runCatching {
        IS_INTERNATIONAL_BUILD
    }.getOrElse {
        false
    }
}
