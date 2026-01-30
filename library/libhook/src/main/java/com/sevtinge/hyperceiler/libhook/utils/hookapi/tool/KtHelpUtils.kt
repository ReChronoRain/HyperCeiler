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
package com.sevtinge.hyperceiler.libhook.utils.hookapi.tool

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook
import com.sevtinge.hyperceiler.libhook.callback.IReplaceHook
import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam
import io.github.libxposed.api.XposedInterface.MethodUnhooker
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.lang.reflect.Modifier

class MethodHookBuilder {
    private var beforeBlock: ((BeforeHookParam) -> Unit)? = null
    private var afterBlock: ((AfterHookParam) -> Unit)? = null

    fun before(block: (BeforeHookParam) -> Unit) {
        beforeBlock = block
    }

    fun after(block: (AfterHookParam) -> Unit) {
        afterBlock = block
    }

    fun build(): IMethodHook {
        return object : IMethodHook {
            override fun before(param: BeforeHookParam) {
                beforeBlock?.invoke(param)
            }

            override fun after(param: AfterHookParam) {
                afterBlock?.invoke(param)
            }
        }
    }
}

inline fun Class<*>.beforeHookMethod(
    methodName: String,
    vararg args: Any,
    crossinline block: (BeforeHookParam) -> Unit
): MethodUnhooker<*> {
    return EzxHelpUtils.findAndHookMethod(
        this, methodName, *args,
        object : IMethodHook {
            override fun before(param: BeforeHookParam) = block(param)
        }
    )
}

inline fun Class<*>.afterHookMethod(
    methodName: String,
    vararg args: Any,
    crossinline block: (AfterHookParam) -> Unit
): MethodUnhooker<*> {
    return EzxHelpUtils.findAndHookMethod(
        this, methodName, *args,
        object : IMethodHook {
            override fun after(param: AfterHookParam) = block(param)
        }
    )
}

inline fun Class<*>.hookMethod(
    methodName: String,
    vararg args: Any,
    block: MethodHookBuilder.() -> Unit
): MethodUnhooker<*> {
    val builder = MethodHookBuilder()
    builder.block()
    return EzxHelpUtils.findAndHookMethod(this, methodName, *args, builder.build())
}

inline fun Class<*>.hookAllMethods(
    methodName: String,
    block: MethodHookBuilder.() -> Unit
): List<MethodUnhooker<*>> {
    val builder = MethodHookBuilder()
    builder.block()
    return EzxHelpUtils.hookAllMethods(this, methodName, builder.build())
}

inline fun Class<*>.replaceMethod(
    methodName: String,
    vararg args: Any,
    crossinline block: (BeforeHookParam) -> Any?
): MethodUnhooker<*> {
    return EzxHelpUtils.findAndHookMethodReplace(
        this, methodName, *args,
        object : IReplaceHook {
            override fun replace(param: BeforeHookParam): Any? = block(param)
        }
    )
}

inline fun Class<*>.beforeHookConstructor(
    vararg args: Any,
    crossinline block: (BeforeHookParam) -> Unit
): MethodUnhooker<*> {
    return EzxHelpUtils.findAndHookConstructor(
        this, *args,
        object : IMethodHook {
            override fun before(param: BeforeHookParam) = block(param)
        }
    )
}

inline fun Class<*>.afterHookConstructor(
    vararg args: Any,
    crossinline block: (AfterHookParam) -> Unit
): MethodUnhooker<*> {
    return EzxHelpUtils.findAndHookConstructor(
        this, *args,
        object : IMethodHook {
            override fun after(param: AfterHookParam) = block(param)
        }
    )
}

inline fun Class<*>.hookConstructor(
    vararg args: Any,
    block: MethodHookBuilder.() -> Unit
): MethodUnhooker<*> {
    val builder = MethodHookBuilder()
    builder.block()
    return EzxHelpUtils.findAndHookConstructor(this, *args, builder.build())
}

inline fun Class<*>.hookAllConstructors(
    block: MethodHookBuilder.() -> Unit
): List<MethodUnhooker<*>> {
    val builder = MethodHookBuilder()
    builder.block()
    return EzxHelpUtils.hookAllConstructors(this, builder.build())
}

// ==================== Any 扩展函数 ====================

fun Any.getObjectField(fieldName: String): Any? = EzxHelpUtils.getObjectField(this, fieldName)
fun <T> Any.getObjectFieldAs(fieldName: String): T = getObjectField(fieldName) as T
fun Any.setObjectField(fieldName: String, value: Any?) =
    EzxHelpUtils.setObjectField(this, fieldName, value)

fun Any.getBooleanField(fieldName: String): Boolean = EzxHelpUtils.getBooleanField(this, fieldName)
fun Any.setBooleanField(fieldName: String, value: Boolean) =
    EzxHelpUtils.setBooleanField(this, fieldName, value)

fun Any.getIntField(fieldName: String): Int = EzxHelpUtils.getIntField(this, fieldName)
fun Any.setIntField(fieldName: String, value: Int) =
    EzxHelpUtils.setIntField(this, fieldName, value)

fun Any.getLongField(fieldName: String): Long = EzxHelpUtils.getLongField(this, fieldName)
fun Any.setLongField(fieldName: String, value: Long) =
    EzxHelpUtils.setLongField(this, fieldName, value)

fun Any.getFloatField(fieldName: String): Float = EzxHelpUtils.getFloatField(this, fieldName)
fun Any.setFloatField(fieldName: String, value: Float) =
    EzxHelpUtils.setFloatField(this, fieldName, value)

fun Any.getObjectFieldOrNull(fieldName: String): Any? {
    return runCatching { getObjectField(fieldName) }.getOrNull()
}

fun <T> Any.getObjectFieldOrNullAs(fieldName: String): T? {
    return runCatching {
        getObjectField(fieldName) as? T
    }.getOrNull()
}

fun Any.getBooleanFieldOrNull(fieldName: String): Boolean? {
    return runCatching { getBooleanField(fieldName) }.getOrNull()
}

fun Any.getIntFieldOrNull(fieldName: String): Int? {
    return runCatching { getIntField(fieldName) }.getOrNull()
}

fun Any.getLongFieldOrNull(fieldName: String): Long? {
    return runCatching { getLongField(fieldName) }.getOrNull()
}

fun Any.getFloatFieldOrNull(fieldName: String): Float? {
    return runCatching { getFloatField(fieldName) }.getOrNull()
}

fun Any.callMethod(methodName: String, vararg args: Any?): Any? =
    EzxHelpUtils.callMethod(this, methodName, *args)

fun <T> Any.callMethodAs(methodName: String, vararg args: Any?): T =
    callMethod(methodName, *args) as T

fun Any.callMethodOrNull(methodName: String, vararg args: Any?): Any? {
    return runCatching {
        EzxHelpUtils.callMethod(this, methodName, *args)
    }.getOrNull()
}

fun Any.getFirstFieldByExactType(type: Class<*>): Any? =
    javaClass.findFirstFieldByExactType(type).get(this)

fun <T> Any.getFirstFieldByExactTypeAs(type: Class<*>) =
    javaClass.findFirstFieldByExactType(type).get(this) as? T

fun Any.getAdditionalInstanceField(field: String): Any? =
    EzxHelpUtils.getAdditionalInstanceField(this, field)

fun <T> Any.getAdditionalInstanceFieldAs(field: String) =
    EzxHelpUtils.getAdditionalInstanceField(this, field) as T

fun Any.setAdditionalInstanceField(
    field: String,
    value: Any?
): Any? = EzxHelpUtils.setAdditionalInstanceField(this, field, value)

fun Any.removeAdditionalInstanceField(
    field: String
): Any? = EzxHelpUtils.removeAdditionalInstanceField(this, field)

// ==================== Class 扩展函数 ====================

fun Class<*>.getStaticObjectField(fieldName: String): Any? =
    EzxHelpUtils.getStaticObjectField(this, fieldName)

fun <T> Class<*>.getStaticObjectFieldAs(fieldName: String): T = getStaticObjectField(fieldName) as T
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

fun Class<*>.getStaticObjectFieldOrNull(fieldName: String): Any? {
    return runCatching { getStaticObjectField(fieldName) }.getOrNull()
}

fun <T> Class<*>.getStaticObjectFieldAsOrNull(fieldName: String): T? {
    return runCatching {
        getStaticObjectField(fieldName) as? T
    }.getOrNull()
}

fun Class<*>.getStaticBooleanFieldOrNull(fieldName: String): Boolean? {
    return runCatching { getStaticBooleanField(fieldName) }.getOrNull()
}

fun Class<*>.getStaticIntFieldOrNull(fieldName: String): Int? {
    return runCatching { getStaticIntField(fieldName) }.getOrNull()
}

fun Class<*>.getStaticLongFieldOrNull(fieldName: String): Long? {
    return runCatching { getStaticLongField(fieldName) }.getOrNull()
}

fun Class<*>.callStaticMethod(methodName: String, vararg args: Any?): Any? =
    EzxHelpUtils.callStaticMethod(this, methodName, *args)

fun <T> Class<*>.callStaticMethodAs(methodName: String, vararg args: Any?): T =
    callStaticMethod(methodName, *args) as T

fun <T> Class<*>.newInstance(vararg args: Any?): T = EzxHelpUtils.newInstance(this, *args) as T

fun Class<*>.findField(name: String): Field =
    EzxHelpUtils.findField(this, name)

fun Class<*>.findFieldOrNull(name: String): Field? = runCatching {
        EzxHelpUtils.findField(this, name)
    }.getOrNull()

fun Class<*>.findFieldByExactType(type: Class<*>): Field =
    EzxHelpUtils.findFirstFieldByExactType(this, type)

fun Class<*>.findFirstFieldByExactType(type: Class<*>): Field =
    EzxHelpUtils.findFirstFieldByExactType(this, type)


// ==================== String 扩展函数 ====================

fun String.toClass(classLoader: ClassLoader?): Class<*> = EzxHelpUtils.findClass(this, classLoader)
fun String.toClassOrNull(classLoader: ClassLoader?): Class<*>? =
    EzxHelpUtils.findClassIfExists(this, classLoader)

// ==================== Hook 扩展函数 ====================

fun Method.hook(callback: IMethodHook): MethodUnhooker<*> = EzxHelpUtils.hookMethod(this, callback)
fun Method.hookReplace(callback: IReplaceHook): MethodUnhooker<*> =
    EzxHelpUtils.hookMethod(this, callback)

fun Constructor<*>.hook(callback: IMethodHook): MethodUnhooker<*> =
    EzxHelpUtils.hookConstructor(this, callback)

fun Method.deoptimizeMethod() = EzxHelpUtils.deoptimize(this)
fun Class<*>.deoptimizeMethods(vararg names: String?) = EzxHelpUtils.deoptimizeMethods(this, *names)

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

@SuppressLint("DiscouragedApi")
fun Context.getIdByName(
    name: String,
    type: String = "id"
): Int = resources.getIdentifier(name, type, packageName)

@StringRes
fun Context.getStringIdByName(name: String): Int = getIdByName(name, "string")
fun Context.getString(name: String): String = getString(getStringIdByName(name))

@DrawableRes
fun Context.getDrawableIdByName(name: String): Int = getIdByName(name, "drawable")
fun Context.getDrawable(
    name: String
): Drawable? = getDrawable(getDrawableIdByName(name))

fun View.setPadding(padding: Int) = setPadding(padding, padding, padding, padding)

fun View.setPaddingLeft(paddingLeft: Int) = setPaddingSide(paddingLeft, paddingRight)
fun View.setPaddingRight(paddingRight: Int) = setPaddingSide(paddingLeft, paddingRight)

fun View.setPaddingSide(paddingSide: Int) = setPaddingSide(paddingSide, paddingSide)
fun View.setPaddingSide(
    paddingLeft: Int,
    paddingRight: Int
) = setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)

data class ResourcesHookData(val type: String, val afterValue: Any)

class ResourcesHookMap<String, ResourcesHookData> : HashMap<String, ResourcesHookData>() {
    fun isKeyExist(key: String): Boolean = getOrDefault(key, null) != null
}
