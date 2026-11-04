/*
 * Derived from MiuiBackGestureHook status-bar-follow implementation.
 * Source: https://github.com/wxxsfxyzm/MiuiBackGestureHook
 * Reference commit: 004bc059e5821775917b771d89aa1d8c18a1e669 (includes b6e582f and later fixes)
 * Copyright (C) 2026 MiuiBackGestureHook contributors.
 * Licensed under the Apache License, Version 2.0; modified for HyperCeiler.
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow;

import android.content.Context;
import android.os.Looper;
import android.view.View;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow.AppBarContentPreparation.call;
import static com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow.AppBarContentPreparation.read;
import static com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow.AppBarContentPreparation.write;

/** Owns native carrier/minimalism/keyguard consumers and the phone layout strategy. */
final class AppBarAuxiliaryBinding implements AutoCloseable {
    private final ClassLoader loader;
    private final Object injector;
    private final View view;
    private final Object region;
    private final Object minimalism;
    private final Object keyguard;
    private final Object regionCallback;
    private final Object minimalismCallback;
    private final Object keyguardCallback;
    private final Class<?> keyguardContract;
    private boolean regionBound;
    private boolean minimalismBound;
    private boolean keyguardBound;
    private Object strategy;
    private boolean closed;

    static AppBarAuxiliaryBinding create(ClassLoader loader, Object injector) throws ReflectiveOperationException {
        requireMain();
        AppBarAuxiliaryBinding result = new AppBarAuxiliaryBinding(loader, injector);
        try { result.initialize(); return result; }
        catch (ReflectiveOperationException | RuntimeException | Error failure) {
            AppBarContentPreparation.cleanupCreation(result, failure);
            throw failure;
        }
    }

    private AppBarAuxiliaryBinding(ClassLoader loader, Object injector) throws ReflectiveOperationException {
        this.loader = loader;
        this.injector = injector;
        view = (View) read(injector, "rootView");
        if (view.isAttachedToWindow() || read(injector, "operatorNameViewController") != null
                || read(injector, "strategyBase") != null)
            throw new IllegalStateException("App auxiliary content already initialized");
        region = read(injector, "regionController");
        minimalism = read(injector, "minimalismModeController");
        keyguard = read(injector, "keyguardStateController");
        regionCallback = read(injector, "regionCallback");
        minimalismCallback = read(injector, "minimalismModeListener");
        keyguardCallback = read(injector, "keyguardCallback");
        keyguardContract = type("com.android.systemui.statusbar.policy.KeyguardStateController$Callback");
        if (!keyguardContract.isInstance(keyguardCallback))
            throw new IllegalStateException("Unknown native keyguard callback");
        // A17 retains the erased CallbackController entry points after R8.
        keyguard.getClass().getMethod("addCallback", Object.class);
        keyguard.getClass().getMethod("removeCallback", Object.class);
        if (!(read(region, "mCallbacks") instanceof Collection<?>)
                || !(read(minimalism, "mListeners") instanceof Collection<?>))
            throw new IllegalStateException("Unknown auxiliary listener ownership");
        type("com.android.systemui.util.ViewController").getMethod("destroy");
    }

    private void initialize() throws ReflectiveOperationException {
        Class<?> configs = type("com.miui.utils.configs.MiuiConfigs");
        if (configs.getField("IS_PAD").getBoolean(null) || configs.getField("IS_FOLD").getBoolean(null)
                || (Boolean) type("miui.util.MiuiMultiDisplayTypeInfo").getMethod("isFlipDevice").invoke(null))
            throw new IllegalStateException("App auxiliary binding requires the native phone strategy");
        Class<?> phone = type("com.android.systemui.statusbar.phone.MiuiPhoneStrategy");
        View cutout = (View) read(injector, "mCutoutSpace");
        if (phone.getDeclaredConstructors().length == 0) {
            // Exact inspected empty subclass; its base constructor only stores
            // Context/View and reads configuration/rotation without mutations.
            Class<?> unsafe = Class.forName("sun.misc.Unsafe");
            Field field = unsafe.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            strategy = unsafe.getMethod("allocateInstance", Class.class).invoke(field.get(null), phone);
            Field contextField = phone.getSuperclass().getDeclaredField("mContext");
            Field cutoutField = phone.getSuperclass().getDeclaredField("mCutoutView");
            contextField.setAccessible(true);
            cutoutField.setAccessible(true);
            contextField.set(strategy, view.getContext());
            cutoutField.set(strategy, cutout);
        } else {
            Constructor<?> constructor = phone.getDeclaredConstructor(Context.class, View.class);
            constructor.setAccessible(true);
            strategy = constructor.newInstance(view.getContext(), cutout);
        }
        write(injector, "strategyBase", strategy);
        call(injector, "initOperatorName");
        regionBound = true;
        call(region, "addCallback", new Class<?>[]{type("com.miui.systemui.controller.RegionController$Callback")}, regionCallback);
        minimalismBound = true;
        call(minimalism, "addListener", new Class<?>[]{type(
                "com.android.systemui.statusbar.policy.MinimalismModeController$MinimalismModeListener")}, minimalismCallback);
        keyguardBound = true;
        call(keyguard, "addCallback", new Class<?>[]{Object.class}, keyguardCallback);
    }

    @Override public void close() throws ReflectiveOperationException {
        requireMain();
        if (closed) return;
        if (view.isAttachedToWindow()) throw new IllegalStateException("Detach App auxiliary Views before disposal");
        List<Throwable> failures = new ArrayList<>();
        release(failures, () -> { if (regionBound) {
            ((Collection<?>) read(region, "mCallbacks")).remove(regionCallback); regionBound = false;
        }});
        release(failures, () -> { if (minimalismBound) {
            ((Collection<?>) read(minimalism, "mListeners")).remove(minimalismCallback); minimalismBound = false;
        }});
        release(failures, () -> { if (keyguardBound) {
            call(keyguard, "removeCallback", new Class<?>[]{Object.class}, keyguardCallback); keyguardBound = false;
        }});
        if (!failures.isEmpty()) {
            ReflectiveOperationException failure = new ReflectiveOperationException("App auxiliary callbacks remain owned");
            for (Throwable cause : failures) failure.addSuppressed(cause);
            throw failure;
        }
        // Region delivery can lazily create the carrier controller. Recover it
        // from this private injector even if its initial update threw.
        Object operator = read(injector, "operatorNameViewController");
        if (operator != null) {
            call(operator, "destroy");
            write(injector, "operatorNameViewController", null);
        }
        if (strategy != null && read(injector, "strategyBase") == strategy) write(injector, "strategyBase", null);
        closed = true;
    }

    private interface Release { void run() throws ReflectiveOperationException; }
    private static void release(List<Throwable> failures, Release release) {
        try { release.run(); }
        catch (ReflectiveOperationException | RuntimeException failure) { failures.add(failure); }
    }
    private Class<?> type(String name) throws ClassNotFoundException { return Class.forName(name, false, loader); }
    private static void requireMain() {
        if (Looper.myLooper() != Looper.getMainLooper()) throw new IllegalStateException("App auxiliary binding requires main Looper");
    }
}
