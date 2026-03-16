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

import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog
import io.github.kyuubiran.ezxhelper.core.finder.FieldFinder
import java.lang.reflect.Field
import java.util.WeakHashMap

/**
 * 字段操作工具
 * 封装字段查找、获取/设置值、附加字段等功能
 */
internal object EzxFieldHelper {

    private const val TAG = "EzxFieldHelper"

    /** 字段缓存（String 键不会被 GC，使用 HashMap 替代 WeakHashMap） */
    private val fieldCache = HashMap<String, Field?>()

    /** 附加字段存储（WeakHashMap 使对象可被 GC 回收） */
    private val additionalFields = WeakHashMap<Any, HashMap<String, Any?>>()

    // ==================== 字段查找 ====================

    /**
     * 在类层次结构中查找字段（包括父类）
     *
     * @param clazz 目标类
     * @param fieldName 字段名
     * @return Field 对象
     * @throws NoSuchFieldError 如果字段不存在
     */
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

    fun findFieldIfExists(clazz: Class<*>, fieldName: String): Field? {
        return try {
            findField(clazz, fieldName)
        } catch (_: NoSuchFieldError) {
            null
        }
    }

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

    fun findFirstFieldByExactTypeIfExists(clazz: Class<*>, type: Class<*>): Field? {
        return FieldFinder.fromClass(clazz)
            .findSuper()
            .firstOrNullByType(type)?.apply { isAccessible = true }
    }

    // ==================== 实例字段操作 ====================

    /**
     * 获取对象字段值
     *
     * @param instance 对象实例
     * @param fieldName 字段名
     * @return 字段值
     */
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

    fun getBooleanField(instance: Any, fieldName: String): Boolean =
        getObjectField(instance, fieldName) as? Boolean ?: false

    fun setBooleanField(instance: Any, fieldName: String, value: Boolean) =
        setObjectField(instance, fieldName, value)

    fun getIntField(instance: Any, fieldName: String): Int =
        getObjectField(instance, fieldName) as? Int ?: 0

    fun setIntField(instance: Any, fieldName: String, value: Int) =
        setObjectField(instance, fieldName, value)

    fun getLongField(instance: Any, fieldName: String): Long =
        getObjectField(instance, fieldName) as? Long ?: 0L

    fun setLongField(instance: Any, fieldName: String, value: Long) =
        setObjectField(instance, fieldName, value)

    fun getFloatField(instance: Any, fieldName: String): Float =
        getObjectField(instance, fieldName) as? Float ?: 0f

    fun setFloatField(instance: Any, fieldName: String, value: Float) =
        setObjectField(instance, fieldName, value)

    // ==================== 静态字段操作 ====================

    fun getStaticObjectField(clazz: Class<*>, fieldName: String): Any? {
        return try {
            val field = findField(clazz, fieldName)
            field.get(null)
        } catch (e: IllegalAccessException) {
            XposedLog.e(TAG, "Failed to get static field: $fieldName", e)
            throw IllegalAccessError(e.message)
        }
    }

    fun setStaticObjectField(clazz: Class<*>, fieldName: String, value: Any?) {
        try {
            val field = findField(clazz, fieldName)
            field.set(null, value)
        } catch (e: IllegalAccessException) {
            XposedLog.e(TAG, "Failed to set static field: $fieldName", e)
            throw IllegalAccessError(e.message)
        }
    }

    fun getStaticBooleanField(clazz: Class<*>, fieldName: String): Boolean =
        getStaticObjectField(clazz, fieldName) as? Boolean ?: false

    fun setStaticBooleanField(clazz: Class<*>, fieldName: String, value: Boolean) =
        setStaticObjectField(clazz, fieldName, value)

    fun getStaticIntField(clazz: Class<*>, fieldName: String): Int =
        getStaticObjectField(clazz, fieldName) as? Int ?: 0

    fun setStaticIntField(clazz: Class<*>, fieldName: String, value: Int) =
        setStaticObjectField(clazz, fieldName, value)

    fun getStaticLongField(clazz: Class<*>, fieldName: String): Long =
        getStaticObjectField(clazz, fieldName) as? Long ?: 0L

    fun setStaticLongField(clazz: Class<*>, fieldName: String, value: Long) =
        setStaticObjectField(clazz, fieldName, value)

    fun getStaticFloatField(clazz: Class<*>, fieldName: String): Float =
        getStaticObjectField(clazz, fieldName) as? Float ?: 0f

    fun setStaticFloatField(clazz: Class<*>, fieldName: String, value: Float) =
        setStaticObjectField(clazz, fieldName, value)

    // ==================== 附加字段操作 ====================

    fun getSurroundingThis(obj: Any): Any? {
        return getObjectField(obj, "this\$0")
    }

    fun setAdditionalInstanceField(instance: Any, key: String, value: Any?): Any? {
        val objectFields: HashMap<String, Any?>
        synchronized(additionalFields) {
            objectFields = additionalFields.getOrPut(instance) { HashMap() }
        }
        synchronized(objectFields) {
            return objectFields.put(key, value)
        }
    }

    fun getAdditionalInstanceField(instance: Any, key: String): Any? {
        val objectFields: HashMap<String, Any?>
        synchronized(additionalFields) {
            objectFields = additionalFields[instance] ?: return null
        }
        synchronized(objectFields) {
            return objectFields[key]
        }
    }

    fun removeAdditionalInstanceField(instance: Any, key: String): Any? {
        val objectFields: HashMap<String, Any?>
        synchronized(additionalFields) {
            objectFields = additionalFields[instance] ?: return null
        }
        synchronized(objectFields) {
            return objectFields.remove(key)
        }
    }

    fun setAdditionalStaticField(obj: Any, key: String, value: Any?): Any? =
        setAdditionalInstanceField(obj.javaClass, key, value)

    fun getAdditionalStaticField(obj: Any, key: String): Any? =
        getAdditionalInstanceField(obj.javaClass, key)

    fun removeAdditionalStaticField(obj: Any, key: String): Any? =
        removeAdditionalInstanceField(obj.javaClass, key)

    fun setAdditionalStaticField(clazz: Class<*>, key: String, value: Any?): Any? =
        setAdditionalInstanceField(clazz, key, value)

    fun getAdditionalStaticField(clazz: Class<*>, key: String): Any? =
        getAdditionalInstanceField(clazz, key)

    fun removeAdditionalStaticField(clazz: Class<*>, key: String): Any? =
        removeAdditionalInstanceField(clazz, key)
}
