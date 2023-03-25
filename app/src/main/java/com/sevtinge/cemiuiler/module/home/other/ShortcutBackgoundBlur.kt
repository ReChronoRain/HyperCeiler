package com.sevtinge.cemiuiler.module.home.other

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ShapeDrawable
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.*
import com.sevtinge.cemiuiler.utils.woobox.callMethod
import com.sevtinge.cemiuiler.utils.woobox.callStaticMethod
import com.sevtinge.cemiuiler.utils.woobox.findClass
import com.sevtinge.cemiuiler.utils.woobox.getObjectField
import com.sevtinge.cemiuiler.utils.woobox.hookAfterAllMethods
import com.sevtinge.cemiuiler.utils.woobox.hookBeforeAllMethods
import de.robv.android.xposed.XposedHelpers
import kotlin.math.sqrt


object ShortcutBackgoundBlur : BaseHook() {
    @RequiresApi(Build.VERSION_CODES.S)
    override fun init() {

        //if (!mPrefsMap.getBoolean("home_shortcut_blur")) return

        // From WINI with MIT
        val shortcutMenuBackgroundAlpha = mPrefsMap.getInt("home_shortcut_alpha", 200)
        val shortcutMenuLayerClass = "com.miui.home.launcher.ShortcutMenuLayer".findClass()
        val shortcutMenuClass = "com.miui.home.launcher.shortcuts.ShortcutMenu".findClass()
        val blurUtilsClass = "com.miui.home.launcher.common.BlurUtils".findClass()
        val applicationClass = "com.miui.home.launcher.Application".findClass()
        val utilitiesClass = "com.miui.home.launcher.common.Utilities".findClass()
        val launcherStateClass = "com.miui.home.launcher.LauncherState".findClass()

        /*
        两层视图alpha计算公式：2x-x^2=y
        x为单层视图alpha 0完全透明 1完全不透明
        y为双层混合后的透明度
        x与y在图层透明度这种情况下永远为正值
        将改公式转换为x=f(y)：x=1-√(1-y)
        */
        val singleLayerAlpha = ((1.0 - sqrt(1.0 - (shortcutMenuBackgroundAlpha / 255.0))) * 255.0).toInt()

        val BLUR_ICON_APP_NAME = arrayOf("锁屏", "手电筒", "数据", "飞行模式", "蓝牙", "WLAN 热点")
        val allBluredDrawable: MutableList<Drawable> = ArrayList()

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
            val iconIsApplicatoin = dragViewInfo.callMethod("isApplicatoin") as Boolean
            val iconTitle = dragViewInfo.callMethod("getTitle") as String

            if (!iconIsApplicatoin && !BLUR_ICON_APP_NAME.contains(iconTitle)) return@hookBeforeAllMethods

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
                renderEffectArray[index] = RenderEffect.createBlurEffect((index + 1).toFloat(), (index + 1).toFloat(), Shader.TileMode.MIRROR)
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
                    valueAnimator.addListener(object : AnimatorListenerAdapter() {
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
                    renderEffectArray[index] = RenderEffect.createBlurEffect((index + 1).toFloat(), (index + 1).toFloat(), Shader.TileMode.MIRROR)
                }
                valueAnimator.addUpdateListener { animator ->
                    val value = animator.animatedValue as Int
                    targetBlurView.setRenderEffect(renderEffectArray[value])
                    if (!mPrefsMap.getBoolean("home_blur_wallpaper")) {
                        blurUtilsClass.callStaticMethod("fastBlurDirectly", value / 50f, mWindow)
                    }
                }
                valueAnimator.addListener(object : AnimatorListenerAdapter() {
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
            shortcutMenuClass.hookBeforeAllMethods("setMenuBg") {
                if (!isShortcutMenuLayerBlurred) {
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
                if (!isShortcutMenuLayerBlurred) {
                    return@hookAfterAllMethods
                }
                val mArrow = it.thisObject.getObjectField("mArrow") as View
                val mArrowBackground = mArrow.background as ShapeDrawable
                mArrowBackground.alpha = shortcutMenuBackgroundAlpha
            }
        }

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