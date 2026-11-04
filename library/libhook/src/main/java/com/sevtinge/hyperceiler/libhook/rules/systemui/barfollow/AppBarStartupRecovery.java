/*
 * Derived from MiuiBackGestureHook status-bar-follow implementation.
 * Source: https://github.com/wxxsfxyzm/MiuiBackGestureHook
 * Reference commit: 004bc059e5821775917b771d89aa1d8c18a1e669
 * Copyright (C) 2026 MiuiBackGestureHook contributors.
 * Licensed under the Apache License, Version 2.0; modified for HyperCeiler.
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow;

import java.util.function.Consumer;

/** Coalesces native Home-ready events on the View owner, fenced by preference/reload generation. */
final class AppBarStartupRecovery {
    private final AppBarFeatureState feature;
    private final Consumer<Runnable> dispatch;
    private Object pending;

    AppBarStartupRecovery(AppBarFeatureState feature, Consumer<Runnable> dispatch) {
        this.feature = feature;
        this.dispatch = dispatch;
    }

    // Called on the same executor that dispatches the recovery.
    void homeReady(Runnable recover) {
        if (!feature.enabled()) return;
        long generation = feature.generation();
        Object attempt = new Object();
        pending = attempt;
        dispatch.accept(() -> {
            if (pending != attempt) return;
            pending = null;
            if (feature.accepts(generation)) recover.run();
        });
    }
}
