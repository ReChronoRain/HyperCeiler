/*
 * This file is part of HyperCeiler, licensed under AGPL-3.0-or-later.
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.media3

import android.os.Looper

/** Loads only for a selected standard-color background task; never for Monet. */
internal object AmbientNativeColorOS4 {
    @Volatile private var loadState = 0
    private var hotReloadPaused = false

    /** Freeze an unloaded generation before cleanup; ART cannot rebind a loaded DSO. */
    @JvmStatic
    @Synchronized
    fun pauseForHotReload(): Boolean {
        if (loadState == 1) return false
        hotReloadPaused = true
        return true
    }

    /** Called only when a proposed reload was rejected and this generation remains active. */
    @JvmStatic
    @Synchronized
    fun resumeAfterRejectedHotReload() {
        hotReloadPaused = false
    }

    fun resolve(pixels: IntArray, width: Int, height: Int): Int? {
        if (Looper.myLooper() === Looper.getMainLooper() || Thread.currentThread().isInterrupted) return null
        val count = width.toLong() * height.toLong()
        if (width <= 0 || height <= 0 || count > pixels.size) return null
        if (!ensureLoaded()) return null
        return try {
            val encoded = nativeResolve(pixels, width, height)
            if (encoded ushr 32 == 1L) encoded.toInt() else null
        } catch (_: Exception) {
            null
        } catch (_: LinkageError) {
            null
        }
    }

    // All three synchronized methods are static and share this Class monitor.
    @JvmStatic
    @Synchronized
    private fun ensureLoaded(): Boolean {
        if (hotReloadPaused) return false
        if (loadState == 0) {
            loadState = try {
                System.loadLibrary("HyperCeilerAmbientOS4")
                1
            } catch (_: Exception) {
                -1
            } catch (_: LinkageError) {
                -1
            }
        }
        return loadState == 1
    }

    // Zero is unavailable. Success is 1 in the high word plus all 32 ARGB bits,
    // so a genuinely transparent result is distinct from native failure.
    @JvmStatic
    private external fun nativeResolve(pixels: IntArray, width: Int, height: Int): Long
}
