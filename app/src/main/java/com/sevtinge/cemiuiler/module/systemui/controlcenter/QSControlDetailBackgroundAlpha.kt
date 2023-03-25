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
        val qsControlDetailBackgroundAlpha = mPrefsMap.getInt("system_ui_control_center_control_detail_background_alpha", 255)
        val QSControlDetailClass = findClassIfExists(
            "com.android.systemui.controlcenter.phone.detail.QSControlDetail"
        )
        if(QSControlDetailClass != null){
            XposedHelpers.findAndHookMethod(
                QSControlDetailClass,
                "updateBackground",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val mDetailContainer = HookUtils.getValueByField(param.thisObject,"mDetailContainer") as View
                        if(mDetailContainer.background != null){
                            val smoothRoundDrawable = mDetailContainer.background
                            smoothRoundDrawable.alpha = qsControlDetailBackgroundAlpha
                        }
                    }
                })
        }
        val ModalQSControlDetailClass = findClassIfExists(
            "com.android.systemui.statusbar.notification.modal.ModalQSControlDetail"
        )
        if(ModalQSControlDetailClass != null){
            XposedHelpers.findAndHookMethod(
                ModalQSControlDetailClass,
                "updateBackground",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val mDetailContainer = HookUtils.getValueByField(param.thisObject,"mDetailContainer") as View
                        if(mDetailContainer.background != null){
                            val smoothRoundDrawable = mDetailContainer.background
                            smoothRoundDrawable.alpha = qsControlDetailBackgroundAlpha
                        }
                    }
                })
        }

        hookClassInPlugin{classLoader ->
            try {
                val SmoothRoundDrawableClass = XposedHelpers.callMethod(
                    classLoader,
                    "loadClass",
                    "miui.systemui.widget.SmoothRoundDrawable"
                ) ?: return@hookClassInPlugin
                XposedBridge.hookAllMethods(
                    SmoothRoundDrawableClass as Class<*>,
                    "inflate",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            try {
                                val currentDrawable = param.thisObject as Drawable
                                currentDrawable.alpha = qsControlDetailBackgroundAlpha
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