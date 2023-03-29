package com.sevtinge.cemiuiler.module.systemui.controlcenter

import android.graphics.drawable.Drawable
import android.view.View
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.HookUtils
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

object QSControlDetailBackgroundAlpha : BaseHook() {
    override fun init() {
        val qSControlDetailBackgroundAlpha = mPrefsMap.getInt("system_ui_control_center_control_detail_background_alpha", 255)
        val qSControlDetailClass = findClassIfExists(
            "com.android.systemui.controlcenter.phone.detail.QSControlDetail"
        )
        if(qSControlDetailClass != null){
            XposedHelpers.findAndHookMethod(
                qSControlDetailClass,
                "updateBackground",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val mDetailContainer = HookUtils.getValueByField(param.thisObject,"mDetailContainer") as View
                        if(mDetailContainer.background != null){
                            val smoothRoundDrawable = mDetailContainer.background
                            smoothRoundDrawable.alpha = qSControlDetailBackgroundAlpha
                        }
                    }
                })
        }
        val modalQSControlDetailClass = findClassIfExists(
            "com.android.systemui.statusbar.notification.modal.ModalQSControlDetail"
        )
        if(modalQSControlDetailClass != null){
            XposedHelpers.findAndHookMethod(
                modalQSControlDetailClass,
                "updateBackground",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val mDetailContainer = HookUtils.getValueByField(param.thisObject,"mDetailContainer") as View
                        if(mDetailContainer.background != null){
                            val smoothRoundDrawable = mDetailContainer.background
                            smoothRoundDrawable.alpha = qSControlDetailBackgroundAlpha
                        }
                    }
                })
        }

        hookClassInPlugin{classLoader ->
            try {
                val smoothRoundDrawableClass = XposedHelpers.callMethod(
                    classLoader,
                    "loadClass",
                    "miui.systemui.widget.SmoothRoundDrawable"
                ) ?: return@hookClassInPlugin
                XposedBridge.hookAllMethods(
                    smoothRoundDrawableClass as Class<*>,
                    "inflate",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            try {
                                val currentDrawable = param.thisObject as Drawable
                                currentDrawable.alpha = qSControlDetailBackgroundAlpha
                            } catch (e: Throwable) {
                                // Do Nothings.
                                HookUtils.log(e.message)
                            }
                        }
                    })
            } catch (e: Throwable) {
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