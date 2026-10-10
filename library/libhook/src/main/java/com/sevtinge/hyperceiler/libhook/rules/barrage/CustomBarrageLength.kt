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
package com.sevtinge.hyperceiler.libhook.rules.barrage

import android.graphics.drawable.Drawable
import android.service.notification.StatusBarNotification
import android.text.SpannableStringBuilder
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreHyperOSVersion
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import io.github.libxposed.api.XposedInterface
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicBoolean

/** Local final-text adaptation for the reviewed OS4 BarrageWindowUtils pipeline.
 * 36 is the existing disabled sentinel, not a claim about the OEM's 31/11 limits.
 * Values count String UTF-16 units; a cut never splits a valid surrogate pair.
 * Original filtering, styles, edit-input limits, scheduling and exceptions remain original.
 */
object CustomBarrageLength : BaseHook() {
    private const val WINDOW = "com.xiaomi.barrage.utils.BarrageWindowUtils"
    private const val MANAGER = "com.xiaomi.barrage.manager.BarrageShowManager"
    private const val BARRAGE = "com.xiaomi.barrage.danmu.model.BaseBarrage"
    private val newlines = Regex("(\r\n|\r|\n|\n\r)")

    private class Frame(
        val receiver: Any?, val args: Array<Any?>, val notification: Boolean,
        val enabled: Boolean, val input: Any?
    ) {
        var wechatInput: String? = null
        var wechatOutput: String? = null
        var rendered: SpannableStringBuilder? = null
        var oemText: String? = null
        var desiredText: String? = null
        var consumed = false
    }

    // ROM calls and rollback can throw arbitrary runtime failures; preserve the host and cleanup boundary.
    @Suppress("TooGenericExceptionCaught")
    override fun init() {
        if (!isMoreHyperOSVersion(4f)) {
            initLegacy()
            return
        }
        // One immutable selection per process, like the old lazy value. No stale alias key.
        val limit = PrefsBridge.getInt("barrage_custom_barrage_length", 36)
        if (limit !in 12..108 || limit == 36) return
        val active = AtomicBoolean(false)
        val frames = ThreadLocal<ArrayDeque<Frame>>()
        val handles = ArrayList<XposedInterface.HookHandle>()
        try {
            val window = requiredClass(WINDOW)
            val manager = requiredClass(MANAGER)
            val barrage = requiredClass(BARRAGE)
            val notify = method(window, "addBarrageNotification", Void.TYPE, StatusBarNotification::class.java, Boolean::class.java)
            val send = method(window, "sendBarrage", Void.TYPE, String::class.java, Boolean::class.java)
            val cut = method(window, "cutWeChatMsg", String::class.java, String::class.java)
            val span = method(window, "createSpannable", SpannableStringBuilder::class.java, Drawable::class.java, String::class.java)
            val publish = method(manager, "addDanmu", Void.TYPE, barrage)
            val managerField = field(window, "mBarrageShowManager", manager)
            val orientation = field(window, "currentOrientation", Int::class.java)
            val text = field(barrage, "text", CharSequence::class.java)
            val message = field(barrage, "barrageMsg", String::class.java)

            fun current(): Frame? = frames.get()?.peekLast()
            fun register(target: Method, callback: IMethodHook) {
                handles.add(checkNotNull(hookMethod(target, callback)))
            }
            fun outer(isNotification: Boolean) = object : IMethodHook {
                override fun before(param: HookParam) {
                    val stack = frames.get() ?: ArrayDeque<Frame>().also { frames.set(it) }
                    // Even inactive/skipped nested calls shadow the outer frame.
                    stack.addLast(Frame(param.thisObject, param.args, isNotification,
                        active.get() && !param.isSkipped && param.thisObject?.javaClass == window,
                        param.args.getOrNull(0)))
                }
                override fun after(param: HookParam) {
                    val stack = frames.get() ?: return
                    val frame = stack.peekLast()
                    if (frame != null && frame.receiver === param.thisObject && frame.args === param.args) {
                        stack.removeLast()
                    } else {
                        // A framework pairing mismatch cannot retain authority for later calls.
                        stack.clear()
                    }
                    if (stack.isEmpty()) frames.remove()
                }
            }
            register(notify, outer(true))
            register(send, outer(false))
            register(cut, object : IMethodHook {
                override fun after(param: HookParam) {
                    val frame = current() ?: return
                    if (!active.get() || !frame.enabled || !frame.notification) return
                    if (param.isSkipped || param.throwable != null || frame.receiver !== param.thisObject) return
                    val input = param.args.getOrNull(0) as? String ?: return
                    val output = param.result as? String ?: return
                    frame.wechatInput = input
                    frame.wechatOutput = output
                }
            })
            register(span, object : IMethodHook {
                // ROM calls and rollback can throw arbitrary runtime failures; preserve the host and cleanup boundary.
                @Suppress("TooGenericExceptionCaught")
                override fun after(param: HookParam) {
                    val frame = current() ?: return
                    if (!active.get() || !frame.enabled || !frame.notification) return
                    if (frame.consumed || frame.receiver !== param.thisObject) return
                    if (param.isSkipped || param.throwable != null) return
                    try {
                        val notice = frame.input as? StatusBarNotification ?: return
                        val extras = notice.notification.extras ?: return
                        // The actual ROM flattens input CharSequence/Spanned with String.valueOf.
                        val title = java.lang.String.valueOf(extras.getCharSequence("android.title"))
                        val raw = java.lang.String.valueOf(extras.getCharSequence("android.text"))
                        val body = if (notice.packageName == "com.tencent.mm") {
                            if (frame.wechatInput != raw) return
                            frame.wechatOutput ?: return
                        } else raw
                        val normalized = (title + ": " + body).replace(newlines, "")
                        val expected = oemClip(normalized, if (orientation.getInt(frame.receiver) == 2) 31 else 11)
                        if (param.args.getOrNull(1) != expected) return
                        val rendered = param.result as? SpannableStringBuilder ?: return
                        if (rendered.toString() != "icon" + expected) return
                        frame.rendered = rendered
                        frame.oemText = expected
                        frame.desiredText = selectedClip(normalized, limit)
                    } catch (error: Exception) {
                        debugCallbackError("barrage capture", error)
                    }
                }
            })
            register(publish, object : IMethodHook {
                // ROM calls and rollback can throw arbitrary runtime failures; preserve the host and cleanup boundary.
                @Suppress("TooGenericExceptionCaught")
                override fun before(param: HookParam) {
                    val frame = current() ?: return
                    if (!active.get() || !frame.enabled || frame.consumed) return
                    if (param.isSkipped || param.thisObject?.javaClass != manager) return
                    try {
                        if (managerField.get(frame.receiver) !== param.thisObject) return
                        val item = param.args.getOrNull(0) ?: return
                        if (!barrage.isInstance(item)) return
                        val oldText = text.get(item)
                        if (frame.notification) {
                            val oldMessage = message.get(item)
                            val rendered = frame.rendered ?: return
                            val desired = frame.desiredText ?: return
                            if (oldText !== rendered || oldMessage != frame.oemText ||
                                rendered.toString() != "icon" + frame.oemText) return
                            // Copy original spans; never mutate a builder already held by the ROM.
                            val replacement = SpannableStringBuilder(rendered)
                            replacement.replace(4, replacement.length, desired)
                            message.set(item, desired)
                            try { text.set(item, replacement) } catch (error: Exception) {
                                if (message.get(item) === desired) message.set(item, oldMessage)
                                throw error
                            }
                        } else {
                            val input = frame.input as? String ?: return
                            val normalized = input.replace(newlines, "")
                            // Match the actual OEM intermediate, including its ellipsis, before replacing it.
                            if (oldText !is String || oldText != oemClip(normalized, 31)) return
                            text.set(item, selectedClip(normalized, limit))
                        }
                        frame.consumed = true
                    } catch (error: Exception) {
                        debugCallbackError("barrage final text", error)
                    }
                }
            })
            active.set(true)
        } catch (error: Exception) {
            active.set(false)
            for (handle in handles) {
                try { handle.unhook() } catch (rollback: RuntimeException) { error.addSuppressed(rollback) }
            }
            throw IllegalStateException("Barrage local consumer shape or registration unavailable", error)
        }
    }

    private fun oemClip(value: String, limit: Int): String =
        if (value.length <= limit) value else value.substring(0, limit) + "..."

    private fun selectedClip(value: String, limit: Int): String {
        if (value.length <= limit) return value
        var end = limit
        if (end > 0 && Character.isHighSurrogate(value[end - 1]) && Character.isLowSurrogate(value[end])) end--
        return value.substring(0, end) + "..."
    }

    private fun requiredClass(name: String): Class<*> =
        findClassIfExists(name) ?: throw IllegalStateException("Missing barrage class: $name")

    private fun method(owner: Class<*>, name: String, result: Class<*>, vararg args: Class<*>): Method =
        owner.getDeclaredMethod(name, *args).apply {
            check(returnType == result && !Modifier.isStatic(modifiers))
            isAccessible = true
        }

    private fun field(owner: Class<*>, name: String, expected: Class<*>): Field =
        owner.getDeclaredField(name).apply {
            check(type == expected && !Modifier.isStatic(modifiers) && !Modifier.isFinal(modifiers))
            isAccessible = true
        }

    // Preserve the original pre-OS4 implementation; its global hooks are never installed on OS4.
    private val barrageLength by lazy {
        PrefsBridge.getInt("barrage_custom_barrage_length", 36)
    }

    private fun initLegacy() {
        val clazzString = loadClass("java.lang.String")

        clazzString.findMethod {
            name("subSequence")
            paramCount(2)
        }.createHook {
            before { param ->
                if (Throwable().stackTrace.any { it.className == WINDOW }) {
                    param.args[1] = minOf(barrageLength, (param.thisObject as String).length)
                }
            }

            after {
                if (it.throwable != null) {
                    it.throwable = null
                    it.result = it.thisObject
                }
            }
        }

        clazzString.findMethod {
            name("length")
            paramCount(0)
        }.createHook {
            after { param ->
                val stacktrace = Throwable().stackTrace
                if (stacktrace.any {
                    it.className in setOf(
                        "java.lang.String",
                        "android.text.SpannableStringBuilder"
                    )
                }) return@after
                if (stacktrace.any {
                    it.className == WINDOW && it.methodName in setOf(
                        "addBarrageNotification", "sendBarrage"
                    )
                }) {
                    val realResult = (param.result as Int)
                    param.result = if (barrageLength < 36) {
                        if (realResult > barrageLength) {
                            maxOf(37, realResult)
                        } else realResult
                    } else {
                        if (realResult <= barrageLength) {
                            minOf(35, realResult)
                        } else realResult
                    }
                }
            }
        }
    }
}
