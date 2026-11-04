/*
 * Derived from MiuiBackGestureHook status-bar-follow implementation.
 * Source: https://github.com/wxxsfxyzm/MiuiBackGestureHook
 * Reference commit: 004bc059e5821775917b771d89aa1d8c18a1e669 (includes b6e582f and later fixes)
 * Copyright (C) 2026 MiuiBackGestureHook contributors.
 * Licensed under the Apache License, Version 2.0; modified for HyperCeiler.
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow;

import android.animation.ValueAnimator;
import android.os.Handler;
import android.os.Looper;

import java.lang.ref.Reference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Native A17 per-content appearance owners, still unbound to any View. */
final class AppBarAppearancePreparation implements AutoCloseable {
    private static final String DAGGER = "com.android.systemui.dagger.DaggerReferenceGlobalRootComponent$";
    private static final String DARK = "com.android.systemui.statusbar.phone.DarkIconDispatcherImpl";
    private final ClassLoader loader;
    private final Object dump;
    private final Object theme;
    private final Object configuration;
    private final Object state;
    private final Object commandQueue;
    private final Object lightFactory;
    private final Object primaryDark;
    private final Object primaryRootDark;
    private final Constructor<?> darkConstructor;
    private Object dark;
    private boolean closed;

    static AppBarAppearancePreparation create(ClassLoader loader, Object component, Object injector)
            throws ReflectiveOperationException {
        requireMain();
        AppBarAppearancePreparation result = new AppBarAppearancePreparation(loader, component, injector);
        try {
            result.initialize();
            return result;
        } catch (ReflectiveOperationException | RuntimeException | Error failure) {
            AppBarContentPreparation.cleanupCreation(result, failure);
            throw failure;
        }
    }

    private AppBarAppearancePreparation(ClassLoader loader, Object component, Object injector)
            throws ReflectiveOperationException {
        this.loader = loader;
        Object global = read(component, "referenceGlobalRootComponentImpl");
        configuration = read(injector, "configurationController");
        state = read(injector, "statusBarStateControllerImpl");
        commandQueue = call(read(component, "provideCommandQueueProvider"), "get");
        theme = call(read(component, "miuiThemeParamsObserverProvider"), "get");
        Constructor<?> selected = null;
        for (Constructor<?> candidate : type(DARK).getDeclaredConstructors()) {
            Class<?>[] parameters = candidate.getParameterTypes();
            if (parameters.length != 4 || parameters[0] != int.class
                    || parameters[2] != theme.getClass()
                    || parameters[3] != type("com.android.systemui.dump.DumpManager")) continue;
            Method create = parameters[1].getDeclaredMethod("create", type(
                    "com.android.systemui.statusbar.phone.LightBarTransitionsController$DarkIntensityApplier"));
            if (create.getReturnType() != type("com.android.systemui.statusbar.phone.LightBarTransitionsController"))
                continue;
            if (selected != null) throw new NoSuchMethodException("Ambiguous native dark owner");
            selected = candidate;
        }
        if (selected == null) throw new NoSuchMethodException("Native dark owner constructor");
        darkConstructor = selected;
        lightFactory = AppBarNativeContracts.provider(component, selected.getParameterTypes()[1]);
        primaryDark = read(injector, "darkIconDispatcher");
        Object repository = call(read(component, "provideDisplayComponentRepositoryProvider"), "get");
        Object display = call(repository, "get", new Class<?>[]{int.class}, 0);
        primaryRootDark = read(call(display, "getStatusBarRootFactory"), "darkIconDispatcher");

        // Native global provider case 26 allocates all three fresh TreeMaps.
        // Never register the second bar under names in the main DumpManager.
        Constructor<?> factory = type(DAGGER + "ReferenceGlobalRootComponentImpl$SwitchingProvider")
                .getDeclaredConstructor(global.getClass(), int.class);
        factory.setAccessible(true);
        dump = call(factory.newInstance(global, 26), "get");
        if (!dump.getClass().getName().equals("com.android.systemui.dump.DumpManagerImpl")
                || !map(dump, "dumpables").isEmpty())
            throw new IllegalStateException("No private native dump registry");

        // Resolve cleanup contracts before any constructor can register itself.
        type(DARK).getMethod("stop");
        type("com.android.internal.policy.GestureNavigationSettingsObserver").getMethod("unregister");
        configuration.getClass().getMethod("removeCallback", type(
                "com.android.systemui.statusbar.policy.ConfigurationController$ConfigurationListener"));
        commandQueue.getClass().getMethod("removeCallback", type("com.android.systemui.statusbar.CommandQueue$Callbacks"));
        state.getClass().getMethod("removeCallback", type(
                "com.android.systemui.plugins.statusbar.StatusBarStateController$StateListener"));
    }

    private void initialize() throws ReflectiveOperationException {
        dark = darkConstructor.newInstance(0, lightFactory, theme, dump);
        if (dark == primaryDark || dark == primaryRootDark || !map(dark, "mReceivers").isEmpty()
                || map(dump, "dumpables").size() != 1)
            throw new IllegalStateException("Appearance owners were not isolated");
        // A16 keeps the physical-display CommandQueue callback for both bars.
        // Retain the physical callback lifecycle. The private LightBarController
        // uses native immediate tint so a pending desktop transition cannot
        // defer the application's latest colors while WMS moves its window.
        Object transitions = read(dark, "mTransitionsController");
        if (read(transitions, "mApplier") != dark)
            throw new IllegalStateException("App tint applier identity changed");
    }

    Object dark() { return dark; }
    Object dump() { return dump; }

    @Override public void close() throws ReflectiveOperationException {
        requireMain();
        if (closed) return;
        List<Throwable> failures = new ArrayList<>();
        // A native constructor may throw after publishing itself. Recover only
        // objects owned by this private registry, never by a broad map diff.
        attempt(failures, () -> {
            if (dark == null) for (Object candidate : new ArrayList<>(map(theme, "map").keySet())) {
                if (candidate.getClass().getName().equals(DARK) && read(candidate, "mDumpManager") == dump) {
                    if (dark != null) throw new IllegalStateException("Ambiguous private dark owner");
                    dark = candidate;
                }
            }
        });
        if (dark != null) {
            List<Object> transitions = new ArrayList<>();
            attempt(failures, () -> {
                Object controller = read(dark, "mTransitionsController");
                if (controller != null) transitions.add(controller);
                // The factory registers its callback before returning the
                // controller to DarkIconDispatcher's constructor.
                for (Object callback : snapshot(commandQueue, "mCallbacks")) {
                    if (!callback.getClass().getName().equals(
                            "com.android.systemui.statusbar.phone.LightBarTransitionsController$Callback")) continue;
                    Object owner = ((Reference<?>) read(callback, "mSelf")).get();
                    if (owner != null && read(owner, "mApplier") == dark && !transitions.contains(owner))
                        transitions.add(owner);
                }
            });
            for (Object controller : transitions) releaseTransitions(controller, failures);
            attempt(failures, () -> call(dark, "stop"));
        }
        attempt(failures, this::verifyReleased);
        if (!failures.isEmpty()) {
            ReflectiveOperationException failure = new ReflectiveOperationException("App bar appearance cleanup incomplete");
            for (Throwable cause : failures) failure.addSuppressed(cause);
            throw failure;
        }
        closed = true;
        dark = null;
    }

    private void releaseTransitions(Object controller, List<Throwable> failures) {
        attempt(failures, () -> {
            Object animator = read(controller, "mTintAnimator");
            if (animator != null) ((ValueAnimator) animator).cancel();
        });
        attempt(failures, () -> {
            Object runnable = read(controller, "mTransitionDeferringDoneRunnable");
            if (runnable != null) ((Handler) read(controller, "mMainHandler")).removeCallbacks((Runnable) runnable);
        });
        attempt(failures, () -> call(commandQueue, "removeCallback",
                new Class<?>[]{type("com.android.systemui.statusbar.CommandQueue$Callbacks")}, read(controller, "mCallback")));
        attempt(failures, () -> call(state, "removeCallback", new Class<?>[]{type(
                "com.android.systemui.plugins.statusbar.StatusBarStateController$StateListener")}, read(controller, "mCallback")));
        // Native unregister queues after register on the same background Handler.
        attempt(failures, () -> {
            Object observer = read(controller, "mGestureNavigationSettingsObserver");
            if (observer != null) call(observer, "unregister");
        });
    }

    private void verifyReleased() throws ReflectiveOperationException {
        // The Home bar may be rebound while App exists. Verify only our
        // retained registrations; unrelated live listener changes are valid.
        if (!map(dump, "dumpables").isEmpty()
                || (dark != null && (!map(dark, "mReceivers").isEmpty()
                || map(theme, "map").containsKey(dark))))
            throw new IllegalStateException("App appearance registrations remain after close: dump="
                    + map(dump, "dumpables").keySet() + ", receivers="
                    + (dark == null ? "none" : map(dark, "mReceivers").keySet())
                    + ", theme=" + (dark != null && map(theme, "map").containsKey(dark)));
        if (dark == null) return;
        for (Object callback : snapshot(commandQueue, "mCallbacks")) {
            if (!callback.getClass().getName().equals(
                    "com.android.systemui.statusbar.phone.LightBarTransitionsController$Callback")) continue;
            Object owner = ((Reference<?>) read(callback, "mSelf")).get();
            if (owner != null && read(owner, "mApplier") == dark)
                throw new IllegalStateException("App light-transition callback remains registered");
        }
    }

    private static Object[] snapshot(Object owner, String field) throws ReflectiveOperationException {
        List<?> list = (List<?>) read(owner, field);
        synchronized (list) { return list.toArray(); }
    }

    private static Map<?, ?> map(Object owner, String field) throws ReflectiveOperationException {
        return (Map<?, ?>) read(owner, field);
    }

    private interface Cleanup { void run() throws ReflectiveOperationException; }
    private static void attempt(List<Throwable> failures, Cleanup cleanup) {
        try { cleanup.run(); }
        catch (ReflectiveOperationException | RuntimeException failure) { failures.add(failure); }
    }

    private Class<?> type(String name) throws ClassNotFoundException { return Class.forName(name, false, loader); }
    private static Object read(Object owner, String name) throws ReflectiveOperationException {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }
    private static Object call(Object owner, String name) throws ReflectiveOperationException {
        return call(owner, name, new Class<?>[0]);
    }
    private static Object call(Object owner, String name, Class<?>[] types, Object... args)
            throws ReflectiveOperationException {
        Method method = owner.getClass().getMethod(name, types);
        method.setAccessible(true);
        return method.invoke(owner, args);
    }
    private static void requireMain() {
        if (Looper.myLooper() != Looper.getMainLooper()) throw new IllegalStateException("Appearance requires main Looper");
    }
}
