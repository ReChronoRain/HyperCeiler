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

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.widget.LinearLayout
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.MemberExtensions.isStatic
import de.robv.android.xposed.XposedHelpers
import java.lang.reflect.Method


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
