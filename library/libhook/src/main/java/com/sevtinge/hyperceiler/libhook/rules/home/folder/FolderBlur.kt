/*
  * This file is part of HyperCeiler.

  * HyperCeiler is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation, either version 3 of the
  * License.

  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.

  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <https://www.gnu.org/licenses/>.

  * Copyright (C) 2023-2026 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.libhook.rules.home.folder

import android.annotation.SuppressLint
import android.app.Activity
import android.view.MotionEvent
import android.view.View
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.afterHookMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.beforeHookMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.callMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.callStaticMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.hookAllMethods
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge

object FolderBlur : BaseHook() {
    @SuppressLint("SuspiciousIndentation")
    override fun init() {
        // 修复文件夹背景模糊与始终模糊壁纸冲突
        if (PrefsBridge.getBoolean("home_other_always_blur_launcher_wallpaper")) return

        val folderInfo = findClassIfExists("com.miui.home.launcher.FolderInfo")
        val launcherClass = findClassIfExists("com.miui.home.launcher.Launcher")
        val blurUtilsClass = findClassIfExists("com.miui.home.launcher.common.BlurUtils")
        val navStubViewClass = findClassIfExists("com.miui.home.recents.NavStubView")
        val cancelShortcutMenuReasonClass =
            findClassIfExists("com.miui.home.launcher.shortcuts.CancelShortcutMenuReason")
        val applicationClass = findClassIfExists("com.miui.home.launcher.Application")
        try {
            launcherClass.beforeHookMethod("isShouldBlur") {
                it.result = false
            }
            blurUtilsClass.beforeHookMethod("fastBlurWhenOpenOrCloseFolder", launcherClass, Boolean::class.java) {
                it.result = null
            }
        } catch (e: Exception) {
            XposedLog.e(TAG, this.lpparam.packageName, e)
        }
        var isShouldBlur = false


        launcherClass.afterHookMethod("openFolder", folderInfo, View::class.java) {
            val mLauncher = applicationClass.callStaticMethod("getLauncher") as Activity
            val isInNormalEditing = mLauncher.callMethod("isInNormalEditing") as Boolean
            if (!isInNormalEditing)
                blurUtilsClass.callStaticMethod("fastBlur", 1.0f, mLauncher.window, true)
        }

        launcherClass.afterHookMethod("isFolderShowing") {
            isShouldBlur = it.result as Boolean
        }

        launcherClass.afterHookMethod("closeFolder", Boolean::class.java) {
            isShouldBlur = false
            val mLauncher = applicationClass.callStaticMethod("getLauncher") as Activity
            val isInNormalEditing = mLauncher.callMethod("isInNormalEditing") as Boolean
            if (isInNormalEditing)
                blurUtilsClass.callStaticMethod("fastBlur", 1.0f, mLauncher.window, true, 0L)
            else
                blurUtilsClass.callStaticMethod("fastBlur", 0.0f, mLauncher.window, true)
        }

        launcherClass.afterHookMethod("cancelShortcutMenu", Int::class.java, cancelShortcutMenuReasonClass) {
            val mLauncher =
                applicationClass.callStaticMethod("getLauncher") as Activity
            if (isShouldBlur)
                blurUtilsClass.callStaticMethod("fastBlur", 1.0f, mLauncher.window, true, 0L)
        }

        launcherClass.beforeHookMethod("onGesturePerformAppToHome") {
            val mLauncher = applicationClass.callStaticMethod("getLauncher") as Activity
            if (isShouldBlur) {
                blurUtilsClass.callStaticMethod("fastBlur", 1.0f, mLauncher.window, true, 0L)
            }
        }

        blurUtilsClass.hookAllMethods("fastBlurWhenStartOpenOrCloseApp") {
            before {
                val mLauncher = applicationClass.callStaticMethod("getLauncher") as Activity
                val isInEditing = mLauncher.callMethod("isInEditing") as Boolean
                if (isShouldBlur) it.result =
                    blurUtilsClass.callStaticMethod("fastBlur", 1.0f, mLauncher.window, true, 0L)
                else if (isInEditing) it.result =
                    blurUtilsClass.callStaticMethod("fastBlur", 1.0f, mLauncher.window, true, 0L)
            }
        }

        blurUtilsClass.hookAllMethods("fastBlurWhenFinishOpenOrCloseApp") {
            before {
                val mLauncher = applicationClass.callStaticMethod("getLauncher") as Activity
                val isInEditing = mLauncher.callMethod("isInEditing") as Boolean
                if (isShouldBlur) it.result =
                    blurUtilsClass.callStaticMethod("fastBlur", 1.0f, mLauncher.window, true, 0L)
                else if (isInEditing) it.result =
                    blurUtilsClass.callStaticMethod("fastBlur", 1.0f, mLauncher.window, true, 0L)
            }
        }

        blurUtilsClass.hookAllMethods("fastBlurWhenEnterRecents") {
            after {
                it.args[0]?.callMethod("hideShortcutMenuWithoutAnim")
            }
        }

        blurUtilsClass.hookAllMethods("fastBlurWhenExitRecents") {
            after {
                val mLauncher = applicationClass.callStaticMethod("getLauncher") as Activity
                val isInEditing = mLauncher.callMethod("isInEditing") as Boolean
                if (isShouldBlur) it.result =
                    blurUtilsClass.callStaticMethod("fastBlur", 1.0f, mLauncher.window, true, 0L)
                else if (isInEditing) it.result =
                    blurUtilsClass.callStaticMethod("fastBlur", 1.0f, mLauncher.window, true, 0L)
            }
        }

        blurUtilsClass.hookAllMethods("fastBlurDirectly") {
            before {
                val blurRatio = it.args[0] as Float
                if (isShouldBlur && blurRatio == 0.0f) it.result = null
            }
        }

        if (((PrefsBridge.getStringAsInt("home_recent_blur_level", 6) == 0) && (PrefsBridge.getStringAsInt("home_recent_blur_level", 6) != 5)) ||
            (PrefsBridge.getStringAsInt("home_recent_blur_level", 6) != 0)
        ) {
            navStubViewClass.beforeHookMethod("appTouchResolution", MotionEvent::class.java) {
                val mLauncher = applicationClass.callStaticMethod("getLauncher") as Activity
                if (isShouldBlur) {
                    blurUtilsClass.callStaticMethod("fastBlurDirectly", 1.0f, mLauncher.window)
                }
            }
        }

    }

}
