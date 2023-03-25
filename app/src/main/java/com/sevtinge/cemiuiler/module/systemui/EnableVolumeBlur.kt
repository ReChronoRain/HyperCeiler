package com.sevtinge.cemiuiler.module.systemui

import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.HookUtils
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

class EnableVolumeBlur : BaseHook() {
    override fun init() {
        hookClassInPlugin { classLoader ->
            try {
                val VolumeUtilClass = XposedHelpers.callMethod(
                    classLoader,
                    "loadClass",
                    "com.android.systemui.miui.volume.Util"
                ) ?: return@hookClassInPlugin
                VolumeUtilClass as Class<*>
                val allVolumeUtilMethods = VolumeUtilClass.methods
                if (allVolumeUtilMethods.isEmpty()) {
                    return@hookClassInPlugin
                }
                allVolumeUtilMethods.forEach { method ->
                    if (method.name == "isSupportBlurS") {
                        XposedBridge.hookAllMethods(
                            VolumeUtilClass,
                            "isSupportBlurS",
                            object : XC_MethodHook() {
                                override fun afterHookedMethod(param: MethodHookParam) {
                                    try {
                                        if (param.result is Boolean) {
                                            param.result = true
                                        }
                                    } catch (e: Throwable) {
                                        // Do Nothings.
                                        HookUtils.log(e.message)
                                    }
                                }
                            })
                        return@hookClassInPlugin
                    }
                }
            } catch (e: Throwable) {
                // Do Nothings.
                HookUtils.log(e.message)
            }
        }
    }

    fun hookClassInPlugin(afterGetClassLoader: (classLoader: ClassLoader) -> Unit) {
        val PluginHandlerClass = findClassIfExists(
            "com.android.systemui.shared.plugins.PluginInstanceManager\$PluginHandler"
        )
        if (PluginHandlerClass != null) {
            XposedBridge.hookAllMethods(
                PluginHandlerClass,
                "handleLoadPlugin",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val componentName = param.args[0]
                        val className =
                            XposedHelpers.callMethod(componentName, "getClassName") as String
                        if (className != "miui.systemui.volume.VolumeDialogPlugin") {
                            return
                        }
                        try {
                            val pluginContextWrapper =
                                HookUtils.getValueByField(param.result ?: return, "mPluginContext") ?: return
                            val classLoader = XposedHelpers.callMethod(
                                pluginContextWrapper,
                                "getClassLoader"
                            ) as ClassLoader
                            afterGetClassLoader(classLoader)
                        } catch (e: Throwable) {
                            // Do Nothings.
                            HookUtils.log(e.message)
                        }
                    }
                })
            return
        }

        val PluginActionManagerClass = findClassIfExists(
            "com.android.systemui.shared.plugins.PluginActionManager"
        )
        if (PluginActionManagerClass != null) {
            XposedBridge.hookAllMethods(
                PluginActionManagerClass,
                "loadPluginComponent",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val componentName = param.args[0]
                        val className =
                            XposedHelpers.callMethod(componentName, "getClassName") as String
                        if (className != "miui.systemui.volume.VolumeDialogPlugin") {
                            return
                        }
                        try {
                            val pluginContextWrapper =
                                HookUtils.getValueByField(param.result ?: return, "mPluginContext")
                                    ?: return
                            val classLoader = XposedHelpers.callMethod(
                                pluginContextWrapper,
                                "getClassLoader"
                            ) as ClassLoader
                            afterGetClassLoader(classLoader)
                        } catch (e: Throwable) {
                            // Do Nothings.
                            HookUtils.log(e.message)
                        }
                    }
                })
            return
        }
    }
}