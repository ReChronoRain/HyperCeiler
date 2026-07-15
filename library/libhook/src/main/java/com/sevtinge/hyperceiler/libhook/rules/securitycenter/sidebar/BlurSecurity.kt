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
package com.sevtinge.hyperceiler.libhook.rules.securitycenter.sidebar

import android.graphics.drawable.LayerDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Space
import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.blur.MiBlurUtilsKt.addMiBackgroundBlendColor
import com.sevtinge.hyperceiler.libhook.utils.hookapi.blur.MiBlurUtilsKt.clearMiBackgroundBlendColor
import com.sevtinge.hyperceiler.libhook.utils.hookapi.blur.MiBlurUtilsKt.setBlurRoundRect
import com.sevtinge.hyperceiler.libhook.utils.hookapi.blur.MiBlurUtilsKt.setMiBackgroundBlurMode
import com.sevtinge.hyperceiler.libhook.utils.hookapi.blur.MiBlurUtilsKt.setMiBackgroundBlurRadius
import com.sevtinge.hyperceiler.libhook.utils.hookapi.blur.MiBlurUtilsKt.setMiViewBlurMode
import com.sevtinge.hyperceiler.libhook.utils.hookapi.blur.MiBlurUtilsKt.setPassWindowBlurEnabled
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.children
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.findDescendantByIdName
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.resourceEntryName
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClassOrNull
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getAdditionalInstanceField
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.hookAllConstructors
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.setAdditionalInstanceField

object BlurSecurity : BaseHook() {
    private const val MATERIAL_MARK = "hyperceiler_sidebar_material"
    private const val MATERIAL_LISTENER_MARK = "hyperceiler_sidebar_material_listener"

    private val blurRadius by lazy {
        PrefsBridge.getInt("security_center_blurradius", 60)
    }

    private val backgroundColor by lazy {
        PrefsBridge.getInt("security_center_color", -1)
    }

    override fun init() {
        val turboLayoutClass = loadClassOrNull(
            "com.miui.gamebooster.windowmanager.newbox.TurboLayout"
        ) ?: return

        hookDockLayout(turboLayoutClass)
        hookToolboxContent(turboLayoutClass)
        hookGameToolbox(turboLayoutClass)
        hideTopLineImages()
    }

    private fun hookDockLayout(turboLayoutClass: Class<*>) {
        val dockLayoutClass = turboLayoutClass.methods
            .firstOrNull { it.name == "getDockLayout" }
            ?.returnType
            ?: return

        dockLayoutClass.hookAllConstructors {
            after { param ->
                (param.thisObject as? View)?.applyMaterialOnAttach(clearBackground = true)
            }
        }
    }

    private fun hookToolboxContent(turboLayoutClass: Class<*>) {
        turboLayoutClass.findMethod {
            name("getTargetBox")
        }.createAfterHook { param ->
            val targetBox = param.result as? ViewGroup ?: return@createAfterHook
            val listener = object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(view: View) {
                    val mainContent = targetBox.findDescendantByIdName("main_content") ?: return
                    mainContent.background = null
                    targetBox.applyMaterial()
                }

                override fun onViewDetachedFromWindow(view: View) {
                    setMaterialProtected(targetBox, false)
                }
            }
            targetBox.addOnAttachStateChangeListener(listener)
            registerHotReloadCleanup {
                targetBox.removeOnAttachStateChangeListener(listener)
            }
        }
    }

    private fun hookGameToolbox(turboLayoutClass: Class<*>) {
        hookGameTurboChildren(turboLayoutClass)

        val newToolBoxTopViewClass = loadClassOrNull(
            "com.miui.gamebooster.windowmanager.newbox.NewToolBoxTopView"
        ) ?: return

        newToolBoxTopViewClass.hookAllConstructors {
            after { param ->
                val topView = param.thisObject as? View ?: return@after
                val listener = object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(view: View) {
                        val toolboxContent = ((view.parent as? ViewGroup)?.parent as? View) ?: return
                        toolboxContent.applyMaterial()
                    }

                    override fun onViewDetachedFromWindow(view: View) {
                        val toolboxContent = ((view.parent as? ViewGroup)?.parent as? View) ?: return
                        setMaterialProtected(toolboxContent, false)
                    }
                }
                topView.addOnAttachStateChangeListener(listener)
                registerHotReloadCleanup {
                    topView.removeOnAttachStateChangeListener(listener)
                }
            }
        }
    }

    private fun hookGameTurboChildren(turboLayoutClass: Class<*>) {
        val gameTurboLayoutClass = turboLayoutClass.methods
            .firstOrNull { it.name == "getGameTurboLayout" }
            ?.returnType
            ?: return

        gameTurboLayoutClass.hookAllConstructors {
            after { param ->
                (param.thisObject as? ViewGroup)?.applyMaterialToGameTurboChildrenOnAttach()
            }
        }

        gameTurboLayoutClass.findMethod {
            name("getMainView")
        }.createAfterHook { param ->
            val gameTurboLayout = param.thisObject as? ViewGroup ?: return@createAfterHook
            gameTurboLayout.applyMaterialToGameTurboChildrenOnAttach()
        }
    }

    private fun hideTopLineImages() {
        ImageView::class.java.hookAllConstructors {
            after { param ->
                val imageView = param.thisObject as? ImageView ?: return@after
                if (imageView.id == View.NO_ID) return@after

                when (imageView.resourceEntryName()) {
                    "video_box_top_line_bg", "game_turbo_top_line_bg" -> {
                        imageView.setImageDrawable(null)
                        imageView.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun View.applyMaterialOnAttach(clearBackground: Boolean = false) {
        if (getAdditionalInstanceField(MATERIAL_LISTENER_MARK) == true) {
            if (isAttachedToWindow) {
                post { applyMaterial(clearBackground) }
            }
            return
        }
        setAdditionalInstanceField(MATERIAL_LISTENER_MARK, true)

        val listener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {
                view.applyMaterial(clearBackground)
            }

            override fun onViewDetachedFromWindow(view: View) {
                setMaterialProtected(view, false)
            }
        }
        addOnAttachStateChangeListener(listener)
        registerHotReloadCleanup {
            removeOnAttachStateChangeListener(listener)
            this@applyMaterialOnAttach.setAdditionalInstanceField(MATERIAL_LISTENER_MARK, false)
        }

        if (isAttachedToWindow) {
            post { applyMaterial(clearBackground) }
        }
    }

    private fun ViewGroup.applyMaterialToGameTurboChildrenOnAttach() {
        applyGameTurboMaterials()

        if (getAdditionalInstanceField(MATERIAL_LISTENER_MARK) == true) {
            if (isAttachedToWindow) {
                post { applyGameTurboMaterials() }
            }
            return
        }
        setAdditionalInstanceField(MATERIAL_LISTENER_MARK, true)

        val listener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {
                (view as? ViewGroup)?.applyGameTurboMaterials()
            }

            override fun onViewDetachedFromWindow(view: View) {
                (view as? ViewGroup)?.children
                    ?.flatMap { it.gameTurboMaterialTargets() }
                    ?.forEach { setMaterialProtected(it, false) }
            }
        }
        addOnAttachStateChangeListener(listener)
        registerHotReloadCleanup {
            removeOnAttachStateChangeListener(listener)
            this@applyMaterialToGameTurboChildrenOnAttach
                .setAdditionalInstanceField(MATERIAL_LISTENER_MARK, false)
        }
    }

    private fun ViewGroup.applyGameTurboMaterials() {
        children
            .flatMap { it.gameTurboMaterialTargets() }
            .forEach {
                it.applyMaterialOnAttach()
            }
    }

    private fun View.gameTurboMaterialTargets(): Sequence<View> {
        if (!shouldApplyGameTurboChildMaterial()) return emptySequence()

        val brightnessViews = findGameTurboBrightnessTargets()
        return brightnessViews?.asSequence() ?: sequenceOf(this)
    }

    private fun View.findGameTurboBrightnessTargets(): List<View>? {
        if (this !is ViewGroup) return null

        val brightnessContainer = findDescendantByIdName("brightness_container") as? ViewGroup
        val directTargets = brightnessContainer?.children
            ?.filter { it.isGameTurboBrightnessTarget() && it.shouldApplyGameTurboChildMaterial() }
            ?.toList()
            .orEmpty()
        if (directTargets.isNotEmpty()) return directTargets

        val autoBrightness = findDescendantByIdName("auto_brightness")
        val brightnessBar = findDescendantByIdName("qs_brightness")
        if (autoBrightness == null && brightnessBar == null) return null

        return listOfNotNull(autoBrightness, brightnessBar)
            .filter { it.shouldApplyGameTurboChildMaterial() }
    }

    private fun View.isGameTurboBrightnessTarget(): Boolean {
        return when (resourceEntryName()) {
            "auto_brightness", "qs_brightness" -> true
            else -> false
        }
    }

    private fun View.prepareGameTurboMaterialTarget() {
        when (resourceEntryName()) {
            "auto_brightness" -> clearAutoBrightnessBackgroundImage()
            "qs_brightness" -> clearBrightnessSliderBackgroundLayer()
        }
    }

    private fun View.clearAutoBrightnessBackgroundImage() {
        val backgroundImage = (this as? ViewGroup)
            ?.findDescendantByIdName("auto_img_bg") as? ImageView
        backgroundImage?.setImageDrawable(null)
        backgroundImage?.visibility = View.GONE
    }

    private fun View.clearBrightnessSliderBackgroundLayer() {
        val slider = (this as? ViewGroup)
            ?.findDescendantByIdName("slider") as? SeekBar ?: return
        (slider.progressDrawable as? LayerDrawable)
            ?.findDrawableByLayerId(android.R.id.background)
            ?.alpha = 0
        slider.invalidate()
    }

    private fun View.shouldApplyGameTurboChildMaterial(): Boolean {
        return this !is Space && layoutParams?.width != 0 && layoutParams?.height != 0
    }

    private fun View.applyMaterial(clearBackground: Boolean = false) {
        runCatching {
            setMaterialProtected(this, false)
            if (clearBackground) {
                background = null
            }
            prepareGameTurboMaterialTarget()
            clearMiBackgroundBlendColor()
            setPassWindowBlurEnabled(true)
            setMiBackgroundBlurMode(1)
            setMiViewBlurMode(1)
            setMiBackgroundBlurRadius(blurRadius)
            setBlurRoundRect(40)
            addMiBackgroundBlendColor(backgroundColor, 101)
            setMaterialProtected(this, true)
        }.onFailure {
            XposedLog.w(TAG, lpparam.packageName, "apply sidebar material failed", it)
        }
    }

    private fun setMaterialProtected(view: View, protected: Boolean) {
        view.setAdditionalInstanceField(MATERIAL_MARK, protected)
    }
}
