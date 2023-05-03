package com.sevtinge.cemiuiler.utils

import android.annotation.SuppressLint
import android.content.Context

object SystemProperties {
    @SuppressLint("PrivateApi")
    operator fun get(context: Context, key: String?): String {
        var ret: String
        try {
            val cl = context.classLoader
            val systemProperties = cl.loadClass("android.os.SystemProperties")
            //参数类型
            val paramTypes: Array<Class<*>?> = arrayOfNulls(1)
            paramTypes[0] = String::class.java
            val get = systemProperties.getMethod("get", *paramTypes)
            //参数
            val params = arrayOfNulls<Any>(1)
            params[0] = key
            ret = get.invoke(systemProperties, *params) as String
        } catch (iAE: IllegalArgumentException) {
            throw iAE
        } catch (e: Exception) {
            ret = ""
        }
        return ret
    }

}