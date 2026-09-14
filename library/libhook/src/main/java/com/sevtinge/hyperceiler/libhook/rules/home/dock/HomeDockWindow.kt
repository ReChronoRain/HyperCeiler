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

import android.content.SharedPreferences
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Bundle
import android.os.IBinder
import android.os.Parcel
import android.os.SystemClock
import android.provider.Settings
import android.view.Display
import android.view.Surface
import android.view.SurfaceControl
import android.view.WindowManager
import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectFieldAs
import java.util.IdentityHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * HYOS launcher has no ART Activity: its Java module entry and JNI preference setter do not run.
 * WMS still owns its windows. Attach an effect layer below the launcher buffer, above wallpaper,
 * using the existing system scope and remote preferences. No injected input window is necessary.
 */
class HomeDockWindow : BaseHook() {
    private companion object {
        const val LOG_TAG = "system"
        const val METHOD_CLOSE = "close"
        const val METHOD_RELEASE = "release"
        const val WM_HANDLER = "mH"
        const val WM_LOCK = "mGlobalLock"
        const val IS_VALID = "isValid"
        const val SET_POSITION = "setPosition"
        const val SET_LAYER = "setLayer"
        const val SET_CROP = "setWindowCrop"
        const val SET_RADIUS = "setCornerRadius"
        const val TRANSACTION = "android.view.SurfaceControl\$Transaction"
        const val STALE_FRAME_NS = 50_000_000L
        /**
         * Cadence of the motion tick that replaced our private vsync clock. The panel runs at
         * 120Hz, so 8ms keeps the row in step with it without owning a DisplayEventReceiver.
         */
        const val FRAME_INTERVAL_MS = 8L
        /**
         * Title prefix of the launcher's overlay window - the minus-one screen. The launcher
         * names the window itself (the string never appears in services.jar), and WMS owns it,
         * so it is the one launcher state this process can observe.
         */
        const val LAUNCHER_OVERLAY_TITLE = "LauncherOverlayWindow"
        /** Minimum gap between window-map scans while the overlay is not known to be showing. */
        const val OVERLAY_SCAN_MS = 50L
        /** Failures older than this start a fresh count, so a slow boot hiccup self-heals. */
        const val PREPARE_FAILURE_WINDOW_MS = 10_000L
        /** Consecutive failures inside that window before the Dock gives up for this process. */
        const val PREPARE_FAILURE_LIMIT = 3
        const val NATIVE_BIND_SWEEP_MS = 1_000L
        /**
         * Ceiling for the sweep's idle backoff.
         *
         * <p>The sweep's job is to re-assert what the framework does not guarantee on an idle
         * window: the native motion identity and the dock preferences. Neither needs a timer while
         * nothing changes - every traversal, layer change and preference event resets the cadence
         * (see [resetSweepCadence]). The ceiling is deliberately low (8 s, not minutes): if any
         * state is ever wrong, this is also how long the Dock can stay wrong, and 2026-09-14 showed
         * a mis-evaluated overlay flag can hold the Dock hidden until the next traversal.
         */
        const val SWEEP_BACKOFF_MAX_MS = 8_000L
        /** Reveal (821ms) plus a margin for the keyguard/home wallpaper swap to settle. */
        const val MATERIAL_SETTLE_MS = 1_600L
        /** Slack added to the rotation settle window before asking for the resuming traversal. */
        const val ROTATION_RESUME_MARGIN_MS = 80L
    }
    private object Surfaces {
        fun buildLayer(name: String, parent: Any, color: Boolean): Any {
            val builder = loadClass("android.view.SurfaceControl\$Builder").getConstructor().newInstance()
            builder.callMethod("setName", name)
            builder.callMethod("setParent", parent)
            builder.callMethod("setHidden", true)
            builder.callMethod(if (color) "setColorLayer" else "setEffectLayer")
            return builder.callMethod("build")!!
        }

        fun destroySurface(surface: Any) {
            val transaction = loadClass(TRANSACTION).getConstructor().newInstance()
            try {
                if (surface.callMethod(IS_VALID) == true) {
                    transaction.callMethod("remove", surface)
                    transaction.callMethod("apply")
                }
            } finally {
                try { transaction.callMethod(METHOD_CLOSE) } finally { surface.callMethod(METHOD_RELEASE) }
            }
        }

    }

    private data class Settings(
        val enabled: Boolean, val mode: Int, val color: Int, val height: Int,
        val margin: Int, val bottom: Int, val radius: Int, val nightMode: Int,
        val revealStyle: DockUnlockReveal.Style
    ) {
        companion object {
            fun read() = Settings(
                PrefsBridge.getBoolean("home_dock_bg_custom_enable"),
                DockWindowPolicy.normalizeBackgroundMode(PrefsBridge.getStringAsInt("home_dock_add_blur", 1)),
                PrefsBridge.getInt("home_dock_bg_color", 0),
                PrefsBridge.getInt("home_dock_bg_height", 150),
                PrefsBridge.getInt("home_dock_bg_margin_horizontal", 25),
                PrefsBridge.getInt("home_dock_bg_margin_bottom", 15),
                PrefsBridge.getInt("home_dock_bg_radius", 30),
                PrefsBridge.getStringAsInt("home_other_home_mode", 0),
                DockUnlockReveal.Style.of(PrefsBridge.getString("home_dock_unlock_style", "daybreak"))
            )
        }
        val blur get() = mode == 1 || mode == DockGlassPreset.MODE
        val glass get() = mode == DockGlassPreset.MODE
    }

    private data class Appearance(val config: Settings, val bounds: DockWindowPolicy.Bounds,
        val dark: Boolean, val visible: Boolean, val glass: DockGlassClient.Ticket?) {
        val surface = glass?.lease
        // A capture frozen with the drawable freeze still composites its last home frame, so the
        // panel stays native glass. Only the vendor's undraw fallback has no visible source and
        // needs the compositor fallback to carry it. Readiness is part of the key, so this flip
        // re-runs applyAppearance.
        val ready = glass?.ready == true && !glass.dead && surface != null && !glass.captureUndrawn
        val key = "$config/$bounds/$dark/$visible/$surface/$ready/${glass?.dead}/" +
            "${glass?.capturePaused}/${glass?.captureUndrawn}"
    }

    private data class Layer(val parent: Any, val effect: Any, val tint: Any,
        var appearance: String = "", var glass: DockGlassClient.Ticket? = null,
        var glassRotationEpoch: Int = 0,
        val motion: DockRecentsMotion = DockRecentsMotion(),
        val nativeMotion: DockNativeMotion = DockNativeMotion(),
        var nativeUid: Int = -1, var nativePid: Int = -1,
        var nativeApplied: Boolean = false, var overview: Boolean = false,
        var nativeScene: Int = -1,
        var overviewGeneration: Long = 0, var nativeOverviewGeneration: Long = 0,
        var lastOverviewValidationNs: Long = 0,
        var lastVisible: Boolean? = null,
        /** True while the launcher's overlay (minus-one screen) is what keeps this layer hidden. */
        var overlayHidden: Boolean = false,
        var lastGlassReady: Boolean? = null,
        var motionSession: Any? = null, var motionClient: IBinder? = null,
        var motionSamples: Int = 0, var motionEndPending: Boolean = false,
        var baseY: Int = 0, var density: Float = 0f, var motionTime: Long = 0,
        var nativeSampleDeadlineNs: Long = 0,
        var autoAimSequence: Long = 0, var autoAimDeadlineNs: Long = 0,
        var autoAimLive: Boolean = false, var autoAimSamples: Int = 0,
        var x: Float = Float.NaN, var y: Float = Float.NaN,
        val reveal: DockUnlockReveal = DockUnlockReveal(),
        var width: Int = 0, var height: Int = 0, var radius: Float = 0f,
        var revealScaleX: Float = 1f, var revealScaleY: Float = 1f,
        var revealCropWidth: Float = 1f, var revealCropHeight: Float = 1f,
        var revealCornerProgress: Float = 1f,
        var revealAlpha: Float = 1f, var revealSlide: Float = 0f,
        /** When the current glass host died, so its replacement is rate limited. */
        var glassDroppedAt: Long = 0L) {
        /** Drop every cached native identity/sample after the launcher Session changed. */
        fun resetNativeMotion() {
            nativeMotion.reset()
            nativeApplied = false
            nativeScene = -1
            nativeSampleDeadlineNs = 0
            motionSamples = 0
            motionEndPending = false
            nativeUid = -1
            nativePid = -1
            resetAutoAim()
        }

        /** Drop the live unlock footprint without disturbing the independent recents state. */
        fun resetAutoAim() {
            autoAimSequence = 0
            autoAimDeadlineNs = 0
            autoAimLive = false
            autoAimSamples = 0
        }
    }
    private val layers = IdentityHashMap<Any, Layer>()
    private val observed = HashSet<String>()
    @Volatile private var stopped = false
    @Volatile private var settings = Settings.read()
    @Volatile private var service: Any? = null
    private var blurAvailable = true
    private var revealScalingAvailable = true
    private var revealCropAvailable = true
    // Protected by WM_LOCK, then layers. Covers a Dock created by the unlock traversal itself.
    private var pendingRevealAt = -1L

    // TODO(twitch-diag): temporary - true for 3s after each unlock so the post-reveal write
    // sequence (which writer moves the dock when) can be traced; remove once the twitch is fixed.
    private fun revealDebugActive(): Boolean =
        pendingRevealAt >= 0L && SystemClock.uptimeMillis() - pendingRevealAt <= 3000L

    // TODO(twitch-diag): rate limit for the temporary frame-write trace.
    private var lastFrameDbgUptime = 0L

    /**
     * Whether the unlock's wallpaper-swap settle window is active.
     *
     * While the keyguard and home wallpapers differ, the transition swaps them, which flips the
     * glass darkness probe and would tear the material down and rebuild it mid-transition - the
     * flash the user sees as "sampling two different wallpapers". During this window the dock
     * keeps its current material (and the reveal's fade-in hides most of that period anyway);
     * exactly one rebuild is allowed once the window closes.
     */
    private fun materialSettleActive(): Boolean =
        pendingRevealAt >= 0L && SystemClock.uptimeMillis() - pendingRevealAt <= MATERIAL_SETTLE_MS
    private var commandSamples = 0
    private val processGuard = DockGlassProcessGuard()
    private val displayListenerRegistered = AtomicBoolean(false)
    private val rotationHookFailureReported = AtomicBoolean(false)
    private val glassClient = DockGlassClient(processGuard, { requestTraversal() },
        // New hosts retain the settle guard; a retained source can resume as soon as portrait returns.
        rotationSettling = { layerUpdate.isRotationSettling() },
        rotationActive = { layerUpdate.isDisplayRotated() })
    private val nativeMotionEndpoint = DockNativeMotionEndpoint(
        {
            if (directMotionAvailable) scheduleAnimationFrame(true) else {
                requestTraversal()
                scheduleDirectMotionRecovery()
            }
        },
        {
            // Native's idle packet does not refresh motion state. It only proves transport
            // liveness and exercises our dedicated frame receiver before the next gesture.
            if (directMotionAvailable) scheduleAnimationFrame() else scheduleDirectMotionRecovery()
        },
        glassClient::record)
    private val nativeMotionReply = ThreadLocal<Int>()
    private data class ScheduledFrame(val epoch: Long, val tick: Runnable)
    /** One background transform for one frame. The pose is identity unless the reveal is live. */
    private data class FrameUpdate(val layer: Layer, val y: Float,
        val pose: DockUnlockReveal.ContainerPose, val alpha: Float, val slide: Float,
        var appliedScaleX: Float = 1f, var appliedScaleY: Float = 1f)
    private val frameEpoch = AtomicLong(0)
    private val scheduledFrameEpoch = AtomicLong(0)
    private val scheduledFrameStartedNs = AtomicLong(0)
    private val urgentFrameRecoveryScheduled = AtomicBoolean(false)
    private val directRecoveryScheduled = AtomicBoolean(false)
    private val nativeBindSweepScheduled = AtomicBoolean(false)
    @Volatile private var animationAvailable = true
    @Volatile private var directMotionAvailable = true
    // Owned and used only on WMS's handler thread, and driven by a plain Handler post rather
    // than a Choreographer. A vsync receiver we create inside system_server can be torn down
    // by the platform (null DisplayEventReceiver.mReceiverPtr) while the display looper still
    // holds a dispatched callback; the next getLatestVsyncEventData then dereferences it and
    // kills system_server with a SIGSEGV that no try/catch can survive. That is why this file
    // no longer creates, retains or un-latches a frame clock: a Handler tick has no lifetime
    // the platform can yank out from under us.
    private var motionTransaction: Any? = null
    private var scheduledFrame: ScheduledFrame? = null
    private var directRecoveryDelay = 100L
    // Display thread only. Cached overlay WindowState and its last observed visibility; see
    // launcherOverlayVisible(). The overlay outlives a page change but not a launcher restart.
    private var overlayWindow: Any? = null
    private var overlayScannedAt = 0L
    private var overlayVisible = false
    // Display thread only. Consecutive pose/traversal failures inside one short window; see
    // notePrepareFailure(). Reset as soon as the failures stop being consecutive.
    private var prepareFailures = 0
    private var prepareFailureWindowAt = 0L
    /** Minimum gap between replacing a dead glass host, so a crash loop cannot spin. */
    private val GLASS_REBUILD_BACKOFF_MS = 2_000L
    /** Current sweep cadence; backs off while idle, resets to the fast interval on any activity. */
    @Volatile private var sweepIntervalMs = NATIVE_BIND_SWEEP_MS

    /**
     * Return the sweep to its fast cadence.
     *
     * <p>Called from every path that means "something about the launcher may have changed": a
     * traversal, a layer appearing or disappearing, a preference event, a launcher window being
     * (re)added. Backing off is only correct while none of those happen.
     */
    private fun resetSweepCadence() {
        sweepIntervalMs = NATIVE_BIND_SWEEP_MS
    }

    override fun init() {
        refreshSettings()
        glassClient.record("hook init diagnosticVersion=33 enabled=${settings.enabled} mode=${settings.mode}")
        glassClient.record("glass capture warmup=positive-alpha-v1 idleWait=retain-producer-v1")
        glassClient.record("glass rotation return=retained-capture-v3 pause=before-wallpaper-zoom resume=pose-committed")
        runCatching { processGuard.install() }
            .onFailure { glassClient.record("renderer guard unavailable=${it.javaClass.simpleName}") }
        val prefs = PrefsBridge.getSharedPreferences()
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || key.contains("home_dock_") || key.endsWith("home_other_home_mode")) {
                // Advisory only. The callback now tells us when it is worth redrawing, it is no
                // longer what makes a value visible: the sweep and every traversal read the
                // preferences themselves. Refresh first so the requested traversal cannot see the
                // previous value, and never fail the hook closed over a read.
                resetSweepCadence()
                refreshSettings()
                requestTraversal()
            }
        }
        prefs?.registerOnSharedPreferenceChangeListener(listener)
        registerHotReloadCleanup {
            stopped = true
            prefs?.unregisterOnSharedPreferenceChangeListener(listener)
            synchronized(layers) { layers.keys.toList().forEach { removeLayer(it) } }
            glassClient.close()
            processGuard.close()
            service?.getObjectFieldAs<Handler>(WM_HANDLER)?.post {
                cancelScheduledFrame()
                runCatching { motionTransaction?.callMethod("close") }
                motionTransaction = null
                directRecoveryScheduled.set(false)
                nativeBindSweepScheduled.set(false)
            }
        }
        WindowHooks(loadClass("com.android.server.wm.WindowState")).install()
        installRotationHook()
        installUnlockReveal()
        XposedLog.i(TAG, LOG_TAG, "WMS dock hook ready: enabled=${settings.enabled}, blur=${settings.blur}")
    }

    /**
     * Re-read every dock preference and publish the new values to [settings].
     *
     * The launcher is drawn by WMS, so restarting the desktop does not re-create this hook - it
     * only re-creates the launcher's WindowState. The snapshot taken at hook init is therefore
     * *not* refreshed by a desktop restart, and the remote preference map only advances when the
     * framework pushes an update into this process. Whenever that push is lost - LSPosed shipped a
     * dedicated fix for exactly this on system_server - the geometry stayed pinned to whatever was
     * current when system_server loaded the module. That is the reported "I changed the height,
     * restarted the desktop, nothing happened", while a local test that happened to keep the
     * update path alive appeared to work.
     *
     * Reading here is cheap (a handful of in-memory lookups on the remote map) and every caller is
     * already doing far more expensive work - one is a full layout pass. A failed read keeps the
     * last known-good values rather than failing the hook closed: a stale dock is strictly better
     * than no dock.
     *
     * @return true when at least one value actually changed.
     */
    private fun refreshSettings(): Boolean {
        val latest = runCatching { Settings.read() }.getOrNull() ?: return false
        // The provider reads the file the settings UI wrote, so it is the source of truth for
        // the reveal style; the remote snapshot system_server sees can be stale for a whole
        // process lifetime when the daemon's push is lost.
        val live = glassClient.liveRevealStyle()
        val resolved = if (live == null) latest
            else latest.copy(revealStyle = DockUnlockReveal.Style.of(live))
        if (resolved == settings) return false
        val previousStyle = settings.revealStyle
        settings = resolved
        // A style change must reach already-built layers; new layers read it at creation.
        synchronized(layers) {
            layers.values.forEach {
                it.reveal.setStyle(resolved.revealStyle)
                if (previousStyle != resolved.revealStyle) it.resetAutoAim()
            }
        }
        glassClient.record("prefs applied enabled=${resolved.enabled} mode=${resolved.mode} " +
            "height=${resolved.height} margin=${resolved.margin} bottom=${resolved.bottom} " +
            "radius=${resolved.radius} reveal=${resolved.revealStyle.name.lowercase()}")
        return true
    }

    /** Keeps optional scene-hook failures separate from the background's lifecycle. */
    private inner class WindowHooks(private val windowClass: Class<*>) {
        private val attrsField = windowClass.getDeclaredField("mAttrs").apply { isAccessible = true }

        fun install() {
            windowClass.getDeclaredMethod("prepareSurfaces").apply { isAccessible = true }
                .createAfterHook { param -> runCatching { prepareWindow(param.thisObject) }.onFailure { notePrepareFailure(it) } }
            windowClass.getDeclaredMethod("removeImmediately").apply { isAccessible = true }
                .createBeforeHook { param -> synchronized(layers) { removeLayer(param.thisObject) } }
            installNativeMotionTransaction()
            installWallpaper()
        }

        private fun installNativeMotionTransaction() {
            runCatching {
                val stub = loadClass("android.view.IWindowManager\$Stub")
                val transact = stub.getDeclaredMethod("onTransact", Integer.TYPE, Parcel::class.java,
                    Parcel::class.java, Integer.TYPE).apply { isAccessible = true }
                transact.createBeforeHook { param ->
                        // A hot reload cannot physically remove callbacks already registered in
                        // system_server. Never short-circuit here: restore the Parcel so every live
                        // endpoint can observe the same frame, including the newest hook.
                        if (stopped) return@createBeforeHook
                        val code = param.args[0] as Int
                        if (code != DockNativeMotionEndpoint.TRANSACTION_CODE) return@createBeforeHook
                        val data = param.args[1] as Parcel
                        val position = data.dataPosition()
                        nativeMotionReply.remove()
                        runCatching {
                            nativeMotionReply.set(
                                nativeMotionEndpoint.receive(code, data, param.args[3] as Int))
                        }.onFailure {
                            if (observed.add("native-motion-transaction-error")) {
                                glassClient.record("native motion transaction rejected=${it.javaClass.simpleName}")
                            }
                        }.also {
                            data.setDataPosition(position)
                        }
                    }
                transact.createAfterHook { param ->
                    if (stopped || param.args[0] as Int != DockNativeMotionEndpoint.TRANSACTION_CODE) {
                        return@createAfterHook
                    }
                    // The original Stub sees an unknown private code. Confirm it only after all
                    // before callbacks have had a chance to consume the restored input Parcel.
                    val acknowledgment = nativeMotionReply.get() ?: DockNativeMotionEndpoint.ACK
                    nativeMotionReply.remove()
                    val reply = param.args[2] as? Parcel ?: return@createAfterHook
                    reply.setDataPosition(0)
                    reply.writeInt(acknowledgment)
                    param.result = true
                }
                glassClient.record("native motion IWindowManager endpoint ready")
            }.onFailure {
                glassClient.record("native motion IWindowManager endpoint unavailable=${it.javaClass.simpleName}")
            }
        }

        private fun prepareWindow(window: Any) {
            if (stopped) return
            val attrs = attrsField.get(window) as WindowManager.LayoutParams
            if (attrs.packageName != "com.miui.home") return
            // Only launcher windows reach this point, so this is the cheapest place to pick up a
            // changed parameter. It is what makes an edit survive a desktop restart: the restart
            // rebuilds the WindowState and traverses into here, but it never re-creates the hook.
            // The style query is asynchronous and throttled; its result lands on a later traversal.
            glassClient.refreshRevealStyle()
            refreshSettings()
            val title = attrs.title.toString()
            synchronized(layers) {
                if (stopped) return
                if (settings.enabled && observed.size < 12 && observed.add("${attrs.type}:$title")) {
                    XposedLog.i(TAG, LOG_TAG, "Launcher window: type=${attrs.type}, title=${title.take(160)}")
                }
                if (!isLauncher(window, attrs)) return
                service = window.getObjectFieldAs<Any>("mWmService")
                if (settings.enabled) glassClient.bindDiagnostics(service!!.getObjectFieldAs<Context>("mContext"))
                registerDisplayListener()
                updateLayer(window)
                scheduleNativeBindSweep()
            }
        }

        private fun isLauncher(window: Any, attrs: WindowManager.LayoutParams): Boolean =
            DockWindowPolicy.isLauncherWindow(attrs.packageName, attrs.title.toString(), attrs.type,
                window.callMethod("getDisplayId") as Int)

        private fun installWallpaper() {
            // Read only the exact launcher's scoped command; never alter wallpaper or gesture handling.
            runCatching {
                val endpoint = DockWallpaperEndpoint.resolve(loadClass("com.android.server.wm.WallpaperController"),
                    windowClass, loadClass("com.android.server.wm.Session"), IBinder::class.java, Bundle::class.java)
                val command = endpoint.method().apply { isAccessible = true }
                command.createBeforeHook { param ->
                    runCatching { wallpaperCommand(endpoint, param, captureOnly = true) }
                        .onFailure { reportMotionError(it) }
                }
                command.createAfterHook { param ->
                    runCatching { wallpaperCommand(endpoint, param) }.onFailure { reportMotionError(it) }
                }
                XposedLog.i(TAG, LOG_TAG, "Dock recents motion observer ready")
                glassClient.record("motion observer ready endpoint=${command.toGenericString()}")
            }.onFailure {
                glassClient.record("motion observer unsupported=${it.javaClass.simpleName}: ${it.message?.take(160)}")
                XposedLog.w(TAG, LOG_TAG, "Dock recents motion unsupported; keeping background", it)
            }
        }

        private fun wallpaperCommand(endpoint: DockWallpaperEndpoint.Endpoint, param: HookParam,
            captureOnly: Boolean = false) {
            if (stopped || !settings.enabled) return
            val wm = service ?: return
            // Session callbacks may run after WMS releases its lock. Preserve WM -> layer lock order.
            synchronized(wm.getObjectFieldAs<Any>(WM_LOCK)) {
                synchronized(layers) {
                    if (stopped || !settings.enabled) return
                    val window = if (endpoint.sessionScoped()) {
                        layers.entries.firstOrNull { (_, layer) ->
                            DockWallpaperEndpoint.ownsWindow(layer.motionSession, layer.motionClient,
                                param.thisObject, param.args[0])
                        }?.key ?: return
                    } else param.args[0] ?: return
                    val attrs = attrsField.get(window) as WindowManager.LayoutParams
                    if (!isLauncher(window, attrs)) return
                    val extras = param.args[5] as? Bundle
                    if (!captureOnly) recordCommand(param.args[1], extras)
                    val action = param.args[1] as? String
                    if (action != DockRecentsMotion.WALLPAPER_ACTION) return
                    if (extras == null) return
                    @Suppress("DEPRECATION")
                    val scale = (extras.get("scale_to") as? Number)?.toDouble() ?: return
                    val sceneAction = extras.getString("action")
                    val command = param.args[1] as String
                    val isSetTo = sceneAction == "setTo"
                    val overview = DockRecentsMotion.overviewTarget(command, sceneAction, scale) ?: return
                    if (captureOnly) {
                        val glass = layers[window]?.glass ?: return
                        if (overview || DockRecentsMotion.homeTarget(command, sceneAction, scale)) {
                            glassClient.releaseDepartureHold(glass)
                            // A plain app switch never rotates the display, so thaw the frozen home
                            // sample here. During a rotation the display is still landscape at this
                            // command and the rotation return owns the thaw instead.
                            if (!layerUpdate.isDisplayRotated()) {
                                glassClient.resumeFrozenCapture(glass, "home-command")
                            }
                            requestTraversal()
                        } else if (DockRecentsMotion.leavingHomeTarget(command, sceneAction, scale)) {
                            // The app-launch zoom fires while the home wallpaper is still the blur
                            // source. Freeze here so the retained frame carries the home sample; the
                            // freeze keeps the panel drawn, so unlike the old undraw pause this costs
                            // nothing on the visible Dock and removes the app's colours from the
                            // returning frame.
                            glassClient.holdForDeparture(glass)
                        }
                        // No departure hold on the generic path: pausing there put the still-visible
                        // Dock into the vendor's undraw state for every app switch. The drawable
                        // freeze above is safe because it keeps compositing the last home sample.
                        return
                    }
                    if (DockRecentsMotion.homeTarget(command, sceneAction, scale)) {
                        scheduleVisibleGlassRefresh(window)
                    }
                    updateOverview(window, overview, isSetTo, scale)
                }
            }
        }

        private fun scheduleVisibleGlassRefresh(window: Any) {
            val wm = service ?: return
            wm.getObjectFieldAs<Handler>(WM_HANDLER).postDelayed({
                runCatching {
                    synchronized(wm.getObjectFieldAs<Any>(WM_LOCK)) {
                        synchronized(layers) {
                            val layer = layers[window] ?: return@postDelayed
                            if (stopped) return@postDelayed
                            val nativeLatest = nativeMotionEndpoint.latest(layer.nativeUid, layer.nativePid)
                                ?.takeUnless { it.scene() == DockNativeMotion.SCENE_AUTO_AIM }
                            if (layer.overview || nativeLatest != null) return@postDelayed
                            if (!layer.nativeApplied && window.callMethod("isVisible") == true) {
                                layer.glass?.let { glassClient.resume(it) }
                            }
                        }
                    }
                }.onFailure { reportMotionError(it) }
            }, 120)
        }

        private fun recordCommand(action: Any?, extras: Bundle?) {
            if (commandSamples++ >= 100) return
            @Suppress("DEPRECATION")
            val value = extras?.get("scale_to")
            glassClient.record("launcher wallpaper command=${(action as? String)?.take(80)} " +
                "action=${extras?.getString("action")?.take(40)} " +
                "scale=${(value as? Number)?.toDouble()} type=${value?.javaClass?.simpleName}")
        }

        private fun updateOverview(window: Any, overview: Boolean, immediate: Boolean, scale: Double) {
            // A controller command can precede the first prepareSurfaces.
            if (layers[window] == null) {
                updateLayer(window)
                requestTraversal() // Commit the new surface's parent/crop/visibility.
            }
            val layer = layers[window] ?: return
            layer.overview = overview
            val now = SystemClock.uptimeMillis()
            layer.motionTime = now
            val wasRunning = layer.motion.isRunning(now)
            val targetChanged = layer.motion.setOverview(overview, now)
            if (immediate) layer.motion.finish()
            val nativeLatest = nativeMotionEndpoint.latest(layer.nativeUid, layer.nativePid)
                ?.takeUnless { it.scene() == DockNativeMotion.SCENE_AUTO_AIM }
            val validationNow = System.nanoTime()
            if (overview && validationNow - layer.lastOverviewValidationNs > 80_000_000L) {
                layer.lastOverviewValidationNs = validationNow
                layer.overviewGeneration++
                val alreadyObserved = nativeMotionEndpoint.sawOverviewSince(
                    layer.nativeUid, layer.nativePid, validationNow - 100_000_000L)
                scheduleNativeHookValidation(window, layer, layer.overviewGeneration,
                    layer.nativeUid, layer.nativePid, validationNow, alreadyObserved)
            }
            if (nativeLatest != null) {
                if (targetChanged || (immediate && wasRunning)
                    || layer.nativeScene == 1
                    || (layer.overview && layer.nativeScene == 0)) {
                    scheduleAnimationFrame(true)
                }
                return
            }
            if (!targetChanged && !(immediate && wasRunning)) {
                glassClient.record("motion overview command without target change overview=$overview immediate=$immediate scale=$scale")
                if (overview) scheduleAnimationFrame() else requestTraversal()
            } else {
                layer.motionSamples = 0
                layer.motionEndPending = true
                glassClient.record("motion target overview=$overview immediate=$immediate scale=$scale liftDp=${DockRecentsMotion.LIFT_DP} curve=sceneSpring")
                if (directMotionAvailable) scheduleAnimationFrame() else requestTraversal()
            }
        }

        private fun scheduleNativeHookValidation(window: Any, expectedLayer: Layer,
            generation: Long, expectedUid: Int, expectedPid: Int,
            since: Long, alreadyObserved: Boolean) {
            val wm = service ?: return
            wm.getObjectFieldAs<Handler>(WM_HANDLER).postDelayed({
                runCatching {
                    synchronized(wm.getObjectFieldAs<Any>(WM_LOCK)) {
                        synchronized(layers) {
                            val layer = layers[window] ?: return@postDelayed
                            if (stopped || layer !== expectedLayer
                                || layer.overviewGeneration != generation
                                || layer.nativeUid != expectedUid || layer.nativePid != expectedPid) {
                                return@postDelayed
                            }
                            if (alreadyObserved || nativeMotionEndpoint.sawOverviewSince(
                                    expectedUid, expectedPid, since)) {
                                layer.nativeOverviewGeneration = generation
                                return@postDelayed
                            }
                            nativeMotionEndpoint.requestHookRevalidation(
                                expectedUid, expectedPid)
                            glassClient.record(
                                "native motion missing after overview target; requesting semantic revalidation")
                        }
                    }
                }.onFailure { reportMotionError(it) }
            }, 250)
        }

        private fun reportMotionError(error: Throwable) {
            synchronized(layers) {
                if (observed.add("recents-motion-error")) {
                    glassClient.record("motion observer error=${error.javaClass.simpleName}: ${error.message?.take(160)}")
                    XposedLog.w(TAG, LOG_TAG, "Dock recents signal unavailable; keeping background", error)
                }
            }
        }
    }

    private inner class LayerUpdate {
        /**
         * Suspends the glass while the display is rotated. The launcher window frame cannot see
         * that state - it stays portrait while the display rotates around it - so this reads the
         * display itself; see [DockRotationPolicy].
         */
        private val rotation = DockRotationPolicy()
        /** Display thread only. Resolved once; the default display never changes here. */
        private var displayRef: Display? = null
        /** Reported once, so a silent rotation reader cannot hide behind a quiet log. */
        private var rotationReadReported = false
        private var rotationReadFailed = false

        fun isRotationSettling(): Boolean = rotation.isRotated() || rotation.isSettling(SystemClock.uptimeMillis())
        fun isDisplayRotated(): Boolean = rotation.isRotated()

        fun update(window: Any) {
            // Already re-read by prepareWindow (every launcher traversal) and by the 1 Hz sweep,
            // so this is the live configuration rather than the value captured at hook init.
            // Nothing below caches it across traversals: changing a parameter and re-laying the
            // window always recomputes the bounds from the current values.
            val config = settings
            if (!config.enabled) { removeLayer(window); return }
            val parent = window.callMethod("getSurfaceControl") ?: return
            if (parent.callMethod(IS_VALID) != true) { removeLayer(window); return }
            val frame = window.callMethod("getFrame") as Rect
            val configuration = window.callMethod("getConfiguration") as Configuration
            val bounds = DockWindowPolicy.layout(frame.width(), frame.height(), configuration.densityDpi / 160f,
                config.height, config.margin, config.bottom, config.radius)
            if (bounds == null) { removeLayer(window); return }
            val layer = obtainLayer(window, parent, frame, bounds, config)
            // The reveal scales about the layer's own centre, so the bounds have to be current.
            layer.width = bounds.width()
            layer.height = bounds.height()
            layer.radius = bounds.radius()
            val dark = when (config.nightMode) {
                1 -> false
                2 -> true
                else -> configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            }
            val windowVisible = window.callMethod("isVisible") == true
            // The launcher hides its own dock while the minus-one screen is showing, and our
            // panel would then be the only thing left floating over it. Everything downstream
            // already keys off `visible` - the appearance transaction hides the effect layer,
            // the glass refresh is paused and the frame loop stops - so folding the overlay into
            // this one flag is what makes the hide (and the restore) automatic.
            val overlayShowing = launcherOverlayVisible(service!!, SystemClock.uptimeMillis())
            if (overlayShowing != layer.overlayHidden) {
                layer.overlayHidden = overlayShowing
                glassClient.record(
                    if (overlayShowing) "dock glass hidden for launcher overlay (minus-one)"
                    else "dock glass restored after launcher overlay")
            }
            // A retained portrait texture can accompany the launcher through its rotated return.
            // Keep sampling paused until the portrait pose commits; an unpreserved source remains gated.
            // The launcher frame stays portrait, so only display rotation is authoritative here.
            refreshRotationGate()
            // The rotation no longer gates visibility or rebuilds the host. Both were stopgaps
            // from before the host could report its own sampling geometry: hiding the panel here
            // left the launcher's dock icons without any background for the whole rotated return,
            // and the epoch rebuild added a warm-up fallback after it. The host now refuses
            // samples whose blur geometry still carries the rotated transform (geometryValid), so
            // the compositor fallback covers exactly those frames and the glass returns the
            // moment the geometry catches up - no hide, no rebuild, no timer.
            refreshRotationGate()
            val visible = windowVisible && !overlayShowing
            val wasVisible = layer.lastVisible == true
            bindNativeMotion(window, layer)
            if (!visible) {
                layer.motion.finish()
                layer.nativeMotion.reset()
                layer.nativeApplied = false
                layer.nativeScene = -1
                layer.nativeSampleDeadlineNs = 0
                layer.resetAutoAim()
            } else if (!animationAvailable && !layer.nativeApplied) {
                layer.motion.finish()
            }
            val glass = updateGlass(layer, config, bounds, dark, visible)
            val resumeGlass = visible && glass != null && (layer.lastVisible == false || glass.retainedCapture)
            if (visible && layer.lastVisible == false && glass != null) glassClient.releaseDepartureHold(glass)
            if (!visible && layer.lastVisible == true && glass != null) {
                // A display rotation hides the launcher behind the foreground app. Freeze the
                // capture while the Dock is invisible so the returning frame keeps the last home
                // sample; otherwise the pass-blur texture is updated with the app's content and the
                // first returning frame paints the wrong colours. Pausing is safe here precisely
                // because the panel is not on screen (a paused capture is not drawn at all).
                if (layerUpdate.isDisplayRotated()) glassClient.holdForDeparture(glass)
                else glassClient.pauseRefresh(glass)
            }
            layer.lastVisible = visible
            val appearance = Appearance(config, bounds, dark, visible, glass)
            val now = SystemClock.uptimeMillis()

            val x = bounds.x().toFloat()
            val density = configuration.densityDpi / 160f
            val geometryChanged = layer.x != x || layer.baseY != bounds.y() || layer.density != density
            layer.baseY = bounds.y()
            layer.density = density
            layer.motionTime = now
            val offset = motionOffset(layer, now)
            val running = !layer.nativeApplied && layer.motion.isRunning(now)
            // In direct mode, do not queue old animation positions in a later WMS traversal.
            // WMS still initializes/repositions our layer when the actual layout changes.
            val movingDirectly = directMotionAvailable && visible && (running ||
                layer.reveal.isRunning() || layer.reveal.hasPendingPose() ||
                layer.revealAlpha != 1f || !hasRestingContainer(layer))
            val keepDirectPosition = movingDirectly && wasVisible && !geometryChanged
            // A WMS sync transaction can be applied after a newer direct vsync transaction.
            // Even the same curve sampled at different times then moves the surface backwards.
            // Once visible, only the frame clock owns animation pose; traversal still owns
            // first-show initialization, real geometry changes and the non-direct fallback.
            val y = if (keepDirectPosition) layer.y
                else bounds.y() + offset + layer.reveal.risePx(layer.density, now)
            if (visible && (movingDirectly || running || layer.reveal.isRunning())) scheduleAnimationFrame()
            val moved = layer.x != x || layer.y != y
            if (!moved && layer.appearance == appearance.key && !resumeGlass) return
            val transaction = window.callMethod("getSyncTransaction")!!
            if (moved) {
                // Move the common parent: glass, tint and fallback blur stay aligned. The size/key
                // remains unchanged, so a frame of motion never recreates the glass host or texture.
                // The reveal's pivot compensation rides along, otherwise a traversal landing
                // mid-reveal would drop it and visibly shift the dock.
                val shiftX = pivotOffset(layer.revealScaleX)
                val shiftY = pivotOffset(layer.revealScaleY)
                // The slide is added at write time and never cached, so layer.x stays the
                // resting position and the frame loop below cannot apply it twice.
                transaction.callMethod(SET_POSITION, layer.effect,
                    x + layer.width * shiftX + layer.reveal.slidePx(layer.width.toFloat(), now),
                    y + layer.height * shiftY)
                layer.x = x
                layer.y = y
                // TODO(twitch-diag): trace traversal writes right after an unlock.
                if (revealDebugActive()) {
                    glassClient.record("reveal dbg layout yOff=${y - layer.baseY} " +
                        "rise=${layer.reveal.risePx(layer.density, now)} " +
                        "revealScale=${layer.revealScaleX}x${layer.revealScaleY}")
                }
                recordMotion(layer, y, now, visible, "layout")
            }
            if (layer.appearance != appearance.key) {
                applyAppearance(transaction, layer, appearance, keepDirectPosition)
                layer.appearance = appearance.key
            }
            if (resumeGlass) {
                glassClient.resume(glass!!, allowFallback = !materialSettleActive(),
                    afterCommit = transaction as SurfaceControl.Transaction)
            }
        }

        /**
         * Read the display rotation and fold it into the rotation gate.
         *
         * @return true when the suspended state changed, so the caller must ask for a traversal.
         * The gate cannot rely on launcher traversals alone: while an app owns a rotated display
         * the launcher window is hidden behind it, WMS destroys its surface and never traverses
         * it, so the one-second sweep is what keeps this state current at all.
         */
        fun refreshRotationGate(): Boolean {
            val displayRotation = readDisplayRotation()
            val rotated = if (displayRotation != null) displayRotation != Surface.ROTATION_0
            else rotation.isRotated()
            val now = SystemClock.uptimeMillis()
            if (!rotation.update(rotated, now)) return false
            glassClient.record(
                if (rotated) "dock glass suspended while the display is rotated"
                else "dock glass returning after rotation epoch=${rotation.getEpoch()} " +
                    "settleMs=${rotation.settleRemainingMs(now)}")
            if (!rotated) {
                // Immediately resync glass on rotation return, don't wait for the next traversal.
                // The traversal-based resume (via resumeGlass) only fires when lastVisible flips,
                // which doesn't happen when the dock stays visible through the rotation. The host
                // recomputes its blur geometry inside this resume, so the first returning frame is
                // mapped through the portrait transform instead of the rotated one.
                synchronized(layers) {
                    for ((_, layer) in layers) {
                        val glass = layer.glass ?: continue
                        glassClient.resumeAfterRotation(glass)
                    }
                }
                // Start capture now, then revisit presentation once portrait has settled.
                service?.getObjectFieldAs<Handler>(WM_HANDLER)?.postDelayed(
                    { requestTraversal() },
                    rotation.settleRemainingMs(SystemClock.uptimeMillis()) + ROTATION_RESUME_MARGIN_MS)
            } else {
                // Freeze the capture as soon as the display rotates, while the launcher is still the
                // visible window. Waiting for the Dock to hide means the compositor has already
                // captured the foreground app, and that stale frame is exactly what the returning
                // panel would paint. The freeze keeps the panel drawn, so this has no visual cost.
                synchronized(layers) {
                    for ((_, layer) in layers) {
                        val glass = layer.glass ?: continue
                        glassClient.holdForDeparture(glass)
                    }
                }
            }
            return true
        }

        /**
         * The default display's current rotation, in [Surface.ROTATION_*] terms, or null when it
         * cannot be resolved.
         *
         * <p>Neither substitute works on this ROM: the launcher window keeps its portrait frame
         * while the display rotates around it, and `Configuration.orientation` flips back and
         * forth while the display is still portrait. A null result makes the caller keep its last
         * decision rather than resume sampling on a rotated screen.
         */
        private fun readDisplayRotation(): Int? {
            if (rotationReadFailed) return null
            val value = catchingRecoverableOr<Int?>(null) {
                val display = displayRef ?: run {
                    val context = service?.getObjectFieldAs<Context>("mContext")
                        ?: error("WMS context unavailable")
                    val manager = context.getSystemService(DisplayManager::class.java)
                        ?: error("DisplayManager unavailable")
                    val resolved = manager.getDisplay(Display.DEFAULT_DISPLAY)
                        ?: error("default display unavailable")
                    displayRef = resolved
                    resolved
                }
                display.rotation
            }
            if (value == null) {
                rotationReadFailed = true
                glassClient.record("display rotation unavailable; keeping the last gate state")
                return null
            }
            if (!rotationReadReported) {
                rotationReadReported = true
                glassClient.record("display rotation read=$value")
            }
            return value
        }

        private fun updateGlass(layer: Layer, config: Settings, bounds: DockWindowPolicy.Bounds,
            dark: Boolean, visible: Boolean): DockGlassClient.Ticket? {
            // Inside the wallpaper-swap settle window keep the existing ticket: releasing and
            // rebuilding the glass while the keyguard and home wallpapers are swapping is the
            // "samples two different wallpapers" flash. One rebuild happens after the window.
            val settledDark = if (materialSettleActive() && layer.glass != null) layer.glass!!.dark else dark
            val glassKey = "${bounds.width()}/${bounds.height()}/${bounds.radius()}/$settledDark"
            // A rotation no longer forces a rebuild: the launcher frame stays portrait through the
            // whole transition, so the host, its bounds and its material token are unchanged, and
            // the vendor's stale blur geometry is covered by the host's geometryValid gate (the
            // compositor fallback carries exactly the mis-mapped frames). Rebuilding here instead
            // traded one wrong-looking state for two: a hidden panel during the rotated return and
            // a warm-up fallback after it.
            // A dead host has to be replaced by us: nothing else clears the reference, so without
            // this the dock stays on the compositor fallback for the rest of the window's life
            // after the renderer process dies (it is the module's own process, so a swipe-away or
            // an out-of-memory kill is enough). The backoff keeps a crash-looping renderer from
            // being restarted on every traversal.
            val dead = layer.glass?.dead == true
            if (dead && layer.glassDroppedAt == 0L) layer.glassDroppedAt = SystemClock.uptimeMillis()
            if (dead && SystemClock.uptimeMillis() - layer.glassDroppedAt < GLASS_REBUILD_BACKOFF_MS) {
                return layer.glass
            }
            if (!config.glass || dead || layer.glass?.key?.let { it != glassKey } == true) {
                layer.glass?.let { glassClient.release(it) }
                layer.glass = null
                layer.glassDroppedAt = 0L
            }
            if (layer.glass != null && layer.glassRotationEpoch != rotation.getEpoch()) {
                // The host survives the rotation; its sampling geometry is validated per probe.
                layer.glassRotationEpoch = rotation.getEpoch()
            }
            // Cold starts and invalid sources still use the guarded warmup path. A paused,
            // ready host survives rotation and supplies glass from the first returning frame.
            if (config.glass && visible && layer.glass == null) {
                val context = service!!.getObjectFieldAs<Context>("mContext")
                layer.glass = glassClient.create(context, glassKey, bounds, settledDark)
                layer.glassRotationEpoch = rotation.getEpoch()
            }
            return layer.glass
        }

        private fun obtainLayer(window: Any, parent: Any, frame: Rect, bounds: DockWindowPolicy.Bounds, config: Settings): Layer {
            var layer = layers[window]
            if (layer != null && (layer.parent !== parent || layer.effect.callMethod(IS_VALID) != true)) {
                removeLayer(window)
                layer = null
            }
            if (layer == null) {
                val effect = Surfaces.buildLayer("HyperCeiler Dock blur", parent, false)
                var tint: Any? = null
                try { tint = Surfaces.buildLayer("HyperCeiler Dock tint", effect, true) }
                finally { if (tint == null) Surfaces.destroySurface(effect) }
                layer = Layer(parent, effect, tint)
                layer.reveal.setStyle(settings.revealStyle)
                val now = SystemClock.uptimeMillis()
                if (DockUnlockReveal.acceptsPending(pendingRevealAt, now)) {
                    layer.reveal.arm(pendingRevealAt)
                    layer.reveal.startIfArmed(pendingRevealAt)
                }
                layers[window] = layer
                val motionLayer = layer
                // Resolve the two identities independently. They used to share one
                // runCatching, so a failure while reading mClient.asBinder() also threw
                // away a perfectly good mSession, leaving this window with nativeUid/Pid
                // = -1 forever: no native sample, no real-time follow, and only a desktop
                // restart (which builds a new WindowState and layer) could recover.
                runCatching { motionLayer.motionSession = window.getObjectFieldAs<Any>("mSession") }
                    .onFailure {
                        motionLayer.motionSession = null
                        glassClient.record("motion window identity unavailable=${it.javaClass.simpleName}")
                    }
                if (motionLayer.motionSession != null) {
                    runCatching {
                        motionLayer.motionClient = window.getObjectFieldAs<Any>("mClient")
                            .callMethod("asBinder") as IBinder
                        glassClient.record("motion window identity bound")
                    }.onFailure {
                        // The client binder is only an optional Session fallback; the native
                        // binding needs mSession's uid/pid alone. Never discard the session.
                        motionLayer.motionClient = null
                        glassClient.record(
                            "motion window identity bound session-only=${it.javaClass.simpleName}")
                    }
                }
                XposedLog.i(TAG, LOG_TAG, "Dock surface created: frame=$frame, bounds=$bounds, blur=${config.blur}")
                if (!config.blur && Color.alpha(config.color) == 0) {
                    XposedLog.w(TAG, LOG_TAG, "Dock color is transparent; select a visible color or enable blur")
                }
            }
            return layer
        }

        private fun applyAppearance(transaction: Any, layer: Layer, appearance: Appearance,
            keepDirectPosition: Boolean) {
            val (config, bounds, dark, visible, glass) = appearance
            val glassSurface = appearance.surface
            val glassReady = appearance.ready
            transaction.callMethod(SET_LAYER, layer.effect, -1)
            transaction.callMethod(SET_CROP, layer.effect, bounds.width(), bounds.height())
            transaction.callMethod(SET_RADIUS, layer.effect, bounds.radius())
            transaction.callMethod(SET_LAYER, layer.tint, 1)
            transaction.callMethod(SET_CROP, layer.tint, bounds.width(), bounds.height())
            transaction.callMethod(SET_RADIUS, layer.tint, bounds.radius())
            if (glassSurface != null && glass?.dead == false) {
                // Remote root attachment and retirement share one serial worker.
                // WMS still controls the owned parent's visibility and motion.
                glassClient.attach(glass, layer.effect, glassReady)
                // Readiness controls opacity only. The capture root stays composited while
                // warming up, and layer.effect still owns launcher/rotation visibility.
                glassClient.setReady(glass, glassReady)
            }
            if (blurAvailable) {
                runCatching { transaction.callMethod("setBackgroundBlurRadius", layer.effect,
                    if (config.blur && !(glassReady && glass?.dead == false)) 120 else 0) }
                    .onFailure {
                        blurAvailable = false
                        XposedLog.w(TAG, LOG_TAG, "Compositor blur unavailable; retaining color fallback", it)
                    }
            }
            val color = when {
                config.glass -> DockGlassPreset.fallbackColor(dark)
                config.blur -> if (dark) 0x66505050 else 0x66FFFFFF
                else -> config.color
            }
            transaction.callMethod("setColor", layer.tint,
                floatArrayOf(Color.red(color) / 255f, Color.green(color) / 255f, Color.blue(color) / 255f))
            transaction.callMethod("setAlpha", layer.tint,
                if (glassReady && glass?.dead == false) 0f else Color.alpha(color) / 255f)
            transaction.callMethod("show", layer.tint)
            transaction.callMethod(if (visible) "show" else "hide", layer.effect)
            // The layer can become visible before the reveal's first animation frame runs. Pose it
            // inside this very transaction, otherwise the dock is drawn once at its resting size
            // and only then flies in - the "shows first, animates after" artefact.
            // Material changes must not enqueue an old animation pose behind a newer frame.
            if (!keepDirectPosition) {
                if (layer.reveal.isRunning()) {
                    // The base crop/radius above were just rewritten, so re-submit the current
                    // morph even when its normalized values match the cached previous frame.
                    applyRevealPose(transaction, layer, SystemClock.uptimeMillis(), true)
                } else if (layer.reveal.hasPendingPose() ||
                    layer.revealAlpha != 1f || !hasRestingContainer(layer)) {
                    restoreRestingTransform(transaction, layer)
                }
            }
            val appliedGlass = glassReady && glass?.dead == false
            if (config.glass && layer.lastGlassReady != appliedGlass) {
                glassClient.record("glass applied native=$appliedGlass fallbackBlur=${if (appliedGlass) 0 else 120} visible=$visible")
                layer.lastGlassReady = appliedGlass
            }
            // Use WMS's transaction, so visibility/position changes commit with the parent window.
        }
    }

    private val layerUpdate = LayerUpdate()
    private fun updateLayer(window: Any) {
        // A traversal means the launcher is being laid out, so the sweep's idle backoff is over.
        resetSweepCadence()
        layerUpdate.update(window)
    }

    /**
     * Carry the rotation gate on the commit point of every rotation this ROM performs.
     *
     * <p>Nothing else observes the display while an app owns a rotated screen: WMS neither
     * traverses the hidden launcher window (so the traversal poll is out) nor keeps our sweep
     * alive (it dies with the last layer), and the display listener verified silent for
     * app-requested rotations on this device. `DisplayRotation.setRotation` is the method the
     * framework calls with the rotation it is about to use, so it fires exactly when the gate
     * has to close - and the gate only acts on a real state change, so repeats cost nothing.
     *
     * <p>A rotation that is proposed but never committed can move the gate here, so the
     * traversal poll re-reads the display itself and corrects the state on the next frame.
     */
    private fun installRotationHook() {
        runCatching {
            loadClass("com.android.server.wm.DisplayRotation")
                .getDeclaredMethod("setRotation", Integer.TYPE).apply { isAccessible = true }
                .createAfterHook { param ->
                    runCatching { if (layerUpdate.refreshRotationGate()) requestTraversal() }
                        .onFailure {
                            if (rotationHookFailureReported.compareAndSet(false, true)) {
                                glassClient.record("rotation hook body failed=" +
                                    "${it.javaClass.simpleName}: ${it.message?.take(160)}")
                            }
                        }
                }
            glassClient.record("rotation hook ready DisplayRotation#setRotation")
        }.onFailure {
            glassClient.record("rotation hook unavailable=${it.javaClass.simpleName}: " +
                "${it.message?.take(160)}")
        }
    }

    /**
     * Observe display changes as a supplementary signal. This ROM does not deliver these
     * callbacks for every app-requested rotation, so the rotation hook remains necessary even
     * when listener registration succeeds.
     *
     * <p>Registered once, from the first launcher window. The callback runs on WMS's own handler
     * and only re-reads the rotation, so a burst of display changes costs one integer comparison.
     */
    private fun registerDisplayListener() {
        if (!displayListenerRegistered.compareAndSet(false, true)) return
        val context = runCatching { service?.getObjectFieldAs<Context>("mContext") }.getOrNull() ?: return
        runCatching {
            val manager = context.getSystemService(DisplayManager::class.java)
                ?: error("DisplayManager unavailable")
            val handler = service?.getObjectFieldAs<Handler>(WM_HANDLER)
            manager.registerDisplayListener(object : DisplayManager.DisplayListener {
                override fun onDisplayAdded(displayId: Int) = onDisplayChanged(displayId)
                override fun onDisplayRemoved(displayId: Int) = Unit
                override fun onDisplayChanged(displayId: Int) {
                    if (displayId == Display.DEFAULT_DISPLAY && layerUpdate.refreshRotationGate()) {
                        requestTraversal()
                    }
                }
            }, handler)
            glassClient.record("display listener registered")
        }.onFailure {
            glassClient.record("display listener unavailable=${it.javaClass.simpleName}")
        }
    }

    private fun removeLayer(window: Any) {
        val layer = layers.remove(window) ?: return
        resetSweepCadence()
        layer.glass?.let { glassClient.release(it) }
        // Only release surfaces created by this hook. Never release the host's parent handle.
        runCatching { Surfaces.destroySurface(layer.tint) }
        runCatching { Surfaces.destroySurface(layer.effect) }
    }

    /**
     * Start the dock reveal when the platform begins the unlock transition.
     *
     * `keyguardGoingAway` precedes Flutter _showPresent by roughly 20 ms in the reference trace.
     * This aligns the local reveal's epoch, not each icon's independently staggered 3D transform.
     * The existing native recents channel does not sample that Flutter unlock transform.
     *
     * Every failure is reported instead of thrown, because the dock must keep working without the
     * reveal. A failed install only costs the animation.
     */
    private fun installUnlockReveal() {
        // services.jar confirms keyguardGoingAway(int flags) on both classes. This is the
        // transition START. setKeyguardShown(false) arrives ~315 ms after Flutter _showPresent
        // on the reference device; it must not be used as an animation-start fallback.
        val targets = listOf(
            "com.android.server.wm.KeyguardController" to "keyguardGoingAway",
            "com.android.server.wm.ActivityTaskManagerService" to "keyguardGoingAway"
        )
        for ((className, methodName) in targets) {
            val type = runCatching { loadClass(className) }.getOrNull()
            if (type == null) {
                glassClient.record("unlock reveal class missing $className")
                continue
            }
            val named = hierarchyDeclaredMethods(type).filter { it.name == methodName }
            val method = named.firstOrNull { candidate ->
                candidate.parameterTypes.contentEquals(arrayOf(Int::class.javaPrimitiveType))
            }
            if (method == null) {
                glassClient.record("unlock reveal no int-flags signature $className#$methodName found=" +
                    named.joinToString("|") { it.toGenericString().substringAfter("$className.") }.take(180))
                continue
            }
            val hooked = runCatching {
                method.isAccessible = true
                method.createBeforeHook {
                    if (stopped) return@createBeforeHook
                    // No screen-state guard here, deliberately: `keyguardGoingAway` is only called
                    // by KeyguardViewMediator once the unlock is authenticated (traced 14:12:
                    // exactly one call per unlock, flags 8/18, never during AOD/doze transitions -
                    // those go through setKeyguardShown instead). An isInteractive check would
                    // REJECT fingerprint unlocks that go straight from doze to keyguard-gone,
                    // killing the animation on the most common unlock path.
                    onKeyguardGone()
                }
            }.isSuccess
            if (hooked) {
                glassClient.record(
                    "unlock reveal trigger ready ${method.toGenericString()} " +
                        "phase=going-away pose=single-clock-v2 style=depth-flip-v1")
                return
            }
            glassClient.record("unlock reveal hook failed $className#$methodName")
        }
        glassClient.record("unlock reveal trigger unavailable")
    }

    /** Include inherited declarations: some ROMs keep these methods one level up. */
    private fun hierarchyDeclaredMethods(type: Class<*>): List<java.lang.reflect.Method> {
        val out = ArrayList<java.lang.reflect.Method>()
        var current: Class<*>? = type
        while (current != null && current != Any::class.java) {
            out.addAll(current.declaredMethods)
            current = current.superclass
        }
        return out
    }

    private fun onKeyguardGone() {
        if (stopped || !settings.enabled) return
        val wm = service ?: return
        // This is a BEFORE hook: posting the arm to mH lets keyguard removal expose the resting
        // Dock before the runnable executes. Prepare and commit the hidden pose before returning.
        // Use the same lock order as traversal; WMS's global monitor is reentrant when already held.
        runCatching {
            var armed = 0
            synchronized(wm.getObjectFieldAs<Any>(WM_LOCK)) {
                synchronized(layers) {
                    val now = SystemClock.uptimeMillis()
                    if (!DockUnlockReveal.acceptsNewEvent(pendingRevealAt, now)) return
                    pendingRevealAt = now
                    layers.values.forEach {
                        // A previous launcher's final scene-3 packet may still be retained by the
                        // endpoint. The epoch check below rejects it; clearing local bookkeeping
                        // also guarantees this unlock cannot inherit a scaled container cache.
                        it.resetAutoAim()
                        if (it.reveal.arm(now)) {
                            // Keep the clock aligned with transition start even if WMS still
                            // reports the launcher hidden for the first few animation frames.
                            it.reveal.startIfArmed(now)
                            armed++
                        }
                    }
                    // Keep arm and prime atomic with respect to traversal and motion callbacks.
                    if (armed > 0) primeReveal()
                }
            }
            // Even with no existing layer, retain the event for obtainLayer and run the safety net.
            glassClient.record("unlock reveal transition-start layers=$armed uptimeMs=$pendingRevealAt " +
                "durationMs=${DockUnlockReveal.DURATION_MS} riseDp=${DockUnlockReveal.RISE_DP} " +
                "reveal=${settings.revealStyle.name.lowercase()}")
            // Hand the epoch to the glass view: the tilt is a real perspective projection but
            // it has to happen inside our own process, so it needs the absolute start time.
            synchronized(layers) {
                layers.values.forEach { layer ->
                    layer.glass?.let {
                        // A paused capture is not drawn at all, and the keyguard keeps it paused
                        // through the unlock. Bring it back before the first animation frame, or
                        // the fly-in animates an empty slot.
                        glassClient.resumeForReveal(it)
                        glassClient.notifyUnlock(it, pendingRevealAt, settings.revealStyle)
                    }
                }
            }
            // Pose the layers now, so the frame where the dock first becomes visible is already
            // the animation's first frame instead of one flash at the resting size.
            if (directMotionAvailable) scheduleAnimationFrame(true) else {
                requestTraversal()
                scheduleDirectMotionRecovery()
            }
            // A reveal that never receives another frame must not leave the dock scaled,
            // translucent - or, after priming, invisible. Restore the resting transform
            // unconditionally once the whole window has elapsed.
            wm.getObjectFieldAs<Handler>(WM_HANDLER)
                .postDelayed({ resetStuckReveal() }, DockUnlockReveal.SETTLE_MS)
            // Close the wallpaper-swap settle window: this traversal applies the home
            // wallpaper's darkness in exactly one material rebuild, and re-probes the
            // producer with fallbacks enabled in case it went unhealthy mid-transition.
            wm.getObjectFieldAs<Handler>(WM_HANDLER)
                .postDelayed({ requestTraversal() }, MATERIAL_SETTLE_MS + 100L)
        }.onFailure {
            glassClient.record("unlock reveal prepare failed=${it.javaClass.simpleName}")
        }
    }

    /**
     * Apply the visual container morph without changing the Dock's measured or laid-out bounds.
     *
     * Perspective fold and capsule fission use a centred SurfaceControl crop, so the underlying
     * glass texture keeps its full-size sampling geometry while only the visible shape opens.
     * Corner radius is interpolated against that crop. The small terminal overshoot and the
     * elastic-burst squash use the existing four-float matrix, with pivot compensation returned
     * to the caller. A ROM without the Rect crop overload falls back to folding the crop fraction
     * into the matrix; layout is never requested from an animation frame.
     */
    private data class AppliedContainer(val scaleX: Float, val scaleY: Float)

    private fun applyRevealContainer(transaction: Any, layer: Layer,
        pose: DockUnlockReveal.ContainerPose, force: Boolean = false): AppliedContainer {
        var cropApplied = revealCropAvailable
        if (cropApplied && (force || pose.cropWidth != layer.revealCropWidth ||
                pose.cropHeight != layer.revealCropHeight)) {
            runCatching {
                val cropWidth = (layer.width * pose.cropWidth).toInt().coerceIn(1, layer.width)
                val cropHeight = (layer.height * pose.cropHeight).toInt().coerceIn(1, layer.height)
                val left = (layer.width - cropWidth) / 2
                val top = (layer.height - cropHeight) / 2
                (transaction as SurfaceControl.Transaction).setCrop(
                    layer.effect as SurfaceControl,
                    Rect(left, top, left + cropWidth, top + cropHeight))
            }.onFailure {
                revealCropAvailable = false
                cropApplied = false
                // Rect cropping may have succeeded on an earlier frame before a transient API
                // failure. Clear it through the long-established width/height overload before
                // switching to the matrix fallback, so no centred sliver can be stranded.
                transaction.callMethod(SET_CROP, layer.effect, layer.width, layer.height)
                glassClient.record("unlock reveal centred crop unavailable ${it.javaClass.simpleName}" +
                    " msg=${it.message?.take(120)}; using matrix fallback")
            }
        }
        val scaleX = pose.scaleX * if (cropApplied) 1f else pose.cropWidth
        val scaleY = pose.scaleY * if (cropApplied) 1f else pose.cropHeight
        if (revealScalingAvailable && (force || scaleX != layer.revealScaleX ||
                scaleY != layer.revealScaleY)) {
            runCatching {
                transaction.callMethod("setMatrix", layer.effect, scaleX, 0f, 0f, scaleY)
            }.onFailure {
                revealScalingAvailable = false
                glassClient.record("unlock reveal scaling unavailable ${it.javaClass.simpleName}" +
                    " msg=${it.message?.take(120)}; keeping crop and fade only")
            }
        }
        if (force || pose.cornerProgress != layer.revealCornerProgress ||
            pose.cropHeight != layer.revealCropHeight) {
            transaction.callMethod(SET_RADIUS, layer.effect,
                pose.cornerRadius(layer.radius, layer.height.toFloat()))
        }
        return AppliedContainer(
            if (revealScalingAvailable) scaleX else 1f,
            if (revealScalingAvailable) scaleY else 1f)
    }

    private fun rememberContainer(layer: Layer, pose: DockUnlockReveal.ContainerPose,
        applied: AppliedContainer) {
        layer.revealScaleX = applied.scaleX
        layer.revealScaleY = applied.scaleY
        layer.revealCropWidth = pose.cropWidth
        layer.revealCropHeight = pose.cropHeight
        layer.revealCornerProgress = pose.cornerProgress
    }

    private fun hasRestingContainer(layer: Layer): Boolean =
        layer.revealScaleX == 1f && layer.revealScaleY == 1f &&
            layer.revealCropWidth == 1f && layer.revealCropHeight == 1f &&
            layer.revealCornerProgress == 1f

    private fun containerChanged(layer: Layer, pose: DockUnlockReveal.ContainerPose): Boolean =
        pose.scaleX * (if (revealCropAvailable) 1f else pose.cropWidth) != layer.revealScaleX ||
            pose.scaleY * (if (revealCropAvailable) 1f else pose.cropHeight) != layer.revealScaleY ||
            pose.cropWidth != layer.revealCropWidth || pose.cropHeight != layer.revealCropHeight ||
            pose.cornerProgress != layer.revealCornerProgress

    /** Half of `1 - scale`, which is what a centre-anchored scale shifts the layer origin by. */
    private fun pivotOffset(scale: Float): Float = (1f - scale) / 2f

    /** Undo every trace of a reveal so a cancelled or superseded one cannot leave a residue. */
    private fun restoreRestingTransform(transaction: Any, layer: Layer) {
        // setWindowCrop is the established full-bounds path and also clears any prior Rect crop.
        transaction.callMethod(SET_CROP, layer.effect, layer.width, layer.height)
        val identity = DockUnlockReveal.ContainerPose.identity()
        val applied = applyRevealContainer(transaction, layer, identity, true)
        rememberContainer(layer, identity, applied)
        transaction.callMethod("setAlpha", layer.effect, 1f)
        layer.revealAlpha = 1f
        layer.revealSlide = 0f
        layer.resetAutoAim()
        if (layer.baseY > 0 && layer.x.isFinite()) {
            val restY = layer.baseY + motionOffset(layer, SystemClock.uptimeMillis())
            transaction.callMethod(SET_POSITION, layer.effect, layer.x, restY)
            layer.y = restY
            // TODO(twitch-diag): every restore is suspicious while investigating the twitch.
            glassClient.record("reveal dbg restore yOff=${restY - layer.baseY}")
        }
    }

    /**
     * Resolve the container pose from its sole owner.
     *
     * AUTO_AIM consumes the latest authenticated Hotseat projection setter sample. The mapping is
     * one-to-one: an icon scale of 0.43 produces a 0.43 x 0.43 Dock surface matrix. A missing,
     * pre-unlock or stale sample resolves to identity rather than a guessed keyframe.
     */
    private fun revealContainerPose(layer: Layer, now: Long,
        nowNs: Long = System.nanoTime()): DockUnlockReveal.ContainerPose {
        if (layer.reveal.getStyle() != DockUnlockReveal.Style.AUTO_AIM ||
            !layer.reveal.isRunning()) {
            if (layer.autoAimLive || layer.autoAimDeadlineNs != 0L) layer.resetAutoAim()
            return layer.reveal.containerPose(now)
        }
        val sample = nativeMotionEndpoint.latest(layer.nativeUid, layer.nativePid)
        val projected = DockNativeMotion.autoAimScale(
            sample, layer.reveal.eventEpochMillis(), nowNs)
        if (projected == null) {
            if (layer.autoAimLive) {
                layer.autoAimLive = false
                layer.autoAimDeadlineNs = 0L
                glassClient.record("auto aim native projection lost; restoring identity")
            }
            return DockUnlockReveal.ContainerPose.identity()
        }
        layer.autoAimDeadlineNs = sample!!.uptimeNanos() + DockNativeMotion.AUTO_AIM_MAX_AGE_NS
        if (sample.sequence() != layer.autoAimSequence) {
            layer.autoAimSequence = sample.sequence()
            if (layer.autoAimSamples++ < 8 || kotlin.math.abs(projected - 1.0) < 0.000001) {
                glassClient.record("auto aim live scale=$projected sequence=${sample.sequence()}")
            }
        }
        layer.autoAimLive = true
        return DockUnlockReveal.ContainerPose.fromProjectedScale(projected)
    }

    /**
     * Write the reveal pose for [now] and remember it, so the frame loop does not repeat a write
     * that is already on the surface.
     *
     * Shared by the prime at arm time and by the first visible appearance. The latter is what
     * guarantees the dock is never drawn at its resting size: the pose travels in the same
     * transaction that shows the layer.
     */
    private fun applyRevealPose(transaction: Any, layer: Layer, now: Long, force: Boolean = false) {
        val pose = revealContainerPose(layer, now)
        val alpha = layer.reveal.alpha(now)
        val applied = applyRevealContainer(transaction, layer, pose, force)
        transaction.callMethod("setAlpha", layer.effect, alpha)
        if (layer.baseY > 0 && layer.x.isFinite()) {
            val restY = layer.baseY + motionOffset(layer, now) + layer.reveal.risePx(layer.density, now)
            val shiftX = pivotOffset(applied.scaleX)
            val shiftY = pivotOffset(applied.scaleY)
            transaction.callMethod(SET_POSITION, layer.effect,
                layer.x + layer.width * shiftX, restY + layer.height * shiftY)
            layer.y = restY
        }
        rememberContainer(layer, pose, applied)
        layer.revealAlpha = alpha
        // TODO(twitch-diag): called by prime and by applyAppearance (visibility flips) - rare.
        glassClient.record("reveal dbg pose alpha=$alpha risePx=${layer.reveal.risePx(layer.density, now)}")
    }

    /**
     * Put every layer into the reveal's first pose as soon as the reveal is armed.
     *
     * Most unlocks find the dock layer already built, but simply hidden behind the keyguard. Posing
     * it here means that even if a traversal shows it before the frame loop's first pass, what
     * becomes visible is already the first frame of the animation. Layers created later are caught
     * by {@code applyAppearance}, which poses them in the transaction that shows them.
     */
    private fun primeReveal() {
        val wm = service ?: return
        if (stopped) return
        runCatching {
            synchronized(wm.getObjectFieldAs<Any>(WM_LOCK)) {
                synchronized(layers) {
                    if (layers.isEmpty()) return
                    val transaction = motionTransaction ?: loadClass(TRANSACTION)
                        .getConstructor().newInstance().also { motionTransaction = it }
                    val now = SystemClock.uptimeMillis()
                    layers.values.forEach { applyRevealPose(transaction, it, now) }
                    transaction.callMethod("apply")
                    glassClient.record("unlock reveal primed layers=${layers.size}")
                }
            }
        }.onFailure {
            glassClient.record("unlock reveal prime failed=${it.javaClass.simpleName}")
        }
    }

    /**
     * Safety net for a reveal whose frame source went away mid-flight.
     *
     * It runs after {@link DockUnlockReveal#SETTLE_MS}, but a timer scheduled by an earlier unlock
     * can still land while a later one is animating, so each layer is asked whether its own reveal
     * has stalled. Whatever is stalled and still holds a non-resting transform therefore lost its
     * frames - including the case where the dock was primed and then never animated, which would
     * otherwise leave it invisible forever.
     */
    private fun resetStuckReveal() {
        val wm = service ?: return
        if (stopped) return
        runCatching {
            synchronized(wm.getObjectFieldAs<Any>(WM_LOCK)) {
                synchronized(layers) {
                    val now = SystemClock.uptimeMillis()
                    val stale = layers.values.filter {
                        it.reveal.isStalled(now) && (it.reveal.hasPendingPose() ||
                            !hasRestingContainer(it) || it.revealAlpha != 1f || it.revealSlide != 0f)
                    }
                    if (stale.isEmpty()) return
                    val transaction = motionTransaction ?: loadClass(TRANSACTION)
                        .getConstructor().newInstance().also { motionTransaction = it }
                    stale.forEach {
                        it.reveal.cancel()
                        restoreRestingTransform(transaction, it)
                    }
                    transaction.callMethod("apply")
                    stale.forEach { it.reveal.onPoseCommitted(now) }
                    glassClient.record("unlock reveal force reset layers=${stale.size}")
                }
            }
        }.onFailure {
            glassClient.record("unlock reveal reset failed=${it.javaClass.simpleName}")
        }
    }

    private fun requestTraversal() {
        val wm = service ?: return
        val handler = wm.getObjectFieldAs<Handler>(WM_HANDLER)
        handler.post {
            if (!stopped) runCatching {
                synchronized(wm.getObjectFieldAs<Any>(WM_LOCK)) {
                    wm.getObjectFieldAs<Any>("mWindowPlacerLocked").callMethod("requestTraversal")
                }
            }.onFailure { notePrepareFailure(it) }
        }
    }

    private fun recordMotion(layer: Layer, y: Float, now: Long, visible: Boolean, source: String) {
        val running = if (layer.nativeApplied) {
            val progress = layer.nativeMotion.progress()
            progress > 0.001f && kotlin.math.abs(progress - 1f) > 0.001f
        } else layer.motion.isRunning(now)
        if (layer.motionEndPending && (layer.motionSamples < 6 || !running)) {
            glassClient.record("motion position offsetY=${y - layer.baseY} running=$running visible=$visible source=${if (layer.nativeApplied) "native-$source" else source}")
            layer.motionSamples++
            if (!running) layer.motionEndPending = false
        }
    }

    private fun updateMotionFrame(frameTimeNanos: Long) {
        val wm = service ?: return
        runCatching {
            synchronized(wm.getObjectFieldAs<Any>(WM_LOCK)) {
                synchronized(layers) {
                    if (stopped || !settings.enabled) return
                    var needsFrame = false
                    val frames = ArrayList<FrameUpdate>()
                    val monotonicNow = System.nanoTime()
                    for ((window, layer) in layers) {
                        // Read the clock before the visibility test: an armed reveal still has to
                        // expire while the launcher is briefly hidden behind a going-away keyguard.
                        val now = DockRecentsMotion.frameTimeMillis(frameTimeNanos, layer.motionTime)
                        layer.motionTime = now
                        if (window.callMethod("isVisible") != true || layer.effect.callMethod(IS_VALID) != true) {
                            layer.motion.finish()
                            layer.nativeMotion.reset()
                            layer.nativeApplied = false
                            layer.nativeScene = -1
                            layer.nativeSampleDeadlineNs = 0
                            layer.resetAutoAim()
                            if (layer.reveal.needsFrame(now)) needsFrame = true
                            continue
                        }
                        if (layer.reveal.startIfArmed(now)) {
                            glassClient.record("unlock reveal started uptimeMs=$now")
                        }
                        if (layer.reveal.needsFrame(now)) needsFrame = true
                        val y = layer.baseY + motionOffset(layer, now) +
                            layer.reveal.risePx(layer.density, now)
                        if (layer.nativeSampleDeadlineNs > monotonicNow
                            || layer.autoAimDeadlineNs > monotonicNow
                            || (!layer.nativeApplied && layer.motion.isRunning(now))) needsFrame = true
                        val pose = revealContainerPose(layer, now, monotonicNow)
                        val alpha = layer.reveal.alpha(now)
                        // The horizontal slide is part of the same gesture as the lift, so it
                        // must keep the frame loop dirty even once opacity and scale settle.
                        val slide = layer.reveal.slidePx(layer.width.toFloat(), now)
                        if (layer.x.isFinite() && layer.baseY > 0 &&
                            (y != layer.y || containerChanged(layer, pose) ||
                                alpha != layer.revealAlpha || slide != layer.revealSlide ||
                                (layer.reveal.hasPendingPose() && !layer.reveal.isRunning()))) {
                            frames.add(FrameUpdate(layer, y, pose, alpha, slide))
                        }
                    }
                    if (frames.isNotEmpty()) {
                        val transaction = motionTransaction ?: loadClass(TRANSACTION)
                            .getConstructor().newInstance().also { motionTransaction = it }
                        for (frame in frames) {
                            val (layer, y, pose, alpha, slide) = frame
                            // Scale and alpha are written only when they change, so the reveal can
                            // share one transaction with the real-time follow. The position always
                            // carries the pivot compensation, which is zero once scale reaches 1.
                            val applied = if (containerChanged(layer, pose)) {
                                applyRevealContainer(transaction, layer, pose)
                            } else AppliedContainer(layer.revealScaleX, layer.revealScaleY)
                            frame.appliedScaleX = applied.scaleX
                            frame.appliedScaleY = applied.scaleY
                            if (alpha != layer.revealAlpha) transaction.callMethod("setAlpha", layer.effect, alpha)
                            val shiftX = pivotOffset(applied.scaleX)
                            val shiftY = pivotOffset(applied.scaleY)
                            transaction.callMethod(SET_POSITION, layer.effect,
                                layer.x + layer.width * shiftX + slide,
                                y + layer.height * shiftY)
                            // TODO(twitch-diag): trace frame-loop writes right after an unlock.
                            if (revealDebugActive() && (alpha != layer.revealAlpha ||
                                SystemClock.uptimeMillis() - lastFrameDbgUptime > 40L)) {
                                lastFrameDbgUptime = SystemClock.uptimeMillis()
                                glassClient.record("reveal dbg frame yOff=${y - layer.baseY} " +
                                    "slide=$slide alpha=$alpha nativeApplied=${layer.nativeApplied}")
                            }
                        }
                        transaction.callMethod("setAnimationTransaction")
                        // Deliberately no frame-timeline tag here. The vsync id came from the
                        // frame clock we no longer own, and a stale id is worse than none: the
                        // static and traversal submissions above apply without one and are just
                        // as correct.
                        transaction.callMethod("apply")
                        // Publish cached positions only after a successful submission.
                        for (frame in frames) {
                            val (layer, y, pose, alpha, slide) = frame
                            layer.y = y
                            rememberContainer(layer, pose,
                                AppliedContainer(frame.appliedScaleX, frame.appliedScaleY))
                            layer.revealAlpha = alpha
                            layer.revealSlide = slide
                            layer.reveal.onPoseCommitted(layer.motionTime)
                            recordMotion(layer, y, layer.motionTime, true, "vsync")
                        }
                    }
                    directRecoveryDelay = 100L
                    if (needsFrame) scheduleAnimationFrame()
                }
            }
        }.onFailure {
            // Wallpaper/display replacement and suspend can invalidate the cached transaction
            // temporarily. Keep the static background, but rebuild the direct path; permanently
            // disabling it makes all later gestures lose real-time following. Nothing is torn
            // down here: the tick is a plain Handler message, so dropping it cannot race a
            // queued callback into a system_server SIGSEGV.
            directMotionAvailable = false
            runCatching { motionTransaction?.callMethod("close") }
            motionTransaction = null
            glassClient.record("motion direct frame unavailable=${it.javaClass.simpleName}")
            requestTraversal()
            scheduleDirectMotionRecovery()
        }
    }

    private fun bindNativeMotion(window: Any, layer: Layer) {
        // Bind the Binder identity eagerly so early samples are never lost.
        // The native transport starts sending as soon as hooks are installed,
        // which may precede the first prepareSurfaces where the window is visible.
        // Re-read the window's Session on every update: HYOS builds a fresh Session for
        // each launcher process, and a cached one would pin this layer to a dead uid/pid,
        // so every sample is authenticated away until the desktop is restarted.
        val current = runCatching { window.getObjectFieldAs<Any>("mSession") }.getOrNull()
        if (current != null && current !== layer.motionSession) {
            layer.motionSession = current
            layer.resetNativeMotion()
        }
        val session = layer.motionSession ?: return
        runCatching {
            val uid = session.getObjectFieldAs<Int>("mUid")
            val pid = session.getObjectFieldAs<Int>("mPid")
            if (uid < 10000 || pid <= 0) return@runCatching
            if (uid != layer.nativeUid || pid != layer.nativePid) {
                layer.nativeMotion.reset()
                layer.nativeApplied = false
                layer.nativeScene = -1
                layer.nativeSampleDeadlineNs = 0
                layer.resetAutoAim()
                layer.nativeUid = uid
                layer.nativePid = pid
                glassClient.record("native motion Binder identity uid=$uid pid=$pid")
            }
            // Re-assert on every traversal, not only when the uid/pid changes. The receiver
            // retains (and refuses to apply) every sample until this exact identity is bound,
            // and a module hot reload installs a fresh receiver whose identity store starts
            // empty. A layer that already holds the right uid/pid would then never bind it, so
            // real-time following looks dead until the window is recreated - leave and re-enter
            // the launcher - even though the native side keeps publishing samples.
            // bindIdentity also promotes an already-retained sample, so a frame that arrived
            // during the race is recovered in this same traversal instead of being dropped.
            nativeMotionEndpoint.bindIdentity(uid, pid)
        }.onFailure {
            if (observed.add("native-motion-identity")) {
                glassClient.record("native motion identity unavailable=${it.javaClass.simpleName}")
            }
        }
    }

    /**
     * Safety net for the receiver identity.
     *
     * [bindNativeMotion] re-asserts the exact uid/pid on every launcher traversal, but a
     * traversal is not guaranteed after the process is replaced: WMS can place the surface
     * once and then leave the window alone while the device idles. Because the receiver
     * retains - and refuses to apply - every sample until that identity is bound, a stale
     * identity would silently kill real-time following until the window happened to be
     * recreated, which is exactly the "leave and re-enter the launcher fixes it" symptom.
     * One field read per layer per second is cheap insurance against that.
     *
     * The same tick also bounds how stale the dock preferences can be, and applies them directly.
     * Neither a change callback nor a WMS traversal is guaranteed: the framework can drop the
     * remote update push, and WMS has no reason to traverse an idle launcher window at all. Since
     * the loop already exists and only runs while a dock layer is on screen, re-reading here turns
     * "edited the height, nothing happened until the window was recreated" into at worst one
     * second of delay.
     */
    private fun scheduleNativeBindSweep() {
        val wm = service ?: return
        if (stopped || !nativeBindSweepScheduled.compareAndSet(false, true)) return
        val handler = wm.getObjectFieldAs<Handler>(WM_HANDLER)
        handler.postDelayed({
            nativeBindSweepScheduled.set(false)
            if (stopped) return@postDelayed
            var keepGoing = false
            runCatching {
                val changed = refreshSettings()
                // The rotation gate has to be polled here: a rotated app hides the launcher window,
                // WMS stops traversing it, and nothing else observes the display while that lasts.
                val rotationChanged = layerUpdate.refreshRotationGate()
                // Back off while nothing changes: an idle home screen must not wake the display
                // thread every second. Any traversal, layer change or preference event calls
                // resetSweepCadence(), so the fast cadence only survives while things move.
                if (changed || rotationChanged) {
                    sweepIntervalMs = NATIVE_BIND_SWEEP_MS
                } else if (sweepIntervalMs < SWEEP_BACKOFF_MAX_MS) {
                    val previous = sweepIntervalMs
                    sweepIntervalMs = (previous * 2).coerceAtMost(SWEEP_BACKOFF_MAX_MS)
                    glassClient.record("sweep cadence backoff ${previous}→$sweepIntervalMs ms")
                }
                // Preserve the WM -> layer lock order used by the wallpaper command path.
                synchronized(wm.getObjectFieldAs<Any>(WM_LOCK)) {
                    synchronized(layers) {
                        if (stopped) return@postDelayed
                        keepGoing = layers.isNotEmpty()
                        layers.entries.toList().forEach { (window, layer) ->
                            bindNativeMotion(window, layer)
                        }
                        if (changed) {
                            // Re-apply the geometry ourselves instead of hoping that the traversal
                            // requested below reaches prepareSurfaces for an idle window. Same
                            // order the wallpaper command path uses: update, then commit.
                            layers.keys.toList().forEach { window ->
                                runCatching { updateLayer(window) }.onFailure {
                                    glassClient.record("prefs apply failed=${it.javaClass.simpleName}")
                                }
                            }
                        }
                    }
                }
                if (changed || rotationChanged) requestTraversal()
            }.onFailure {
                if (observed.add("native-bind-sweep")) {
                    glassClient.record("native motion bind sweep failed=${it.javaClass.simpleName}")
                }
            }
            if (keepGoing) scheduleNativeBindSweep()
        }, sweepIntervalMs)
    }

    // Called only under the layer lock. The authenticated Binder receiver publishes an
    // immutable latest sample; intermediate queued values never become a second animation.
    private fun motionOffset(layer: Layer, now: Long): Float {
        val sample = nativeMotionEndpoint.latest(layer.nativeUid, layer.nativePid)
        if (sample?.scene() == DockNativeMotion.SCENE_AUTO_AIM) {
            // Scene 3 is a size truth signal, not the recents hotseat scale. Keeping it out of
            // DockNativeMotion is what prevents unlock from inheriting or resuming a vertical lift.
            if (layer.nativeApplied) {
                layer.nativeMotion.reset()
                layer.nativeApplied = false
                layer.nativeScene = -1
                layer.nativeSampleDeadlineNs = 0
                layer.motion.finish()
            }
            return layer.motion.offsetY(layer.density, layer.baseY, now)
        }
        if (sample != null) {
            layer.nativeMotion.accept(sample, layer.overview)
            layer.nativeApplied = true
            layer.nativeSampleDeadlineNs = sample.uptimeNanos() + DockNativeMotion.MAX_AGE_NS
            if (sample.scene() == 1) {
                layer.nativeOverviewGeneration = layer.overviewGeneration
            }
            if (sample.scene() != layer.nativeScene) {
                layer.nativeScene = sample.scene()
                layer.motionSamples = 0
                layer.motionEndPending = true
                glassClient.record("native motion scene=${sample.scene()} scale=${sample.scale()}")
            }
            return layer.nativeMotion.offsetY(layer.density, layer.baseY)
        }
        if (layer.nativeApplied) {
            // A held recents gesture legitimately produces no changing scale samples.
            // Once the verified overview target has cleared, resume the local return
            // curve from the exact native position instead of freezing or snapping.
            if (layer.overview) return layer.nativeMotion.offsetY(layer.density, layer.baseY)
            layer.motion.resumeFrom(layer.nativeMotion.progress(), false, now)
            layer.nativeApplied = false
            layer.nativeMotion.reset()
            layer.nativeScene = -1
            layer.nativeSampleDeadlineNs = 0
            layer.motionSamples = 0
            layer.motionEndPending = true
            scheduleAnimationFrame()
            glassClient.record("native motion idle after overview exit; resuming return")
        }
        return layer.motion.offsetY(layer.density, layer.baseY, now)
    }


    /**
     * Whether the launcher's overlay window - the minus-one screen - is currently visible.
     *
     * <p>Read straight from WMS's window map, on the display thread and under the same global
     * lock as every other traversal write. This is deliberately the only launcher state we read:
     * OS4's launcher is a Flutter application with no dex at all, so edit mode and the pages
     * themselves live in Dart and no Java hook can reach them, while the overlay is a real
     * WindowState that WMS owns.
     *
     * <p>The visible case short-circuits on the cached state. Otherwise the map is re-scanned at
     * most every [OVERLAY_SCAN_MS]: the overlay survives page changes, but a launcher restart
     * replaces the WindowState and the stale one would never report visible again.
     */
    private fun launcherOverlayVisible(wm: Any, now: Long): Boolean {
        val cached = overlayWindow
        if (cached != null) {
            val visible = runCatching { cached.callMethod("isVisible") as Boolean }.getOrNull()
            if (visible == true) {
                overlayVisible = true
                return true
            }
        }
        if (now - overlayScannedAt < OVERLAY_SCAN_MS) return overlayVisible
        overlayScannedAt = now
        val found = runCatching { findLauncherOverlay(wm) }.onFailure {
            if (observed.add("launcher-overlay-scan")) {
                glassClient.record("launcher overlay scan unavailable=${it.javaClass.simpleName}: " +
                    "${it.message?.take(120)}")
            }
        }.getOrNull()
        overlayWindow = found
        overlayVisible = found != null &&
            runCatching { found.callMethod("isVisible") as Boolean }.getOrDefault(false)
        return overlayVisible
    }

    private fun findLauncherOverlay(wm: Any): Any? {
        for (state in wm.getObjectFieldAs<Map<*, *>>("mWindowMap").values) {
            val name = state?.let {
                runCatching { it.callMethod("getName") as String }.getOrNull()
            } ?: continue
            if (name.startsWith(LAUNCHER_OVERLAY_TITLE)) return state
        }
        return null
    }

    private fun hasPendingMotionFrame(): Boolean {
        val now = SystemClock.uptimeMillis()
        val nowNs = System.nanoTime()
        return synchronized(layers) {
            layers.values.any { layer ->
                // Clock expiry does not submit the resting pose. Keep the terminal frame
                // pending across repeated lost callbacks, including a rise-only residue.
                val revealNeedsFrame = layer.reveal.needsFrame(now)
                val latest = nativeMotionEndpoint.latest(layer.nativeUid, layer.nativePid)
                val nativeNeedsFrame = latest != null &&
                    (latest.scene() != DockNativeMotion.SCENE_AUTO_AIM ||
                        (layer.reveal.getStyle() == DockUnlockReveal.Style.AUTO_AIM &&
                            DockNativeMotion.autoAimScale(
                                latest, layer.reveal.eventEpochMillis(), nowNs) != null))
                revealNeedsFrame || (layer.lastVisible == true &&
                    (layer.reveal.hasPendingPose() ||
                        layer.revealAlpha != 1f || !hasRestingContainer(layer) ||
                        layer.revealSlide != 0f))
                    || nativeNeedsFrame
                    || (layer.nativeApplied && !layer.overview)
                    || (!layer.nativeApplied && layer.motion.isRunning(now))
            }
        }
    }

    private fun wmHandler(): Handler? = runCatching {
        service?.getObjectFieldAs<Handler>(WM_HANDLER)
    }.getOrNull()

    private fun scheduleAnimationFrame(urgent: Boolean = false) {
        if (stopped || !directMotionAvailable) return
        val handler = wmHandler() ?: return
        val epoch = frameEpoch.incrementAndGet()
        val requestedAt = SystemClock.elapsedRealtimeNanos()
        if (!scheduledFrameEpoch.compareAndSet(0, epoch)) {
            val pendingEpoch = scheduledFrameEpoch.get()
            val pendingSince = scheduledFrameStartedNs.get()
            if (urgent && pendingEpoch != 0L && pendingSince != 0L
                && requestedAt - pendingSince >= STALE_FRAME_NS
                && urgentFrameRecoveryScheduled.compareAndSet(false, true)) {
                handler.post {
                    try {
                        if (scheduledFrameEpoch.get() == pendingEpoch) {
                            glassClient.record(
                                "motion progress replaced stale frame epoch=$pendingEpoch")
                            recoverStalledFrame(pendingEpoch)
                        }
                    } finally {
                        urgentFrameRecoveryScheduled.set(false)
                    }
                }
            }
            return
        }
        scheduledFrameStartedNs.set(requestedAt)
        // One Handler tick per displayed frame, re-armed by updateMotionFrame() while the gesture
        // still needs a frame. System.nanoTime() is the same monotonic base the old frame callback
        // handed us, so every consumer downstream is unchanged.
        val tick = Runnable {
            // A tick whose epoch was superseded (a replacement armed, or the loop stopped) is
            // dropped: the epoch is the single source of truth for "this frame is live".
            if (clearScheduledFrame(epoch) && !stopped && directMotionAvailable) {
                runCatching {
                    updateMotionFrame(System.nanoTime())
                }.onFailure {
                    // Never let an optional animation callback throw on a system handler thread.
                    // Nothing is disposed here: the recovery probe simply re-arms the tick later.
                    animationAvailable = false
                    glassClient.record("motion scheduling failed=${it.javaClass.simpleName}: ${it.message?.take(160)}")
                    XposedLog.w(TAG, LOG_TAG, "Dock animation tick failed; retrying on a later frame", it)
                    requestTraversal()
                    scheduleDirectMotionRecovery()
                }
            }
        }
        scheduledFrame = ScheduledFrame(epoch, tick)
        handler.postDelayed(tick, FRAME_INTERVAL_MS)
        // Handler time stops in deep sleep, so this runs shortly after resume even when the
        // pre-suspend tick never ran. The epoch makes a late old tick harmless afterwards.
        handler.postDelayed({ recoverStalledFrame(epoch) }, 100)
    }

    private fun clearScheduledFrame(epoch: Long): Boolean {
        if (!scheduledFrameEpoch.compareAndSet(epoch, 0)) return false
        scheduledFrameStartedNs.set(0)
        if (scheduledFrame?.epoch == epoch) scheduledFrame = null
        return true
    }

    private fun cancelScheduledFrame() {
        val scheduled = scheduledFrame
        if (scheduled != null) {
            wmHandler()?.removeCallbacks(scheduled.tick)
            scheduledFrame = null
        }
        scheduledFrameEpoch.set(0)
        scheduledFrameStartedNs.set(0)
    }

    private fun recoverStalledFrame(epoch: Long) {
        if (scheduledFrameEpoch.get() != epoch) return
        scheduledFrame?.takeIf { it.epoch == epoch }?.let { wmHandler()?.removeCallbacks(it.tick) }
        if (!clearScheduledFrame(epoch)) return
        // Screen-off legitimately pauses the loop, and an idle keepalive must not start a
        // 10 Hz recovery loop. Re-arm only while something still needs a frame.
        if (!hasPendingMotionFrame()) return
        glassClient.record("motion frame tick stalled epoch=$epoch")
        scheduleAnimationFrame()
    }

    private fun scheduleDirectMotionRecovery() {
        if (stopped) return
        val handler = wmHandler() ?: return
        if (!directRecoveryScheduled.compareAndSet(false, true)) return
        val delay = directRecoveryDelay
        directRecoveryDelay = (directRecoveryDelay * 2).coerceAtMost(5_000L)
        handler.postDelayed({
            directRecoveryScheduled.set(false)
            if (stopped) return@postDelayed
            cancelScheduledFrame()
            animationAvailable = true
            directMotionAvailable = true
            glassClient.record("motion direct frame retry after lifecycle interruption delayMs=$delay")
            scheduleAnimationFrame()
        }, delay)
    }

    /**
     * A single failure while posing the Dock must not cost the background for the rest of this
     * system_server's life.
     *
     * <p>Both callers run from the very first traversal after boot, before the launcher, the
     * wallpaper or even our own provider is necessarily reachable, so a transient failure there is
     * expected rather than a defect. [failClosed] is permanent - it latches `stopped` and releases
     * every layer until system_server restarts - which is exactly what a boot-time hiccup must
     * never trigger: the dock simply disappears and no later traversal can bring it back. Give the
     * Dock a few chances inside a short window instead, and keep the failure visible in logcat so
     * the next report names the exception.
     */
    private fun notePrepareFailure(error: Throwable) {
        val now = SystemClock.uptimeMillis()
        if (prepareFailureWindowAt == 0L || now - prepareFailureWindowAt > PREPARE_FAILURE_WINDOW_MS) {
            prepareFailureWindowAt = now
            prepareFailures = 0
        }
        prepareFailures++
        // Error level on purpose: release builds drop v/i/w, and this line is the only record that
        // survives the journal once it has been flushed.
        XposedLog.e(TAG, LOG_TAG, "dock pose failed (#$prepareFailures of $PREPARE_FAILURE_LIMIT): "
            + "${error.javaClass.simpleName}; retrying on the next traversal", error)
        glassClient.record("dock pose failed count=$prepareFailures " +
            "${error.javaClass.simpleName}: ${error.message?.take(160)}")
        if (prepareFailures >= PREPARE_FAILURE_LIMIT) failClosed(error)
    }

    private fun failClosed(error: Throwable) {
        synchronized(layers) {
            if (stopped) return
            stopped = true
            glassClient.record("hook disabled error=${error.javaClass.simpleName}: ${error.message?.take(160)}")
            layers.keys.toList().forEach { removeLayer(it) }
            glassClient.close()
            processGuard.close()
            XposedLog.e(TAG, LOG_TAG, "WMS dock disabled after an error; system windows left unchanged", error)
        }
    }
}
