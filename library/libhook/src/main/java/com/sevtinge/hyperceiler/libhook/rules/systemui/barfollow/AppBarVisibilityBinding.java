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

import static com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow.AppBarContentPreparation.read;
import static com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow.AppBarContentPreparation.write;

/** Owns the stock clock, notification and system-info CREATED collectors. */
final class AppBarVisibilityBinding implements AutoCloseable {
    private static final String INJECTOR = "com.android.systemui.statusbar.pipeline.shared.ui.binder.HomeStatusBarViewBinderInjector";
    private final ClassLoader loader;
    private final Object injector;
    private final View view;
    private final List<View> animatedViews = new ArrayList<>();
    private Object binding;
    private Object hydrationBinding;
    private boolean closed;

    static AppBarVisibilityBinding create(ClassLoader loader, Object injector, Object viewModel,
                                          Object eventInteractor) throws ReflectiveOperationException {
        requireMain();
        AppBarVisibilityBinding result = new AppBarVisibilityBinding(loader, injector);
        try { result.initialize(viewModel, eventInteractor); return result; }
        catch (ReflectiveOperationException | RuntimeException | Error failure) {
            AppBarContentPreparation.cleanupCreation(result, failure);
            throw failure;
        }
    }

    private AppBarVisibilityBinding(ClassLoader loader, Object injector) throws ReflectiveOperationException {
        this.loader = loader;
        this.injector = injector;
        view = (View) read(injector, "rootView");
        if (view.isAttachedToWindow()) throw new IllegalStateException("Visibility must bind before App attachment");
    }

    private void initialize(Object viewModel, Object eventInteractor) throws ReflectiveOperationException {
        String[][] fields = {{"mStatusBarContent", "status_bar_contents"},
                {"mStatusBarPrivacyContainer", "privacy_container"}, {"mStatusBarIcons", "status_bar_icons"},
                {"mStatusContainer", "statusIcons"}, {"mBatteryView", "battery"}, {"mClockView", "clock"},
                {"mBatteryContainer", "battery_icon_container"}, {"mCutoutSpace", "cutout_space_view"},
                {"mEndSideContent", "system_icons"}, {"mNotificationIconAreaInner", "notificationIcons"}};
        for (String[] field : fields) write(injector, field[0], child(field[1]));
        for (String id : new String[]{"clock", "notificationIcons", "system_icons", "privacy_container"})
            animatedViews.add(child(id));
        // Match bind()'s initial visibility; native collectors own subsequent changes.
        for (String id : new String[]{"status_bar_end_side_content", "clock", "notificationIcons"}) {
            View child = child(id);
            if (child.getVisibility() != View.INVISIBLE && child.getVisibility() != View.GONE) {
                child.setAlpha(0f);
                child.setVisibility(View.INVISIBLE);
            }
        }

        Class<?> function2 = type("kotlin.jvm.functions.Function2");
        Class<?> function3 = type("kotlin.jvm.functions.Function3");
        Class<?> continuation = type("kotlin.coroutines.Continuation");
        Class<?> context = type("kotlin.coroutines.CoroutineContext");
        Class<?> lifecycleOwner = type("androidx.lifecycle.LifecycleOwner");
        Class<?> state = type("androidx.lifecycle.Lifecycle$State");
        Object created = state.getField("CREATED").get(null);
        Object unit = type("kotlin.Unit").getField("INSTANCE").get(null);
        Object suspended = type("kotlin.coroutines.intrinsics.CoroutineSingletons").getField("COROUTINE_SUSPENDED").get(null);
        Object hydrator = viewModel.getClass().getField("hydrator").get(viewModel);
        Method activate = hydrator.getClass().getMethod("activate", continuation);
        Object hydrate = proxy(function3, args -> {
            if (closed) return unit;
            requireInitialized();
            // Hydrate only this ViewModel's delegates. The native onActivated
            // tail logs shared chip UI events; Home remains its single owner.
            Object result = invoke(activate, hydrator, args[2]);
            return activate.getReturnType() == void.class ? suspended : result;
        });
        hydrationBinding = type("com.android.systemui.lifecycle.RepeatWhenAttachedKt")
                .getMethod("repeatWhenAttached$default", View.class, context, function3, int.class)
                .invoke(null, view, null, hydrate, 3);
        Method repeat = type("androidx.lifecycle.RepeatOnLifecycleKt").getMethod("repeatOnLifecycle",
                lifecycleOwner, state, function2, continuation);
        Method launch = type("kotlinx.coroutines.BuildersKt").getMethod("launch$default",
                type("kotlinx.coroutines.CoroutineScope"), context, type("kotlinx.coroutines.CoroutineStart"),
                function2, int.class);
        Object[] collectors = {
                coroutine("$bindOperatorName$1", viewModel),
                coroutine("$bindClockVisibility$1", viewModel),
                coroutine("$bindNotificationIconVisibility$1", viewModel),
                localCoroutine("$bindMiuiReactive$1$1"),
                coroutine("$bindMiuiReactive$1$4", viewModel),
                systemInfoCoroutine(viewModel, eventInteractor)
        };
        Object block = proxy(function2, args -> {
            requireMain();
            if (closed) return unit;
            // Do not run an incompletely initialized graph if a caller attaches it prematurely.
            requireInitialized();
            for (Object collector : collectors) invoke(launch, null, args[0], null, null, collector, 3);
            AppBarContentPreparation.call(injector, "updateSystemInfoIconVisibilities");
            return unit;
        });
        Object attached = proxy(function3, args -> closed ? unit
                : invoke(repeat, null, args[0], created, block, args[2]));
        binding = type("com.android.systemui.lifecycle.RepeatWhenAttachedKt")
                .getMethod("repeatWhenAttached$default", View.class, context, function3, int.class)
                .invoke(null, view, null, attached, 3);
    }

    private Object coroutine(String suffix, Object viewModel) throws ReflectiveOperationException {
        Constructor<?> constructor = type(INJECTOR + suffix).getDeclaredConstructor(type(INJECTOR),
                type("com.android.systemui.statusbar.pipeline.shared.ui.viewmodel.HomeStatusBarViewModel"),
                type("kotlin.coroutines.Continuation"));
        constructor.setAccessible(true);
        return constructor.newInstance(injector, viewModel, null);
    }

    private Object localCoroutine(String suffix) throws ReflectiveOperationException {
        Constructor<?> constructor = type(INJECTOR + suffix).getDeclaredConstructor(type(INJECTOR),
                type("kotlin.coroutines.Continuation"));
        constructor.setAccessible(true);
        return constructor.newInstance(injector, null);
    }

    private void requireInitialized() throws ReflectiveOperationException {
        requireMain();
        if (!(Boolean) read(view, "mInited") || read(view, "mDependence") == null)
            throw new IllegalStateException("App visibility started before native View initialization");
    }

    private Object systemInfoCoroutine(Object viewModel, Object eventInteractor) throws ReflectiveOperationException {
        Class<?> function1 = type("kotlin.jvm.functions.Function1");
        Object unit = type("kotlin.Unit").getField("INSTANCE").get(null);
        Method enter = eventInteractor.getClass().getMethod("animateStatusBarContentForChipEnter", View.class);
        Method exit = eventInteractor.getClass().getMethod("animateStatusBarContentForChipExit", View.class);
        Object in = proxy(function1, args -> { invoke(enter, eventInteractor, args[0]); return unit; });
        Object out = proxy(function1, args -> { invoke(exit, eventInteractor, args[0]); return unit; });
        Constructor<?> constructor = type(INJECTOR + "$bindMiuiReactive$1$5").getDeclaredConstructor(type(INJECTOR),
                type("com.android.systemui.statusbar.pipeline.shared.ui.viewmodel.HomeStatusBarViewModel"),
                function1, function1, type("kotlin.coroutines.Continuation"));
        constructor.setAccessible(true);
        return constructor.newInstance(injector, viewModel, in, out, null);
    }

    int collectorCount() { return binding != null && hydrationBinding != null && !closed ? 6 : 0; }

    @Override public void close() throws ReflectiveOperationException {
        requireMain();
        closed = true;
        if (binding != null) {
            type("kotlinx.coroutines.DisposableHandle").getMethod("dispose").invoke(binding);
            binding = null;
        }
        if (hydrationBinding != null) {
            type("kotlinx.coroutines.DisposableHandle").getMethod("dispose").invoke(hydrationBinding);
            hydrationBinding = null;
        }
        // Native injector onUnbind clears shared Home island providers. Release
        // only this binding's view animations after its lifecycle is cancelled.
        for (View child : animatedViews) child.animate().cancel();
    }

    private interface Invocation { Object run(Object[] args) throws Throwable; }
    private Object proxy(Class<?> contract, Invocation invocation) {
        return Proxy.newProxyInstance(loader, new Class<?>[]{contract}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                if (method.getName().equals("equals")) return proxy == args[0];
                return "AppBarVisibility:" + contract.getSimpleName();
            }
            if (!method.getName().equals("invoke")) throw new IllegalStateException("Unexpected coroutine method " + method);
            return invocation.run(args);
        });
    }
    private static Object invoke(Method method, Object owner, Object... args) throws Throwable {
        try { return method.invoke(owner, args); }
        catch (InvocationTargetException failure) { throw failure.getCause(); }
    }
    private View child(String name) {
        int id = view.getResources().getIdentifier(name, "id", "com.android.systemui");
        if (id == 0) throw new IllegalStateException("Missing native App visibility view " + name);
        return view.requireViewById(id);
    }
    private Class<?> type(String name) throws ClassNotFoundException { return Class.forName(name, false, loader); }
    private static void requireMain() {
        if (Looper.myLooper() != Looper.getMainLooper()) throw new IllegalStateException("Visibility requires main Looper");
    }
}
