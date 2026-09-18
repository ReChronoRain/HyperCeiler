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
package com.sevtinge.hyperceiler.libhook.rules.home.dock

import android.content.Context
import android.content.ContentProviderClient
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import android.view.SurfaceControl
import android.view.SurfaceControlViewHost
import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.common.utils.prefs.PrefType
import com.sevtinge.hyperceiler.common.utils.prefs.PrefsChangeObserver
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import java.util.UUID

/**
 * No provider IPC, waiting or HWUI work on the WMS thread/under the global WM lock.
 *
 * <p>This class runs inside system_server, so an exception that leaves any posted task or
 * framework callback reaches the process uncaught handler and becomes
 * "FATAL EXCEPTION IN SYSTEM PROCESS". Every asynchronous entry point therefore goes through
 * [guard], and recoverable failures are turned into a bounded retry instead of an escape.
 */
internal class DockGlassClient(private val processGuard: DockGlassProcessGuard, private val changed: () -> Unit,
    /** True while the display is rotated or has only just returned to natural rotation. */
    private val rotationSettling: () -> Boolean = { false },
    /** A retained texture needs only a portrait display, not the new-host settle delay. */
    private val rotationActive: () -> Boolean = { false }) {
    class Ticket(val key: String, val context: Context,
        val bounds: DockWindowPolicy.Bounds, val dark: Boolean) {
        val id: String = UUID.randomUUID().toString()

        /**
         * Generation counter, single-flight recovery latch, retry budget, readiness and
         * refresh epochs. All bookkeeping lives here so the transitions are unit-testable
         * without an Android runtime.
         */
        val gate = DockGlassRecoveryGate()

        @Volatile var lease: DockGlassSurfaceLease? = null
        @Volatile var ready = false
        @Volatile var dead = false
        /** The renderer acknowledged that a ready texture survived a sampling pause. */
        @Volatile var retainedCapture = false
        // Worker only: even a pause without a retained frame must be resumed.
        var capturePaused = false
        /**
         * The host had to use the vendor's undraw pause instead of the drawable freeze, so the
         * panel has nothing to composite and must fall back to the compositor.
         *
         * <p>Read by the appearance on the WMS thread, written on the worker.
         */
        @Volatile var captureUndrawn = false

        /** Retired by [DockGlassClient.release]; also permanently blocks later retries. */
        val cancelled: Boolean get() = gate.isCancelled()

        // Only touched by the serial IPC worker.
        var parcel: SurfaceControlViewHost.SurfacePackage? = null
        var lifetime: IBinder? = null
        var death: IBinder.DeathRecipient? = null
        var client: ContentProviderClient? = null
        var refreshes = 0

        fun request(method: String, args: Bundle? = null): Bundle {
            // Keep an UNSTABLE reference for the lifetime of the active windowless host,
            // instead of acquiring/releasing its process around every readiness check.
            // Renderer death still never makes system_server a stable provider dependent.
            val activeClient = client ?: context.contentResolver.acquireUnstableContentProviderClient(uri)
                ?.also { client = it } ?: error("HyperCeiler provider unavailable")
            // All callers recover/dispose on failure. Let dispose try releasing an
            // existing live host before closing the reference, even after a failed call.
            return activeClient.call(method, id, args) ?: Bundle.EMPTY
        }
    }

    private val owner = Binder()
    private val workerDelegate = lazy {
        Handler(HandlerThread("HyperCeiler-DockGlass-IPC").apply { start() }.looper)
    }
    private val worker by workerDelegate
    private companion object {
        val uri: Uri = Uri.parse("content://com.sevtinge.hyperceiler.provider.sharedprefs")

        /** The settings key the unlock fly-in style lives under. */
        const val REVEAL_STYLE_KEY = "prefs_key_home_dock_unlock_style"

        /**
         * Fallback interval for the style read, now that the provider pushes its changes.
         *
         * <p>This used to be the primary path at two seconds: one cross-process query and one
         * renderer wakeup every two seconds, forever - including with the Dock idle and the screen
         * off. The push covers freshness, so this only exists for a ROM that drops the provider
         * notification.
         */
        const val STYLE_QUERY_INTERVAL_MS = 30_000L

        /**
         * How long a burst of ring entries waits before it is handed to the provider.
         *
         * <p>Animations record per frame, so the previous 250 ms debounce turned a gesture into
         * about four cross-process calls per second, each carrying up to 96 strings. The ring
         * contents are unchanged - only the flush cadence is - so nothing is lost for diagnosis.
         */
        const val DIAGNOSTIC_FLUSH_MS = 1_000L

        /**
         * Bound of geometry-stale re-probes after a return.
         *
         * <p>The host refuses samples whose blur geometry still carries the rotated transform and
         * recomputes it inside every status/probe, so this usually clears on the first call. The
         * cap exists so a vendor whose geometry never follows the display falls through to a host
         * rebuild instead of probing forever.
         */
        const val GEOMETRY_REPROBE_LIMIT = 20

        /** Cadence of the geometry re-probe; roughly two frames, so no stale colour lingers. */
        const val GEOMETRY_REPROBE_MS = 32L

        /**
         * Faster cadence for the first rotation rechecks.
         *
         * <p>A returning host needs the compositor's first post-rotation capture, which normally
         * arrives within one frame. Probing at the ordinary 32 ms cadence would leave the fallback
         * on screen for up to two frames longer than necessary.
         */
        const val ROTATION_RECHECK_MS = 8L

        /** Number of fast rotation rechecks before falling back to the ordinary cadence. */
        const val ROTATION_RECHECK_FAST = 8

        /**
         * How long a frozen home sample keeps carrying the panel after the display returns.
         *
         * <p>Thawing on the first returning frame can publish one frame captured from the closing
         * app. The frozen home sample is already correct, so hold it until the compositor has had
         * time to produce a home-based frame - this is invisible, unlike the compositor fallback.
         */
        const val ROTATION_THAW_DELAY_MS = 120L

        /** Cadence of the timed fallback for a resume whose commit listener is unusable. */
        const val CAPTURE_RESUME_FALLBACK_MS = 1_200L
    }
    @Volatile private var closed = false
    @Volatile private var styleContext: Context? = null
    @Volatile private var liveRevealStyle: String? = null
    /** Dropped by [close], so a hot reload cannot accumulate observer registrations. */
    @Volatile private var styleObserver: PrefsChangeObserver? = null
    private var styleQueryAt = 0L
    private val journal = Journal { worker }

    private class Journal(private val worker: () -> Handler) {
        @Volatile private var diagnosticContext: Context? = null
        private val events = ArrayDeque<String>() // IPC worker only; no frame-by-frame history.
        private var diagnosticFlushScheduled = false
        private val diagnosticFlush = Runnable {
            diagnosticFlushScheduled = false
            flushDiagnostics()
        }

        fun bind(context: Context) {
            if (diagnosticContext != null) return
            diagnosticContext = context
            worker().post { flushDiagnostics() }
        }

        fun record(event: String) {
            if (isSampledFrameNoise(event)) return
            val message = "wall=${System.currentTimeMillis()} up=${SystemClock.uptimeMillis()} $event".take(512)
            // Deliberately independent of the release build's error-only Xposed log filter.
            Log.i("HyperCeiler.DockGlass", message)
            worker().post {
                if (events.size >= 96) events.removeFirst()
                events.addLast(message)
                if (!diagnosticFlushScheduled) {
                    diagnosticFlushScheduled = true
                    worker().postDelayed(diagnosticFlush, DIAGNOSTIC_FLUSH_MS)
                }
            }
        }


        /**
         * Per-frame diagnostic kinds, sampled instead of recorded verbatim.
         *
         * <p>At 120 Hz the reveal/motion debug records arrive about four times per frame: that
         * flooded the ring (which is how §37 was misdiagnosed) and cost a logd write each. Sampling
         * keeps the progression readable at ~7 Hz; a full-rate diagnostic is a one-line revert.
         */
        private val noisyPrefixes = listOf(
            "reveal dbg frame", "reveal dbg pose", "reveal dbg layout", "motion position")
        private val frameNoiseSampleMs = 150L
        @Volatile private var lastNoisyAtMs = 0L

        private fun isSampledFrameNoise(event: String): Boolean {
            if (noisyPrefixes.none { event.startsWith(it) }) return false
            val now = SystemClock.uptimeMillis()
            if (now - lastNoisyAtMs < frameNoiseSampleMs) return true
            lastNoisyAtMs = now
            return false
        }

        private fun flushDiagnostics() {
            val context = diagnosticContext ?: return
            if (events.isEmpty()) return
            catchingRecoverable({
                val client = context.contentResolver.acquireUnstableContentProviderClient(uri)
                    ?: return
                client.use {
                    it.call("dock_glass_record", null, Bundle().apply {
                        putStringArray("events", events.toTypedArray())
                    }) ?: error("No journal response")
                }
                events.clear()
            })
            // If boot-time provider acquisition fails, retain the bounded queue for the next event.
        }

        fun finish() {
            worker().removeCallbacks(diagnosticFlush)
            flushDiagnostics()
        }
    }

    fun bindDiagnostics(context: Context) {
        styleContext = context
        if (!closed) {
            journal.bind(context)
            watchRevealStyle(context)
        }
    }

    /**
     * Follow the settings file instead of polling it.
     *
     * <p>The provider notifies its observers whenever the UI writes, and [PrefsChangeObserver]
     * also subscribes to LSPosed's in-process remote listener when that is available, so a style
     * change arrives without any traffic of our own. Registered once, from the first traversal
     * that knows the WMS context, and released by [close] on hot reload.
     */
    private fun watchRevealStyle(context: Context) {
        if (styleObserver != null || closed) return
        catchingRecoverable({
            val observer = object : PrefsChangeObserver(context, Handler(worker.looper), false,
                PrefType.String, REVEAL_STYLE_KEY, null) {
                override fun onChange(type: PrefType, changed: Uri?, name: String?, def: Any?) {
                    // The notification carries no value this endpoint can use - the provider still
                    // owns the read - so drop the fallback throttle and let the read happen now.
                    styleQueryAt = 0L
                    refreshRevealStyle()
                }
            }
            styleObserver = observer
            record("reveal style observer registered")
        }) {
            record("reveal style observer unavailable=${it.javaClass.simpleName}: " +
                "${it.message?.take(120)}")
        }
    }

    fun record(event: String) { if (!closed) journal.record(event) }

    /** Latest reveal style name read from the module's own preferences, or null before the first read. */
    fun liveRevealStyle(): String? = liveRevealStyle

    /**
     * Re-read the unlock fly-in style from the module provider.
     *
     * <p>The style is consumed in system_server, but LSPosed's remote preferences there are a
     * snapshot pushed by the daemon: when that push is lost the value stays stale for the rest of
     * the process lifetime, and neither a launcher restart nor anything else refreshes it. The
     * provider reads the settings file the UI actually wrote, so reading it is what makes a style
     * change take effect at all - but reading it on a timer is not: [watchRevealStyle] asks for
     * this read when the settings actually change, and the interval below is only the fallback.
     */
    fun refreshRevealStyle() {
        val context = styleContext ?: return
        if (closed) return
        val now = SystemClock.uptimeMillis()
        if (now - styleQueryAt < STYLE_QUERY_INTERVAL_MS) return
        styleQueryAt = now
        worker.post {
            guard("style query") {
                catchingRecoverable({
                    context.contentResolver.query(
                        Uri.parse("$uri/string/$REVEAL_STYLE_KEY"),
                        null, null, null, null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val value = cursor.getString(0)
                            if (liveRevealStyle != value) {
                                liveRevealStyle = value
                                record("reveal style queried=$value")
                                // Apply it on the next traversal instead of waiting for the sweep.
                                changed()
                            }
                        }
                    }
                })
            }
        }
    }

    /**
     * Run one asynchronous DockGlass task behind a hard exception boundary.
     *
     * <p>Only [Exception] is caught: `OutOfMemoryError`, `StackOverflowError` and `ThreadDeath`
     * keep their normal, fatal behaviour rather than being silently swallowed. The boundary
     * exists so a defect in DockGlass degrades glass instead of killing system_server.
     */
    private fun guard(task: String, body: () -> Unit) {
        try {
            body()
        } catch (error: Exception) {
            val reason = "${error.javaClass.simpleName}: ${error.message?.take(160)}"
            try {
                record("glass task failed task=$task reason=$reason")
            } catch (ignored: Exception) {
                // The diagnostic path must never mask the original failure.
            }
            try {
                XposedLog.w("DockGlass", "system", "DockGlass $task failed: $reason")
            } catch (ignored: Exception) {
            }
        }
    }

    fun create(context: Context, key: String, bounds: DockWindowPolicy.Bounds, dark: Boolean): Ticket {
        val ticket = Ticket(key, context, bounds, dark)
        bindDiagnostics(context)
        worker.post { guard("create") { attemptCreate(ticket) } }
        return ticket
    }

    private fun attemptCreate(ticket: Ticket) {
        if (!ticket.gate.beginAttempt(closed)) return
        ticket.dead = false
        record("glass create id=${ticket.id} attempt=${ticket.gate.getAttempts()}")
        try {
            check(dispose(ticket)) { "Previous glass surface could not be detached" }
            if (!processGuard.acquire(ticket.context, ticket)) {
                // The package is mid-replacement: nothing is broken and nothing may be
                // disposed. Park the generation and retry on the dependency cadence.
                deferForDependency(ticket)
                return
            }
            val bounds = ticket.bounds
            val args = Bundle().apply {
                putInt("width", bounds.width()); putInt("height", bounds.height())
                putFloat("radius", bounds.radius()); putBoolean("dark", ticket.dark)
                putBinder("owner", owner)
            }
            val response = ticket.request("dock_glass_create", args)
            processGuard.setPid(ticket, response.getInt("rendererPid", -1))
            response.classLoader = SurfaceControlViewHost.SurfacePackage::class.java.classLoader
            ticket.parcel = response.getParcelable("surface", SurfaceControlViewHost.SurfacePackage::class.java)
                ?: error(response.getString("error") ?: "Renderer returned no surface")
            ticket.lifetime = response.getBinder("lifetime") ?: error("Renderer returned no lifetime token")
            if (ticket.cancelled || closed) { dispose(ticket); return }
            val generation = ticket.gate.getAttempts()
            val readinessEpoch = ticket.gate.openReadinessEpoch()
            val death = IBinder.DeathRecipient {
                worker.post {
                    guard("renderer death") {
                        if (!ticket.gate.isAttemptCurrent(generation)) {
                            if (ticket.gate.noteStaleCallback()) {
                                record("glass stale death callback id=${ticket.id} " +
                                    "generation=$generation current=${ticket.gate.getAttempts()}")
                            }
                            return@guard
                        }
                        recover(ticket, "renderer died")
                    }
                }
            }
            ticket.death = death
            ticket.lifetime!!.linkToDeath(death, 0)
            val parcel = ticket.parcel!!
            val surface = parcel.callMethod("getSurfaceControl") as? SurfaceControl
                ?: error("Renderer returned no SurfaceControl")
            ticket.lease = DockGlassSurfaceLease(object : DockGlassSurfaceLease.Operations {
                override fun attach(parent: Any, alpha: Float) {
                    SurfaceControl.Transaction().use { transaction ->
                        transaction.reparent(surface, parent as SurfaceControl)
                        transaction.setLayer(surface, 2)
                        transaction.setPosition(surface, 0f, 0f)
                        transaction.callMethod("setWindowCrop", surface, bounds.width(), bounds.height())
                        // Keep the capture source composited before readiness, with only a
                        // 1/255 contribution over the fallback. Hiding it can starve the source.
                        transaction.setAlpha(surface, alpha)
                        transaction.callMethod("show", surface)
                        transaction.apply()
                    }
                }

                override fun setAlpha(alpha: Float) {
                    check(surface.isValid) { "Glass surface is no longer valid" }
                    SurfaceControl.Transaction().use {
                        it.setAlpha(surface, alpha)
                        it.apply()
                    }
                }

                override fun detach() {
                    // release() only drops our handle; an attached server-side root
                    // otherwise survives renderer death underneath the Dock parent.
                    if (surface.isValid) SurfaceControl.Transaction().use {
                        it.reparent(surface, null).apply()
                    }
                }

                override fun release() { parcel.release() }
            })
            changed()
            // Parent first, then wait for the vendor background texture, not just a drawn buffer.
            checkBackground(ticket, 0, generation, readinessEpoch)
        } catch (error: Exception) {
            // Covers the framework exception types this path used to catch individually
            // (IllegalStateException, RemoteException, SecurityException) plus any reflection
            // failure from the guard/framework calls. VM fatals are intentionally not caught.
            recover(ticket, "create ${error.javaClass.simpleName}: ${error.message?.take(160)}")
        }
    }

    private fun checkBackground(ticket: Ticket, attempt: Int, generation: Int, readinessEpoch: Int,
        delayMs: Long = DockGlassRetryPolicy.BACKGROUND_CHECK_MS) {
        worker.postDelayed({
            guard("readiness") {
                if (ticket.cancelled || ticket.dead || closed) return@guard
                if (!ticket.gate.isReadinessCurrent(generation, readinessEpoch)) {
                    if (ticket.gate.noteStaleCallback()) {
                        record("glass stale readiness id=${ticket.id} generation=$generation " +
                            "current=${ticket.gate.getAttempts()}")
                    }
                    return@guard
                }
                try {
                    val status = ticket.request("dock_glass_status")
                    status.getString("error")?.let { error(it) }
                    val wasReady = ticket.ready
                    val attached = ticket.lease?.isAttached == true
                    val producerActive = status.getBoolean("producerActive")
                    // GeometryValid is intentionally excluded: after a display rotation the
                    // windowless host's mConfigRot is stale, but the dock is behind the
                    // foreground app so the wrong blur is invisible.  Keeping the glass avoids
                    // the visible 高级材质→玻璃 flash.
                    ticket.ready = attached && status.getBoolean("backgroundReady")
                    if (wasReady != ticket.ready) changed()
                    if (ticket.ready) {
                        ticket.gate.markReady()
                        record("glass ready id=${ticket.id} attempt=${ticket.gate.getAttempts()} check=${attempt + 1}")
                        return@guard
                    }
                    val next = minOf(attempt + 1, DockGlassRetryPolicy.BACKGROUND_IDLE_CHECK)
                    val nextDelay = DockGlassRetryPolicy.delayAfterBackgroundCheck(
                        next, attached, producerActive)
                    if (nextDelay < 0) {
                        recover(ticket, "background producer unavailable after $next checks " +
                            "attached=$attached active=$producerActive")
                    } else {
                        if (next == DockGlassRetryPolicy.BACKGROUND_CHECKS ||
                            (next == DockGlassRetryPolicy.BACKGROUND_IDLE_CHECK && next != attempt)) {
                            record("glass awaiting first texture id=${ticket.id} " +
                                "generation=$generation pollMs=$nextDelay; retaining live host")
                        }
                        // A live producer with no frame is not a compatibility failure. Retain
                        // its texture and wait at a capped cadence instead of repeatedly resetting
                        // it (refresh also destroys mSurTex) and exhausting the create budget.
                        checkBackground(ticket, next, generation, readinessEpoch, nextDelay)
                    }
                } catch (error: Exception) {
                    recover(ticket, "status ${error.javaClass.simpleName}: ${error.message?.take(160)}")
                }
            }
        }, delayMs)
    }

    /**
     * Reparent the host root into `parent` and write its initial readiness opacity.
     *
     * <p>`ready` is passed in rather than re-read from the ticket so the value matches the
     * appearance transition that requested the attach; a later readiness flip always drives
     * another traversal and an idempotent [setReady].
     */
    fun attach(ticket: Ticket, parent: Any, ready: Boolean) {
        val lease = ticket.lease ?: return
        // Never queue remote reparent operations in WMS's deferred sync transaction:
        // it could commit AFTER worker cleanup and resurrect a retired surface.
        worker.post {
            guard("attach") {
                val stale = closed || ticket.cancelled || ticket.dead || ticket.lease !== lease
                if (stale) {
                    if (ticket.gate.noteStaleCallback()) {
                        record("glass stale attach ignored id=${ticket.id} closed=$closed " +
                            "cancelled=${ticket.cancelled} dead=${ticket.dead}")
                    }
                    return@guard
                }
                catchingRecoverable({ lease.attach(parent, ready) }) {
                    recover(ticket, "surface attachment failed: ${it.javaClass.simpleName}")
                }
            }
        }
    }

    /**
     * Promote or dim the host material without moving or hiding its capture source.
     *
     * <p>[attach] can only run once per generation and therefore cannot be the only place that
     * decides whether the material is ready: the renderer reports a background texture
     * hundreds of milliseconds later, at which point the Dock writes a transparent tint and no
     * blur. Every appearance change (the key includes readiness) re-issues this call, and
     * the lease drops writes that repeat the value already on screen.
     */
    fun setReady(ticket: Ticket, ready: Boolean) {
        val lease = ticket.lease ?: return
        worker.post {
            guard("readiness opacity") {
                val stale = closed || ticket.cancelled || ticket.dead || ticket.lease !== lease
                if (stale) {
                    if (ticket.gate.noteStaleCallback()) {
                        record("glass stale opacity ignored id=${ticket.id} ready=$ready")
                    }
                    return@guard
                }
                catchingRecoverable({ lease.setReady(ready) }) {
                    recover(ticket, "opacity failed: ${it.javaClass.simpleName}")
                }
            }
        }
    }

    /** Verify HyperCeiler's own texture after its parent becomes visible again. */
    fun resume(ticket: Ticket, allowFallback: Boolean = true,
        afterCommit: SurfaceControl.Transaction? = null, probeDelayMs: Long = 50L,
        force: Boolean = false) {
        // Visibility and wallpaper-home callbacks can describe the same return.
        val retained = ticket.retainedCapture
        // Wallpaper commands do not own a SurfaceControl transaction. Let the traversal
        // couple retained sampling to the transaction that actually restores the Dock pose.
        // A forced rotation resume is the exception: the maintained frame is already the correct
        // home sample and there is no wallpaper-zoom transaction to wait for.
        if (retained && afterCommit == null && !force) {
            changed()
            return
        }
        val requestEpoch = if (force) ticket.gate.forceRefresh(SystemClock.uptimeMillis())
            else ticket.gate.requestRefresh(SystemClock.uptimeMillis(), retained && rotationActive())
        if (requestEpoch == DockGlassRecoveryGate.REFRESH_DEDUPLICATED) return
        val requestedAt = SystemClock.uptimeMillis()
        var geometryWaits = 0
        fun resumeProbe() {
            guard("resume probe") {
                if (closed || ticket.cancelled || ticket.dead || ticket.lease == null) return@guard
                if (!ticket.gate.isRefreshCurrent(requestEpoch)) {
                    if (ticket.gate.noteStaleCallback()) {
                        record("glass stale resume probe id=${ticket.id} epoch=$requestEpoch")
                    }
                    return@guard
                }
                // Rotation can reverse while this request is queued. Do not sample landscape,
                // and do not let this abandoned request throttle the next portrait return.
                if (ticket.retainedCapture && rotationActive()) {
                    ticket.gate.cancelRefresh(requestEpoch)
                    return@guard
                }
                // Initial creation owns its readiness loop - unless pauseRefresh() retired that
                // loop while the parent was hidden. A retired loop does not reschedule itself, and
                // nothing else ever opens a new one, so a layer that was briefly not visible before
                // it first rendered (a launcher start, a rotation) would keep the compositor
                // fallback - blur plus tint - for the rest of the window's life. Re-open the loop
                // here instead of waiting for a probe that can never see a ready generation.
                if (!ticket.gate.isPreviouslyReady()) {
                    checkBackground(ticket, 0, ticket.gate.getAttempts(),
                        ticket.gate.openReadinessEpoch())
                    return@guard
                }
                catchingRecoverable({
                    val response = ticket.request(
                        if (ticket.capturePaused) "dock_glass_resume_capture" else "dock_glass_probe")
                    response.getString("error")?.let { error(it) }
                    val resumedRetained = ticket.retainedCapture
                    ticket.capturePaused = false
                    ticket.captureUndrawn = false
                    ticket.retainedCapture = false
                    // The host refuses samples whose blur geometry still carries the rotated
                    // transform: they are mapped through the wrong rotation, which is the
                    // "sampling position changed" artefact. Hold the compositor fallback for
                    // exactly that window and re-probe - a producer restart cannot make the
                    // geometry catch up faster, and the retained frame is wrong for the same
                    // reason.
                    val geometryValid = response.geometryValidOrDefault()
                    val healthy = ticket.lease?.isAttached == true
                        && response.getBoolean("backgroundReady")
                    if (healthy) {
                        val wasReady = ticket.ready
                        ticket.ready = true
                        ticket.gate.markReady()
                        if (!wasReady) changed()
                        if (resumedRetained) record("glass capture resumed with retained texture id=${ticket.id} " +
                            "timing=pose-committed waitMs=${SystemClock.uptimeMillis() - requestedAt} " +
                            "geometry=${response.getString("captureGeometry")}")
                    } else if (!geometryValid && geometryWaits < GEOMETRY_REPROBE_LIMIT) {
                        // The host's windowless ViewRoot still carries the rotated blur geometry.
                        // Every status/probe heals it in place, so another probe is what makes it
                        // catch up - a producer restart cannot, and only adds a second visible
                        // state. Never leave the stale sample on screen while that happens.
                        geometryWaits++
                        if (ticket.ready) {
                            ticket.ready = false
                            changed()
                        }
                        if (geometryWaits == 1) {
                            record("glass geometry stale id=${ticket.id} " +
                                "geometry=${response.getString("captureGeometry")}")
                        }
                        worker.postDelayed(
                            { guard("geometry recheck") { resumeProbe() } },
                            GEOMETRY_REPROBE_MS)
                    } else if (!geometryValid) {
                        // The vendor never followed the display. Rebuilding the host is the only
                        // remaining way to obtain a geometry resolved at texture creation.
                        recover(ticket, "blur geometry did not follow the display after $geometryWaits syncs")
                    } else if (force && geometryWaits < GEOMETRY_REPROBE_LIMIT) {
                        // A rotation heal can rebuild the capture texture, and its first frame may
                        // arrive after this probe. The panel is already on the fallback, so re-check
                        // briefly instead of leaving a healthy host dimmed for the window's life.
                        geometryWaits++
                        val recheckMs = if (force && geometryWaits <= ROTATION_RECHECK_FAST)
                            ROTATION_RECHECK_MS else GEOMETRY_REPROBE_MS
                        worker.postDelayed(
                            { guard("rotation recheck") { resumeProbe() } },
                            recheckMs)
                    } else if (force) {
                        // A rotation resume that never reached a ready glass means the host is
                        // unusable, not merely settling: rebuild it instead of staying on fallback.
                        recover(ticket, "rotation resume produced no ready glass after $geometryWaits probes")
                    } else if (!allowFallback) {
                        // Settle window: the wallpaper swap keeps the producer busy, so an unhealthy
                        // probe here is expected and transient. Keep the current material on screen
                        // instead of flashing the compositor fallback and restarting the producer;
                        // the caller re-probes once the window closes.
                        record("glass probe unhealthy during settle; keeping current material")
                    } else {
                        // Put compositor fallback behind the Dock before restarting the
                        // private producer. This prevents a transparent/white flash.
                        val wasReady = ticket.ready
                        ticket.ready = false
                        if (wasReady) changed()
                        worker.postDelayed(
                            { guard("refresh") { hardRefresh(ticket, requestEpoch) } },
                            if (wasReady) 80 else 0)
                    }
                }) {
                    recover(ticket, "resume probe ${it.javaClass.simpleName}: ${it.message?.take(160)}")
                }
            }
        }
        if (retained) {
            // Register after the caller has written position/crop/visibility. A Java rotation
            // callback can precede this transaction: sampling there captures the old region.
            val commitRegistered = catchingRecoverableOr(false, {
                afterCommit!!.addTransactionCommittedListener(
                    { command -> worker.post { guard("capture commit callback") { command.run() } } },
                    // Resuming at the pose commit shows the retained frame at once, which already
                    // carries the settled portrait sample. The frames sampled while the vendor's
                    // blur geometry still carries the rotated transform are reported not-ready by
                    // the host (geometryValid), so the compositor fallback covers exactly that
                    // window and the glass reappears when the geometry catches up - no timer.
                    { resumeProbe() })
                true
            })
            if (!commitRegistered) {
                // Never strand the capture in its paused state: without the listener the retained
                // frame would be the last thing this host ever shows.
                record("glass capture commit listener unavailable; timing the return instead")
                worker.postDelayed({ resumeProbe() }, CAPTURE_RESUME_FALLBACK_MS)
            }
        } else worker.postDelayed({ resumeProbe() }, probeDelayMs)
    }

    /**
     * The display finished a rotation cycle while this ticket kept a ready host.
     *
     * <p>Two things are stale after a rotation: the host's blur geometry, which the probe
     * recomputes, and the capture texture itself, which still holds the closed app's content until
     * the compositor produces a new frame. Require a post-return frame and keep the compositor
     * fallback for the one or two frames that takes, instead of painting the previous foreground's
     * colours over the returning Dock. The requirement is bounded inside the host, so a static
     * background cannot strand the panel. The forced probe must not be swallowed by the return
     * deduplication window that collapses a visibility callback describing the same return.
     */
    /**
     * Resume a capture that was frozen while the Dock was hidden, at the earliest signal that the
     * launcher is coming back.
     *
     * <p>The rotation return is announced by the launcher's own wallpaper command before the display
     * reports portrait, and the panel only becomes visible a traversal later. Resuming here - at the
     * front of the worker queue - means the retained home sample is already being drawn when the
     * panel appears, instead of the compositor fallback flashing until a resume catches up.
     */
    fun resumeFrozenCapture(ticket: Ticket, reason: String) {
        if (closed || ticket.cancelled || ticket.dead) return
        worker.postAtFrontOfQueue {
            guard("frozen resume") {
                if (closed || ticket.cancelled || ticket.dead || ticket.lease == null) return@guard
                if (!ticket.capturePaused && !ticket.retainedCapture) return@guard
                catchingRecoverable({
                    val response = ticket.request(
                        if (ticket.capturePaused) "dock_glass_resume_capture" else "dock_glass_probe")
                    response.getString("error")?.let { error(it) }
                    val resumedRetained = ticket.retainedCapture
                    ticket.capturePaused = false
                    ticket.captureUndrawn = false
                    ticket.retainedCapture = false
                    val wasReady = ticket.ready
                    val healthy = ticket.lease?.isAttached == true
                        && response.getBoolean("backgroundReady")
                    if (healthy) {
                        ticket.ready = true
                        ticket.gate.markReady()
                        if (!wasReady) changed()
                        if (resumedRetained) record("glass frozen capture resumed id=${ticket.id} " +
                            "reason=$reason geometry=${response.getString("captureGeometry")}")
                    } else {
                        ticket.ready = false
                        if (wasReady) changed()
                    }
                }) {
                    record("glass frozen resume failed=${it.javaClass.simpleName}: ${it.message?.take(120)}")
                    recover(ticket, "frozen resume ${it.javaClass.simpleName}")
                }
            }
        }
    }

    fun resumeAfterRotation(ticket: Ticket) {
        if (closed || ticket.cancelled || ticket.dead) return
        // A capture frozen while the Dock was hidden already holds the last home sample, so it can
        // be shown as soon as it resumes. The wallpaper command normally resumes it earlier still;
        // this covers a rotation that never emits one.
        if (ticket.retainedCapture) {
            worker.postDelayed(
                { resumeFrozenCapture(ticket, "rotation-return") },
                ROTATION_THAW_DELAY_MS)
            return
        }
        // A host that kept sampling the foreground app needs a fresh frame before it may present
        // again; drop to the compositor fallback for that window synchronously, before any traversal
        // can composite the first returning frame.
        if (ticket.ready) {
            ticket.ready = false
            changed()
        }
        worker.postAtFrontOfQueue {
            guard("rotation freshness") {
                if (closed || ticket.cancelled || ticket.dead || ticket.lease == null) return@guard
                // The display can reverse while this task is queued.
                if (rotationActive()) return@guard
                catchingRecoverable({ ticket.request("dock_glass_fresh_capture") }) {
                    record("glass freshness mark failed=${it.javaClass.simpleName}")
                }
                resume(ticket, allowFallback = false, probeDelayMs = 0L, force = true)
            }
        }
    }

    /**
     * Absent on a host from before the geometry gate: fail open rather than hiding the glass.
     */
    private fun Bundle.geometryValidOrDefault(): Boolean =
        if (containsKey("geometryValid")) getBoolean("geometryValid") else true

    private fun hardRefresh(ticket: Ticket, requestEpoch: Int) {
        if (closed || ticket.cancelled || ticket.dead || ticket.lease == null) return
        if (!ticket.gate.isRefreshCurrent(requestEpoch)) {
            if (ticket.gate.noteStaleCallback()) {
                record("glass stale refresh ignored id=${ticket.id} epoch=$requestEpoch")
            }
            return
        }
        catchingRecoverable({
            val response = ticket.request("dock_glass_refresh")
            response.getString("error")?.let { error(it) }
            ticket.refreshes++
            val generation = ticket.gate.getAttempts()
            val readinessEpoch = ticket.gate.openReadinessEpoch()
            if (ticket.refreshes <= 8) {
                record("glass stale producer restarted id=${ticket.id} count=${ticket.refreshes}")
            }
            checkBackground(ticket, 0, generation, readinessEpoch)
        }) {
            recover(ticket, "refresh ${it.javaClass.simpleName}: ${it.message?.take(160)}")
        }
    }

    /** Cancel delayed refresh/readiness work while the launcher parent is hidden. */
    /**
     * Hand the unlock epoch to the glass view once per unlock.
     *
     * <p>The 3D projection runs in the module's own process against its own Choreographer,
     * because the SurfaceControl carrying the glass only supports an affine matrix. One
     * absolute timestamp crosses the boundary; everything else is derived locally, so no
     * per-frame IPC is needed and a slow round trip cannot stall a frame.
     */
    fun notifyUnlock(ticket: Ticket, startedAtMs: Long, style: DockUnlockReveal.Style) {
        if (closed || ticket.cancelled) return
        worker.post {
            guard("unlock reveal") {
                catchingRecoverable({
                    val args = Bundle().apply {
                        putLong("startedAtMs", startedAtMs)
                        putLong("durationMs", DockUnlockReveal.DURATION_MS)
                        putLong("leadMs", DockUnlockReveal.ICON_LEAD_MS)
                        putString("style", style.name)
                    }
                    ticket.request("dock_glass_unlock", args)
                })
            }
        }
    }

    fun pauseRefresh(ticket: Ticket) {
        // Cancel both loops immediately, including one opened by an in-flight create after
        // the parent became hidden. Resume explicitly reopens them when the Dock returns.
        //
        // Deliberately NOT pausing the capture: updateTextureState(false) puts the whole host
        // into the vendor's undraw state, and the departure fires while the Dock is still on
        // screen (the wallpaper zoom starts first), so every app switch visibly swapped the
        // panel to the compositor fallback and back. The rotation mis-sampling this pause used
        // to protect against is now handled precisely by the host's geometry gate, which costs
        // a fallback only for the frames that are actually mapped wrong.
        ticket.gate.pauseRefresh()
    }

    fun holdForDeparture(ticket: Ticket) {
        val epoch = ticket.gate.holdForDeparture()
        if (epoch != DockGlassRecoveryGate.REFRESH_DEDUPLICATED) {
            pauseCapture(ticket, epoch, "before-wallpaper-zoom")
        }
    }

    fun releaseDepartureHold(ticket: Ticket) = ticket.gate.releaseDepartureHold()

    /**
     * Make the host drawable again for a fly-in that is about to run.
     *
     * <p>The capture is paused whenever the launcher is not visible, and that is exactly the state
     * the Dock sits in while the keyguard is up. Leaving it paused through the reveal left the host
     * in the vendor's "undraw" state for the whole animation: a host that is not drawn cannot be
     * composited, so every entrance style animated its container while the panel itself was
     * missing. The reveal needs the capture live before its first frame, so this resumes it now -
     * without the return settle window, which exists for the wallpaper animation, not for this -
     * and deliberately keeps the ticket ready: the retained frame is still valid, and dropping to
     * the compositor fallback mid-reveal would be a worse artefact than the frame it replaces.
     */
    fun resumeForReveal(ticket: Ticket) {
        if (closed || ticket.cancelled || ticket.dead) return
        ticket.gate.releaseDepartureHold()
        if (!ticket.capturePaused) return
        worker.post {
            guard("reveal capture resume") {
                if (closed || ticket.cancelled || ticket.dead || ticket.lease == null) return@guard
                catchingRecoverable({
                    val response = ticket.request("dock_glass_resume_capture")
                    response.getString("error")?.let { error(it) }
                    val wasPaused = ticket.capturePaused
                    ticket.capturePaused = false
                    ticket.captureUndrawn = false
                    ticket.retainedCapture = false
                    if (wasPaused) {
                        record("glass capture resumed for reveal id=${ticket.id}")
                        // The panel falls back to the compositor while a capture is paused, so the
                        // traversal has to rewrite the appearance before the animation's first
                        // frame - otherwise the reveal starts on the fallback and swaps mid-flight.
                        changed()
                    }
                }) {
                    record("glass reveal capture resume failed=${it.javaClass.simpleName}")
                }
            }
        }
    }

    private fun pauseCapture(ticket: Ticket, pauseEpoch: Int, reason: String) {
        val lease = ticket.lease
        // Jump the queue: the freeze has to land before the compositor captures the foreground app,
        // and a busy worker otherwise delays it by a hundred milliseconds or more.
        worker.postAtFrontOfQueue {
            guard("pause capture") {
                if (closed || ticket.cancelled || ticket.dead || !ticket.ready || ticket.capturePaused
                    || lease == null || ticket.lease !== lease || !ticket.gate.isPauseCurrent(pauseEpoch)) return@guard
                catchingRecoverable({
                    val response = ticket.request("dock_glass_pause_capture")
                    response.getString("error")?.let { error(it) }
                    ticket.capturePaused = response.getBoolean("capturePaused")
                    ticket.captureUndrawn = ticket.capturePaused && !response.getBoolean("frozenDrawn")
                    ticket.retainedCapture = ticket.capturePaused && response.getBoolean("retained")
                    record("glass capture frozen id=${ticket.id} retained=${ticket.retainedCapture} " +
                        "reason=$reason geometry=${response.getString("captureGeometry")}")
                    changed()
                }) {
                    record("glass capture freeze unavailable=${it.javaClass.simpleName}")
                }
            }
        }
    }

    private fun recover(ticket: Ticket, reason: String) {
        val wasReady = ticket.ready
        val outcome = ticket.gate.requestRecovery(closed, wasReady)
        if (outcome == DockGlassRecoveryGate.Outcome.IGNORED) return
        ticket.dead = true
        ticket.ready = false
        dispose(ticket)
        record("glass failed id=${ticket.id} attempt=${ticket.gate.getAttempts()} " +
            "failures=${ticket.gate.getConsecutiveFailures()} " +
            "deduplicated=${ticket.gate.getDeduplicatedRecoveries()} " +
            "retryMs=${ticket.gate.getRetryDelayMs()} reason=$reason")
        changed()
        scheduleRetry(ticket, outcome, ticket.gate.getRetryDelayMs())
    }

    /**
     * The HyperCeiler package itself is momentarily unresolvable, typically because it is
     * being replaced by an in-place upgrade.
     *
     * <p>Nothing is disposed and no compatibility budget is consumed: this is an external,
     * self-healing outage. The retry cadence is capped, so a long outage stays bounded.
     */
    private fun deferForDependency(ticket: Ticket) {
        val outcome = ticket.gate.requestDependencyDefer(closed)
        if (outcome == DockGlassRecoveryGate.Outcome.IGNORED) return
        ticket.dead = true
        if (ticket.ready) {
            ticket.ready = false
            changed()
        }
        if (ticket.gate.shouldReportDependencyDefer()) {
            record("glass dependency unavailable id=${ticket.id} " +
                "attempt=${ticket.gate.getAttempts()} retryMs=${ticket.gate.getRetryDelayMs()}")
        }
        scheduleRetry(ticket, outcome, ticket.gate.getRetryDelayMs())
    }

    private fun scheduleRetry(ticket: Ticket, outcome: DockGlassRecoveryGate.Outcome, delayMs: Long) {
        when (outcome) {
            DockGlassRecoveryGate.Outcome.RETRY_NOW,
            DockGlassRecoveryGate.Outcome.RETRY_LATER ->
                worker.postDelayed({ guard("retry") { attemptCreate(ticket) } }, delayMs)
            DockGlassRecoveryGate.Outcome.EXHAUSTED ->
                XposedLog.w("DockGlass", "system", "Glass recovery budget exhausted; retaining fallback")
            DockGlassRecoveryGate.Outcome.IGNORED -> Unit
        }
    }

    fun release(ticket: Ticket) {
        // Retire exactly once: a repeated release must not queue a second teardown, and it
        // must also cancel every retry/recovery already scheduled for this ticket.
        if (!ticket.gate.markRetired()) {
            record("glass release ignored id=${ticket.id} reason=already retired")
            return
        }
        record("glass cancelled id=${ticket.id}")
        worker.post { guard("release") { dispose(ticket) } }
    }

    fun close() {
        if (closed) return
        // Marking the client closing first stops every later task from touching resources,
        // including retries already posted by recover().
        closed = true
        styleObserver?.let { observer ->
            catchingRecoverable({ styleContext?.contentResolver?.unregisterContentObserver(observer) })
        }
        styleObserver = null
        if (workerDelegate.isInitialized()) worker.post {
            guard("closing") {
                journal.record("glass client closing")
                journal.finish()
            }
            worker.looper.quitSafely()
        }
    }

    private fun dispose(ticket: Ticket): Boolean {
        ticket.ready = false
        ticket.capturePaused = false
        ticket.captureUndrawn = false
        ticket.retainedCapture = false
        ticket.death?.let { catchingRecoverable({ ticket.lifetime?.unlinkToDeath(it, 0) }) }
        ticket.death = null
        ticket.lifetime = null
        try {
            val lease = ticket.lease
            if (lease != null) lease.close() else ticket.parcel?.release()
        } catch (error: IllegalStateException) {
            // Keep the last handle and do not create another host until cleanup
            // succeeds. Recovery's bounded backoff retries this same retirement.
            record("glass detach failed id=${ticket.id}: ${error.javaClass.simpleName}")
        } catch (error: SecurityException) {
            record("glass detach failed id=${ticket.id}: ${error.javaClass.simpleName}")
            return false
        }
        ticket.lease = null
        ticket.parcel = null
        // Do not reacquire/start a renderer merely to release a failed acquisition.
        try {
            if (ticket.client != null) catchingRecoverable({ ticket.request("dock_glass_release") })
        } finally {
            catchingRecoverable({ ticket.client?.close() })
            ticket.client = null
            processGuard.release(ticket)
        }
        return true
    }

}
