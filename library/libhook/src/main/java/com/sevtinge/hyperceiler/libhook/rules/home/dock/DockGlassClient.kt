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

        /** Two seconds is imperceptible when picking a style and cheap while the dock is up. */
        const val STYLE_QUERY_INTERVAL_MS = 2_000L

        /**
         * How long the retained frame keeps being presented after the return pose commits.
         *
         * <p>Sized from the on-device trace of 2026-09-14 09:57: the pose commits 830 ms after the
         * launcher starts its wallpaper zoom and the return motion only goes idle 475 ms after
         * that commit. A capture started inside that window samples the wallpaper mid-zoom, which
         * is the one visible change reported *after* the desktop looks settled - so this is set
         * well past the measured end, because holding a frame that already carries the settled
         * portrait sample costs nothing, while switching to a transitional one is visible.
         */
        const val WALLPAPER_RETURN_SETTLE_MS = 800L

        /** Slack added to that window for the timed fallback when the commit listener is unusable. */
        const val CAPTURE_RESUME_FALLBACK_MS = 1_200L
    }
    @Volatile private var closed = false
    @Volatile private var styleContext: Context? = null
    @Volatile private var liveRevealStyle: String? = null
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
            val message = "wall=${System.currentTimeMillis()} up=${SystemClock.uptimeMillis()} $event".take(512)
            // Deliberately independent of the release build's error-only Xposed log filter.
            Log.i("HyperCeiler.DockGlass", message)
            worker().post {
                if (events.size >= 96) events.removeFirst()
                events.addLast(message)
                if (!diagnosticFlushScheduled) {
                    diagnosticFlushScheduled = true
                    worker().postDelayed(diagnosticFlush, 250)
                }
            }
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
        if (!closed) journal.bind(context)
    }

    fun record(event: String) { if (!closed) journal.record(event) }

    /** Latest reveal style name read from the module's own preferences, or null before the first read. */
    fun liveRevealStyle(): String? = liveRevealStyle

    /**
     * Re-read the unlock fly-in style from the module provider.
     *
     * <p>The style is consumed in system_server, but LSPosed's remote preferences there are a
     * snapshot pushed by the daemon: when that push is lost the value stays stale for the rest
     * of the process lifetime, and neither a launcher restart nor anything else refreshes it.
     * The provider reads the settings file the UI actually wrote, so querying it directly makes
     * a style change take effect within a couple of seconds. Throttled because every launcher
     * traversal would otherwise issue its own cross-process query.
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
                        Uri.parse("$uri/string/prefs_key_home_dock_unlock_style"),
                        null, null, null, null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val value = cursor.getString(0)
                            if (liveRevealStyle != value) {
                                liveRevealStyle = value
                                record("reveal style queried=$value")
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
                    // A host whose background was captured while the display was still rotating
                    // must not be shown as ready: the settle window is what keeps that sample off
                    // screen until the wallpaper has landed in portrait.
                    ticket.ready = attached && status.getBoolean("backgroundReady")
                        && !rotationSettling()
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
        afterCommit: SurfaceControl.Transaction? = null) {
        // Visibility and wallpaper-home callbacks can describe the same return.
        val retained = ticket.retainedCapture
        // Wallpaper commands do not own a SurfaceControl transaction. Let the traversal
        // couple retained sampling to the transaction that actually restores the Dock pose.
        if (retained && afterCommit == null) {
            changed()
            return
        }
        val requestEpoch = ticket.gate.requestRefresh(SystemClock.uptimeMillis(),
            retained && rotationActive())
        if (requestEpoch == DockGlassRecoveryGate.REFRESH_DEDUPLICATED) return
        val requestedAt = SystemClock.uptimeMillis()
        val resumeProbe = Runnable {
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
                    ticket.retainedCapture = false
                    val healthy = ticket.lease?.isAttached == true
                        && response.getBoolean("backgroundReady") && (ticket.ready || !rotationSettling())
                    if (healthy) {
                        val wasReady = ticket.ready
                        ticket.ready = true
                        ticket.gate.markReady()
                        if (!wasReady) changed()
                        if (resumedRetained) record("glass capture resumed with retained texture id=${ticket.id} " +
                            "timing=pose-committed waitMs=${SystemClock.uptimeMillis() - requestedAt} " +
                            "geometry=${response.getString("captureGeometry")}")
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
                    // The pose commit is not the end of the return. Measured on-device
                    // (2026-09-14 09:53): the launcher starts its wallpaper zoom at T, the pose
                    // commits at T+832ms, and its return motion only goes idle at T+1303ms. A
                    // capture started at the commit therefore samples the wallpaper mid-zoom,
                    // which is the one visible change reported *after* the desktop looks settled.
                    // Keep presenting the retained frame through that window: it already carries
                    // the settled portrait sample, so holding it one animation longer is
                    // invisible, while switching to a transitional capture is not.
                    { worker.postDelayed({ resumeProbe.run() }, WALLPAPER_RETURN_SETTLE_MS) })
                true
            })
            if (!commitRegistered) {
                // Never strand the capture in its paused state: without the listener the retained
                // frame would be the last thing this host ever shows.
                record("glass capture commit listener unavailable; timing the return instead")
                worker.postDelayed({ resumeProbe.run() },
                    WALLPAPER_RETURN_SETTLE_MS + CAPTURE_RESUME_FALLBACK_MS)
            }
        } else worker.postDelayed(resumeProbe, 50)
    }

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
        pauseCapture(ticket, ticket.gate.pauseRefresh(), "hidden")
    }

    fun holdForDeparture(ticket: Ticket) {
        val epoch = ticket.gate.holdForDeparture()
        if (epoch != DockGlassRecoveryGate.REFRESH_DEDUPLICATED) {
            pauseCapture(ticket, epoch, "before-wallpaper-zoom")
        }
    }

    fun releaseDepartureHold(ticket: Ticket) = ticket.gate.releaseDepartureHold()

    private fun pauseCapture(ticket: Ticket, pauseEpoch: Int, reason: String) {
        val lease = ticket.lease
        worker.post {
            guard("pause capture") {
                if (closed || ticket.cancelled || ticket.dead || !ticket.ready || ticket.capturePaused
                    || lease == null || ticket.lease !== lease || !ticket.gate.isPauseCurrent(pauseEpoch)) return@guard
                catchingRecoverable({
                    val response = ticket.request("dock_glass_pause_capture")
                    response.getString("error")?.let { error(it) }
                    ticket.capturePaused = response.getBoolean("capturePaused")
                    ticket.retainedCapture = ticket.capturePaused && response.getBoolean("retained")
                    record("glass capture paused id=${ticket.id} retained=${ticket.retainedCapture} " +
                        "reason=$reason geometry=${response.getString("captureGeometry")}")
                    changed()
                }) {
                    record("glass capture pause unavailable=${it.javaClass.simpleName}")
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
