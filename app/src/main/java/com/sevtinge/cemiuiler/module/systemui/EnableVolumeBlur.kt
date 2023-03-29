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
                val volumeUtilClass = XposedHelpers.callMethod(
                    classLoader,
                    "loadClass",
                    "com.android.systemui.miui.volume.Util"
                ) ?: return@hookClassInPlugin
                volumeUtilClass as Class<*>
                val allVolumeUtilMethods = volumeUtilClass.methods
                if (allVolumeUtilMethods.isEmpty()) {
                    return@hookClassInPlugin
                }
                allVolumeUtilMethods.forEach { method ->
                    if (method.name == "isSupportBlurS") {
                        XposedBridge.hookAllMethods(
                            volumeUtilClass,
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

    private fun hookClassInPlugin(afterGetClassLoader: (classLoader: ClassLoader) -> Unit) {
        val pluginHandlerClass = findClassIfExists(
            "com.android.systemui.shared.plugins.PluginInstanceManager\$PluginHandler"
        )
        if (pluginHandlerClass != null) {
            XposedBridge.hookAllMethods(
                pluginHandlerClass,
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

        val pluginActionManagerClass = findClassIfExists(
            "com.android.systemui.shared.plugins.PluginActionManager"
        )
        if (pluginActionManagerClass != null) {
            XposedBridge.hookAllMethods(
                pluginActionManagerClass,
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