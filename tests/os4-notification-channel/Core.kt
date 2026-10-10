package io.github.lingqiqi5211.ezhooktool.core
import java.lang.reflect.Method
class MethodSelector { var methodName = ""; fun name(value: String) { methodName=value } }
fun Class<*>.findMethodOrNull(block: MethodSelector.() -> Unit): Method? {
    val selector = MethodSelector().apply(block)
    return methods.singleOrNull { it.name == selector.methodName }
}
fun Class<*>.findMethod(block: MethodSelector.() -> Unit): Method = checkNotNull(findMethodOrNull(block))
