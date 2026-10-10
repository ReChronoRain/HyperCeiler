/*
 * This file is part of HyperCeiler, licensed under AGPL-3.0-or-later.
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.media3

import android.content.Context
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.api.HostExecutor
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.drawable.AmbientLightDrawable
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
object AmbientLightOS4 : BaseHook() {
    private const val NC = "com.android.systemui.statusbar.notification.mediacontrol."
    private const val DI = "com.android.systemui.statusbar.notification.mediaisland."
    private const val NP = "system_ui_control_center_media_control"
    private const val DP = "system_ui_island_media_control"
    private enum class Event { ATTACH, DETACH, BIND, FOREGROUND, AOD, PULL }

    // ROM calls and rollback can throw arbitrary runtime failures; preserve the host and cleanup boundary.
    @Suppress("TooGenericExceptionCaught")
    override fun init() {
        val notification = PrefsBridge.getStringAsInt(NP + "_background_mode", 0) == 0 &&
            PrefsBridge.getBoolean(NP + "_ambient_light")
        val mode = if (CustomBackground.isIsland && PrefsBridge.getStringAsInt(DP + "_background_mode", 0) == 0)
            PrefsBridge.getStringAsInt(DP + "_ambient_light", 0) else 0
        // Unknown imported modes do not enable a replacement.
        val island = mode == 1 || mode == 2
        if (!notification && !island) return
        val ncOpt = notification && PrefsBridge.getBoolean(NP + "_ambient_light_opt")
        val diOpt = mode == 2 && PrefsBridge.getBoolean(DP + "_ambient_light_opt")
        val active = AtomicBoolean(false)
        val handles = ArrayList<XposedInterface.HookHandle>()
        val groups = ArrayList<Group>()
        try {
            val loader = checkNotNull(required(if (notification) NC + "MiuiMediaViewControllerImpl" else DI + "MiuiIslandMediaViewBinderImpl").classLoader)
            val custom = notification || mode == 2
            val resolver = if (custom) checkNotNull(AmbientColorResolverOS4.create(loader,
                notification && !ncOpt || mode == 2 && !diOpt, ncOpt || diOpt)) { "OS4 ambient color ABI unavailable" } else null
            if (notification) groups.add(plan(false, 2, ncOpt, PrefsBridge.getBoolean(NP + "_always_dark"), active, resolver))
            if (island) groups.add(plan(true, mode, diOpt, false, active, resolver))
            // All selected endpoints preflight before the first registration. A
            // partially installed transaction stays inert, including failed unhooks.
            for (group in groups) for ((method, event) in group.methods) {
                handles.add(checkNotNull(hookMethod(method, Callback(group, event))) { "OS4 ambient registration failed" })
            }
            registerHotReloadCleanup {
                active.set(false)
                val cleanup = Runnable { cleanAll(groups.map { { it.clear() } }) }
                if (mainThread()) cleanup.run() else {
                    val finished = CountDownLatch(1)
                    val failure = arrayOfNulls<Throwable>(1)
                    check(Handler(Looper.getMainLooper()).post {
                        try { cleanup.run() } catch (error: Throwable) { failure[0] = error } finally { finished.countDown() }
                    }) { "Ambient cleanup could not reach main thread" }
                    check(finished.await(500, TimeUnit.MILLISECONDS)) { "Ambient cleanup has not completed" }
                    failure[0]?.let { throw IllegalStateException("Ambient cleanup failed", it) }
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
        val island: Boolean, val mode: Int, val optimized: Boolean, val alwaysDark: Boolean,
        val active: AtomicBoolean, val resolver: AmbientColorResolverOS4?, val owner: Class<*>,
        val holder: Class<*>, val holders: List<Field>, val background: Field, val context: Field,
        val media: Field?, val playing: Field?, val aod: Field?, val offset: Field?, val layout: Constructor<*>?,
        val edges: List<Field>, val collector: Class<*>?, val collectorOwner: Field?,
        val collectorHolders: List<Field>, val methods: List<Pair<Method, Event>>
    ) {
        // Neither the value nor listeners retain a controller/holder through a strong reference.
        val states = WeakHashMap<Any, State>()
        fun state(receiver: Any) = states.getOrPut(receiver) { State() }
        fun clear() { cleanAll(states.values.map { state -> { state.session?.let(::retire); state.session = null; state.pending = null } }); states.clear() }
        fun fullAod(): Boolean = aod?.getBoolean(null) == true
        fun actualHolders(receiver: Any) = holders.map { it.get(receiver) }
    }
    private class State { var structure = 0L; var busy = 0; var request = 0L; var session: Session? = null; var pending: Pending? = null }
    private class Pending(val ticket: Long, values: List<Any?>) { val holders = values.map { WeakReference(it) } }
    private class Outcome(val colors: AmbientColorResolverOS4.Colors?)
    private class Session(val group: Group, owner: Any, val state: State, values: List<Any?>) {
        val owner = WeakReference(owner)
        val holders = values.map { WeakReference(it) }
        val live = AtomicBoolean(true)
        val taskTag = Any()
        val slots = ArrayList<Slot>()
        var data = WeakReference<Any>(null)
        var colors: AmbientColorResolverOS4.Colors? = null
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
        var drawable = WeakReference<AmbientLightDrawable>(null)
        var removed = false
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
                    param.thisObject?.javaClass == if (event == Event.PULL) group.collector else group.owner
                if (eligible) {
                    state = captureState(param)
                    eligible = state != null
                }
            } catch (error: Exception) { eligible = false; debugCallbackError("ambient before", error) }
            val stack = frames.get() ?: ArrayDeque<Frame>().also(frames::set)
            stack.addLast(Frame(param.thisObject, param.args, state, state?.session, state?.structure ?: -1, eligible, eligible && state != null && (event == Event.ATTACH || event == Event.DETACH)))
        }

        private fun captureState(param: HookParam): State? {
            val owner = if (event == Event.PULL) group.collectorOwner!!.get(param.thisObject) else param.thisObject
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
                val owner = if (event == Event.PULL) group.collectorOwner!!.get(param.thisObject) else param.thisObject
                if (owner == null || owner.javaClass != group.owner) return
                if (param.isSkipped || param.hasThrowable) {
                    // An original that throws after clearing/replacing its holder
                    // still loses authority over the captured old generation.
                    retireChangedSession(frame, state, owner)
                    return
                }
                handleEvent(param, frame, state, owner)
            } catch (error: Exception) { debugCallbackError("ambient $event", error) }
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
            guarded("ambient attach commit") {
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
                        if (group.media!!.get(owner) == null) clearMedia(session, owner)
                        return
                    }
                    if (data.javaClass != group.resolver!!.mediaDataClass || group.media!!.get(owner) !== data) return
                    session.data = WeakReference(data)
                    sync(session)
                    requestColor(session, owner, data)
                }
                Event.FOREGROUND, Event.AOD -> state.session?.let { if (state.busy == 0) sync(it) }
                Event.PULL -> {
                    val session = state.session ?: return
                    if (!current(session, owner) || state.busy != 0 || group.collectorHolders.indices.any {
                            group.collectorHolders[it].get(param.thisObject) !== session.holders[it].get()
                        }) return
                    val pair = param.args.getOrNull(0) as? Pair<*, *> ?: return
                    when (pair.first) {
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
        if (group.mode == 1) {
            try { for (slot in session.slots) { ensureCurrent(session); removeOriginal(slot); ensureCurrent(session) } }
            catch (error: Exception) {
                try { retire(session) } catch (cleanup: Exception) { error.addSuppressed(cleanup) }
                if (state.session === session) state.session = null
                throw error
            }
        } else {
            // attach may retain an existing mediaData, or its original body may
            // synchronously bind while our structural frame is busy. Consume the
            // actual current value once; a following bind receives a newer ticket.
            val data = group.media!!.get(owner)
            if (data != null && data.javaClass == group.resolver!!.mediaDataClass && current(session, owner)) {
                session.data = WeakReference(data)
                sync(session)
                requestColor(session, owner, data)
            }
        }
    }

    // ROM calls and rollback can throw arbitrary runtime failures; preserve the host and cleanup boundary.
    @Suppress("TooGenericExceptionCaught")
    private fun requestColor(session: Session, owner: Any, data: Any) {
        val group = session.group
        val context = group.context.get(owner) as? Context ?: return
        val ticket = ++session.state.request
        val resolver = group.resolver ?: return
        fun eligible(): Boolean {
            val receiver = session.owner.get() ?: return false
            return group.active.get() && mainThread() && current(session, receiver) && session.state.busy == 0 &&
                session.state.request == ticket && group.media!!.get(receiver) === data && session.data.get() === data
        }
        try {
            HostExecutor.execute(session.taskTag, backgroundTask = {
                if (!group.active.get() || !session.live.get()) null else {
                    val colors = try { resolver.resolve(context, data, group.optimized) }
                    catch (error: Exception) { debugCallbackError("ambient resolver", error); null }
                    Outcome(colors)
                }
            }, onResult = { applyResult(session, it, ::eligible) })
        } catch (error: Exception) { applyResult(session, Outcome(null), ::eligible); debugCallbackError("ambient executor", error) }
    }

    private fun clearMedia(session: Session, owner: Any) {
        val ticket = ++session.state.request
        session.data.clear()
        applyResult(session, Outcome(null)) {
            session.group.active.get() && mainThread() && current(session, owner) && session.state.busy == 0 &&
                session.state.request == ticket && session.group.media!!.get(owner) == null
        }
    }

    private fun applyResult(session: Session, outcome: Outcome, eligible: () -> Boolean) {
        guarded("ambient color result") {
            if (!eligible()) return
            // add/removeView may synchronously reenter another bind/result.
            // Defer that latest result until the current view journal closes.
            if (session.installing) { session.pendingResult = { applyResult(session, outcome, eligible) }; return }
            session.installing = true
            try {
                val colors = outcome.colors
                if (colors == null) {
                    session.colors = null
                    cleanAll(session.slots.asReversed().map { slot -> { undo(slot) } })
                } else {
                    if (session.colors == null) installViews(session, colors, eligible)
                    check(eligible()) { "Ambient request changed before color commit" }
                    session.colors = colors
                    sync(session)
                }
            } finally {
                session.installing = false
                val pending = session.pendingResult
                session.pendingResult = null
                pending?.invoke()
            }
        }
    }

    // ROM calls and rollback can throw arbitrary runtime failures; preserve the host and cleanup boundary.
    @Suppress("TooGenericExceptionCaught")
    private fun installViews(session: Session, colors: AmbientColorResolverOS4.Colors, eligible: () -> Boolean) {
        val group = session.group
        try {
            // A valid shader and palette precede replacing any original background.
            for (slot in session.slots) {
                ensureCurrent(session); check(eligible())
                val original = checkNotNull(slot.original.get())
                val parent = checkNotNull(slot.parent.get())
                check(original.parent === parent) { "Ambient background ownership changed" }
                val drawable = AmbientLightDrawable()
                check(drawable.isReady) { "Ambient shader unavailable" }
                val view = ImageView(original.context)
                view.id = if (group.island) original.id else View.generateViewId()
                view.clipToOutline = true
                view.outlineProvider = original.outlineProvider
                view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                view.setImageDrawable(drawable)
                drawable.setGradientColor(colors.dark, true)
                drawable.start(); drawable.pause()
                val params = group.layout!!.newInstance(0, 0) as ViewGroup.LayoutParams
                group.edges.forEach { it.setInt(params, 0) }
                view.layoutParams = params
                slot.overlay = WeakReference(view)
                slot.drawable = WeakReference(drawable)
                val attach = object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View) { guarded("ambient view attach") { sync(session) } }
                    override fun onViewDetachedFromWindow(v: View) { guarded("ambient view detach") { drawable.stop() } }
                }
                slot.attachListener = attach
                view.addOnAttachStateChangeListener(attach)
                if (group.island) removeOriginal(slot)
                ensureCurrent(session); check(eligible())
                parent.addView(view, (slot.index + if (group.island) 0 else 1).coerceIn(0, parent.childCount))
                ensureCurrent(session); check(eligible())
                if (!group.island) {
                    val listener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> guarded("ambient layout") { followBounds(slot) } }
                    slot.layoutListener = listener
                    original.addOnLayoutChangeListener(listener)
                    view.addOnLayoutChangeListener(listener)
                    followBounds(slot)
                }
            }
        } catch (error: Exception) {
            // Roll back only our views and only still-unparented originals. Foreign
            // replacement of a holder or child never grants permission to overwrite it.
            try { cleanAll(session.slots.asReversed().map { slot -> { undo(slot) } }) } catch (cleanup: Exception) { error.addSuppressed(cleanup) }
            throw error
        }
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
            view.x = original.x; view.y = original.y
        } finally { slot.syncing = false }
    }

    private fun sync(session: Session) {
        val group = session.group
        val owner = session.owner.get() ?: return
        if (!mainThread() || !group.active.get()) return
        if (!current(session, owner) || session.state.busy != 0) return
        val data = group.media?.get(owner)
        val playing = data != null && group.playing?.get(data) == true
        val aod = group.fullAod()
        val resize = aod != session.fullAod
        session.fullAod = aod
        val context = group.context.get(owner) as? Context ?: return
        val dark = group.island || aod || group.alwaysDark ||
            context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        for (slot in session.slots) {
            val view = slot.overlay.get() ?: continue
            val drawable = slot.drawable.get() ?: continue
            if (view.parent !== slot.parent.get() || view.drawable !== drawable) { drawable.stop(); continue }
            if (resize) drawable.animateNextResize()
            if (!group.island) followBounds(slot)
            else if (slot.role == 1) view.translationY = group.offset!!.getFloat(owner)
            session.colors?.let { drawable.setGradientColor(if (dark) it.dark else it.light) }
            drawable.setLightMode(!dark)
            val canAnimate = playing && !aod && view.isAttachedToWindow
            val dragAllowsAnimation = !session.dragging || slot.role != 0
            if (canAnimate && dragAllowsAnimation) drawable.resume() else drawable.pause()
        }
    }

    private fun removeOriginal(slot: Slot) {
        val original = checkNotNull(slot.original.get())
        val parent = checkNotNull(slot.parent.get())
        check(original.parent === parent)
        slot.removed = true
        parent.removeView(original)
        check(original.parent == null) { "Ambient background removal failed" }
    }
    private fun undo(slot: Slot) {
        val original = slot.original.get()
        val view = slot.overlay.get()
        val parent = slot.parent.get()
        cleanAll(listOf(
            { slot.drawable.get()?.stop(); Unit },
            { slot.layoutListener?.let { original?.removeOnLayoutChangeListener(it) }; Unit },
            { slot.layoutListener?.let { view?.removeOnLayoutChangeListener(it) }; Unit },
            { slot.attachListener?.let { view?.removeOnAttachStateChangeListener(it) }; Unit },
            { removeOwnedOverlay(view, parent) },
            {
                restoreOwnedOriginal(slot, original, parent)
            }
        ))
        slot.removed = false
        slot.overlay.clear(); slot.drawable.clear()
        slot.layoutListener = null; slot.attachListener = null
    }
    private fun restoreOwnedOriginal(slot: Slot, original: View?, parent: ViewGroup?) {
        if (!slot.removed || original == null || original.parent != null) return
        if (parent == null || slot.holder.get()?.let { slot.background.get(it) === original } != true) return
        if (original.id != View.NO_ID && parent.findViewById<View>(original.id) != null) return
        parent.addView(original, slot.index.coerceIn(0, parent.childCount))
    }

    private fun removeOwnedOverlay(view: ImageView?, parent: ViewGroup?) {
        if (view != null && view.parent === parent) parent?.removeView(view)
    }

    private fun retire(session: Session) {
        if (session.live.getAndSet(false)) session.state.request++
        session.colors = null
        session.data.clear()
        session.pendingResult = null
        cleanAll(session.slots.asReversed().map { slot -> { undo(slot) } })
    }
    // ROM calls and rollback can throw arbitrary runtime failures; preserve the host and cleanup boundary.
    @Suppress("TooGenericExceptionCaught")
    private fun cleanAll(actions: List<() -> Unit>) {
        var failure: IllegalStateException? = null
        for (action in actions) try { action() } catch (error: Throwable) {
            if (failure == null) failure = IllegalStateException("Ambient owned cleanup incomplete")
            failure.addSuppressed(error)
        }
        failure?.let { throw it }
    }
    private fun ensureCurrent(session: Session) {
        val owner = session.owner.get()
        check(session.group.active.get() && owner != null && current(session, owner) && session.state.busy == 0) { "Ambient generation changed" }
    }
    private fun matches(session: Session, owner: Any): Boolean = session.group.holders.indices.all {
        session.group.holders[it].get(owner) === session.holders[it].get()
    }
    private fun current(session: Session, owner: Any): Boolean = session.live.get() &&
        session.state.session === session && session.owner.get() === owner && matches(session, owner)

    // This immutable contract records the complete validated ROM shape or captured invocation.
    @Suppress("LongParameterList", "SpreadOperator")
    private fun plan(island: Boolean, mode: Int, optimized: Boolean, dark: Boolean, active: AtomicBoolean, resolver: AmbientColorResolverOS4?): Group {
        val owner = required(if (island) DI + "MiuiIslandMediaViewBinderImpl" else NC + "MiuiMediaViewControllerImpl")
        val holder = required(if (island) DI + "MiuiIslandMediaViewHolder" else NC + "MiuiMediaViewHolder")
        val holders = (if (island) listOf("holder", "dummyHolder") else listOf("holder")).map { field(owner, it, holder) }
        val background = ownerField(holder, if (island) "mediaBgView" else "mediaBg")
        check(View::class.java.isAssignableFrom(background.type) && !Modifier.isStatic(background.modifiers))
        val context = field(owner, "context", Context::class.java)
        val methods = arrayListOf(method(owner, "attach", Void.TYPE, *(if (island) arrayOf(holder, holder) else arrayOf(holder))) to Event.ATTACH,
            method(owner, "detach", Void.TYPE) to Event.DETACH)
        var media: Field? = null; var playing: Field? = null; var aod: Field? = null; var offset: Field? = null
        var collector: Class<*>? = null; var collectorOwner: Field? = null
        var collectorHolders = emptyList<Field>(); var layout: Constructor<*>? = null; var edges = emptyList<Field>()
        if (mode == 2) {
            val data = checkNotNull(resolver).mediaDataClass
            media = field(owner, "mediaData", data)
            playing = field(data, "isPlaying", java.lang.Boolean::class.java)
            methods.add(method(owner, "bindMediaData", Void.TYPE, data) to Event.BIND)
            val params = required("androidx.constraintlayout.widget.ConstraintLayout\$LayoutParams")
            check(ViewGroup.LayoutParams::class.java.isAssignableFrom(params))
            layout = params.getDeclaredConstructor(Int::class.java, Int::class.java).also { it.isAccessible = true }
            edges = listOf("startToStart", "endToEnd", "topToTop", "bottomToBottom").map { field(params, it, Int::class.java) }
            if (island) {
                offset = field(owner, "mediaBgTransYOffset", Float::class.java)
                collector = required(DI + "MiuiIslandMediaViewBinderImpl\$attach\$4\$1")
                collectorOwner = field(collector, "this\$0", owner)
                collectorHolders = listOf("\$holder", "\$dummyHolder").map { field(collector, it, holder) }
                methods.add(method(collector, "emit", Any::class.java, Any::class.java, required("kotlin.coroutines.Continuation")) to Event.PULL)
            } else {
                aod = ownerField(required("com.android.systemui.statusbar.notification.fullaod.NotifiFullAodController"), "mEnableFullAod")
                check(aod.type == Boolean::class.java && Modifier.isStatic(aod.modifiers) && Modifier.isVolatile(aod.modifiers))
                methods.add(method(owner, "updateForegroundColors", Void.TYPE) to Event.FOREGROUND)
                methods.add(method(owner, "onFullAodStateChanged", Void.TYPE, Boolean::class.java) to Event.AOD)
            }
        }
        return Group(island, mode, optimized, dark, active, resolver, owner, holder, holders, background, context,
            media, playing, aod, offset, layout, edges, collector, collectorOwner, collectorHolders, methods)
    }
    private fun required(name: String) = checkNotNull(findClassIfExists(name)) { "OS4 ambient class unavailable: $name" }
    private fun ownerField(type: Class<*>, name: String) = type.getDeclaredField(name).also { it.isAccessible = true }
    private fun field(type: Class<*>, name: String, expected: Class<*>) = ownerField(type, name).also {
        check(it.type == expected && !Modifier.isStatic(it.modifiers)) { "OS4 ambient field changed: $name" }
    }
    private fun method(type: Class<*>, name: String, result: Class<*>, vararg args: Class<*>) = type.getDeclaredMethod(name, *args).also {
        check(it.returnType == result && !Modifier.isStatic(it.modifiers)) { "OS4 ambient method changed: $name" }; it.isAccessible = true
    }
    // ROM calls and rollback can throw arbitrary runtime failures; preserve the host and cleanup boundary.
    @Suppress("TooGenericExceptionCaught")
    private inline fun guarded(where: String, action: () -> Unit) {
        try { action() } catch (error: Exception) { debugCallbackError(where, error) }
    }
    private fun mainThread() = Looper.myLooper() === Looper.getMainLooper()
}
