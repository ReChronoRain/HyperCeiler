package com.sevtinge.hyperceiler.module.hook.systemui

import com.github.kyuubiran.ezxhelper.*
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.MobileTypeSingle2Hook.findClass
import com.sevtinge.hyperceiler.utils.*

@Suppress("MemberVisibilityCanBePrivate")
object Dependency {
    private const val DEPENDENCY = "com.android.systemui.Dependency"
    val sDependency by lazy {
        findClass(DEPENDENCY, EzXHelper.classLoader).getStaticObjectField("sDependency")
    }
    val mMiuiLegacyDependency : Any?
        get() = sDependency?.getObjectField("mMiuiLegacyDependency")
    val mDependencies : Map<*, *>?
        get() = sDependency?.getObjectField("mDependencies") as Map<*, *>?

    fun getDependencyInner(depClz: Class<*>): Any? {
        return sDependency?.callMethod("getDependencyInner", depClz)
    }

    fun getDependencyInner(depClzName: String): Any? {
        return getDependencyInner(findClass(depClzName, EzXHelper.classLoader))
    }
}