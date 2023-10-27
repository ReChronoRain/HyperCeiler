package com.sevtinge.hyperceiler.module.hook.home.folder

import android.annotation.SuppressLint
import android.app.Activity
import android.view.MotionEvent
import android.view.View
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.*
import com.sevtinge.hyperceiler.utils.log.XposedLogUtils

object FolderBlur : BaseHook() {
    @SuppressLint("SuspiciousIndentation")
    override fun init() {
        // 修复文件夹背景模糊与始终模糊壁纸冲突
        if (mPrefsMap.getBoolean("home_other_always_blur_launcher_wallpaper")) return

        val folderInfo = findClassIfExists("com.miui.home.launcher.FolderInfo")
        val launcherClass = findClassIfExists("com.miui.home.launcher.Launcher")
        val blurUtilsClass = findClassIfExists("com.miui.home.launcher.common.BlurUtils")
        val navStubViewClass = findClassIfExists("com.miui.home.recents.NavStubView")
        val cancelShortcutMenuReasonClass =
            findClassIfExists("com.miui.home.launcher.shortcuts.CancelShortcutMenuReason")
        val applicationClass = findClassIfExists("com.miui.home.launcher.Application")
        try {
            launcherClass.hookBeforeMethod("isShouldBlur") {
                it.result = false
            }
            blurUtilsClass.hookBeforeMethod("fastBlurWhenOpenOrCloseFolder", launcherClass, Boolean::class.java) {
                it.result = null
            }
        } catch (e: Exception) {
            XposedLogUtils.logE(TAG, e)
        }
        var isShouldBlur = false


        launcherClass.hookAfterMethod("openFolder", folderInfo, View::class.java) {
            val mLauncher = applicationClass.callStaticMethod("getLauncher") as Activity
            val isInNormalEditing = mLauncher.callMethod("isInNormalEditing") as Boolean
            if (!isInNormalEditing)
                blurUtilsClass.callStaticMethod("fastBlur", 1.0f, mLauncher.window, true)
        }

        launcherClass.hookAfterMethod("isFolderShowing") {
            isShouldBlur = it.result as Boolean
        }

        launcherClass.hookAfterMethod("closeFolder", Boolean::class.java) {
            isShouldBlur = false
            val mLauncher = applicationClass.callStaticMethod("getLauncher") as Activity
            val isInNormalEditing = mLauncher.callMethod("isInNormalEditing") as Boolean
            if (isInNormalEditing)
                blurUtilsClass.callStaticMethod("fastBlur", 1.0f, mLauncher.window, true, 0L)
            else
                blurUtilsClass.callStaticMethod("fastBlur", 0.0f, mLauncher.window, true)
        }

        launcherClass.hookAfterMethod("cancelShortcutMenu", Int::class.java, cancelShortcutMenuReasonClass) {
            val mLauncher =
                applicationClass.callStaticMethod("getLauncher") as Activity
            if (isShouldBlur)
                blurUtilsClass.callStaticMethod("fastBlur", 1.0f, mLauncher.window, true, 0L)
        }

        launcherClass.hookBeforeMethod("onGesturePerformAppToHome") {
            val mLauncher = applicationClass.callStaticMethod("getLauncher") as Activity
            if (isShouldBlur) {
                blurUtilsClass.callStaticMethod("fastBlur", 1.0f, mLauncher.window, true, 0L)
            }
        }

        blurUtilsClass.hookBeforeAllMethods("fastBlurWhenStartOpenOrCloseApp") {
            val mLauncher = applicationClass.callStaticMethod("getLauncher") as Activity
            val isInEditing = mLauncher.callMethod("isInEditing") as Boolean
            if (isShouldBlur) it.result =
                blurUtilsClass.callStaticMethod("fastBlur", 1.0f, mLauncher.window, true, 0L)
            else if (isInEditing) it.result =
                blurUtilsClass.callStaticMethod("fastBlur", 1.0f, mLauncher.window, true, 0L)
        }

        blurUtilsClass.hookBeforeAllMethods("fastBlurWhenFinishOpenOrCloseApp") {
            val mLauncher = applicationClass.callStaticMethod("getLauncher") as Activity
            val isInEditing = mLauncher.callMethod("isInEditing") as Boolean
            if (isShouldBlur) it.result =
                blurUtilsClass.callStaticMethod("fastBlur", 1.0f, mLauncher.window, true, 0L)
            else if (isInEditing) it.result =
                blurUtilsClass.callStaticMethod("fastBlur", 1.0f, mLauncher.window, true, 0L)
        }

        blurUtilsClass.hookAfterAllMethods("fastBlurWhenEnterRecents") {
            it.args[0]?.callMethod("hideShortcutMenuWithoutAnim")
        }

        blurUtilsClass.hookAfterAllMethods("fastBlurWhenExitRecents") {
            val mLauncher = applicationClass.callStaticMethod("getLauncher") as Activity
            val isInEditing = mLauncher.callMethod("isInEditing") as Boolean
            if (isShouldBlur) it.result =
                blurUtilsClass.callStaticMethod("fastBlur", 1.0f, mLauncher.window, true, 0L)
            else if (isInEditing) it.result =
                blurUtilsClass.callStaticMethod("fastBlur", 1.0f, mLauncher.window, true, 0L)
        }

        blurUtilsClass.hookBeforeAllMethods("fastBlurDirectly") {
            val blurRatio = it.args[0] as Float
            if (isShouldBlur && blurRatio == 0.0f) it.result = null
        }


        if (((mPrefsMap.getStringAsInt("home_recent_blur_level", 6) == 0) && (mPrefsMap.getStringAsInt("home_recent_blur_level", 6) != 5)) ||
            (mPrefsMap.getStringAsInt("home_recent_blur_level", 6) != 0)
        ) {
            navStubViewClass.hookBeforeMethod("appTouchResolution", MotionEvent::class.java) {
                val mLauncher = applicationClass.callStaticMethod("getLauncher") as Activity
                if (isShouldBlur) {
                    blurUtilsClass.callStaticMethod("fastBlurDirectly", 1.0f, mLauncher.window)
                }
            }
        }

    }

}



