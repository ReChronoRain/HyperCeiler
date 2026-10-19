package io.github.lingqiqi5211.ezhooktool.xposed.dsl
import java.lang.reflect.Method
class HookParam(val thisObject: Any, val args: Array<Any?> = emptyArray()) {
    var intercepted = false
    var result: Any? = "original-result"
        set(value) { field=value; intercepted=true }
}
class HookBuilder {
    var beforeCallback: ((HookParam) -> Unit)? = null
    var afterCallback: ((HookParam) -> Unit)? = null
    fun before(callback: (HookParam) -> Unit) { beforeCallback=callback }
    fun after(callback: (HookParam) -> Unit) { afterCallback=callback }
}
object Hooks {
    val registered = linkedMapOf<Method, HookBuilder>()
    fun hook(name: String) = registered.entries.single { it.key.name == name }.value
}
fun Method.createHook(block: HookBuilder.() -> Unit) { Hooks.registered[this]=HookBuilder().apply(block) }
fun Any.getObjectFieldOrNull(name: String): Any? = runCatching { javaClass.getField(name).get(this) }.getOrNull()
inline fun <reified T> Any.getObjectFieldOrNullAs(name: String): T? = getObjectFieldOrNull(name) as? T
