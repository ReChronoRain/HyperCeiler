/*
 * Derived from MiuiBackGestureHook status-bar-follow implementation.
 * Source: https://github.com/wxxsfxyzm/MiuiBackGestureHook
 * Reference commit: 004bc059e5821775917b771d89aa1d8c18a1e669 (includes b6e582f and later fixes)
 * Copyright (C) 2026 MiuiBackGestureHook contributors.
 * Licensed under the Apache License, Version 2.0; modified for HyperCeiler.
 */
package com.sevtinge.hyperceiler.libhook.rules.systemframework.statusbarfollow;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/** OS4 services callbacks verified against their allocation sites in JADX. */
final class ServerBarCallbackProfile {
    static final class Entry {
        final String id;
        final Constructor<?> constructor;
        final Method run;
        final Method producer;
        Entry(String id, Constructor<?> constructor, Method run, Method producer) {
            this.id = id;
            this.constructor = constructor;
            this.run = run;
            this.producer = producer;
        }
    }

    static List<Entry> resolve(ClassLoader loader) throws ReflectiveOperationException {
        Class<?> impl = Class.forName("com.android.server.wm.BarFollowAnimationImpl", false, loader);
        Class<?> controller = Class.forName("com.android.server.wm.MiuiStatusBarController", false, loader);
        Class<?> transition = Class.forName("com.android.server.wm.Transition", false, loader);
        Class<?> window = Class.forName("com.android.server.wm.WindowState", false, loader);
        Class<?> task = Class.forName("com.android.server.wm.Task", false, loader);
        Class<?> display = Class.forName("com.android.server.wm.DisplayContent", false, loader);
        List<Entry> entries = new ArrayList<>();
        entries.add(resolve("fold", impl, 10, new Class<?>[]{impl}, "lambda$systemReady$1", Boolean.class));
        entries.add(resolve("reset", impl, 5, new Class<?>[]{impl, transition, boolean.class, boolean.class},
                "resetBarState", transition));
        entries.add(resolve("window_added", impl, 12, new Class<?>[]{impl, window}, "onSystemWindowAdded", window));
        entries.add(resolve("window_removed", impl, 6, new Class<?>[]{impl, window}, "onSystemWindowRemoved", window));
        entries.add(resolve("task_changed", impl, 9, new Class<?>[]{impl, boolean.class, task},
                "onTaskInfoChanedForCaptionBar", task));
        entries.add(resolve("task_vanished", impl, 2, new Class<?>[]{impl}, "onTaskValishedForCaptionBar", task));
        entries.add(resolve("folme_init", controller, 0, new Class<?>[]{controller}, "init", display, android.os.Handler.class));
        return entries;
    }

    private static Entry resolve(String id, Class<?> owner, int ordinal, Class<?>[] captured,
            String producerName, Class<?>... arguments) throws ReflectiveOperationException {
        Class<?> callback = Class.forName(owner.getName() + "$$ExternalSyntheticLambda" + ordinal,
                false, owner.getClassLoader());
        Constructor<?> constructor = callback.getDeclaredConstructor(captured);
        Method run = callback.getDeclaredMethod("run");
        Method producer = owner.getDeclaredMethod(producerName, arguments);
        if (!Runnable.class.isAssignableFrom(callback) || run.getReturnType() != void.class
                || producer.getReturnType() != void.class || Modifier.isStatic(producer.getModifiers())
                || callback.getDeclaredConstructors().length != 1)
            throw new NoSuchMethodException("Native bar callback changed: " + id);
        int instanceFields = 0;
        for (Field field : callback.getDeclaredFields()) if (!Modifier.isStatic(field.getModifiers())) instanceFields++;
        if (instanceFields != captured.length) throw new NoSuchFieldException("Unexpected captures: " + id);
        for (int i = 0; i < captured.length; i++) {
            Field field = callback.getDeclaredField("f$" + i);
            if (field.getType() != captured[i] || Modifier.isStatic(field.getModifiers())
                    || !Modifier.isFinal(field.getModifiers()))
                throw new NoSuchFieldException("Native bar capture changed: " + id);
        }
        return new Entry(id, constructor, run, producer);
    }
}
