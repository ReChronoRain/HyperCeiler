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
package com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.internal

import com.sevtinge.hyperceiler.libhook.callback.IMethodHook
import com.sevtinge.hyperceiler.libhook.callback.IReplaceHook
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog
import io.github.kyuubiran.ezxhelper.core.ClassLoaderProvider.safeClassLoader
import io.github.kyuubiran.ezxhelper.core.finder.ConstructorFinder
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedInterface.MethodUnhooker
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.Arrays

/**
 * Hook 操作工具
 * 封装方法/构造器 Hook、批量 Hook、deoptimize 等功能
 */
internal object EzxHookHelper {

    private const val TAG = "EzxHookHelper"

    // ==================== 格式化工具 ====================

    /**
     * 格式化构造器签名用于日志输出
     */
    private fun formatConstructorSignature(constructor: Constructor<*>): String {
        val params = constructor.parameterTypes.joinToString(", ") { it.simpleName }
        return "${constructor.declaringClass.simpleName}<init>($params)"
    }

    // ==================== Hook 方法 ====================

    /**
     * Hook 方法
     *
     * @param method 要 Hook 的方法
     * @param callback Hook 回调
     * @return MethodUnhooker 对象，可用于取消 Hook
     */
    fun hookMethod(method: Method, callback: IMethodHook): MethodUnhooker<*> {
        val signature = EzxMethodHelper.formatMethodSignature(method)
        return try {
            val unhook = method.createHook {
                before { param ->
                    try {
                        callback.before(param)
                    } catch (t: Throwable) {
                        XposedLog.e(TAG, "[$signature] before callback error", t)
                    }
                }
                after { param ->
                    try {
                        callback.after(param)
                    } catch (t: Throwable) {
                        XposedLog.e(TAG, "[$signature] after callback error", t)
                    }
                }
            }
            unhook
        } catch (t: Throwable) {
            XposedLog.e(TAG, "[$signature] hook failed", t)
            throw t
        }
    }

    /**
     * Hook 方法（替换模式）
     *
     * @param method 要 Hook 的方法
     * @param callback 替换回调
     * @return MethodUnhooker 对象，可用于取消 Hook
     */
    fun hookMethod(method: Method, callback: IReplaceHook): MethodUnhooker<*> {
        val signature = EzxMethodHelper.formatMethodSignature(method)
        return try {
            val unhook = method.createHook {
                replace { param ->
                    try {
                        callback.replace(param)
                    } catch (t: Throwable) {
                        XposedLog.e(TAG, "[$signature] replace callback error", t)
                        throw t
                    }
                }
            }
            unhook
        } catch (t: Throwable) {
            XposedLog.e(TAG, "[$signature] hook (replace) failed", t)
            throw t
        }
    }

    /**
     * Hook 构造器
     *
     * @param constructor 要 Hook 的构造器
     * @param callback Hook 回调
     * @return MethodUnhooker 对象，可用于取消 Hook
     */
    fun hookConstructor(constructor: Constructor<*>, callback: IMethodHook): MethodUnhooker<*> {
        val signature = formatConstructorSignature(constructor)
        return try {
            val unhook = constructor.createHook {
                before { param ->
                    try {
                        callback.before(param)
                    } catch (t: Throwable) {
                        XposedLog.e(TAG, "[$signature] before callback error", t)
                    }
                }
                after { param ->
                    try {
                        callback.after(param)
                    } catch (t: Throwable) {
                        XposedLog.e(TAG, "[$signature] after callback error", t)
                    }
                }
            }
            unhook
        } catch (t: Throwable) {
            XposedLog.e(TAG, "[$signature] hook failed", t)
            throw t
        }
    }

    // ==================== findAndHook 方法 ====================

    /**
     * 查找并 Hook 方法
     *
     * @param clazz 目标类
     * @param methodName 方法名
     * @param parameterTypes 参数类型
     * @param callback Hook 回调（放在最后以支持 Java lambda）
     * @return Unhook 对象
     */
    fun findAndHookMethod(
        clazz: Class<*>,
        methodName: String,
        vararg parameterTypes: Class<*>,
        callback: IMethodHook
    ): MethodUnhooker<*> {
        val method = EzxMethodHelper.findMethodExactIfExists(clazz, methodName, *parameterTypes)
        return hookMethod(method, callback)
    }

    fun findAndHookMethod(
        clazzName: String,
        methodName: String,
        vararg args: Any,
    ): MethodUnhooker<*> {
        val clazz = EzxClassHelper.findClass(clazzName, safeClassLoader)
        return findAndHookMethod(clazz, methodName, *args)
    }

    fun findAndHookMethod(
        clazzName: String,
        classLoader: ClassLoader,
        methodName: String,
        vararg args: Any
    ): MethodUnhooker<*> {
        val clazz = EzxClassHelper.findClass(clazzName, classLoader)
        return findAndHookMethod(clazz, methodName, *args)
    }

    /**
     * 查找并 Hook 方法
     *
     * @param clazz 目标类
     * @param methodName 方法名
     * @param args 参数类型数组，最后一个元素必须是 IMethodHook
     * @return MethodUnhooker 对象
     */
    fun findAndHookMethod(
        clazz: Class<*>,
        methodName: String,
        vararg args: Any
    ): MethodUnhooker<*> {
        require(args.isNotEmpty()) { "args must contain at least the callback" }
        val callback = args.last()
        require(callback is IMethodHook) { "Last argument must be IMethodHook" }

        val paramTypes = EzxMethodHelper.getParameterClasses(clazz, *args)
        val method = EzxMethodHelper.findMethodExactIfExists(clazz, methodName, *paramTypes)
        return hookMethod(method, callback)
    }

    // ==================== findAndHookMethodReplace ====================

    fun findAndHookMethodReplace(
        clazzName: String,
        methodName: String,
        vararg args: Any,
    ): MethodUnhooker<*> {
        val clazz = EzxClassHelper.findClass(clazzName, safeClassLoader)
        return findAndHookMethodReplace(clazz, methodName, *args)
    }

    fun findAndHookMethodReplace(
        clazzName: String,
        classLoader: ClassLoader,
        methodName: String,
        vararg args: Any
    ): MethodUnhooker<*> {
        val clazz = EzxClassHelper.findClass(clazzName, classLoader)
        return findAndHookMethodReplace(clazz, methodName, *args)
    }

    /**
     * 查找并 Hook 方法（替换模式）
     *
     * @param clazz 目标类
     * @param methodName 方法名
     * @param args 参数类型数组，最后一个元素必须是 IReplaceHook
     * @return MethodUnhooker 对象
     */
    fun findAndHookMethodReplace(
        clazz: Class<*>,
        methodName: String,
        vararg args: Any
    ): MethodUnhooker<*> {
        require(args.isNotEmpty()) { "args must contain at least the callback" }
        val callback = args.last()
        require(callback is IReplaceHook) { "Last argument must be IReplaceHook" }

        val paramTypes = EzxMethodHelper.getParameterClasses(clazz, *args)
        val method = EzxMethodHelper.findMethodExactIfExists(clazz, methodName, *paramTypes)
        return hookMethod(method, callback)
    }

    // ==================== findAndHookConstructor ====================

    /**
     * 查找并 Hook 构造器
     *
     * @param clazz 目标类
     * @param parameterTypes 参数类型
     * @param callback Hook 回调
     * @return MethodUnhooker 对象
     */
    fun findAndHookConstructor(
        clazz: Class<*>,
        vararg parameterTypes: Class<*>,
        callback: IMethodHook
    ): MethodUnhooker<*> {
        val constructor = EzxMethodHelper.findConstructorExact(clazz, *parameterTypes)
        return hookConstructor(constructor, callback)
    }

    fun findAndHookConstructor(
        clazzName: String,
        vararg args: Any
    ): MethodUnhooker<*> {
        val clazz = EzxClassHelper.findClass(clazzName, safeClassLoader)
        return findAndHookConstructor(clazz, *args)
    }

    fun findAndHookConstructor(
        clazzName: String,
        classLoader: ClassLoader,
        vararg args: Any
    ): MethodUnhooker<*> {
        val clazz = EzxClassHelper.findClass(clazzName, classLoader)
        return findAndHookConstructor(clazz, *args)
    }

    /**
     * 查找并 Hook 构造器
     *
     * @param clazz 目标类
     * @param args 参数类型数组，最后一个元素必须是 IMethodHook
     * @return MethodUnhooker 对象
     */
    fun findAndHookConstructor(clazz: Class<*>, vararg args: Any): MethodUnhooker<*> {
        require(args.isNotEmpty()) { "args must contain at least the callback" }
        val callback = args.last()
        require(callback is IMethodHook) { "Last argument must be IMethodHook" }

        val paramTypes = EzxMethodHelper.getParameterClasses(clazz, *args)
        val constructor = EzxMethodHelper.findConstructorExact(clazz, *paramTypes)
        return hookConstructor(constructor, callback)
    }

    // ==================== hookAll 方法 ====================

    fun hookAllMethods(
        clazzName: String,
        methodName: String,
        callback: IMethodHook
    ): List<MethodUnhooker<*>> {
        val clazz = EzxClassHelper.findClass(clazzName, safeClassLoader)
        return hookAllMethods(clazz, methodName, callback)
    }

    fun hookAllMethods(
        clazzName: String,
        classLoader: ClassLoader,
        methodName: String,
        callback: IMethodHook
    ): List<MethodUnhooker<*>> {
        val clazz = EzxClassHelper.findClass(clazzName, classLoader)
        return hookAllMethods(clazz, methodName, callback)
    }

    /**
     * Hook 类中所有指定名称的方法
     *
     * @param clazz 目标类
     * @param methodName 方法名
     * @param callback Hook 回调
     * @return MethodUnhooker 对象列表
     */
    fun hookAllMethods(
        clazz: Class<*>,
        methodName: String,
        callback: IMethodHook
    ): List<MethodUnhooker<*>> {
        val methods = MethodFinder.fromClass(clazz)
            .filterByName(methodName)
            .toList()

        if (methods.isEmpty()) {
            XposedLog.w(TAG, "[${clazz.simpleName}#$methodName] no methods found")
            return emptyList()
        }

        return methods.mapNotNull { method ->
            try {
                if (Modifier.isAbstract(method.modifiers)) {
                    XposedLog.w(TAG, "[${clazz.simpleName}#${method.name}] is abstract, skipping")
                    return@mapNotNull null
                }
                hookMethod(method, callback)
            } catch (_: Throwable) {
                null
            }
        }
    }

    /**
     * Hook 类中所有构造器
     *
     * @param clazz 目标类
     * @param callback Hook 回调
     * @return MethodUnhooker 对象列表
     */
    fun hookAllConstructors(clazz: Class<*>, callback: IMethodHook): List<MethodUnhooker<*>> {
        val constructors = ConstructorFinder.fromClass(clazz).toList()

        if (constructors.isEmpty()) {
            XposedLog.w(TAG, "[${clazz.simpleName}<init>] no constructors found")
            return emptyList()
        }

        return constructors.mapNotNull { constructor ->
            try {
                hookConstructor(constructor, callback)
            } catch (_: Throwable) {
                null
            }
        }
    }

    // ==================== 便捷 Hook 工具 ====================

    /**
     * 创建一个返回常量值的 Hook 回调
     *
     * @param result 要返回的常量值
     * @return IMethodHook 实例
     */
    fun returnConstant(result: Any?): IMethodHook {
        return object : IMethodHook {
            override fun before(param: io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam) {
                param.result = result
            }
        }
    }

    /**
     * 什么都不做的 Hook（阻止原方法执行，返回 null）
     */
    val DO_NOTHING: IMethodHook = object : IMethodHook {
        override fun before(param: io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam) {
            param.result = null
        }
    }

    // ==================== deoptimize 相关 ====================

    fun deoptimize(method: Method): Boolean {
        return try {
            EzxModuleHolder.xposedModule.deoptimize(method)
            XposedLog.d(TAG, "deoptimize $method success")
            true
        } catch (t: Throwable) {
            XposedLog.e(TAG, "deoptimize $method failed, log: $t")
            return false
        }
    }

    fun deoptimizeMethods(clazz: Class<*>, vararg names: String?) {
        val list = listOf(*names)
        Arrays.stream(clazz.declaredMethods)
            .filter { method: Method? ->
                list.contains(method!!.name)
            }
            .forEach { method: Method? ->
                deoptimize(method!!)
            }
    }

    fun libHook(method: Method, hooker: Class<out Hooker>): MethodUnhooker<Method?> {
        return EzxModuleHolder.xposedModule.hook(method, hooker)
    }
}
