/*
 * Derived from MiuiBackGestureHook status-bar-follow implementation.
 * Source: https://github.com/wxxsfxyzm/MiuiBackGestureHook
 * Reference commit: 004bc059e5821775917b771d89aa1d8c18a1e669 (includes b6e582f and later fixes)
 * Copyright (C) 2026 MiuiBackGestureHook contributors.
 * Licensed under the Apache License, Version 2.0; modified for HyperCeiler.
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow;

import android.os.Looper;
import android.view.View;

import java.lang.reflect.Method;

import static com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow.AppBarContentPreparation.read;
import static com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow.AppBarContentPreparation.write;

/** Clock time and battery state stay View-owned; only tint ownership differs from Home. */
final class AppBarClockBatteryBinding implements AutoCloseable {
    private final View clock;
    private final View battery;
    private final Object dark;
    private final Method add;
    private final Method remove;
    private final boolean originalClockShowDark;
    private boolean clockRegistered;
    private boolean batteryRegistered;
    private boolean closed;
    private final View.OnAttachStateChangeListener listener = new View.OnAttachStateChangeListener() {
        @Override public void onViewAttachedToWindow(View view) {
            if (closed) return;
            try { register(view); }
            catch (ReflectiveOperationException failure) {
                throw new IllegalStateException("Native App tint attachment failed", failure);
            }
        }
        @Override public void onViewDetachedFromWindow(View view) {
            try { unregister(view); }
            catch (ReflectiveOperationException failure) {
                throw new IllegalStateException("Native App tint detachment failed", failure);
            }
        }
    };

    static AppBarClockBatteryBinding create(ClassLoader loader, View root, Object dark)
            throws ReflectiveOperationException {
        requireMain();
        AppBarClockBatteryBinding result = new AppBarClockBatteryBinding(loader, root, dark);
        try {
            // MiuiClock's own optional tint subscription uses the shared clock
            // controller's HOME dispatcher. Leave its native time registration
            // intact and route tint like PhoneStatusBarViewController does.
            write(result.clock, "mShowDark", false);
            result.clock.addOnAttachStateChangeListener(result.listener);
            result.battery.addOnAttachStateChangeListener(result.listener);
            return result;
        } catch (ReflectiveOperationException | RuntimeException | Error failure) {
            AppBarContentPreparation.cleanupCreation(result, failure);
            throw failure;
        }
    }

    private AppBarClockBatteryBinding(ClassLoader loader, View root, Object dark)
            throws ReflectiveOperationException {
        this.dark = dark;
        clock = child(root, "clock");
        battery = child(root, "battery");
        if (clock.isAttachedToWindow() || battery.isAttachedToWindow())
            throw new IllegalStateException("App clock/battery already attached");
        if (!clock.getClass().getName().equals("com.android.systemui.statusbar.views.MiuiClock")
                || !battery.getClass().getName().equals("com.android.systemui.statusbar.views.MiuiBatteryMeterView"))
            throw new IllegalStateException("Unknown native App clock/battery shape");
        originalClockShowDark = (Boolean) read(clock, "mShowDark");
        Class<?> receiver = Class.forName("com.android.systemui.plugins.DarkIconDispatcher$DarkReceiver", false, loader);
        if (!receiver.isInstance(clock) || !receiver.isInstance(battery))
            throw new IllegalStateException("Missing native clock/battery tint contract");
        add = dark.getClass().getMethod("addDarkReceiver", receiver);
        remove = dark.getClass().getMethod("removeDarkReceiver", receiver);
    }

    private void register(View view) throws ReflectiveOperationException {
        requireMain();
        if (view == clock ? clockRegistered : batteryRegistered) return;
        // Retain ownership even if native immediate onDarkChanged throws.
        if (view == clock) clockRegistered = true; else batteryRegistered = true;
        add.invoke(dark, view);
    }

    private void unregister(View view) throws ReflectiveOperationException {
        requireMain();
        if (!(view == clock ? clockRegistered : batteryRegistered)) return;
        remove.invoke(dark, view);
        if (view == clock) clockRegistered = false; else batteryRegistered = false;
    }

    void verifyTintReceivers() throws ReflectiveOperationException {
        requireMain();
        if (closed || clock.isAttachedToWindow() || battery.isAttachedToWindow())
            throw new IllegalStateException("Tint verification requires detached App views");
        try {
            register(clock);
            register(battery);
        } finally {
            try { unregister(battery); } finally { unregister(clock); }
        }
    }

    @Override public void close() throws ReflectiveOperationException {
        requireMain();
        // Restoring mShowDark while attached would make native detach remove a
        // receiver from the wrong dispatcher. The owner must detach first.
        if (clock.isAttachedToWindow() || battery.isAttachedToWindow())
            throw new IllegalStateException("Detach App clock/battery before disposal");
        closed = true;
        try { unregister(battery); } finally { unregister(clock); }
        clock.removeOnAttachStateChangeListener(listener);
        battery.removeOnAttachStateChangeListener(listener);
        write(clock, "mShowDark", originalClockShowDark);
    }

    void delegateToNativeController() throws ReflectiveOperationException {
        requireMain();
        if (clock.isAttachedToWindow() || battery.isAttachedToWindow())
            throw new IllegalStateException("Native tint ownership must be selected before attachment");
        clock.removeOnAttachStateChangeListener(listener);
        battery.removeOnAttachStateChangeListener(listener);
        // Keep mShowDark disabled: the native PhoneStatusBarViewController
        // registers these Views on our private dispatcher, while MiuiClock's
        // own optional registration would use the global Home dispatcher.
    }

    private static View child(View root, String name) {
        int id = root.getResources().getIdentifier(name, "id", "com.android.systemui");
        if (id == 0) throw new IllegalStateException("Missing App clock/battery resource " + name);
        return root.requireViewById(id);
    }
    private static void requireMain() {
        if (Looper.myLooper() != Looper.getMainLooper()) throw new IllegalStateException("Tint requires main Looper");
    }
}
