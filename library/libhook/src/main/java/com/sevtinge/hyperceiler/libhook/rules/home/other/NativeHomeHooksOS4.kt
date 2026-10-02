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
package com.sevtinge.hyperceiler.libhook.rules.home.other

import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.annotation.Keep
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.utils.api.ContextUtils
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Loads and configures the launcher-native hooks.
 *
 * The LSPosed native-hook contract only invokes the module's `native_init` at the moment the module
 * itself dlopens the library (`System.loadLibrary`) inside the target process. There is no
 * framework-side automatic load. That makes this object the single entry point of the whole
 * `com.miui.home` native motion chain, so it must not depend on a single rule being selected:
 * both [init] (from `HomePhone`) and [ensureLoadedFromModuleEntry] (from the module entry, before
 * any rule gate) funnel into [ensureLoaded].
 *
 * The two `external` functions below are resolved by their JNI symbol names, which encode this
 * class's fully qualified name. Renaming or moving this object therefore requires updating the
 * matching `Java_com_sevtinge_hyperceiler_libhook_rules_home_other_NativeHomeHooksOS4_native*`
 * definitions in `app/src/main/cpp/targets/home/hyperceiler_home.cpp`. There is no
 * `RegisterNatives` table to keep the two in sync automatically.
 */
@Keep
object NativeHomeHooksOS4 {
    private external fun nativeConfigure(
        highDeviceLevel: Boolean,
        disablePrestart: Boolean,
        softGlass: Boolean
    ): Int
    private external fun nativeStatus(): Int

    /**
     * Push the Dock switch into the native pipeline.
     *
     * <p>The native chain is started by process name and has no other gate, so without this a
     * launcher inside the module's scope keeps its hook health worker and its Binder publisher
     * running whether or not the user ever enabled the Dock. Fail-open on the native side: a failed
     * or missing push leaves the pipeline enabled.
     */
    private external fun nativeSetDockEnabled(enabled: Boolean): Int

    /** Unconditional logcat tag: these stages must be readable even when prefs/log level are broken. */
    private const val LOG_TAG = "HyperCeiler.NativeHome"
    private const val LIBRARY = "HyperCeilerNative"
    /** The Dock feature switch; see `HomeDockWindow`'s settings read. */
    private const val DOCK_ENABLED_KEY = "home_dock_bg_custom_enable"
    private const val LIBRARY_FILE = "libHyperCeilerNative.so"
    private const val PACKAGE = "com.sevtinge.hyperceiler"

    private val loadStarted = AtomicBoolean()
    private val diagnosticsStarted = AtomicBoolean()
    private val dockWatchStarted = AtomicBoolean()
    private val diagnosticsUri = Uri.parse("content://com.sevtinge.hyperceiler.provider.sharedprefs")

    /**
     * Rule-side entry (`HomePhone.onPackageLoaded`). Kept for the normal path; the module-entry
     * probe covers the case where the launcher rule never gets selected.
     */
    fun init() {
        ensureLoaded("homeRule")
    }

    /**
     * Module-entry probe. Called from `XposedInitEntry` as soon as the framework hands over the
     * launcher process, before rule matching, crash safe mode and per-package preference gates.
     * Being independent of those gates is the point: if any of them refuses to load `HomePhone`,
     * the native chain would otherwise never start and the device would silently fall back to the
     * `miui.wallpaper.animation` spring in system_server.
     */
    fun ensureLoadedFromModuleEntry(processName: String?) {
        ensureLoaded("moduleEntry", processName)
    }

    private fun ensureLoaded(source: String, processName: String? = null) {
        stage("probe begin source=$source process=${processName ?: "unknown"}")
        if (!loadStarted.compareAndSet(false, true)) {
            stage("probe skipped source=$source alreadyLoaded=true")
            return
        }

        var initialStatus = 0
        var failure: Throwable? = null
        try {
            loadLibrary()
            stage("library loaded name=$LIBRARY")
        } catch (t: Throwable) {
            failure = t
            stage("library load failed detail=${t.javaClass.simpleName}: ${describe(t)}")
        }

        if (failure == null) {
            try {
                initialStatus = nativeConfigure(
                    prefBoolean("home_other_high_models"),
                    prefBoolean("home_other_disable_prestart"),
                    prefBoolean("home_dock_bg_custom_enable") && prefStringInt("home_dock_add_blur") == 1
                )
                stage("configure done status=${hex(initialStatus)}${describeStatus(initialStatus)}")
                // The Dock preference is what decides whether the launcher pays for the native
                // motion pipeline at all; push it before anything else can start following.
                val dockStatus = syncDockEnabled()
                stage("dock switch status=${hex(dockStatus)} enabled=${prefBoolean(DOCK_ENABLED_KEY)}")
                watchDockEnabled()
            } catch (t: Throwable) {
                failure = t
                stage("configure failed detail=${t.javaClass.simpleName}: ${describe(t)}")
            }
        }

        startDiagnostics(initialStatus, failure)
    }

    /**
     * `System.loadLibrary` resolves through the namespace LSPosed prepared for the module. When
     * that namespace is missing (for example when an in-place reinstall invalidated the module APK
     * path the framework still had cached), fall back to the absolute library path of the installed
     * module. Both forms end in `/libHyperCeilerNative.so`, which is what the framework matches to
     * decide whether to call `native_init`.
     */
    private fun loadLibrary() {
        try {
            System.loadLibrary(LIBRARY)
            return
        } catch (first: Throwable) {
            stage("loadLibrary($LIBRARY) failed detail=${first.javaClass.simpleName}: ${describe(first)}")
            val directory = runCatching {
                ContextUtils.getContextNoError(ContextUtils.FLAG_CURRENT_APP)
                    ?.packageManager
                    ?.getApplicationInfo(PACKAGE, 0)
                    ?.nativeLibraryDir
            }.getOrNull()
            if (directory.isNullOrEmpty()) {
                stage("absolute library path unavailable; rethrowing first failure")
                throw first
            }
            System.load("$directory/$LIBRARY_FILE")
        }
    }

    private fun startDiagnostics(initialStatus: Int, failure: Throwable?) {
        if (!diagnosticsStarted.compareAndSet(false, true)) return
        Thread({
            val delays = longArrayOf(0L, 2_000L, 16_000L)
            delays.forEachIndexed { phase, delay ->
                if (delay != 0L) SystemClock.sleep(delay - delays[phase - 1])
                val status = if (failure == null) {
                    runCatching { nativeStatus() }.getOrDefault(initialStatus)
                } else initialStatus
                recordStatus(phase, status, failure)
            }
        }, "HyperCeiler-NativeHomeStatus").start()
    }

    private fun recordStatus(phase: Int, status: Int, failure: Throwable?) {
        val event = buildString {
            append("wall=").append(System.currentTimeMillis())
            append(" up=").append(SystemClock.uptimeMillis())
            append(" native launcher v33 phase=").append(phase)
            append(" status=").append(hex(status))
            append(describeStatus(status))
            failure?.let {
                append(" error=").append(it.javaClass.simpleName)
                append(':').append(describe(it))
            }
        }.take(512)
        stage("status phase=$phase status=${hex(status)}${describeStatus(status)}")
        val context = ContextUtils.getContextNoError(ContextUtils.FLAG_CURRENT_APP) ?: return
        runCatching {
            context.contentResolver.call(
                diagnosticsUri,
                "dock_glass_native_record",
                null,
                Bundle().apply { putStringArray("events", arrayOf(event)) }
            )
        }
    }

    private fun describeStatus(status: Int) = buildString {
        append(" configured=").append(status and 0x01 != 0)
        append(" hookApi=").append(status and 0x02 != 0)
        append(" unhookApi=").append(status and 0x04 != 0)
        append(" workerStarted=").append(status and 0x08 != 0)
        append(" workerAlive=").append(status and 0x10 != 0)
        append(" runtimeReady=").append(status and 0x20 != 0)
        append(" launcher=").append(status and 0x40 != 0)
        append(" spawner=").append(status and 0x80 != 0)
    }

    private fun hex(value: Int) = "0x" + value.toString(16)

    private fun describe(t: Throwable) = (t.message ?: t.javaClass.name).take(160)

    /** Diagnostics must never become a second failure path. */
    private fun stage(message: String) {
        runCatching { Log.i(LOG_TAG, "stage=$message") }
    }

    /** Re-push the switch when the preference changes; LSPosed pushes updates to hooked processes. */
    private fun watchDockEnabled() {
        if (!dockWatchStarted.compareAndSet(false, true)) return
        runCatching {
            PrefsBridge.getSharedPreferences()?.registerOnSharedPreferenceChangeListener { _, key ->
                if (key == DOCK_ENABLED_KEY) {
                    val status = syncDockEnabled()
                    stage("dock switch refreshed status=${hex(status)} enabled=${prefBoolean(DOCK_ENABLED_KEY)}")
                }
            }
        }.onFailure {
            stage("dock switch listener unavailable detail=${describe(it)}")
        }
    }

    private fun syncDockEnabled(): Int = runCatching {
        nativeSetDockEnabled(prefBoolean(DOCK_ENABLED_KEY))
    }.onFailure {
        stage("dock switch push failed detail=${describe(it)}")
    }.getOrDefault(0)

    private fun prefBoolean(key: String): Boolean =
        runCatching { PrefsBridge.getBoolean(key) }.getOrDefault(false)

    private fun prefStringInt(key: String): Int =
        runCatching { PrefsBridge.getStringAsInt(key, 0) }.getOrDefault(0)
}
