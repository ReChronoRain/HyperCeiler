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
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.base

import io.github.libxposed.api.XposedInterface
import io.github.lingqiqi5211.ezhooktool.core.EzReflect
import io.github.lingqiqi5211.ezhooktool.core.findConstructorBestMatch
import io.github.lingqiqi5211.ezhooktool.core.findMethodBestMatch
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createInterceptHook
import java.lang.reflect.Constructor
import java.lang.reflect.Method

/** Java 调用方使用的 EzHookTool DSL 桥接，保留旧 API 的优先级和异常策略。 */
object HookBridge {
    @JvmStatic
    fun intercept(
        method: Method,
        priority: Int,
        exceptionMode: XposedInterface.ExceptionMode,
        hooker: XposedInterface.Hooker,
    ): XposedInterface.HookHandle = method.createInterceptHook(priority, exceptionMode) { chain ->
        hooker.intercept(chain)
    }

    @JvmStatic
    fun intercept(
        constructor: Constructor<*>,
        priority: Int,
        exceptionMode: XposedInterface.ExceptionMode,
        hooker: XposedInterface.Hooker,
    ): XposedInterface.HookHandle = constructor.createInterceptHook(priority, exceptionMode) { chain ->
        hooker.intercept(chain)
    }

    @JvmStatic
    fun findAndInterceptMethod(
        clazz: Class<*>,
        methodName: String,
        priority: Int,
        exceptionMode: XposedInterface.ExceptionMode,
        parameterTypesAndHooker: Array<out Any>,
    ): XposedInterface.HookHandle {
        val hooker = requireHooker(parameterTypesAndHooker)
        val parameterTypes = resolveParameterTypes(clazz, parameterTypesAndHooker)
        val method = runCatching {
            clazz.getDeclaredMethod(methodName, *parameterTypes).also { it.isAccessible = true }
        }.getOrNull() ?: findMethodBestMatch(clazz, methodName, *parameterTypes)
        return intercept(method, priority, exceptionMode, hooker)
    }

    @JvmStatic
    fun findAndInterceptConstructor(
        clazz: Class<*>,
        priority: Int,
        exceptionMode: XposedInterface.ExceptionMode,
        parameterTypesAndHooker: Array<out Any>,
    ): XposedInterface.HookHandle {
        val hooker = requireHooker(parameterTypesAndHooker)
        val parameterTypes = resolveParameterTypes(clazz, parameterTypesAndHooker)
        val constructor = runCatching {
            clazz.getDeclaredConstructor(*parameterTypes).also { it.isAccessible = true }
        }.getOrNull() ?: findConstructorBestMatch(clazz, *parameterTypes)
        return intercept(constructor, priority, exceptionMode, hooker)
    }

    private fun requireHooker(parameterTypesAndHooker: Array<out Any>): XposedInterface.Hooker {
        require(parameterTypesAndHooker.isNotEmpty()) {
            "findAndChain requires parameter types followed by a Hooker"
        }
        return parameterTypesAndHooker.last() as? XposedInterface.Hooker
            ?: throw IllegalArgumentException("The last argument must be a Hooker")
    }

    private fun resolveParameterTypes(
        owner: Class<*>,
        parameterTypesAndHooker: Array<out Any>,
    ): Array<Class<*>> = parameterTypesAndHooker.dropLast(1).map { value ->
        when (value) {
            is Class<*> -> value
            is String -> loadClass(
                value,
                owner.classLoader ?: EzReflect.safeClassLoader,
            )
            else -> throw IllegalArgumentException(
                "Parameter type must be Class or class name String, got ${value.javaClass.name}"
            )
        }
    }.toTypedArray()
}
