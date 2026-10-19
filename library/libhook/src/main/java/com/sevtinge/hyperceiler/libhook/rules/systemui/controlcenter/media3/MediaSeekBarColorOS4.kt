/*
 * This file is part of HyperCeiler, licensed under AGPL-3.0-or-later.
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.media3

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Looper
import android.widget.SeekBar
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.ArrayDeque

/** The OEM high/middle shader ignores all four primary-color setters. */
internal class MediaSeekBarColorOS4 private constructor(
    private val type: Class<*>, private val level: Field, private val paint: Field,
    val method: Method
) {
    private class Frame(val receiver: Any?, val args: Array<Any?>) {
        var paint: Paint? = null
        var previous: ColorFilter? = null
        var applied: ColorFilter? = null
        fun restore() {
            val target = paint
            if (target != null && target.colorFilter === applied) target.colorFilter = previous
            paint = null; previous = null; applied = null
        }
    }

    /** colorFor must only select the caller's current, privately owned media presentation. */
    fun callback(colorFor: (Any) -> Int?, error: (Exception) -> Unit): IMethodHook = object : IMethodHook {
        private val frames = ThreadLocal<ArrayDeque<Frame>>()
        // ROM calls and rollback can throw arbitrary runtime failures; preserve the host and cleanup boundary.
        @Suppress("TooGenericExceptionCaught")
        private fun restore(frame: Frame) { try { frame.restore() } catch (failure: Exception) { error(failure) } }
        // ROM calls and rollback can throw arbitrary runtime failures; preserve the host and cleanup boundary.
        @Suppress("TooGenericExceptionCaught")
        override fun before(param: HookParam) {
            val stack = frames.get() ?: ArrayDeque<Frame>().also(frames::set)
            val frame = Frame(param.thisObject, param.args)
            stack.addLast(frame)
            try {
                if (param.isSkipped || Looper.myLooper() !== Looper.getMainLooper() || param.thisObject.javaClass != type) return
                if (level.getInt(param.thisObject) !in 1..2) return
                val color = colorFor(param.thisObject) ?: return
                val target = paint.get(param.thisObject) as Paint
                frame.previous = target.colorFilter
                frame.applied = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
                frame.paint = target // Journal before a setter can reenter or throw.
                target.colorFilter = frame.applied
            } catch (failure: Exception) { restore(frame); error(failure) }
        }
        override fun after(param: HookParam) {
            val stack = frames.get() ?: return
            val frame = stack.pollLast()
            if (frame == null || frame.receiver !== param.thisObject || frame.args !== param.args) {
                frame?.let(::restore)
                while (stack.isNotEmpty()) restore(stack.removeLast())
                frames.remove(); return
            }
            if (stack.isEmpty()) frames.remove()
            // Original exceptions/skips and foreign filter writes are retained.
            restore(frame)
        }
    }

    companion object {
        fun create(type: Class<*>): MediaSeekBarColorOS4 {
            check(SeekBar::class.java.isAssignableFrom(type))
            fun field(name: String, expected: Class<*>) = type.getDeclaredField(name).also {
                check(it.type == expected && !Modifier.isStatic(it.modifiers) && Modifier.isFinal(it.modifiers))
                it.isAccessible = true
            }
            val method = type.getDeclaredMethod("drawHighEndMode", Canvas::class.java).also {
                check(it.returnType == Void.TYPE && !Modifier.isStatic(it.modifiers) && Modifier.isFinal(it.modifiers))
                it.isAccessible = true
            }
            return MediaSeekBarColorOS4(type, field("mDeviceLevel", Int::class.java), field("mPaint", Paint::class.java), method)
        }
    }
}
