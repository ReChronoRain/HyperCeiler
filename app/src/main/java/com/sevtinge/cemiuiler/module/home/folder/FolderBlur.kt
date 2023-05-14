package com.sevtinge.cemiuiler.module.home.folder

import android.app.Activity
import android.os.Bundle
import android.view.View
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.*
import android.app.Application
import android.content.Context
import com.github.kyuubiran.ezxhelper.init.EzXHelperInit


object FolderBlur : BaseHook() {
    override fun init() {
        Application::class.java.hookBeforeMethod("attach", Context::class.java) { it ->
            EzXHelperInit.initHandleLoadPackage(lpparam)
            EzXHelperInit.setLogTag(TAG)
            EzXHelperInit.setToastTag(TAG)
            EzXHelperInit.initAppContext(it.args[0] as Context)


            if (mPrefsMap.getStringAsInt(
                    "home_recent_blur_level", 0
                ) == 4 || !mPrefsMap.getBoolean("home_folder_blur")
            ) {
                if (isAlpha()) {
                    "com.miui.home.launcher.common.BlurUtils".hookBeforeMethod("isUserBlurWhenOpenFolder") {
                        it.result = false
                    }
                }
            } else {
                if (isAlpha()) {
                    "com.miui.home.launcher.common.BlurUtils".hookBeforeMethod("isUserBlurWhenOpenFolder") {
                        it.result = true
                    }
                } else {
                    val blurClass = "com.miui.home.launcher.common.BlurUtils".findClass()
                    val folderInfo = "com.miui.home.launcher.FolderInfo".findClass()
                    val launcherClass = "com.miui.home.launcher.Launcher".findClass()
                    val launcherStateClass = "com.miui.home.launcher.LauncherState".findClass()
                    val cancelShortcutMenuReasonClass =
                        "com.miui.home.launcher.shortcuts.CancelShortcutMenuReason".findClass()
                    launcherClass.hookAfterMethod("onCreate", Bundle::class.java) {
                        val activity = it.thisObject as Activity
                        var isFolderShowing = false
                        var isShowEditPanel = false
                        launcherClass.hookAfterMethod("isFolderShowing") { hookParam ->
                            isFolderShowing = hookParam.result as Boolean
                        }
                        launcherClass.hookAfterMethod("showEditPanel", Boolean::class.java) { hookParam ->
                            isShowEditPanel = hookParam.args[0] as Boolean
                        }
                        launcherClass.hookAfterMethod("openFolder", folderInfo, View::class.java) {
                            blurClass.callStaticMethod("fastBlur", 1.0f, activity.window, true)
                        }
                        launcherClass.hookAfterMethod("closeFolder", Boolean::class.java) {
                            if (isShowEditPanel) blurClass.callStaticMethod("fastBlur", 1.0f, activity.window, true, 0L)
                            else blurClass.callStaticMethod("fastBlur", 0.0f, activity.window, true, 300L)
                        }
                        launcherClass.hookAfterMethod(
                            "cancelShortcutMenu",
                            Int::class.java,
                            cancelShortcutMenuReasonClass
                        ) {
                            if (isFolderShowing) blurClass.callStaticMethod("fastBlur", 1.0f, activity.window, true, 0L)
                        }
                        blurClass.hookAfterMethod(
                            "fastBlurWhenStartOpenOrCloseApp", Boolean::class.java, launcherClass
                        ) { hookParam ->
                            if (isFolderShowing) hookParam.result =
                                blurClass.callStaticMethod("fastBlur", 1.0f, activity.window, true, 0L)
                            else if (isShowEditPanel) hookParam.result =
                                blurClass.callStaticMethod("fastBlur", 1.0f, activity.window, true, 0L)
                        }
                        blurClass.hookAfterMethod(
                            "fastBlurWhenFinishOpenOrCloseApp", launcherClass
                        ) { hookParam ->
                            if (isFolderShowing) hookParam.result =
                                blurClass.callStaticMethod("fastBlur", 1.0f, activity.window, true, 0L)
                            else if (isShowEditPanel) hookParam.result =
                                blurClass.callStaticMethod("fastBlur", 1.0f, activity.window, true, 0L)
                        }
                        blurClass.hookAfterMethod(
                            "fastBlurWhenExitRecents", launcherClass, launcherStateClass, Boolean::class.java
                        ) { hookParam ->
                            if (isFolderShowing) hookParam.result =
                                blurClass.callStaticMethod("fastBlur", 1.0f, activity.window, true, 0L)
                            else if (isShowEditPanel) hookParam.result =
                                blurClass.callStaticMethod("fastBlur", 1.0f, activity.window, true, 0L)
                        }
                        launcherClass.hookAfterMethod("onGesturePerformAppToHome") {
                            if (isFolderShowing) blurClass.callStaticMethod(
                                "fastBlur",
                                1.0f,
                                activity.window,
                                true,
                                300L
                            )
                        }
                    }
                }

            }

        }

    }

}

