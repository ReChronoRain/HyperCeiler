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

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.module.hook.home.other

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ShapeDrawable
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.core.view.ViewCompat.animate
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.HookUtils
import com.sevtinge.hyperceiler.utils.getObjectField

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import kotlin.math.sqrt

object BlurWhenShowShortcutMenu : BaseHook() {

    override fun init() {

        val shortcutMenuBackgroundAlpha = mPrefsMap.getInt("home_other_shortcut_background_blur_custom", 200)
        val shortcutMenuLayerClass: Class<*> = findClassIfExists("com.miui.home.launcher.ShortcutMenuLayer")
        val shortcutMenuClass: Class<*> = findClassIfExists("com.miui.home.launcher.shortcuts.ShortcutMenu")
        val blurUtilsClass: Class<*> = findClassIfExists("com.miui.home.launcher.common.BlurUtils")
        val applicationClass: Class<*> = findClassIfExists("com.miui.home.launcher.Application")
        val utilitiesClass: Class<*> = findClassIfExists("com.miui.home.launcher.common.Utilities")
        val dragViewClass: Class<*> = findClassIfExists("com.miui.home.launcher.DragView")

        val allBluredDrawable: MutableList<Drawable> = ArrayList()

        val singleLayerAlpha =
            ((1.0 - sqrt(1.0 - (shortcutMenuBackgroundAlpha / 255.0))) * 255.0).toInt()

        var isShortcutMenuLayerBlurred = false
        var targetView: ViewGroup? = null
        var dragView: View? = null
        var blurBackground = true

        fun showBlurDrawable() {
            allBluredDrawable.forEach { drawable ->
                XposedHelpers.callMethod(drawable, "setVisible", true, false)
            }
        }

        fun hideBlurDrawable() {
            allBluredDrawable.forEach { drawable ->
                XposedHelpers.callMethod(drawable, "setVisible", false, false)
            }
        }

        XposedBridge.hookAllMethods(
            shortcutMenuLayerClass,
            "showShortcutMenu",
            object : XC_MethodHook() {
                @RequiresApi(Build.VERSION_CODES.S)
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val dragObject = param.args[0]
                    val dragViewInfo = XposedHelpers.callMethod(dragObject, "getDragInfo")
                    val iconIsInFolder =
                        XposedHelpers.callMethod(dragViewInfo, "isInFolder") as Boolean

                    val mLauncher = XposedHelpers.callStaticMethod(applicationClass, "getLauncher")
                    val systemUiController =
                        XposedHelpers.callMethod(mLauncher, "getSystemUiController")
                    val mWindow = HookUtils.getValueByField(systemUiController, "mWindow")
                    val targetBlurView = XposedHelpers.callMethod(mLauncher, "getScreen") as View
                    // 修复文件夹内各种模糊冲突异常
                    blurBackground = if (iconIsInFolder) !mPrefsMap.getBoolean("home_folder_blur") else true
                    val renderEffectArray = arrayOfNulls<RenderEffect>(51)
                    for (index in 0..50) {
                        renderEffectArray[index] = RenderEffect.createBlurEffect(
                            (index + 1).toFloat(),
                            (index + 1).toFloat(),
                            Shader.TileMode.MIRROR
                        )
                    }

                    val valueAnimator = ValueAnimator.ofInt(0, 50)
                    valueAnimator.addUpdateListener { animator ->
                        val value = animator.animatedValue as Int
                        targetBlurView.setRenderEffect(renderEffectArray[value])
                        // 修复始终模糊壁纸冲突导致的各种模糊异常
                        if (blurBackground && !mPrefsMap.getBoolean("home_other_always_blur_launcher_wallpaper")) {
                            XposedHelpers.callStaticMethod(
                                blurUtilsClass,
                                "fastBlurDirectly",
                                value / 50f,
                                mWindow
                            )
                        }
                    }
                    dragView =
                        XposedHelpers.callMethod(dragObject, "getDragView") as View
                    targetView = XposedHelpers.callMethod(dragView, "getContent") as ViewGroup
                    valueAnimator.duration = 200
                    valueAnimator.start()
                    hideBlurDrawable()
                    isShortcutMenuLayerBlurred = true
                }
            })

        XposedBridge.hookAllMethods(
            shortcutMenuLayerClass,
            "onDragStart",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (isShortcutMenuLayerBlurred) {
                        if (targetView != null) {
                            targetView!!.transitionAlpha = 0f
                        }
                    }
                }
            })

        XposedBridge.hookAllMethods(
            shortcutMenuLayerClass,
            "onDragEnd",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (isShortcutMenuLayerBlurred) {
                        val isLocked = XposedHelpers.callStaticMethod(
                            utilitiesClass,
                            "isScreenCellsLocked"
                        ) as Boolean
                        if (isLocked && dragView != null) {
                            animate(dragView!!).scaleX(1f).scaleY(1f).setDuration(200).start()
                        }
                    }
                }
            })

        XposedBridge.hookAllMethods(dragViewClass, "remove", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                if (isShortcutMenuLayerBlurred) {
                    param.result = null
                }
            }
        })

        XposedBridge.hookAllMethods(
            shortcutMenuClass,
            "reset",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (isShortcutMenuLayerBlurred) {
                        isShortcutMenuLayerBlurred = false
                        if (targetView != null) {
                            targetView!!.transitionAlpha = 1f
                        }
                        val mLauncher =
                            XposedHelpers.callStaticMethod(applicationClass, "getLauncher")
                        val systemUiController =
                            XposedHelpers.callMethod(mLauncher, "getSystemUiController")
                        val mWindow = HookUtils.getValueByField(systemUiController, "mWindow")

                        if (blurBackground) {
                            XposedHelpers.callStaticMethod(
                                blurUtilsClass, "fastBlurDirectly", 0f, mWindow
                            )
                        }
                    }
                }
            })

        XposedBridge.hookAllMethods(
            shortcutMenuLayerClass,
            "hideShortcutMenu",
            object : XC_MethodHook() {
                @RequiresApi(Build.VERSION_CODES.S)
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (isShortcutMenuLayerBlurred) {
                        val editStateChangeReason = param.args[0]
                        val shortcutMenuLayer = param.thisObject as FrameLayout
                        val mLauncher =
                            XposedHelpers.callStaticMethod(applicationClass, "getLauncher")
                        val systemUiController =
                            XposedHelpers.callMethod(mLauncher, "getSystemUiController")
                        val mWindow = HookUtils.getValueByField(systemUiController, "mWindow")

                        val targetBlurView =
                            XposedHelpers.callMethod(mLauncher, "getScreen") as View

                        val valueAnimator = ValueAnimator.ofInt(50, 0)
                        val renderEffectArray = arrayOfNulls<RenderEffect>(51)
                        for (index in 0..50) {
                            renderEffectArray[index] = RenderEffect.createBlurEffect(
                                (index + 1).toFloat(),
                                (index + 1).toFloat(),
                                Shader.TileMode.MIRROR
                            )
                        }
                        valueAnimator.addUpdateListener { animator ->
                            val value = animator.animatedValue as Int
                            targetBlurView.setRenderEffect(renderEffectArray[value])
                            // 修复始终模糊壁纸模糊丢失
                            if (blurBackground && !mPrefsMap.getBoolean("home_other_always_blur_launcher_wallpaper")) {
                                XposedHelpers.callStaticMethod(
                                    blurUtilsClass, "fastBlurDirectly", value / 50f, mWindow
                                )
                            }
                        }
                        valueAnimator.addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                shortcutMenuLayer.background = null
                                showBlurDrawable()
                                targetView!!.transitionAlpha = 1f
                                targetBlurView.setRenderEffect(null)
                                isShortcutMenuLayerBlurred = false
                                if (editStateChangeReason != null && editStateChangeReason.toString() != "drag_over_threshold") {
                                    XposedHelpers.callMethod(dragView, "remove")
                                }
                            }
                        })
                        valueAnimator.duration = 200
                        valueAnimator.start()

                        if (editStateChangeReason != null) {
                            logI(
                                TAG,
                                this@BlurWhenShowShortcutMenu.lpparam.packageName,
                                editStateChangeReason.toString()
                            )
                        } else {
                            isShortcutMenuLayerBlurred = false
                            XposedHelpers.callMethod(dragView, "remove")
                        }
                    }
                }
            })

        XposedBridge.hookAllMethods(
            blurUtilsClass,
            "fastBlurDirectly",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val blurRatio = param.args[0] as Float
                    if (isShortcutMenuLayerBlurred && blurRatio == 0.0f) {
                        param.result = null
                    }
                }
            })


        if (shortcutMenuBackgroundAlpha != 255) {
            XposedBridge.hookAllMethods(
                shortcutMenuClass,
                "setMenuBg",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (!isShortcutMenuLayerBlurred) {
                            return
                        }

                        val mAppShortcutMenu: ViewGroup
                        val mAppShortcutMenuBackground: GradientDrawable

                        val mSystemShortcutMenu: ViewGroup
                        val mSystemShortcutMenuBackground: GradientDrawable
                        /*
                             val mWidgetShortcutMenu: ViewGroup
                             val mWidgetShortcutMenuBackground: GradientDrawable
                        */
                        val mAppPersonaliseShortcutMenu: ViewGroup
                        val mAppPersonaliseShortcutMenuBackground: GradientDrawable

                        val mFolderShortcutMenu: ViewGroup
                        val mFolderShortcutMenuBackground: GradientDrawable

                        try {
                            mAppShortcutMenu = param.thisObject.getObjectField("mAppShortcutMenu") as ViewGroup
                            mAppShortcutMenuBackground =
                                mAppShortcutMenu.background as GradientDrawable
                            mAppShortcutMenuBackground.alpha = singleLayerAlpha
                            for (index in 0..mAppShortcutMenu.childCount) {
                                val child = mAppShortcutMenu.getChildAt(index)
                                if (child != null && child.background != null) {
                                    if (child.background is Drawable) {
                                        val childBackground = child.background as Drawable
                                        childBackground.alpha = singleLayerAlpha
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            logE(
                                TAG,
                                this@BlurWhenShowShortcutMenu.lpparam.packageName,
                                "BlurWhenShowShortcutMenu get mAppShortcutMenu failed by: $e"
                            )
                        }
                        try {
                            mSystemShortcutMenu = param.thisObject.getObjectField("mSystemShortcutMenu") as ViewGroup
                            mSystemShortcutMenuBackground =
                                mSystemShortcutMenu.background as GradientDrawable
                            mSystemShortcutMenuBackground.alpha = singleLayerAlpha
                            for (index in 0..mSystemShortcutMenu.childCount) {
                                val child = mSystemShortcutMenu.getChildAt(index)
                                if (child != null && child.background != null) {
                                    if (child.background is Drawable) {
                                        val childBackground = child.background as Drawable
                                        childBackground.alpha = singleLayerAlpha
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            logE(
                                TAG,
                                this@BlurWhenShowShortcutMenu.lpparam.packageName,
                                "BlurWhenShowShortcutMenu get mSystemShortcutMenu failed by: $e"
                            )
                        }
                        try {
                            mAppPersonaliseShortcutMenu =
                                param.thisObject.getObjectField("mAppPersonaliseShortcutMenu") as ViewGroup
                            mAppPersonaliseShortcutMenuBackground =
                                mAppPersonaliseShortcutMenu.background as GradientDrawable
                            mAppPersonaliseShortcutMenuBackground.alpha = singleLayerAlpha
                            for (index in 0..mAppPersonaliseShortcutMenu.childCount) {
                                val child = mAppPersonaliseShortcutMenu.getChildAt(index)
                                if (child != null && child.background != null) {
                                    if (child.background is Drawable) {
                                        val childBackground = child.background as Drawable
                                        childBackground.alpha = singleLayerAlpha
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            logE(
                                TAG,
                                this@BlurWhenShowShortcutMenu.lpparam.packageName,
                                "BlurWhenShowShortcutMenu get mAppPersonaliseShortcutMenu failed by: $e"
                            )
                        }
                        try {
                            mFolderShortcutMenu = param.thisObject.getObjectField("mFolderShortcutMenu") as ViewGroup
                            mFolderShortcutMenuBackground =
                                mFolderShortcutMenu.background as GradientDrawable
                            mFolderShortcutMenuBackground.alpha = singleLayerAlpha
                            for (index in 0..mFolderShortcutMenu.childCount) {
                                val child = mFolderShortcutMenu.getChildAt(index)
                                if (child != null && child.background != null) {
                                    if (child.background is Drawable) {
                                        val childBackground = child.background as Drawable
                                        childBackground.alpha = singleLayerAlpha
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            logE(
                                TAG,
                                this@BlurWhenShowShortcutMenu.lpparam.packageName,
                                "BlurWhenShowShortcutMenu get mFolderShortcutMenu failed by: $e"
                            )
                        }
                        /*try {
                             mWidgetShortcutMenu = param.thisObject.getObjectField("mWidgetShortcutMenu") as ViewGroup
                             mWidgetShortcutMenuBackground =
                                 mWidgetShortcutMenu.background as GradientDrawable
                             mWidgetShortcutMenuBackground.alpha = singleLayerAlpha
                             for (index in 0..mWidgetShortcutMenu.childCount) {
                                 val child = mWidgetShortcutMenu.getChildAt(index)
                                 if (child != null && child.background != null) {
                                     if (child.background is Drawable) {
                                         val childBackground = child.background as Drawable
                                         childBackground.alpha = singleLayerAlpha
                                     }
                                 }
                             }
                         } catch (e: Exception) {
                             XposedBridge.log("HyperCeiler: BlurWhenShowShortcutMenu get mWidgetShortcutMenu failed by: $e")
                         }*/
                    }
                })
            XposedBridge.hookAllMethods(
                shortcutMenuClass,
                "addArrow",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (!isShortcutMenuLayerBlurred) {
                            return
                        }
                        val mArrow = HookUtils.getValueByField(
                            param.thisObject, "mArrow"
                        ) as View
                        val mArrowBackground = mArrow.background as ShapeDrawable
                        mArrowBackground.alpha = shortcutMenuBackgroundAlpha
                    }
                })
        }
    }

}
