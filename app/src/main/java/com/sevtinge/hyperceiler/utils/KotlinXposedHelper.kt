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

  * Copyright (C) 2023-2025 HyperCeiler Contributions
*/
@file:Suppress("unused", "UNCHECKED_CAST")

package com.sevtinge.hyperceiler.utils

import android.annotation.SuppressLint
import android.content.res.XResources
import com.github.kyuubiran.ezxhelper.EzXHelper.classLoader
import com.github.kyuubiran.ezxhelper.Log
import dalvik.system.BaseDexClassLoader
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge.*
import de.robv.android.xposed.XposedHelpers.*
import de.robv.android.xposed.callbacks.XC_LayoutInflated
import java.lang.reflect.Field
import java.lang.reflect.Member
import java.lang.reflect.Modifier
import java.util.Enumeration

typealias MethodHookParam = MethodHookParam
typealias Replacer = (MethodHookParam) -> Any?
typealias Hooker = (MethodHookParam) -> Unit

fun Class<*>.hookMethod(method: String?, vararg args: Any?) = try {
    findAndHookMethod(this, method, *args)
} catch (e: NoSuchMethodError) {
    Log.e(e)
    null
} catch (e: ClassNotFoundError) {
    Log.e(e)
    null
} catch (e: ClassNotFoundException) {
    Log.e(e)
    null
}

fun Member.hookMethod(callback: XC_MethodHook) = try {
    hookMethod(this, callback)
} catch (e: Throwable) {
    Log.e(e)
    null
}

inline fun MethodHookParam.callHooker(crossinline hooker: Hooker) = try {
    hooker(this)
} catch (e: Throwable) {
    Log.e("Error occurred calling hooker on ${this.method}")
    Log.e(e)
}

inline fun MethodHookParam.callReplacer(crossinline replacer: Replacer) = try {
    replacer(this)
} catch (e: Throwable) {
    Log.e("Error occurred calling replacer on ${this.method}")
    Log.e(e)
    null
}

inline fun Member.replaceMethod(crossinline replacer: Replacer) =
    hookMethod(object : XC_MethodReplacement() {
        override fun replaceHookedMethod(param: MethodHookParam) = param.callReplacer(replacer)
    })

inline fun Member.hookAfterMethod(crossinline hooker: Hooker) =
    hookMethod(object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) = param.callHooker(hooker)
    })

inline fun Member.hookBeforeMethod(crossinline hooker: (MethodHookParam) -> Unit) =
    hookMethod(object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) = param.callHooker(hooker)
    })

inline fun Class<*>.hookBeforeMethod(
    method: String?, vararg args: Any?, crossinline hooker: Hooker
) = hookMethod(method, *args, object : XC_MethodHook() {
    override fun beforeHookedMethod(param: MethodHookParam) = param.callHooker(hooker)
})

inline fun Class<*>.hookAfterMethod(
    method: String?, vararg args: Any?, crossinline hooker: Hooker
) = hookMethod(method, *args, object : XC_MethodHook() {
    override fun afterHookedMethod(param: MethodHookParam) = param.callHooker(hooker)
})

inline fun Class<*>.replaceMethod(
    method: String?, vararg args: Any?, crossinline replacer: Replacer
) = hookMethod(method, *args, object : XC_MethodReplacement() {
    override fun replaceHookedMethod(param: MethodHookParam) = param.callReplacer(replacer)
})

fun Class<*>.hookAllMethods(methodName: String?, hooker: XC_MethodHook): Set<XC_MethodHook.Unhook> =
    try {
        hookAllMethods(this, methodName, hooker)
    } catch (e: NoSuchMethodError) {
        Log.e(e)
        emptySet()
    } catch (e: ClassNotFoundError) {
        Log.e(e)
        emptySet()
    } catch (e: ClassNotFoundException) {
        Log.e(e)
        emptySet()
    }

inline fun Class<*>.hookBeforeAllMethods(methodName: String?, crossinline hooker: Hooker) =
    hookAllMethods(methodName, object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) = param.callHooker(hooker)
    })

inline fun Class<*>.hookAfterAllMethods(methodName: String?, crossinline hooker: Hooker) =
    hookAllMethods(methodName, object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) = param.callHooker(hooker)

    })

inline fun Class<*>.replaceAllMethods(methodName: String?, crossinline replacer: Replacer) =
    hookAllMethods(methodName, object : XC_MethodReplacement() {
        override fun replaceHookedMethod(param: MethodHookParam) = param.callReplacer(replacer)
    })

fun Class<*>.hookConstructor(vararg args: Any?) = try {
    findAndHookConstructor(this, *args)
} catch (e: NoSuchMethodError) {
    Log.e(e)
    null
} catch (e: ClassNotFoundError) {
    Log.e(e)
    null
} catch (e: ClassNotFoundException) {
    Log.e(e)
    null
}

inline fun Class<*>.hookBeforeConstructor(vararg args: Any?, crossinline hooker: Hooker) =
    hookConstructor(*args, object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) = param.callHooker(hooker)
    })

inline fun Class<*>.hookAfterConstructor(vararg args: Any?, crossinline hooker: Hooker) =
    hookConstructor(*args, object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) = param.callHooker(hooker)
    })

inline fun Class<*>.replaceConstructor(vararg args: Any?, crossinline hooker: Hooker) =
    hookConstructor(*args, object : XC_MethodReplacement() {
        override fun replaceHookedMethod(param: MethodHookParam) = param.callHooker(hooker)
    })

fun Class<*>.hookAllConstructors(hooker: XC_MethodHook): Set<XC_MethodHook.Unhook> = try {
    hookAllConstructors(this, hooker)
} catch (e: NoSuchMethodError) {
    Log.e(e)
    emptySet()
} catch (e: ClassNotFoundError) {
    Log.e(e)
    emptySet()
} catch (e: ClassNotFoundException) {
    Log.e(e)
    emptySet()
}

inline fun Class<*>.hookAfterAllConstructors(crossinline hooker: Hooker) =
    hookAllConstructors(object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) = param.callHooker(hooker)
    })

inline fun Class<*>.hookBeforeAllConstructors(crossinline hooker: Hooker) =
    hookAllConstructors(object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) = param.callHooker(hooker)
    })

inline fun Class<*>.replaceAllConstructors(crossinline hooker: Hooker) =
    hookAllConstructors(object : XC_MethodReplacement() {
        override fun replaceHookedMethod(param: MethodHookParam) = param.callHooker(hooker)
    })

fun String.hookMethod(
    method: String?, vararg args: Any?
) = try {
    findClass().hookMethod(method, *args)
} catch (e: ClassNotFoundError) {
    Log.e(e)
    null
} catch (e: ClassNotFoundException) {
    Log.e(e)
    null
}

inline fun String.hookBeforeMethod(
    method: String?, vararg args: Any?, crossinline hooker: Hooker
) = try {
    findClass().hookBeforeMethod(method, *args, hooker = hooker)
} catch (e: ClassNotFoundError) {
    Log.e(e)
    null
} catch (e: ClassNotFoundException) {
    Log.e(e)
    null
}

inline fun String.hookAfterMethod(
    method: String?, vararg args: Any?, crossinline hooker: Hooker
) = try {
    findClass().hookAfterMethod(method, *args, hooker = hooker)
} catch (e: ClassNotFoundError) {
    Log.e(e)
    null
} catch (e: ClassNotFoundException) {
    Log.e(e)
    null
}

inline fun String.replaceMethod(
    method: String?, vararg args: Any?, crossinline replacer: Replacer
) = try {
    findClass().replaceMethod(method, *args, replacer = replacer)
} catch (e: ClassNotFoundError) {
    Log.e(e)
    null
} catch (e: ClassNotFoundException) {
    Log.e(e)
    null
}

fun MethodHookParam.invokeOriginalMethod(): Any? = invokeOriginalMethod(method, thisObject, args)

inline fun <T, R> T.runCatchingOrNull(func: T.() -> R?) = try {
    func()
} catch (e: Throwable) {
    null
}

fun Any.getAdditionalInstanceField(field: String): Any? = getAdditionalInstanceField(this, field)

fun <T> Any.getAdditionalInstanceFieldAs(field: String) = getAdditionalInstanceField(field) as T

fun Any.setAdditionalInstanceField(
    field: String,
    value: Any?
) = setAdditionalInstanceField(this, field, value)

fun Any.removeAdditionalInstanceField(
    field: String
): Any? = removeAdditionalInstanceField(this, field)

fun Any.getObjectField(field: String?): Any? = getObjectField(this, field)

fun Any.getObjectFieldOrNull(field: String?): Any? = runCatchingOrNull {
    getObjectField(this, field)
}

fun <T> Any.getObjectFieldAs(field: String?) = getObjectField(this, field) as T

fun <T> Any.getObjectFieldOrNullAs(field: String?) = runCatchingOrNull {
    getObjectField(this, field) as T
}

fun Any.getIntField(field: String?) = getIntField(this, field)

fun Any.getIntFieldOrNull(field: String?) = runCatchingOrNull {
    getIntField(this, field)
}

fun Any.getFloatField(field: String?) = getFloatField(this, field)

fun Any.getFloatFieldOrNull(field: String?) = runCatchingOrNull {
    getFloatField(this, field)
}

fun Any.getLongField(field: String?) = getLongField(this, field)

fun Any.getLongFieldOrNull(field: String?) = runCatchingOrNull {
    getLongField(this, field)
}

fun Any.getBooleanField(field: String?) = getBooleanField(this, field)

fun Any.getBooleanFieldOrNull(field: String?) = runCatchingOrNull {
    getBooleanField(this, field)
}

fun Any.callMethod(methodName: String?, vararg args: Any?): Any? =
    callMethod(this, methodName, *args)

fun Any.callMethodOrNull(methodName: String?, vararg args: Any?): Any? = runCatchingOrNull {
    callMethod(this, methodName, *args)
}

fun Class<*>.callStaticMethod(methodName: String?, vararg args: Any?): Any? =
    callStaticMethod(this, methodName, *args)

fun Class<*>.callStaticMethodOrNull(methodName: String?, vararg args: Any?): Any? =
    runCatchingOrNull {
        callStaticMethod(this, methodName, *args)
    }

fun <T> Class<*>.callStaticMethodAs(methodName: String?, vararg args: Any?) =
    callStaticMethod(this, methodName, *args) as T

fun <T> Class<*>.callStaticMethodOrNullAs(methodName: String?, vararg args: Any?) =
    runCatchingOrNull {
        callStaticMethod(this, methodName, *args) as T
    }

fun <T> Class<*>.getStaticObjectFieldAs(field: String?) = getStaticObjectField(this, field) as T

fun <T> Class<*>.getStaticObjectFieldOrNullAs(field: String?) = runCatchingOrNull {
    getStaticObjectField(this, field) as T
}

fun Class<*>.getStaticObjectField(field: String?): Any? = getStaticObjectField(this, field)

fun Class<*>.getStaticObjectFieldOrNull(field: String?): Any? = runCatchingOrNull {
    getStaticObjectField(this, field)
}

fun Class<*>.setStaticObjectField(field: String?, obj: Any?) = apply {
    setStaticObjectField(this, field, obj)
}

fun Class<*>.setStaticObjectFieldIfExist(field: String?, obj: Any?) = apply {
    try {
        setStaticObjectField(this, field, obj)
    } catch (ignored: Throwable) {
    }
}

fun getValueByField(target: Any, fieldName: String, clazz: Class<*>? = null): Any? {
    var targetClass = clazz
    if (targetClass == null) {
        targetClass = target.javaClass
    }
    return try {
        val field = targetClass.getDeclaredField(fieldName)
        field.isAccessible = true
        field.get(target)
    } catch (e: Throwable) {
        if (targetClass.superclass == null) {
            null
        } else {
            getValueByField(target, fieldName, targetClass.superclass)
        }
    }
}

inline fun <reified T> Class<*>.findFieldByExactType(): Field? =
    findFirstFieldByExactType(this, T::class.java)

fun Class<*>.findFieldByExactType(type: Class<*>): Field? = findFirstFieldByExactType(this, type)

fun <T> Any.callMethodAs(methodName: String?, vararg args: Any?) =
    callMethod(this, methodName, *args) as T

fun <T> Any.callMethodOrNullAs(methodName: String?, vararg args: Any?) = runCatchingOrNull {
    callMethod(this, methodName, *args) as T
}

fun Any.callMethod(methodName: String?, parameterTypes: Array<Class<*>>, vararg args: Any?): Any? =
    callMethod(this, methodName, parameterTypes, *args)

fun Any.callMethodOrNull(
    methodName: String?, parameterTypes: Array<Class<*>>, vararg args: Any?
): Any? = runCatchingOrNull {
    callMethod(this, methodName, parameterTypes, *args)
}

fun Class<*>.callStaticMethod(
    methodName: String?, parameterTypes: Array<Class<*>>, vararg args: Any?
): Any? = callStaticMethod(this, methodName, parameterTypes, *args)

fun Class<*>.callStaticMethodOrNull(
    methodName: String?, parameterTypes: Array<Class<*>>, vararg args: Any?
): Any? = runCatchingOrNull {
    callStaticMethod(this, methodName, parameterTypes, *args)
}

fun String.findClass(): Class<*> = findClass(this, classLoader)

fun String.findClassOrNull(): Class<*>? = findClassIfExists(this, classLoader)

fun Class<*>.new(vararg args: Any?): Any = newInstance(this, *args)

fun Class<*>.new(parameterTypes: Array<Class<*>>, vararg args: Any?): Any =
    newInstance(this, parameterTypes, *args)

fun Class<*>.findField(field: String?): Field = findField(this, field)

fun Class<*>.findFieldOrNull(field: String?): Field? = findFieldIfExists(this, field)

data class ResourcesHookData(val type: String, val afterValue: Any)

class ResourcesHookMap<String, ResourcesHookData> : HashMap<String, ResourcesHookData>() {
    fun isKeyExist(key: String): Boolean = getOrDefault(key, null) != null
}

fun <T> T.setIntField(field: String?, value: Int) = apply {
    setIntField(this, field, value)
}

fun <T> T.setLongField(field: String?, value: Long) = apply {
    setLongField(this, field, value)
}

fun <T> T.setFloatField(field: String?, value: Float) = apply {
    setFloatField(this, field, value)
}

fun <T> T.setBooleanField(field: String?, value: Boolean) = apply {
    setBooleanField(this, field, value)
}

fun <T> T.setObjectField(field: String?, value: Any?) = apply {
    setObjectField(this, field, value)
}

inline fun XResources.hookLayout(
    id: Int, crossinline hooker: (XC_LayoutInflated.LayoutInflatedParam) -> Unit
) {
    try {
        hookLayout(id, object : XC_LayoutInflated() {
            override fun handleLayoutInflated(liparam: LayoutInflatedParam) {
                try {
                    hooker(liparam)
                } catch (e: Throwable) {
                    Log.e(e)
                }
            }
        })
    } catch (e: Throwable) {
        Log.e(e)
    }
}

@SuppressLint("DiscouragedApi")
inline fun XResources.hookLayout(
    pkg: String,
    type: String,
    name: String,
    crossinline hooker: (XC_LayoutInflated.LayoutInflatedParam) -> Unit
) {
    try {
        val id = getIdentifier(name, type, pkg)
        hookLayout(id, hooker)
    } catch (e: Throwable) {
        Log.e(e)
    }
}

fun Class<*>.findFirstFieldByExactType(type: Class<*>): Field =
    findFirstFieldByExactType(this, type)

fun Class<*>.findFirstFieldByExactTypeOrNull(type: Class<*>?): Field? = runCatchingOrNull {
    findFirstFieldByExactType(this, type)
}

fun Any.getFirstFieldByExactType(type: Class<*>): Any? =
    javaClass.findFirstFieldByExactType(type).get(this)

fun <T> Any.getFirstFieldByExactTypeAs(type: Class<*>) =
    javaClass.findFirstFieldByExactType(type).get(this) as? T

inline fun <reified T : Any> Any.getFirstFieldByExactType() =
    javaClass.findFirstFieldByExactType(T::class.java).get(this) as? T

fun Any.getFirstFieldByExactTypeOrNull(type: Class<*>?): Any? = runCatchingOrNull {
    javaClass.findFirstFieldByExactTypeOrNull(type)?.get(this)
}

fun <T> Any.getFirstFieldByExactTypeOrNullAs(type: Class<*>?) =
    getFirstFieldByExactTypeOrNull(type) as? T

inline fun <reified T> Any.getFirstFieldByExactTypeOrNull() =
    getFirstFieldByExactTypeOrNull(T::class.java) as? T

inline fun ClassLoader.findDexClassLoader(crossinline delegator: (BaseDexClassLoader) -> BaseDexClassLoader = { x -> x }): BaseDexClassLoader? {
    var classLoader = this
    while (classLoader !is BaseDexClassLoader) {
        if (classLoader.parent != null) classLoader = classLoader.parent
        else return null
    }
    return delegator(classLoader)
}

inline fun ClassLoader.allClassesList(crossinline delegator: (BaseDexClassLoader) -> BaseDexClassLoader = { x -> x }): List<String> {
    return findDexClassLoader(delegator)?.getObjectField("pathList")
        ?.getObjectFieldAs<Array<Any>>("dexElements")
        ?.flatMap {
            it.getObjectField("dexFile")?.callMethodAs<Enumeration<String>>("entries")?.toList()
                .orEmpty()
        }.orEmpty()
}

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
