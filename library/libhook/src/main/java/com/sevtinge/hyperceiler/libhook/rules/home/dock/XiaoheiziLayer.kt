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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.SystemClock
import android.view.SurfaceControl
import android.view.SurfaceControlViewHost
import io.github.lingqiqi5211.ezhooktool.core.callMethod

/**
 * Drives the "Xiaoheizi mode" performance layer from inside system_server.
 *
 * <p>This class never renders: it asks the module process for a SurfacePackage, reparents it under
 * the launcher window and positions it over the search capsule. All drawing happens in the module's
 * own HWUI, so a failure here costs one empty layer and never the system process.
 *
 * <p>Threading rule: [show] and [hide] run on the WMS thread and therefore does nothing but a few
 * volatile reads and one {@code post}. Every Binder call, transaction and teardown happens on the
 * private worker. An exception escaping onto the WMS thread would kill system_server, so the worker
 * body catches {@link Exception} only - an Error still propagates and is not swallowed.
 *
 * <p>The capsule geometry is measured, not derived: the OS4 capsule is a small centred pill sitting
 * above the Dock. The overlay is wider than the pill so its perimeter can expand without
 * clipping. Its position follows the live Dock bounds and remains centered in the window.
 */
class XiaoheiziLayer(
    private val contextProvider: () -> Context?,
    private val record: (String) -> Unit,
) {
    /** Written from the preference listener, read on the WMS thread. */
    @Volatile var enabled = false
    @Volatile var rage = false

    /** Width of the animation canvas, including room for the expanding perimeter. */
    private val capsuleWidthDp = 82f

    /** Capsule height in dp, measured on the OS4 launcher (20dp at 3x = 60px). */
    private val capsuleHeightDp = 20f

    /** Gap between the capsule's bottom edge and the Dock's top edge, in dp. */
    private val capsuleDockGapDp = 22f

    /** Older hosts acknowledge playback without its duration. */
    private val legacyVisibleMs = 2_000L

    /** Each unobscured desktop entry gets one attempt. The worker drops obsolete entries. */
    private class Attempt(val rage: Boolean)
    @Volatile private var entry: Attempt? = null
    val hasEntry get() = entry != null
    fun retireChangedMode() {
        if (entry?.rage != rage) hide()
    }

    private val worker by lazy {
        Handler(HandlerThread("HyperCeiler-Xiaoheizi").apply { start() }.looper)
    }

    private class Lease(
        val surface: SurfaceControl,
        val parcel: SurfaceControlViewHost.SurfacePackage,
        val lifetime: IBinder?,
        val death: IBinder.DeathRecipient,
    )

    /** Identity handed to the host so it can drop the layer if this process goes away. */
    private val ownerToken: IBinder = Binder()

    /** Worker-owned host and transaction resources. */
    private data class RageBinding(val context: Context, val connection: ServiceConnection)
    private var binding: RageBinding? = null
    private var hostId: String? = null
    private var lease: Lease? = null
    private val finished = Runnable { clearLease("performance finished") }

    fun show(parent: SurfaceControl, frameWidth: Int, dockTop: Int, density: Float) {
        if (entry != null) return
        val attempt = Attempt(rage)
        entry = attempt
        val height = (capsuleHeightDp * density).toInt()
        val width = if (attempt.rage) minOf(frameWidth, (480f * density).toInt())
            else (capsuleWidthDp * density).toInt()
        val gap = (capsuleDockGapDp * density).toInt()
        val left = (frameWidth - width) / 2f
        val top = dockTop - gap - height.toFloat()
        worker.post {
            if (entry !== attempt || !enabled || !parent.isValid) return@post
            val context = contextProvider() ?: return@post
            clearLease("replaced")
            if (attempt.rage) {
                bindRageRenderer(context) {
                    if (entry === attempt && enabled && parent.isValid) {
                        perform(context, parent, left, top, width, height, true)
                    } else clearLease("desktop changed before rage renderer connected")
                }
            } else perform(context, parent, left, top, width, height, false)
        }
    }

    fun hide() {
        if (entry == null) return
        entry = null
        worker.post { clearLease("desktop hidden or disabled") }
    }

    private fun bindRageRenderer(context: Context, connected: () -> Unit) {
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                if (binding?.connection === this) connected()
            }
            override fun onServiceDisconnected(name: ComponentName) {
                if (binding?.connection === this) clearLease("rage renderer disconnected")
            }
            override fun onBindingDied(name: ComponentName) = onServiceDisconnected(name)
            override fun onNullBinding(name: ComponentName) = onServiceDisconnected(name)
        }
        val intent = Intent().setClassName("com.sevtinge.hyperceiler",
            "com.sevtinge.hyperceiler.libhook.provider.XiaoheiziRageService")
        try {
            if (!context.bindService(intent, Context.BIND_AUTO_CREATE or Context.BIND_IMPORTANT,
                    { worker.post(it) }, connection)) {
                record("xiaoheizi rage renderer binding refused")
                return
            }
            binding = RageBinding(context, connection)
            worker.postDelayed({
                if (binding?.connection === connection && hostId == null) clearLease("rage renderer connection timed out")
            }, 3_000L)
        } catch (error: RuntimeException) {
            record("xiaoheizi rage renderer unavailable: ${error.javaClass.simpleName}")
        }
    }

    private fun perform(
        context: Context,
        parent: SurfaceControl,
        left: Float,
        top: Float,
        width: Int,
        height: Int,
        rage: Boolean,
    ) {
        try {
            val id = "xc-" + SystemClock.uptimeMillis()
            hostId = id
            val args = Bundle().apply {
                putInt("width", width)
                putInt("capsuleHeight", height)
                putBinder("owner", ownerToken)
                if (rage) putBoolean("rage", true)
            }
            val response = context.contentResolver.call(uri, "xiaoheizi_create", id, args)
            val refusal = response?.getString("error")
            if (response == null || refusal != null) {
                record("xiaoheizi create refused: ${refusal ?: "no response"}")
                clearLease("create refused")
                return
            }
            response.classLoader = SurfaceControlViewHost.SurfacePackage::class.java.classLoader
            val parcel = response.getParcelable(
                "surface", SurfaceControlViewHost.SurfacePackage::class.java
            )
            if (parcel == null) {
                record("xiaoheizi create returned no surface")
                clearLease("surface package unavailable")
                return
            }
            val surface = parcel.callMethod("getSurfaceControl") as? SurfaceControl
            if (surface == null) {
                parcel.release()
                clearLease("surface unavailable")
                return
            }
            val capsuleInset = response.getInt("capsuleInset")
            val lifetime = response.getBinder("lifetime")
            val death = IBinder.DeathRecipient {
                worker.post { if (hostId == id) clearLease("renderer died") }
            }
            lease = Lease(surface, parcel, lifetime, death)
            lifetime?.linkToDeath(death, 0)
            SurfaceControl.Transaction().use { transaction ->
                transaction.reparent(surface, parent)
                // Above the Dock glass (layer 2), below anything the launcher puts higher.
                transaction.setLayer(surface, 3)
                transaction.setPosition(surface, left, top - capsuleInset)
                transaction.callMethod("setWindowCrop", surface, width, height + capsuleInset * 2)
                transaction.callMethod("show", surface)
                transaction.apply()
            }
            val play = context.contentResolver.call(uri, "xiaoheizi_play", id, Bundle.EMPTY)
            if (play?.getBoolean("playing") != true) {
                record("xiaoheizi play refused: ${play?.getString("error") ?: "no acknowledgement"}")
                clearLease("play refused")
                return
            }
            if (rage) {
                if (play.getBoolean("continuous")) record("xiaoheizi rage playing x=$left y=$top w=$width h=$height")
                else clearLease("continuous playback unsupported")
                return
            }
            val durationMs = play.getInt("durationMs")
            val visibleMs = if (durationMs in 1..5_000) durationMs + 300L else legacyVisibleMs
            record("xiaoheizi playing x=$left y=$top w=$width h=$height durationMs=$durationMs")
            worker.postDelayed(finished, visibleMs)
        } catch (error: Exception) {
            record("xiaoheizi attach failed ${error.javaClass.simpleName}: ${error.message?.take(120)}")
            clearLease("attach failed")
        }
    }

    private fun clearLease(reason: String) {
        worker.removeCallbacks(finished)
        val rageBinding = binding
        binding = null
        val id = hostId
        hostId = null
        val current = lease
        lease = null
        if (current != null) {
            try {
                if (current.surface.isValid) {
                    SurfaceControl.Transaction().use { it.reparent(current.surface, null).apply() }
                }
            } catch (error: Exception) {
                record("xiaoheizi detach failed ${error.javaClass.simpleName}")
            }
            try { current.lifetime?.unlinkToDeath(current.death, 0) } catch (_: RuntimeException) {}
            current.parcel.release()
        }
        if (id != null) {
            try {
                contextProvider()?.contentResolver?.call(uri, "xiaoheizi_release", id, Bundle.EMPTY)
            } catch (error: Exception) {
                record("xiaoheizi release failed ${error.javaClass.simpleName}")
            }
        }
        if (rageBinding != null) {
            try { rageBinding.context.unbindService(rageBinding.connection) }
            catch (error: RuntimeException) { record("xiaoheizi rage unbind failed: ${error.javaClass.simpleName}") }
        }
        if (id != null || rageBinding != null) record("xiaoheizi layer retired: $reason")
    }

    fun close() {
        enabled = false
        hide()
    }

    private companion object {
        val uri: Uri = Uri.parse("content://com.sevtinge.hyperceiler.provider.sharedprefs")
    }
}
