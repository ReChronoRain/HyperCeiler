/*
 * Derived from MiuiBackGestureHook status-bar-follow implementation.
 * Source: https://github.com/wxxsfxyzm/MiuiBackGestureHook
 * Reference commit: 004bc059e5821775917b771d89aa1d8c18a1e669 (includes b6e582f and later fixes)
 * Copyright (C) 2026 MiuiBackGestureHook contributors.
 * Licensed under the Apache License, Version 2.0; modified for HyperCeiler.
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow;

/** Fence asynchronous attachment and preference callbacks across disable/re-enable and reload. */
final class AppBarFeatureState {
    private boolean enabled;
    private boolean suspended;
    private long generation;

    synchronized void setEnabled(boolean requested) {
        if (enabled != requested) { enabled = requested; generation++; }
    }

    synchronized boolean enabled() { return enabled && !suspended; }
    synchronized long generation() { return generation; }
    synchronized boolean accepts(long expected) { return enabled() && generation == expected; }

    synchronized void suspend() {
        suspended = true;
        generation++;
    }

    synchronized void resumeAfterRejectedReload() {
        suspended = false;
        generation++;
    }
}
