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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.BooleanSupplier;
import static com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow.AppBarContentPreparation.read;

/** Native A16 IAnimatable qualification, preserving the previous native predicate. */
final class AppBarIconAnimationBinding implements AutoCloseable {
    private final View container;
    private final Object previous;
    private final Method setter;
    private final Object gate;
    private boolean closed;

    static AppBarIconAnimationBinding create(ClassLoader loader, View container,
            BooleanSupplier allowed) throws ReflectiveOperationException {
        requireMain();
        AppBarIconAnimationBinding result = new AppBarIconAnimationBinding(loader, container, allowed);
        try { result.setter.invoke(container, result.gate); return result; }
        catch (ReflectiveOperationException | RuntimeException | Error failure) {
            AppBarContentPreparation.cleanupCreation(result, failure);
            throw failure;
        }
    }

    private AppBarIconAnimationBinding(ClassLoader loader, View container, BooleanSupplier allowed)
            throws ReflectiveOperationException {
        this.container = container;
        if (!container.getClass().getName().equals("com.android.systemui.statusbar.views.MiuiStatusIconContainer"))
            throw new IllegalStateException("Unexpected real status-icon container");
        Class<?> contract = Class.forName("com.android.systemui.statusbar.views.IAnimatable", false, loader);
        Method predicate = contract.getMethod("animatorEnable");
        if (predicate.getReturnType() != boolean.class) throw new NoSuchMethodException("IAnimatable changed");
        setter = container.getClass().getMethod("setAnimatable", contract);
        previous = read(container, "animatable");
        gate = Proxy.newProxyInstance(loader, new Class<?>[]{contract}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                if (method.getName().equals("equals")) return proxy == args[0];
                if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                return "OwnedBarAnimationEligibility";
            }
            requireMain();
            if (!method.equals(predicate)) throw new IllegalStateException("Unknown IAnimatable method");
            if (closed || !allowed.getAsBoolean()) return false;
            try { return previous == null || (Boolean) predicate.invoke(previous); }
            catch (InvocationTargetException failure) { throw failure.getCause(); }
        });
    }

    @Override public void close() throws ReflectiveOperationException {
        requireMain();
        // A native rebind may already have replaced this delegate. Never put an
        // obsolete predicate over its replacement, and retain failures for retry.
        closed = true;
        if (read(container, "animatable") == gate) setter.invoke(container, previous);
    }

    private static void requireMain() {
        if (Looper.myLooper() != Looper.getMainLooper()) throw new IllegalStateException("Bar icon gate requires main Looper");
    }
}
