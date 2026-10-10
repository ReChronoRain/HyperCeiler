/*
 * This file is part of HyperCeiler, licensed under AGPL-3.0-or-later.
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.media3

import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.os.PowerManager
import android.view.ViewTreeObserver
import android.widget.TextView
import android.widget.SeekBar
import android.widget.ImageButton
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getAdditionalInstanceFieldAs
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.api.HostExecutor
import io.github.libxposed.api.XposedInterface
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook
import java.lang.ref.WeakReference
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.ArrayDeque
import java.util.WeakHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** OS4 view ownership is local to one controller's current attach generation. */
// Keep this owner-specific lifecycle and its private rollback helpers in one auditable scope.
@Suppress("TooManyFunctions")
object CustomBackgroundOS4 : BaseHook() {
    private const val NC = "com.android.systemui.statusbar.notification.mediacontrol."
    private const val DI = "com.android.systemui.statusbar.notification.mediaisland."
    private const val NP = "system_ui_control_center_media_control"
    private const val DP = "system_ui_island_media_control"
    private enum class Event { ATTACH, DETACH, BIND, FOREGROUND, AOD, PULL, LAYOUT }

    // ROM calls and rollback can throw arbitrary runtime failures; preserve the host and cleanup boundary.
    @Suppress("TooGenericExceptionCaught")
    override fun init() {
        val ncStyle = PrefsBridge.getStringAsInt(NP + "_background_mode", 0)
        val diStyle = if (CustomBackground.isIsland) PrefsBridge.getStringAsInt(DP + "_background_mode", 0) else 0
        val notification = ncStyle in 1..4
        val island = diStyle in 1..4
        if (!notification && !island) return
        val active = AtomicBoolean(false)
        val handles = ArrayList<XposedInterface.HookHandle>()
        val groups = ArrayList<Group>()
        try {
            val loader = checkNotNull(required(if (notification) NC + "MiuiMediaViewControllerImpl" else DI + "MiuiIslandMediaViewBinderImpl").classLoader)
            val styles = listOfNotNull(ncStyle.takeIf { notification }, diStyle.takeIf { island }).toSet()
            val renderer = checkNotNull(CustomBackgroundRendererOS4.create(loader, styles)) { "OS4 background renderer ABI unavailable" }
            if (notification) groups.add(plan(false, ncStyle, active, renderer))
            if (island) groups.add(plan(true, diStyle, active, renderer))
            val shader = MediaSeekBarColorOS4.create(required("miuix.miuixbasewidget.widget.HyperProgressSeekBar"))
            // All selected endpoints preflight before the first registration. A
            // partially installed transaction stays inert, including failed unhooks.
            for (group in groups) for ((method, event) in group.methods) {
                handles.add(checkNotNull(hookMethod(method, Callback(group, event))) { "OS4 custom background registration failed" })
            }
            handles.add(checkNotNull(hookMethod(shader.method, shader.callback({ seek ->
                if (active.get()) shaderColor(groups, seek) else null
            }, { debugCallbackError("custom background shader", it) }))) { "OS4 custom background shader registration failed" })
            registerHotReloadCleanup {
                active.set(false)
                val cleanup = Runnable { cleanAll(groups.map { { it.clear() } }) }
                if (mainThread()) cleanup.run() else {
                    val finished = CountDownLatch(1)
                    val failure = arrayOfNulls<Throwable>(1)
                    check(Handler(Looper.getMainLooper()).post {
                        try { cleanup.run() } catch (error: Throwable) { failure[0] = error } finally { finished.countDown() }
                    }) { "Custom background cleanup could not reach main thread" }
                    check(finished.await(500, TimeUnit.MILLISECONDS)) { "Custom background cleanup has not completed" }
                    failure[0]?.let { throw IllegalStateException("Custom background cleanup failed", it) }
                }
            }
            active.set(true)
        } catch (error: Throwable) {
            active.set(false)
            for (handle in handles.asReversed()) try { handle.unhook() } catch (rollback: Throwable) { error.addSuppressed(rollback) }
            throw error
        }
    }

    // This immutable contract records the complete validated ROM shape or captured invocation.
    @Suppress("LongParameterList")
    private class Group(
        val island: Boolean, val style: Int, val options: CustomBackgroundRendererOS4.Options,
        val active: AtomicBoolean, val renderer: CustomBackgroundRendererOS4, val owner: Class<*>,
        val holder: Class<*>, val holders: List<Field>, val background: Field, val context: Field,
        val media: Field, val playing: Field, val aod: Field?, val layout: Constructor<*>,
        val edges: List<Field>, val collector: Class<*>?, val collectorOwner: Field?,
        val collectorHolders: List<Field>, val pair: Class<*>?, val pairFirst: Method?,
        val textFields: List<Field>, val iconFields: List<Field>, val seek: Field, val seekColors: List<Pair<Field, Method>>,
        val layoutOwner: Class<*>?, val layoutController: Field?, val layoutHolder: Field?,
        val methods: List<Pair<Method, Event>>
    ) {
        // Neither the value nor listeners retain a controller/holder through a strong reference.
        val states = WeakHashMap<Any, State>()
        fun state(receiver: Any) = states.getOrPut(receiver) { State() }
        val sessions = ArrayList<WeakReference<Session>>()
        var screenReceiver: BroadcastReceiver? = null
        var screenContext = WeakReference<Context>(null)
        fun clear() {
            cleanAll(sessions.mapNotNull { it.get() }.distinct().map { session -> { retire(session) } } + listOf({
                screenReceiver?.let { screenContext.get()?.unregisterReceiver(it) }
                screenReceiver = null; screenContext.clear()
            }))
            states.clear(); sessions.clear()
        }
        fun fullAod(): Boolean = aod?.getBoolean(null) == true
        fun actualHolders(receiver: Any) = holders.map { it.get(receiver) }
    }
    private class State { var structure = 0L; var busy = 0; var request = 0L; var session: Session? = null; var pending: Pending? = null }
    private class Pending(val ticket: Long, values: List<Any?>) { val holders = values.map { WeakReference(it) } }
    private class Outcome(val frames: List<CustomBackgroundRendererOS4.Frame>?)
    private class Session(val group: Group, owner: Any, val state: State, values: List<Any?>) {
        val owner = WeakReference(owner)
        val holders = values.map { WeakReference(it) }
        val live = AtomicBoolean(true)
        val taskTag = Any()
        val slots = ArrayList<Slot>()
        var data = WeakReference<Any>(null)
        var frames: List<CustomBackgroundRendererOS4.Frame>? = null
        var dragging = false
        var fullAod = group.fullAod()
        var installing = false
        var pendingResult: (() -> Unit)? = null
    }
    private class Slot(holder: Any, original: View, parent: ViewGroup, val index: Int, val role: Int, val background: Field) {
        val holder = WeakReference(holder)
        val original = WeakReference(original)
        val parent = WeakReference(parent)
        var overlay = WeakReference<ImageView>(null)
        var drawable = WeakReference<CustomBackgroundDrawableOS4>(null)
        val colors = ArrayList<OwnedColor>()
        var preDraw: ViewTreeObserver.OnPreDrawListener? = null
        var observer = WeakReference<ViewTreeObserver>(null)
        var ownsTransition = false
        var transitionBefore = 1f
        var renderWidth = 0
        var renderHeight = 0
        var layoutListener: View.OnLayoutChangeListener? = null
        var attachListener: View.OnAttachStateChangeListener? = null
        var syncing = false
    }
    // This immutable contract records the complete validated ROM shape or captured invocation.
    @Suppress("LongParameterList")
    private class Frame(val receiver: Any?, val args: Array<Any?>, val state: State?, val session: Session?, val ticket: Long, val eligible: Boolean, var ownsBusy: Boolean)

    private class Callback(private val group: Group, private val event: Event) : IMethodHook {
        private val frames = ThreadLocal<ArrayDeque<Frame>>()
        // ROM calls and rollback can throw arbitrary runtime failures; preserve the host and cleanup boundary.
        @Suppress("TooGenericExceptionCaught")
        override fun before(param: HookParam) {
            var state: State? = null
            var eligible = false
            try {
                eligible = group.active.get() && !param.isSkipped && mainThread() &&
                    param.thisObject.javaClass == when (event) { Event.PULL -> group.collector; Event.LAYOUT -> group.layoutOwner; else -> group.owner }
                if (eligible) {
                    state = captureState(param)
                    eligible = state != null
                }
            } catch (error: Exception) { eligible = false; debugCallbackError("custom background before", error) }
            val stack = frames.get() ?: ArrayDeque<Frame>().also(frames::set)
            stack.addLast(Frame(param.thisObject, param.args, state, state?.session, state?.structure ?: -1, eligible, eligible && state != null && (event == Event.ATTACH || event == Event.DETACH)))
        }

        private fun captureState(param: HookParam): State? {
            val owner = when (event) { Event.PULL -> group.collectorOwner!!.get(param.thisObject); Event.LAYOUT -> group.layoutController!!.get(param.thisObject); else -> param.thisObject }
            if (owner?.javaClass != group.owner) return null
            val state = group.state(owner)
            if (event == Event.ATTACH || event == Event.DETACH) {
                state.structure++
                state.busy++
                state.pending = null
            }
            return state
        }

        // ROM calls and rollback can throw arbitrary runtime failures; preserve the host and cleanup boundary.
        @Suppress("TooGenericExceptionCaught")
        override fun after(param: HookParam) {
            val stack = frames.get() ?: return
            val frame = stack.pollLast()
            if (stack.isEmpty()) frames.remove()
            if (frame == null || frame.receiver !== param.thisObject || frame.args !== param.args) {
                frame?.let(::releaseFrame)
                stack.forEach(::releaseFrame)
                frame?.state?.pending = null
                stack.forEach { it.state?.pending = null }
                stack.clear(); frames.remove(); return
            }
            val state = frame.state ?: return
            releaseFrame(frame)
            if (!frame.eligible || !group.active.get() || !mainThread()) return
            try {
                val owner = when (event) { Event.PULL -> group.collectorOwner!!.get(param.thisObject); Event.LAYOUT -> group.layoutController!!.get(param.thisObject); else -> param.thisObject }
                if (owner == null || owner.javaClass != group.owner) return
                if (param.isSkipped || param.hasThrowable) {
                    // An original that throws after clearing/replacing its holder
                    // still loses authority over the captured old generation.
                    retireChangedSession(frame, state, owner)
                    return
                }
                handleEvent(param, frame, state, owner)
            } catch (error: Exception) {
                state.session?.let { try { failVisual(it) } catch (cleanup: Exception) { error.addSuppressed(cleanup) } }
                debugCallbackError("custom background $event", error)
            }
            finally {
                commitPendingAttach(param, state)
            }
        }

        private fun retireChangedSession(frame: Frame, state: State, owner: Any) {
            val session = frame.session ?: return
            if (matches(session, owner)) return
            retire(session)
            if (state.session === session) state.session = null
        }

        private fun commitPendingAttach(param: HookParam, state: State) {
            if (event != Event.ATTACH && event != Event.DETACH) return
            if (state.busy != 0) return
            guarded("custom background attach commit") {
                param.thisObject?.let { commitAttach(group, state, it) }
            }
        }

        private fun handleEvent(param: HookParam, frame: Frame, state: State, owner: Any) {
            when (event) {
                Event.ATTACH -> {
                    if (state.structure == frame.ticket) {
                        val values = group.actualHolders(owner)
                        if (values.indices.all { values[it] === param.args.getOrNull(it) }) state.pending = Pending(frame.ticket, values)
                    }
                }
                Event.DETACH -> {
                    frame.session?.let(::retire)
                    if (state.session === frame.session) state.session = null
                }
                Event.BIND -> {
                    val session = state.session ?: return
                    if (!current(session, owner) || state.busy != 0) return
                    val data = param.args.getOrNull(0)
                    if (data == null) {
                        // DI accepts a null bind and clears mediaData; NC's
                        // null bind is a no-op. Only the actual cleared owner
                        // revokes the current palette and pending result.
                        if (group.media.get(owner) == null) clearMedia(session, owner)
                        return
                    }
                    if (data.javaClass != group.renderer.mediaDataClass || group.media.get(owner) !== data) return
                    session.data = WeakReference(data)
                    sync(session)
                    requestColor(session, owner, data)
                }
                Event.FOREGROUND, Event.AOD, Event.LAYOUT -> state.session?.let {
                    if (state.busy == 0 && (event != Event.LAYOUT || group.layoutHolder!!.get(param.thisObject) === it.holders[0].get())) {
                        applyForeground(it); sync(it)
                    }
                }
                Event.PULL -> {
                    val session = state.session ?: return
                    if (!current(session, owner) || state.busy != 0 || group.collectorHolders.indices.any {
                            group.collectorHolders[it].get(param.thisObject) !== session.holders[it].get()
                        }) return
                    val pair = param.args.getOrNull(0) ?: return
                    if (!group.pair!!.isInstance(pair)) return
                    when (group.pairFirst!!.invoke(pair)) {
                        "pull_down_type_start" -> session.dragging = true
                        "pull_down_type_finish" -> session.dragging = false
                        "pull_down_type_update" -> Unit // Original code owns dummy translation and bounds.
                        else -> return
                    }
                    sync(session)
                }
            }
        }
    }

    private fun releaseFrame(frame: Frame) {
        if (frame.ownsBusy) { frame.ownsBusy = false; frame.state?.let { it.busy = (it.busy - 1).coerceAtLeast(0) } }
    }

    // ROM calls and rollback can throw arbitrary runtime failures; preserve the host and cleanup boundary.
    @Suppress("TooGenericExceptionCaught")
    private fun commitAttach(group: Group, state: State, owner: Any) {
        val pending = state.pending ?: return
        state.pending = null
        if (!group.active.get() || state.busy != 0) return
        if (state.structure != pending.ticket || owner.javaClass != group.owner) return
        val values = group.actualHolders(owner)
        if (values.indices.any { values[it] !== pending.holders[it].get() }) return
        val previous = state.session
        previous?.let(::retire)
        // Removing an owned old view can reenter attach. That newer successful
        // generation must survive this outer commit's return.
        if (state.structure != pending.ticket || state.busy != 0) return
        if (state.session !== previous || values.indices.any { group.holders[it].get(owner) !== values[it] }) return
        state.session = null
        val session = Session(group, owner, state, values)
        for ((role, value) in values.withIndex()) {
            if (value == null || session.slots.any { it.holder.get() === value }) continue
            if (value.javaClass != group.holder) return
            val original = group.background.get(value) as? View ?: return
            val parent = original.parent as? ViewGroup ?: return
            session.slots.add(Slot(value, original, parent, parent.indexOfChild(original), role, group.background))
        }
        if (session.slots.isEmpty()) return
        state.session = session
        group.sessions.removeAll { it.get() == null }
        group.sessions.add(WeakReference(session))
        try {
            watchScreen(session, group.context.get(owner) as Context)
            val data = group.media.get(owner)
            if (data != null && data.javaClass == group.renderer.mediaDataClass && current(session, owner)) {
                session.data = WeakReference(data)
                requestColor(session, owner, data)
            }
        } catch (error: Exception) {
            try { retire(session) } catch (cleanup: Exception) { error.addSuppressed(cleanup) }
            if (state.session === session) state.session = null
            throw error
        }
    }

    // ROM calls and rollback can throw arbitrary runtime failures; preserve the host and cleanup boundary.
    @Suppress("TooGenericExceptionCaught")
    private fun requestColor(session: Session, owner: Any, data: Any) {
        val group = session.group
        val context = group.context.get(owner) as? Context ?: return
        val ticket = ++session.state.request
        val renderer = group.renderer
        val sizes = session.slots.map { slot ->
            val width = slot.original.get()?.width ?: 0
            val height = slot.original.get()?.height ?: 0
            slot.renderWidth = width; slot.renderHeight = height
            width to height
        }
        fun eligible(): Boolean {
            val receiver = session.owner.get() ?: return false
            return group.active.get() && mainThread() && current(session, receiver) && session.state.busy == 0 &&
                session.state.request == ticket && group.media.get(receiver) === data && session.data.get() === data
        }
        try {
            HostExecutor.execute(session.taskTag, backgroundTask = {
                if (!group.active.get() || !session.live.get()) null else {
                    val frames = try {
                        val frame = renderer.render(context, data, group.style, group.options,
                            sizes.maxOf { it.first }, sizes.maxOf { it.second })
                        frame?.let { List(sizes.size) { frame } }
                    }
                    catch (error: Exception) { debugCallbackError("custom background resolver", error); null }
                    Outcome(frames)
                }
            }, onResult = { applyResult(session, it, ::eligible) })
        } catch (error: Exception) { applyResult(session, Outcome(null), ::eligible); debugCallbackError("custom background executor", error) }
    }

    private fun clearMedia(session: Session, owner: Any) {
        val ticket = ++session.state.request
        session.data.clear()
        applyResult(session, Outcome(null)) {
            session.group.active.get() && mainThread() && current(session, owner) && session.state.busy == 0 &&
                session.state.request == ticket && session.group.media.get(owner) == null
        }
    }

    // ROM calls and rollback can throw arbitrary runtime failures; preserve the host and cleanup boundary.
    @Suppress("TooGenericExceptionCaught")
    private fun applyResult(session: Session, outcome: Outcome, eligible: () -> Boolean) {
        guarded("custom background color result") {
            if (!eligible()) return
            // add/removeView may synchronously reenter another bind/result.
            // Defer that latest result until the current view journal closes.
            if (session.installing) { session.pendingResult = { applyResult(session, outcome, eligible) }; return }
            session.installing = true
            try {
                val frames = outcome.frames
                if (frames == null) {
                    session.frames = null
                    cleanAll(session.slots.asReversed().map { slot -> { undo(slot) } })
                } else {
                    if (session.frames == null) installViews(session, frames, eligible)
                    check(eligible()) { "Background request changed before commit" }
                    for ((i, slot) in session.slots.withIndex()) {
                        val drawer = checkNotNull(slot.drawable.get())
                        frames[i].applyTo(drawer, slot.overlay.get()?.isShown == true)
                        check(eligible()) { "Background request changed during commit" }
                    }
                    session.frames = frames
                    applyForeground(session)
                    sync(session)
                }
            } catch (error: Exception) {
                session.frames = null
                try { cleanAll(session.slots.asReversed().map { slot -> { undo(slot) } }) }
                catch (cleanup: Exception) { error.addSuppressed(cleanup) }
                throw error
            } finally {
                session.installing = false
                val pending = session.pendingResult
                session.pendingResult = null
                pending?.invoke()
            }
        }
    }

    private fun installViews(session: Session, frames: List<CustomBackgroundRendererOS4.Frame>, eligible: () -> Boolean) {
        val group = session.group
        for ((i, slot) in session.slots.withIndex()) {
            ensureCurrent(session); check(eligible())
            val original = checkNotNull(slot.original.get())
            val parent = checkNotNull(slot.parent.get())
            check(original.parent === parent && slot.holder.get()?.let { group.background.get(it) === original } == true)
            val drawable = frames[i].newPresentation(!group.island)
            val view = ImageView(original.context)
            view.id = View.generateViewId()
            view.clipToOutline = original.clipToOutline
            view.outlineProvider = original.outlineProvider
            view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            view.setImageDrawable(drawable)
            drawable.setActive(false)
            val params = group.layout.newInstance(0, 0) as ViewGroup.LayoutParams
            group.edges.forEach { it.setInt(params, 0) }
            view.layoutParams = params
            slot.overlay = WeakReference(view)
            slot.drawable = WeakReference(drawable)
            val attach = object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) { guardSession("background view attach", session) { observeDraw(session, slot); sync(session) } }
                override fun onViewDetachedFromWindow(v: View) { guarded("background view detach") { slot.drawable.get()?.setActive(false); unobserveDraw(slot) } }
            }
            slot.attachListener = attach
            view.addOnAttachStateChangeListener(attach)
            val listener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                guardSession("background layout", session) {
                    sync(session)
                    renderIfSizeChanged(session, slot)
                }
            }
            slot.layoutListener = listener
            original.addOnLayoutChangeListener(listener)
            view.addOnLayoutChangeListener(listener)
            slot.renderWidth = original.width; slot.renderHeight = original.height
            ensureCurrent(session); check(eligible())
            val index = parent.indexOfChild(original)
            check(index >= 0) { "Original background no longer belongs to parent" }
            parent.addView(view, index + 1)
            ensureCurrent(session); check(eligible())
            observeDraw(session, slot)
            followBounds(slot)
        }
    }

    private fun renderIfSizeChanged(session: Session, slot: Slot) {
        if (slot.syncing) return
        val owner = session.owner.get() ?: return
        val data = session.data.get() ?: return
        val measured = slot.original.get() ?: return
        if (!current(session, owner)) return
        if (measured.width != slot.renderWidth || measured.height != slot.renderHeight) {
            requestColor(session, owner, data)
        }
    }

    private fun observeDraw(session: Session, slot: Slot) {
        val view = slot.overlay.get() ?: return
        val observer = view.viewTreeObserver
        if (slot.observer.get() === observer && slot.preDraw != null) return
        unobserveDraw(slot)
        val listener = ViewTreeObserver.OnPreDrawListener { guardSession("background pre-draw", session) { sync(session) }; true }
        slot.preDraw = listener; slot.observer = WeakReference(observer)
        observer.addOnPreDrawListener(listener)
    }
    private fun unobserveDraw(slot: Slot) {
        val observer = slot.observer.get()
        slot.preDraw?.let { if (observer?.isAlive == true) observer.removeOnPreDrawListener(it) }
        slot.preDraw = null; slot.observer.clear()
    }

    private fun followBounds(slot: Slot) {
        if (slot.syncing || !mainThread()) return
        val original = slot.original.get() ?: return
        val view = slot.overlay.get() ?: return
        val parent = slot.parent.get() ?: return
        if (original.parent !== parent || view.parent !== parent) return
        slot.syncing = true
        try {
            val params = view.layoutParams
            if (params.width != original.width || params.height != original.height) {
                params.width = original.width; params.height = original.height; view.layoutParams = params
            }
            if (view.x != original.x) view.x = original.x
            if (view.y != original.y) view.y = original.y
            val transition = original.transitionAlpha
            if (!slot.ownsTransition || transition != 0f) slot.transitionBefore = transition
            slot.ownsTransition = true
            if (transition != 0f) original.transitionAlpha = 0f
            val alpha = original.alpha * slot.transitionBefore
            if (view.alpha != alpha) view.alpha = alpha
            if (view.visibility != original.visibility) view.visibility = original.visibility
            if (view.scaleX != original.scaleX) view.scaleX = original.scaleX
            if (view.scaleY != original.scaleY) view.scaleY = original.scaleY
            if (view.pivotX != original.pivotX) view.pivotX = original.pivotX
            if (view.pivotY != original.pivotY) view.pivotY = original.pivotY
            if (view.rotation != original.rotation) view.rotation = original.rotation
            if (view.outlineProvider !== original.outlineProvider) view.outlineProvider = original.outlineProvider
            if (view.clipToOutline != original.clipToOutline) view.clipToOutline = original.clipToOutline
        } finally { slot.syncing = false }
    }

    private fun sync(session: Session) {
        val group = session.group
        if (!mainThread() || session.state.busy != 0) return
        if (session.installing && session.frames == null) return
        val owner = session.owner.get()
        if (!group.active.get() || owner == null || !current(session, owner)) { retire(session); return }
        val data = group.media.get(owner)
        val playing = data != null && group.playing.get(data) == true
        val aod = group.fullAod()
        val context = group.context.get(owner) as? Context ?: return
        val interactive = (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isInteractive == true
        for (slot in session.slots) {
            val view = slot.overlay.get() ?: continue
            val drawer = slot.drawable.get() ?: continue
            val original = slot.original.get()
            if (!ownsBackground(slot, view, drawer, original)) {
                session.frames = null
                cleanAll(session.slots.asReversed().map { owned -> { undo(owned) } })
                return
            }
            followBounds(slot)
            drawer.setActive(playing && interactive && !aod && (!session.dragging || slot.role != 0) && view.isAttachedToWindow && view.isShown && view.alpha > 0f)
        }
    }

    // ROM calls and rollback can throw arbitrary runtime failures; preserve the host and cleanup boundary.
    @Suppress("TooGenericExceptionCaught")
    private fun ownsBackground(slot: Slot, view: ImageView, drawer: CustomBackgroundDrawableOS4, original: View?): Boolean {
        if (view.parent !== slot.parent.get() || view.drawable !== drawer) return false
        if (original?.parent !== slot.parent.get()) return false
        return slot.holder.get()?.let { slot.background.get(it) === original } == true
    }

    // Receiver registration may fail after mutation; always attempt its matching rollback.
    @Suppress("TooGenericExceptionCaught")
    private fun watchScreen(session: Session, context: Context) {
        val group = session.group
        if (group.screenReceiver != null) return
        val app = checkNotNull(context.applicationContext) { "Background application context unavailable" }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action != Intent.ACTION_SCREEN_ON && intent?.action != Intent.ACTION_SCREEN_OFF) return
                // Registry remains discoverable after WeakHashMap drops a dead
                // controller. Its values are weak; only attached views/tasks own
                // live sessions. No receiver closes over one session.
                for (item in group.sessions.toList()) item.get()?.let { current ->
                    guardSession("background screen", current) {
                        if (current.owner.get() == null) retire(current) else sync(current)
                    }
                }
                group.sessions.removeAll { it.get() == null }
            }
        }
        group.screenContext = WeakReference(app); group.screenReceiver = receiver
        try {
            app.registerReceiver(receiver, IntentFilter().apply { addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_SCREEN_OFF) }, Context.RECEIVER_NOT_EXPORTED)
        } catch (error: Exception) {
            try { app.unregisterReceiver(receiver) } catch (cleanup: Exception) { error.addSuppressed(cleanup) }
            group.screenReceiver = null; group.screenContext.clear()
            throw error
        }
    }
    private fun undo(slot: Slot) {
        val original = slot.original.get()
        val view = slot.overlay.get()
        val parent = slot.parent.get()
        cleanAll(listOf(
            { slot.drawable.get()?.dispose(); Unit },
            { restoreColors(slot) },
            {
                if (slot.ownsTransition && original != null && original.transitionAlpha == 0f)
                    original.transitionAlpha = slot.transitionBefore
                slot.ownsTransition = false
            },
            { unobserveDraw(slot) },
            { slot.layoutListener?.let { original?.removeOnLayoutChangeListener(it) }; Unit },
            { slot.layoutListener?.let { view?.removeOnLayoutChangeListener(it) }; Unit },
            { slot.attachListener?.let { view?.removeOnAttachStateChangeListener(it) }; Unit },
            { removeOwnedOverlay(view, parent) }
        ))
        slot.overlay.clear(); slot.drawable.clear()
        slot.layoutListener = null; slot.attachListener = null
    }
    private fun removeOwnedOverlay(view: ImageView?, parent: ViewGroup?) {
        if (view != null && view.parent === parent) parent?.removeView(view)
    }

    private fun retire(session: Session) {
        if (session.live.getAndSet(false)) session.state.request++
        session.frames = null
        session.data.clear()
        session.pendingResult = null
        cleanAll(session.slots.asReversed().map { slot -> { undo(slot) } })
    }
    private fun failVisual(session: Session) {
        session.state.request++
        session.frames = null
        cleanAll(session.slots.asReversed().map { slot -> { undo(slot) } })
    }
    // ROM calls and rollback can throw arbitrary runtime failures; preserve the host and cleanup boundary.
    @Suppress("TooGenericExceptionCaught")
    private inline fun guardSession(where: String, session: Session, action: () -> Unit) {
        try { action() } catch (error: Exception) {
            try { failVisual(session) } catch (cleanup: Exception) { error.addSuppressed(cleanup) }
            debugCallbackError(where, error)
        }
    }
    // ROM calls and rollback can throw arbitrary runtime failures; preserve the host and cleanup boundary.
    @Suppress("TooGenericExceptionCaught")
    private fun cleanAll(actions: List<() -> Unit>) {
        var failure: IllegalStateException? = null
        for (action in actions) try { action() } catch (error: Throwable) {
            if (failure == null) failure = IllegalStateException("Custom background owned cleanup incomplete")
            failure.addSuppressed(error)
        }
        failure?.let { throw it }
    }
    private fun ensureCurrent(session: Session) {
        val owner = session.owner.get()
        check(session.group.active.get() && owner != null && current(session, owner) && session.state.busy == 0) { "Custom background generation changed" }
    }
    private fun matches(session: Session, owner: Any): Boolean = session.group.holders.indices.all {
        session.group.holders[it].get(owner) === session.holders[it].get()
    }
    private fun current(session: Session, owner: Any): Boolean = session.live.get() &&
        session.state.session === session && session.owner.get() === owner && matches(session, owner)

    // The journal holds weak view identities and value-only colors. No lambda
    // closes over a holder/controller, including alternate module SeekBars.
    private class OwnedColor(target: Any, val kind: Int, val read: Field? = null, val write: Method? = null) {
        val target = WeakReference(target)
        var before: Any? = null
        var applied: Any? = null
        var owns = false
        fun get(value: Any): Any? = when (kind) {
            0 -> (value as TextView).textColors
            1 -> (value as ImageView).imageTintList
            2 -> (value as SeekBar).thumbTintList
            3 -> (value as SeekBar).progressTintList
            4 -> (value as SeekBar).progressBackgroundTintList
            else -> read!!.getInt(value)
        }
        fun set(value: Any, color: Any?) {
            when (kind) {
                0 -> (value as TextView).setTextColor(color as ColorStateList)
                1 -> (value as ImageView).imageTintList = color as ColorStateList?
                2 -> (value as SeekBar).thumbTintList = color as ColorStateList?
                3 -> (value as SeekBar).progressTintList = color as ColorStateList?
                4 -> (value as SeekBar).progressBackgroundTintList = color as ColorStateList?
                else -> write!!.invoke(value, color)
            }
        }
        fun matches(a: Any?, b: Any?): Boolean = if (kind >= 5) a == b else a === b
        fun apply(color: Any) {
            val value = target.get() ?: return
            val current = get(value)
            // An OEM foreground update has just completed. Its new value, not
            // the previous module tint, becomes the restoration baseline.
            if (!owns || !matches(current, applied)) before = current
            applied = color; owns = true // publish before a setter can reenter/throw
            set(value, color)
        }
        fun restore() {
            val value = target.get()
            if (owns && value != null && matches(get(value), applied)) set(value, before)
            owns = false; before = null; applied = null
        }
    }
    // This immutable contract records the complete validated ROM shape or captured invocation.
    @Suppress("LongParameterList")
    private fun tint(slot: Slot, target: Any, kind: Int, color: Any, read: Field? = null, write: Method? = null) {
        val entry = slot.colors.firstOrNull { it.target.get() === target && it.kind == kind }
            ?: OwnedColor(target, kind, read, write).also(slot.colors::add)
        entry.apply(color)
    }
    private fun applyForeground(session: Session) {
        val frames = session.frames ?: return
        val owner = session.owner.get() ?: return
        if (!current(session, owner) || !mainThread() || session.state.busy != 0) return
        val group = session.group
        for ((index, slot) in session.slots.withIndex()) {
            val holder = slot.holder.get() ?: continue
            val overlay = slot.overlay.get() ?: continue
            if (overlay.parent !== slot.parent.get()) continue
            val color = frames[index].colors
            val primary = ColorStateList.valueOf(color.textPrimary)
            val secondary = ColorStateList.valueOf(color.textSecondary)
            group.textFields.forEachIndexed { i, field -> (field.get(holder) as? TextView)?.let { tint(slot, it, 0, if (i == 1) secondary else primary) } }
            group.iconFields.forEach { field -> (field.get(holder) as? ImageView)?.let { tint(slot, it, 1, primary) } }
            val seek = group.seek.get(holder)
            if (seek != null) {
                val alpha = color.textPrimary ushr 24
                val disabled = (color.textPrimary and 0x00ffffff) or ((alpha * 0.3f).toInt() shl 24)
                val background = (color.textPrimary and 0x00ffffff) or ((alpha * 0.2f).toInt() shl 24)
                val values = listOf(color.textPrimary, disabled, background, background)
                group.seekColors.forEachIndexed { i, (field, setter) -> tint(slot, seek, 5 + i, values[i], field, setter) }
            }
            holder.getAdditionalInstanceFieldAs<SeekBar?>(CustomBackground.KEY_REAL_PROGRESS_BAR)?.let { alternate ->
                if (alternate.parent != null) {
                    tint(slot, alternate, 2, primary); tint(slot, alternate, 3, primary)
                    tint(slot, alternate, 4, ColorStateList.valueOf((color.textPrimary and 0x00ffffff) or (0x33 shl 24)))
                }
            }
        }
    }
    private fun shaderColor(groups: List<Group>, seek: Any): Int? {
        for (group in groups) {
            for (state in group.states.values.toList()) {
                val session = state.session ?: continue
                colorForSeek(session, seek)?.let { return it }
            }
        }
        return null
    }

    private fun colorForSeek(session: Session, seek: Any): Int? {
        val owner = session.owner.get() ?: return null
        val frames = session.frames ?: return null
        if (session.state.busy != 0 || !current(session, owner)) return null
        for ((index, slot) in session.slots.withIndex()) {
            val holder = slot.holder.get() ?: continue
            val overlay = slot.overlay.get() ?: continue
            if (overlay.parent === slot.parent.get() && session.group.seek.get(holder) === seek) {
                return frames[index].colors.textPrimary
            }
        }
        return null
    }

    private fun restoreColors(slot: Slot) {
        cleanAll(slot.colors.asReversed().map { entry -> { entry.restore() } })
        slot.colors.clear()
    }

    // Reflection consumes the exact validated argument array once during registration.
    @Suppress("SpreadOperator")
    private fun plan(island: Boolean, style: Int, active: AtomicBoolean, renderer: CustomBackgroundRendererOS4): Group {
        val owner = required(if (island) DI + "MiuiIslandMediaViewBinderImpl" else NC + "MiuiMediaViewControllerImpl")
        val holder = required(if (island) DI + "MiuiIslandMediaViewHolder" else NC + "MiuiMediaViewHolder")
        val holders = (if (island) listOf("holder", "dummyHolder") else listOf("holder")).map { field(owner, it, holder) }
        val background = ownerField(holder, if (island) "mediaBgView" else "mediaBg")
        check(View::class.java.isAssignableFrom(background.type) && !Modifier.isStatic(background.modifiers))
        val context = field(owner, "context", Context::class.java)
        val data = renderer.mediaDataClass
        val media = field(owner, "mediaData", data)
        val playing = field(data, "isPlaying", java.lang.Boolean::class.java)
        val methods = arrayListOf(method(owner, "attach", Void.TYPE, *(if (island) arrayOf(holder, holder) else arrayOf(holder))) to Event.ATTACH,
            method(owner, "detach", Void.TYPE) to Event.DETACH,
            method(owner, "bindMediaData", Void.TYPE, data) to Event.BIND,
            method(owner, "updateForegroundColors", Void.TYPE, *(if (island) arrayOf(holder) else emptyArray())) to Event.FOREGROUND)
        val params = required("androidx.constraintlayout.widget.ConstraintLayout\$LayoutParams")
        check(ViewGroup.LayoutParams::class.java.isAssignableFrom(params))
        val layout = params.getDeclaredConstructor(Int::class.java, Int::class.java).also { it.isAccessible = true }
        val edges = listOf("startToStart", "endToEnd", "topToTop", "bottomToBottom").map { field(params, it, Int::class.java) }
        var aod: Field? = null; var collector: Class<*>? = null; var collectorOwner: Field? = null
        var collectorHolders = emptyList<Field>(); var pair: Class<*>? = null; var first: Method? = null
        var layoutOwner: Class<*>? = null; var layoutController: Field? = null; var layoutHolder: Field? = null
        if (island) {
            collector = required(DI + "MiuiIslandMediaViewBinderImpl\$attach\$4\$1")
            collectorOwner = field(collector, "this\$0", owner)
            collectorHolders = listOf("\$holder", "\$dummyHolder").map { field(collector, it, holder) }
            pair = required("kotlin.Pair")
            first = method(pair, "getFirst", Any::class.java)
            methods.add(method(collector, "emit", Any::class.java, Any::class.java, required("kotlin.coroutines.Continuation")) to Event.PULL)
        } else {
            aod = ownerField(required("com.android.systemui.statusbar.notification.fullaod.NotifiFullAodController"), "mEnableFullAod")
            check(aod.type == Boolean::class.java && Modifier.isStatic(aod.modifiers) && Modifier.isVolatile(aod.modifiers))
            methods.add(method(owner, "onFullAodStateChanged", Void.TYPE, Boolean::class.java) to Event.AOD)
            layoutOwner = required(NC + "MiuiMediaNotificationControllerImpl")
            layoutController = field(layoutOwner, "mediaViewController", owner)
            layoutHolder = field(layoutOwner, "mediaViewHolder", holder)
            methods.add(method(layoutOwner, "updateLayout\$1", Void.TYPE) to Event.LAYOUT)
        }
        val texts = listOf("titleText", "artistText", "elapsedTimeView", "totalTimeView").map { field(holder, it, TextView::class.java) }
        val icons = listOf("action0", "action1", "action2", "action3", "action4").map { field(holder, it, ImageButton::class.java) } + field(holder, "seamlessIcon", ImageView::class.java)
        val seekClass = required("miuix.miuixbasewidget.widget.HyperProgressSeekBar")
        val seek = field(holder, "seekBar", seekClass)
        val seekColors = listOf("ForegroundPrimaryColor", "ForegroundPrimaryDisableColor", "BackgroundPrimaryColor", "BackgroundPrimaryDisableColor").map { name ->
            field(seekClass, "m" + name, Int::class.java) to method(seekClass, "set" + name, Void.TYPE, Int::class.java)
        }
        val prefix = if (island) DP else NP
        val options = CustomBackgroundRendererOS4.Options(PrefsBridge.getBoolean(prefix + "_control_color_anim"),
            PrefsBridge.getBoolean(prefix + "_inverse_color"), PrefsBridge.getInt(prefix + "_panel_background_blur", 10).coerceIn(0, 20))
        return Group(island, style, options, active, renderer, owner, holder, holders, background, context,
            media, playing, aod, layout, edges, collector, collectorOwner, collectorHolders, pair, first, texts, icons, seek, seekColors, layoutOwner, layoutController, layoutHolder, methods)
    }
    private fun required(name: String) = checkNotNull(findClassIfExists(name)) { "OS4 custom background class unavailable: $name" }
    private fun ownerField(type: Class<*>, name: String) = type.getDeclaredField(name).also { it.isAccessible = true }
    private fun field(type: Class<*>, name: String, expected: Class<*>) = ownerField(type, name).also {
        check(it.type == expected && !Modifier.isStatic(it.modifiers)) { "OS4 custom background field changed: $name" }
    }
    private fun method(type: Class<*>, name: String, result: Class<*>, vararg args: Class<*>) = type.getDeclaredMethod(name, *args).also {
        check(it.returnType == result && !Modifier.isStatic(it.modifiers)) { "OS4 custom background method changed: $name" }; it.isAccessible = true
    }
    // ROM calls and rollback can throw arbitrary runtime failures; preserve the host and cleanup boundary.
    @Suppress("TooGenericExceptionCaught")
    private inline fun guarded(where: String, action: () -> Unit) {
        try { action() } catch (error: Exception) { debugCallbackError(where, error) }
    }
    private fun mainThread() = Looper.myLooper() === Looper.getMainLooper()
}
