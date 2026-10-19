/*
 * This file is part of HyperCeiler.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Copyright (C) 2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import io.github.lingqiqi5211.ezhooktool.xposed.java.IReplaceHook;

/** Disables only Xiaomi's post-bind per-package count eviction on the OS4 pipeline. */
public final class RemoveNotifNumLimitOS4 extends BaseHook {
    @Override
    public void init() {
        String coordinatorName = "com.android.systemui.statusbar.notification.collection.coordinator.CountLimitCoordinator";
        Class<?> coordinator = findClassIfExists(coordinatorName);
        Class<?> listener = findClassIfExists(coordinatorName + "$$ExternalSyntheticLambda0");
        Class<?> entry = findClassIfExists("com.android.systemui.statusbar.notification.collection.NotificationEntry");
        if (coordinator == null || listener == null || entry == null) {
            throw new IllegalStateException("OS4 notification count callback classes unavailable");
        }

        try {
            Method callback = listener.getDeclaredMethod("onViewBound$1", entry);
            if (callback.getReturnType() != void.class
                || Modifier.isStatic(callback.getModifiers())
                || listener.getDeclaredField("f$0").getType() != coordinator) {
                throw new IllegalStateException("Unrecognized OS4 notification count callback shape");
            }
            // In the reviewed OS4 ROM this callback only finds over-quota groups and
            // dismisses one eligible entry per group. Keep attach(), the bind-event
            // manager, other listeners, and all ordinary dismiss paths intact.
            if (hookMethod(callback, (IReplaceHook) param -> null) == null) {
                throw new IllegalStateException("OS4 notification count callback was not hooked");
            }
        } catch (ReflectiveOperationException error) {
            // Do not broaden the target to attach(), all bind callbacks, or dismissNotification.
            throw new IllegalStateException("OS4 notification count callback signature unavailable", error);
        }
    }
}
