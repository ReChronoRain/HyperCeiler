/*
 * Derived from MiuiBackGestureHook status-bar-follow implementation.
 * Source: https://github.com/wxxsfxyzm/MiuiBackGestureHook
 * Reference commit: 004bc059e5821775917b771d89aa1d8c18a1e669 (includes b6e582f and later fixes)
 * Copyright (C) 2026 MiuiBackGestureHook contributors.
 * Licensed under the Apache License, Version 2.0; modified for HyperCeiler.
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow;

import android.content.Context;
import android.os.Binder;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;

import static com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow.AppBarContentPreparation.call;
import static com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow.AppBarContentPreparation.read;
import static com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow.AppBarContentPreparation.write;

/**
 * A16 App-token window identity on A17's native window controller. Physical
 * resources still belong to display 0. The native injector gets an owned
 * Job so releasing this window cannot cancel the display/application scope.
 * Creation is detached; the content owner must bind before calling attach().
 */
final class AppBarWindow implements AutoCloseable {
    private static final String WINDOW = "com.android.systemui.statusbar.window.";
    private final ClassLoader loader;
    private final Object controller;
    private final Context context;
    private final WindowManager manager;
    private final ViewGroup root;
    private final Object configuration;
    private final Object insets;
    private final Object configurationListener;
    private final Object insetsListener;
    private final Class<?> jobType;
    private Object job;
    private Object parentJob;
    private Object unfoldProvider;
    private Object unfoldListener;
    private boolean windowAdded;
    private boolean configurationRegistered;
    private boolean insetsRegistered;
    private boolean closed;
    private View.OnAttachStateChangeListener probeListener;
    private final java.util.IdentityHashMap<WindowManager.LayoutParams, Integer> nativeFlags = new java.util.IdentityHashMap<>();

    static AppBarWindow create(ClassLoader loader, Object component, Object display, Context context)
            throws ReflectiveOperationException {
        requireMain();
        AppBarWindow result = new AppBarWindow(loader, component, display, context);
        try {
            result.initializeInjector(component);
            result.prepareLayout();
            result.registerUnfold(component);
            result.verifyDetached();
            return result;
        } catch (ReflectiveOperationException | RuntimeException | Error failure) {
            AppBarContentPreparation.cleanupCreation(result, failure);
            throw failure;
        }
    }

    private AppBarWindow(ClassLoader loader, Object component, Object display, Context context)
            throws ReflectiveOperationException {
        this.loader = loader;
        jobType = type("kotlinx.coroutines.Job");
        jobType.getMethod("cancel", CancellationException.class);
        Object global = read(component, "referenceGlobalRootComponentImpl");
        this.context = context;
        if (context.getDisplay() == null || context.getDisplay().getDisplayId() != 0)
            throw new IllegalStateException("App bar requires native default-display context");
        manager = context.getSystemService(WindowManager.class);
        configuration = call(display, "getStatusBarConfigurationController");
        insets = call(display, "getStatusBarContentInsetsProvider");
        controller = fresh(WINDOW + "StatusBarWindowControllerImpl");
        configurationListener = fresh(WINDOW + "StatusBarWindowControllerImpl$1");
        write(configurationListener, "this$0", controller);
        insetsListener = fresh(WINDOW + "StatusBarWindowControllerImpl$$ExternalSyntheticLambda5");
        write(insetsListener, "f$0", controller);
        // Resolve release contracts before registering any native owner.
        configuration.getClass().getMethod("removeCallback", type(
                "com.android.systemui.statusbar.policy.ConfigurationController$ConfigurationListener"));
        insets.getClass().getMethod("removeCallback", Object.class);

        // A17's optimized Dagger factory performs these assignments. Calling its
        // factory directly loses both the injector jobs and unfold-listener handle.
        // Keep native inflation and controller methods, with explicit ownership.
        root = (ViewGroup) LayoutInflater.from(context).inflate(resource("layout", "super_status_bar"), null);
        if (!root.getClass().getName().equals(WINDOW + "StatusBarWindowView"))
            throw new IllegalStateException("Unexpected native status-bar window layout");
        write(controller, "mBarHeight", -1);
        write(controller, "mCurrentState", fresh(WINDOW + "StatusBarWindowControllerImpl$State"));
        write(controller, "mInsetsSourceOwner", new Binder());
        write(controller, "mConfigurationListener", configurationListener);
        write(controller, "mContext", context);
        write(controller, "mDisplayId", 0);
        write(controller, "mWindowManager", manager);
        write(controller, "mStatusBarConfigurationController", configuration);
        write(controller, "mContentInsetsProvider", insets);
        write(controller, "mIWindowManager", provider(global, "provideIWindowManagerProvider"));
        write(controller, "mLogBuffer", provider(component, "provideWindowLogBufferProvider"));
        write(controller, "mMultiDisplayLogger", call(component, "multiDisplayStatusBarLogger"));
        write(controller, "mStatusBarWindowView", root);
        write(controller, "mLaunchAnimationContainer", root.requireViewById(
                resource("id", "status_bar_launch_animation_container")));
        write(controller, "mLpChanged", new WindowManager.LayoutParams());
    }

    private void initializeInjector(Object component) throws ReflectiveOperationException {
        Object global = read(component, "referenceGlobalRootComponentImpl");
        Object repository = provider(component, "provideDisplayCoroutineScopeRepositoryProvider");
        Object displayScope = call(repository, "get", new Class<?>[]{int.class}, 0);
        if (displayScope == null) throw new IllegalStateException("Default-display scope unavailable");
        Class<?> scopeType = type("kotlinx.coroutines.CoroutineScope");
        Class<?> contextType = type("kotlin.coroutines.CoroutineContext");
        Object parentContext = scopeType.getMethod("getCoroutineContext").invoke(displayScope);
        parentJob = contextJob(parentContext);
        if (parentJob == null || !(Boolean) jobType.getMethod("isActive").invoke(parentJob))
            throw new IllegalStateException("Default-display Job not active");
        // Use the exact no-argument factory called by this SystemUI's native
        // ContextualEducationModule, not the duplicate unoptimized coroutine
        // artifact also present in the research workspace.
        job = type("kotlinx.coroutines.SupervisorKt").getMethod("SupervisorJob$default").invoke(null);
        Object mainContext = provider(global, "mainCoroutineContextProvider");
        Object ownedContext = contextType.getMethod("plus", contextType).invoke(mainContext, job);
        Class<?> injectorType = type("com.miui.systemui.statusbar.window.StatusBarWindowControllerImplInjector");
        Class<?> configType = type("com.miui.systemui.common.ui.data.repository.MiuiConfigurationRepositoryImpl");
        Object injector;
        try {
            injector = injectorType.getConstructor(controller.getClass(), Context.class,
                    repository.getClass(), scopeType, contextType, configType)
                    .newInstance(controller, context, repository, provider(global, "applicationScopeProvider"),
                            ownedContext, provider(component, "provideGlobalConfigRepositoryProvider2"));
        } catch (NoSuchMethodException legacy) {
            injector = injectorType.getConstructor(controller.getClass(), Context.class, repository.getClass(),
                    scopeType, contextType, type("com.android.systemui.statusbar.OneHandedModeRepository"), configType)
                    .newInstance(controller, context, repository, provider(global, "applicationScopeProvider"),
                            ownedContext, provider(component, "oneHandedModeRepositoryProvider"),
                            provider(component, "provideGlobalConfigRepositoryProvider2"));
        }
        write(controller, "mInjector", injector);
        Object actualContext = scopeType.getMethod("getCoroutineContext").invoke(read(injector, "scope"));
        if (contextJob(actualContext) != job || job == parentJob)
            throw new IllegalStateException("Native window injector did not adopt its owned Job");
        call(controller, "refreshStatusBarHeight");
    }

    private Object contextJob(Object context) throws ReflectiveOperationException {
        // SystemUI's optimized coroutine artifact keeps the singleton on the
        // nested Key, while another loaded artifact exposes Job.Key. Resolve
        // the one exact Job$Key singleton in this process's class loader.
        Class<?> keyType = type("kotlinx.coroutines.Job$Key");
        Object key = null;
        for (Class<?> owner : new Class<?>[]{jobType, keyType}) {
            for (Field field : owner.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) || field.getType() != keyType) continue;
                field.setAccessible(true);
                Object candidate = field.get(null);
                if (candidate == null || (key != null && key != candidate))
                    throw new IllegalStateException("Ambiguous native Job key");
                key = candidate;
            }
        }
        if (key == null) throw new IllegalStateException("Native Job key unavailable");
        return type("kotlin.coroutines.CoroutineContext").getMethod("get",
                type("kotlin.coroutines.CoroutineContext$Key")).invoke(context, key);
    }

    private void registerUnfold(Object component) throws ReflectiveOperationException {
        Object global = read(component, "referenceGlobalRootComponentImpl");
        Optional<?> optional = (Optional<?>) provider(global, "unfoldTransitionProgressProvider");
        if (!optional.isPresent()) return;
        Object target = optional.get();
        target.getClass().getMethod("removeCallback", Object.class);
        Object viewProvider = fresh(WINDOW + "StatusBarWindowControllerImpl$$ExternalSyntheticLambda4");
        write(viewProvider, "f$0", controller);
        Object listener = fresh("com.android.systemui.unfold.util.JankMonitorTransitionProgressListener");
        write(listener, "attachedViewProvider", viewProvider);
        write(listener, "interactionJankMonitor", type("com.android.internal.jank.InteractionJankMonitor")
                .getMethod("getInstance").invoke(null));
        unfoldProvider = target;
        unfoldListener = listener;
        call(target, "addCallback", new Class<?>[]{Object.class}, listener);
    }

    private WindowManager.LayoutParams params(int rotation) throws ReflectiveOperationException {
        WindowManager.LayoutParams result = (WindowManager.LayoutParams) call(controller,
                "getBarLayoutParamsForRotation", new Class<?>[]{int.class}, rotation);
        // Exact A16 index-1 token identity; preserve native flags, inset source
        // ownership, rotation geometry, package, cutout policy and Binder token.
        result.type = 2041;
        result.setTitle("StatusBar1");
        return result;
    }

    private void prepareLayout() throws ReflectiveOperationException {
        WindowManager.LayoutParams lp = params(context.getDisplay().getRotation());
        WindowManager.LayoutParams[] rotations = new WindowManager.LayoutParams[4];
        for (int i = 0; i < rotations.length; i++) rotations[i] = params(i);
        WindowManager.LayoutParams.class.getField("paramsForRotation").set(lp, rotations);
        write(controller, "mLp", lp);
        ((WindowManager.LayoutParams) read(controller, "mLpChanged")).copyFrom(lp);
    }

    ViewGroup root() { return root; }

    Object controller() { return controller; }

    void attachProbe(Runnable attached) throws ReflectiveOperationException {
        verifyDetached();
        WindowManager.LayoutParams lp = (WindowManager.LayoutParams) read(controller, "mLp");
        makeProbeParams(lp);
        WindowManager.LayoutParams[] rotations = (WindowManager.LayoutParams[])
                WindowManager.LayoutParams.class.getField("paramsForRotation").get(lp);
        for (WindowManager.LayoutParams rotation : rotations) makeProbeParams(rotation);
        ((WindowManager.LayoutParams) read(controller, "mLpChanged")).copyFrom(lp);
        probeListener = new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View view) {
                root.removeOnAttachStateChangeListener(this);
                probeListener = null;
                // Let the native child attachment dispatch finish before inspection/teardown.
                new android.os.Handler(Looper.getMainLooper()).post(() -> {
                    if (!closed && windowAdded) attached.run();
                });
            }
            @Override public void onViewDetachedFromWindow(View view) { }
        };
        root.addOnAttachStateChangeListener(probeListener);
        attach();
    }

    private void makeProbeParams(WindowManager.LayoutParams params) {
        nativeFlags.put(params, params.flags);
        params.alpha = 0f;
        params.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
    }

    void promote() throws ReflectiveOperationException {
        verifyProbeAttached();
        WindowManager.LayoutParams lp = (WindowManager.LayoutParams) read(controller, "mLp");
        for (java.util.Map.Entry<WindowManager.LayoutParams, Integer> entry : nativeFlags.entrySet()) {
            entry.getKey().flags = entry.getValue();
            entry.getKey().alpha = 1f;
        }
        ((WindowManager.LayoutParams) read(controller, "mLpChanged")).copyFrom(lp);
        manager.updateViewLayout(root, lp);
    }

    void verifyProbeAttached() throws ReflectiveOperationException {
        requireMain();
        WindowManager.LayoutParams lp = (WindowManager.LayoutParams) read(controller, "mLp");
        if (closed || !windowAdded || !root.isAttachedToWindow() || root.getWindowToken() == null
                || root.getDisplay() == null || root.getDisplay().getDisplayId() != 0
                || lp.type != 2041 || lp.alpha != 0f || !"StatusBar1".contentEquals(lp.getTitle())
                || (lp.flags & WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) == 0)
            throw new IllegalStateException("App window attachment proof unavailable");
    }

    void bindContent(View content) throws ReflectiveOperationException {
        verifyDetached();
        ViewGroup container = root.requireViewById(resource("id", "status_bar_container"));
        if (content.getParent() != null || container.getChildCount() != 0)
            throw new IllegalStateException("App window content already owned");
        container.addView(content, new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    Object scope() throws ReflectiveOperationException {
        requireMain();
        if (closed) throw new IllegalStateException("App window already closed");
        return read(read(controller, "mInjector"), "scope");
    }

    void verifyDetached() throws ReflectiveOperationException {
        requireMain();
        if (closed || windowAdded || root.isAttachedToWindow() || root.getParent() != null
                || (Boolean) read(controller, "mIsAttached"))
            throw new IllegalStateException("App window is not detached");
        WindowManager.LayoutParams lp = (WindowManager.LayoutParams) read(controller, "mLp");
        if (lp.type != 2041 || !"StatusBar1".contentEquals(lp.getTitle()))
            throw new IllegalStateException("App window token role was lost");
    }

    // The content owner must complete native binding and role gates first.
    // Do not create an empty window from the allocation verification path.
    void attach() throws ReflectiveOperationException {
        verifyDetached();
        View content = root.findViewById(resource("id", "status_bar"));
        if (content == null || !(Boolean) read(content, "mInited") || read(content, "mDependence") == null)
            throw new IllegalStateException("App status-bar content has not been bound");
        call(root, "setStatusBarConfigurationController", new Class<?>[]{type(
                "com.android.systemui.statusbar.data.repository.StatusBarConfigurationController")}, configuration);
        try {
            manager.addView(root, (WindowManager.LayoutParams) read(controller, "mLp"));
            windowAdded = true;
            configurationRegistered = true;
            call(configuration, "addCallback", new Class<?>[]{type(
                    "com.android.systemui.statusbar.policy.ConfigurationController$ConfigurationListener")}, configurationListener);
            insetsRegistered = true;
            call(insets, "addCallback", new Class<?>[]{Object.class}, insetsListener);
            call(controller, "calculateStatusBarLocationsForAllRotations");
            write(controller, "mIsAttached", true);
            Object state = read(controller, "mCurrentState");
            call(controller, "apply", new Class<?>[]{state.getClass()}, state);
        } catch (ReflectiveOperationException | RuntimeException | Error failure) {
            // addView can throw after ViewRoot publication. Account for that
            // partial attachment so close still removes this exact root.
            windowAdded |= root.getParent() != null;
            try { close(); }
            catch (Throwable cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }

    @Override public void close() throws ReflectiveOperationException {
        requireMain();
        if (closed) return;
        if (probeListener != null) {
            root.removeOnAttachStateChangeListener(probeListener);
            probeListener = null;
        }
        List<Throwable> failures = new ArrayList<>();
        attempt(failures, () -> { if (job != null) jobType.getMethod("cancel", CancellationException.class)
                .invoke(job, new CancellationException("App status-bar window disposed")); });
        attempt(failures, () -> write(controller, "mIsAttached", false));
        attempt(failures, () -> { if (unfoldProvider != null) {
            call(unfoldProvider, "removeCallback", new Class<?>[]{Object.class}, unfoldListener);
            unfoldProvider = unfoldListener = null;
        }});
        attempt(failures, () -> { if (insetsRegistered) {
            call(insets, "removeCallback", new Class<?>[]{Object.class}, insetsListener);
            insetsRegistered = false;
        }});
        attempt(failures, () -> { if (configurationRegistered) {
            call(configuration, "removeCallback", new Class<?>[]{type(
                    "com.android.systemui.statusbar.policy.ConfigurationController$ConfigurationListener")}, configurationListener);
            configurationRegistered = false;
        }});
        attempt(failures, () -> { if (windowAdded) { manager.removeViewImmediate(root); windowAdded = false; }});
        attempt(failures, () -> {
            if (parentJob != null && !(Boolean) jobType.getMethod("isActive").invoke(parentJob))
                throw new IllegalStateException("Default-display Job no longer active");
        });
        if (!failures.isEmpty()) {
            ReflectiveOperationException failure = new ReflectiveOperationException("App window cleanup incomplete");
            for (Throwable cause : failures) failure.addSuppressed(cause);
            throw failure;
        }
        closed = true;
    }

    private int resource(String kind, String name) {
        int id = context.getResources().getIdentifier(name, kind, "com.android.systemui");
        if (id == 0) throw new IllegalStateException("Missing native window resource " + kind + "/" + name);
        return id;
    }
    private Class<?> type(String name) throws ClassNotFoundException { return Class.forName(name, false, loader); }
    private Object fresh(String name) throws ReflectiveOperationException {
        Class<?> target = type(name);
        // A17 R8 removes these trivial constructors entirely (the native
        // Dagger factory emits allocation + field stores). Never skip a real
        // constructor, or apply constructor-free allocation to an unknown type.
        if (target.getDeclaredConstructors().length == 0) {
            if (!name.equals(WINDOW + "StatusBarWindowControllerImpl")
                    && !name.equals(WINDOW + "StatusBarWindowControllerImpl$State")
                    && !name.equals(WINDOW + "StatusBarWindowControllerImpl$1")
                    && !name.equals(WINDOW + "StatusBarWindowControllerImpl$$ExternalSyntheticLambda4")
                    && !name.equals(WINDOW + "StatusBarWindowControllerImpl$$ExternalSyntheticLambda5")
                    && !name.equals("com.android.systemui.unfold.util.JankMonitorTransitionProgressListener"))
                throw new IllegalStateException("Unverified constructor-free window type " + name);
            Class<?> unsafeType = type("sun.misc.Unsafe");
            Object unsafe = unsafeType.getMethod("getUnsafe").invoke(null);
            return unsafeType.getMethod("allocateInstance", Class.class).invoke(unsafe, target);
        }
        Constructor<?> constructor = target.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }
    private static Object provider(Object owner, String field) throws ReflectiveOperationException {
        return call(read(owner, field), "get");
    }
    private interface Cleanup { void run() throws ReflectiveOperationException; }
    private static void attempt(List<Throwable> failures, Cleanup cleanup) {
        try { cleanup.run(); }
        catch (ReflectiveOperationException | RuntimeException failure) { failures.add(failure); }
    }
    private static void requireMain() {
        if (Looper.myLooper() != Looper.getMainLooper()) throw new IllegalStateException("App window requires main Looper");
    }
}
