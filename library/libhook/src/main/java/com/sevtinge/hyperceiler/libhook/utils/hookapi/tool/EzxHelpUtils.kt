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

import android.app.Application
import android.content.Context
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook
import com.sevtinge.hyperceiler.libhook.callback.IReplaceHook
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog
import io.github.kyuubiran.ezxhelper.core.ClassLoaderProvider.classLoader
import io.github.kyuubiran.ezxhelper.core.ClassLoaderProvider.safeClassLoader
import io.github.kyuubiran.ezxhelper.core.finder.ConstructorFinder
import io.github.kyuubiran.ezxhelper.core.finder.FieldFinder
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder
import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedInterface.MethodUnhooker
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.Arrays
import java.util.WeakHashMap

object EzxHelpUtils {

    @JvmStatic
    private lateinit var xposedModule: XposedModule
    private val additionalFields = WeakHashMap<Any, HashMap<String, Any?>>()

    private val methodCache = WeakHashMap<String, Method?>()
    private val constructorCache = WeakHashMap<String, Constructor<*>?>()
    private val fieldCache = WeakHashMap<String, Field?>()

    private const val TAG = "EzxHelpUtils"

    @JvmStatic
    fun setXposedModule(module: XposedModule) {
        xposedModule = module
    }

    @JvmStatic
    fun findClass(name: String): Class<*> {
        return findClassInternal(name, safeClassLoader)
            ?: throw ClassNotFoundException("Class not found: $name")
    }

    @JvmStatic
    fun findClass(name: String, classLoader: ClassLoader?): Class<*> {
        val cl = classLoader ?: safeClassLoader
        return findClassInternal(name, cl)
            ?: throw ClassNotFoundException("Class not found: $name")
    }

    @JvmStatic
    fun findClassIfExists(name: String): Class<*>? {
        return findClassInternal(name, safeClassLoader)
    }

    @JvmStatic
    fun findClassIfExists(name: String, classLoader: ClassLoader?): Class<*>? {
        val cl = classLoader ?: safeClassLoader
        return findClassInternal(name, cl)
    }

    /**
     * 带有内部类回退的内部类查找。
     * 支持 "a.b.C.D" 和 "a.b.C$D" 两种内部类表示法
     */
    private fun findClassInternal(name: String, classLoader: ClassLoader): Class<*>? {
        // Direct lookup
        try {
            return Class.forName(name, false, classLoader)
        } catch (_: ClassNotFoundException) {}

        // Inner class fallback: replace '.' with '$' from right to left
        val chars = name.toCharArray()
        for (i in chars.indices.reversed()) {
            if (chars[i] == '.') {
                chars[i] = '$'
                try {
                    return Class.forName(String(chars), false, classLoader)
                } catch (_: ClassNotFoundException) {}
            }
        }
        return null
    }

    /**
     * 在类层次结构中查找字段（包括父类）
     *
     * @param clazz 目标类
     * @param fieldName 字段名
     * @return Field 对象
     * @throws NoSuchFieldError 如果字段不存在
     */
    @JvmStatic
    fun findField(clazz: Class<*>, fieldName: String): Field {
        val fullFieldName = "${clazz.name}#$fieldName"

        synchronized(fieldCache) {
            if (fieldCache.containsKey(fullFieldName)) {
                return fieldCache[fullFieldName] ?: throw NoSuchFieldError(fullFieldName)
            }
        }

        val field = FieldFinder.fromClass(clazz)
            .findSuper()
            .firstOrNullByName(fieldName)

        synchronized(fieldCache) {
            fieldCache[fullFieldName] = field
        }

        return field?.apply { isAccessible = true }
            ?: throw NoSuchFieldError(fullFieldName)
    }

    @JvmStatic
    fun findFieldIfExists(clazz: Class<*>, fieldName: String): Field? {
        return try {
            findField(clazz, fieldName)
        } catch (_: NoSuchFieldError) {
            null
        }
    }

    @JvmStatic
    fun findFirstFieldByExactType(clazz: Class<*>, type: Class<*>): Field {
        val cacheKey = "${clazz.name}#type:${type.name}"

        synchronized(fieldCache) {
            if (fieldCache.containsKey(cacheKey)) {
                return fieldCache[cacheKey] ?: throw NoSuchFieldError(cacheKey)
            }
        }

        val field = FieldFinder.fromClass(clazz)
            .findSuper()
            .firstOrNullByType(type)

        synchronized(fieldCache) {
            fieldCache[cacheKey] = field
        }

        return field?.apply { isAccessible = true }
            ?: throw NoSuchFieldError("Field of type ${type.name} in class ${clazz.name}")
    }

    @JvmStatic
    fun findFirstFieldByExactTypeIfExists(clazz: Class<*>, type: Class<*>): Field? {
        return FieldFinder.fromClass(clazz)
            .findSuper()
            .firstOrNullByType(type)?.apply { isAccessible = true }
    }

    /**
     * 获取对象字段值
     *
     * @param instance 对象实例
     * @param fieldName 字段名
     * @return 字段值
     */
    @JvmStatic
    fun getObjectField(instance: Any, fieldName: String): Any? {
        return try {
            val field = findField(instance.javaClass, fieldName)
            field.get(instance)
        } catch (e: IllegalAccessException) {
            XposedLog.e(TAG, "Failed to get field: $fieldName", e)
            throw IllegalAccessError(e.message)
        }
    }

    /**
     * 设置对象字段值（支持父类字段）
     *
     * @param instance 对象实例
     * @param fieldName 字段名
     * @param value 要设置的值
     */
    @JvmStatic
    fun setObjectField(instance: Any, fieldName: String, value: Any?) {
        try {
            val field = findField(instance.javaClass, fieldName)
            field.set(instance, value)
        } catch (e: IllegalAccessException) {
            XposedLog.e(TAG, "Failed to set field: $fieldName", e)
            throw IllegalAccessError(e.message)
        } catch (e: IllegalArgumentException) {
            XposedLog.e(TAG, "Illegal argument for field: $fieldName", e)
            throw e
        }
    }

    @JvmStatic
    fun getBooleanField(instance: Any, fieldName: String): Boolean {
        return getObjectField(instance, fieldName) as? Boolean ?: false
    }

    @JvmStatic
    fun setBooleanField(instance: Any, fieldName: String, value: Boolean) {
        setObjectField(instance, fieldName, value)
    }

    @JvmStatic
    fun getIntField(instance: Any, fieldName: String): Int {
        return getObjectField(instance, fieldName) as? Int ?: 0
    }

    @JvmStatic
    fun setIntField(instance: Any, fieldName: String, value: Int) {
        setObjectField(instance, fieldName, value)
    }

    @JvmStatic
    fun getLongField(instance: Any, fieldName: String): Long {
        return getObjectField(instance, fieldName) as? Long ?: 0L
    }

    @JvmStatic
    fun setLongField(instance: Any, fieldName: String, value: Long) {
        setObjectField(instance, fieldName, value)
    }

    @JvmStatic
    fun getFloatField(instance: Any, fieldName: String): Float {
        return getObjectField(instance, fieldName) as? Float ?: 0f
    }

    @JvmStatic
    fun setFloatField(instance: Any, fieldName: String, value: Float) {
        setObjectField(instance, fieldName, value)
    }

    @JvmStatic
    fun getStaticObjectField(clazz: Class<*>, fieldName: String): Any? {
        return try {
            val field = findField(clazz, fieldName)
            field.get(null)
        } catch (e: IllegalAccessException) {
            XposedLog.e(TAG, "Failed to get static field: $fieldName", e)
            throw IllegalAccessError(e.message)
        }
    }

    @JvmStatic
    fun setStaticObjectField(clazz: Class<*>, fieldName: String, value: Any?) {
        try {
            val field = findField(clazz, fieldName)
            field.set(null, value)
        } catch (e: IllegalAccessException) {
            XposedLog.e(TAG, "Failed to set static field: $fieldName", e)
            throw IllegalAccessError(e.message)
        }
    }

    @JvmStatic
    fun getStaticBooleanField(clazz: Class<*>, fieldName: String): Boolean {
        return getStaticObjectField(clazz, fieldName) as? Boolean ?: false
    }

    @JvmStatic
    fun setStaticBooleanField(clazz: Class<*>, fieldName: String, value: Boolean) {
        setStaticObjectField(clazz, fieldName, value)
    }

    @JvmStatic
    fun getStaticIntField(clazz: Class<*>, fieldName: String): Int {
        return getStaticObjectField(clazz, fieldName) as? Int ?: 0
    }

    @JvmStatic
    fun setStaticIntField(clazz: Class<*>, fieldName: String, value: Int) {
        setStaticObjectField(clazz, fieldName, value)
    }

    @JvmStatic
    fun getStaticLongField(clazz: Class<*>, fieldName: String): Long {
        return getStaticObjectField(clazz, fieldName) as? Long ?: 0L
    }

    @JvmStatic
    fun setStaticLongField(clazz: Class<*>, fieldName: String, value: Long) {
        setStaticObjectField(clazz, fieldName, value)
    }

    @JvmStatic
    fun getStaticFloatField(clazz: Class<*>, fieldName: String): Float {
        return getStaticObjectField(clazz, fieldName) as? Float ?: 0f
    }

    @JvmStatic
    fun setStaticFloatField(clazz: Class<*>, fieldName: String, value: Float) {
        setStaticObjectField(clazz, fieldName, value)
    }

    @JvmStatic
    fun callMethod(instance: Any, methodName: String, vararg args: Any?): Any? {
        return findMethodBestMatch(instance::class.java, methodName, *args)
            .invoke(instance, *args)
    }

    @JvmStatic
    fun callStaticMethod(clazz: Class<*>, methodName: String, vararg args: Any?): Any? {
        return findStaticMethodBestMatch(clazz, methodName, *args)
    }

    @JvmStatic
    fun getParameterTypes(vararg args: Any?): Array<Class<*>> {
        val result = arrayOfNulls<Class<*>>(args.size)
        for (i in args.indices) {
            result[i] = args[i]?.javaClass
        }
        return result as Array<Class<*>>
    }

    @JvmStatic
    fun getParameterClasses(clazz: Class<*>, vararg args: Any?): Array<Class<*>> {
        return args.filterNot { it is IMethodHook || it is IReplaceHook }
            .mapIndexed { index, arg ->
                when (arg) {
                    null -> throw IllegalArgumentException(
                        "Parameter type at index $index must not be null"
                    )
                    is Class<*> -> arg
                    is String -> runCatching {
                        findClass(arg, classLoader)
                    }.recoverCatching {
                        findClass(arg, clazz.classLoader)
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

    @JvmStatic
    fun findMethodBestMatch(
        clazz: Class<*>,
        methodName: String,
        vararg args: Any?
    ): Method {
        val parameterTypes = getParameterTypes(*args)
        return findMethodBestMatch(clazz, methodName, *parameterTypes)
    }

    @JvmStatic
    fun findStaticMethodBestMatch(
        clazz: Class<*>,
        methodName: String,
        vararg args: Any?
    ): Any? {
        val method = findMethodBestMatch(clazz, methodName, *args)
        return try {
            method.invoke(null, *args)
        } catch (t: Throwable) {
            XposedLog.e(TAG, "invokeStaticMethodBestMatch failed for ${formatMethodSignature(method)}", t)
        }
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

    @JvmStatic
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
            val method = runCatching {
                if (parameterTypes.isEmpty()) {
                    MethodFinder.fromClass(currentClass)
                        .filterByName(methodName)
                        .firstOrNull()
                } else {
                    MethodFinder.fromClass(currentClass)
                        .filterByName(methodName)
                        .filterByParamTypes(*parameterTypes)
                        .firstOrNull()
                }
            }.recoverCatching { e ->
                XposedLog.w(TAG, "MethodFinder failed for ${currentClass.name}.$methodName, fallback to reflection", e)
                findMethodByReflection(currentClass, methodName, *parameterTypes)
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
     * 查找方法（精确匹配，如果不存在返回 null）
     * 支持参数类型为 Class<?> 或 String
     *
     * @param clazz 目标类
     * @param methodName 方法名
     * @param parameterTypes 参数类型数组，可以是 Class<?> 或 String
     * @return Method 对象，如果不存在返回 null
     */
    @JvmStatic
    fun findMethodExactIfExists(
        clazz: Class<*>,
        methodName: String,
        vararg parameterTypes: Any
    ): Method {
        val paramTypes = getParameterClasses(clazz, *parameterTypes)
        return findMethodExactIfExists(clazz, methodName, *paramTypes)
    }

    @JvmStatic
    fun findMethodExactIfExists(
        clazzName: String,
        classLoader: ClassLoader,
        methodName: String,
        vararg parameterTypes: Any
    ): Method {
        val clazz = findClass(clazzName, classLoader)
        return findMethodExactIfExists(clazz, methodName, *parameterTypes)
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
     * @throws InvocationTargetException 如果被调用的方法抛出异常
     */
    @JvmStatic
    fun invokeOriginalMethod(method: Method, thisObject: Any?, vararg args: Any?): Any? {
        return try {
            xposedModule.invokeOrigin(method, thisObject, *args)
        } catch (t: Throwable) {
            XposedLog.e(TAG, "invokeOriginalMethod failed for ${formatMethodSignature(method)}", t)
            throw t
        }
    }

    @JvmStatic
    fun newInstance(clazz: Class<*>, vararg args: Any?): Any {
        val parameterTypes = getParameterTypes(*args)
        val ctor = findConstructorBestMatch(clazz, *parameterTypes)
        return ctor.newInstance(*args)
    }

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
    @JvmStatic
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

    @JvmStatic
    fun getSurroundingThis(obj: Any): Any? {
        return getObjectField(obj, $$"this$0")
    }

    @JvmStatic
    fun setAdditionalInstanceField(instance: Any, key: String, value: Any?): Any? {
        val objectFields: HashMap<String, Any?>
        synchronized(additionalFields) {
            objectFields = additionalFields.getOrPut(instance) { HashMap() }
        }

        synchronized(objectFields) {
            return objectFields.put(key, value)
        }
    }

    @JvmStatic
    fun getAdditionalInstanceField(instance: Any, key: String): Any? {
        val objectFields: HashMap<String, Any?>
        synchronized(additionalFields) {
            objectFields = additionalFields[instance] ?: return null
        }

        synchronized(objectFields) {
            return objectFields[key]
        }
    }

    @JvmStatic
    fun removeAdditionalInstanceField(instance: Any, key: String): Any? {
        val objectFields: HashMap<String, Any?>
        synchronized(additionalFields) {
            objectFields = additionalFields[instance] ?: return null
        }

        synchronized(objectFields) {
            return objectFields.remove(key)
        }
    }

    @JvmStatic
    fun setAdditionalStaticField(obj: Any, key: String, value: Any?): Any? {
        return setAdditionalInstanceField(obj.javaClass, key, value)
    }

    @JvmStatic
    fun getAdditionalStaticField(obj: Any, key: String): Any? {
        return getAdditionalInstanceField(obj.javaClass, key)
    }

    @JvmStatic
    fun removeAdditionalStaticField(obj: Any, key: String): Any? {
        return removeAdditionalInstanceField(obj.javaClass, key)
    }

    @JvmStatic
    fun setAdditionalStaticField(clazz: Class<*>, key: String, value: Any?): Any? {
        return setAdditionalInstanceField(clazz, key, value)
    }

    @JvmStatic
    fun getAdditionalStaticField(clazz: Class<*>, key: String): Any? {
        return getAdditionalInstanceField(clazz, key)
    }

    @JvmStatic
    fun removeAdditionalStaticField(clazz: Class<*>, key: String): Any? {
        return removeAdditionalInstanceField(clazz, key)
    }

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

    private val applicationHooks = mutableListOf<IApplicationHook>()
    private var isApplicationHooked = false
    private val applicationHookLock = Any()

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
    fun registerApplicationHook(hook: IApplicationHook) {
        synchronized(applicationHookLock) {
            applicationHooks.add(hook)
            ensureApplicationHooked()
        }
    }

    /**
     * 注册 Application 生命周期回调
     *
     * @param before attach 之前的回调，可为 null
     * @param after attach 之后的回调，可为 null
     */
    @JvmStatic
    fun registerApplicationHook(
        before: ContextConsumer?,
        after: ContextConsumer?
    ) {
        registerApplicationHook(object : IApplicationHook {
            override fun onApplicationAttachBefore(context: Context) {
                before?.accept(context)
            }

            override fun onApplicationAttachAfter(context: Context) {
                after?.accept(context)
            }
        })
    }

    /**
     * 仅注册 Application attach 之后的回调
     *
     * @param callback 回调函数
     */
    @JvmStatic
    fun runOnApplicationAttach(callback: ContextConsumer) {
        registerApplicationHook(null, callback)
    }

    /**
     * 确保 Application.attach 已被 Hook
     */
    private fun ensureApplicationHooked() {
        if (isApplicationHooked) return

        synchronized(applicationHookLock) {
            if (isApplicationHooked) return

            try {
                findAndHookMethod(
                    Application::class.java,
                    "attach",
                    Context::class.java,
                    object : IMethodHook {
                        override fun before(param: BeforeHookParam) {
                            val context = param.args[0] as? Context ?: return
                            applicationHooks.forEach { hook ->
                                try {
                                    hook.onApplicationAttachBefore(context)
                                } catch (t: Throwable) {
                                    XposedLog.e(TAG, "Application attach before callback error", t)
                                }
                            }
                        }

                        override fun after(param: AfterHookParam) {
                            val context = param.args[0] as? Context ?: return
                            applicationHooks.forEach { hook ->
                                try {
                                    hook.onApplicationAttachAfter(context)
                                } catch (t: Throwable) {
                                    XposedLog.e(TAG, "Application attach after callback error", t)
                                }
                            }
                            XposedLog.d(TAG, "Application created! package: ${context.packageName}")
                        }
                    }
                )
                isApplicationHooked = true
            } catch (t: Throwable) {
                XposedLog.e(TAG, "Failed to hook Application.attach", t)
            }
        }
    }

    /**
     * 取消注册 Application 生命周期回调
     *
     * @param hook 要取消的回调实例
     */
    @JvmStatic
    fun unregisterApplicationHook(hook: IApplicationHook) {
        synchronized(applicationHookLock) {
            applicationHooks.remove(hook)
        }
    }

    /**
     * 清除所有 Application 生命周期回调
     */
    @JvmStatic
    fun clearApplicationHooks() {
        synchronized(applicationHookLock) {
            applicationHooks.clear()
        }
    }

    // ==================== Hook 方法 ====================

    /**
     * 格式化方法签名用于日志输出
     */
    private fun formatMethodSignature(method: Method): String {
        val params = method.parameterTypes.joinToString(", ") { it.simpleName }
        return "${method.declaringClass.simpleName}#${method.name}($params)"
    }

    /**
     * 格式化构造器签名用于日志输出
     */
    private fun formatConstructorSignature(constructor: Constructor<*>): String {
        val params = constructor.parameterTypes.joinToString(", ") { it.simpleName }
        return "${constructor.declaringClass.simpleName}<init>($params)"
    }

    /**
     * Hook 方法
     *
     * @param method 要 Hook 的方法
     * @param callback Hook 回调
     * @return MethodUnhooker 对象，可用于取消 Hook
     */
    @JvmStatic
    fun hookMethod(method: Method, callback: IMethodHook): MethodUnhooker<*> {
        val signature = formatMethodSignature(method)
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
            // XposedLog.i(TAG, "[$signature] hook success")
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
    @JvmStatic
    fun hookMethod(method: Method, callback: IReplaceHook): MethodUnhooker<*> {
        val signature = formatMethodSignature(method)
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
            // XposedLog.i(TAG, "[$signature] hook (replace) success")
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
    @JvmStatic
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
            // XposedLog.i(TAG, "[$signature] hook success")
            unhook
        } catch (t: Throwable) {
            XposedLog.e(TAG, "[$signature] hook failed", t)
            throw t
        }
    }

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
    ): MethodUnhooker<*> {
        val method = findMethodExactIfExists(clazz, methodName, *parameterTypes)
        return hookMethod(method, callback)
    }


    @JvmStatic
    fun findAndHookMethod(
        clazzName: String,
        methodName: String,
        vararg args: Any,
    ): MethodUnhooker<*> {
        val clazz = findClass(clazzName, safeClassLoader)
        return findAndHookMethod(clazz, methodName, *args)
    }

    @JvmStatic
    fun findAndHookMethod(
        clazzName: String,
        classLoader: ClassLoader,
        methodName: String,
        vararg args: Any
    ): MethodUnhooker<*> {
        val clazz = findClass(clazzName, classLoader)
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
    @JvmStatic
    fun findAndHookMethod(
        clazz: Class<*>,
        methodName: String,
        vararg args: Any
    ): MethodUnhooker<*> {
        require(args.isNotEmpty()) { "args must contain at least the callback" }
        val callback = args.last()
        require(callback is IMethodHook) { "Last argument must be IMethodHook" }

        val paramTypes = getParameterClasses(clazz, *args)
        val method = findMethodExactIfExists(clazz, methodName, *paramTypes)
        return hookMethod(method, callback)
    }

    @JvmStatic
    fun findAndHookMethodReplace(
        clazzName: String,
        methodName: String,
        vararg args: Any,
    ): MethodUnhooker<*> {
        val clazz = findClass(clazzName, classLoader)
        return findAndHookMethodReplace(clazz, methodName, *args)
    }

    @JvmStatic
    fun findAndHookMethodReplace(
        clazzName: String,
        classLoader: ClassLoader,
        methodName: String,
        vararg args: Any
    ): MethodUnhooker<*> {
        val clazz = findClass(clazzName, classLoader)
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
    @JvmStatic
    fun findAndHookMethodReplace(
        clazz: Class<*>,
        methodName: String,
        vararg args: Any
    ): MethodUnhooker<*> {
        require(args.isNotEmpty()) { "args must contain at least the callback" }
        val callback = args.last()
        require(callback is IReplaceHook) { "Last argument must be IReplaceHook" }

        val paramTypes = getParameterClasses(clazz, *args)
        val method = findMethodExactIfExists(clazz, methodName, *paramTypes)
        return hookMethod(method, callback)
    }

    /**
     * 查找并 Hook 构造器
     *
     * @param clazz 目标类
     * @param parameterTypes 参数类型
     * @param callback Hook 回调
     * @return MethodUnhooker 对象
     */
    @JvmStatic
    fun findAndHookConstructor(
        clazz: Class<*>,
        vararg parameterTypes: Class<*>,
        callback: IMethodHook
    ): MethodUnhooker<*> {
        val constructor = findConstructorExact(clazz, *parameterTypes)
        return hookConstructor(constructor, callback)
    }

    @JvmStatic
    fun findAndHookConstructor(
        clazzName: String,
        vararg args: Any
    ): MethodUnhooker<*> {
        val clazz = findClass(clazzName, classLoader)
        return findAndHookConstructor(clazz, *args)
    }

    @JvmStatic
    fun findAndHookConstructor(
        clazzName: String,
        classLoader: ClassLoader,
        vararg args: Any
    ): MethodUnhooker<*> {
        val clazz = findClass(clazzName, classLoader)
        return findAndHookConstructor(clazz, *args)
    }

    /**
     * 查找并 Hook 构造器
     *
     * @param clazz 目标类
     * @param args 参数类型数组，最后一个元素必须是 IMethodHook
     * @return MethodUnhooker 对象
     */
    @JvmStatic
    fun findAndHookConstructor(clazz: Class<*>, vararg args: Any): MethodUnhooker<*> {
        require(args.isNotEmpty()) { "args must contain at least the callback" }
        val callback = args.last()
        require(callback is IMethodHook) { "Last argument must be IMethodHook" }

        val paramTypes = getParameterClasses(clazz, *args)
        val constructor = findConstructorExact(clazz, *paramTypes)
        return hookConstructor(constructor, callback)
    }

    @JvmStatic
    fun hookAllMethods(
        clazzName: String,
        methodName: String,
        callback: IMethodHook
    ): List<MethodUnhooker<*>> {
        val clazz = findClass(clazzName, classLoader)
        return hookAllMethods(clazz, methodName, callback)
    }

    @JvmStatic
    fun hookAllMethods(
        clazzName: String,
        classLoader: ClassLoader,
        methodName: String,
        callback: IMethodHook
    ): List<MethodUnhooker<*>> {
        val clazz = findClass(clazzName, classLoader)
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
    @JvmStatic
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
    @JvmStatic
    fun hookAllConstructors(clazz: Class<*>, callback: IMethodHook): List<MethodUnhooker<*>> {
        val constructors = ConstructorFinder.fromClass(clazz).toList()

        if (constructors.isEmpty()) {
            XposedLog.w(TAG, "[${clazz.simpleName}<init>] no constructors found")
            return emptyList()
        }

        return constructors.mapNotNull { constructor ->
            try {
                hookConstructor(constructor, callback)
            } catch (t: Throwable) {
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
    @JvmStatic
    fun returnConstant(result: Any?): IMethodHook {
        return object : IMethodHook {
            override fun before(param: BeforeHookParam) {
                param.result = result
            }
        }
    }

    /**
     * 什么都不做的 Hook（阻止原方法执行，返回 null）
     */
    @JvmField
    val DO_NOTHING: IMethodHook = object : IMethodHook {
        override fun before(param: BeforeHookParam) {
            param.result = null
        }
    }

    // ==================== deoptimize 相关 ====================

    @JvmStatic
    fun deoptimize(method: Method): Boolean {
        return try {
            xposedModule.deoptimize(method)
            XposedLog.d(TAG, "deoptimize $method success")
            true
        } catch (t: Throwable) {
            XposedLog.e(TAG, "deoptimize $method failed, log: $t")
            return false
        }
    }

    @JvmStatic
    fun deoptimizeMethods(clazz: Class<*>, vararg names: String?) {
        val list = listOf(*names)
        Arrays.stream(clazz.declaredMethods)
            .filter { method: Method? ->
                list.contains(method!!.name)
            }
            .forEach { method: Method? ->
                this.deoptimize(method!!)
            }
    }

    @JvmStatic
    fun libHook(method: Method, hooker: Class<out Hooker>): MethodUnhooker<Method?> {
        return xposedModule.hook(method, hooker)
    }

    @JvmStatic
    fun invokeSuperMethod(methodName: String, thisObject: Any, vararg args: Any?) {
        val paramTypes = getParameterTypes(*args)
        val method = findMethodBestMatch(thisObject::class.java.superclass, methodName, *paramTypes)
        xposedModule.invokeSpecial(method, thisObject, *paramTypes)
    }
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
    val primitiveToBoxed = mapOf(
        java.lang.Integer.TYPE to Integer::class.java,
        java.lang.Long.TYPE to java.lang.Long::class.java,
        java.lang.Float.TYPE to java.lang.Float::class.java,
        java.lang.Double.TYPE to java.lang.Double::class.java,
        java.lang.Boolean.TYPE to java.lang.Boolean::class.java,
        java.lang.Byte.TYPE to java.lang.Byte::class.java,
        java.lang.Short.TYPE to java.lang.Short::class.java,
        java.lang.Character.TYPE to java.lang.Character::class.java
    )

    return (primitiveToBoxed[from] == to) || (primitiveToBoxed[to] == from)
}

/**
 * 获取参数类型字符串表示
 *
 * @param parameterTypes 参数类型数组
 * @return 格式化的参数字符串，如 "(String,int)"
 */
private fun getParametersString(parameterTypes: Array<out Class<*>?>): String {
    return parameterTypes.joinToString(",", "(", ")") { it?.simpleName ?: "null" }
}
