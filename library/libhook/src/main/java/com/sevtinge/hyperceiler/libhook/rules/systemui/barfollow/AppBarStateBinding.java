/*
 * Derived from MiuiBackGestureHook status-bar-follow implementation.
 * Source: https://github.com/wxxsfxyzm/MiuiBackGestureHook
 * Reference commit: 004bc059e5821775917b771d89aa1d8c18a1e669 (includes b6e582f and later fixes)
 * Copyright (C) 2026 MiuiBackGestureHook contributors.
 * Licensed under the Apache License, Version 2.0; modified for HyperCeiler.
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow;

import android.content.res.Configuration;
import android.os.Looper;
import android.view.View;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow.AppBarContentPreparation.call;
import static com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow.AppBarContentPreparation.read;
import static com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow.AppBarContentPreparation.write;

/** App-local panel/orientation state, with stock keyguard transition handling. */
final class AppBarStateBinding implements AutoCloseable {
    private final ClassLoader loader;
    private final Object injector;
    private final View view;
    private final List<Registration> registrations = new ArrayList<>();
    private Object controlCenterSubscription;
    private Set<Object> windowListeners;
    private Object windowListener;
    private Object[] originalWindowListeners;
    private Object windowController;
    private Method windowStateChanged;
    private boolean windowRegistered;
    private boolean closed;

    static AppBarStateBinding create(ClassLoader loader, Object injector) throws ReflectiveOperationException {
        requireMain();
        AppBarStateBinding result = new AppBarStateBinding(loader, injector);
        try { result.initialize(); return result; }
        catch (ReflectiveOperationException | RuntimeException | Error failure) {
            AppBarContentPreparation.cleanupCreation(result, failure);
            throw failure;
        }
    }

    private AppBarStateBinding(ClassLoader loader, Object injector) throws ReflectiveOperationException {
        this.loader = loader;
        this.injector = injector;
        view = (View) read(injector, "rootView");
    }

    private void initialize() throws ReflectiveOperationException {
        Object notification = read(injector, "notification");
        boolean expanded = (Boolean) read(notification, "expanded");
        state("mNotificationPanelExpandFlow", expanded);
        state("mOrientationFlow", view.getResources().getConfiguration().orientation);
        Object stretch = read(injector, "stretchAnimation");
        if (stretch != null) write(stretch, "notificationExpand", expanded);
        Object icons = call(read(injector, "mNotificationIconAreaInner"), "getInject");
        write(icons, "notificationPanelExpanded", expanded);
        Object status = read(injector, "statusBarStateControllerImpl");
        state("statusBarStateFlow", read(status, "mState"));

        Class<?> panel = type("com.android.systemui.plugins.miui.shade.PanelExpandController$Callback");
        Object callback = proxy(panel, (method, args) -> {
            if (method.equals("onVisibleChanged")) panelChanged((Boolean) args[0]);
        });
        register(read(notification, "panelExpandController"), panel, callback);
        Class<?> configuration = type("com.android.systemui.statusbar.policy.ConfigurationController$ConfigurationListener");
        Object configCallback = proxy(configuration, (method, args) -> {
            if (method.equals("onConfigChanged")) {
                Configuration config = (Configuration) args[0];
                if (config != null) state("mOrientationFlow", config.orientation);
            }
        });
        // Global island maximum width remains Home-owned. App geometry will
        // consume its own device strategy when native View binding is complete.
        register(read(injector, "configurationController"), configuration, configCallback);
        register(status, type("com.android.systemui.plugins.statusbar.StatusBarStateController$StateListener"),
                read(injector, "statusBarStateCallback"));
        bindControlCenter();
        bindWindowState();
    }

    @SuppressWarnings("unchecked")
    private void bindWindowState() throws ReflectiveOperationException {
        windowController = read(injector, "statusBarWindowStateController");
        if (((Number) read(windowController, "thisDisplayId")).intValue() != 0)
            throw new IllegalStateException("App visibility requires physical display 0");
        Object listeners = read(windowController, "listeners");
        if (!(listeners instanceof Set<?>)) throw new IllegalStateException("Unknown native window listener storage");
        windowListeners = (Set<Object>) listeners;
        originalWindowListeners = windowListeners.toArray();
        windowListener = read(injector, "statusBarWindowStateListener");
        if (read(windowListener, "this$0") != injector || windowListeners.contains(windowListener))
            throw new IllegalStateException("App window listener already owned");
        windowStateChanged = type("com.android.systemui.statusbar.window.StatusBarWindowStateListener")
                .getMethod("onStatusBarWindowStateChanged", int.class);
        int state = ((Number) read(windowController, "windowState")).intValue();
        if (state < 0 || state > 2) throw new IllegalStateException("Unknown physical status-bar state " + state);
        // InsetsPolicy/StatusBarManagerService forward physical display IDs on
        // A17. Share that producer; only this injector's consumer is App-owned.
        windowRegistered = windowListeners.add(windowListener);
        if (!windowRegistered) throw new IllegalStateException("App window listener registration failed");
        windowStateChanged.invoke(windowListener, state);
        verifyWindowValue(state);
    }

    private void verifyWindowValue(int state) throws ReflectiveOperationException {
        boolean hidden = state == 2;
        Object icons = call(read(injector, "mNotificationIconAreaInner"), "getInject");
        if ((Boolean) read(injector, "mIsTempWindowHidden") != hidden
                || (Boolean) read(icons, "statusbarWindowHidden") != hidden)
            throw new IllegalStateException("App window state did not reach both local consumers");
    }

    void verifyWindowStateBinding() throws ReflectiveOperationException {
        requireMain();
        if (closed || view.isAttachedToWindow() || !windowRegistered)
            throw new IllegalStateException("Window consumer check requires detached App graph");
        int original = ((Number) read(windowController, "windowState")).intValue();
        // Exercise only the private consumer, never CommandQueue or its shared
        // controller. Preserve the producer and restore its current value.
        try {
            for (int state : new int[]{2, 0, 1}) {
                windowStateChanged.invoke(windowListener, state);
                verifyWindowValue(state);
            }
        } finally { windowStateChanged.invoke(windowListener, original); }
        if (((Number) read(windowController, "windowState")).intValue() != original)
            throw new IllegalStateException("Private window consumer changed physical producer");
        verifyWindowValue(original);
    }

    void verifyLiveWindowState() throws ReflectiveOperationException {
        requireMain();
        if (closed || !view.isAttachedToWindow() || !windowRegistered
                || !windowListeners.contains(windowListener))
            throw new IllegalStateException("Live App window consumer is not registered");
        int state = ((Number) read(windowController, "windowState")).intValue();
        if (state < 0 || state > 2) throw new IllegalStateException("Unknown physical window state");
        verifyWindowValue(state);
    }

    private void bindControlCenter() throws ReflectiveOperationException {
        Object flow = read(read(injector, "controlCenter"), "expandStateForStatusBar");
        boolean expanded = (Boolean) call(flow, "getValue");
        // Seed the detached graph from the same native StateFlow. A later attach
        // receives its latest value through the native CREATED collector.
        write(injector, "mControlPanelExpand", expanded);
        Object icons = call(read(injector, "mNotificationIconAreaInner"), "getInject");
        write(icons, "controlCenterPanelExpanded", expanded);
        Object privacy = read(read(injector, "fragmentDep"), "miuiHomePrivacyController");
        write(privacy, "mControlPanelExpand", expanded);
        type("kotlinx.coroutines.DisposableHandle").getMethod("dispose");
        Consumer<Object> consumer = value -> {
            if (closed) return;
            requireMain();
            if (!(value instanceof Boolean)) throw new IllegalStateException("Invalid native control-center state");
            try { controlCenterChanged((Boolean) value); }
            catch (ReflectiveOperationException failure) {
                throw new IllegalStateException("App control-center state failed", failure);
            }
        };
        controlCenterSubscription = type("com.android.systemui.util.kotlin.JavaAdapterKt")
                .getMethod("collectFlow$default", View.class, type("kotlinx.coroutines.flow.Flow"),
                        Consumer.class, type("kotlin.coroutines.CoroutineContext"), int.class)
                .invoke(null, view, flow, consumer, null, 24);
    }

    private void controlCenterChanged(boolean expanded) throws ReflectiveOperationException {
        if ((Boolean) read(injector, "mControlPanelExpand") == expanded) return;
        write(injector, "mControlPanelExpand", expanded);
        // Preserve the native consumer's local effects. Home remains the writer
        // of shared animatorController.controlPanelExpand, just as it remains
        // the publisher of island layout/notification state.
        Object privacy = read(read(injector, "fragmentDep"), "miuiHomePrivacyController");
        if ((Boolean) read(privacy, "mControlPanelExpand") != expanded) {
            write(privacy, "mControlPanelExpand", expanded);
            if (expanded) {
                Object state = read(privacy, "mPrivacyState");
                Class<?> stateType = type("com.android.systemui.statusbar.phone.MiuiHomePrivacyController$PrivacyState");
                if (state == stateType.getField("START_SHOW_PRIVACY").get(null)
                        || state == stateType.getField("COMPLETE_SHOW_PRIVACY").get(null)
                        || state == stateType.getField("START_HOME_TO_DOT").get(null))
                    call(privacy, "showPrivacyNotAnimate");
                else if (state == stateType.getField("START_HIDE_PRIVACY").get(null))
                    call(privacy, "hidePrivacyNotAnimate");
            }
        }
        if (expanded) resetHover();
        write(call(read(injector, "mNotificationIconAreaInner"), "getInject"), "controlCenterPanelExpanded", expanded);
        call(injector, "updateSystemInfoIconVisibilities");
    }

    private void resetHover() throws ReflectiveOperationException {
        Object hover = read(view, "mHoverHelper");
        if (hover == null) return;
        Object event = read(hover, "eventHandler");
        write(event, "hoverWhere", 0);
        write(read(event, "hoverViewManager"), "hoverWhere", 0);
        call(read(hover, "viewManager"), "updateHoverViewBg", new Class<?>[]{String.class}, "config changed");
    }

    private void panelChanged(boolean expanded) throws ReflectiveOperationException {
        boolean previous = (Boolean) call(read(injector, "mNotificationPanelExpandFlow"), "getValue");
        if (previous == expanded) return;
        state("mNotificationPanelExpandFlow", expanded);
        Object stretch = read(injector, "stretchAnimation");
        if (stretch != null) write(stretch, "notificationExpand", expanded);
        // A16 App-role exclusion: never write islandController.isNotificationShowing.
        if (expanded) resetHover();
        write(call(read(injector, "mNotificationIconAreaInner"), "getInject"), "notificationPanelExpanded", expanded);
        if (view.getResources().getConfiguration().orientation != Configuration.ORIENTATION_PORTRAIT) return;
        if (expanded) {
            call(injector, "hideClock", new Class<?>[]{boolean.class}, false);
        } else if (!(Boolean) read(injector, "mIsUnlockingFromKeyguard")
                && (!(Boolean) read(read(injector, "keyguardStateController"), "mShowing")
                || ((Number) read(read(injector, "statusBarStateControllerImpl"), "mState")).intValue() != 1)) {
            call(injector, "showClock", new Class<?>[]{boolean.class}, false);
        }
    }

    private void state(String field, Object value) throws ReflectiveOperationException {
        call(read(injector, field), "updateState$1", new Class<?>[]{Object.class, Object.class}, null, value);
    }

    private interface Callback { void accept(String name, Object[] args) throws ReflectiveOperationException; }
    private Object proxy(Class<?> contract, Callback callback) {
        return Proxy.newProxyInstance(loader, new Class<?>[]{contract}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                if (method.getName().equals("equals")) return proxy == args[0];
                return "AppBarState:" + contract.getSimpleName();
            }
            if (!closed) { requireMain(); callback.accept(method.getName(), args); }
            return null;
        });
    }

    private void register(Object owner, Class<?> contract, Object listener) throws ReflectiveOperationException {
        Method remove = owner.getClass().getMethod("removeCallback", contract);
        Method add = owner.getClass().getMethod("addCallback", contract);
        remove.setAccessible(true);
        add.setAccessible(true);
        registrations.add(new Registration(owner, listener, remove));
        add.invoke(owner, listener);
    }

    int registrationCount() { return registrations.size(); }

    boolean controlCenterPrepared() { return !closed && controlCenterSubscription != null; }

    @Override public void close() throws ReflectiveOperationException {
        requireMain();
        closed = true;
        ReflectiveOperationException failure = null;
        if (windowRegistered) {
            windowListeners.remove(windowListener);
            windowRegistered = false;
            boolean same = windowListeners.size() == originalWindowListeners.length;
            for (Object original : originalWindowListeners) {
                boolean found = false;
                for (Object actual : windowListeners) if (actual == original) found = true;
                same &= found;
            }
            if (!same) failure = new ReflectiveOperationException("Physical window listener owners changed");
        }
        if (controlCenterSubscription != null) {
            try {
                type("kotlinx.coroutines.DisposableHandle").getMethod("dispose").invoke(controlCenterSubscription);
                controlCenterSubscription = null;
            } catch (ReflectiveOperationException cause) {
                if (failure == null) failure = cause; else failure.addSuppressed(cause);
            }
        }
        for (int i = registrations.size() - 1; i >= 0; i--) {
            Registration registration = registrations.get(i);
            try {
                registration.remove.invoke(registration.owner, registration.listener);
                registrations.remove(i);
            } catch (ReflectiveOperationException cause) {
                if (failure == null) failure = cause; else failure.addSuppressed(cause);
            }
        }
        if (failure != null) throw failure;
    }

    private static final class Registration {
        final Object owner;
        final Object listener;
        final Method remove;
        Registration(Object owner, Object listener, Method remove) {
            this.owner = owner; this.listener = listener; this.remove = remove;
        }
    }
    private Class<?> type(String name) throws ClassNotFoundException { return Class.forName(name, false, loader); }
    private static void requireMain() {
        if (Looper.myLooper() != Looper.getMainLooper()) throw new IllegalStateException("App state requires main Looper");
    }
}
