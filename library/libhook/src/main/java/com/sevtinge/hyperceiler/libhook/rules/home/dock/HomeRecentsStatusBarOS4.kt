// SPDX-License-Identifier: AGPL-3.0-or-later
package com.sevtinge.hyperceiler.libhook.rules.home.dock

import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Parcel
import android.os.SystemClock
import android.view.WindowInsets
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.base.OriginalArgumentsHook
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.concurrent.atomic.AtomicBoolean

/** A temporary Insets request policy, never overwrites the client's requested types. */
internal class HomeRecentsStatusBarOS4(private val record: (String) -> Unit) {
    companion object {
        const val TRANSACTION = 0x00484354
        const val ACK = 0x48435231
        private val STATUS = WindowInsets.Type.statusBars()
    }
    private val state = HomeRecentsBarState()
    private val queued = AtomicBoolean(false)
    @Volatile private var stopped = false
    @Volatile private var ready = false
    @Volatile private var window: Any? = null
    @Volatile private var preference = HomeLayoutNativeEndpointOS4.RecentsBarPreference(false, false, 0)
    private var service: Any? = null
    private var handler: Handler? = null
    private var lock: Any? = null
    private lateinit var display: Method
    private lateinit var visible: Method
    private lateinit var keyguard: Method
    private lateinit var insetsPolicy: Method
    private lateinit var requestedChanged: Method
    private lateinit var focus: Field
    private var applied = false
    private var appliedDisplay: Any? = null
    private var reportedFailure = false
    private val expiry = Runnable { schedule() }

    private fun method(type: Class<*>, name: String, result: Class<*>, vararg args: Class<*>): Method {
        var c: Class<*>? = type
        while (c != null) {
            val found = c.declaredMethods.filter { it.name == name && it.returnType == result && it.parameterTypes.contentEquals(args) }
            if (found.size == 1) return found.single().apply { isAccessible = true }
            check(found.isEmpty()) { "ambiguous $name" }
            c = c.superclass
        }
        error("missing $name")
    }
    private fun field(type: Class<*>, name: String): Field {
        var c: Class<*>? = type
        while (c != null) {
            try { return c.getDeclaredField(name).apply { isAccessible = true } }
            catch (_: NoSuchFieldException) { c = c.superclass }
        }
        error("missing $name")
    }
    fun install(type: Class<*>) {
        try {
            val dc = Class.forName("com.android.server.wm.DisplayContent", false, type.classLoader)
            val policy = Class.forName("com.android.server.wm.InsetsPolicy", false, type.classLoader)
            val wm = Class.forName("com.android.server.wm.WindowManagerService", false, type.classLoader)
            display = method(type, "getDisplayContent", dc)
            visible = method(type, "isVisible", Boolean::class.javaPrimitiveType!!)
            keyguard = method(wm, "isKeyguardLocked", Boolean::class.javaPrimitiveType!!)
            insetsPolicy = method(dc, "getInsetsPolicy", policy)
            focus = field(dc, "mCurrentFocus")
            // Android 16/17 exposes this typed requested-types notification, older WMS has
            // onInsetsModified. Resolve the actual contract, never a field/method address.
            val changed = policy.declaredMethods.filter { m ->
                m.returnType == Void.TYPE && ((m.name == "onRequestedVisibleTypesChanged"
                    && m.parameterCount == 3 && m.parameterTypes[0].isAssignableFrom(type)
                    && m.parameterTypes[1] == Int::class.javaPrimitiveType
                    && m.parameterTypes[2].name == "android.view.inputmethod.ImeTracker\$Token")
                    || (m.name == "onInsetsModified" && m.parameterCount == 1 && m.parameterTypes[0].isAssignableFrom(type)))
            }
            check(changed.size == 1) { "requested-types notification unavailable or ambiguous" }
            requestedChanged = changed.single().apply { isAccessible = true }
            val forciblyHidden = method(policy, "areTypesForciblyHidden", Boolean::class.javaPrimitiveType!!,
                Int::class.javaPrimitiveType!!)
            val controlTarget = Class.forName("com.android.server.wm.InsetsControlTarget", false, type.classLoader)
            val controlCallers = listOf(
                method(policy, "getStatusControlTargetInner", controlTarget, type, Boolean::class.javaPrimitiveType!!),
                method(policy, "getStatusControlTarget", controlTarget, type, Boolean::class.javaPrimitiveType!!, IntArray::class.java),
                method(policy, "updateBarControlTarget", Void.TYPE, type))
            observeAfter(method(type, "getRequestedVisibleTypes", Int::class.javaPrimitiveType!!)) { param ->
                if (param.thisObject === window && shouldHide()) param.result = (param.result as Int) and STATUS.inv()
            }
            observeAfter(method(type, "isRequestedVisible", Boolean::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!)) { param ->
                if (param.thisObject === window && param.args[0] == STATUS && shouldHide()) param.result = false
            }
            observeAfter(forciblyHidden) { param ->
                if (param.args[0] == STATUS && shouldHide() && runCatching {
                        val target = window ?: return@runCatching false
                        val dc = display.invoke(target) ?: return@runCatching false
                        insetsPolicy.invoke(dc) === param.thisObject
                    }.getOrDefault(false)) param.result = true
            }
            // A client may keep its insets leash shown despite the filtered request.
            // The platform's forced-hidden route transfers only status-bar control
            // to its original hiding target, which drives the surface animation.
            // No forced-types field is changed, normal focus/transient policy still
            // runs first, and lease expiry/exit restores the original control target.
            // These tiny predicates may be AOT-inlined into the target selection
            // chain. Invalidate just those three callers once at installation,
            // otherwise the source can be hidden while the client keeps its leash.
            val deoptimized = controlCallers.count { BaseHook.deoptimize(it) }
            record("recents status bar control callers deoptimized=$deoptimized/${controlCallers.size}")
            ready = true
            record("recents status bar window policy installed, native execution and visible effect require separate evidence")
        } catch (error: Throwable) { fail(error) }
    }
    private fun observeAfter(method: Method, callback: (HookParam) -> Unit) {
        // Do not re-submit untouched primitive arguments through proceedWith.
        // Preserve the original WMS call and its exception, then filter its result only.
        BaseHook.chain(method, OriginalArgumentsHook.of(object : IMethodHook {
            override fun after(param: HookParam) {
                if (param.throwable == null) callback(param)
            }
        }))
    }
    fun bind(target: Any, wm: Any) {
        if (!ready || stopped) return
        try {
            if (window === target) return
            val dc = display.invoke(target) ?: return
            val id = method(dc.javaClass, "getDisplayId", Int::class.javaPrimitiveType!!).invoke(dc) as Int
            if (id != 0) return
            val session = field(target.javaClass, "mSession").get(target) ?: return
            val uid = field(session.javaClass, "mUid").getInt(session)
            val pid = field(session.javaClass, "mPid").getInt(session)
            if (uid < 10000 || pid <= 0) return
            service = wm
            handler = field(wm.javaClass, "mH").get(wm) as Handler
            lock = field(wm.javaClass, "mGlobalLock").get(wm)
            state.bind(uid, pid)
            window = target
            schedule()
        } catch (error: Throwable) { fail(error) }
    }
    fun removed(target: Any) {
        if (window !== target) return
        window = null
        state.clear()
        schedule()
    }
    private fun shouldHide(): Boolean {
        val pref = preference
        val target = window
        if (stopped || !ready || target == null || !pref.available() || !pref.enabled()
            || SystemClock.elapsedRealtimeNanos() >= pref.deadline() || !state.overview(System.nanoTime())) return false
        return runCatching {
            val dc = display.invoke(target)
            dc != null && focus.get(dc) === target && visible.invoke(target) == true
                && keyguard.invoke(service) == false
        }.getOrDefault(false)
    }
    fun receive(data: Parcel, reply: Parcel, flags: Int) {
        var pref = HomeLayoutNativeEndpointOS4.RecentsBarPreference(false, false, 0)
        try {
            data.enforceInterface("android.view.IWindowManager")
            if (!stopped && ready && flags and IBinder.FLAG_ONEWAY == 0 && data.dataAvail() == 40
                && state.identity(Binder.getCallingUid(), Binder.getCallingPid())) {
                val sequence = data.readLong()
                val timestamp = data.readLong()
                val mask = data.readLong()
                val revision = data.readLong()
                val confirm = data.readLong()
                val now = System.nanoTime()
                val probe = sequence > 0 && timestamp > 0 && timestamp <= now
                    && now - timestamp < HomeRecentsBarState.MAX_AGE_NS && mask == 0L && revision == 0L && confirm == 0L
                if (probe || state.accept(Binder.getCallingUid(), Binder.getCallingPid(), sequence,
                        timestamp, mask, revision, confirm, now)) {
                    pref = HomeLayoutNativeEndpointOS4.recentsBarPreference()
                    preference = pref
                    schedule()
                }
            }
        } catch (_: Throwable) { /* Fail closed, preserve the original request. */ }
        reply.writeInt(ACK)
        reply.writeInt(if (pref.available()) 1 else 0)
        reply.writeInt(if (pref.available() && pref.enabled()) 1 else 0)
    }
    private fun schedule() {
        val h = handler ?: return
        if (queued.compareAndSet(false, true) && !h.post {
                queued.set(false)
                val wmLock = lock ?: return@post
                synchronized(wmLock) { refresh() }
            }) queued.set(false)
    }
    private fun notifyPolicy(dc: Any, target: Any) {
        val policy = insetsPolicy.invoke(dc) ?: return
        if (requestedChanged.parameterCount == 1) requestedChanged.invoke(policy, target)
        else requestedChanged.invoke(policy, target, STATUS, null)
    }
    private fun refresh() {
        val h = handler ?: return
        h.removeCallbacks(expiry)
        try {
            val target = window
            val dc = if (target != null) display.invoke(target) else null
            val hidden = shouldHide()
            val oldDc = appliedDisplay
            if (oldDc != null && oldDc !== dc) {
                // The old target is no longer filtered by our getters. Ask its display to
                // recompute using its own current focus, never overwrite any window attrs.
                focus.get(oldDc)?.let { notifyPolicy(oldDc, it) }
            }
            if (dc != null && target != null && (hidden != applied || oldDc !== dc)) {
                focus.get(dc)?.let { notifyPolicy(dc, it) }
                if (hidden != applied) record("recents status bar policy applied hidden=$hidden, Insets consumer notified")
            }
            applied = hidden
            appliedDisplay = dc
            if (hidden) {
                val delay = minOf(state.deadline() - System.nanoTime(),
                    preference.deadline() - SystemClock.elapsedRealtimeNanos())
                h.postDelayed(expiry, maxOf(1, delay / 1_000_000 + 1))
            }
        } catch (error: Throwable) { fail(error) }
    }
    private fun fail(error: Throwable) {
        ready = false
        preference = HomeLayoutNativeEndpointOS4.RecentsBarPreference(false, false, 0)
        if (!reportedFailure) { reportedFailure = true; schedule(); record("recents status bar policy unavailable=${error.javaClass.simpleName}, stock requests retained") }
    }
    fun close() {
        stopped = true
        state.clear()
        schedule() // Recompute the original requests on the WM handler under the WM lock.
    }
}
