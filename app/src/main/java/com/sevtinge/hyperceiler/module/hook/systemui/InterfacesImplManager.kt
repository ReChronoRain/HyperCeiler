package com.sevtinge.hyperceiler.module.hook.systemui

import com.github.kyuubiran.ezxhelper.*
import com.sevtinge.hyperceiler.utils.*
import de.robv.android.xposed.XposedHelpers.*

/**
 * only for HyperOS2
 */
object InterfacesImplManager {
    private const val IMPL_MANAGER = "com.miui.systemui.interfacesmanager.InterfacesImplManager"

    const val I_ACTIVITY_STARTER = "com.android.systemui.plugins.ActivityStarter"

    @JvmStatic
    @get:JvmName(name = "getClassContainer")
    val sClassContainer by lazy {
        managerClz.getStaticObjectFieldAs<Map<Class<*>, Any>>("sClassContainer")
    }

    private val managerClz by lazy {
        findClass(IMPL_MANAGER, EzXHelper.classLoader)
    }

    @JvmStatic
    fun registerImpl(clz: Class<*>, obj: Any) {
        managerClz.callStaticMethod("registerImpl", clz, obj)
    }
}