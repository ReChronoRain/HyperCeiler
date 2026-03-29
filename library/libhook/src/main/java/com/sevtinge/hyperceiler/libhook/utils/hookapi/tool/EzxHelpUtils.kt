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
@file:Suppress("UNCHECKED_CAST")

package com.sevtinge.hyperceiler.libhook.utils.hookapi.tool

import android.content.Context
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook
import com.sevtinge.hyperceiler.libhook.callback.IReplaceHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.internal.EzxApplicationHookHelper
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.internal.EzxClassHelper
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.internal.EzxFieldHelper
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.internal.EzxHookHelper
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.internal.EzxMethodHelper
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.internal.EzxModuleHolder
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * Hook API
 *
 * 提供类查找、字段操作、方法操作、Hook 操作、Application 生命周期管理等功能。
 * 内部实现委托给 internal 包下的各个 Helper 对象：
 * - [EzxClassHelper] - 类查找
 * - [EzxFieldHelper] - 字段操作
 * - [EzxMethodHelper] - 方法/构造器查找与调用
 * - [EzxHookHelper] - Hook 操作
 * - [EzxApplicationHookHelper] - Application 生命周期管理
 *
 * Java 调用方通过 EzxHelpUtils.xxx() 访问，
 * Kotlin 调用方通过 KtHelpUtils.kt 中的扩展函数访问。
 */
object EzxHelpUtils {

    @JvmStatic
    fun setXposedModule(module: XposedModule) {
        EzxModuleHolder.xposedModule = module
    }

    // ==================== 类查找 ====================

    @JvmStatic
    fun findClass(name: String): Class<*> = EzxClassHelper.findClass(name)

    @JvmStatic
    fun findClass(name: String, classLoader: ClassLoader?): Class<*> =
        EzxClassHelper.findClass(name, classLoader)

    @JvmStatic
    fun findClassIfExists(name: String): Class<*>? = EzxClassHelper.findClassIfExists(name)

    @JvmStatic
    fun findClassIfExists(name: String, classLoader: ClassLoader?): Class<*>? =
        EzxClassHelper.findClassIfExists(name, classLoader)

    // ==================== 字段查找 ====================

    /**
     * 在类层次结构中查找字段（包括父类）
     *
     * @param clazz 目标类
     * @param fieldName 字段名
     * @return Field 对象
     * @throws NoSuchFieldError 如果字段不存在
     */
    @JvmStatic
    fun findField(clazz: Class<*>, fieldName: String): Field =
        EzxFieldHelper.findField(clazz, fieldName)

    @JvmStatic
    fun findFieldIfExists(clazz: Class<*>, fieldName: String): Field? =
        EzxFieldHelper.findFieldIfExists(clazz, fieldName)

    @JvmStatic
    fun findFirstFieldByExactType(clazz: Class<*>, type: Class<*>): Field =
        EzxFieldHelper.findFirstFieldByExactType(clazz, type)

    @JvmStatic
    fun findFirstFieldByExactTypeIfExists(clazz: Class<*>, type: Class<*>): Field? =
        EzxFieldHelper.findFirstFieldByExactTypeIfExists(clazz, type)

    // ==================== 实例字段操作 ====================

    /**
     * 获取对象字段值
     *
     * @param instance 对象实例
     * @param fieldName 字段名
     * @return 字段值
     */
    @JvmStatic
    fun getObjectField(instance: Any, fieldName: String): Any? =
        EzxFieldHelper.getObjectField(instance, fieldName)

    /**
     * 设置对象字段值（支持父类字段）
     *
     * @param instance 对象实例
     * @param fieldName 字段名
     * @param value 要设置的值
     */
    @JvmStatic
    fun setObjectField(instance: Any, fieldName: String, value: Any?) =
        EzxFieldHelper.setObjectField(instance, fieldName, value)

    @JvmStatic
    fun getBooleanField(instance: Any, fieldName: String): Boolean =
        EzxFieldHelper.getBooleanField(instance, fieldName)

    @JvmStatic
    fun setBooleanField(instance: Any, fieldName: String, value: Boolean) =
        EzxFieldHelper.setBooleanField(instance, fieldName, value)

    @JvmStatic
    fun getIntField(instance: Any, fieldName: String): Int =
        EzxFieldHelper.getIntField(instance, fieldName)

    @JvmStatic
    fun setIntField(instance: Any, fieldName: String, value: Int) =
        EzxFieldHelper.setIntField(instance, fieldName, value)

    @JvmStatic
    fun getLongField(instance: Any, fieldName: String): Long =
        EzxFieldHelper.getLongField(instance, fieldName)

    @JvmStatic
    fun setLongField(instance: Any, fieldName: String, value: Long) =
        EzxFieldHelper.setLongField(instance, fieldName, value)

    @JvmStatic
    fun getFloatField(instance: Any, fieldName: String): Float =
        EzxFieldHelper.getFloatField(instance, fieldName)

    @JvmStatic
    fun setFloatField(instance: Any, fieldName: String, value: Float) =
        EzxFieldHelper.setFloatField(instance, fieldName, value)

    // ==================== 静态字段操作 ====================

    @JvmStatic
    fun getStaticObjectField(clazz: Class<*>, fieldName: String): Any? =
        EzxFieldHelper.getStaticObjectField(clazz, fieldName)

    @JvmStatic
    fun setStaticObjectField(clazz: Class<*>, fieldName: String, value: Any?) =
        EzxFieldHelper.setStaticObjectField(clazz, fieldName, value)

    @JvmStatic
    fun getStaticBooleanField(clazz: Class<*>, fieldName: String): Boolean =
        EzxFieldHelper.getStaticBooleanField(clazz, fieldName)

    @JvmStatic
    fun setStaticBooleanField(clazz: Class<*>, fieldName: String, value: Boolean) =
        EzxFieldHelper.setStaticBooleanField(clazz, fieldName, value)

    @JvmStatic
    fun getStaticIntField(clazz: Class<*>, fieldName: String): Int =
        EzxFieldHelper.getStaticIntField(clazz, fieldName)

    @JvmStatic
    fun setStaticIntField(clazz: Class<*>, fieldName: String, value: Int) =
        EzxFieldHelper.setStaticIntField(clazz, fieldName, value)

    @JvmStatic
    fun getStaticLongField(clazz: Class<*>, fieldName: String): Long =
        EzxFieldHelper.getStaticLongField(clazz, fieldName)

    @JvmStatic
    fun setStaticLongField(clazz: Class<*>, fieldName: String, value: Long) =
        EzxFieldHelper.setStaticLongField(clazz, fieldName, value)

    @JvmStatic
    fun getStaticFloatField(clazz: Class<*>, fieldName: String): Float =
        EzxFieldHelper.getStaticFloatField(clazz, fieldName)

    @JvmStatic
    fun setStaticFloatField(clazz: Class<*>, fieldName: String, value: Float) =
        EzxFieldHelper.setStaticFloatField(clazz, fieldName, value)

    // ==================== 方法调用 ====================

    @JvmStatic
    fun callMethod(instance: Any, methodName: String, vararg args: Any?): Any? =
        EzxMethodHelper.callMethod(instance, methodName, *args)

    @JvmStatic
    fun callStaticMethod(clazz: Class<*>, methodName: String, vararg args: Any?): Any? =
        EzxMethodHelper.callStaticMethod(clazz, methodName, *args)

    @JvmStatic
    fun getParameterTypes(vararg args: Any?): Array<Class<*>> =
        EzxMethodHelper.getParameterTypes(*args)

    @JvmStatic
    fun getParameterClasses(clazz: Class<*>, vararg args: Any?): Array<Class<*>> =
        EzxMethodHelper.getParameterClasses(clazz, *args)

    // ==================== 方法查找 ====================

    /**
     * 查找方法（最佳匹配）
     *
     * @param clazz 声明、继承或覆盖该方法的类
     * @param methodName 方法名
     * @param parameterTypes 方法参数的类型
     * @return 最佳匹配方法的引用
     * @throws NoSuchMethodError 如果找不到合适的方法
     */
    @JvmStatic
    fun findMethodBestMatch(
        clazz: Class<*>,
        methodName: String,
        vararg parameterTypes: Class<*>
    ): Method = EzxMethodHelper.findMethodBestMatch(clazz, methodName, *parameterTypes)

    @JvmStatic
    fun findMethodBestMatch(
        clazz: Class<*>,
        methodName: String,
        vararg args: Any?
    ): Method = EzxMethodHelper.findMethodBestMatch(clazz, methodName, *args)

    /**
     * 查找并调用静态方法（最佳匹配）
     *
     * 注意：此方法同时执行查找和调用，返回调用结果
     */
    @JvmStatic
    fun findStaticMethodBestMatch(
        clazz: Class<*>,
        methodName: String,
        vararg args: Any?
    ): Any? = EzxMethodHelper.callStaticMethod(clazz, methodName, *args)

    @JvmStatic
    fun findMethodExactIfExists(
        clazz: Class<*>,
        methodName: String,
        vararg parameterTypes: Class<*>
    ): Method = EzxMethodHelper.findMethodExactIfExists(clazz, methodName, *parameterTypes)

    /**
     * 查找方法（精确匹配）
     * 支持参数类型为 Class<?> 或 String
     *
     * @param clazz 目标类
     * @param methodName 方法名
     * @param parameterTypes 参数类型数组，可以是 Class<?> 或 String
     * @return Method 对象
     */
    @JvmStatic
    fun findMethodExactIfExists(
        clazz: Class<*>,
        methodName: String,
        vararg parameterTypes: Any
    ): Method = EzxMethodHelper.findMethodExactIfExists(clazz, methodName, *parameterTypes)

    @JvmStatic
    fun findMethodExactIfExists(
        clazzName: String,
        classLoader: ClassLoader,
        methodName: String,
        vararg parameterTypes: Any
    ): Method = EzxMethodHelper.findMethodExactIfExists(clazzName, classLoader, methodName, *parameterTypes)

    /**
     * 查找类中所有具有指定参数类型和返回类型的方法
     *
     * @param clazz 目标类
     * @param returnType 返回类型，如果为 null 则不比较返回类型。使用 void.class 搜索返回 nothing 的方法
     * @param parameterTypes 参数类型
     * @return 匹配的方法数组，已设置为可访问
     */
    @JvmStatic
    fun findMethodsByExactParameters(
        clazz: Class<*>,
        returnType: Class<*>?,
        vararg parameterTypes: Class<*>?
    ): Array<Method> = EzxMethodHelper.findMethodsByExactParameters(clazz, returnType, *parameterTypes)

    /**
     * 调用原始方法
     *
     * @param method 要调用的方法
     * @param thisObject 对于非静态方法，传入 "this" 指针；对于静态方法传 null
     * @param args 方法参数数组
     * @return 方法返回值
     */
    @JvmStatic
    fun invokeOriginalMethod(method: Method, thisObject: Any?, vararg args: Any?): Any? =
        EzxMethodHelper.invokeOriginalMethod(method, thisObject, *args)

    @JvmStatic
    fun newInstance(clazz: Class<*>, vararg args: Any?): Any =
        EzxMethodHelper.newInstance(clazz, *args)

    // ==================== 构造器查找 ====================

    /**
     * 查找构造器（精确匹配）
     *
     * @param clazz 目标类
     * @param parameterTypes 参数类型数组
     * @return 精确匹配的构造器
     * @throws NoSuchMethodError 如果找不到
     */
    @JvmStatic
    fun findConstructorExact(
        clazz: Class<*>,
        vararg parameterTypes: Class<*>
    ): Constructor<*> = EzxMethodHelper.findConstructorExact(clazz, *parameterTypes)

    /**
     * 查找构造器（最佳匹配）
     *
     * @param clazz 目标类
     * @param parameterTypes 参数类型数组
     * @return 最佳匹配的构造器
     * @throws NoSuchMethodError 如果找不到合适的构造器
     */
    @JvmStatic
    fun findConstructorBestMatch(
        clazz: Class<*>,
        vararg parameterTypes: Class<*>
    ): Constructor<*> = EzxMethodHelper.findConstructorBestMatch(clazz, *parameterTypes)

    // ==================== 附加字段 ====================

    @JvmStatic
    fun getSurroundingThis(obj: Any): Any? = EzxFieldHelper.getSurroundingThis(obj)

    @JvmStatic
    fun setAdditionalInstanceField(instance: Any, key: String, value: Any?): Any? =
        EzxFieldHelper.setAdditionalInstanceField(instance, key, value)

    @JvmStatic
    fun getAdditionalInstanceField(instance: Any, key: String): Any? =
        EzxFieldHelper.getAdditionalInstanceField(instance, key)

    @JvmStatic
    fun removeAdditionalInstanceField(instance: Any, key: String): Any? =
        EzxFieldHelper.removeAdditionalInstanceField(instance, key)

    @JvmStatic
    fun setAdditionalStaticField(obj: Any, key: String, value: Any?): Any? =
        EzxFieldHelper.setAdditionalStaticField(obj, key, value)

    @JvmStatic
    fun getAdditionalStaticField(obj: Any, key: String): Any? =
        EzxFieldHelper.getAdditionalStaticField(obj, key)

    @JvmStatic
    fun removeAdditionalStaticField(obj: Any, key: String): Any? =
        EzxFieldHelper.removeAdditionalStaticField(obj, key)

    @JvmStatic
    fun setAdditionalStaticField(clazz: Class<*>, key: String, value: Any?): Any? =
        EzxFieldHelper.setAdditionalStaticField(clazz, key, value)

    @JvmStatic
    fun getAdditionalStaticField(clazz: Class<*>, key: String): Any? =
        EzxFieldHelper.getAdditionalStaticField(clazz, key)

    @JvmStatic
    fun removeAdditionalStaticField(clazz: Class<*>, key: String): Any? =
        EzxFieldHelper.removeAdditionalStaticField(clazz, key)

    // ==================== Application Hook ====================

    /**
     * Application 生命周期回调接口
     */
    interface IApplicationHook {
        /**
         * Application.attach(Context) 之前调用
         */
        fun onApplicationAttachBefore(context: Context) {}

        /**
         * Application.attach(Context) 之后调用
         */
        fun onApplicationAttachAfter(context: Context) {}
    }

    /**
     * Java Consumer 接口
     */
    fun interface ContextConsumer {
        fun accept(context: Context)
    }

    /**
     * 注册 Application 生命周期回调
     *
     * @param hook 回调实例
     */
    @JvmStatic
    fun registerApplicationHook(hook: IApplicationHook) =
        EzxApplicationHookHelper.registerApplicationHook(hook)

    /**
     * 注册 Application 生命周期回调
     *
     * @param before attach 之前的回调，可为 null
     * @param after attach 之后的回调，可为 null
     */
    @JvmStatic
    fun registerApplicationHook(before: ContextConsumer?, after: ContextConsumer?) =
        EzxApplicationHookHelper.registerApplicationHook(before, after)

    /**
     * 仅注册 Application attach 之后的回调
     *
     * @param callback 回调函数
     */
    @JvmStatic
    fun runOnApplicationAttach(callback: ContextConsumer) =
        EzxApplicationHookHelper.runOnApplicationAttach(callback)

    /**
     * 取消注册 Application 生命周期回调
     *
     * @param hook 要取消的回调实例
     */
    @JvmStatic
    fun unregisterApplicationHook(hook: IApplicationHook) =
        EzxApplicationHookHelper.unregisterApplicationHook(hook)

    /**
     * 清除所有 Application 生命周期回调
     */
    @JvmStatic
    fun clearApplicationHooks() = EzxApplicationHookHelper.clearApplicationHooks()

    // ==================== Hook 方法 ====================

    /**
     * Hook 方法
     *
     * @param method 要 Hook 的方法
     * @param callback Hook 回调
     * @return HookHandle 对象，可用于取消 Hook
     */
    @JvmStatic
    fun hookMethod(method: Method, callback: IMethodHook): HookHandle =
        EzxHookHelper.hookMethod(method, callback)

    /**
     * Hook 方法（替换模式）
     *
     * @param method 要 Hook 的方法
     * @param callback 替换回调
     * @return HookHandle 对象，可用于取消 Hook
     */
    @JvmStatic
    fun hookMethod(method: Method, callback: IReplaceHook): HookHandle =
        EzxHookHelper.hookMethod(method, callback)

    /**
     * Hook 构造器
     *
     * @param constructor 要 Hook 的构造器
     * @param callback Hook 回调
     * @return HookHandle 对象，可用于取消 Hook
     */
    @JvmStatic
    fun hookConstructor(constructor: Constructor<*>, callback: IMethodHook): HookHandle =
        EzxHookHelper.hookConstructor(constructor, callback)

    private fun chainHooker(block: (XposedInterface.Chain) -> Any?): Hooker =
        Hooker { chain -> block(chain) }

    @JvmSynthetic
    fun chain(
        method: Method,
        priority: Int = XposedInterface.PRIORITY_DEFAULT,
        exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
        block: (XposedInterface.Chain) -> Any?
    ): HookHandle = EzxHookHelper.chain(method, priority, exceptionMode, chainHooker(block))

    @JvmSynthetic
    fun chain(
        constructor: Constructor<*>,
        priority: Int = XposedInterface.PRIORITY_DEFAULT,
        exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
        block: (XposedInterface.Chain) -> Any?
    ): HookHandle = EzxHookHelper.chain(constructor, priority, exceptionMode, chainHooker(block))

    @JvmStatic
    fun chain(method: Method, hooker: Hooker): HookHandle =
        EzxHookHelper.chain(method, hooker)

    @JvmStatic
    fun chain(
        method: Method,
        priority: Int,
        exceptionMode: XposedInterface.ExceptionMode,
        hooker: Hooker
    ): HookHandle = EzxHookHelper.chain(method, priority, exceptionMode, hooker)

    @JvmStatic
    fun chain(constructor: Constructor<*>, hooker: Hooker): HookHandle =
        EzxHookHelper.chain(constructor, hooker)

    @JvmStatic
    fun chain(
        constructor: Constructor<*>,
        priority: Int,
        exceptionMode: XposedInterface.ExceptionMode,
        hooker: Hooker
    ): HookHandle = EzxHookHelper.chain(constructor, priority, exceptionMode, hooker)

    @JvmSynthetic
    fun findAndChainMethod(
        clazz: Class<*>,
        methodName: String,
        vararg args: Any,
        priority: Int = XposedInterface.PRIORITY_DEFAULT,
        exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
        block: (XposedInterface.Chain) -> Any?
    ): HookHandle = EzxHookHelper.findAndChainMethod(
        clazz,
        methodName,
        priority,
        exceptionMode,
        *args,
        chainHooker(block)
    )

    @JvmStatic
    fun findAndChainMethod(
        clazz: Class<*>,
        methodName: String,
        vararg args: Any
    ): HookHandle = EzxHookHelper.findAndChainMethod(clazz, methodName, *args)

    @JvmStatic
    fun findAndChainMethod(
        clazz: Class<*>,
        methodName: String,
        priority: Int,
        exceptionMode: XposedInterface.ExceptionMode,
        vararg args: Any
    ): HookHandle = EzxHookHelper.findAndChainMethod(clazz, methodName, priority, exceptionMode, *args)

    @JvmSynthetic
    fun findAndChainMethod(
        clazzName: String,
        methodName: String,
        vararg args: Any,
        priority: Int = XposedInterface.PRIORITY_DEFAULT,
        exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
        block: (XposedInterface.Chain) -> Any?
    ): HookHandle = EzxHookHelper.findAndChainMethod(
        clazzName,
        methodName,
        priority,
        exceptionMode,
        *args,
        chainHooker(block)
    )

    @JvmStatic
    fun findAndChainMethod(
        clazzName: String,
        methodName: String,
        vararg args: Any
    ): HookHandle = EzxHookHelper.findAndChainMethod(clazzName, methodName, *args)

    @JvmStatic
    fun findAndChainMethod(
        clazzName: String,
        methodName: String,
        priority: Int,
        exceptionMode: XposedInterface.ExceptionMode,
        vararg args: Any
    ): HookHandle = EzxHookHelper.findAndChainMethod(clazzName, methodName, priority, exceptionMode, *args)

    @JvmSynthetic
    fun findAndChainMethod(
        clazzName: String,
        classLoader: ClassLoader,
        methodName: String,
        vararg args: Any,
        priority: Int = XposedInterface.PRIORITY_DEFAULT,
        exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
        block: (XposedInterface.Chain) -> Any?
    ): HookHandle = EzxHookHelper.findAndChainMethod(
        clazzName,
        classLoader,
        methodName,
        priority,
        exceptionMode,
        *args,
        chainHooker(block)
    )

    @JvmStatic
    fun findAndChainMethod(
        clazzName: String,
        classLoader: ClassLoader,
        methodName: String,
        vararg args: Any
    ): HookHandle = EzxHookHelper.findAndChainMethod(clazzName, classLoader, methodName, *args)

    @JvmStatic
    fun findAndChainMethod(
        clazzName: String,
        classLoader: ClassLoader,
        methodName: String,
        priority: Int,
        exceptionMode: XposedInterface.ExceptionMode,
        vararg args: Any
    ): HookHandle = EzxHookHelper.findAndChainMethod(
        clazzName,
        classLoader,
        methodName,
        priority,
        exceptionMode,
        *args
    )

    @JvmSynthetic
    fun findAndChainConstructor(
        clazz: Class<*>,
        vararg args: Any,
        priority: Int = XposedInterface.PRIORITY_DEFAULT,
        exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
        block: (XposedInterface.Chain) -> Any?
    ): HookHandle = EzxHookHelper.findAndChainConstructor(
        clazz,
        priority,
        exceptionMode,
        *args,
        chainHooker(block)
    )

    @JvmStatic
    fun findAndChainConstructor(
        clazz: Class<*>,
        vararg args: Any
    ): HookHandle = EzxHookHelper.findAndChainConstructor(clazz, *args)

    @JvmStatic
    fun findAndChainConstructor(
        clazz: Class<*>,
        priority: Int,
        exceptionMode: XposedInterface.ExceptionMode,
        vararg args: Any
    ): HookHandle = EzxHookHelper.findAndChainConstructor(clazz, priority, exceptionMode, *args)

    @JvmSynthetic
    fun findAndChainConstructor(
        clazzName: String,
        vararg args: Any,
        priority: Int = XposedInterface.PRIORITY_DEFAULT,
        exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
        block: (XposedInterface.Chain) -> Any?
    ): HookHandle = EzxHookHelper.findAndChainConstructor(
        clazzName,
        priority,
        exceptionMode,
        *args,
        chainHooker(block)
    )

    @JvmStatic
    fun findAndChainConstructor(
        clazzName: String,
        vararg args: Any
    ): HookHandle = EzxHookHelper.findAndChainConstructor(clazzName, *args)

    @JvmStatic
    fun findAndChainConstructor(
        clazzName: String,
        priority: Int,
        exceptionMode: XposedInterface.ExceptionMode,
        vararg args: Any
    ): HookHandle = EzxHookHelper.findAndChainConstructor(clazzName, priority, exceptionMode, *args)

    @JvmSynthetic
    fun findAndChainConstructor(
        clazzName: String,
        classLoader: ClassLoader,
        vararg args: Any,
        priority: Int = XposedInterface.PRIORITY_DEFAULT,
        exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
        block: (XposedInterface.Chain) -> Any?
    ): HookHandle = EzxHookHelper.findAndChainConstructor(
        clazzName,
        classLoader,
        priority,
        exceptionMode,
        *args,
        chainHooker(block)
    )

    @JvmStatic
    fun findAndChainConstructor(
        clazzName: String,
        classLoader: ClassLoader,
        vararg args: Any
    ): HookHandle = EzxHookHelper.findAndChainConstructor(clazzName, classLoader, *args)

    @JvmStatic
    fun findAndChainConstructor(
        clazzName: String,
        classLoader: ClassLoader,
        priority: Int,
        exceptionMode: XposedInterface.ExceptionMode,
        vararg args: Any
    ): HookHandle = EzxHookHelper.findAndChainConstructor(
        clazzName,
        classLoader,
        priority,
        exceptionMode,
        *args
    )

    @JvmStatic
    fun chainAllMethods(clazz: Class<*>, methodName: String, hooker: Hooker): Set<HookHandle> =
        EzxHookHelper.chainAllMethods(clazz, methodName, hooker)

    @JvmStatic
    fun chainAllMethods(clazzName: String, methodName: String, hooker: Hooker): Set<HookHandle> =
        EzxHookHelper.chainAllMethods(clazzName, methodName, hooker)

    @JvmStatic
    fun chainAllMethods(
        clazzName: String,
        classLoader: ClassLoader,
        methodName: String,
        hooker: Hooker
    ): Set<HookHandle> = EzxHookHelper.chainAllMethods(clazzName, classLoader, methodName, hooker)

    @JvmStatic
    fun chainAllConstructors(clazz: Class<*>, hooker: Hooker): Set<HookHandle> =
        EzxHookHelper.chainAllConstructors(clazz, hooker)

    @JvmStatic
    fun chainAllConstructors(clazzName: String, hooker: Hooker): Set<HookHandle> =
        EzxHookHelper.chainAllConstructors(clazzName, hooker)

    @JvmStatic
    fun chainAllConstructors(clazzName: String, classLoader: ClassLoader, hooker: Hooker): Set<HookHandle> =
        EzxHookHelper.chainAllConstructors(clazzName, classLoader, hooker)

    /**
     * 查找并 Hook 方法
     *
     * @param clazz 目标类
     * @param methodName 方法名
     * @param parameterTypes 参数类型
     * @param callback Hook 回调（放在最后以支持 Java lambda）
     * @return Unhook 对象
     */
    @JvmStatic
    fun findAndHookMethod(
        clazz: Class<*>,
        methodName: String,
        vararg parameterTypes: Class<*>,
        callback: IMethodHook
    ): HookHandle {
        val method = EzxMethodHelper.findMethodExactIfExists(clazz, methodName, *parameterTypes)
        return EzxHookHelper.hookMethod(method, callback)
    }

    @JvmStatic
    fun findAndHookMethod(
        clazzName: String,
        methodName: String,
        vararg args: Any,
    ): HookHandle = EzxHookHelper.findAndHookMethod(clazzName, methodName, *args)

    @JvmStatic
    fun findAndHookMethod(
        clazzName: String,
        classLoader: ClassLoader,
        methodName: String,
        vararg args: Any
    ): HookHandle = EzxHookHelper.findAndHookMethod(clazzName, classLoader, methodName, *args)

    /**
     * 查找并 Hook 方法
     *
     * @param clazz 目标类
     * @param methodName 方法名
     * @param args 参数类型数组，最后一个元素必须是 IMethodHook
     * @return HookHandle 对象
     */
    @JvmStatic
    fun findAndHookMethod(
        clazz: Class<*>,
        methodName: String,
        vararg args: Any
    ): HookHandle = EzxHookHelper.findAndHookMethod(clazz, methodName, *args)

    @JvmStatic
    fun findAndHookMethodReplace(
        clazzName: String,
        methodName: String,
        vararg args: Any,
    ): HookHandle = EzxHookHelper.findAndHookMethodReplace(clazzName, methodName, *args)

    @JvmStatic
    fun findAndHookMethodReplace(
        clazzName: String,
        classLoader: ClassLoader,
        methodName: String,
        vararg args: Any
    ): HookHandle = EzxHookHelper.findAndHookMethodReplace(clazzName, classLoader, methodName, *args)

    /**
     * 查找并 Hook 方法（替换模式）
     *
     * @param clazz 目标类
     * @param methodName 方法名
     * @param args 参数类型数组，最后一个元素必须是 IReplaceHook
     * @return HookHandle 对象
     */
    @JvmStatic
    fun findAndHookMethodReplace(
        clazz: Class<*>,
        methodName: String,
        vararg args: Any
    ): HookHandle = EzxHookHelper.findAndHookMethodReplace(clazz, methodName, *args)

    /**
     * 查找并 Hook 构造器
     *
     * @param clazz 目标类
     * @param parameterTypes 参数类型
     * @param callback Hook 回调
     * @return HookHandle 对象
     */
    @JvmStatic
    fun findAndHookConstructor(
        clazz: Class<*>,
        vararg parameterTypes: Class<*>,
        callback: IMethodHook
    ): HookHandle {
        val constructor = EzxMethodHelper.findConstructorExact(clazz, *parameterTypes)
        return EzxHookHelper.hookConstructor(constructor, callback)
    }

    @JvmStatic
    fun findAndHookConstructor(
        clazzName: String,
        vararg args: Any
    ): HookHandle = EzxHookHelper.findAndHookConstructor(clazzName, *args)

    @JvmStatic
    fun findAndHookConstructor(
        clazzName: String,
        classLoader: ClassLoader,
        vararg args: Any
    ): HookHandle = EzxHookHelper.findAndHookConstructor(clazzName, classLoader, *args)

    /**
     * 查找并 Hook 构造器
     *
     * @param clazz 目标类
     * @param args 参数类型数组，最后一个元素必须是 IMethodHook
     * @return HookHandle 对象
     */
    @JvmStatic
    fun findAndHookConstructor(clazz: Class<*>, vararg args: Any): HookHandle =
        EzxHookHelper.findAndHookConstructor(clazz, *args)

    @JvmStatic
    fun hookAllMethods(
        clazzName: String,
        methodName: String,
        callback: IMethodHook
    ): List<HookHandle> = EzxHookHelper.hookAllMethods(clazzName, methodName, callback)

    @JvmStatic
    fun hookAllMethods(
        clazzName: String,
        classLoader: ClassLoader,
        methodName: String,
        callback: IMethodHook
    ): List<HookHandle> = EzxHookHelper.hookAllMethods(clazzName, classLoader, methodName, callback)

    /**
     * Hook 类中所有指定名称的方法
     *
     * @param clazz 目标类
     * @param methodName 方法名
     * @param callback Hook 回调
     * @return HookHandle 对象列表
     */
    @JvmStatic
    fun hookAllMethods(
        clazz: Class<*>,
        methodName: String,
        callback: IMethodHook
    ): List<HookHandle> = EzxHookHelper.hookAllMethods(clazz, methodName, callback)

    /**
     * Hook 类中所有构造器
     *
     * @param clazz 目标类
     * @param callback Hook 回调
     * @return HookHandle 对象列表
     */
    @JvmStatic
    fun hookAllConstructors(clazz: Class<*>, callback: IMethodHook): List<HookHandle> =
        EzxHookHelper.hookAllConstructors(clazz, callback)

    // ==================== 便捷 Hook 工具 ====================

    /**
     * 创建一个返回常量值的 Hook 回调
     *
     * @param result 要返回的常量值
     * @return IMethodHook 实例
     */
    @JvmStatic
    fun returnConstant(result: Any?): IMethodHook = EzxHookHelper.returnConstant(result)

    /**
     * 什么都不做的 Hook（阻止原方法执行，返回 null）
     */
    @JvmField
    val DO_NOTHING: IMethodHook = EzxHookHelper.DO_NOTHING

    // ==================== deoptimize 相关 ====================

    @JvmStatic
    fun deoptimize(method: Method): Boolean = EzxHookHelper.deoptimize(method)

    @JvmStatic
    fun deoptimize(constructor: Constructor<*>): Boolean = EzxHookHelper.deoptimize(constructor)

    @JvmStatic
    fun deoptimizeMethods(clazz: Class<*>, vararg names: String?) =
        EzxHookHelper.deoptimizeMethods(clazz, *names)

    @JvmStatic
    fun libHook(method: Method, hooker: Class<out Hooker>): HookHandle =
        EzxHookHelper.libHook(method, hooker)

    @JvmStatic
    fun invokeSuperMethod(methodName: String, thisObject: Any, vararg args: Any?) =
        EzxMethodHelper.invokeSuperMethod(methodName, thisObject, *args)
}
