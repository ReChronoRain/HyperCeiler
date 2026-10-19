package io.github.lingqiqi5211.ezhooktool.core

import java.lang.reflect.Method
import com.sevtinge.hyperceiler.tests.notification.HeadsUpEntry
import com.sevtinge.hyperceiler.tests.notification.Presenter

fun loadClass(name: String): Class<*> = when (name) {
    "com.android.systemui.statusbar.notification.headsup.HeadsUpManagerImpl\$HeadsUpEntry" -> HeadsUpEntry::class.java
    "com.android.systemui.statusbar.phone.StatusBarNotificationPresenter" -> Presenter::class.java
    else -> error("Unexpected class lookup: $name")
}
class MethodSelector {
    var requestedName = ""
    fun name(value: String) { requestedName = value }
}
fun Class<*>.findAllMethods(select: MethodSelector.() -> Unit): List<Method> {
    val selector = MethodSelector().apply(select)
    return declaredMethods.filter { it.name == selector.requestedName }
}
fun Class<*>.findMethod(select: MethodSelector.() -> Unit): Method = findAllMethods(select).single()
fun Any.callMethod(name: String, vararg args: Any?): Any? = javaClass.methods
    .single { it.name == name && it.parameterCount == args.size }
    .invoke(this, *args)
fun Any.callMethodOrNull(name: String, vararg args: Any?): Any? =
    runCatching { callMethod(name, *args) }.getOrNull()
