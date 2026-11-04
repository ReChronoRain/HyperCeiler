/*
 * Derived from MiuiBackGestureHook status-bar-follow implementation.
 * Source: https://github.com/wxxsfxyzm/MiuiBackGestureHook
 * Reference commit: 004bc059e5821775917b771d89aa1d8c18a1e669 (includes b6e582f and later fixes)
 * Copyright (C) 2026 MiuiBackGestureHook contributors.
 * Licensed under the Apache License, Version 2.0; modified for HyperCeiler.
 */
package com.sevtinge.hyperceiler.libhook.rules.systemframework.statusbarfollow;

import android.os.Handler;
import android.os.Looper;
import android.view.SurfaceControl;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.Executor;

/** Exact native bar container restoration, callable only after callback retirement. */
final class ServerBarSurfaceRestorer implements ServerBarFollowLifecycle.Restorer {
    private final Object controller;
    private final Object data;
    private final Object homeToken;
    private final Object appToken;
    private final SurfaceControl home;
    private final SurfaceControl app;
    private final SurfaceControl homeParent;
    private final SurfaceControl appParent;
    private final Handler handler;
    private final Object displayContent;
    private final Object globalLock;
    private final Method stop;
    private final Method reset;
    private final Method reassignAppLayer;
    private final Field reparented;
    private final Field appSurface;
    private final Field associatedTask;
    private final Field appTaskId;
    private final Field lastHomeVisible;
    private final Field lastAppVisible;
    private final SurfaceControl.Transaction nativeTransaction;

    ServerBarSurfaceRestorer(Object controller) throws ReflectiveOperationException {
        this.controller = controller;
        if (!controller.getClass().getName().equals("com.android.server.wm.MiuiStatusBarController"))
            throw new IllegalStateException("Unknown native bar controller");
        handler = (Handler) read(controller, "mHandler");
        requireOwner();
        displayContent = read(controller, "mDisplayContent");
        globalLock = read(read(displayContent, "mWmService"), "mGlobalLock");
        if (globalLock == null) throw new IllegalStateException("Native WMS lock unavailable");
        synchronized (globalLock) {
        data = read(controller, "mTaskBarData");
        nativeTransaction = (SurfaceControl.Transaction) read(controller, "mStatusBarT");
        if (nativeTransaction == null) throw new IllegalStateException("Native bar transaction unavailable");
        homeToken = read(data, "mHomelBarContainer");
        appToken = read(data, "mAppBarContainer");
        home = surface("mHomeBarContainerSurface");
        app = surface("mAppBarContainerSurface");
        homeParent = surface("mHomeBarBirthParent");
        appParent = surface("mAppBarBirthParent");
        if (homeToken == null || appToken == null || homeToken == appToken || home == app)
            throw new IllegalStateException("Native bar container identities unavailable");
        stop = method(controller.getClass(), "stopFolmeAnimate", float.class, float.class);
        reset = method(controller.getClass(), "resetToInitState");
        reassignAppLayer = method(appToken.getClass(), "reassignLayer", SurfaceControl.Transaction.class);
        reparented = field(controller.getClass(), "mReparentToOriginParent");
        appSurface = field(controller.getClass(), "mAppSurface");
        associatedTask = field(controller.getClass(), "mAssociatedTask");
        appTaskId = field(controller.getClass(), "mAppTaskId");
        lastHomeVisible = field(controller.getClass(), "mLastHomeBarVisible");
        lastAppVisible = field(controller.getClass(), "mLastAppBarVisible");
        verifyIdentity();
        }
    }

    Executor executor() {
        return task -> {
            if (!handler.post(task)) throw new IllegalStateException("Native bar Looper rejected restoration");
        };
    }

    @Override public void restore(Runnable committed) throws ReflectiveOperationException {
        restore(false, false, committed);
    }

    void restore(boolean homeRemoved, boolean appRemoved, Runnable committed) throws ReflectiveOperationException {
        requireOwner();
        // WindowToken layer assignment and container identity belong to WMS.
        // Never wait for callbacks or compositor completion while holding it.
        synchronized (globalLock) {
            restoreLocked(homeRemoved, appRemoved, committed);
        }
    }

    private void restoreLocked(boolean homeRemoved, boolean appRemoved, Runnable committed) throws ReflectiveOperationException {
        verifyIdentity(homeRemoved, appRemoved);
        // The lifecycle gate must already deny old listener updates. Native
        // setTo may synchronously dispatch those callbacks itself.
        stop.invoke(controller, 1f, 0f);
        verifyIdentity(homeRemoved, appRemoved);
        // Restate the exact native birth parents in one final transaction.
        // Native appReparent chooses visibility from tasks; retirement needs
        // Home-only rendering before SystemUI removes the App window.
        try (SurfaceControl.Transaction transaction = new SurfaceControl.Transaction()) {
            // Drain pending native reparent/alpha operations into this same final
            // transaction, then overwrite their bar pose. No old apply may win later.
            transaction.merge(nativeTransaction);
            if (!homeRemoved)
                transaction.reparent(home, homeParent).setLayer(home, Integer.MAX_VALUE).setAlpha(home, 1f).setVisibility(home, true);
            if (!appRemoved) {
                transaction.reparent(app, appParent).setAlpha(app, 0f).setVisibility(app, true);
                reassignAppLayer.invoke(appToken, transaction);
            }
            transaction.addTransactionCommittedListener(executor(), committed::run);
            transaction.apply();
        }
        // Source port of the native reparent methods' bookkeeping. Calling
        // both methods here would submit separate intermediate alpha states.
        // Preserve controller objects and visibility policy; only retire its
        // old App association and describe the submitted Home-only pose.
        reparented.setBoolean(controller, true);
        appSurface.set(controller, null);
        associatedTask.set(controller, null);
        appTaskId.setInt(controller, 0);
        lastHomeVisible.setBoolean(controller, true);
        lastAppVisible.setBoolean(controller, false);
        reset.invoke(controller);
    }

    private void verifyIdentity() throws ReflectiveOperationException {
        verifyIdentity(false, false);
    }

    private void verifyIdentity(boolean homeRemoved, boolean appRemoved) throws ReflectiveOperationException {
        if (!Thread.holdsLock(globalLock)) throw new IllegalStateException("Native bar identity requires WMS lock");
        if (read(controller, "mDisplayContent") != displayContent
                || read(read(displayContent, "mWmService"), "mGlobalLock") != globalLock
                || read(controller, "mStatusBarT") != nativeTransaction
                || read(controller, "mTaskBarData") != data || read(data, "mHomelBarContainer") != homeToken
                || read(data, "mAppBarContainer") != appToken
                || read(data, "mHomeBarContainerSurface") != home || read(data, "mAppBarContainerSurface") != app
                || read(data, "mHomeBarBirthParent") != homeParent || read(data, "mAppBarBirthParent") != appParent)
            throw new IllegalStateException("Native bar surface owner replaced before retirement");
        if ((!homeRemoved && (!home.isValid() || !homeParent.isValid()))
                || (!appRemoved && (!app.isValid() || !appParent.isValid())))
            throw new IllegalStateException("Native bar surface invalid before retirement");
    }

    private SurfaceControl surface(String name) throws ReflectiveOperationException {
        Object value = read(data, name);
        if (!(value instanceof SurfaceControl) || !((SurfaceControl) value).isValid())
            throw new IllegalStateException("Missing native bar surface " + name);
        return (SurfaceControl) value;
    }

    private void requireOwner() {
        if (handler == null || Looper.myLooper() != handler.getLooper())
            throw new IllegalStateException("Native bar restoration requires its owner Looper");
    }

    private static Object read(Object owner, String name) throws ReflectiveOperationException {
        if (owner == null) throw new IllegalStateException("Missing native owner for " + name);
        return field(owner.getClass(), name).get(owner);
    }

    private static Field field(Class<?> type, String name) throws NoSuchFieldException {
        for (; type != null; type = type.getSuperclass()) {
            try { Field field = type.getDeclaredField(name); field.setAccessible(true); return field; }
            catch (NoSuchFieldException absent) { }
        }
        throw new NoSuchFieldException(name);
    }

    private static Method method(Class<?> type, String name, Class<?>... parameters) throws NoSuchMethodException {
        for (; type != null; type = type.getSuperclass()) {
            try { Method method = type.getDeclaredMethod(name, parameters); method.setAccessible(true); return method; }
            catch (NoSuchMethodException absent) { }
        }
        throw new NoSuchMethodException(name);
    }
}
