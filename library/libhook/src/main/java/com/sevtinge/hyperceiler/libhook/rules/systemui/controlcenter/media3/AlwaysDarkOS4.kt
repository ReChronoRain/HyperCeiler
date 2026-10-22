/*
 * This file is part of HyperCeiler, licensed under AGPL-3.0-or-later.
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.media3

import android.animation.ArgbEvaluator
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.TextView
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.libxposed.api.XposedInterface
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.ArrayDeque
import java.util.WeakHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/** Night selection for the OS4 notification media card; never changes a controller Context. */
// Keep this owner-specific lifecycle and its private rollback helpers in one auditable scope.
@Suppress("TooManyFunctions")
object AlwaysDarkOS4 : BaseHook() {
    private const val NC = "com.android.systemui.statusbar.notification.mediacontrol."
    private const val AOD = "com.android.systemui.statusbar.notification.fullaod."
    private const val EFFECT = "com.android.systemui.statusbar.notification.style.vieweffect."
    private const val PREF = "system_ui_control_center_media_control"
    private enum class Kind { EFFECT, FOREGROUND, LAYOUT, DETACH, FRAME, RESET }

    // ROM calls and rollback can throw arbitrary runtime failures; preserve the host and cleanup boundary.
    @Suppress("TooGenericExceptionCaught")
    override fun init() {
        if (PrefsBridge.getStringAsInt(PREF + "_background_mode", 0) != 0 ||
            !PrefsBridge.getBoolean(PREF + "_always_dark")) return
        val plan = Plan()
        val handles = ArrayList<XposedInterface.HookHandle>()
        try {
            for ((method, kind) in plan.hooks) handles.add(checkNotNull(hookMethod(method, Callback(plan, method, kind))))
            registerHotReloadCleanup {
                plan.active.set(false)
                val cleanup = Runnable { plan.clear() }
                if (main()) cleanup.run() else {
                    val done = CountDownLatch(1); val error = arrayOfNulls<Throwable>(1)
                    check(Handler(Looper.getMainLooper()).post {
                        try { cleanup.run() } catch (failure: Throwable) { error[0] = failure } finally { done.countDown() }
                    })
                    check(done.await(500, TimeUnit.MILLISECONDS)) { "Media night cleanup pending" }
                    error[0]?.let { throw IllegalStateException("Media night cleanup failed", it) }
                }
            }
            plan.active.set(true)
        } catch (failure: Throwable) {
            plan.active.set(false)
            for (handle in handles.asReversed()) try { handle.unhook() } catch (rollback: Throwable) { failure.addSuppressed(rollback) }
            throw failure
        }
    }

    private class Plan {
        val active = AtomicBoolean(false)
        val busy = AtomicInteger()
        val resets = ThreadLocal<ArrayDeque<Any>>()
        val contexts = ThreadLocal<ArrayDeque<ContextOrigin>>()
        val owner = required(NC + "MiuiMediaViewControllerImpl")
        val holder = required(NC + "MiuiMediaViewHolder")
        val header = required(NC + "MiuiMediaHeaderView")
        val layoutOwner = required(NC + "MiuiMediaNotificationControllerImpl")
        val layoutHolder = field(layoutOwner, "mediaViewHolder", holder)
        val layoutContext = field(layoutOwner, "context", Context::class.java)
        val listener = required(NC + "MiuiMediaViewControllerImpl\$mediaFullAodListener\$1")
        val aod = required(AOD + "NotifiFullAodController")
        val lazy = required("dagger.Lazy")
        val lazyGet = method(lazy, "get", Any::class.java)
        val context = field(owner, "context", Context::class.java)
        val controllerHolder = field(owner, "holder", holder)
        val controllerAod = field(owner, "fullAodController", lazy)
        val listenerOwner = field(listener, "this\$0", owner)
        val headerHolder = method(header, "getMediaViewHolder", holder)
        val background = field(holder, "mediaBg", ImageView::class.java)
        val texts = listOf("titleText", "artistText", "elapsedTimeView", "totalTimeView").map { field(holder, it, TextView::class.java) }
        val actions = method(holder, "getActionList", List::class.java)
        val seamless = field(holder, "seamlessIcon", ImageView::class.java)
        val seekType = required("miuix.miuixbasewidget.widget.HyperProgressSeekBar")
        val seek = field(holder, "seekBar", seekType)
        val seekNames = listOf("ForegroundPrimaryColor", "ForegroundPrimaryDisableColor", "BackgroundPrimaryColor", "BackgroundPrimaryDisableColor")
        val seekFields = seekNames.map { field(seekType, "m$it", Int::class.java) }
        val seekSetters = seekNames.map { method(seekType, "set$it", Void.TYPE, Int::class.java) }
        val tiny = staticMethod(required("com.miui.utils.configs.MiuiConfigs"), "isFlipTinyScreen", Boolean::class.java, Context::class.java)
        val fraction = field(aod, "mCurrentFraction", Float::class.java)
        val desktop = field(aod, "mFromDesktop", Boolean::class.java)
        val colors = WeakHashMap<Any, MutableList<Change>>()
        val backgrounds = WeakHashMap<Any, Replay>()
        val hooks = ArrayList<Pair<Method, Kind>>()
        init {
            for (name in listOf("MediaViewNormalEffect", "MediaViewBlurEffect", "MediaViewBlurOnKeyguardEffect", "MediaViewGlassEffect",
                "MediaViewGlassFullAodEffect", "MediaViewGlassOnKeyguardEffect", "MediaViewGlassOnKeyguardLightWallPaperEffect")) {
                val type = required(EFFECT + name)
                // Glass effects call the typed blur overload directly, bypassing its erased bridge.
                hooks.add(method(type, "apply", Void.TYPE, if (name == "MediaViewBlurEffect") header else Any::class.java, Context::class.java) to Kind.EFFECT)
            }
            hooks.add(method(owner, "updateForegroundColors", Void.TYPE) to Kind.FOREGROUND)
            hooks.add(method(layoutOwner, "updateLayout\$1", Void.TYPE) to Kind.LAYOUT)
            hooks.add(method(owner, "detach", Void.TYPE) to Kind.DETACH)
            // Reflection requires the complete fixed ROM signature during registration.
            @Suppress("SpreadOperator")
            hooks.add(method(listener, "updateFullAodAnimState", Void.TYPE,
                *Array(10) { Int::class.java }, FloatArray::class.java, FloatArray::class.java, Boolean::class.java, Float::class.java) to Kind.FRAME)
            hooks.add(method(aod, "resetRowFullAodStyle", Void.TYPE) to Kind.RESET)
        }
        fun current(receiver: Any, kind: Kind): Any? = when (kind) {
            Kind.EFFECT -> headerHolder.invoke(receiver)
            Kind.FRAME -> controllerHolder.get(listenerOwner.get(receiver))
            Kind.FOREGROUND, Kind.DETACH -> controllerHolder.get(receiver)
            Kind.LAYOUT -> layoutHolder.get(receiver)
            Kind.RESET -> null
        }
        fun restore(holder: Any) {
            val journal = colors.remove(holder)
            clean(journal.orEmpty().asReversed().map { { it.undo() } })
        }
        fun clear() {
            check(busy.get() == 0) { "Media night callback still executing" }
            clean(colors.keys.toList().map { h -> { restore(h) } } + backgrounds.values.toList().map { r -> { r.restore(this) } })
            backgrounds.clear()
        }
    }

    private class ContextOrigin(val original: Context, val replacement: Context)

    private class Frame(val receiver: Any?, val args: Array<Any?>, val holder: Any?, val eligible: Boolean) {
        val saved = LinkedHashMap<Int, Any?>()
        val replacement = LinkedHashMap<Int, Any?>()
        var context: Context? = null
        var resetting = false
        var contextOrigin: ContextOrigin? = null
        fun replace(index: Int, value: Any?) { saved[index] = args[index]; replacement[index] = value; args[index] = value }
        fun undoArgs() { for ((index, value) in saved) if (args[index] === replacement[index]) args[index] = value }
    }

    private class Callback(private val plan: Plan, private val method: Method, private val kind: Kind) : IMethodHook {
        private val frames = ThreadLocal<ArrayDeque<Frame>>()
        // ROM calls and rollback can throw arbitrary runtime failures; preserve the host and cleanup boundary.
        @Suppress("TooGenericExceptionCaught")
        override fun before(param: HookParam) {
            val receiver = param.thisObject
            val eligible = plan.active.get() && main() && !param.isSkipped && receiver?.javaClass == method.declaringClass
            var holder: Any? = null
            if (eligible) try { holder = plan.current(if (kind == Kind.EFFECT) param.args[0]!! else receiver!!, kind) }
            catch (failure: Exception) { debugCallbackError("media night holder", failure) }
            val frame = Frame(receiver, param.args, holder, eligible)
            val stack = frames.get() ?: ArrayDeque<Frame>().also(frames::set); stack.addLast(frame)
            if (!eligible) return
            plan.busy.incrementAndGet()
            try {
                if (kind == Kind.RESET) {
                    val resets = plan.resets.get() ?: ArrayDeque<Any>().also(plan.resets::set)
                    resets.addLast(receiver!!); frame.resetting = true
                } else if (holder?.javaClass == plan.holder) {
                    if (kind == Kind.EFFECT && param.args[0]?.javaClass == plan.header) {
                        val context = param.args[1] as Context
                        val stack = plan.contexts.get() ?: ArrayDeque<ContextOrigin>().also(plan.contexts::set)
                        val original = stack.lastOrNull { it.replacement === context }?.original ?: context
                        val replacement = night(context)
                        frame.context = original
                        frame.contextOrigin = ContextOrigin(original, replacement).also(stack::addLast)
                        frame.replace(1, replacement)
                    } else if (kind == Kind.FRAME) {
                        applyFrameColors(receiver, frame)
                    }
                }
            } catch (failure: Exception) {
                frame.undoArgs(); frame.saved.clear(); frame.replacement.clear()
                debugCallbackError("media night before", failure)
            }
        }
        private fun applyFrameColors(receiver: Any?, frame: Frame) {
            val owner = plan.listenerOwner.get(receiver)
            val dark = night(plan.context.get(owner) as Context)
            val aod = plan.lazyGet.invoke(plan.controllerAod.get(owner))
            check(aod?.javaClass == plan.aod)
            val reset = plan.resets.get()?.any { it === aod } == true
            val f = if (reset) 1f else plan.fraction.getFloat(aod)
            check(f.isFinite() && f in 0f..1f)
            val fromDesktop = plan.desktop.getBoolean(aod) && !reset
            val endpoint = arrayOf("focus_notification_element_blend_keyguard_color_1", "focus_notification_element_blend_keyguard_color_2", "focus_notification_element_blend_keyguard_color_3")
            val start = arrayOf("focus_notification_element_blend_shade_color_1", "focus_notification_element_blend_shade_color_2", "focus_notification_element_blend_shade_color_3")
            val values = endpoint.indices.map { i ->
                val end = color(dark, endpoint[i])
                if (fromDesktop) ArgbEvaluator().evaluate(f, color(dark, start[i]), end) as Int else end
            }
            // Only values consumed by this media listener; modes, glass arrays,
            // animation alpha, global state and every other listener remain original.
            frame.replace(2, color(dark, "notification_focus_bg_color"))
            for (i in values.indices) frame.replace(4 + i * 2, values[i])
        }

        // ROM calls and rollback can throw arbitrary runtime failures; preserve the host and cleanup boundary.
        @Suppress("TooGenericExceptionCaught")
        override fun after(param: HookParam) {
            val stack = frames.get(); val frame = stack?.pollLast()
            if (stack?.isEmpty() == true) frames.remove()
            if (frame == null) return
            try {
                frame.undoArgs()
                if (frame.receiver !== param.thisObject || frame.args !== param.args) return
                if (!frame.eligible || param.isSkipped) return
                val holder = frame.holder ?: return
                if (kind == Kind.DETACH) {
                    if (!param.hasThrowable) { plan.restore(holder); plan.backgrounds.remove(holder) }
                    return
                }
                val target = if (kind == Kind.EFFECT) param.args[0]!! else param.thisObject!!
                if (plan.current(target, kind) !== holder) return
                val updatesForeground = kind == Kind.FOREGROUND || kind == Kind.LAYOUT
                if (updatesForeground && !param.hasThrowable && plan.active.get()) foreground(plan, target, holder, kind)
                else if ((kind == Kind.EFFECT || kind == Kind.FRAME) && frame.saved.isNotEmpty()) {
                    // A platform setter may write before throwing. Keep its original
                    // parameters for bounded cleanup while leaving that exception intact.
                    val bg = plan.background.get(holder) as ImageView
                    plan.backgrounds[holder] = Replay(method, param.thisObject!!, target, holder, bg, frame.args, kind, frame.context)
                }
            } catch (failure: Exception) { debugCallbackError("media night after", failure) }
            finally {
                if (frame.resetting) {
                    plan.resets.get()?.removeLastOccurrence(frame.receiver)
                    if (plan.resets.get()?.isEmpty() == true) plan.resets.remove()
                }
                frame.contextOrigin?.let { plan.contexts.get()?.removeLastOccurrence(it) }
                if (plan.contexts.get()?.isEmpty() == true) plan.contexts.remove()
                if (frame.eligible) plan.busy.decrementAndGet()
            }
        }
    }

    // This immutable contract records the complete validated ROM shape or captured invocation.
    @Suppress("LongParameterList")
    private class Replay(method: Method, receiver: Any, target: Any, holder: Any, background: ImageView, args: Array<Any?>, val kind: Kind, originalContext: Context?) {
        private val method = method
        private val receiver = WeakReference(receiver)
        private val target = WeakReference(target)
        private val holder = WeakReference(holder)
        private val background = WeakReference(background)
        private val drawable = WeakReference(background.background)
        private val context = if (kind == Kind.EFFECT) WeakReference(originalContext ?: args[1] as Context) else null
        private val args = if (kind == Kind.FRAME) args.map { if (it is FloatArray) it.clone() else it }.toTypedArray() else null
        // Invocation replays the captured ROM signature; the defensive array copy is intentional.
        @Suppress("SpreadOperator")
        fun restore(plan: Plan) {
            val receiver = receiver.get() ?: return; val target = target.get() ?: return
            val holder = holder.get() ?: return; val bg = background.get() ?: return
            if (plan.current(target, kind) !== holder || plan.background.get(holder) !== bg || bg.background !== drawable.get()) return
            if (kind == Kind.EFFECT) method.invoke(receiver, target, checkNotNull(context?.get()) { "Original media effect Context no longer available" })
            else method.invoke(receiver, *checkNotNull(args))
        }
    }

    private class Change(val key: String, target: Any, val original: Any?, val value: Any?, val read: (Any) -> Any?, val write: (Any, Any?) -> Unit) {
        val target = WeakReference(target)
        fun apply() { target.get()?.let { write(it, value) } }
        fun undo() { target.get()?.let { if (read(it) == value) write(it, original) } }
    }
    // ROM calls and rollback can throw arbitrary runtime failures; preserve the host and cleanup boundary.
    @Suppress("TooGenericExceptionCaught")
    private fun foreground(plan: Plan, owner: Any, holder: Any, kind: Kind) {
        val layout = kind == Kind.LAYOUT
        val dark = night((if (layout) plan.layoutContext else plan.context).get(owner) as Context)
        val tiny = plan.tiny.invoke(null, dark) as Boolean
        val textValues = intArrayOf(color(dark, if (tiny) "flip_media_primary_text" else "media_primary_text"),
            color(dark, if (tiny) "flip_media_secondary_text" else "media_secondary_text"),
            color(dark, "media_duration_time_font_color"), color(dark, "media_duration_time_font_color"))
        val changes = ArrayList<Change>()
        for (i in 0 until if (layout) 2 else plan.texts.size) {
            val view = plan.texts[i].get(holder) as TextView
            changes.add(Change("text$i", view, view.textColors, ColorStateList.valueOf(textValues[i]), { (it as TextView).textColors }, { v, c -> (v as TextView).setTextColor(c as ColorStateList) }))
        }
        if (!layout) {
        val icons = (plan.actions.invoke(holder) as List<*>).map { it as ImageView } + (plan.seamless.get(holder) as ImageView)
        for ((i, view) in icons.withIndex()) {
            val value = ColorStateList.valueOf(if (i == icons.lastIndex) -1 else color(dark, "notification_media_action_button_color"))
            changes.add(Change("icon$i", view, view.imageTintList, value, { (it as ImageView).imageTintList }, { v, c -> (v as ImageView).imageTintList = c as ColorStateList? }))
        }
        val seek = plan.seek.get(holder)
        val seekColors = intArrayOf(-1, color(dark, "media_seekbar_progress_disable_color"), color(dark, "media_seekbar_background_color"), color(dark, "media_seekbar_background_color"))
        for (i in plan.seekFields.indices) {
            val field = plan.seekFields[i]; val setter = plan.seekSetters[i]
            changes.add(Change("seek$i", seek, field.getInt(seek), seekColors[i], { field.getInt(it) }, { v, c -> setter.invoke(v, c) }))
        }
        }
        // The complete original method just rewrote these same properties. Capture
        // that current baseline, not an earlier density/theme generation's colors.
        val journal = if (layout) (plan.colors[holder].orEmpty().filter { it.key != "text0" && it.key != "text1" } + changes).toMutableList() else changes
        plan.colors[holder] = journal
        try { for (change in changes) {
            check(plan.active.get() && plan.current(owner, kind) === holder && plan.colors[holder] === journal) { "Media night holder changed" }
            change.apply()
        } } catch (failure: Exception) {
            if (plan.colors[holder] === journal) clean(changes.asReversed().map { { it.undo() } })
            throw failure
        }
    }

    private fun night(context: Context): Context {
        val config = context.resources.configuration
        if ((config.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) return context
        return context.createConfigurationContext(Configuration(config).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_YES
        })
    }
    private fun color(context: Context, name: String): Int {
        val id = context.resources.getIdentifier(name, "color", "com.android.systemui")
        check(id != 0) { "OS4 media color unavailable: $name" }
        return context.getColor(id)
    }
    private fun main() = Looper.myLooper() === Looper.getMainLooper()
    private fun required(name: String) = checkNotNull(findClassIfExists(name)) { "OS4 media night class unavailable: $name" }
    private fun field(type: Class<*>, name: String, expected: Class<*>) = type.getDeclaredField(name).also {
        check(it.type == expected && !Modifier.isStatic(it.modifiers)); it.isAccessible = true
    }
    private fun method(type: Class<*>, name: String, result: Class<*>, vararg args: Class<*>) = type.getDeclaredMethod(name, *args).also {
        check(it.returnType == result && !Modifier.isStatic(it.modifiers)); it.isAccessible = true
    }
    private fun staticMethod(type: Class<*>, name: String, result: Class<*>, vararg args: Class<*>) = type.getDeclaredMethod(name, *args).also {
        check(it.returnType == result && Modifier.isStatic(it.modifiers)); it.isAccessible = true
    }
    // ROM calls and rollback can throw arbitrary runtime failures; preserve the host and cleanup boundary.
    @Suppress("TooGenericExceptionCaught")
    private fun clean(actions: List<() -> Unit>) {
        var failure: IllegalStateException? = null
        for (action in actions) try { action() } catch (error: Exception) {
            if (failure == null) failure = IllegalStateException("Media night restoration failed")
            failure.addSuppressed(error)
        }
        failure?.let { throw it }
    }
}
