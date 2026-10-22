package io.github.lingqiqi5211.ezhooktool.xposed.dsl

import java.lang.reflect.Method

data class HookParam(val thisObject: Any, val args: Array<Any?>)
object RegisteredHooks {
    val callbacks = linkedMapOf<Method, (HookParam) -> Unit>()
    fun invoke(name: String, count: Int, receiver: Any, vararg args: Any?) {
        callbacks.entries.single { it.key.name == name && it.key.parameterCount == count }
            .value(HookParam(receiver, arrayOf(*args)))
    }
}
fun Method.createAfterHook(callback: (HookParam) -> Unit) {
    RegisteredHooks.callbacks[this] = callback
}
class HookBuilder {
    lateinit var callback: (HookParam) -> Unit
    fun after(body: (HookParam) -> Unit) { callback = body }
}
fun List<Method>.createHooks(block: HookBuilder.() -> Unit) {
    val builder = HookBuilder().apply(block)
    forEach { it.createAfterHook(builder.callback) }
}
fun Any.getObjectField(name: String): Any? = javaClass.getField(name).get(this)
fun Any.getObjectFieldOrNull(name: String): Any? = runCatching { getObjectField(name) }.getOrNull()
fun Any.getBooleanField(name: String): Boolean = getObjectField(name) as Boolean
fun Any.getBooleanFieldOrNull(name: String): Boolean? = getObjectFieldOrNull(name) as? Boolean
