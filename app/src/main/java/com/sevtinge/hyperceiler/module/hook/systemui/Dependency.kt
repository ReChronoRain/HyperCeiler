package com.sevtinge.hyperceiler.module.hook.systemui

import com.github.kyuubiran.ezxhelper.*
import com.sevtinge.hyperceiler.utils.*

import de.robv.android.xposed.XposedHelpers.*

@Suppress("MemberVisibilityCanBePrivate")
object Dependency {
    private const val DEPENDENCY = "com.android.systemui.Dependency"

    private val dependencyClz by lazy {
        findClass(DEPENDENCY, EzXHelper.classLoader)
    }

    /* ========================== only for HyperOS2 ========================== */
    @JvmStatic
    @get:JvmName(name = "getDependency")
    val sDependency: Any?
        get() = dependencyClz.getStaticObjectField("sDependency")

    @JvmStatic
    @get:JvmName(name = "getMiuiLegacyDependency")
    val mMiuiLegacyDependency: Any?
        get() = sDependency?.getObjectField("mMiuiLegacyDependency")

    @JvmStatic
    @get:JvmName(name = "getDependencies")
    val mDependencies: Map<*, *>?
        get() = sDependency?.getObjectField("mDependencies") as Map<*, *>?

    @JvmStatic
    fun getDependencyInner(depClz: Class<*>): Any? {
        return sDependency?.callMethod("getDependencyInner", depClz)
    }

    @JvmStatic
    fun getDependencyInner(depClzName: String): Any? {
        return getDependencyInner(findClass(depClzName, EzXHelper.classLoader))
    }

    /* ========================== only for HyperOS1 ========================== */
    @JvmStatic
    fun get(depClz: Class<*>): Any? {
        return dependencyClz.callStaticMethod("get", depClz)
    }
}