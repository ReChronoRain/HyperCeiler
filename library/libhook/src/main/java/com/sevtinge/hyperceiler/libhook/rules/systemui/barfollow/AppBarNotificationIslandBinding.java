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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow.AppBarContentPreparation.call;
import static com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow.AppBarContentPreparation.read;
import static com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow.AppBarContentPreparation.write;

/** Native notification island geometry/animation with an explicitly owned attach listener. */
final class AppBarNotificationIslandBinding implements AutoCloseable {
    private static final String MONITOR = "com.android.systemui.statusbar.IslandMonitor$NotificationContainerIslandMonitor";
    private final View container;
    private final Object inject;
    private final Object island;
    private final Object animator;
    private final Object monitor;
    private final Object listener;
    private final Method add;
    private final Method remove;
    private final boolean originalFeature;
    private final View.OnAttachStateChangeListener attachment;
    private boolean registered;
    private boolean closing;

    static AppBarNotificationIslandBinding create(ClassLoader loader, View container,
            Object island, Object animator) throws ReflectiveOperationException {
        requireMain();
        AppBarNotificationIslandBinding result = new AppBarNotificationIslandBinding(loader, container, island, animator);
        try {
            write(result.inject, "_islandMonitor", result.monitor);
            write(result.inject, "animationController", animator);
            write(result.inject, "newAnimationFeature", true);
            container.addOnAttachStateChangeListener(result.attachment);
            return result;
        } catch (ReflectiveOperationException | RuntimeException | Error failure) {
            AppBarContentPreparation.cleanupCreation(result, failure);
            throw failure;
        }
    }

    private AppBarNotificationIslandBinding(ClassLoader loader, View container,
            Object island, Object animator) throws ReflectiveOperationException {
        this.container = container;
        this.island = island;
        this.animator = animator;
        inject = call(container, "getInject");
        if (container.isAttachedToWindow() || read(inject, "target") != container
                || read(inject, "_islandMonitor") != null || read(inject, "animationController") != null)
            throw new IllegalStateException("App notification island already owned");
        originalFeature = (Boolean) read(inject, "newAnimationFeature");
        Class<?> listenerType = Class.forName("com.android.systemui.statusbar.OnIslandStatusChangedListener", false, loader);
        add = island.getClass().getMethod("addOnIslandStatusChangedListener", listenerType);
        remove = island.getClass().getMethod("removeOnIslandStatusChangedListener", listenerType);
        Class<?> monitorType = Class.forName(MONITOR, false, loader);
        // This exact native data holder has only allocation + field stores in
        // HomeStatusBarViewBinderImpl. R8 may remove its trivial constructor.
        if (monitorType.getDeclaredConstructors().length == 0) {
            Class<?> unsafeType = Class.forName("sun.misc.Unsafe");
            Field singleton = unsafeType.getDeclaredField("theUnsafe");
            singleton.setAccessible(true);
            monitor = unsafeType.getMethod("allocateInstance", Class.class).invoke(singleton.get(null), monitorType);
        } else {
            Constructor<?> constructor = monitorType.getDeclaredConstructor();
            constructor.setAccessible(true);
            monitor = constructor.newInstance();
        }
        write(monitor, "container", container);
        write(monitor, "islandController", island);
        Class<?> nativeListener = Class.forName(
                "com.android.systemui.statusbar.IslandMonitor$RealContainerIslandMonitor$stateListener$1", false, loader);
        Constructor<?> constructor = nativeListener.getDeclaredConstructor(int.class);
        constructor.setAccessible(true);
        listener = constructor.newInstance(1); // Native notification branch.
        write(listener, "this$0", monitor);
        write(monitor, "stateListener", listener);
        Class<?> function = Class.forName("kotlin.jvm.functions.Function0", false, loader);
        Object arrayFactory = Proxy.newProxyInstance(loader, new Class<?>[]{function}, (proxy, method, args) -> {
            if (method.getName().equals("invoke")) return new int[2];
            if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
            if (method.getName().equals("equals")) return proxy == args[0];
            return "AppNotificationIslandLocation";
        });
        Method lazy = Class.forName("kotlin.LazyKt__LazyJVMKt", false, loader).getMethod("lazy", function);
        lazy.setAccessible(true);
        write(monitor, "locationArray$delegate", lazy.invoke(null, arrayFactory));
        attachment = new View.OnAttachStateChangeListener() {
            public void onViewAttachedToWindow(View view) { changeAttachment(true); }
            public void onViewDetachedFromWindow(View view) { changeAttachment(false); }
        };
        // Do not call monitor.init(): it discards the attach-listener handle.
        // Our listener invokes exactly the same native add/remove operations.
    }

    private void changeAttachment(boolean attached) {
        requireMain();
        try {
            if (attached && !closing && !registered) {
                registered = true;
                add.invoke(island, listener);
            } else if (!attached && registered) {
                remove.invoke(island, listener);
                registered = false;
            }
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("App notification island lifecycle failed", failure);
        }
    }

    @Override public void close() throws ReflectiveOperationException {
        requireMain();
        closing = true;
        container.removeOnAttachStateChangeListener(attachment);
        if (registered) { remove.invoke(island, listener); registered = false; }
        if (read(inject, "_islandMonitor") == monitor) write(inject, "_islandMonitor", null);
        if (read(inject, "animationController") == animator) {
            write(inject, "animationController", null);
            write(inject, "newAnimationFeature", originalFeature);
        }
    }

    private static void requireMain() {
        if (Looper.myLooper() != Looper.getMainLooper()) throw new IllegalStateException("Notification island requires main Looper");
    }
}
