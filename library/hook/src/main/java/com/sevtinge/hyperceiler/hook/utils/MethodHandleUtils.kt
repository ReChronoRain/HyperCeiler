/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.hook.utils

import de.robv.android.xposed.XposedHelpers
import java.lang.invoke.MethodHandles
import java.lang.reflect.Method

/**
 * 方法句柄工具类
 */
@Suppress("unused")
object MethodHandleUtils {
    @JvmStatic
    fun invokeSuperMethod(
        obj: Any,
        methodName: String,
        vararg toArgs: Any?
    ): Any? = invokeSpecialMethod(
        obj,
        XposedHelpers.findMethodBestMatch(obj.javaClass.superclass, methodName, *toArgs),
        *toArgs
    )

    @JvmStatic
    fun invokeSpecialMethod(
        obj: Any,
        refClass: Class<*>,
        methodName: String,
        vararg args: Any?
    ): Any? = invokeSpecialMethod(
        obj,
        XposedHelpers.findMethodBestMatch(refClass, methodName, *args),
        *args
    )

    @JvmStatic
    fun invokeSpecialMethod(
        obj: Any,
        method: Method,
        vararg args: Any?
    ): Any? = MethodHandles.privateLookupIn(obj.javaClass, MethodHandles.lookup())
        .unreflectSpecial(method, obj.javaClass)
        .invokeWithArguments(obj, *args)
}
