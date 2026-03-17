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

import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook
import com.sevtinge.hyperceiler.libhook.callback.IReplaceHook
import io.github.kyuubiran.ezxhelper.core.ClassLoaderProvider.safeClassLoader
import io.github.kyuubiran.ezxhelper.core.finder.ConstructorFinder
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * 方法/构造器查找与调用工具
 * 封装方法查找（精确匹配/最佳匹配）、调用、参数类型推断等功能
 *
 * 修复项：
 * - 缓存使用 HashMap 替代 WeakHashMap（String 键不会被 GC 回收）
 * - primitiveToBoxed 映射提取为常量（避免热路径重复分配）
 * - getParameterClasses 统一使用 safeClassLoader
 * - invokeSuperMethod 修复参数传递 bug（*paramTypes → *args）
 * - callStaticMethod 与 callMethod 行为一致（纯查找+调用）
 */
internal object EzxMethodHelper {

    private const val TAG = "EzxMethodHelper"

    /** 方法缓存（String 键不会被 GC，使用 HashMap 替代 WeakHashMap） */
    private val methodCache = HashMap<String, Method?>()

    /** 构造器缓存 */
    private val constructorCache = HashMap<String, Constructor<*>?>()

    /** 基本类型到包装类型的映射（常量，避免每次调用创建） */
    private val PRIMITIVE_TO_BOXED = mapOf(
        java.lang.Integer.TYPE to Integer::class.java,
        java.lang.Long.TYPE to java.lang.Long::class.java,
        java.lang.Float.TYPE to java.lang.Float::class.java,
        java.lang.Double.TYPE to java.lang.Double::class.java,
        java.lang.Boolean.TYPE to java.lang.Boolean::class.java,
        java.lang.Byte.TYPE to java.lang.Byte::class.java,
        java.lang.Short.TYPE to java.lang.Short::class.java,
        java.lang.Character.TYPE to java.lang.Character::class.java
    )

    // ==================== 方法调用 ====================

    fun callMethod(instance: Any, methodName: String, vararg args: Any?): Any? {
        return findMethodBestMatch(instance::class.java, methodName, *args)
            .invoke(instance, *args)
    }

    /**
     * 调用静态方法
     *
     * @param clazz 目标类
     * @param methodName 方法名
     * @param args 方法参数
     * @return 调用结果
     */
    fun callStaticMethod(clazz: Class<*>, methodName: String, vararg args: Any?): Any? {
        val method = findMethodBestMatch(clazz, methodName, *args)
        return method.invoke(null, *args)
    }

    // ==================== 参数类型工具 ====================

    @Suppress("UNCHECKED_CAST")
    fun getParameterTypes(vararg args: Any?): Array<Class<*>> {
        val result = arrayOfNulls<Class<*>>(args.size)
        for (i in args.indices) {
            result[i] = args[i]?.javaClass
        }
        return result as Array<Class<*>>
    }

    /**
     * 解析参数类型数组（支持 Class<?> 和 String 类型名）
     * 会过滤掉 IMethodHook 和 IReplaceHook 回调参数
     *
     * @param clazz 当前目标类（用于回退 classLoader）
     * @param args 参数数组
     * @return 解析后的 Class 数组
     */
    fun getParameterClasses(clazz: Class<*>, vararg args: Any?): Array<Class<*>> {
        return args.filterNot { it is IMethodHook || it is IReplaceHook }
            .mapIndexed { index, arg ->
                when (arg) {
                    null -> throw IllegalArgumentException(
                        "Parameter type at index $index must not be null"
                    )
                    is Class<*> -> arg
                    is String -> runCatching {
                        EzxClassHelper.findClass(arg, safeClassLoader)
                    }.recoverCatching {
                        EzxClassHelper.findClass(arg, clazz.classLoader)
                    }.getOrElse { e ->
                        throw IllegalArgumentException(
                            "Failed to find class '$arg'", e
                        )
                    }
                    else -> throw IllegalArgumentException(
                        "Parameter type at index $index must be Class<?> or String, " +
                            "but got ${arg.javaClass.simpleName}"
                    )
                }
            }.toTypedArray()
    }

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
    fun findMethodBestMatch(
        clazz: Class<*>,
        methodName: String,
        vararg parameterTypes: Class<*>
    ): Method {
        val fullMethodName =
            "${clazz.name}#$methodName${getParametersString(parameterTypes)}#bestmatch"

        synchronized(methodCache) {
            if (methodCache.containsKey(fullMethodName)) {
                val method = methodCache[fullMethodName]
                    ?: throw NoSuchMethodError(fullMethodName)
                return method
            }
        }

        fun cacheAndReturn(method: Method): Method {
            method.isAccessible = true
            synchronized(methodCache) {
                methodCache[fullMethodName] = method
            }
            return method
        }

        try {
            val exactMethod = findMethodExactIfExists(clazz, methodName, *parameterTypes)
            return cacheAndReturn(exactMethod)
        } catch (_: NoSuchMethodError) {
        }

        var bestMatch: Method? = null
        var clz: Class<*>? = clazz
        var considerPrivateMethods = true

        while (clz != null) {
            for (method in clz.declaredMethods) {
                if (!considerPrivateMethods && Modifier.isPrivate(method.modifiers)) {
                    continue
                }

                if (method.name == methodName && isAssignable(parameterTypes, method.parameterTypes)) {
                    if (bestMatch == null || compareParameterTypes(
                            method.parameterTypes,
                            bestMatch.parameterTypes,
                            parameterTypes
                        ) < 0
                    ) {
                        bestMatch = method
                    }
                }
            }
            considerPrivateMethods = false
            clz = clz.superclass
        }

        if (bestMatch != null) {
            return cacheAndReturn(bestMatch)
        } else {
            synchronized(methodCache) {
                methodCache[fullMethodName] = null
            }
            throw NoSuchMethodError(fullMethodName)
        }
    }

    fun findMethodBestMatch(
        clazz: Class<*>,
        methodName: String,
        vararg args: Any?
    ): Method {
        val parameterTypes = getParameterTypes(*args)
        return findMethodBestMatch(clazz, methodName, *parameterTypes)
    }

    private fun findMethodByReflection(
        clazz: Class<*>,
        methodName: String,
        vararg parameterTypes: Class<*>
    ): Method? {
        val method = try {
            if (parameterTypes.isEmpty()) {
                clazz.getDeclaredMethod(methodName)
            } else {
                clazz.getDeclaredMethod(methodName, *parameterTypes)
            }
        } catch (_: Throwable) {
            null
        }
        method?.isAccessible = true
        return method
    }

    fun findMethodExactIfExists(
        clazz: Class<*>,
        methodName: String,
        vararg parameterTypes: Class<*>
    ): Method {
        val fullMethodName =
            "${clazz.name}#$methodName${getParametersString(parameterTypes)}#exact"

        synchronized(methodCache) {
            if (methodCache.containsKey(fullMethodName)) {
                return methodCache[fullMethodName] ?: throw NoSuchMethodError(fullMethodName)
            }
        }

        var currentClass: Class<*>? = clazz

        while (currentClass != null) {
            val searchClass = currentClass ?: break
            val method = runCatching {
                if (parameterTypes.isEmpty()) {
                    MethodFinder.fromClass(searchClass)
                        .filterByName(methodName)
                        .firstOrNull()
                } else {
                    MethodFinder.fromClass(searchClass)
                        .filterByName(methodName)
                        .filterByParamTypes(*parameterTypes)
                        .firstOrNull()
                }
            }.recoverCatching { e ->
                XposedLog.w(TAG, "MethodFinder failed for ${searchClass.name}.$methodName, fallback to reflection", e)
                findMethodByReflection(searchClass, methodName, *parameterTypes)
            }.getOrNull()

            if (method != null) {
                method.isAccessible = true
                synchronized(methodCache) {
                    methodCache[fullMethodName] = method
                }
                return method
            }

            currentClass = currentClass.superclass
        }

        synchronized(methodCache) {
            methodCache[fullMethodName] = null
        }
        throw NoSuchMethodError(fullMethodName)
    }

    /**
     * 查找方法（精确匹配）
     * 支持参数类型为 Class<?> 或 String
     *
     * @param clazz 目标类
     * @param methodName 方法名
     * @param parameterTypes 参数类型数组，可以是 Class<?> 或 String
     * @return Method 对象
     */
    fun findMethodExactIfExists(
        clazz: Class<*>,
        methodName: String,
        vararg parameterTypes: Any
    ): Method {
        val paramTypes = getParameterClasses(clazz, *parameterTypes)
        return findMethodExactIfExists(clazz, methodName, *paramTypes)
    }

    fun findMethodExactIfExists(
        clazzName: String,
        classLoader: ClassLoader,
        methodName: String,
        vararg parameterTypes: Any
    ): Method {
        val clazz = EzxClassHelper.findClass(clazzName, classLoader)
        return findMethodExactIfExists(clazz, methodName, *parameterTypes)
    }

    /**
     * 查找类中所有具有指定参数类型和返回类型的方法
     *
     * @param clazz 目标类
     * @param returnType 返回类型，如果为 null 则不比较返回类型。使用 void.class 搜索返回 nothing 的方法
     * @param parameterTypes 参数类型
     * @return 匹配的方法数组，已设置为可访问
     */
    fun findMethodsByExactParameters(
        clazz: Class<*>,
        returnType: Class<*>?,
        vararg parameterTypes: Class<*>?
    ): Array<Method> {
        val result = mutableListOf<Method>()

        for (method in clazz.declaredMethods) {
            if (returnType != null && returnType != method.returnType) {
                continue
            }

            val methodParameterTypes = method.parameterTypes
            if (parameterTypes.size != methodParameterTypes.size) {
                continue
            }

            var match = true
            for (i in parameterTypes.indices) {
                if (parameterTypes[i] != methodParameterTypes[i]) {
                    match = false
                    break
                }
            }

            if (!match) {
                continue
            }

            method.isAccessible = true
            result.add(method)
        }

        return result.toTypedArray()
    }

    // ==================== 构造器查找 ====================

    /**
     * 查找构造器（精确匹配）
     *
     * @param clazz 目标类
     * @param parameterTypes 参数类型数组
     * @return 精确匹配的构造器
     * @throws NoSuchMethodError 如果找不到
     */
    fun findConstructorExact(
        clazz: Class<*>,
        vararg parameterTypes: Class<*>
    ): Constructor<*> {
        return if (parameterTypes.isEmpty()) {
            ConstructorFinder.fromClass(clazz)
                .firstOrNull()
        } else {
            ConstructorFinder.fromClass(clazz)
                .filterByParamTypes(*parameterTypes)
                .firstOrNull()
        } ?: throw NoSuchMethodError(
            "${clazz.name}${getParametersString(parameterTypes)}#exact"
        )
    }

    /**
     * 查找构造器（最佳匹配）
     *
     * @param clazz 目标类
     * @param parameterTypes 参数类型数组
     * @return 最佳匹配的构造器
     * @throws NoSuchMethodError 如果找不到合适的构造器
     */
    fun findConstructorBestMatch(
        clazz: Class<*>,
        vararg parameterTypes: Class<*>
    ): Constructor<*> {
        val fullConstructorName =
            "${clazz.name}${getParametersString(parameterTypes)}#bestmatch"

        synchronized(constructorCache) {
            if (constructorCache.containsKey(fullConstructorName)) {
                return constructorCache[fullConstructorName] ?: throw NoSuchMethodError(
                    fullConstructorName
                )
            }
        }

        fun cacheAndReturn(constructor: Constructor<*>): Constructor<*> {
            constructor.isAccessible = true
            synchronized(constructorCache) {
                constructorCache[fullConstructorName] = constructor
            }
            return constructor
        }

        try {
            val exactConstructor = findConstructorExact(clazz, *parameterTypes)
            return cacheAndReturn(exactConstructor)
        } catch (_: NoSuchMethodError) {
        }

        var bestMatch: Constructor<*>? = null

        for (constructor in clazz.declaredConstructors) {
            if (isAssignable(parameterTypes, constructor.parameterTypes)) {
                if (bestMatch == null || compareParameterTypes(
                        constructor.parameterTypes,
                        bestMatch.parameterTypes,
                        parameterTypes
                    ) < 0
                ) {
                    bestMatch = constructor
                }
            }
        }

        if (bestMatch != null) {
            return cacheAndReturn(bestMatch)
        } else {
            synchronized(constructorCache) {
                constructorCache[fullConstructorName] = null
            }
            throw NoSuchMethodError(fullConstructorName)
        }
    }

    // ==================== 方法调用工具 ====================

    fun newInstance(clazz: Class<*>, vararg args: Any?): Any {
        val parameterTypes = getParameterTypes(*args)
        val ctor = findConstructorBestMatch(clazz, *parameterTypes)
        return ctor.newInstance(*args)
    }

    /**
     * 调用原始方法
     *
     * @param method 要调用的方法
     * @param thisObject 对于非静态方法，传入 "this" 指针；对于静态方法传 null
     * @param args 方法参数数组
     * @return 方法返回值
     * @throws NullPointerException 如果非静态方法的 receiver 为 null
     * @throws IllegalAccessException 如果方法不可访问
     * @throws IllegalArgumentException 如果参数数量或类型不匹配
     * @throws java.lang.reflect.InvocationTargetException 如果被调用的方法抛出异常
     */
    fun invokeOriginalMethod(method: Method, thisObject: Any?, vararg args: Any?): Any? {
        return try {
            EzxModuleHolder.xposedModule.invokeOrigin(method, thisObject, *args)
        } catch (t: Throwable) {
            XposedLog.e(TAG, "invokeOriginalMethod failed for ${formatMethodSignature(method)}", t)
            throw t
        }
    }

    /**
     * 调用父类方法
     *
     * @param methodName 方法名
     * @param thisObject "this" 指针
     * @param args 方法参数
     */
    fun invokeSuperMethod(methodName: String, thisObject: Any, vararg args: Any?) {
        val paramTypes = getParameterTypes(*args)
        val superClass = thisObject::class.java.superclass as Class<*>
        val method = findMethodBestMatch(superClass, methodName, *paramTypes)
        EzxModuleHolder.xposedModule.invokeSpecial(method, thisObject, *args)
    }

    // ==================== 内部辅助方法 ====================

    /**
     * 格式化方法签名用于日志输出
     */
    fun formatMethodSignature(method: Method): String {
        val params = method.parameterTypes.joinToString(", ") { it.simpleName }
        return "${method.declaringClass.simpleName}#${method.name}($params)"
    }

    /**
     * 获取参数类型字符串表示
     *
     * @param parameterTypes 参数类型数组
     * @return 格式化的参数字符串，如 "(String,int)"
     */
    fun getParametersString(parameterTypes: Array<out Class<*>?>): String {
        return parameterTypes.joinToString(",", "(", ")") { it?.simpleName ?: "null" }
    }

    /**
     * 检查参数类型是否可分配
     *
     * @param parameterTypes 实际参数类型
     * @param methodParameterTypes 方法形式参数类型
     * @param autoboxing 是否允许自动装箱/拆箱
     * @return 如果所有参数都可分配则返回 true
     */
    private fun isAssignable(
        parameterTypes: Array<out Class<*>?>,
        methodParameterTypes: Array<Class<*>>,
        autoboxing: Boolean = true
    ): Boolean {
        if (parameterTypes.size != methodParameterTypes.size) {
            return false
        }

        for (i in parameterTypes.indices) {
            val paramType = parameterTypes[i] ?: continue  // null 可以匹配任何类型
            val methodParamType = methodParameterTypes[i]

            // 直接赋值
            if (methodParamType.isAssignableFrom(paramType)) {
                continue
            }

            // 自动装箱/拆箱支持
            if (autoboxing && isAutoboxingCompatible(paramType, methodParamType)) {
                continue
            }

            return false
        }

        return true
    }

    /**
     * 比较参数类型的匹配程度
     *
     * 返回值：
     * - 负数：method1 比 method2 更匹配
     * - 0：两者匹配程度相同
     * - 正数：method2 比 method1 更匹配
     *
     * @param method1ParameterTypes 第一个方法的参数类型
     * @param method2ParameterTypes 第二个方法的参数类型
     * @param actualParameterTypes 实际参数类型
     * @return 比较结果
     */
    private fun compareParameterTypes(
        method1ParameterTypes: Array<Class<*>>,
        method2ParameterTypes: Array<Class<*>>,
        actualParameterTypes: Array<out Class<*>?>
    ): Int {
        for (i in actualParameterTypes.indices) {
            val method1ParamType = method1ParameterTypes[i]
            val method2ParamType = method2ParameterTypes[i]
            val actualParamType = actualParameterTypes[i]

            if (method1ParamType == method2ParamType || actualParamType == null) {
                continue
            }

            when {
                method1ParamType == actualParamType -> {
                    return -1
                }
                method2ParamType == actualParamType -> {
                    return 1
                }
                method1ParamType.isAssignableFrom(method2ParamType) -> {
                    return 1
                }
                method2ParamType.isAssignableFrom(method1ParamType) -> {
                    return -1
                }
            }
        }

        return 0
    }

    /**
     * 检查两个类型是否兼容（支持自动装箱/拆箱）
     */
    private fun isAutoboxingCompatible(from: Class<*>, to: Class<*>): Boolean {
        return (PRIMITIVE_TO_BOXED[from] == to) || (PRIMITIVE_TO_BOXED[to] == from)
    }
}
