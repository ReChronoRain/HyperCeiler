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

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
@file:Suppress("UNCHECKED_CAST")

package com.sevtinge.hyperceiler.libhook.utils.hookapi.tool

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook
import com.sevtinge.hyperceiler.libhook.callback.IReplaceHook
import io.github.kyuubiran.ezxhelper.xposed.common.HookParam
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.HookHandle
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.lang.reflect.Modifier

typealias HookBlock = (HookParam) -> Unit
typealias ReplaceHookBlock = (HookParam) -> Any?
typealias ChainInterceptor = XposedInterface.Chain.() -> Any?
typealias HookExceptionMode = XposedInterface.ExceptionMode

/**
 * Hook 回调 DSL 构建器
 *
 * 用法示例：
 * ```kotlin
 * clazz.hookMethod("methodName", String::class.java) {
 *     before { param -> ... }
 *     after { param -> ... }
 * }
 * ```
 */
class MethodHookBuilder {
    private var beforeBlock: HookBlock? = null
    private var afterBlock: HookBlock? = null

    fun before(block: HookBlock) {
        beforeBlock = block
    }

    fun after(block: HookBlock) {
        afterBlock = block
    }

    fun build(): IMethodHook {
        return object : IMethodHook {
            override fun before(param: HookParam) {
                beforeBlock?.invoke(param)
            }

            override fun after(param: HookParam) {
                afterBlock?.invoke(param)
            }
        }
    }
}

inline fun methodHook(block: MethodHookBuilder.() -> Unit): IMethodHook {
    val builder = MethodHookBuilder()
    builder.block()
    return builder.build()
}

inline fun beforeHook(crossinline block: HookBlock): IMethodHook {
    return object : IMethodHook {
        override fun before(param: HookParam) = block(param)
    }
}

inline fun afterHook(crossinline block: HookBlock): IMethodHook {
    return object : IMethodHook {
        override fun after(param: HookParam) = block(param)
    }
}

inline fun replaceHook(crossinline block: ReplaceHookBlock): IReplaceHook {
    return IReplaceHook { param -> block(param) }
}

fun Method.hook(
    priority: Int = XposedInterface.PRIORITY_DEFAULT,
    exceptionMode: HookExceptionMode = HookExceptionMode.DEFAULT,
    block: ChainInterceptor
): HookHandle = EzxHelpUtils.chain(this, priority, exceptionMode) { chain -> chain.block() }

fun Constructor<*>.hook(
    priority: Int = XposedInterface.PRIORITY_DEFAULT,
    exceptionMode: HookExceptionMode = HookExceptionMode.DEFAULT,
    block: ChainInterceptor
): HookHandle = EzxHelpUtils.chain(this, priority, exceptionMode) { chain -> chain.block() }

fun Class<*>.chainMethod(
    methodName: String,
    vararg args: Any,
    priority: Int = XposedInterface.PRIORITY_DEFAULT,
    exceptionMode: HookExceptionMode = HookExceptionMode.DEFAULT,
    block: ChainInterceptor
): HookHandle = EzxHelpUtils.findAndChainMethod(
    this,
    methodName,
    *args,
    priority = priority,
    exceptionMode = exceptionMode,
    block = { chain -> chain.block() }
)

fun Class<*>.chainConstructor(
    vararg args: Any,
    priority: Int = XposedInterface.PRIORITY_DEFAULT,
    exceptionMode: HookExceptionMode = HookExceptionMode.DEFAULT,
    block: ChainInterceptor
): HookHandle = EzxHelpUtils.findAndChainConstructor(
    this,
    *args,
    priority = priority,
    exceptionMode = exceptionMode,
    block = { chain -> chain.block() }
)

// -------------------- 方法 Hook --------------------

inline fun Class<*>.beforeHookMethod(
    methodName: String,
    vararg args: Any,
    crossinline block: HookBlock
): HookHandle {
    return EzxHelpUtils.findAndHookMethod(this, methodName, *args, beforeHook(block))
}

inline fun Class<*>.afterHookMethod(
    methodName: String,
    vararg args: Any,
    crossinline block: HookBlock
): HookHandle {
    return EzxHelpUtils.findAndHookMethod(this, methodName, *args, afterHook(block))
}

inline fun Class<*>.hookMethod(
    methodName: String,
    vararg args: Any,
    block: MethodHookBuilder.() -> Unit
): HookHandle {
    return EzxHelpUtils.findAndHookMethod(this, methodName, *args, methodHook(block))
}

inline fun Class<*>.hookAllMethods(
    methodName: String,
    block: MethodHookBuilder.() -> Unit
): List<HookHandle> {
    return EzxHelpUtils.hookAllMethods(this, methodName, methodHook(block))
}

inline fun Class<*>.replaceMethod(
    methodName: String,
    vararg args: Any,
    crossinline block: ReplaceHookBlock
): HookHandle {
    return EzxHelpUtils.findAndHookMethodReplace(this, methodName, *args, replaceHook(block))
}

// -------------------- 构造器 Hook --------------------

inline fun Class<*>.beforeHookConstructor(
    vararg args: Any,
    crossinline block: HookBlock
): HookHandle {
    return EzxHelpUtils.findAndHookConstructor(this, *args, beforeHook(block))
}

inline fun Class<*>.afterHookConstructor(
    vararg args: Any,
    crossinline block: HookBlock
): HookHandle {
    return EzxHelpUtils.findAndHookConstructor(this, *args, afterHook(block))
}

inline fun Class<*>.hookConstructor(
    vararg args: Any,
    block: MethodHookBuilder.() -> Unit
): HookHandle {
    return EzxHelpUtils.findAndHookConstructor(this, *args, methodHook(block))
}

inline fun Class<*>.hookAllConstructors(
    block: MethodHookBuilder.() -> Unit
): List<HookHandle> {
    return EzxHelpUtils.hookAllConstructors(this, methodHook(block))
}


fun Method.hookCallback(callback: IMethodHook): HookHandle =
    EzxHelpUtils.hookMethod(this, callback)

fun Method.replaceCallback(callback: IReplaceHook): HookHandle =
    EzxHelpUtils.hookMethod(this, callback)

fun Constructor<*>.hookCallback(callback: IMethodHook): HookHandle =
    EzxHelpUtils.hookConstructor(this, callback)

fun Method.deoptimizeMethod() = EzxHelpUtils.deoptimize(this)

fun Class<*>.deoptimizeMethods(vararg names: String?) =
    EzxHelpUtils.deoptimizeMethods(this, *names)


// -------------------- 顶层字段查找工具 --------------------

/**
 * 通过字段名递归查找字段值（遍历父类）
 *
 * @param target 对象实例
 * @param fieldName 字段名
 * @param clazz 指定起始搜索类，默认使用对象的实际类型
 * @return 字段值，找不到返回 null
 */
fun getValueByField(target: Any, fieldName: String, clazz: Class<*>? = null): Any? {
    val targetClass = clazz ?: target.javaClass
    return try {
        val field = targetClass.getDeclaredField(fieldName)
        field.isAccessible = true
        field.get(target)
    } catch (_: Throwable) {
        if (targetClass.superclass == null) {
            null
        } else {
            getValueByField(target, fieldName, targetClass.superclass)
        }
    }
}

// -------------------- Any 实例字段 get/set --------------------

fun Any.getObjectField(fieldName: String): Any? =
    EzxHelpUtils.getObjectField(this, fieldName)

fun <T> Any.getObjectFieldAs(fieldName: String): T =
    getObjectField(fieldName) as T

fun Any.setObjectField(fieldName: String, value: Any?) =
    EzxHelpUtils.setObjectField(this, fieldName, value)

fun Any.getBooleanField(fieldName: String): Boolean =
    EzxHelpUtils.getBooleanField(this, fieldName)

fun Any.setBooleanField(fieldName: String, value: Boolean) =
    EzxHelpUtils.setBooleanField(this, fieldName, value)

fun Any.getIntField(fieldName: String): Int =
    EzxHelpUtils.getIntField(this, fieldName)

fun Any.setIntField(fieldName: String, value: Int) =
    EzxHelpUtils.setIntField(this, fieldName, value)

fun Any.getLongField(fieldName: String): Long =
    EzxHelpUtils.getLongField(this, fieldName)

fun Any.setLongField(fieldName: String, value: Long) =
    EzxHelpUtils.setLongField(this, fieldName, value)

fun Any.getFloatField(fieldName: String): Float =
    EzxHelpUtils.getFloatField(this, fieldName)

fun Any.setFloatField(fieldName: String, value: Float) =
    EzxHelpUtils.setFloatField(this, fieldName, value)

// -------------------- Any 实例字段 safe (OrNull) --------------------

fun Any.getObjectFieldOrNull(fieldName: String): Any? =
    runCatching { getObjectField(fieldName) }.getOrNull()

fun <T> Any.getObjectFieldOrNullAs(fieldName: String): T? =
    runCatching { getObjectField(fieldName) as? T }.getOrNull()

fun Any.getBooleanFieldOrNull(fieldName: String): Boolean? =
    runCatching { getBooleanField(fieldName) }.getOrNull()

fun Any.getIntFieldOrNull(fieldName: String): Int? =
    runCatching { getIntField(fieldName) }.getOrNull()

fun Any.getLongFieldOrNull(fieldName: String): Long? =
    runCatching { getLongField(fieldName) }.getOrNull()

fun Any.getFloatFieldOrNull(fieldName: String): Float? =
    runCatching { getFloatField(fieldName) }.getOrNull()

// -------------------- Any 按类型查找字段 --------------------

fun Any.getFirstFieldByExactType(type: Class<*>): Any? =
    javaClass.findFirstFieldByExactType(type).get(this)

fun <T> Any.getFirstFieldByExactTypeAs(type: Class<*>) =
    javaClass.findFirstFieldByExactType(type).get(this) as? T

// -------------------- Class<*> 静态字段 get/set --------------------

fun Class<*>.getStaticObjectField(fieldName: String): Any? =
    EzxHelpUtils.getStaticObjectField(this, fieldName)

fun <T> Class<*>.getStaticObjectFieldAs(fieldName: String): T =
    getStaticObjectField(fieldName) as T

fun Class<*>.setStaticObjectField(fieldName: String, value: Any?) =
    EzxHelpUtils.setStaticObjectField(this, fieldName, value)

fun Class<*>.getStaticBooleanField(fieldName: String): Boolean =
    EzxHelpUtils.getStaticBooleanField(this, fieldName)

fun Class<*>.setStaticBooleanField(fieldName: String, value: Boolean) =
    EzxHelpUtils.setStaticBooleanField(this, fieldName, value)

fun Class<*>.getStaticIntField(fieldName: String): Int =
    EzxHelpUtils.getStaticIntField(this, fieldName)

fun Class<*>.setStaticIntField(fieldName: String, value: Int) =
    EzxHelpUtils.setStaticIntField(this, fieldName, value)

fun Class<*>.getStaticLongField(fieldName: String): Long =
    EzxHelpUtils.getStaticLongField(this, fieldName)

fun Class<*>.setStaticLongField(fieldName: String, value: Long) =
    EzxHelpUtils.setStaticLongField(this, fieldName, value)

// -------------------- Class<*> 静态字段 safe (OrNull) --------------------

fun Class<*>.getStaticObjectFieldOrNull(fieldName: String): Any? =
    runCatching { getStaticObjectField(fieldName) }.getOrNull()

fun <T> Class<*>.getStaticObjectFieldAsOrNull(fieldName: String): T? =
    runCatching { getStaticObjectField(fieldName) as? T }.getOrNull()

fun Class<*>.getStaticBooleanFieldOrNull(fieldName: String): Boolean? =
    runCatching { getStaticBooleanField(fieldName) }.getOrNull()

fun Class<*>.getStaticIntFieldOrNull(fieldName: String): Int? =
    runCatching { getStaticIntField(fieldName) }.getOrNull()

fun Class<*>.getStaticLongFieldOrNull(fieldName: String): Long? =
    runCatching { getStaticLongField(fieldName) }.getOrNull()

// -------------------- Class<*> 字段查找 --------------------

fun Class<*>.findField(name: String): Field =
    EzxHelpUtils.findField(this, name)

fun Class<*>.findFieldOrNull(name: String): Field? =
    runCatching { EzxHelpUtils.findField(this, name) }.getOrNull()

fun Class<*>.findFieldByExactType(type: Class<*>): Field =
    EzxHelpUtils.findFirstFieldByExactType(this, type)

fun Class<*>.findFirstFieldByExactType(type: Class<*>): Field =
    EzxHelpUtils.findFirstFieldByExactType(this, type)

// -------------------- 附加实例字段 --------------------

fun Any.getAdditionalInstanceField(field: String): Any? =
    EzxHelpUtils.getAdditionalInstanceField(this, field)

fun <T> Any.getAdditionalInstanceFieldAs(field: String) =
    EzxHelpUtils.getAdditionalInstanceField(this, field) as? T

fun Any.setAdditionalInstanceField(field: String, value: Any?): Any? =
    EzxHelpUtils.setAdditionalInstanceField(this, field, value)

fun Any.removeAdditionalInstanceField(field: String): Any? =
    EzxHelpUtils.removeAdditionalInstanceField(this, field)


// -------------------- Any 实例方法调用 --------------------

fun Any.callMethod(methodName: String, vararg args: Any?): Any? =
    EzxHelpUtils.callMethod(this, methodName, *args)

fun <T> Any.callMethodAs(methodName: String, vararg args: Any?): T =
    callMethod(methodName, *args) as T

fun Any.callMethodOrNull(methodName: String, vararg args: Any?): Any? =
    runCatching { EzxHelpUtils.callMethod(this, methodName, *args) }.getOrNull()

// -------------------- Class<*> 静态方法调用 --------------------

fun Class<*>.callStaticMethod(methodName: String, vararg args: Any?): Any? =
    EzxHelpUtils.callStaticMethod(this, methodName, *args)

fun <T> Class<*>.callStaticMethodAs(methodName: String, vararg args: Any?): T =
    callStaticMethod(methodName, *args) as T

// -------------------- 实例化 --------------------

fun <T> Class<*>.newInstance(vararg args: Any?): T =
    EzxHelpUtils.newInstance(this, *args) as T

fun String.toClass(classLoader: ClassLoader?): Class<*> =
    EzxHelpUtils.findClass(this, classLoader)

fun String.toClassOrNull(classLoader: ClassLoader?): Class<*>? =
    EzxHelpUtils.findClassIfExists(this, classLoader)


val Member.isStatic: Boolean
    inline get() = Modifier.isStatic(modifiers)

val Member.isFinal: Boolean
    inline get() = Modifier.isFinal(modifiers)

val Member.isPublic: Boolean
    inline get() = Modifier.isPublic(modifiers)

val Member.isNotStatic: Boolean
    inline get() = !isStatic

val Class<*>.isAbstract: Boolean
    inline get() = !isPrimitive && Modifier.isAbstract(modifiers)


// -------------------- Context 资源查找 --------------------

@SuppressLint("DiscouragedApi")
fun Context.getIdByName(
    name: String,
    type: String = "id"
): Int = resources.getIdentifier(name, type, packageName)

@StringRes
fun Context.getStringIdByName(name: String): Int = getIdByName(name, "string")
fun Context.getString(name: String): String = getString(getStringIdByName(name))

@DimenRes
fun Context.getDimenByName(name: String): Int = getIdByName(name, "dimen")

@DrawableRes
fun Context.getDrawableIdByName(name: String): Int = getIdByName(name, "drawable")
fun Context.getDrawable(name: String): Drawable? =
    getDrawable(getDrawableIdByName(name))

fun View.findViewByIdName(name: String): View? {
    val viewId = context.getIdByName(name)
    return if (viewId != 0) findViewById(viewId) else null
}

fun Activity.findViewByIdName(name: String): View? {
    val viewId = this.getIdByName(name)
    return if (viewId != 0) findViewById(viewId) else null
}

// -------------------- View padding --------------------

fun View.setPadding(padding: Int) =
    setPadding(padding, padding, padding, padding)

fun View.setPaddingLeft(paddingLeft: Int) =
    setPaddingSide(paddingLeft, paddingRight)

fun View.setPaddingRight(paddingRight: Int) =
    setPaddingSide(paddingLeft, paddingRight)

fun View.setPaddingSide(paddingSide: Int) =
    setPaddingSide(paddingSide, paddingSide)

fun View.setPaddingSide(paddingLeft: Int, paddingRight: Int) =
    setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)

// -------------------- Resources Hook 数据类 --------------------

data class ResourcesHookData(val type: String, val afterValue: Any)

class ResourcesHookMap<String, ResourcesHookData> : HashMap<String, ResourcesHookData>() {
    fun isKeyExist(key: String): Boolean = getOrDefault(key, null) != null
}
