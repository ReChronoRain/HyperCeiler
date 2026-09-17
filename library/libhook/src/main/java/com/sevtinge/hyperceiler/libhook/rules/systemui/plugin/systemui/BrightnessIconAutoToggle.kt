/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.plugin.systemui

import android.database.ContentObserver
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.core.java.Constructors
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHooks
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectFieldOrNullAs
import java.lang.ref.WeakReference
import java.util.WeakHashMap

/** Adds an optional click action and an explicit auto-brightness state badge. */
object BrightnessIconAutoToggle {
    private const val TAG = "BrightnessIconAutoToggle"
    private const val BADGE_SHIFT_DP = 6f
    private val bindings = WeakHashMap<View, IconBinding>()
    private var cleanupRegistered = false

    /** Installs the brightness slider hooks for the supplied SystemUI plugin class loader. */
    fun initLoaderHook(classLoader: ClassLoader) {
        if (!cleanupRegistered) {
            cleanupRegistered = true
            BaseHook.registerHotReloadCleanup {
                val oldBindings = bindings.values.toList()
                bindings.clear()
                val cleanup = Runnable { oldBindings.forEach { it.dispose() } }
                if (Looper.myLooper() == Looper.getMainLooper()) cleanup.run()
                else Handler(Looper.getMainLooper()).post(cleanup)
            }
        }
        val name = "miui.systemui.controlcenter.panel.main.brightness.BrightnessSliderController"
        runCatching {
            val clazz = loadClass(name, classLoader)
            Constructors.find(clazz).toList().createAfterHooks { param -> install(param.thisObject) }
            clazz.findMethod { name("onBindViewHolder") }
                .createAfterHook { param -> install(param.thisObject) }
            clazz.findMethod { name("updateIconProgress") }
                .createAfterHook { param -> install(param.thisObject) }
        }.onFailure {
            XposedLog.d(TAG, "class/method unavailable: $name")
        }
    }

    private fun install(controller: Any) {
        runCatching {
            // Only the primary brightness slider's binding; do not search other sliders.
            val holder = controller.callMethod("getSliderHolder") ?: return
            val binding = holder.callMethod("getBinding") ?: return
            val icon = binding.getObjectFieldOrNullAs<View>("icon") ?: return
            val state = bindings[icon] ?: IconBinding(icon, controller).also { bindings[icon] = it }
            state.bindController(controller)
            val highlighted = controller.getObjectFieldOrNullAs<Any>("currentColorState")
                ?.toString() == "HIGHLIGHT"
            state.updateColor(highlighted)
        }.onFailure {
            XposedLog.d(TAG, "install failed: ${it.message}")
        }
    }

    private class IconBinding(icon: View, controller: Any) : View.OnAttachStateChangeListener,
        View.OnLayoutChangeListener {
        private val iconRef = WeakReference(icon)
        private var controllerRef = WeakReference(controller)
        private val resolver = icon.context.contentResolver
        private val wasClickable = icon.isClickable
        private val badge = AutoBrightnessBadgeDrawable()
        private var badgeHostRef = WeakReference<ViewGroup>(null)
        private var observing = false
        private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) = syncMode()
        }

        init {
            icon.setOnClickListener { toggle() }
            icon.addOnAttachStateChangeListener(this)
            icon.addOnLayoutChangeListener(this)
            if (icon.isAttachedToWindow) onViewAttachedToWindow(icon)
        }

        fun bindController(controller: Any) {
            if (controllerRef.get() !== controller) controllerRef = WeakReference(controller)
        }

        fun updateColor(highlighted: Boolean) {
            val icon = iconRef.get() ?: return
            val name = if (highlighted) "toggle_slider_brightness_icon_color"
                else "toggle_slider_icon_color"
            val id = icon.resources.getIdentifier(name, "color", "miui.systemui.plugin")
            badge.setColor(if (id != 0) icon.context.getColor(id)
                else if (highlighted) 0xffff9f05.toInt() else 0xff959595.toInt())
        }

        private fun syncMode() {
            val icon = iconRef.get() ?: return
            runCatching {
                val automatic = Settings.System.getInt(resolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
                ) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                val shouldShow = automatic && icon.isAttachedToWindow
                val host = icon.parent as? ViewGroup
                if (shouldShow) {
                    if (badgeHostRef.get() !== host) {
                        badgeHostRef.get()?.overlay?.remove(badge)
                        badgeHostRef = WeakReference(host)
                        host?.overlay?.add(badge)
                    }
                } else {
                    removeBadge()
                }
                updateBounds(icon, host)
            }.onFailure {
                XposedLog.e(TAG, "sync mode failed: ${it.message}")
            }
        }

        private fun updateBounds(icon: View, host: ViewGroup? = badgeHostRef.get()) {
            // Draw in the slider parent's overlay. The icon overlay is only 34dp wide,
            // so a rightward marker would otherwise be clipped at the icon edge.
            if (host == null) return
            val size = (minOf(icon.width, icon.height) * 14f / 34f).toInt()
            // Leave room for the sun's longest rays at maximum brightness.
            val shift = (icon.resources.displayMetrics.density * BADGE_SHIFT_DP).toInt()
            val left = icon.left + icon.width - size + shift
            val top = icon.top
            badge.setBounds(left, top, left + size, top + size)
        }

        private fun removeBadge() {
            badgeHostRef.get()?.overlay?.remove(badge)
            badgeHostRef.clear()
        }

        private fun toggle() {
            runCatching {
                val controller = checkNotNull(controllerRef.get()) { "brightness controller unavailable" }
                val panelProvider = checkNotNull(controller.getObjectFieldOrNullAs<Any>("brightnessPanelController"))
                val panel = checkNotNull(panelProvider.callMethod("get"))
                val delegate = checkNotNull(panel.callMethod("getDelegate"))
                val tiles = checkNotNull(delegate.getObjectFieldOrNullAs<Any>("tilesDelegate"))
                val tile = checkNotNull(tiles.callMethod("getQSTile", "autobrightness"))
                // Reuse the secondary panel's tile. Its controller also resets MIUI's
                // short-term brightness model when disabling auto brightness. Writing
                // SCREEN_BRIGHTNESS_MODE alone leaves the manual adjustment in place.
                // The setting observer updates the badge after this asynchronous click.
                tile.callMethod("click", null)
            }.onFailure {
                XposedLog.e(TAG, "toggle failed: ${it.message}")
            }
        }

        override fun onViewAttachedToWindow(v: View) {
            if (!observing) {
                runCatching {
                    resolver.registerContentObserver(
                        Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE),
                        false, observer,
                    )
                    observing = true
                }.onFailure {
                    XposedLog.e(TAG, "register observer failed: ${it.message}")
                }
            }
            syncMode()
        }

        override fun onViewDetachedFromWindow(v: View) {
            if (observing) {
                resolver.unregisterContentObserver(observer)
                observing = false
            }
            removeBadge()
        }

        override fun onLayoutChange(v: View, left: Int, top: Int, right: Int, bottom: Int,
            oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int) = updateBounds(v)

        fun dispose() {
            removeBadge()
            iconRef.get()?.let {
                onViewDetachedFromWindow(it)
                it.removeOnAttachStateChangeListener(this)
                it.removeOnLayoutChangeListener(this)
                it.setOnClickListener(null)
                it.isClickable = wasClickable
            }
            if (observing) {
                resolver.unregisterContentObserver(observer)
                observing = false
            }
        }
    }

    /** A solid glyph in the slider overlay, positioned relative to its brightness icon. */
    private class AutoBrightnessBadgeDrawable : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xffff9f05.toInt()
            typeface = Typeface.DEFAULT_BOLD
        }
        private val shape = Path()

        override fun onBoundsChange(bounds: Rect) {
            shape.reset()
            val size = minOf(bounds.width(), bounds.height()).toFloat()
            if (size <= 0f) return
            paint.textSize = size * 0.82f
            paint.getTextPath("A", 0, 1, 0f, 0f, shape)
            val glyphBounds = RectF()
            shape.computeBounds(glyphBounds, true)
            shape.offset(bounds.exactCenterX() - glyphBounds.centerX(),
                bounds.exactCenterY() - glyphBounds.centerY())
            invalidateSelf()
        }

        fun setColor(color: Int) {
            if (paint.color == color) return
            paint.color = color
            invalidateSelf()
        }

        override fun draw(canvas: Canvas) = canvas.drawPath(shape, paint)

        override fun setAlpha(alpha: Int) {
            paint.alpha = alpha
            invalidateSelf()
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            paint.colorFilter = colorFilter
            invalidateSelf()
        }

        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }
}
