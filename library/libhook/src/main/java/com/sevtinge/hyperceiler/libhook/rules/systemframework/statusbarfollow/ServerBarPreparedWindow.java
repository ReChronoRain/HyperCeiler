/*
 * Derived from MiuiBackGestureHook status-bar-follow implementation.
 * Source: https://github.com/wxxsfxyzm/MiuiBackGestureHook
 * Reference commit: 004bc059e5821775917b771d89aa1d8c18a1e669 (includes b6e582f and later fixes)
 * Copyright (C) 2026 MiuiBackGestureHook contributors.
 * Licensed under the Apache License, Version 2.0; modified for HyperCeiler.
 */
package com.sevtinge.hyperceiler.libhook.rules.systemframework.statusbarfollow;

import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.view.SurfaceControl;
import java.util.concurrent.Executor;

/** Prepared-window lease: no animation ownership until its explicit activation stage. */
final class ServerBarPreparedWindow implements ServerBarFollowLifecycle.Restorer {
    final Object controller;
    final Object appWindow;
    final Object homeWindow;
    final Object appToken;
    final Object homeToken;
    final SurfaceControl appSurface;
    final SurfaceControl homeSurface;
    final Object globalLock;
    final Handler owner;
    final IBinder client;
    final String generation;
    final int uid;
    private final ServerBarFollowLifecycle lifecycle;
    private final Runnable acknowledge;
    private final IBinder.DeathRecipient died = () -> ownerPostRetire();
    private boolean linked;
    boolean begun;
    private ServerBarSurfaceRestorer nativeRestorer;
    private boolean retiring;
    volatile boolean enabled;
    final java.util.concurrent.atomic.AtomicInteger frames = new java.util.concurrent.atomic.AtomicInteger();
    private Object policy;
    private int originalVisibilityPolicy;

    ServerBarPreparedWindow(Object controller, Object appWindow, Object homeWindow, Object globalLock,
            Handler owner, IBinder client, String generation, int uid,
            ServerBarFollowLifecycle lifecycle, Runnable acknowledge) throws ReflectiveOperationException {
        this.controller = controller;
        this.appWindow = appWindow;
        this.homeWindow = homeWindow;
        this.globalLock = globalLock;
        this.owner = owner;
        this.client = client;
        this.generation = generation;
        this.uid = uid;
        this.lifecycle = lifecycle;
        this.acknowledge = acknowledge;
        requireOwner();
        if (!Thread.holdsLock(globalLock)) throw new IllegalStateException("Prepared bar capture requires WMS lock");
        appToken = ServerBarWindowBridge.read(appWindow, "mToken");
        homeToken = ServerBarWindowBridge.read(homeWindow, "mToken");
        appSurface = (SurfaceControl) ServerBarWindowBridge.call(appToken, "getSurfaceControl");
        homeSurface = (SurfaceControl) ServerBarWindowBridge.call(homeToken, "getSurfaceControl");
        verify();
    }

    Executor executor() { return task -> {
        if (!owner.post(task)) throw new IllegalStateException("Native bar owner rejected lease work");
    }; }

    void begin() throws RemoteException {
        requireOwner();
        client.linkToDeath(died, 0);
        linked = true;
        try { lifecycle.begin(controller, executor(), this); begun = true; }
        catch (RuntimeException failure) { unlink(); throw failure; }
    }

    void retire() { requireOwner(); enabled = false; retiring = true; if (begun) lifecycle.retire(controller); }
    void verifyCurrent() throws ReflectiveOperationException { synchronized (globalLock) { verify(); } }

    void arm(Runnable committed) throws ReflectiveOperationException {
        requireOwner();
        if (!begun || retiring || nativeRestorer != null) throw new IllegalStateException("Bar lease cannot arm");
        synchronized (globalLock) {
            verify();
            // Native first-transition initialization: fill its own WindowToken
            // cache, then capture immutable restoration identities before enable.
            ServerBarWindowBridge.call(controller, "updateStatusBar");
            nativeRestorer = new ServerBarSurfaceRestorer(controller);
            policy = ServerBarWindowBridge.call(ServerBarWindowBridge.read(controller, "mDisplayContent"), "getDisplayPolicy");
            originalVisibilityPolicy = ((Number) ServerBarWindowBridge.read(controller, "mStatusBarVisiblePolicy")).intValue();
            try (SurfaceControl.Transaction transaction = new SurfaceControl.Transaction()) {
                transaction.setAlpha(appSurface, 0f);
                transaction.addTransactionCommittedListener(executor(), () -> {
                    if (!retiring) committed.run();
                });
                transaction.apply();
            }
        }
    }

    void activate() throws ReflectiveOperationException {
        requireOwner();
        if (!begun || retiring || enabled || nativeRestorer == null) throw new IllegalStateException("Bar lease not armed");
        synchronized (globalLock) {
            verify();
            if ((Boolean) ServerBarWindowBridge.call(controller, "isInSplitScreen"))
                throw new IllegalStateException("Cannot activate during split screen");
            java.lang.reflect.Method visibility = controller.getClass().getDeclaredMethod("enableStatusBarVisibility", int.class);
            visibility.setAccessible(true);
            visibility.invoke(controller, 1);
            enabled = true;
            // The transparent preparation ran native hideStatusBarIfNeed while
            // multi-status was disabled. Type 2041 is excluded from keyguard's
            // normal show pass, so explicitly re-enter WindowState's stock show
            // policy once. It still checks Xiaomi visibility, user and app-op gates.
            java.lang.reflect.Method show = appWindow.getClass().getDeclaredMethod("show", boolean.class, boolean.class);
            show.setAccessible(true);
            show.invoke(appWindow, false, true);
            // Native status-ID comparison in updateSystemBarAttributes forces
            // the new App raw-ID packet when ownership changes; do not forge regions.
            ServerBarWindowBridge.call(policy, "updateSystemBarAttributes");
        }
    }
    private void ownerPostRetire() { owner.post(this::retire); }

    @Override public void restore(Runnable committed) throws Exception {
        requireOwner();
        if (nativeRestorer != null) {
            synchronized (globalLock) {
                boolean homeRemoved = (Boolean) ServerBarWindowBridge.read(homeWindow, "mRemoved");
                boolean appRemoved = (Boolean) ServerBarWindowBridge.read(appWindow, "mRemoved");
                // WMS may finish WindowState removal before Binder death delivery.
                // Only skip that exact removed window, never adopt its replacement.
                if ((!homeRemoved && ServerBarWindowBridge.call(controller, "getHomeTaskBar") != homeWindow)
                        || (!appRemoved && ServerBarWindowBridge.call(controller, "getAppTaskBar") != appWindow))
                    throw new IllegalStateException("Live native bar WindowState replaced");
                java.lang.reflect.Field visibility = controller.getClass().getDeclaredField("mStatusBarVisiblePolicy");
                visibility.setAccessible(true);
                visibility.setInt(controller, originalVisibilityPolicy);
                ServerBarWindowBridge.call(policy, "updateSystemBarAttributes");
                nativeRestorer.restore(homeRemoved, appRemoved, () -> finishCommitted(committed));
            }
            return;
        }
        synchronized (globalLock) {
            // Preparation never touched Home or enabled native animations.
            // If the app process died, WMS may already have removed its surface;
            // there is then no prepared surface left to retire.
            if (!client.isBinderAlive() || (Boolean) ServerBarWindowBridge.read(appWindow, "mRemoved")) {
                finishCommitted(committed);
                return;
            }
            verify();
            try (SurfaceControl.Transaction transaction = new SurfaceControl.Transaction()) {
                transaction.setAlpha(appSurface, 0f);
                transaction.addTransactionCommittedListener(executor(), () -> finishCommitted(committed));
                transaction.apply();
            }
        }
    }

    private void finishCommitted(Runnable committed) {
        committed.run();
        // If completion is synchronous, lifecycle's successful restore return
        // still needs to happen first. This is an ordered owner turn, not polling.
        owner.post(() -> {
            if (!lifecycle.isRetired(controller)) return;
            unlink();
            acknowledge.run();
        });
    }

    private void unlink() { if (linked) { client.unlinkToDeath(died, 0); linked = false; } }
    private void requireOwner() {
        if (owner == null || Looper.myLooper() != owner.getLooper()) throw new IllegalStateException("Wrong bar owner");
    }
    private void verify() throws ReflectiveOperationException {
        if (ServerBarWindowBridge.call(controller, "getAppTaskBar") != appWindow
                || ServerBarWindowBridge.call(controller, "getHomeTaskBar") != homeWindow
                || ServerBarWindowBridge.read(appWindow, "mToken") != appToken
                || ServerBarWindowBridge.read(homeWindow, "mToken") != homeToken
                || ServerBarWindowBridge.call(appToken, "getSurfaceControl") != appSurface
                || ServerBarWindowBridge.call(homeToken, "getSurfaceControl") != homeSurface
                || appSurface == null || homeSurface == null || appSurface == homeSurface
                || !appSurface.isValid() || !homeSurface.isValid())
            throw new IllegalStateException("Prepared bar token/surface identity changed");
    }
}
