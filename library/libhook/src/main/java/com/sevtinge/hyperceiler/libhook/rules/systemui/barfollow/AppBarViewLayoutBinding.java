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
import android.view.ViewGroup;

import static com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow.AppBarContentPreparation.call;
import static com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow.AppBarContentPreparation.read;
import static com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow.AppBarContentPreparation.write;

/** Native notification placement and BatteryIndicator initialization with explicit disposal. */
final class AppBarViewLayoutBinding implements AutoCloseable {
    private final ClassLoader loader;
    private final View view;
    private final View notification;
    private final View indicator;
    private final Object dependency;
    private final Object dotsController;
    private final Object dotsListener;
    private final java.lang.reflect.Method addDots;
    private final java.lang.reflect.Method removeDots;
    private final ViewGroup originalParent;
    private final int originalIndex;
    private final ViewGroup.LayoutParams originalParams;
    private boolean indicatorBound;
    private boolean notificationBound;
    private boolean dotsBound;

    static AppBarViewLayoutBinding create(ClassLoader loader, View view, Object fragment, Object viewDependency)
            throws ReflectiveOperationException {
        requireMain();
        AppBarViewLayoutBinding result = new AppBarViewLayoutBinding(loader, view, fragment, viewDependency);
        try {
            result.notificationBound = true;
            call(view, "setNotificationIconAreaInnner", new Class<?>[]{View.class}, result.notification);
            if (result.notification.getParent() != read(view, "mNotificationIconArea"))
                throw new IllegalStateException("Native App notification placement failed");
            result.indicatorBound = true;
            call(result.indicator, "setDependency", new Class<?>[]{result.dependency.getClass()}, result.dependency);
            if (!(Boolean) read(result.indicator, "mInit"))
                throw new IllegalStateException("Native App battery indicator did not initialize");
            result.dotsBound = true;
            result.addDots.invoke(result.dotsController, result.dotsListener);
            return result;
        } catch (ReflectiveOperationException | RuntimeException | Error failure) {
            AppBarContentPreparation.cleanupCreation(result, failure);
            throw failure;
        }
    }

    private AppBarViewLayoutBinding(ClassLoader loader, View view, Object fragment, Object viewDependency)
            throws ReflectiveOperationException {
        this.loader = loader;
        this.view = view;
        notification = child("notificationIcons");
        indicator = child("battery_indicator");
        if (view.isAttachedToWindow() || read(view, "mNotificationIconAreaInner") != null
                || (Boolean) read(indicator, "mInit") || read(indicator, "mDep") != null)
            throw new IllegalStateException("App layout already initialized");
        originalParent = (ViewGroup) notification.getParent();
        originalIndex = originalParent.indexOfChild(notification);
        originalParams = notification.getLayoutParams();
        dependency = read(fragment, "batteryIndicatorDep");
        dotsController = read(read(viewDependency, "multiTaskFactory"), "mMultiTaskStatusBarDotsAreaController");
        dotsListener = read(view, "splitScreenWhiteDotAreaListener");
        if (read(dotsListener, "this$0") != view)
            throw new IllegalStateException("App split-screen listener has a different View owner");
        Class<?> dotsType = type("com.miui.systemui.statusbar.policy.MultiTaskStatusBarDotsAreaController");
        addDots = dotsType.getMethod("addListener", dotsListener.getClass());
        removeDots = dotsType.getMethod("removeListener", dotsListener.getClass());
        // Verify both native release contracts before setDependency's immediate callbacks.
        read(dependency, "batteryController").getClass().getMethod("removeCallback", Object.class);
        read(dependency, "configController").getClass().getMethod("removeCallback",
                type("com.android.systemui.statusbar.policy.ConfigurationController$ConfigurationListener"));
    }

    boolean initialized() throws ReflectiveOperationException {
        return notificationBound && indicatorBound && dotsBound && (Boolean) read(indicator, "mInit")
                && notification.getParent() == read(view, "mNotificationIconArea");
    }

    @Override public void close() throws ReflectiveOperationException {
        requireMain();
        if (view.isAttachedToWindow()) throw new IllegalStateException("Detach App layout before disposal");
        if (dotsBound) {
            removeDots.invoke(dotsController, dotsListener);
            dotsBound = false;
        }
        try {
            if (indicatorBound) {
                // Do not invoke onDetachedFromWindow on a detached View. Release
                // precisely the two registrations made by BatteryIndicator.init.
                try {
                    call(read(dependency, "batteryController"), "removeCallback", new Class<?>[]{Object.class}, indicator);
                } finally {
                    call(read(dependency, "configController"), "removeCallback", new Class<?>[]{type(
                            "com.android.systemui.statusbar.policy.ConfigurationController$ConfigurationListener")}, indicator);
                }
                write(indicator, "mInit", false);
                write(indicator, "mDep", null);
                indicatorBound = false;
            }
        } finally {
            if (notificationBound) {
                call(view, "setNotificationIconAreaInnner", new Class<?>[]{View.class}, (Object) null);
                View.OnLayoutChangeListener listener = (View.OnLayoutChangeListener) read(view, "onLayoutChangeListener");
                ((View) read(view, "mFullscreenStatusBarNotificationIconArea")).removeOnLayoutChangeListener(listener);
                ((View) read(view, "mDripStatusBarNotificationIconArea")).removeOnLayoutChangeListener(listener);
                if (notification.getParent() == null) originalParent.addView(notification,
                        Math.min(originalIndex, originalParent.getChildCount()), originalParams);
                notificationBound = false;
            }
        }
    }

    private View child(String name) {
        int id = view.getResources().getIdentifier(name, "id", "com.android.systemui");
        if (id == 0) throw new IllegalStateException("Missing App native layout resource " + name);
        return view.requireViewById(id);
    }
    private Class<?> type(String name) throws ClassNotFoundException { return Class.forName(name, false, loader); }
    private static void requireMain() {
        if (Looper.myLooper() != Looper.getMainLooper()) throw new IllegalStateException("App layout requires main Looper");
    }
}
