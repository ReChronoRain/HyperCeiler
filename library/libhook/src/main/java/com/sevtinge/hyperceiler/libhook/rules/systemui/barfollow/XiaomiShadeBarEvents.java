/*
 * Derived from MiuiBackGestureHook status-bar-follow implementation.
 * Source: https://github.com/wxxsfxyzm/MiuiBackGestureHook
 * Reference commit: 004bc059e5821775917b771d89aa1d8c18a1e669 (includes b6e582f and later fixes)
 * Copyright (C) 2026 MiuiBackGestureHook contributors.
 * Licensed under the Apache License, Version 2.0; modified for HyperCeiler.
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow;

import android.content.ComponentName;
import android.os.Handler;
import android.os.Looper;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** A16 CommandQueue event source and task selection, independent of gesture progress. */
final class XiaomiShadeBarEvents implements AutoCloseable {
    interface Sink { void selected(int role); void failed(Throwable failure); }
    private final Object commandQueue;
    private final Object activityTasks;
    private final Method focused;
    private final Method roots;
    private final Sink sink;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> new Thread(r, "AppBarTaskSelection"));
    private final XiaomiShadeBarSelection selection = new XiaomiShadeBarSelection();
    private final Object lock = new Object();
    private boolean dirty;
    private boolean draining;
    private boolean closed;
    private volatile int taskRole = XiaomiShadeBarSelection.HOME;

    static XiaomiShadeBarEvents create(ClassLoader loader, Sink sink) throws ReflectiveOperationException {
        Object dependency = Class.forName("com.android.systemui.Dependency", false, loader)
                .getField("sDependency").get(null);
        Object navigation = AppBarContentPreparation.call(AppBarContentPreparation.read(dependency, "mNavigationBarController"), "get");
        Object component = AppBarContentPreparation.read(AppBarContentPreparation.read(navigation,
                "mNavigationBarComponentFactory"), "referenceSysUIComponentImpl");
        Object queue = AppBarContentPreparation.call(AppBarContentPreparation.read(component,
                "provideCommandQueueProvider"), "get");
        if (!queue.getClass().getName().equals("com.android.systemui.statusbar.CommandQueue"))
            throw new IllegalStateException("Unexpected native CommandQueue");
        Class<?> contract = Class.forName("android.app.IActivityTaskManager", false, loader);
        Object tasks = Class.forName("android.app.ActivityTaskManager", false, loader)
                .getMethod("getService").invoke(null);
        return new XiaomiShadeBarEvents(queue, tasks, contract.getMethod("getFocusedRootTaskInfo"),
                contract.getMethod("getAllRootTaskInfosOnDisplay", int.class), sink);
    }

    private XiaomiShadeBarEvents(Object queue, Object tasks, Method focused, Method roots, Sink sink) {
        this.commandQueue = queue;
        this.activityTasks = tasks;
        this.focused = focused;
        this.roots = roots;
        this.sink = sink;
    }

    void event(Object queue, int displayId) {
        if (queue != commandQueue || displayId != 0) return;
        synchronized (lock) {
            if (closed) return;
            dirty = true;
            if (draining) return;
            draining = true;
            worker.execute(this::drain);
        }
    }

    private void drain() {
        for (;;) {
            synchronized (lock) {
                if (closed || !dirty) { draining = false; return; }
                dirty = false;
            }
            try {
                XiaomiShadeBarSelection.Root focus = snapshot(focused.invoke(activityTasks));
                List<XiaomiShadeBarSelection.Root> ordered = null;
                // Exactly the native fallback: only query ordered roots when
                // focus is on another display, freeform or transparent/ineligible.
                if (focus == null || focus.displayId != 0 || !XiaomiShadeBarSelection.eligible(focus)) {
                    Object result = roots.invoke(activityTasks, 0);
                    if (result != null) {
                        ordered = new ArrayList<>();
                        for (Object root : (List<?>) result) ordered.add(snapshot(root));
                    }
                }
                if (selection.onAppTransition(0, focus, ordered)) {
                    int role = selection.current(false, true);
                    synchronized (lock) {
                        if (closed) return;
                        main.post(() -> {
                            synchronized (lock) {
                                if (closed) return;
                                taskRole = role;
                                sink.selected(role);
                            }
                        });
                    }
                }
            } catch (ReflectiveOperationException | RuntimeException failure) {
                synchronized (lock) {
                    if (closed) return;
                    main.post(() -> { synchronized (lock) { if (!closed) sink.failed(failure); } });
                }
            }
        }
    }

    // Task role alone is not the showing token. A16 combines it with actual
    // App-token availability and flip-fold state before publishing targets.
    int current(boolean flipFolded, boolean appTokenAvailable) {
        return flipFolded || !appTokenAvailable ? XiaomiShadeBarSelection.HOME : taskRole;
    }

    private static XiaomiShadeBarSelection.Root snapshot(Object info) throws ReflectiveOperationException {
        if (info == null) return null;
        if (!info.getClass().getName().equals("android.app.ActivityTaskManager$RootTaskInfo"))
            throw new IllegalStateException("Unexpected root task shape");
        Class<?> type = info.getClass();
        ComponentName top = (ComponentName) type.getField("topActivity").get(info);
        return new XiaomiShadeBarSelection.Root(type.getField("displayId").getInt(info),
                (Integer) type.getMethod("getWindowingMode").invoke(info),
                (Integer) type.getMethod("getActivityType").invoke(info),
                type.getField("isTopActivityTransparent").getBoolean(info),
                top == null ? null : top.getClassName());
    }

    @Override public void close() {
        synchronized (lock) {
            closed = true;
            dirty = false;
            taskRole = XiaomiShadeBarSelection.HOME;
            main.removeCallbacksAndMessages(null);
            // Do not interrupt an in-flight Binder query. Its result is fenced
            // by closed before publication; shutdown prevents another task.
            worker.shutdown();
        }
    }
}
