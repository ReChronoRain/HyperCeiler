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

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.utils.api

import android.app.admin.*
import android.content.*
import android.content.res.Resources.*
import android.view.*
import android.widget.*
import com.github.kyuubiran.ezxhelper.*
import com.github.kyuubiran.ezxhelper.MemberExtensions.isStatic
import de.robv.android.xposed.*
import java.lang.reflect.*

@JvmInline
value class Args(val args: Array<out Any?>)

@JvmInline
value class ArgTypes(val argTypes: Array<out Class<*>>)

@Suppress("NOTHING_TO_INLINE")
inline fun args(vararg args: Any?) = Args(args)

@Suppress("NOTHING_TO_INLINE")
inline fun argTypes(vararg argTypes: Class<*>) = ArgTypes(argTypes)

/**
 * 扩展函数 用于遍历到指定或哪些 Field
 *  @param target 获得目标函数的 object
 *  @param fieldNames 指定遍历的 field 范围
 *  @param clazz 宿主的 classLoader
 *  @return 符合条件的 field
 */
fun getValueByFields(target: Any, fieldNames: List<String>, clazz: Class<*>? = null): Any? {
    var targetClass = clazz ?: target.javaClass
    while (targetClass != Any::class.java) {
        for (fieldName in fieldNames) {
            try {
                val field = targetClass.getDeclaredField(fieldName)
                field.isAccessible = true
                val value = field[target]
                if (value is Window) {
                    // Log.i("BlurPersonalAssistant Window field name: $fieldName")
                    return value
                }
            } catch (e: NoSuchFieldException) {
                // This field doesn't exist in this class, skip it
            } catch (e: IllegalAccessException) {
                // This field isn't accessible, skip it
            }
        }
        targetClass = targetClass.superclass ?: break
    }
    return null
}

/**
 * 扩展函数 通过类或者对象获取单个属性
 * @param fieldName 属性名
 * @param isStatic 是否静态类型
 * @param fieldType 属性类型
 * @return 符合条件的属性
 * @throws IllegalArgumentException 属性名为空
 * @throws NoSuchFieldException 未找到属性
 */
fun Any.field(
    fieldName: String,
    isStatic: Boolean = false,
    fieldType: Class<*>? = null
): Field {
    if (fieldName.isBlank()) throw IllegalArgumentException("Field name must not be empty!")
    var c: Class<*> = if (this is Class<*>) this else this.javaClass
    do {
        c.declaredFields
            .filter { isStatic == it.isStatic }
            .firstOrNull { (fieldType == null || it.type == fieldType) && (it.name == fieldName) }
            ?.let { it.isAccessible = true;return it }
    } while (c.superclass?.also { c = it } != null)
    throw NoSuchFieldException("Name: $fieldName,Static: $isStatic, Type: ${if (fieldType == null) "ignore" else fieldType.name}")
}

/**
 * 源自 EzXHelper 1.x 版本所附赠的扩展函数，2.0 丢失，暂时先复用
 *
 * 判断类是否相同(用于判断参数)
 * eg: fun foo(a: Boolean, b: Int) { }
 * foo.parameterTypes.sameAs(*array)
 * foo.parameterTypes.sameAs(Boolean::class.java, Int::class.java)
 * foo.parameterTypes.sameAs("boolean", "int")
 * foo.parameterTypes.sameAs(Boolean::class.java, "int")
 *
 * @param other 其他类(支持String或者Class<*>)
 * @return 是否相等
 */
fun Array<Class<*>>.sameAs(vararg other: Any): Boolean {
    if (this.size != other.size) return false
    for (i in this.indices) {
        when (val otherClazz = other[i]) {
            is Class<*> -> {
                if (this[i] != otherClazz) return false
            }

            is String -> {
                if (this[i].name != otherClazz) return false
            }

            else -> {
                throw IllegalArgumentException("Only support Class<*> or String")
            }
        }
    }
    return true
}


/**
 * 源自 EzXHelper 1.x 版本所附赠的扩展函数，2.0 丢失，暂时先复用
 *
 * 扩展函数 通过类或者对象获取单个方法
 * @param methodName 方法名
 * @param isStatic 是否为静态方法
 * @param returnType 方法返回值 填入null为无视返回值
 * @param argTypes 方法参数类型
 * @return 符合条件的方法
 * @throws IllegalArgumentException 方法名为空
 * @throws NoSuchMethodException 未找到方法
 */
fun Any.method(
    methodName: String,
    returnType: Class<*>? = null,
    isStatic: Boolean = false,
    argTypes: ArgTypes = argTypes()
): Method {
    if (methodName.isBlank()) throw IllegalArgumentException("Method name must not be empty!")
    var c = if (this is Class<*>) this else this.javaClass
    do {
        c.declaredMethods.toList().asSequence()
            .filter { it.name == methodName }
            .filter { it.parameterTypes.size == argTypes.argTypes.size }
            .apply { if (returnType != null) filter { returnType == it.returnType } }
            .filter { it.parameterTypes.sameAs(*argTypes.argTypes) }
            .filter { it.isStatic == isStatic }
            .firstOrNull()?.let { it.isAccessible = true; return it }
    } while (c.superclass?.also { c = it } != null)
    throw NoSuchMethodException("Name:$methodName, Static: $isStatic, ArgTypes:${argTypes.argTypes.joinToString(",")}")
}

/**
 * 源自 EzXHelper 1.x 版本所附赠的扩展函数，2.0 丢失，暂时先复用
 * 扩展函数 调用对象的方法
 *
 * @param methodName 方法名
 * @param args 形参表 可空
 * @param argTypes 形参类型 可空
 * @param returnType 返回值类型 为null时无视返回值类型
 * @return 函数调用后的返回值
 * @throws IllegalArgumentException 当方法名为空时
 * @throws IllegalArgumentException 当args的长度与argTypes的长度不符时
 * @throws IllegalArgumentException 当对象是一个Class时
 */
fun Any.invokeMethod(
    methodName: String,
    args: Args = args(),
    argTypes: ArgTypes = argTypes(),
    returnType: Class<*>? = null
): Any? {
    if (methodName.isBlank()) throw IllegalArgumentException("Object name must not be empty!")
    if (args.args.size != argTypes.argTypes.size) throw IllegalArgumentException("Method args size must equals argTypes size!")
    return if (args.args.isEmpty()) {
        try {
            this.method(methodName, returnType, false)
        } catch (e: NoSuchMethodException) {
            return null
        }.invoke(this)
    } else {
        try {
            this.method(methodName, returnType, false, argTypes = argTypes)
        } catch (e: NoSuchMethodException) {
            return null
        }.invoke(this, *args.args)
    }
}

/**
 * 检测设备是否加密，来源米客
 * @return 返回一个 Boolean 值，true 为已加密，false 为未加密
 * API 34 已弃用 DevicePolicyManager.ENCRYPTION_STATUS_ACTIVATING 属性值，官方说法如下
 * This result code has never actually been used, so there is no reason for apps to check for it.
 */
fun isDeviceEncrypted(context: Context): Boolean {
    val policyMgr = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val encryption = policyMgr.storageEncryptionStatus
    return encryption == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE ||
        encryption == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER
}

/**
 * 判断是否为新网速指示器
 * @return 返回一个 Boolean 值，true 为新布局，false 为旧布局
 */

fun isNewNetworkStyle(): Boolean {
    val networkSpeedViewCls = XposedHelpers.findClassIfExists(
        "com.android.systemui.statusbar.views.NetworkSpeedView", EzXHelper.classLoader
    )
    return if (networkSpeedViewCls != null) {
        LinearLayout::class.java.isAssignableFrom(networkSpeedViewCls)
    } else {
        false
    }
}

val Int.dp: Int get() = (this.toFloat().dp).toInt()
val Float.dp: Float get() = this / getSystem().displayMetrics.density