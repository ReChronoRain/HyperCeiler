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

  * Copyright (C) 2023-2025 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.module.hook.home.other

import android.animation.*
import android.app.*
import android.graphics.*
import android.graphics.drawable.*
import android.view.*
import android.widget.*
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.utils.*
import com.sevtinge.hyperceiler.module.base.BaseHook
import de.robv.android.xposed.*
import kotlin.math.*


object ShortcutBackgroundBlur : BaseHook() {
    override fun init() {

        // if (!mPrefsMap.getBoolean("home_shortcut_blur")) return

        // From WINI with MIT
        val shortcutMenuBackgroundAlpha = mPrefsMap.getInt("home_other_shortcut_background_blur_custom", 200)
        val shortcutMenuLayerClass = findClassIfExists("com.miui.home.launcher.ShortcutMenuLayer")
        val shortcutMenuClass = findClassIfExists("com.miui.home.launcher.shortcuts.ShortcutMenu")
        val blurUtilsClass = findClassIfExists("com.miui.home.launcher.common.BlurUtils")
        val applicationClass = findClassIfExists("com.miui.home.launcher.Application")
        val utilitiesClass = findClassIfExists("com.miui.home.launcher.common.Utilities")
        val launcherStateClass = findClassIfExists("com.miui.home.launcher.LauncherState")

        /*
        两层视图alpha计算公式：2x-x^2=y
        x为单层视图alpha 0完全透明 1完全不透明
        y为双层混合后的透明度
        x与y在图层透明度这种情况下永远为正值
        将改公式转换为x=f(y)：x=1-√(1-y)
        */
        val singleLayerAlpha =
            ((1.0 - sqrt(1.0 - (shortcutMenuBackgroundAlpha / 255.0))) * 255.0).toInt()
        logI(
            TAG,
            this.lpparam.packageName,
            "shortcutMenuBackgroundAlpha = $shortcutMenuBackgroundAlpha"
        )
        logI(TAG, this.lpparam.packageName, "singleLayerAlpha = $singleLayerAlpha")

        val mBlurIconAppName = arrayOf("锁屏", "手电筒", "数据", "飞行模式", "蓝牙", "WLAN 热点")
        val allBlurredDrawable: MutableList<Drawable> = ArrayList()

        fun showBlurDrawable() {
            allBlurredDrawable.forEach { drawable ->
                XposedHelpers.callMethod(drawable, "setVisible", true, false)
            }
        }

        fun hideBlurDrawable() {
            allBlurredDrawable.forEach { drawable ->
                XposedHelpers.callMethod(drawable, "setVisible", false, false)
            }
        }

        var isShortcutMenuLayerBlurred = false
        var dragLayer: ViewGroup? = null
        var targetView: View? = null
        var dragLayerBackground: Drawable? = null

        shortcutMenuLayerClass.hookBeforeAllMethods("showShortcutMenu") {
            hideBlurDrawable()
            val dragObject = it.args[0]
            val dragViewInfo = dragObject.callMethod("getDragInfo")
            val iconIsInFolder = dragViewInfo?.callMethod("isInFolder") as Boolean
            if (iconIsInFolder) return@hookBeforeAllMethods

            // 文件夹内不模糊
            val iconIsApplication = dragViewInfo.callMethod("isApplicatoin") as Boolean
            val iconTitle = dragViewInfo.callMethod("getTitle") as String

            if (!iconIsApplication && !mBlurIconAppName.contains(iconTitle)) return@hookBeforeAllMethods

            val mLauncher = applicationClass.callStaticMethod("getLauncher") as Activity
            val launcherStatusField = launcherStateClass.getDeclaredField("ALL_APPS")
            launcherStatusField.isAccessible = true
            val allAppsStatus = launcherStatusField.get(null)
            val stateManager = mLauncher.callMethod("getStateManager")
            val currentState = stateManager?.callMethod("getState")
            if (currentState == allAppsStatus) return@hookBeforeAllMethods

            val targetBlurView = mLauncher.callMethod("getScreen") as View

            val renderEffectArray = arrayOfNulls<RenderEffect>(51)
            for (index in 0..50) {
                renderEffectArray[index] =
                    RenderEffect.createBlurEffect((index + 1).toFloat(), (index + 1).toFloat(), Shader.TileMode.MIRROR)
            }

            val valueAnimator = ValueAnimator.ofInt(0, 50)
            valueAnimator.addUpdateListener { animator ->
                val value = animator.animatedValue as Int
                if (!mPrefsMap.getBoolean("home_blur_wallpaper")) {
                    blurUtilsClass.callStaticMethod("fastBlurDirectly", value / 50f, mLauncher.window)
                }
                targetBlurView.setRenderEffect(renderEffectArray[value])
            }
            val dragView = dragObject.callMethod("getDragView") as View
            targetView = dragView.callMethod("getContent") as View
            dragLayer = targetBlurView.parent as ViewGroup
            valueAnimator.duration = 200
            valueAnimator.start()
            isShortcutMenuLayerBlurred = true
        }

        shortcutMenuLayerClass.hookBeforeAllMethods("onDragStart") {
            if (isShortcutMenuLayerBlurred) {
                val dragObject = it.args[1]
                val dragView = dragObject.callMethod("getDragView") as View
                val dragViewParent = dragView.parent as View
                val bitmap = Bitmap.createBitmap(dragLayer!!.width, dragLayer!!.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                val originalScale = dragView.scaleX
                dragView.scaleX = 1f
                dragView.scaleY = 1f
                dragViewParent.draw(canvas)
                dragView.scaleX = originalScale
                dragView.scaleY = originalScale
                dragLayerBackground = BitmapDrawable(dragLayer!!.context.resources, bitmap)
                targetView?.alpha = 0f
            }
        }

        shortcutMenuLayerClass.hookBeforeAllMethods("onDragEnd") {
            if (isShortcutMenuLayerBlurred) {
                val isLocked = utilitiesClass.callStaticMethod("isScreenCellsLocked") as Boolean
                if (isLocked) {
                    dragLayer?.background = dragLayerBackground
                } else {
                    val valueAnimator = ValueAnimator.ofFloat(0f, 1f)
                    valueAnimator.addListener(object : android.animation.AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            dragLayer?.background = dragLayerBackground
                        }
                    })
                    valueAnimator.duration = 200
                    valueAnimator.start()
                }
            }
        }

        shortcutMenuClass.hookBeforeAllMethods("reset") {
            if (isShortcutMenuLayerBlurred) {
                isShortcutMenuLayerBlurred = false
                targetView!!.alpha = 1f
                val mLauncher = applicationClass.callStaticMethod("getLauncher")
                val systemUiController = mLauncher?.callMethod("getSystemUiController")
                val mWindow = systemUiController?.getObjectField("mWindow")
                blurUtilsClass.callStaticMethod("fastBlurDirectly", 0f, mWindow)
            }
        }

        shortcutMenuLayerClass.hookBeforeAllMethods("hideShortcutMenu") {
            if (isShortcutMenuLayerBlurred) {
                val shortcutMenuLayer = it.thisObject as FrameLayout
                val mLauncher = XposedHelpers.callStaticMethod(applicationClass, "getLauncher")
                val systemUiController = XposedHelpers.callMethod(mLauncher, "getSystemUiController")
                val mWindow = systemUiController.getObjectField("mWindow")
                val targetBlurView = mLauncher.callMethod("getScreen") as View
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
                    if (!mPrefsMap.getBoolean("home_blur_wallpaper")) {
                        blurUtilsClass.callStaticMethod("fastBlurDirectly", value / 50f, mWindow)
                    }
                }
                valueAnimator.addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        shortcutMenuLayer.background = null
                        showBlurDrawable()
                        targetView!!.alpha = 1f
                        targetBlurView.setRenderEffect(null)
                        dragLayer?.background = null
                    }
                })
                valueAnimator.duration = 200
                valueAnimator.start()
            }
            isShortcutMenuLayerBlurred = false
        }

        blurUtilsClass.hookBeforeAllMethods("fastBlurDirectly") {
            val blurRatio = it.args[0] as Float
            if (isShortcutMenuLayerBlurred && blurRatio == 0.0f) {
                it.result = null
            }
        }

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

                        val mWidgetShortcutMenu: ViewGroup
                        val mWidgetShortcutMenuBackground: GradientDrawable

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
                        } catch (_: Exception) {
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
                        } catch (_: Exception) {
                        }
                        try {
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
                        } catch (_: Exception) {
                        }
                    }
                })
            XposedBridge.hookAllMethods(shortcutMenuClass, "addArrow",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (!isShortcutMenuLayerBlurred) {
                            return
                        }
                        val mArrow = getValueByField(
                            param.thisObject,
                            "mArrow"
                        ) as View
                        val mArrowBackground = mArrow.background as ShapeDrawable
                        mArrowBackground.alpha = shortcutMenuBackgroundAlpha
                    }
                }
            )
        }

        /*

        if (shortcutMenuBackgroundAlpha != 255) {
            log("1")
            shortcutMenuClass.hookBeforeAllMethods("setMenuBg") {
                log("3")
                if (!isShortcutMenuLayerBlurred) {
                    log("2")
                    return@hookBeforeAllMethods
                }

                val mAppShortcutMenu = it.thisObject.getObjectField("mAppShortcutMenu") as ViewGroup
                val mAppShortcutMenuBackground = mAppShortcutMenu.background as GradientDrawable
                mAppShortcutMenuBackground.alpha = singleLayerAlpha
                val mSystemShortcutMenu = it.thisObject.getObjectField("mSystemShortcutMenu") as ViewGroup
                val mSystemShortcutMenuBackground = mSystemShortcutMenu.background as GradientDrawable
                mSystemShortcutMenuBackground.alpha = singleLayerAlpha

                for (index in 0..mAppShortcutMenu.childCount) {
                    val child = mAppShortcutMenu.getChildAt(index)
                    if (child != null && child.background != null) {
                        if (child.background is Drawable) {
                            val childBackground = child.background as Drawable
                            childBackground.alpha = singleLayerAlpha
                        }
                    }
                }

                for (index in 0..mSystemShortcutMenu.childCount) {
                    val child = mSystemShortcutMenu.getChildAt(index)
                    if (child != null && child.background != null) {
                        if (child.background is Drawable) {
                            val childBackground = child.background as Drawable
                            childBackground.alpha = singleLayerAlpha
                        }
                    }
                }

            }

            shortcutMenuClass.hookAfterAllMethods("addArrow") {
                log("4")
                if (!isShortcutMenuLayerBlurred) {
                    log("5")
                    return@hookAfterAllMethods
                }
                val mArrow = it.thisObject.getObjectField("mArrow") as View
                val mArrowBackground = mArrow.background as ShapeDrawable
                mArrowBackground.alpha = shortcutMenuBackgroundAlpha
            }
        }

         */

        blurUtilsClass.hookAfterAllMethods("fastBlurWhenEnterRecents") {
            val launcher = it.args[0]
            if (launcher != null) {
                XposedHelpers.callMethod(launcher, "hideShortcutMenuWithoutAnim")
            }
            hideBlurDrawable()
        }

        blurUtilsClass.hookAfterAllMethods("fastBlurWhenExitRecents") {
            showBlurDrawable()
        }

    }
}
