/** This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.home.other

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ShapeDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.callMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.callStaticMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getObjectField
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getObjectFieldOrNull
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.hookAllMethods
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog
import kotlin.math.sqrt

@SuppressLint("StaticFieldLeak")
object BlurWhenShowShortcutMenu : BaseHook() {

    private const val BLUR_DURATION = 200L
    private const val MAX_BLUR_RADIUS = 50f

    private val allBlurredDrawables = mutableListOf<Drawable>()
    private var isShortcutMenuLayerBlurred = false
    private var targetView: ViewGroup? = null
    private var dragView: View? = null
    private var blurBackground = true

    private val shortcutMenuBackgroundAlpha by lazy {
        mPrefsMap.getInt("home_other_shortcut_background_blur_custom", 200)
    }

    private val singleLayerAlpha by lazy {
        ((1.0 - sqrt(1.0 - (shortcutMenuBackgroundAlpha / 255.0))) * 255.0).toInt()
    }

    // 类引用
    private lateinit var shortcutMenuLayerClass: Class<*>
    private lateinit var shortcutMenuClass: Class<*>
    private lateinit var blurUtilsClass: Class<*>
    private lateinit var applicationClass: Class<*>
    private lateinit var utilitiesClass: Class<*>
    private lateinit var dragViewClass: Class<*>

    override fun init() {
        if (!loadClasses()) return

        hookShowShortcutMenu()
        hookDragEvents()
        hookDragViewRemove()
        hookShortcutMenuReset()
        hookHideShortcutMenu()
        hookFastBlurDirectly()

        if (shortcutMenuBackgroundAlpha != 255) {
            hookMenuBackground()
            hookArrowBackground()
        }
    }

    private fun loadClasses(): Boolean {
        return try {
            shortcutMenuLayerClass = findClass("com.miui.home.launcher.ShortcutMenuLayer")
            shortcutMenuClass = findClass("com.miui.home.launcher.shortcuts.ShortcutMenu")
            blurUtilsClass = findClass("com.miui.home.launcher.common.BlurUtils")
            applicationClass = findClass("com.miui.home.launcher.Application")
            utilitiesClass = findClass("com.miui.home.launcher.common.Utilities")
            dragViewClass = findClass("com.miui.home.launcher.DragView")
            true
        } catch (e: Throwable) {
            XposedLog.e(TAG, packageName, "Failed to load classes", e)
            false
        }
    }

    private fun hookShowShortcutMenu() {
        shortcutMenuClass.hookAllMethods("showShortcutMenu") {
            before { param ->
                val dragObject = param.args[0]
                val dragViewInfo = dragObject?.callMethod("getDragInfo")
                val iconIsInFolder = dragViewInfo?.callMethod("isInFolder") as Boolean

                val launcher = applicationClass.callStaticMethod("getLauncher")
                val systemUiController = launcher?.callMethod("getSystemUiController")
                val window = systemUiController?.getObjectField("mWindow")
                val targetBlurView = launcher?.callMethod("getScreen") as View

                blurBackground = if (iconIsInFolder) {
                    !mPrefsMap.getBoolean("home_folder_blur")
                } else {
                    true
                }

                createBlurAnimation(targetBlurView, window, 0, 50).start()

                dragView = dragObject.callMethod("getDragView") as View
                targetView = dragView?.callMethod("getContent") as? ViewGroup

                hideBlurDrawables()
                isShortcutMenuLayerBlurred = true}
        }
    }

    private fun hookDragEvents() {
        shortcutMenuLayerClass.apply {
            hookAllMethods("onDragStart") {
                before {
                    if (isShortcutMenuLayerBlurred) {
                        targetView?.transitionAlpha = 0f
                    }
                }
            }

            hookAllMethods("onDragEnd") {
                before {
                    if (isShortcutMenuLayerBlurred) {
                        val isLocked = utilitiesClass.callStaticMethod("isScreenCellsLocked") as Boolean
                        if (isLocked && dragView != null) {
                            dragView?.animate()?.scaleX(1f)?.scaleY(1f)?.setDuration(BLUR_DURATION)?.start()
                        }
                    }
                }
            }
        }
    }

    private fun hookDragViewRemove() {
        dragViewClass.hookAllMethods("remove") {
            before {
                if (isShortcutMenuLayerBlurred) {
                    it.result = null
                }
            }
        }
    }

    private fun hookShortcutMenuReset() {
        shortcutMenuClass.hookAllMethods("reset") {
            before {
                if (isShortcutMenuLayerBlurred) {
                    resetBlurState()
                }
            }
        }
    }

    private fun hookHideShortcutMenu() {
        shortcutMenuLayerClass.hookAllMethods("hideShortcutMenu") {
            before { param ->
                if (!isShortcutMenuLayerBlurred) return@before

                val editStateChangeReason = param.args[0]
                val shortcutMenuLayer = param.thisObject as FrameLayout
                val launcher = applicationClass.callStaticMethod("getLauncher")
                val systemUiController = launcher?.callMethod("getSystemUiController")
                val window = systemUiController?.getObjectField("mWindow")
                val targetBlurView = launcher?.callMethod("getScreen") as View

                val animator = createBlurAnimation(targetBlurView, window, 50, 0)
                animator.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        shortcutMenuLayer.background = null
                        showBlurDrawables()
                        targetView?.transitionAlpha = 1f
                        targetBlurView.setRenderEffect(null)
                        isShortcutMenuLayerBlurred = false

                        if (editStateChangeReason?.toString() != "drag_over_threshold") {
                            dragView?.callMethod("remove")
                        }
                    }
                })
                animator.start()

                if (editStateChangeReason == null) {
                    isShortcutMenuLayerBlurred = false
                    dragView?.callMethod("remove")
                }
            }
        }
    }

    private fun hookFastBlurDirectly() {
        blurUtilsClass.hookAllMethods("fastBlurDirectly") {
            before { param ->
                val blurRatio = param.args[0] as Float
                if (isShortcutMenuLayerBlurred && blurRatio == 0f) {
                    param.result = null
                }
            }
        }
    }

    private fun hookMenuBackground() {
        shortcutMenuClass.hookAllMethods("setMenuBg") {
            after { param ->
                if (!isShortcutMenuLayerBlurred) return@after

                setMenuAlpha(param.thisObject, "mAppShortcutMenu")
                setMenuAlpha(param.thisObject, "mSystemShortcutMenu")
                setMenuAlpha(param.thisObject, "mAppPersonaliseShortcutMenu")
                setMenuAlpha(param.thisObject, "mFolderShortcutMenu")
            }
        }
    }

    private fun hookArrowBackground() {
        shortcutMenuClass.hookAllMethods("addArrow") {
            after { param ->
                if (!isShortcutMenuLayerBlurred) return@after

                val arrow = param.thisObject.getObjectField("mArrow") as? View
                val arrowBackground = arrow?.background as? ShapeDrawable
                arrowBackground?.alpha = shortcutMenuBackgroundAlpha
            }
        }
    }

    private fun createBlurAnimation(
        targetView: View,
        window: Any?,
        startRadius: Int,
        endRadius: Int
    ): ValueAnimator {
        val renderEffects = Array(51) { index ->
            RenderEffect.createBlurEffect(
                (index + 1).toFloat(),
                (index + 1).toFloat(),
                Shader.TileMode.MIRROR
            )
        }

        return ValueAnimator.ofInt(startRadius, endRadius).apply {
            duration = BLUR_DURATION
            addUpdateListener { animator ->
                val value = animator.animatedValue as Int
                targetView.setRenderEffect(renderEffects[value])

                if (blurBackground && !mPrefsMap.getBoolean("home_other_always_blur_launcher_wallpaper")) {
                    blurUtilsClass.callStaticMethod("fastBlurDirectly", value / MAX_BLUR_RADIUS, window)
                }
            }
        }
    }

    private fun resetBlurState() {
        isShortcutMenuLayerBlurred = false
        targetView?.transitionAlpha = 1f

        val launcher = applicationClass.callStaticMethod("getLauncher")
        val systemUiController = launcher?.callMethod("getSystemUiController")
        val window = systemUiController?.getObjectField("mWindow")

        if (blurBackground) {
            blurUtilsClass.callStaticMethod("fastBlurDirectly", 0f, window)
        }
    }

    private fun setMenuAlpha(shortcutMenu: Any, fieldName: String) {
        try {
            val menu = shortcutMenu.getObjectFieldOrNull(fieldName) as? ViewGroup ?: return
            (menu.background as? GradientDrawable)?.alpha = singleLayerAlpha

            for (i in 0 until menu.childCount) {
                val child = menu.getChildAt(i)
                (child?.background as? Drawable)?.alpha = singleLayerAlpha
            }
        } catch (e: Exception) {
            XposedLog.e(TAG, packageName, "setMenuAlpha failed for $fieldName", e)
        }
    }

    private fun showBlurDrawables() {
        allBlurredDrawables.forEach { it.callMethod("setVisible", true, false) }
    }

    private fun hideBlurDrawables() {
        allBlurredDrawables.forEach { it.callMethod("setVisible", false, false) }
    }
}
