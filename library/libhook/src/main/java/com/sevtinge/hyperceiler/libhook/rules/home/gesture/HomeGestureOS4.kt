/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.home.gesture

import android.app.Activity
import android.os.SystemClock
import android.view.MotionEvent
import android.view.ViewConfiguration
import com.sevtinge.hyperceiler.libhook.appbase.systemframework.GlobalActionBridge
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.api.DisplayUtils.dp2px
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHook
import java.util.WeakHashMap
import kotlin.math.abs

/** Gesture fallback for HyperOS 4's dex-less Flutter launcher. */
object HomeGestureOS4 : BaseHook() {
    private const val LAUNCHER_ACTIVITY = "com.miui.home.launcher.Launcher"
    private const val DOUBLE_TAP_TIMEOUT = 500L

    private val states = WeakHashMap<Activity, GestureState>()

    override fun init() {
        registerHotReloadCleanup { states.clear() }

        Activity::class.java.findMethod {
            name("onTouchEvent")
            parameterTypes(MotionEvent::class.java)
        }.createBeforeHook { param ->
            val activity = param.thisObject as Activity
            if (activity.componentName.className == LAUNCHER_ACTIVITY) {
                val event = param.args[0] as MotionEvent
                val state = states.getOrPut(activity) {
                    GestureState(ViewConfiguration.get(activity).scaledTouchSlop * 2)
                }
                if (state.handle(activity, event)) param.result = true
            }
        }
    }

    private class GestureState(private val touchSlop: Int) {
        private var downX = 0f
        private var downY = 0f
        private var maxPointerCount = 1
        private var firstTapX = 0f
        private var firstTapY = 0f
        private var firstTapTime = 0L

        fun handle(activity: Activity, event: MotionEvent): Boolean {
            maxPointerCount = maxOf(maxPointerCount, event.pointerCount)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    maxPointerCount = 1
                    return false
                }

                MotionEvent.ACTION_CANCEL -> {
                    maxPointerCount = 1
                    return false
                }

                MotionEvent.ACTION_UP -> {
                    val deltaX = event.rawX - downX
                    val deltaY = event.rawY - downY
                    val swipeThreshold = maxOf(dp2px(64), touchSlop * 3)

                    if (abs(deltaY) >= swipeThreshold && abs(deltaY) > abs(deltaX) * 1.5f) {
                        val suffix = if (maxPointerCount >= 2) "2" else ""
                        val direction = if (deltaY < 0) "up" else "down"
                        maxPointerCount = 1
                        firstTapTime = 0L
                        return GlobalActionBridge.handleAction(
                            activity,
                            "home_gesture_${direction}_swipe$suffix"
                        )
                    }

                    if (maxPointerCount == 1 && abs(deltaX) <= touchSlop && abs(deltaY) <= touchSlop) {
                        val now = SystemClock.elapsedRealtime()
                        val isDoubleTap = now - firstTapTime <= DOUBLE_TAP_TIMEOUT &&
                            abs(event.rawX - firstTapX) <= touchSlop &&
                            abs(event.rawY - firstTapY) <= touchSlop
                        if (isDoubleTap) {
                            firstTapTime = 0L
                            return GlobalActionBridge.handleAction(activity, "home_gesture_double_tap")
                        }
                        firstTapX = event.rawX
                        firstTapY = event.rawY
                        firstTapTime = now
                    }
                    maxPointerCount = 1
                }
            }
            return false
        }
    }
}
