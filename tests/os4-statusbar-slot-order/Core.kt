package io.github.lingqiqi5211.ezhooktool.core
import java.lang.reflect.Method
import fixture.Factory
import fixture.IconList
class Query { var methodName = ""; fun name(value: String) { methodName = value } }
fun loadClass(name: String): Class<*> = when (name) {
    "com.android.systemui.statusbar.dagger.CentralSurfacesDependenciesModule_ProvideStatusBarIconListFactory" -> Factory::class.java
    "com.android.systemui.statusbar.phone.ui.StatusBarIconList" -> IconList::class.java
    else -> error("Unexpected target: $name")
}
fun Class<*>.findMethod(block: Query.() -> Unit): Method {
    val query = Query().apply(block)
    return declaredMethods.single { it.name == query.methodName }
}
