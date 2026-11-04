/*
 * Derived from MiuiBackGestureHook status-bar-follow implementation.
 * Source: https://github.com/wxxsfxyzm/MiuiBackGestureHook
 * Reference commit: 004bc059e5821775917b771d89aa1d8c18a1e669 (includes b6e582f and later fixes)
 * Copyright (C) 2026 MiuiBackGestureHook contributors.
 * Licensed under the Apache License, Version 2.0; modified for HyperCeiler.
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow.AppBarContentPreparation.call;
import static com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow.AppBarContentPreparation.read;
import static com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow.AppBarContentPreparation.write;

/** A16's per-binding privacy owner, using A17's native privacy implementation. */
final class AppBarPrivacyBinding implements AutoCloseable {
    private static final String PRIVACY = "com.android.systemui.statusbar.phone.MiuiHomePrivacyController";
    private static final String CALLBACK = "com.android.systemui.plugins.miui.statusbar.MiuiPrivacyController$MiuiPrivacyCallback";
    private final ClassLoader loader;
    private final View view;
    private final Object shared;
    private final Object primary;
    private final Object[] primaryViews;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Object controller;
    private LinearLayout privacy;
    private LinearLayout homePrivacy;
    private boolean closed;

    static AppBarPrivacyBinding create(ClassLoader loader, Object component, Object primaryView,
                                      View view, Object ownedScope) throws ReflectiveOperationException {
        requireMain();
        AppBarPrivacyBinding result = new AppBarPrivacyBinding(loader, primaryView, view);
        try {
            result.initialize(component, ownedScope);
            result.bindContainers();
            result.verifyPrimary();
            return result;
        } catch (ReflectiveOperationException | RuntimeException | Error failure) {
            AppBarContentPreparation.cleanupCreation(result, failure);
            throw failure;
        }
    }

    private AppBarPrivacyBinding(ClassLoader loader, Object primaryView, View view)
            throws ReflectiveOperationException {
        this.loader = loader;
        this.view = view;
        Object dependency = read(primaryView, "mDependence");
        shared = read(dependency, "miuiPrivacy");
        primary = read(dependency, "miuiHomePrivacy");
        primaryViews = containers(primary);
        shared.getClass().getMethod("removePrivacyCallback", type(CALLBACK));
        shared.getClass().getMethod("removePromptContainer", LinearLayout.class);
        type(PRIVACY).getMethod("initFolme$2");
        type(PRIVACY).getMethod("cancelFolme");
    }

    private void initialize(Object component, Object ownedScope) throws ReflectiveOperationException {
        Class<?> scopeType = type("kotlinx.coroutines.CoroutineScope");
        Class<?> adapterType = type("com.android.systemui.util.kotlin.JavaAdapter");
        Object adapter = adapterType.getConstructor(scopeType, scopeType).newInstance(ownedScope, ownedScope);
        Object observer = call(read(component, "statusBarIconObserverProvider"), "get");
        Class<?> observerType = type("com.android.systemui.statusbar.policy.StatusBarIconObserver");
        if (observer.getClass() != observerType) throw new IllegalStateException("Unexpected native icon observer");
        // Its alwaysCollectFlow is now owned by the App window's scope. The
        // constructor registers a privacy callback; recover by our private
        // Handler identity if initial publication throws before it returns.
        try {
            Constructor<?> constructor = type(PRIVACY).getConstructor(Handler.class,
                    shared.getClass(), adapterType, observerType);
            controller = constructor.newInstance(handler, shared, adapter, observer);
        } catch (NoSuchMethodException legacy) {
            Constructor<?> constructor = type(PRIVACY).getConstructor(Context.class, Handler.class,
                    shared.getClass(), adapterType, observerType);
            controller = constructor.newInstance(view.getContext(), handler, shared, adapter, observer);
        }
        if (controller == primary || read(controller, "mHandler") != handler)
            throw new IllegalStateException("Native privacy factory reused primary owner");
    }

    private void bindContainers() throws ReflectiveOperationException {
        privacy = (LinearLayout) read(view, "mPrivacyContainer");
        homePrivacy = (LinearLayout) read(view, "mHomePrivacyContainer");
        View battery = (View) read(view, "mBattery");
        View batteryContainer = (View) read(view, "mStatusBatteryContainer");
        if (privacy == null || homePrivacy == null || battery == null || batteryContainer == null)
            throw new IllegalStateException("Native App privacy containers unavailable");
        // addPromptContainer inflates the privacy children. Preserve native
        // initDependence ordering: registration precedes resolving those Views.
        call(shared, "addPromptContainer", new Class<?>[]{LinearLayout.class, int.class}, privacy, 1);
        call(shared, "addPromptContainer", new Class<?>[]{LinearLayout.class, int.class}, homePrivacy, 3);
        View mini = required(homePrivacy, "mini_state_container");
        View chip = required(homePrivacy, "privacy_chip");
        View dot = required(privacy, "privacy_dot_image");
        for (Object candidate : new Object[]{mini, chip, dot, battery, batteryContainer})
            for (Object original : primaryViews) if (candidate == original)
                throw new IllegalStateException("App privacy reused primary View");
        write(controller, "mBatteryMeterView", battery);
        write(controller, "mBatteryContainer", batteryContainer);
        write(controller, "mMiniStateContainer", mini);
        write(controller, "mPrivacyChip", chip);
        write(controller, "mDotView", dot);
        // Exact A17 initDependence container initialization, without its global
        // clock/control-center writes. These are native real Views, not copies.
        chip.setScaleX(0f);
        chip.setScaleY(0f);
        chip.setAlpha(0f);
        chip.setVisibility(View.VISIBLE);
        mini.setScaleX(0f);
        mini.setScaleY(0f);
        call(controller, "initFolme$2");
        write(controller, "mPrivacyState", type(PRIVACY + "$PrivacyState")
                .getField("COMPLETE_HIDE_PRIVACY").get(null));
        call(controller, "updateStatusBarDotViewVisibility");
    }

    Object controller() { return controller; }

    boolean applyWithoutAnimation() throws ReflectiveOperationException {
        requireMain();
        if (closed) return true;
        return applyWithoutAnimation(controller);
    }

    static boolean applyWithoutAnimation(Object controller) throws ReflectiveOperationException {
        requireMain();
        // A16 checks token/window eligibility only after the reduced-location
        // dot branch. Preserve that branch and all privacy data exactly.
        if (!(Boolean) read(controller, "mShowPrivacy")) {
            call(controller, "hidePrivacyNotAnimate");
            return true;
        }
        boolean onlyLocation = !(Boolean) read(controller, "mFirstUseLocationPrompt")
                && (Boolean) read(controller, "mOnlyUpdateLocation");
        if ((((Number) read(controller, "mLessLocationTip")).intValue() > 0
                && (onlyLocation || (Boolean) read(controller, "mReducePromptInfo")))
                || (Boolean) read(controller, "mSettingPrivacy")) return false;
        call(controller, "showPrivacyNotAnimate");
        return true;
    }

    void verifyPrimary() throws ReflectiveOperationException {
        Object[] actual = containers(primary);
        for (int i = 0; i < actual.length; i++) if (actual[i] != primaryViews[i])
            throw new IllegalStateException("Primary privacy container changed");
    }

    @Override public void close() throws ReflectiveOperationException {
        requireMain();
        if (closed) return;
        List<Throwable> failures = new ArrayList<>();
        attempt(failures, () -> {
            if (controller == null) for (Object candidate : new ArrayList<>(callbacks())) {
                if (candidate.getClass().getName().equals(PRIVACY) && read(candidate, "mHandler") == handler) {
                    if (controller != null) throw new IllegalStateException("Ambiguous owned privacy callback");
                    controller = candidate;
                }
            }
        });
        attempt(failures, () -> { if (controller != null)
            call(shared, "removePrivacyCallback", new Class<?>[]{type(CALLBACK)}, controller); });
        handler.removeCallbacksAndMessages(null);
        attempt(failures, () -> { if (privacy != null)
            call(shared, "removePromptContainer", new Class<?>[]{LinearLayout.class}, privacy); });
        attempt(failures, () -> { if (homePrivacy != null)
            call(shared, "removePromptContainer", new Class<?>[]{LinearLayout.class}, homePrivacy); });
        attempt(failures, () -> {
            if (controller == null) return;
            call(controller, "cancelFolme");
            for (String field : new String[]{"mMiniStateContainer", "mPrivacyChip", "mDotView",
                    "mBatteryMeterView", "mBatteryContainer"}) write(controller, field, null);
            // Native missing-container branch cancels and cleans the four
            // state animators. Never clean Folme belonging to the main bar.
            call(controller, "initFolme$2");
            for (String field : new String[]{"mMiniStateFolme", "mPrivacyChipFolme", "mDotFolme", "mBatteryFolme"})
                write(controller, field, null);
        });
        attempt(failures, () -> {
            for (Object callback : callbacks()) if (controller != null && callback == controller)
                throw new IllegalStateException("App privacy callback remains registered");
            for (Object container : prompts().keySet()) if (container == privacy || container == homePrivacy)
                throw new IllegalStateException("App privacy container remains registered");
        });
        if (!failures.isEmpty()) {
            ReflectiveOperationException failure = new ReflectiveOperationException("App privacy cleanup incomplete");
            for (Throwable cause : failures) failure.addSuppressed(cause);
            throw failure;
        }
        closed = true;
    }

    private View required(View parent, String name) {
        int id = view.getResources().getIdentifier(name, "id", "com.android.systemui");
        if (id == 0) throw new IllegalStateException("Missing native privacy resource " + name);
        return parent.requireViewById(id);
    }
    private static Object[] containers(Object owner) throws ReflectiveOperationException {
        return new Object[]{read(owner, "mMiniStateContainer"), read(owner, "mPrivacyChip"),
                read(owner, "mDotView"), read(owner, "mBatteryMeterView"), read(owner, "mBatteryContainer")};
    }
    private List<?> callbacks() throws ReflectiveOperationException { return (List<?>) read(shared, "mMiuiPrivacyCallbacks"); }
    @SuppressWarnings("unchecked")
    private Map<Object, Object> prompts() throws ReflectiveOperationException {
        return (Map<Object, Object>) read(shared, "mPromptContainers");
    }
    private Class<?> type(String name) throws ClassNotFoundException { return Class.forName(name, false, loader); }
    private interface Cleanup { void run() throws ReflectiveOperationException; }
    private static void attempt(List<Throwable> failures, Cleanup cleanup) {
        try { cleanup.run(); }
        catch (ReflectiveOperationException | RuntimeException failure) { failures.add(failure); }
    }
    private static void requireMain() {
        if (Looper.myLooper() != Looper.getMainLooper()) throw new IllegalStateException("App privacy requires main Looper");
    }
}
