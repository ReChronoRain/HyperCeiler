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

package com.sevtinge.hyperceiler.hook.utils.reflect

import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap

object ReflectUtils {

    private data class FieldCacheKey(val clazz: Class<*>, val name: String)
    private data class MethodCacheKey(val clazz: Class<*>, val name: String, val parameterTypes: List<Class<*>?>, val exact: Boolean)
    private data class ConstructorCacheKey(val clazz: Class<*>, val parameterTypes: List<Class<*>?>, val exact: Boolean)

    private val fieldCache = ConcurrentHashMap<FieldCacheKey, Optional<Field>>()
    private val methodCache = ConcurrentHashMap<MethodCacheKey, Optional<Method>>()
    private val constructorCache = ConcurrentHashMap<ConstructorCacheKey, Optional<Constructor<*>>>()

    @JvmStatic
    fun findClass(name: String, classLoader: ClassLoader?): Class<*> {
        return Class.forName(name, false, classLoader)
    }

    @JvmStatic
    fun findClassIfExists(name: String, classLoader: ClassLoader?): Class<*>? {
        return try {
            findClass(name, classLoader)
        } catch (_: Throwable) {
            null
        }
    }

    @JvmStatic
    fun findField(clazz: Class<*>, fieldName: String): Field {
        val key = FieldCacheKey(clazz, fieldName)
        return fieldCache.computeIfAbsent(key) { k ->
            val field = findFieldInternal(k.clazz, k.name)
            if (field != null) Optional.of(field) else Optional.empty()
        }.orElseThrow { NoSuchFieldException(fieldName) }
    }

    @JvmStatic
    fun findFieldIfExists(clazz: Class<*>, fieldName: String): Field? {
        val key = FieldCacheKey(clazz, fieldName)
        return fieldCache.computeIfAbsent(key) { k ->
            val field = findFieldInternal(k.clazz, k.name)
            if (field != null) Optional.of(field) else Optional.empty()
        }.orElse(null)
    }

    @JvmStatic
    fun findFirstFieldByExactType(clazz: Class<*>, type: Class<*>): Field {
        var c: Class<*>? = clazz
        while (c != null) {
            for (field in c.declaredFields) {
                if (field.type == type) {
                    field.isAccessible = true
                    return field
                }
            }
            c = c.superclass
        }
        throw NoSuchFieldException("Field of type ${type.name} in class ${clazz.name}")
    }

    @JvmStatic
    fun getObjectField(instance: Any, fieldName: String): Any? {
        return findField(instance.javaClass, fieldName).get(instance)
    }

    @JvmStatic
    fun setObjectField(instance: Any, fieldName: String, value: Any?) {
        findField(instance.javaClass, fieldName).set(instance, value)
    }

    @JvmStatic
    fun getBooleanField(instance: Any, fieldName: String): Boolean {
        return findField(instance.javaClass, fieldName).getBoolean(instance)
    }

    @JvmStatic
    fun setBooleanField(instance: Any, fieldName: String, value: Boolean) {
        findField(instance.javaClass, fieldName).setBoolean(instance, value)
    }

    @JvmStatic
    fun getIntField(instance: Any, fieldName: String): Int {
        return findField(instance.javaClass, fieldName).getInt(instance)
    }

    @JvmStatic
    fun setIntField(instance: Any, fieldName: String, value: Int) {
        findField(instance.javaClass, fieldName).setInt(instance, value)
    }

    @JvmStatic
    fun getLongField(instance: Any, fieldName: String): Long {
        return findField(instance.javaClass, fieldName).getLong(instance)
    }

    @JvmStatic
    fun setLongField(instance: Any, fieldName: String, value: Long) {
        findField(instance.javaClass, fieldName).setLong(instance, value)
    }

    @JvmStatic
    fun getFloatField(instance: Any, fieldName: String): Float {
        return findField(instance.javaClass, fieldName).getFloat(instance)
    }

    @JvmStatic
    fun setFloatField(instance: Any, fieldName: String, value: Float) {
        findField(instance.javaClass, fieldName).setFloat(instance, value)
    }

    @JvmStatic
    fun getDoubleField(instance: Any, fieldName: String): Double {
        return findField(instance.javaClass, fieldName).getDouble(instance)
    }

    @JvmStatic
    fun setDoubleField(instance: Any, fieldName: String, value: Double) {
        findField(instance.javaClass, fieldName).setDouble(instance, value)
    }

    @JvmStatic
    fun getStaticObjectField(clazz: Class<*>, fieldName: String): Any? {
        return findField(clazz, fieldName).get(null)
    }

    @JvmStatic
    fun setStaticObjectField(clazz: Class<*>, fieldName: String, value: Any?) {
        findField(clazz, fieldName).set(null, value)
    }

    @JvmStatic
    fun getStaticBooleanField(clazz: Class<*>, fieldName: String): Boolean {
        return findField(clazz, fieldName).getBoolean(null)
    }

    @JvmStatic
    fun setStaticBooleanField(clazz: Class<*>, fieldName: String, value: Boolean) {
        findField(clazz, fieldName).setBoolean(null, value)
    }

    @JvmStatic
    fun getStaticIntField(clazz: Class<*>, fieldName: String): Int {
        return findField(clazz, fieldName).getInt(null)
    }

    @JvmStatic
    fun setStaticIntField(clazz: Class<*>, fieldName: String, value: Int) {
        findField(clazz, fieldName).setInt(null, value)
    }

    @JvmStatic
    fun getStaticLongField(clazz: Class<*>, fieldName: String): Long {
        return findField(clazz, fieldName).getLong(null)
    }

    @JvmStatic
    fun setStaticLongField(clazz: Class<*>, fieldName: String, value: Long) {
        findField(clazz, fieldName).setLong(null, value)
    }

    @JvmStatic
    fun callMethod(instance: Any, methodName: String, vararg args: Any?): Any? {
        val method = findBestMethod(instance.javaClass, methodName, args)
            ?: throw NoSuchMethodException("${instance.javaClass.name}#$methodName")
        return method.invoke(instance, *args)
    }

    @JvmStatic
    fun callStaticMethod(clazz: Class<*>, methodName: String, vararg args: Any?): Any? {
        val method = findBestMethod(clazz, methodName, args)
            ?: throw NoSuchMethodException("${clazz.name}#$methodName")
        return method.invoke(null, *args)
    }

    @JvmStatic
    fun findMethodBestMatch(clazz: Class<*>, methodName: String, vararg args: Any?): Method {
        return findBestMethod(clazz, methodName, args)
            ?: throw NoSuchMethodException("${clazz.name}#$methodName")
    }

    @JvmStatic
    fun findMethodExact(clazz: Class<*>, methodName: String, vararg parameterTypes: Class<*>): Method {
        val key = MethodCacheKey(clazz, methodName, parameterTypes.toList(), true)
        return methodCache.computeIfAbsent(key) { k ->
            try {
                val method = k.clazz.getDeclaredMethod(k.name, *k.parameterTypes.filterNotNull().toTypedArray())
                method.isAccessible = true
                Optional.of(method)
            } catch (_: NoSuchMethodException) {
                Optional.empty()
            }
        }.orElseThrow { NoSuchMethodException("${clazz.name}#$methodName") }
    }

    @JvmStatic
    fun findMethodExactIfExists(clazz: Class<*>, methodName: String, vararg parameterTypes: Class<*>): Method? {
        return try {
            findMethodExact(clazz, methodName, *parameterTypes)
        } catch (_: Throwable) {
            null
        }
    }

    @JvmStatic
    fun newInstance(clazz: Class<*>, vararg args: Any?): Any {
        val ctor = findBestConstructor(clazz, args) ?: throw NoSuchMethodException("${clazz.name}<init>")
        return ctor.newInstance(*args)
    }

    @JvmStatic
    fun findConstructorExact(clazz: Class<*>, vararg parameterTypes: Class<*>): Constructor<*> {
        val key = ConstructorCacheKey(clazz, parameterTypes.toList(), true)
        return constructorCache.computeIfAbsent(key) { k ->
            try {
                val ctor = k.clazz.getDeclaredConstructor(*k.parameterTypes.filterNotNull().toTypedArray())
                ctor.isAccessible = true
                Optional.of(ctor)
            } catch (_: NoSuchMethodException) {
                Optional.empty()
            }
        }.orElseThrow { NoSuchMethodException("${clazz.name}<init>") }
    }

    @JvmStatic
    fun findConstructorBestMatch(clazz: Class<*>, vararg args: Any?): Constructor<*> {
        return findBestConstructor(clazz, args)
            ?: throw NoSuchMethodException("${clazz.name}<init>")
    }

    @JvmStatic
    fun getSurroundingThis(obj: Any): Any? {
        return getObjectField(obj, "this\$0")
    }

    private fun findFieldInternal(start: Class<*>, fieldName: String): Field? {
        var c: Class<*>? = start
        while (c != null) {
            try {
                val f = c.getDeclaredField(fieldName)
                f.isAccessible = true
                return f
            } catch (_: Throwable) {
            }
            c = c.superclass
        }
        return null
    }

    private fun findBestMethod(start: Class<*>, methodName: String, args: Array<out Any?>): Method? {
        val argTypes = args.map { it?.javaClass }
        val key = MethodCacheKey(start, methodName, argTypes, false)

        return methodCache.computeIfAbsent(key) { k ->
            var c: Class<*>? = k.clazz
            val argTypesArray = k.parameterTypes.toTypedArray()
            while (c != null) {
                val candidates = c.declaredMethods.filter { it.name == k.name && it.parameterTypes.size == k.parameterTypes.size }
                val best = candidates.firstOrNull { isApplicable(it.parameterTypes, argTypesArray) }
                if (best != null) {
                    best.isAccessible = true
                    return@computeIfAbsent Optional.of(best)
                }
                c = c.superclass
            }
            Optional.empty()
        }.orElse(null)
    }

    private fun findBestConstructor(clazz: Class<*>, args: Array<out Any?>): Constructor<*>? {
        val argTypes = args.map { it?.javaClass }
        val key = ConstructorCacheKey(clazz, argTypes, false)

        return constructorCache.computeIfAbsent(key) { k ->
            val argTypesArray = k.parameterTypes.toTypedArray()
            val candidates = k.clazz.declaredConstructors.filter { it.parameterTypes.size == k.parameterTypes.size }
            val best = candidates.firstOrNull { isApplicable(it.parameterTypes, argTypesArray) }
            if (best != null) {
                best.isAccessible = true
                Optional.of(best)
            } else {
                Optional.empty()
            }
        }.orElse(null)
    }

    private fun isApplicable(params: Array<Class<*>>, args: Array<Class<*>?>): Boolean {
        for (i in params.indices) {
            val p = params[i]
            val a = args[i] ?: continue
            if (p.isPrimitive) {
                if (!isBoxedTypeOf(a, p)) return false
            } else {
                if (!p.isAssignableFrom(a)) return false
            }
        }
        return true
    }

    private fun isBoxedTypeOf(arg: Class<*>, primitive: Class<*>): Boolean {
        return when (primitive) {
            java.lang.Boolean.TYPE -> arg == java.lang.Boolean::class.java
            java.lang.Byte.TYPE -> arg == Byte::class.java
            java.lang.Short.TYPE -> arg == Short::class.java
            Character.TYPE -> arg == Character::class.java
            Integer.TYPE -> arg == Integer::class.java
            java.lang.Long.TYPE -> arg == java.lang.Long::class.java
            java.lang.Float.TYPE -> arg == java.lang.Float::class.java
            java.lang.Double.TYPE -> arg == java.lang.Double::class.java
            else -> false
        }
    }
}

fun Any.getObjectField(fieldName: String): Any? = ReflectUtils.getObjectField(this, fieldName)
fun <T> Any.getObjectFieldAs(fieldName: String): T = getObjectField(fieldName) as T
fun Any.setObjectField(fieldName: String, value: Any?) = ReflectUtils.setObjectField(this, fieldName, value)

fun Any.getBooleanField(fieldName: String): Boolean = ReflectUtils.getBooleanField(this, fieldName)
fun Any.setBooleanField(fieldName: String, value: Boolean) = ReflectUtils.setBooleanField(this, fieldName, value)
fun Any.getIntField(fieldName: String): Int = ReflectUtils.getIntField(this, fieldName)
fun Any.setIntField(fieldName: String, value: Int) = ReflectUtils.setIntField(this, fieldName, value)
fun Any.getLongField(fieldName: String): Long = ReflectUtils.getLongField(this, fieldName)
fun Any.setLongField(fieldName: String, value: Long) = ReflectUtils.setLongField(this, fieldName, value)
fun Any.getFloatField(fieldName: String): Float = ReflectUtils.getFloatField(this, fieldName)
fun Any.setFloatField(fieldName: String, value: Float) = ReflectUtils.setFloatField(this, fieldName, value)
fun Any.getDoubleField(fieldName: String): Double = ReflectUtils.getDoubleField(this, fieldName)
fun Any.setDoubleField(fieldName: String, value: Double) = ReflectUtils.setDoubleField(this, fieldName, value)

fun Any.callMethod(methodName: String, vararg args: Any?): Any? = ReflectUtils.callMethod(this, methodName, *args)
fun <T> Any.callMethodAs(methodName: String, vararg args: Any?): T = callMethod(methodName, *args) as T

fun Class<*>.getStaticObjectField(fieldName: String): Any? = ReflectUtils.getStaticObjectField(this, fieldName)
fun <T> Class<*>.getStaticObjectFieldAs(fieldName: String): T = getStaticObjectField(fieldName) as T
fun Class<*>.setStaticObjectField(fieldName: String, value: Any?) = ReflectUtils.setStaticObjectField(this, fieldName, value)

fun Class<*>.getStaticBooleanField(fieldName: String): Boolean = ReflectUtils.getStaticBooleanField(this, fieldName)
fun Class<*>.setStaticBooleanField(fieldName: String, value: Boolean) = ReflectUtils.setStaticBooleanField(this, fieldName, value)
fun Class<*>.getStaticIntField(fieldName: String): Int = ReflectUtils.getStaticIntField(this, fieldName)
fun Class<*>.setStaticIntField(fieldName: String, value: Int) = ReflectUtils.setStaticIntField(this, fieldName, value)
fun Class<*>.getStaticLongField(fieldName: String): Long = ReflectUtils.getStaticLongField(this, fieldName)
fun Class<*>.setStaticLongField(fieldName: String, value: Long) = ReflectUtils.setStaticLongField(this, fieldName, value)

fun Class<*>.callStaticMethod(methodName: String, vararg args: Any?): Any? = ReflectUtils.callStaticMethod(this, methodName, *args)
fun <T> Class<*>.callStaticMethodAs(methodName: String, vararg args: Any?): T = callStaticMethod(methodName, *args) as T

fun <T> Class<*>.newInstance(vararg args: Any?): T = ReflectUtils.newInstance(this, *args) as T

fun String.toClass(classLoader: ClassLoader?): Class<*> = ReflectUtils.findClass(this, classLoader)
fun String.toClassOrNull(classLoader: ClassLoader?): Class<*>? = ReflectUtils.findClassIfExists(this, classLoader)
