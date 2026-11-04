/*
 * Derived from MiuiBackGestureHook status-bar-follow implementation.
 * Source: https://github.com/wxxsfxyzm/MiuiBackGestureHook
 * Reference commit: 004bc059e5821775917b771d89aa1d8c18a1e669 (includes b6e582f and later fixes)
 * Copyright (C) 2026 MiuiBackGestureHook contributors.
 * Licensed under the Apache License, Version 2.0; modified for HyperCeiler.
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow;

import android.os.Handler;
import android.os.Looper;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.IdentityHashMap;

/** Waits for native coroutine finally blocks through Job completion, without polling. */
final class AppBarJobCompletion {
    private final ClassLoader loader;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final IdentityHashMap<Object, Boolean> jobs = new IdentityHashMap<>();
    private Runnable drained;

    AppBarJobCompletion(ClassLoader loader) { this.loader = loader; }

    void observeLifecycle(Object lifecycleOwner) throws ReflectiveOperationException {
        requireMain();
        Class<?> contextType = type("kotlin.coroutines.CoroutineContext");
        // The native binder launches siblings in lifecycleScope and returns
        // immediately. Its invocation's Job does not own those collectors.
        Object scope = type("androidx.lifecycle.LifecycleOwnerKt").getMethod("getLifecycleScope",
                type("androidx.lifecycle.LifecycleOwner")).invoke(null, lifecycleOwner);
        Object context = type("kotlinx.coroutines.CoroutineScope").getMethod("getCoroutineContext").invoke(scope);
        Class<?> jobType = type("kotlinx.coroutines.Job");
        Class<?> keyType = type("kotlinx.coroutines.Job$Key");
        Object key = null;
        for (Class<?> owner : new Class<?>[]{jobType, keyType}) for (Field field : owner.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == keyType) {
                field.setAccessible(true);
                Object value = field.get(null);
                if (value == null || (key != null && key != value)) throw new IllegalStateException("Ambiguous native Job key");
                key = value;
            }
        }
        if (key == null) throw new IllegalStateException("Native Job key missing");
        Object job = contextType.getMethod("get", type("kotlin.coroutines.CoroutineContext$Key")).invoke(context, key);
        if (job == null) throw new IllegalStateException("Native attached coroutine has no Job");
        if (jobs.containsKey(job)) return;
        Class<?> function = type("kotlin.jvm.functions.Function1");
        Object unit = type("kotlin.Unit").getField("INSTANCE").get(null);
        Object complete = Proxy.newProxyInstance(loader, new Class<?>[]{function}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                if (method.getName().equals("equals")) return proxy == args[0];
                return "AppBarJobCompletion";
            }
            if (!method.getName().equals("invoke")) throw new IllegalStateException("Unknown Job callback");
            main.post(() -> {
                jobs.remove(job);
                if (jobs.isEmpty() && drained != null) {
                    Runnable ready = drained;
                    drained = null;
                    ready.run();
                }
            });
            return unit;
        });
        jobs.put(job, true);
        try { jobType.getMethod("invokeOnCompletion", function).invoke(job, complete); }
        catch (ReflectiveOperationException failure) { jobs.remove(job); throw failure; }
    }

    boolean pending() { requireMain(); return !jobs.isEmpty(); }
    void whenDrained(Runnable callback) {
        requireMain();
        if (jobs.isEmpty()) main.post(callback); else drained = callback;
    }
    private Class<?> type(String name) throws ClassNotFoundException { return Class.forName(name, false, loader); }
    private static void requireMain() {
        if (Looper.myLooper() != Looper.getMainLooper()) throw new IllegalStateException("Job drain requires Main");
    }
}
