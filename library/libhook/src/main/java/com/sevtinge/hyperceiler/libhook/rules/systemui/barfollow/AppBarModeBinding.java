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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow.AppBarContentPreparation.call;
import static com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow.AppBarContentPreparation.read;

/** A16 index-1 appearance repository and light controller, using native A17 implementations. */
final class AppBarModeBinding implements AutoCloseable {
    private static volatile AppBarModeBinding activeAppearance;
    static final int RAW_DISPLAY_ID = 1 << 24;
    private final ClassLoader loader;
    private final Object component;
    private final Object display;
    private final Object scope;
    private final Object dark;
    private final Object dump;
    private final Object nativeComponent;
    private final View view;
    private final AppBarModeSession session;
    private Object repository;
    private Object light;
    private Object battery;
    private Object navigation;
    private boolean repositoryStarted;
    private boolean lightStarted;
    private final List<Object> bindings = new ArrayList<>();
    private boolean closing;
    private Object transitions;
    private Object power;
    private Object windowState;
    private Object extension;
    private Object demo;
    private Field modeField;
    private android.graphics.drawable.Drawable background;
    private Field backgroundAnimating;

    static AppBarModeBinding create(ClassLoader loader, Object component, Object display,
            Object scope, Object dark, Object dump, Object nativeComponent, View view,
            Consumer<RuntimeException> onFailure)
            throws ReflectiveOperationException {
        requireMain();
        AppBarModeBinding result = new AppBarModeBinding(loader, component, display, scope,
                dark, dump, nativeComponent, view, onFailure);
        try { result.initialize(); return result; }
        catch (ReflectiveOperationException | RuntimeException | Error failure) {
            AppBarContentPreparation.cleanupCreation(result, failure);
            throw failure;
        }
    }

    private AppBarModeBinding(ClassLoader loader, Object component, Object display, Object scope,
            Object dark, Object dump, Object nativeComponent, View view,
            Consumer<RuntimeException> onFailure) {
        this.loader = loader;
        this.component = component;
        this.display = display;
        this.scope = scope;
        this.dark = dark;
        this.dump = dump;
        this.nativeComponent = nativeComponent;
        this.view = view;
        session = new AppBarModeSession(this::applyMode, onFailure);
    }

    private void initialize() throws ReflectiveOperationException {
        Object queue = provider(component, "provideCommandQueueProvider");
        Object letterbox = provider(component, "letterboxAppearanceCalculatorProvider");
        Class<?> modeType = type("com.android.systemui.statusbar.data.repository.StatusBarModePerDisplayRepositoryImpl");
        repository = modeType.getConstructor(type("kotlinx.coroutines.CoroutineScope"), int.class,
                queue.getClass(), letterbox.getClass(), type("com.android.systemui.dump.DumpManager"))
                .newInstance(scope, RAW_DISPLAY_ID, queue, letterbox, dump);
        Object callback = read(repository, "commandQueueCallback");
        if (((Number) read(callback, "$thisDisplayId")).intValue() != RAW_DISPLAY_ID)
            throw new IllegalStateException("App appearance callback lost its encoded role");
        call(repository, "onStatusBarViewInitialized", new Class<?>[]{type(
                "com.android.systemui.statusbar.phone.fragment.dagger.HomeStatusBarComponent")}, nativeComponent);
        repositoryStarted = true;
        call(repository, "start");

        battery = provider(component, "provideBatteryControllerProvider");
        navigation = provider(component, "navigationModeControllerProvider");
        Object global = read(component, "referenceGlobalRootComponentImpl");
        Object mainContext = provider(global, "mainCoroutineContextProvider");
        Object biometric = provider(component, "biometricUnlockControllerProvider");
        // Resolve release before start publishes callbacks. Scope-owned native
        // collectors terminate when AppBarWindow cancels the content Job.
        battery.getClass().getMethod("removeCallback", Object.class);
        navigation.getClass().getMethod("removeListener", type(
                "com.android.systemui.navigationbar.NavigationModeController$ModeChangedListener"));
        dump.getClass().getMethod("unregisterDumpable", String.class);
        Class<?> lightType = type("com.android.systemui.statusbar.phone.LightBarControllerImpl");
        Constructor<?> constructor = null;
        for (Constructor<?> candidate : lightType.getDeclaredConstructors()) {
            Class<?>[] p = candidate.getParameterTypes();
            if (p.length != 10 || p[0] != int.class
                    || p[1] != type("kotlinx.coroutines.CoroutineScope")
                    || p[2] != type("com.android.systemui.plugins.DarkIconDispatcher")
                    || !p[3].isInstance(battery) || !p[4].isInstance(navigation)
                    || p[5] != type("com.android.systemui.statusbar.data.repository.StatusBarModePerDisplayRepository")
                    || p[6] != type("com.android.systemui.dump.DumpManager")
                    || p[7] != type("kotlin.coroutines.CoroutineContext") || !p[9].isInstance(biometric)) continue;
            if (constructor != null) throw new NoSuchMethodException("Ambiguous native light controller");
            constructor = candidate;
        }
        if (constructor == null) throw new NoSuchMethodException("Native light controller constructor");
        Object injectorFactory = AppBarNativeContracts.provider(component, constructor.getParameterTypes()[8]);
        light = constructor.newInstance(RAW_DISPLAY_ID, scope, dark, battery, navigation,
                repository, dump, mainContext, injectorFactory, biometric);
        if (activeAppearance != null) throw new IllegalStateException("App appearance owner already exists");
        activeAppearance = this;
        lightStarted = true;
        call(light, "start");

        // A17 InsetsPolicy still emits physical-display transient state. Keep
        // that producer shared; application AppearanceRegion packets stay raw-ID scoped.
        Object physical = provider(display, "statusBarModePerDisplayRepositoryImplProvider");
        collect(read(physical, "isTransientShown"), value -> setFlow("_isTransientShown", value));
        transitions = call(nativeComponent, "getPhoneStatusBarTransitions");
        power = provider(component, "powerInteractorProvider");
        windowState = provider(display, "statusBarWindowStateControllerProvider");
        extension = provider(component, "centralSurfacesExtensionImplProvider");
        Object central = provider(component, "centralSurfacesImplProvider");
        demo = inherited(central, "mDemoModeController");
        modeField = transitions.getClass().getSuperclass().getDeclaredField("mMode");
        modeField.setAccessible(true);
        background = (android.graphics.drawable.Drawable) inherited(transitions, "mBarBackground");
        backgroundAnimating = background.getClass().getDeclaredField("mAnimating");
        backgroundAnimating.setAccessible(true);
        // PowerInteractor.isAwake is a derived Flow, not a StateFlow. Both
        // inputs must emit before a mode can use a real wakefulness snapshot.
        collect(read(power, "isAwake"), session::awake);
        collect(read(repository, "statusBarMode"), session::mode);
        collect(read(power, "isAsleep"), this::finishBackgroundWhenAsleep);
    }

    private void collect(Object flow, Consumer<Object> consumer) throws ReflectiveOperationException {
        Consumer<Object> guarded = session.guard(value -> {
            if (!closing) { requireMain(); consumer.accept(value); }
        });
        bindings.add(type("com.android.systemui.util.kotlin.JavaAdapterKt").getMethod("collectFlow$default",
                View.class, type("kotlinx.coroutines.flow.Flow"), Consumer.class,
                type("kotlin.coroutines.CoroutineContext"), int.class).invoke(null, view, flow, guarded, null, 24));
    }

    private void setFlow(String name, Object value) {
        try { call(read(repository, name), "setValue", new Class<?>[]{Object.class}, value); }
        catch (ReflectiveOperationException failure) { throw new IllegalStateException("App mode consumer changed", failure); }
    }

    private void applyMode(Object mode, boolean awake) {
        try {
            if ((Boolean) read(demo, "isInDemoMode")) return;
            int next = ((Number) call(mode, "toTransitionModeInt")).intValue();
            if (modeField.getInt(transitions) == next) return;
            boolean transientShown = (Boolean) call(read(repository, "isTransientShown"), "getValue");
            boolean hidden = ((Number) read(windowState, "windowState")).intValue() == 2;
            boolean notification = (Boolean) read(read(extension, "notification"), "expanded");
            boolean controlCenter = (Boolean) call(read(read(extension, "controlCenter"), "expandStateForStatusBar"), "getValue");
            modeField.setInt(transitions, next);
            call(transitions, "applyModeBackground", new Class<?>[]{int.class, boolean.class}, next,
                    awake && !transientShown && !hidden && !notification && !controlCenter);
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException("App background mode failed", failure); }
    }

    private void finishBackgroundWhenAsleep(Object asleep) {
        try {
            // Native StatusBarOrchestrator's barTransitionsAndDeviceAsleep
            // consumer ends only this background; no shared navigation work.
            if (Boolean.TRUE.equals(asleep) && backgroundAnimating.getBoolean(background)) {
                backgroundAnimating.setBoolean(background, false);
                background.invalidateSelf();
            }
        } catch (IllegalAccessException failure) {
            throw new IllegalStateException("App background sleep consumer failed", failure);
        }
    }

    private static Object inherited(Object owner, String name) throws ReflectiveOperationException {
        for (Class<?> type = owner.getClass(); type != null; type = type.getSuperclass()) {
            try {
                java.lang.reflect.Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(owner);
            } catch (NoSuchFieldException absent) { }
        }
        throw new NoSuchFieldException(name);
    }

    Object repository() { return repository; }

    static boolean ownsLightController(Object controller) {
        AppBarModeBinding owner = activeAppearance;
        return owner != null && !owner.closing && owner.light == controller
                && Looper.myLooper() == Looper.getMainLooper();
    }

    static void settlePendingTintBeforeUpdate(Object controller) throws ReflectiveOperationException {
        if (!ownsLightController(controller)) return;
        Object tint = read(activeAppearance.dark, "mTransitionsController");
        if (Boolean.TRUE.equals(read(tint, "mTintChangePending"))) {
            // Enabling the preference can leave a color queued by the native
            // mode. Let native code drain it before updateStatus computes and
            // immediately applies the current color, cancelling the old tint.
            call(tint, "checkForPendingTransitions");
        }
    }

    static String nativeAppearanceDiagnostic(Object controller) throws ReflectiveOperationException {
        AppBarModeBinding owner = activeAppearance;
        if (owner == null || owner.closing || owner.light != controller) return null;
        requireMain();
        Object injector = read(controller, "mInjector");
        Object sampler = read(injector, "regionSamplingInteractor");
        Object helper = read(sampler, "regionSamplingHelper");
        View sampledView = (View) read(helper, "mSampledView");
        return owner.diagnostic()
                + ", sampledDark=" + call(read(sampler, "isRegionDark"), "getValue")
                + ", samplingBounds=" + read(sampler, "samplingBounds")
                + ", luma=" + read(helper, "mCurrentMedianLuma")
                + ", previousLuma=" + read(helper, "mLastMedianLuma")
                + ", sampleRegistered=" + read(helper, "mSamplingListenerRegistered")
                + ", registeredBounds=" + read(helper, "mRegisteredSamplingBounds")
                + ", stopLayer=" + read(helper, "mRegisteredStopLayer")
                + ", samplingEnabled=" + read(helper, "mSamplingEnabled")
                + ", samplingWindowVisible=" + read(helper, "mWindowVisible")
                + ", samplingWindowBlur=" + read(helper, "mWindowHasBlurs")
                + ", sampledView=" + sampledView
                + ", sampledViewAttached=" + sampledView.isAttachedToWindow()
                + ", sampledViewAlpha=" + sampledView.getAlpha()
                + ", resamplePending=" + ((android.os.Handler) read(sampler, "resampleHandler"))
                        .hasCallbacks((Runnable) read(sampler, "resampleRunnable"))
                + ", night=" + call(read(read(injector, "configurationRepository"), "isNightMode"), "getValue")
                + ", keyguard=" + call(read(injector, "isOnKeyguard"), "getValue");
    }

    void observeAppearance(java.util.function.BiConsumer<String, Throwable> log) throws ReflectiveOperationException {
        Object physical = provider(display, "statusBarModePerDisplayRepositoryImplProvider");
        collect(read(physical, "_originalStatusBarAttributes"), value -> log.accept("BarAppearance Home packet=" + value, null));
        collect(read(repository, "_originalStatusBarAttributes"), value -> log.accept("BarAppearance App packet=" + value, null));
        collect(read(repository, "statusBarAppearance"), value -> log.accept("BarAppearance App resolved=" + value, null));
    }

    String diagnostic() throws ReflectiveOperationException {
        Object tint = read(dark, "mTransitionsController");
        return "appearance=" + call(read(repository, "statusBarAppearance"), "getValue")
                + ", dark=" + read(tint, "mDarkIntensity") + ", nextDark=" + read(tint, "mNextDarkIntensity")
                + ", transitionPending=" + read(tint, "mTransitionPending")
                + ", tintPending=" + read(tint, "mTintChangePending")
                + ", pendingDark=" + read(tint, "mPendingDarkIntensity")
                + ", appliedDark=" + read(dark, "mDarkIntensity")
                + ", iconTint=" + read(dark, "mIconTint")
                + ", tintAreas=" + read(dark, "mTintAreas")
                + ", viewAlpha=" + view.getAlpha() + ", visible=" + view.getVisibility();
    }

    @Override public void close() throws ReflectiveOperationException {
        requireMain();
        closing = true;
        session.close();
        if (activeAppearance == this) activeAppearance = null;
        ReflectiveOperationException failure = null;
        for (int i = bindings.size() - 1; i >= 0; i--) {
            try {
                type("kotlinx.coroutines.DisposableHandle").getMethod("dispose").invoke(bindings.get(i));
                bindings.remove(i);
            } catch (ReflectiveOperationException cause) {
                if (failure == null) failure = cause; else failure.addSuppressed(cause);
            }
        }
        if (failure != null) throw failure;
        if (lightStarted) {
            call(battery, "removeCallback", new Class<?>[]{Object.class}, light);
            call(navigation, "removeListener", new Class<?>[]{type(
                    "com.android.systemui.navigationbar.NavigationModeController$ModeChangedListener")}, light);
            call(dump, "unregisterDumpable", new Class<?>[]{String.class}, read(light, "mDumpableName"));
            lightStarted = false;
        }
        if (repositoryStarted) { call(repository, "stop"); repositoryStarted = false; }
    }

    private Object provider(Object owner, String name) throws ReflectiveOperationException { return call(read(owner, name), "get"); }
    private Class<?> type(String name) throws ClassNotFoundException { return Class.forName(name, false, loader); }
    private static void requireMain() {
        if (Looper.myLooper() != Looper.getMainLooper()) throw new IllegalStateException("App mode requires main Looper");
    }
}
