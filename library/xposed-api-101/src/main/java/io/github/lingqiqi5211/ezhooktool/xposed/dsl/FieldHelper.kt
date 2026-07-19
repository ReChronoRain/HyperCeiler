@file:Suppress("UNCHECKED_CAST")
@file:JvmName("FieldHelper")

package io.github.lingqiqi5211.ezhooktool.xposed.dsl

import io.github.lingqiqi5211.ezhooktool.core.field
import io.github.lingqiqi5211.ezhooktool.core.fieldOrNull
import io.github.lingqiqi5211.ezhooktool.core.findAllFields
import io.github.lingqiqi5211.ezhooktool.core.findField
import io.github.lingqiqi5211.ezhooktool.core.findFieldOrNull
import io.github.lingqiqi5211.ezhooktool.core.getField
import io.github.lingqiqi5211.ezhooktool.core.getFieldAs
import io.github.lingqiqi5211.ezhooktool.core.getFieldByType
import io.github.lingqiqi5211.ezhooktool.core.getFieldByTypeOrNull
import io.github.lingqiqi5211.ezhooktool.core.getFieldOrNull
import io.github.lingqiqi5211.ezhooktool.core.getFieldOrNullAs
import io.github.lingqiqi5211.ezhooktool.core.getStaticField
import io.github.lingqiqi5211.ezhooktool.core.getStaticFieldAs
import io.github.lingqiqi5211.ezhooktool.core.getStaticFieldOrNull
import io.github.lingqiqi5211.ezhooktool.core.getStaticFieldOrNullAs
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.core.putField
import io.github.lingqiqi5211.ezhooktool.core.putStaticField
import io.github.lingqiqi5211.ezhooktool.core.query.FieldQuery
import io.github.lingqiqi5211.ezhooktool.xposed.internal.AdditionalFields
import io.github.lingqiqi5211.ezhooktool.xposed.internal.HookClassLoader
import java.lang.reflect.Field

/**
 * 递归读取实例字段。
 *
 * 优先读取当前类，找不到时继续查父类；全部找不到时返回 `null`。
 *
 * @param target 目标实例
 * @param fieldName 字段名
 * @param clazz 当前查找类，通常不需要手动传入
 */
fun getValueByField(target: Any, fieldName: String, clazz: Class<*>? = null): Any? {
    val targetClass = clazz ?: target.javaClass
    return try {
        val field = targetClass.getDeclaredField(fieldName)
        field.isAccessible = true
        field.get(target)
    } catch (_: Throwable) {
        val superClass = targetClass.superclass ?: return null
        getValueByField(target, fieldName, superClass)
    }
}

/**
 * 把 `findSuper` 语义应用到 [FieldQuery]。
 *
 * `null` 沿用 query 默认行为，`false` 调用 [FieldQuery.findOnlyClass]，`true` 调用 [FieldQuery.findAndSuper]。
 */
private fun FieldQuery.applyFindSuper(findSuper: Boolean?) {
    when (findSuper) {
        true -> findAndSuper()
        false -> findOnlyClass()
        null -> Unit
    }
}

/**
 * 按类名查找字段。
 *
 * 默认使用当前 hook 运行时的 `ClassLoader`，适合在 Xposed 模块里直接用目标类名查找。
 *
 * @param classLoader 用于加载目标类的 `ClassLoader`
 * @param findSuper `null` 为智能查找，`false` 只查当前类，`true` 查当前类和全部父类
 * @param query 字段查询条件
 */
fun String.findField(
    classLoader: ClassLoader = HookClassLoader.currentOrDefault(),
    findSuper: Boolean? = null,
    query: FieldQuery.() -> Unit,
): Field = findField(this, classLoader) {
    applyFindSuper(findSuper)
    query()
}

/**
 * 按类名查找字段，找不到时返回 `null`。
 *
 * @param classLoader 用于加载目标类的 `ClassLoader`
 * @param findSuper `null` 为智能查找，`false` 只查当前类，`true` 查当前类和全部父类
 * @param query 字段查询条件
 */
fun String.findFieldOrNull(
    classLoader: ClassLoader = HookClassLoader.currentOrDefault(),
    findSuper: Boolean? = null,
    query: FieldQuery.() -> Unit,
): Field? = findFieldOrNull(this, classLoader) {
    applyFindSuper(findSuper)
    query()
}

/**
 * 按类名查找全部匹配字段。
 *
 * 不传查询条件时可使用另一个重载列出当前查找范围内的全部字段。
 *
 * @param classLoader 用于加载目标类的 `ClassLoader`
 * @param findSuper `null` 为智能查找，`false` 只查当前类，`true` 查当前类和全部父类
 * @param query 字段查询条件
 */
fun String.findAllFields(
    classLoader: ClassLoader = HookClassLoader.currentOrDefault(),
    findSuper: Boolean? = null,
    query: FieldQuery.() -> Unit,
): List<Field> = findAllFields(this, classLoader) {
    applyFindSuper(findSuper)
    query()
}

/**
 * 按类名列出字段。
 *
 * @param classLoader 用于加载目标类的 `ClassLoader`
 * @param findSuper `null` 为智能查找，`false` 只查当前类，`true` 查当前类和全部父类
 */
fun String.findAllFields(
    classLoader: ClassLoader = HookClassLoader.currentOrDefault(),
    findSuper: Boolean? = null,
): List<Field> = findAllFields(this, classLoader) {
    applyFindSuper(findSuper)
}

/**
 * 按字段类型查找字段。
 *
 * @param type 字段类型
 * @param findSuper `null` 为智能查找，`false` 只查当前类，`true` 查当前类和全部父类
 */
fun Class<*>.findFieldByType(type: Class<*>, findSuper: Boolean? = null): Field =
    findField {
        applyFindSuper(findSuper)
        type(type)
    }

/**
 * 按字段类型查找字段，找不到时返回 `null`。
 *
 * @param type 字段类型
 * @param findSuper `null` 为智能查找，`false` 只查当前类，`true` 查当前类和全部父类
 */
fun Class<*>.findFieldByTypeOrNull(type: Class<*>, findSuper: Boolean? = null): Field? =
    findFieldOrNull {
        applyFindSuper(findSuper)
        type(type)
    }

/**
 * 按名称查找实例字段。
 *
 * @param name 字段名
 * @param type 可选字段类型，用于进一步确认匹配结果
 */
fun Class<*>.findField(name: String, type: Class<*>? = null): Field =
    field(name, fieldType = type)

/**
 * 按名称查找实例字段，找不到时返回 `null`。
 *
 * @param name 字段名
 * @param type 可选字段类型，用于进一步确认匹配结果
 */
fun Class<*>.findFieldOrNull(name: String, type: Class<*>? = null): Field? =
    fieldOrNull(name, fieldType = type)

/**
 * 按类名和字段名查找实例字段。
 *
 * @param fieldName 字段名
 * @param classLoader 用于加载目标类的 `ClassLoader`
 * @param type 可选字段类型，用于进一步确认匹配结果
 */
fun String.findField(
    fieldName: String,
    classLoader: ClassLoader = HookClassLoader.currentOrDefault(),
    type: Class<*>? = null,
): Field = loadClass(this, classLoader).findField(fieldName, type)

/**
 * 按类名和字段名查找实例字段，找不到时返回 `null`。
 *
 * @param fieldName 字段名
 * @param classLoader 用于加载目标类的 `ClassLoader`
 * @param type 可选字段类型，用于进一步确认匹配结果
 */
fun String.findFieldOrNull(
    fieldName: String,
    classLoader: ClassLoader = HookClassLoader.currentOrDefault(),
    type: Class<*>? = null,
): Field? = loadClass(this, classLoader).findFieldOrNull(fieldName, type)

/**
 * 按名称查找静态字段。
 *
 * @param name 字段名
 * @param type 可选字段类型，用于进一步确认匹配结果
 */
fun Class<*>.findStaticField(name: String, type: Class<*>? = null): Field =
    staticFieldRef(name, type)

/**
 * 按名称查找静态字段，找不到时返回 `null`。
 *
 * @param name 字段名
 * @param type 可选字段类型，用于进一步确认匹配结果
 */
fun Class<*>.findStaticFieldOrNull(name: String, type: Class<*>? = null): Field? =
    staticFieldRefOrNull(name, type)

/**
 * 按类名和字段名查找静态字段。
 *
 * @param fieldName 字段名
 * @param classLoader 用于加载目标类的 `ClassLoader`
 * @param type 可选字段类型，用于进一步确认匹配结果
 */
fun String.findStaticField(
    fieldName: String,
    classLoader: ClassLoader = HookClassLoader.currentOrDefault(),
    type: Class<*>? = null,
): Field = loadClass(this, classLoader).findStaticField(fieldName, type)

/**
 * 按类名和字段名查找静态字段，找不到时返回 `null`。
 *
 * @param fieldName 字段名
 * @param classLoader 用于加载目标类的 `ClassLoader`
 * @param type 可选字段类型，用于进一步确认匹配结果
 */
fun String.findStaticFieldOrNull(
    fieldName: String,
    classLoader: ClassLoader = HookClassLoader.currentOrDefault(),
    type: Class<*>? = null,
): Field? = loadClass(this, classLoader).findStaticFieldOrNull(fieldName, type)

/**
 * 按精确字段类型查找字段。
 *
 * @param type 字段类型
 * @param findSuper `null` 为智能查找，`false` 只查当前类，`true` 查当前类和全部父类
 */
fun Class<*>.findFieldByExactType(type: Class<*>, findSuper: Boolean? = null): Field =
    findFieldByType(type, findSuper)

/**
 * 按精确字段类型查找第一个字段。
 *
 * @param type 字段类型
 * @param findSuper `null` 为智能查找，`false` 只查当前类，`true` 查当前类和全部父类
 */
fun Class<*>.findFirstFieldByExactType(type: Class<*>, findSuper: Boolean? = null): Field =
    findFieldByType(type, findSuper)

/**
 * 按精确字段类型查找第一个字段，找不到时返回 `null`。
 *
 * @param type 字段类型
 * @param findSuper `null` 为智能查找，`false` 只查当前类，`true` 查当前类和全部父类
 */
fun Class<*>.findFirstFieldByExactTypeOrNull(type: Class<*>, findSuper: Boolean? = null): Field? =
    findFieldByTypeOrNull(type, findSuper)

private fun Any.objectField(fieldName: String): Field =
    findField(javaClass) { name(fieldName) }

private fun Any.objectFieldOrNull(fieldName: String): Field? =
    findFieldOrNull(javaClass) { name(fieldName) }

/**
 * 读取对象字段。按类层级读取最近的同名字段，以兼容旧版静态字段访问。
 *
 * @param fieldName 字段名
 */
fun Any.getObjectField(fieldName: String): Any? =
    objectField(fieldName).get(this)

/**
 * 读取对象字段并转为指定类型。
 *
 * @param fieldName 字段名
 */
fun <T> Any.getObjectFieldAs(fieldName: String): T =
    getObjectField(fieldName) as T

/**
 * 写入对象字段。按类层级写入最近的同名字段，以兼容旧版静态字段访问。
 *
 * @param fieldName 字段名
 * @param value 新值
 */
fun Any.setObjectField(fieldName: String, value: Any?) =
    objectField(fieldName).set(this, value)

/**
 * 读取对象字段，找不到时返回 `null`；实例字段值为 `null` 时不回退。
 *
 * @param fieldName 字段名
 */
fun Any.getObjectFieldOrNull(fieldName: String): Any? =
    objectFieldOrNull(fieldName)?.get(this)

/**
 * 读取对象字段并转为指定类型，失败时返回 `null`。
 *
 * @param fieldName 字段名
 */
fun <T> Any.getObjectFieldOrNullAs(fieldName: String): T? =
    getObjectFieldOrNull(fieldName) as T?

/** 读取实例 `Boolean` 字段。 */
fun Any.getBooleanField(fieldName: String): Boolean = getObjectField(fieldName) as Boolean

/** 写入实例 `Boolean` 字段。 */
fun Any.setBooleanField(fieldName: String, value: Boolean) = setObjectField(fieldName, value)

/** 读取实例 `Int` 字段。 */
fun Any.getIntField(fieldName: String): Int = getObjectField(fieldName) as Int

/** 写入实例 `Int` 字段。 */
fun Any.setIntField(fieldName: String, value: Int) = setObjectField(fieldName, value)

/** 读取实例 `Long` 字段。 */
fun Any.getLongField(fieldName: String): Long = getObjectField(fieldName) as Long

/** 写入实例 `Long` 字段。 */
fun Any.setLongField(fieldName: String, value: Long) = setObjectField(fieldName, value)

/** 读取实例 `Float` 字段。 */
fun Any.getFloatField(fieldName: String): Float = getObjectField(fieldName) as Float

/** 写入实例 `Float` 字段。 */
fun Any.setFloatField(fieldName: String, value: Float) = setObjectField(fieldName, value)

/** 读取实例 `Double` 字段。 */
fun Any.getDoubleField(fieldName: String): Double = getObjectField(fieldName) as Double

/** 写入实例 `Double` 字段。 */
fun Any.setDoubleField(fieldName: String, value: Double) = setObjectField(fieldName, value)

/** 读取实例 `Byte` 字段。 */
fun Any.getByteField(fieldName: String): Byte = getObjectField(fieldName) as Byte

/** 写入实例 `Byte` 字段。 */
fun Any.setByteField(fieldName: String, value: Byte) = setObjectField(fieldName, value)

/** 读取实例 `Short` 字段。 */
fun Any.getShortField(fieldName: String): Short = getObjectField(fieldName) as Short

/** 写入实例 `Short` 字段。 */
fun Any.setShortField(fieldName: String, value: Short) = setObjectField(fieldName, value)

/** 读取实例 `Char` 字段。 */
fun Any.getCharField(fieldName: String): Char = getObjectField(fieldName) as Char

/** 写入实例 `Char` 字段。 */
fun Any.setCharField(fieldName: String, value: Char) = setObjectField(fieldName, value)

/** 读取实例 `Boolean` 字段，失败时返回 `null`。 */
fun Any.getBooleanFieldOrNull(fieldName: String): Boolean? = runCatching { getBooleanField(fieldName) }.getOrNull()

/** 读取实例 `Int` 字段，失败时返回 `null`。 */
fun Any.getIntFieldOrNull(fieldName: String): Int? = runCatching { getIntField(fieldName) }.getOrNull()

/** 读取实例 `Long` 字段，失败时返回 `null`。 */
fun Any.getLongFieldOrNull(fieldName: String): Long? = runCatching { getLongField(fieldName) }.getOrNull()

/** 读取实例 `Float` 字段，失败时返回 `null`。 */
fun Any.getFloatFieldOrNull(fieldName: String): Float? = runCatching { getFloatField(fieldName) }.getOrNull()

/** 读取实例 `Double` 字段，失败时返回 `null`。 */
fun Any.getDoubleFieldOrNull(fieldName: String): Double? = runCatching { getDoubleField(fieldName) }.getOrNull()

/** 读取实例 `Byte` 字段，失败时返回 `null`。 */
fun Any.getByteFieldOrNull(fieldName: String): Byte? = runCatching { getByteField(fieldName) }.getOrNull()

/** 读取实例 `Short` 字段，失败时返回 `null`。 */
fun Any.getShortFieldOrNull(fieldName: String): Short? = runCatching { getShortField(fieldName) }.getOrNull()

/** 读取实例 `Char` 字段，失败时返回 `null`。 */
fun Any.getCharFieldOrNull(fieldName: String): Char? = runCatching { getCharField(fieldName) }.getOrNull()

/**
 * 读取第一个指定精确类型的实例字段值。
 *
 * @param type 字段类型
 */
fun Any.getFirstFieldByExactType(type: Class<*>): Any? =
    javaClass.findFirstFieldByExactType(type).get(this)

/**
 * 读取第一个指定精确类型的实例字段值并转为指定类型。
 *
 * @param type 字段类型
 */
fun <T> Any.getFirstFieldByExactTypeAs(type: Class<*>): T? =
    javaClass.findFirstFieldByExactType(type).get(this) as? T

/**
 * 获取实例字段引用。
 *
 * @param fieldName 字段名
 * @param type 可选字段类型，用于进一步确认匹配结果
 */
fun Any.fieldRef(fieldName: String, type: Class<*>? = null): Field =
    field(fieldName, fieldType = type)

/**
 * 获取实例字段引用，找不到时返回 `null`。
 *
 * @param fieldName 字段名
 * @param type 可选字段类型，用于进一步确认匹配结果
 */
fun Any.fieldRefOrNull(fieldName: String, type: Class<*>? = null): Field? =
    fieldOrNull(fieldName, fieldType = type)

/**
 * 读取实例字段值。
 *
 * @param fieldName 字段名
 * @param type 可选字段类型，用于进一步确认匹配结果
 */
fun Any.fieldValue(fieldName: String, type: Class<*>? = null): Any? =
    getField(fieldName, type)

/**
 * 读取实例字段值并转为指定类型。
 *
 * @param fieldName 字段名
 * @param type 可选字段类型，用于进一步确认匹配结果
 */
fun <T> Any.fieldValueAs(fieldName: String, type: Class<*>? = null): T? =
    getFieldAs(fieldName, type)

/**
 * 读取实例字段值，失败时返回 `null`。
 *
 * @param fieldName 字段名
 * @param type 可选字段类型，用于进一步确认匹配结果
 */
fun Any.fieldValueOrNull(fieldName: String, type: Class<*>? = null): Any? =
    getFieldOrNull(fieldName, type)

/**
 * 读取实例字段值并转为指定类型，失败时返回 `null`。
 *
 * @param fieldName 字段名
 * @param type 可选字段类型，用于进一步确认匹配结果
 */
fun <T> Any.fieldValueOrNullAs(fieldName: String, type: Class<*>? = null): T? =
    getFieldOrNullAs(fieldName, type)

/**
 * 按字段类型读取字段值。
 *
 * @param type 字段类型
 * @param isStatic 是否读取静态字段
 */
fun Any.fieldValueByType(type: Class<*>, isStatic: Boolean = false): Any? =
    getFieldByType<Any?>(type, isStatic)

/**
 * 按字段类型读取字段值并转为指定类型。
 *
 * @param type 字段类型
 * @param isStatic 是否读取静态字段
 */
fun <T> Any.fieldValueByTypeAs(type: Class<*>, isStatic: Boolean = false): T? =
    getFieldByType(type, isStatic)

/**
 * 按字段类型读取字段值，失败时返回 `null`。
 *
 * @param type 字段类型
 * @param isStatic 是否读取静态字段
 */
fun Any.fieldValueByTypeOrNull(type: Class<*>, isStatic: Boolean = false): Any? =
    getFieldByTypeOrNull<Any?>(type, isStatic)

/**
 * 按字段类型读取字段值并转为指定类型，失败时返回 `null`。
 *
 * @param type 字段类型
 * @param isStatic 是否读取静态字段
 */
fun <T> Any.fieldValueByTypeOrNullAs(type: Class<*>, isStatic: Boolean = false): T? =
    getFieldByTypeOrNull(type, isStatic)

/**
 * 写入实例字段值。
 *
 * @param fieldName 字段名
 * @param value 新值
 * @param type 可选字段类型，用于进一步确认匹配结果
 */
fun Any.setFieldValue(fieldName: String, value: Any?, type: Class<*>? = null) =
    putField(fieldName, value, type)

/**
 * 通过字段引用写入实例字段值。
 *
 * @param field 字段引用
 * @param value 新值
 */
fun Any.setFieldValue(field: Field, value: Any?) =
    putField(field, value)

/**
 * 读取静态字段。
 *
 * @param fieldName 字段名
 */
fun Class<*>.getStaticObjectField(fieldName: String): Any? =
    getStaticField(fieldName)

/**
 * 读取静态字段并转为指定类型。
 *
 * @param fieldName 字段名
 */
fun <T> Class<*>.getStaticObjectFieldAs(fieldName: String): T =
    getStaticObjectField(fieldName) as T

/**
 * 写入静态字段。
 *
 * @param fieldName 字段名
 * @param value 新值
 */
fun Class<*>.setStaticObjectField(fieldName: String, value: Any?) =
    putStaticField(fieldName, value)

/**
 * 读取静态字段，失败时返回 `null`。
 *
 * @param fieldName 字段名
 */
fun Class<*>.getStaticObjectFieldOrNull(fieldName: String): Any? =
    getStaticFieldOrNull(fieldName)

/**
 * 读取静态字段并转为指定类型，失败时返回 `null`。
 *
 * @param fieldName 字段名
 */
fun <T> Class<*>.getStaticObjectFieldAsOrNull(fieldName: String): T? =
    getStaticFieldOrNullAs(fieldName)

/** 读取静态 `Boolean` 字段。 */
fun Class<*>.getStaticBooleanField(fieldName: String): Boolean = getStaticObjectField(fieldName) as Boolean

/** 写入静态 `Boolean` 字段。 */
fun Class<*>.setStaticBooleanField(fieldName: String, value: Boolean) = setStaticObjectField(fieldName, value)

/** 读取静态 `Int` 字段。 */
fun Class<*>.getStaticIntField(fieldName: String): Int = getStaticObjectField(fieldName) as Int

/** 写入静态 `Int` 字段。 */
fun Class<*>.setStaticIntField(fieldName: String, value: Int) = setStaticObjectField(fieldName, value)

/** 读取静态 `Long` 字段。 */
fun Class<*>.getStaticLongField(fieldName: String): Long = getStaticObjectField(fieldName) as Long

/** 写入静态 `Long` 字段。 */
fun Class<*>.setStaticLongField(fieldName: String, value: Long) = setStaticObjectField(fieldName, value)

/** 读取静态 `Float` 字段。 */
fun Class<*>.getStaticFloatField(fieldName: String): Float = getStaticObjectField(fieldName) as Float

/** 写入静态 `Float` 字段。 */
fun Class<*>.setStaticFloatField(fieldName: String, value: Float) = setStaticObjectField(fieldName, value)

/** 读取静态 `Double` 字段。 */
fun Class<*>.getStaticDoubleField(fieldName: String): Double = getStaticObjectField(fieldName) as Double

/** 写入静态 `Double` 字段。 */
fun Class<*>.setStaticDoubleField(fieldName: String, value: Double) = setStaticObjectField(fieldName, value)

/** 读取静态 `Byte` 字段。 */
fun Class<*>.getStaticByteField(fieldName: String): Byte = getStaticObjectField(fieldName) as Byte

/** 写入静态 `Byte` 字段。 */
fun Class<*>.setStaticByteField(fieldName: String, value: Byte) = setStaticObjectField(fieldName, value)

/** 读取静态 `Short` 字段。 */
fun Class<*>.getStaticShortField(fieldName: String): Short = getStaticObjectField(fieldName) as Short

/** 写入静态 `Short` 字段。 */
fun Class<*>.setStaticShortField(fieldName: String, value: Short) = setStaticObjectField(fieldName, value)

/** 读取静态 `Char` 字段。 */
fun Class<*>.getStaticCharField(fieldName: String): Char = getStaticObjectField(fieldName) as Char

/** 写入静态 `Char` 字段。 */
fun Class<*>.setStaticCharField(fieldName: String, value: Char) = setStaticObjectField(fieldName, value)

/** 读取静态 `Boolean` 字段，失败时返回 `null`。 */
fun Class<*>.getStaticBooleanFieldOrNull(fieldName: String): Boolean? = runCatching { getStaticBooleanField(fieldName) }.getOrNull()

/** 读取静态 `Int` 字段，失败时返回 `null`。 */
fun Class<*>.getStaticIntFieldOrNull(fieldName: String): Int? = runCatching { getStaticIntField(fieldName) }.getOrNull()

/** 读取静态 `Long` 字段，失败时返回 `null`。 */
fun Class<*>.getStaticLongFieldOrNull(fieldName: String): Long? = runCatching { getStaticLongField(fieldName) }.getOrNull()

/** 读取静态 `Float` 字段，失败时返回 `null`。 */
fun Class<*>.getStaticFloatFieldOrNull(fieldName: String): Float? = runCatching { getStaticFloatField(fieldName) }.getOrNull()

/** 读取静态 `Double` 字段，失败时返回 `null`。 */
fun Class<*>.getStaticDoubleFieldOrNull(fieldName: String): Double? = runCatching { getStaticDoubleField(fieldName) }.getOrNull()

/** 读取静态 `Byte` 字段，失败时返回 `null`。 */
fun Class<*>.getStaticByteFieldOrNull(fieldName: String): Byte? = runCatching { getStaticByteField(fieldName) }.getOrNull()

/** 读取静态 `Short` 字段，失败时返回 `null`。 */
fun Class<*>.getStaticShortFieldOrNull(fieldName: String): Short? = runCatching { getStaticShortField(fieldName) }.getOrNull()

/** 读取静态 `Char` 字段，失败时返回 `null`。 */
fun Class<*>.getStaticCharFieldOrNull(fieldName: String): Char? = runCatching { getStaticCharField(fieldName) }.getOrNull()

/**
 * 获取静态字段引用。
 *
 * @param fieldName 字段名
 * @param type 可选字段类型，用于进一步确认匹配结果
 */
fun Class<*>.staticFieldRef(fieldName: String, type: Class<*>? = null): Field =
    field(fieldName, isStatic = true, fieldType = type)

/**
 * 获取静态字段引用，找不到时返回 `null`。
 *
 * @param fieldName 字段名
 * @param type 可选字段类型，用于进一步确认匹配结果
 */
fun Class<*>.staticFieldRefOrNull(fieldName: String, type: Class<*>? = null): Field? =
    fieldOrNull(fieldName, isStatic = true, fieldType = type)

/**
 * 读取静态字段值。
 *
 * @param fieldName 字段名
 * @param type 可选字段类型，用于进一步确认匹配结果
 */
fun Class<*>.staticFieldValue(fieldName: String, type: Class<*>? = null): Any? =
    getStaticField(fieldName, type)

/**
 * 读取静态字段值并转为指定类型。
 *
 * @param fieldName 字段名
 * @param type 可选字段类型，用于进一步确认匹配结果
 */
fun <T> Class<*>.staticFieldValueAs(fieldName: String, type: Class<*>? = null): T? =
    getStaticFieldAs(fieldName, type)

/**
 * 读取静态字段值，失败时返回 `null`。
 *
 * @param fieldName 字段名
 * @param type 可选字段类型，用于进一步确认匹配结果
 */
fun Class<*>.staticFieldValueOrNull(fieldName: String, type: Class<*>? = null): Any? =
    getStaticFieldOrNull(fieldName, type)

/**
 * 读取静态字段值并转为指定类型，失败时返回 `null`。
 *
 * @param fieldName 字段名
 * @param type 可选字段类型，用于进一步确认匹配结果
 */
fun <T> Class<*>.staticFieldValueOrNullAs(fieldName: String, type: Class<*>? = null): T? =
    getStaticFieldOrNullAs(fieldName, type)

/**
 * 写入静态字段值。
 *
 * @param fieldName 字段名
 * @param value 新值
 * @param type 可选字段类型，用于进一步确认匹配结果
 */
fun Class<*>.setStaticFieldValue(fieldName: String, value: Any?, type: Class<*>? = null) =
    putStaticField(fieldName, value, type)

/**
 * 通过字段引用写入静态字段值。
 *
 * @param field 字段引用
 * @param value 新值
 */
fun Class<*>.setStaticFieldValue(field: Field, value: Any?) =
    putStaticField(field, value)

/**
 * 按类名读取静态字段值。
 *
 * @param fieldName 字段名
 * @param classLoader 用于加载目标类的 `ClassLoader`
 * @param type 可选字段类型，用于进一步确认匹配结果
 */
fun String.staticFieldValue(
    fieldName: String,
    classLoader: ClassLoader = HookClassLoader.currentOrDefault(),
    type: Class<*>? = null,
): Any? = loadClass(this, classLoader).staticFieldValue(fieldName, type)

/**
 * 按类名读取静态字段值并转为指定类型。
 *
 * @param fieldName 字段名
 * @param classLoader 用于加载目标类的 `ClassLoader`
 * @param type 可选字段类型，用于进一步确认匹配结果
 */
fun <T> String.staticFieldValueAs(
    fieldName: String,
    classLoader: ClassLoader = HookClassLoader.currentOrDefault(),
    type: Class<*>? = null,
): T? = loadClass(this, classLoader).staticFieldValueAs(fieldName, type)

/**
 * 按类名写入静态字段值。
 *
 * @param fieldName 字段名
 * @param value 新值
 * @param classLoader 用于加载目标类的 `ClassLoader`
 * @param type 可选字段类型，用于进一步确认匹配结果
 */
fun String.setStaticFieldValue(
    fieldName: String,
    value: Any?,
    classLoader: ClassLoader = HookClassLoader.currentOrDefault(),
    type: Class<*>? = null,
) = loadClass(this, classLoader).setStaticFieldValue(fieldName, value, type)

/**
 * 按类名读取静态字段。
 *
 * @param fieldName 字段名
 * @param classLoader 用于加载目标类的 `ClassLoader`
 */
fun String.getStaticObjectField(
    fieldName: String,
    classLoader: ClassLoader = HookClassLoader.currentOrDefault(),
): Any? = loadClass(this, classLoader).getStaticObjectField(fieldName)

/**
 * 按类名读取静态字段并转为指定类型。
 *
 * @param fieldName 字段名
 * @param classLoader 用于加载目标类的 `ClassLoader`
 */
fun <T> String.getStaticObjectFieldAs(
    fieldName: String,
    classLoader: ClassLoader = HookClassLoader.currentOrDefault(),
): T = loadClass(this, classLoader).getStaticObjectFieldAs(fieldName)

/**
 * 按类名读取静态字段，失败时返回 `null`。
 *
 * @param fieldName 字段名
 * @param classLoader 用于加载目标类的 `ClassLoader`
 */
fun String.getStaticObjectFieldOrNull(
    fieldName: String,
    classLoader: ClassLoader = HookClassLoader.currentOrDefault(),
): Any? = loadClass(this, classLoader).getStaticObjectFieldOrNull(fieldName)

/**
 * 按类名读取静态字段并转为指定类型，失败时返回 `null`。
 *
 * @param fieldName 字段名
 * @param classLoader 用于加载目标类的 `ClassLoader`
 */
fun <T> String.getStaticObjectFieldAsOrNull(
    fieldName: String,
    classLoader: ClassLoader = HookClassLoader.currentOrDefault(),
): T? = loadClass(this, classLoader).getStaticObjectFieldAsOrNull(fieldName)

/**
 * 按类名写入静态字段。
 *
 * @param fieldName 字段名
 * @param value 新值
 * @param classLoader 用于加载目标类的 `ClassLoader`
 */
fun String.setStaticObjectField(
    fieldName: String,
    value: Any?,
    classLoader: ClassLoader = HookClassLoader.currentOrDefault(),
) = loadClass(this, classLoader).setStaticObjectField(fieldName, value)

/**
 * 读取附加到实例上的字段。
 *
 * Additional field 不会写入目标对象原始字段表，适合在 hook 中临时保存状态。
 *
 * @param field 附加字段名
 */
fun Any.getAdditionalInstanceField(field: String): Any? =
    AdditionalFields.getInstance(this, field)

/**
 * 读取附加到实例上的字段并转为指定类型。
 *
 * @param field 附加字段名
 */
fun <T> Any.getAdditionalInstanceFieldAs(field: String): T? =
    getAdditionalInstanceField(field) as? T

/**
 * 写入附加到实例上的字段。
 *
 * @param field 附加字段名
 * @param value 新值
 */
fun Any.setAdditionalInstanceField(field: String, value: Any?): Any? =
    AdditionalFields.setInstance(this, field, value)

/**
 * 移除附加到实例上的字段。
 *
 * @param field 附加字段名
 */
fun Any.removeAdditionalInstanceField(field: String): Any? =
    AdditionalFields.removeInstance(this, field)

/**
 * 读取附加到类上的静态字段。
 *
 * @param field 附加字段名
 */
fun Class<*>.getAdditionalStaticField(field: String): Any? =
    AdditionalFields.getStatic(this, field)

/**
 * 读取附加到类上的静态字段并转为指定类型。
 *
 * @param field 附加字段名
 */
fun <T> Class<*>.getAdditionalStaticFieldAs(field: String): T? =
    getAdditionalStaticField(field) as? T

/**
 * 写入附加到类上的静态字段。
 *
 * @param field 附加字段名
 * @param value 新值
 */
fun Class<*>.setAdditionalStaticField(field: String, value: Any?): Any? =
    AdditionalFields.setStatic(this, field, value)

/**
 * 移除附加到类上的静态字段。
 *
 * @param field 附加字段名
 */
fun Class<*>.removeAdditionalStaticField(field: String): Any? =
    AdditionalFields.removeStatic(this, field)

/**
 * 按类名读取附加静态字段。
 *
 * @param field 附加字段名
 * @param classLoader 用于加载目标类的 `ClassLoader`
 */
fun String.getAdditionalStaticField(
    field: String,
    classLoader: ClassLoader = HookClassLoader.currentOrDefault(),
): Any? = loadClass(this, classLoader).getAdditionalStaticField(field)

/**
 * 按类名读取附加静态字段并转为指定类型。
 *
 * @param field 附加字段名
 * @param classLoader 用于加载目标类的 `ClassLoader`
 */
fun <T> String.getAdditionalStaticFieldAs(
    field: String,
    classLoader: ClassLoader = HookClassLoader.currentOrDefault(),
): T? = loadClass(this, classLoader).getAdditionalStaticFieldAs(field)

/**
 * 按类名写入附加静态字段。
 *
 * @param field 附加字段名
 * @param value 新值
 * @param classLoader 用于加载目标类的 `ClassLoader`
 */
fun String.setAdditionalStaticField(
    field: String,
    value: Any?,
    classLoader: ClassLoader = HookClassLoader.currentOrDefault(),
): Any? = loadClass(this, classLoader).setAdditionalStaticField(field, value)

/**
 * 按类名移除附加静态字段。
 *
 * @param field 附加字段名
 * @param classLoader 用于加载目标类的 `ClassLoader`
 */
fun String.removeAdditionalStaticField(
    field: String,
    classLoader: ClassLoader = HookClassLoader.currentOrDefault(),
): Any? = loadClass(this, classLoader).removeAdditionalStaticField(field)

/**
 * 读取附加到实例上的字段。
 *
 * @param key 附加字段名
 */
fun Any.additionalField(key: String): Any? =
    getAdditionalInstanceField(key)

/**
 * 读取附加到实例上的字段并转为指定类型。
 *
 * @param key 附加字段名
 */
fun <T> Any.additionalFieldAs(key: String): T? =
    getAdditionalInstanceFieldAs(key)

/**
 * 写入附加到实例上的字段。
 *
 * @param key 附加字段名
 * @param value 新值
 */
fun Any.setAdditionalField(key: String, value: Any?): Any? =
    setAdditionalInstanceField(key, value)

/**
 * 移除附加到实例上的字段。
 *
 * @param key 附加字段名
 */
fun Any.removeAdditionalField(key: String): Any? =
    removeAdditionalInstanceField(key)

/**
 * 读取附加到类上的静态字段。
 *
 * @param key 附加字段名
 */
fun Class<*>.additionalStaticField(key: String): Any? =
    getAdditionalStaticField(key)

/**
 * 读取附加到类上的静态字段并转为指定类型。
 *
 * @param key 附加字段名
 */
fun <T> Class<*>.additionalStaticFieldAs(key: String): T? =
    getAdditionalStaticFieldAs(key)

/**
 * 按类名读取附加静态字段。
 *
 * @param key 附加字段名
 * @param classLoader 用于加载目标类的 `ClassLoader`
 */
fun String.additionalStaticField(
    key: String,
    classLoader: ClassLoader = HookClassLoader.currentOrDefault(),
): Any? = loadClass(this, classLoader).getAdditionalStaticField(key)

/**
 * 按类名读取附加静态字段并转为指定类型。
 *
 * @param key 附加字段名
 * @param classLoader 用于加载目标类的 `ClassLoader`
 */
fun <T> String.additionalStaticFieldAs(
    key: String,
    classLoader: ClassLoader = HookClassLoader.currentOrDefault(),
): T? = loadClass(this, classLoader).getAdditionalStaticFieldAs(key)
