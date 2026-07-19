package io.github.lingqiqi5211.ezhooktool.xposed.internal

import java.util.Collections
import java.util.WeakHashMap

internal object AdditionalFields {
    private val instanceFields = Collections.synchronizedMap(WeakHashMap<Any, MutableMap<String, Any?>>())
    private val staticFields = Collections.synchronizedMap(WeakHashMap<Class<*>, MutableMap<String, Any?>>())

    private fun innerInstance(target: Any): MutableMap<String, Any?> = synchronized(instanceFields) {
        instanceFields.getOrPut(target) { Collections.synchronizedMap(HashMap()) }
    }

    private fun innerStatic(target: Class<*>): MutableMap<String, Any?> = synchronized(staticFields) {
        staticFields.getOrPut(target) { Collections.synchronizedMap(HashMap()) }
    }

    fun setInstance(target: Any, key: String, value: Any?): Any? = innerInstance(target).put(key, value)

    fun getInstance(target: Any, key: String): Any? {
        val map = synchronized(instanceFields) { instanceFields[target] } ?: return null
        return map[key]
    }

    fun removeInstance(target: Any, key: String): Any? {
        val map = synchronized(instanceFields) { instanceFields[target] } ?: return null
        return map.remove(key)
    }

    fun setStatic(target: Class<*>, key: String, value: Any?): Any? = innerStatic(target).put(key, value)

    fun getStatic(target: Class<*>, key: String): Any? {
        val map = synchronized(staticFields) { staticFields[target] } ?: return null
        return map[key]
    }

    fun removeStatic(target: Class<*>, key: String): Any? {
        val map = synchronized(staticFields) { staticFields[target] } ?: return null
        return map.remove(key)
    }
}
