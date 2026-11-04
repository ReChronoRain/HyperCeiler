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

import static com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow.AppBarContentPreparation.call;
import static com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow.AppBarContentPreparation.read;
import static com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow.AppBarContentPreparation.write;

/** A16 ShadeStatusBarViewInteractor target publication, using A17 controllers. */
final class AppBarShadeTargets implements AutoCloseable {
    private final Object primaryBinder;
    private final Object primaryInjector;
    private final View home;
    private final View app;
    private final Object clockController;
    private final Object controlCenter;
    private final Object[] homeTargets;
    private final Object[] appTargets;
    private Object[] published;
    private boolean closed;

    AppBarShadeTargets(Object primaryBinder, View app) throws ReflectiveOperationException {
        requireMain();
        this.primaryBinder = primaryBinder;
        primaryInjector = read(primaryBinder, "mInjector");
        home = (View) read(primaryInjector, "rootView");
        this.app = app;
        if (home == app || home.getClass() != app.getClass())
            throw new IllegalStateException("App/Home target roots are not independent native Views");
        Object dependency = read(home, "mDependence");
        clockController = read(dependency, "clockController");
        controlCenter = read(dependency, "ccFakeViewController");
        controlCenter.getClass().getMethod("adjustRealSystemIcons");
        controlCenter.getClass().getMethod("adjustRealPromptIcon");
        homeTargets = targets(home);
        appTargets = targets(app);
        for (int i = 0; i < homeTargets.length; i++) {
            if (homeTargets[i] == appTargets[i] || homeTargets[i].getClass() != appTargets[i].getClass())
                throw new IllegalStateException("App/Home target child contract mismatch");
        }
        published = current();
        if (!same(published, homeTargets))
            throw new IllegalStateException("Primary View is not the current native shade target");
    }

    /** App-token readiness must be proven by its window/content owner. */
    void select(int role, boolean appTokenReady) throws ReflectiveOperationException {
        requireMain();
        if (closed) throw new IllegalStateException("Shade target owner closed");
        if (role != XiaomiShadeBarSelection.HOME && role != XiaomiShadeBarSelection.APP)
            throw new IllegalArgumentException("Unknown bar role");
        if (!currentPrimary()) throw new IllegalStateException("Primary status-bar View was replaced");
        Object[] next = homeTargets;
        if (role == XiaomiShadeBarSelection.APP && appTokenReady) {
            if (!app.isAttachedToWindow() || !(Boolean) read(app, "mInited")
                    || read(app, "mDependence") == null)
                throw new IllegalStateException("App token has no attached bound View");
            next = appTargets;
        }
        if (!same(current(), published))
            throw new IllegalStateException("Shade targets replaced outside the current bar owner");
        Object[] before = published;
        try {
            publish(next);
            published = next;
        } catch (ReflectiveOperationException | RuntimeException failure) {
            // Native adjustRealSystemIcons may fail after moving its listener.
            // Restore through the same native methods, not by writing real*.
            try {
                publish(before);
                call(controlCenter, "adjustRealSystemIcons");
                call(controlCenter, "adjustRealPromptIcon");
            }
            catch (Throwable cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }

    private void publish(Object[] next) throws ReflectiveOperationException {
        if (read(clockController, "statusBarClock") != next[0])
            write(clockController, "statusBarClock", next[0]);
        if (read(controlCenter, "statusBarSystemIcons") != next[1]) {
            write(controlCenter, "statusBarSystemIcons", next[1]);
            call(controlCenter, "adjustRealSystemIcons");
        }
        if (read(controlCenter, "statusBarPromptIcon") != next[2]) {
            write(controlCenter, "statusBarPromptIcon", next[2]);
            call(controlCenter, "adjustRealPromptIcon");
        }
    }

    void verifyHome() throws ReflectiveOperationException {
        requireMain();
        if (!currentPrimary() || !same(current(), homeTargets))
            throw new IllegalStateException("Main clock/control-center target changed");
    }

    @Override public void close() throws ReflectiveOperationException {
        requireMain();
        if (closed) return;
        Object[] actual = current();
        boolean ownsTarget = false;
        for (int i = 0; i < actual.length; i++) ownsTarget |= actual[i] == appTargets[i];
        if (!ownsTarget) { closed = true; return; }
        // A main-bar rebind can already have published replacement targets.
        // Never restore stale Home Views over those replacement owners.
        View fallback = currentPrimary() ? home : currentHome();
        Object[] fallbackTargets = targets(fallback);
        for (int i = 0; i < actual.length; i++)
            if (actual[i] == appTargets[i]) actual[i] = fallbackTargets[i];
        publish(actual);
        closed = true;
    }

    private boolean currentPrimary() throws ReflectiveOperationException {
        return read(primaryBinder, "mInjector") == primaryInjector
                && read(primaryInjector, "rootView") == home;
    }

    private View currentHome() throws ReflectiveOperationException {
        Object injector = read(primaryBinder, "mInjector");
        if (injector == null) throw new IllegalStateException("No replacement Home View for App target release");
        View view = (View) read(injector, "rootView");
        Object dependency = read(view, "mDependence");
        if (view == app || dependency == null || read(dependency, "clockController") != clockController
                || read(dependency, "ccFakeViewController") != controlCenter)
            throw new IllegalStateException("Replacement Home controller identity is uncertain");
        return view;
    }

    private Object[] current() throws ReflectiveOperationException {
        return new Object[]{read(clockController, "statusBarClock"),
                read(controlCenter, "statusBarSystemIcons"), read(controlCenter, "statusBarPromptIcon")};
    }

    private static Object[] targets(View view) throws ReflectiveOperationException {
        int clockId = view.getResources().getIdentifier("clock", "id", "com.android.systemui");
        if (clockId == 0) throw new IllegalStateException("Native clock resource unavailable");
        Object icons = read(view, "mStatusBatteryContainer");
        Object prompt = read(view, "mPrivacyContainer");
        if (icons == null || prompt == null) throw new IllegalStateException("Native shade containers unavailable");
        return new Object[]{view.requireViewById(clockId), icons, prompt};
    }

    private static boolean same(Object[] a, Object[] b) {
        for (int i = 0; i < a.length; i++) if (a[i] != b[i]) return false;
        return true;
    }
    private static void requireMain() {
        if (Looper.myLooper() != Looper.getMainLooper()) throw new IllegalStateException("Shade targets require main Looper");
    }
}
