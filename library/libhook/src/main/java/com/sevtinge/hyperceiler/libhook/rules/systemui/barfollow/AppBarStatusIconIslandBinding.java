/*
 * Derived from MiuiBackGestureHook status-bar-follow implementation.
 * Source: https://github.com/wxxsfxyzm/MiuiBackGestureHook
 * Reference commit: 004bc059e5821775917b771d89aa1d8c18a1e669 (includes b6e582f and later fixes)
 * Copyright (C) 2026 MiuiBackGestureHook contributors.
 * Licensed under the Apache License, Version 2.0; modified for HyperCeiler.
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow;

import android.content.Context;
import android.graphics.Rect;
import android.os.Looper;
import android.view.View;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow.AppBarContentPreparation.call;
import static com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow.AppBarContentPreparation.read;
import static com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow.AppBarContentPreparation.write;

/** Native real-container geometry; Home-only position providers are a separate contract. */
final class AppBarStatusIconIslandBinding implements AutoCloseable {
    private final View container;
    private final Object island;
    private final Object state;
    private final Object animator;
    private final Class<?> listenerType;
    private final Method add;
    private final Method remove;
    private final Method location;
    private final Method tinyScreen;
    private final Method setSpace;
    private final Object space;
    private final Object monitor;
    private final Object listener;
    private final int[] coordinates = new int[2];
    private final View.OnAttachStateChangeListener attachment;
    private boolean registered;
    private boolean closing;
    private boolean showing;
    // Native RealContainerIslandMonitor starts at Java's zero field value.
    private int width;

    static AppBarStatusIconIslandBinding create(ClassLoader loader, View container,
            Object island, Object animator) throws ReflectiveOperationException {
        requireMain();
        AppBarStatusIconIslandBinding result = new AppBarStatusIconIslandBinding(
                loader, container, island, animator);
        try {
            write(container, "_islandMonitor", result.monitor);
            call(container, "setAnimatorController", new Class<?>[]{animator.getClass()}, animator);
            container.addOnAttachStateChangeListener(result.attachment);
            return result;
        } catch (ReflectiveOperationException | RuntimeException | Error failure) {
            AppBarContentPreparation.cleanupCreation(result, failure);
            throw failure;
        }
    }

    private AppBarStatusIconIslandBinding(ClassLoader loader, View container, Object island,
            Object animator) throws ReflectiveOperationException {
        this.container = container;
        this.island = island;
        this.animator = animator;
        if (!container.getClass().getName().equals("com.android.systemui.statusbar.views.MiuiStatusIconContainer")
                || container.isAttachedToWindow() || read(container, "_islandMonitor") != null
                || read(container, "animatorController") != null)
            throw new IllegalStateException("App status-icon monitor already owned");
        state = read(island, "islandStateHandler");
        listenerType = Class.forName("com.android.systemui.statusbar.OnIslandStatusChangedListener", false, loader);
        add = island.getClass().getMethod("addOnIslandStatusChangedListener", listenerType);
        remove = island.getClass().getMethod("removeOnIslandStatusChangedListener", listenerType);
        location = Class.forName("com.android.systemui.controlcenter.shade.CombinedHeaderController$Companion", false, loader)
                .getMethod("getLocationInWindowWithoutTransform", View.class, int[].class);
        tinyScreen = Class.forName("com.miui.utils.configs.MiuiConfigs", false, loader)
                .getMethod("isFlipTinyScreen", Context.class);
        space = read(island, "statusContainerSpace");
        setSpace = space.getClass().getMethod("setValue", Object.class);
        Class<?> contract = Class.forName("com.android.systemui.statusbar.IslandMonitor", false, loader);
        monitor = Proxy.newProxyInstance(loader, new Class<?>[]{contract}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) return identity(proxy, method, args);
            requireMain();
            switch (method.getName()) {
                case "getBlocked": return false;
                case "getIslandShowing": return showing;
                case "getIslandWidth": return width;
                case "init": if (!closing && container.isAttachedToWindow()) register(); return null;
                case "onLayoutChanged":
                    if (!closing && (Boolean) read(state, "islandShowing"))
                        update((Rect) read(state, "islandRect"), true, false);
                    return null;
                case "updateLocation": return null; // Native real monitor inherits the no-op.
                default: throw new IllegalStateException("Unknown island monitor contract " + method);
            }
        });
        listener = Proxy.newProxyInstance(loader, new Class<?>[]{listenerType}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) return identity(proxy, method, args);
            requireMain();
            if (closing) return null;
            if (method.getName().equals("onIslandSizeChanged")) update((Rect) args[0], (Boolean) args[1], true);
            else if (!method.getName().equals("onIslandStatusChanged"))
                throw new IllegalStateException("Unknown island state contract " + method);
            return null; // Native real monitor's status callback is empty.
        });
        attachment = new View.OnAttachStateChangeListener() {
            public void onViewAttachedToWindow(View view) { lifecycle(true); }
            public void onViewDetachedFromWindow(View view) { lifecycle(false); }
        };
    }

    private void register() throws ReflectiveOperationException {
        if (closing || registered) return;
        registered = true; // add may publish immediately and throw afterward.
        add.invoke(island, listener);
    }

    private void lifecycle(boolean attached) {
        try {
            requireMain();
            if (attached) register();
            else if (registered) { remove.invoke(island, listener); registered = false; }
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException("App island lifecycle failed", failure); }
    }

    private void update(Rect rect, boolean animate, boolean requestLayout) throws ReflectiveOperationException {
        showing = !rect.isEmpty();
        int next = -1;
        if (container.getWidth() != 0 && showing && !(Boolean) tinyScreen.invoke(null, container.getContext())) {
            location.invoke(null, container, coordinates);
            int id = container.getResources().getIdentifier("status_bar_island_padding", "dimen", "com.android.systemui");
            if (id == 0) throw new IllegalStateException("Missing native island padding");
            int padding = container.getResources().getDimensionPixelSize(id);
            int layoutFrom = ((Number) call(container, "getLayoutFrom")).intValue();
            int translation = (Boolean) call(island, "supportIslandTranslation", new Class<?>[]{int.class}, layoutFrom)
                    ? ((Number) call(island, "getTranslationX")).intValue() : 0;
            next = container.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL
                    ? translation + container.getWidth() + coordinates[0] - (rect.left - padding)
                    : rect.right + padding - coordinates[0] - translation;
        }
        if (next == width) return;
        width = next;
        // Both A16 and A17 RealContainerIslandMonitor publish layoutFrom=0
        // width changes, independently of selected-token animation eligibility.
        // Registration starts on attachment, so detached preparation cannot
        // publish geometry. Home-only clock/battery providers remain untouched.
        if (((Number) call(container, "getLayoutFrom")).intValue() == 0)
            setSpace.invoke(space, width);
        write(container, "islandWidthChanged", true);
        write(container, "islandAnimate", animate);
        if (requestLayout) container.requestLayout();
    }

    @Override public void close() throws ReflectiveOperationException {
        requireMain();
        closing = true;
        container.removeOnAttachStateChangeListener(attachment);
        if (registered) { remove.invoke(island, listener); registered = false; }
        if (read(container, "_islandMonitor") == monitor) write(container, "_islandMonitor", null);
        if (read(container, "animatorController") == animator) write(container, "animatorController", null);
    }

    private static Object identity(Object proxy, Method method, Object[] args) {
        if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
        if (method.getName().equals("equals")) return proxy == args[0];
        return "AppStatusIconIslandMonitor";
    }
    private static void requireMain() {
        if (Looper.myLooper() != Looper.getMainLooper()) throw new IllegalStateException("App island requires main Looper");
    }
}
