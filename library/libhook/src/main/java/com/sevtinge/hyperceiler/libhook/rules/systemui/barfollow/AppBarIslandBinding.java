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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow.AppBarContentPreparation.call;
import static com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow.AppBarContentPreparation.read;
import static com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow.AppBarContentPreparation.write;

/** App-role island binding: observes native producers, never publishes Home layout providers. */
final class AppBarIslandBinding implements AutoCloseable {
    private final ClassLoader loader;
    private final Object injector;
    private final Object island;
    private final View view;
    private final BooleanSupplier selected;
    private final Class<?> listenerType;
    private final Object[] homeProviders;
    private final List<Object> subscriptions = new ArrayList<>();
    private Object stretch;
    private Object listener;
    private boolean registered;
    private boolean closed;

    static AppBarIslandBinding bind(ClassLoader loader, Object injector, BooleanSupplier selected)
            throws ReflectiveOperationException {
        requireMain();
        AppBarIslandBinding result = new AppBarIslandBinding(loader, injector, selected);
        try { result.initialize(); return result; }
        catch (ReflectiveOperationException | RuntimeException | Error failure) {
            AppBarContentPreparation.cleanupCreation(result, failure);
            throw failure;
        }
    }

    private AppBarIslandBinding(ClassLoader loader, Object injector, BooleanSupplier selected)
            throws ReflectiveOperationException {
        this.loader = loader;
        this.injector = injector;
        this.selected = selected;
        island = read(injector, "islandController");
        view = (View) read(injector, "rootView");
        listenerType = type("com.android.systemui.statusbar.OnIslandStatusChangedListener");
        island.getClass().getMethod("removeOnIslandStatusChangedListener", listenerType);
        homeProviders = providers();
        type("kotlinx.coroutines.DisposableHandle").getMethod("dispose");
    }

    private void initialize() throws ReflectiveOperationException {
        if (read(injector, "stretchAnimation") != null)
            throw new IllegalStateException("App island injector already bound");
        Object delegate = read(injector, "statusBarDelegate");
        Class<?> stretchType = type("com.android.systemui.statusbar.phone.IslandStretchAnimation");
        stretch = stretchType.getConstructor(int.class, island.getClass(), delegate.getClass())
                .newInstance(0, island, delegate);
        write(injector, "stretchAnimation", stretch);
        write(injector, "mStatusBar", view);
        // Exact native initMiuiViewsOnViewCreated View mapping. Its two native
        // flow consumers are retained, with explicitly removable subscriptions.
        write(stretch, "leftContainer", child("phone_status_bar_left_container"));
        write(stretch, "rightContainer", child("system_icon_area"));
        write(stretch, "privacyArea", child("privacy_area"));
        write(stretch, "clockView", child("clock"));
        write(stretch, "padClockView", child("pad_clock"));
        write(injector, "mClockView", child("clock"));
        call(stretch, "setNotificationView", new Class<?>[]{View.class},
                call(view, "getFullscreenStatusBarNotificationIconArea"));
        Object nativeListener = read(injector, "islandListener");
        listener = Proxy.newProxyInstance(loader, new Class<?>[]{listenerType}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                if (method.getName().equals("equals")) return proxy == args[0];
                return "AppBarIslandListener";
            }
            if (closed) return null;
            requireMain();
            Object[] forwarded = args.clone();
            // A16 isSupportAnimForWindow requires the selected token and a
            // non-hidden window. State/size still reach the native listener.
            forwarded[1] = (Boolean) args[1] && selected.getAsBoolean()
                    && !(Boolean) read(injector, "mIsTempWindowHidden");
            try { return method.invoke(nativeListener, forwarded); }
            catch (InvocationTargetException failure) { throw failure.getCause(); }
        });
        registered = true;
        call(island, "addOnIslandStatusChangedListener", new Class<?>[]{listenerType}, listener);
        startFlows();
        verifyHomeProviders();
    }

    private void startFlows() throws ReflectiveOperationException {
        requireMain();
        if (closed || !subscriptions.isEmpty()) return;
        collect(read(island, "translationFlow"), 0);
        Object width = read(read(injector, "statusBarDelegate"), "bigIslandWidth");
        if (width != null) collect(width, 1);
    }

    private void collect(Object flow, int kind) throws ReflectiveOperationException {
        Class<?> consumerType = type("com.android.systemui.statusbar.phone.IslandStretchAnimation$initMiuiViewsOnViewCreated$1");
        Constructor<?> constructor = consumerType.getDeclaredConstructor(int.class);
        constructor.setAccessible(true);
        Object consumer = constructor.newInstance(kind);
        write(consumer, "this$0", stretch);
        Method collect = type("com.android.systemui.util.kotlin.JavaAdapterKt").getMethod("collectFlow$default",
                View.class, type("kotlinx.coroutines.flow.Flow"), Consumer.class,
                type("kotlin.coroutines.CoroutineContext"), int.class);
        // Same native View/CREATED lifecycle collector and mask used by
        // initMiuiViewsOnViewCreated. Keep its returned handle for disposal.
        subscriptions.add(collect.invoke(null, view, flow, consumer, null, 24));
    }

    private void stopFlows() throws ReflectiveOperationException {
        Method dispose = type("kotlinx.coroutines.DisposableHandle").getMethod("dispose");
        ReflectiveOperationException failure = null;
        for (Object subscription : subscriptions) try { dispose.invoke(subscription); }
        catch (ReflectiveOperationException cause) {
            if (failure == null) failure = cause; else failure.addSuppressed(cause);
        }
        if (failure != null) throw failure;
        subscriptions.clear();
    }

    void verifyHomeProviders() throws ReflectiveOperationException {
        Object[] actual = providers();
        for (int i = 0; i < actual.length; i++) if (actual[i] != homeProviders[i])
            throw new IllegalStateException("App island binding changed Home layout provider");
    }

    @Override public void close() throws ReflectiveOperationException {
        requireMain();
        closed = true;
        List<Throwable> failures = new ArrayList<>();
        try { stopFlows(); } catch (ReflectiveOperationException | RuntimeException failure) { failures.add(failure); }
        try { if (registered) {
            call(island, "removeOnIslandStatusChangedListener", new Class<?>[]{listenerType}, listener);
            registered = false;
        }} catch (ReflectiveOperationException | RuntimeException failure) { failures.add(failure); }
        try { if (stretch != null) {
            call(stretch, "cancelAnimation");
            for (String field : new String[]{"leftFolme", "rightFolme", "privacyFolme", "notificationFolme"})
                write(stretch, field, null);
            if (read(injector, "stretchAnimation") == stretch) write(injector, "stretchAnimation", null);
        }} catch (ReflectiveOperationException | RuntimeException failure) { failures.add(failure); }
        try {
            for (Object current : listeners()) if (current == listener)
                throw new IllegalStateException("App island listener remains registered");
        } catch (ReflectiveOperationException | RuntimeException failure) { failures.add(failure); }
        if (!failures.isEmpty()) {
            ReflectiveOperationException failure = new ReflectiveOperationException("App island release incomplete");
            for (Throwable cause : failures) failure.addSuppressed(cause);
            throw failure;
        }
    }

    private java.util.Collection<?> listeners() throws ReflectiveOperationException {
        return (java.util.Collection<?>) read(read(island, "islandStateHandler"), "onIslandStateChangedListeners");
    }
    private Object[] providers() throws ReflectiveOperationException {
        return new Object[]{read(island, "clockSpaceProvider"), read(island, "batterySpaceProvider"), read(island, "padClockLeftProvider")};
    }
    private View child(String name) {
        int id = view.getResources().getIdentifier(name, "id", "com.android.systemui");
        if (id == 0) throw new IllegalStateException("Missing island View " + name);
        return view.requireViewById(id);
    }
    private Class<?> type(String name) throws ClassNotFoundException { return Class.forName(name, false, loader); }
    private static void requireMain() {
        if (Looper.myLooper() != Looper.getMainLooper()) throw new IllegalStateException("App island requires main Looper");
    }
}
