package io.github.lingqiqi5211.ezhooktool.xposed.dsl
import java.lang.reflect.Method
class Param(var result: Any?, val thisObject: Any, val args: Array<Any?>)
object Hooks {
    var factory: ((Param) -> Unit)? = null
    var legacy: ((Param) -> Unit)? = null
}
fun Method.createAfterHook(block: (Param) -> Unit) { Hooks.factory = block }
fun Class<*>.beforeHookConstructor(type: Class<*>, block: (Param) -> Unit) {
    check(type == Array<String>::class.java)
    Hooks.legacy = block
}
fun Any.getObjectFieldOrNull(name: String): Any? = try {
    javaClass.getDeclaredField(name).apply { isAccessible = true }.get(this)
} catch (_: NoSuchFieldException) { null }
