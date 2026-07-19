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
import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.callStaticMethod
import io.github.lingqiqi5211.ezhooktool.core.findAllMethods
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHooks
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHooks

object FolderBlur : BaseHook() {
    @SuppressLint("SuspiciousIndentation")
    override fun init() {
        // 修复文件夹背景模糊与始终模糊壁纸冲突
        if (PrefsBridge.getBoolean("home_other_always_blur_launcher_wallpaper")) return

        val folderInfo = findClassIfExists("com.miui.home.launcher.FolderInfo") ?: return
        val launcherClass = findClassIfExists("com.miui.home.launcher.Launcher") ?: return
        val blurUtilsClass = findClassIfExists("com.miui.home.launcher.common.BlurUtils") ?: return
        val navStubViewClass = findClassIfExists("com.miui.home.recents.NavStubView") ?: return
        val cancelShortcutMenuReasonClass =
            findClassIfExists("com.miui.home.launcher.shortcuts.CancelShortcutMenuReason") ?: return
        val applicationClass = findClassIfExists("com.miui.home.launcher.Application") ?: return
        try {
            launcherClass.findMethod {
                name("isShouldBlur")
            }.createBeforeHook {
                it.result = false
            }
            blurUtilsClass.findMethod {
                name("fastBlurWhenOpenOrCloseFolder")
                parameterTypes(launcherClass, Boolean::class.java)
            }.createBeforeHook {
                it.result = null
            }
        } catch (e: Exception) {
            XposedLog.e(TAG, this.lpparam.packageName, e)
        }
        var isShouldBlur = false


        launcherClass.findMethod {
            name("openFolder")
            parameterTypes(folderInfo, View::class.java)
        }.createAfterHook {
            val mLauncher = applicationClass.callStaticMethod("getLauncher") as Activity
            val isInNormalEditing = mLauncher.callMethod("isInNormalEditing") as Boolean
            if (!isInNormalEditing)
                blurUtilsClass.callStaticMethod("fastBlur", 1.0f, mLauncher.window, true)
        }

        launcherClass.findMethod {
            name("isFolderShowing")
        }.createAfterHook {
            isShouldBlur = it.result as Boolean
        }

        launcherClass.findMethod {
            name("closeFolder")
            parameterTypes(Boolean::class.java)
        }.createAfterHook {
            isShouldBlur = false
            val mLauncher = applicationClass.callStaticMethod("getLauncher") as Activity
            val isInNormalEditing = mLauncher.callMethod("isInNormalEditing") as Boolean
            if (isInNormalEditing)
                blurUtilsClass.callStaticMethod("fastBlur", 1.0f, mLauncher.window, true, 0L)
            else
                blurUtilsClass.callStaticMethod("fastBlur", 0.0f, mLauncher.window, true)
        }

        launcherClass.findMethod {
            name("cancelShortcutMenu")
            parameterTypes(Int::class.java, cancelShortcutMenuReasonClass)
        }.createAfterHook {
            val mLauncher =
                applicationClass.callStaticMethod("getLauncher") as Activity
            if (isShouldBlur)
                blurUtilsClass.callStaticMethod("fastBlur", 1.0f, mLauncher.window, true, 0L)
        }

        launcherClass.findMethod {
            name("onGesturePerformAppToHome")
        }.createBeforeHook {
            val mLauncher = applicationClass.callStaticMethod("getLauncher") as Activity
            if (isShouldBlur) {
                blurUtilsClass.callStaticMethod("fastBlur", 1.0f, mLauncher.window, true, 0L)
            }
        }

        blurUtilsClass.findAllMethods { name("fastBlurWhenStartOpenOrCloseApp") }
            .createBeforeHooks {
                val mLauncher = applicationClass.callStaticMethod("getLauncher") as Activity
                val isInEditing = mLauncher.callMethod("isInEditing") as Boolean
                if (isShouldBlur) it.result =
                    blurUtilsClass.callStaticMethod("fastBlur", 1.0f, mLauncher.window, true, 0L)
                else if (isInEditing) it.result =
                    blurUtilsClass.callStaticMethod("fastBlur", 1.0f, mLauncher.window, true, 0L)
            }

        blurUtilsClass.findAllMethods { name("fastBlurWhenFinishOpenOrCloseApp") }
            .createBeforeHooks {
                val mLauncher = applicationClass.callStaticMethod("getLauncher") as Activity
                val isInEditing = mLauncher.callMethod("isInEditing") as Boolean
                if (isShouldBlur) it.result =
                    blurUtilsClass.callStaticMethod("fastBlur", 1.0f, mLauncher.window, true, 0L)
                else if (isInEditing) it.result =
                    blurUtilsClass.callStaticMethod("fastBlur", 1.0f, mLauncher.window, true, 0L)
            }

        blurUtilsClass.findAllMethods { name("fastBlurWhenEnterRecents") }
            .createAfterHooks {
                it.args[0]?.callMethod("hideShortcutMenuWithoutAnim")
            }

        blurUtilsClass.findAllMethods { name("fastBlurWhenExitRecents") }
            .createAfterHooks {
                val mLauncher = applicationClass.callStaticMethod("getLauncher") as Activity
                val isInEditing = mLauncher.callMethod("isInEditing") as Boolean
                if (isShouldBlur) it.result =
                    blurUtilsClass.callStaticMethod("fastBlur", 1.0f, mLauncher.window, true, 0L)
                else if (isInEditing) it.result =
                    blurUtilsClass.callStaticMethod("fastBlur", 1.0f, mLauncher.window, true, 0L)
            }

        blurUtilsClass.findAllMethods { name("fastBlurDirectly") }
            .createBeforeHooks {
                val blurRatio = it.args[0] as Float
                if (isShouldBlur && blurRatio == 0.0f) it.result = null
            }

        if (((PrefsBridge.getStringAsInt("home_recent_blur_level", 6) == 0) && (PrefsBridge.getStringAsInt("home_recent_blur_level", 6) != 5)) ||
            (PrefsBridge.getStringAsInt("home_recent_blur_level", 6) != 0)
        ) {
            navStubViewClass.findMethod {
                name("appTouchResolution")
                parameterTypes(MotionEvent::class.java)
            }.createBeforeHook {
                val mLauncher = applicationClass.callStaticMethod("getLauncher") as Activity
                if (isShouldBlur) {
                    blurUtilsClass.callStaticMethod("fastBlurDirectly", 1.0f, mLauncher.window)
                }
            }
        }

    }

}
