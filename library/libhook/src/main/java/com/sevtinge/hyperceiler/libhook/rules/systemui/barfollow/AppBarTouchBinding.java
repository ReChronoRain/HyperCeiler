/*
 * Derived from MiuiBackGestureHook status-bar-follow implementation.
 * Source: https://github.com/wxxsfxyzm/MiuiBackGestureHook
 * Reference commit: 004bc059e5821775917b771d89aa1d8c18a1e669 (includes b6e582f and later fixes)
 * Copyright (C) 2026 MiuiBackGestureHook contributors.
 * Licensed under the Apache License, Version 2.0; modified for HyperCeiler.
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow;

import android.graphics.Rect;
import android.graphics.Region;
import android.view.View;
import java.lang.reflect.Constructor;
import java.util.List;
import static com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow.AppBarContentPreparation.*;

/** Native AppHandles exclusion observer, with a separately disposable App attachment. */
final class AppBarTouchBinding implements AutoCloseable {
    private static final String BINDER = "com.android.systemui.statusbar.pipeline.shared.ui.binder.HomeStatusBarTouchExclusionRegionBinder";
    private final ClassLoader loader;
    private final View view;
    private final Object model;
    private final View.OnLayoutChangeListener layout;
    private Object attachment;

    AppBarTouchBinding(ClassLoader loader, View home, View view) throws ReflectiveOperationException {
        this.loader = loader;
        this.view = view;
        Object found = null;
        Object listeners = read(read(home, "mListenerInfo"), "mOnLayoutChangeListeners");
        if (listeners instanceof List<?>) for (Object listener : (List<?>) listeners) {
            if (!listener.getClass().getName().equals(BINDER + "$bind$2")) continue;
            if (read(listener, "$view") != home || found != null)
                throw new IllegalStateException("Ambiguous Home exclusion owner");
            found = read(listener, "$appHandlesViewModel");
        }
        if (found == null) throw new IllegalStateException("Active native AppHandles owner missing");
        model = found; // Home already hydrates this physical display's exclusion producer.
        Class<?> listenerType = type(BINDER + "$bind$2");
        layout = (View.OnLayoutChangeListener) captured(listenerType,
                new String[]{"$view", "$appHandlesViewModel"}, new Object[]{view, model});
        Constructor<?> block = type(BINDER + "$bind$1").getDeclaredConstructor(
                type("com.android.systemui.statusbar.phone.PhoneStatusBarView"), model.getClass(),
                type("kotlin.coroutines.Continuation"));
        block.setAccessible(true);
        attachment = type("com.android.systemui.lifecycle.RepeatWhenAttachedKt")
                .getMethod("repeatWhenAttached$default", View.class, type("kotlin.coroutines.CoroutineContext"),
                        type("kotlin.jvm.functions.Function3"), int.class)
                .invoke(null, view, null, block.newInstance(view, model, null), 3);
        view.addOnLayoutChangeListener(layout);
    }

    String verify(Object controller) throws ReflectiveOperationException {
        if (!view.isAttachedToWindow()) throw new IllegalStateException("Touch proof requires attachment");
        Object handler = read(view, "mTouchEventHandler");
        if (handler == null || read(handler, "phoneStatusBarViewController") != controller
                || read(handler, "shadeTouchHandler") == null)
            throw new IllegalStateException("Native touch handler owner mismatch");
        Rect bounds = new Rect();
        call(view, "getBoundsOnScreen", new Class<?>[]{Rect.class}, bounds);
        Region expected = new Region(bounds);
        Region exclusion = (Region) call(read(model, "touchableExclusionRegion$delegate"), "getValue");
        expected.op(exclusion, Region.Op.DIFFERENCE);
        Region actual = (Region) read(view, "mTouchableRegion");
        if (!expected.equals(actual)) throw new IllegalStateException("Native touch region mismatch: " + actual + " expected=" + expected);
        return "nativeTouchRegion=" + actual + ", touchHandlerOwner=verified";
    }

    @Override public void close() throws ReflectiveOperationException {
        if (view.isAttachedToWindow()) throw new IllegalStateException("Detach App touch observer first");
        view.removeOnLayoutChangeListener(layout);
        if (attachment != null) {
            type("kotlinx.coroutines.DisposableHandle").getMethod("dispose").invoke(attachment);
            attachment = null;
        }
        // SnapshotViewBinding releases its observer in the native detach callback.
    }
    private Class<?> type(String name) throws ClassNotFoundException { return Class.forName(name, false, loader); }
    static Object captured(Class<?> type, String[] fields, Object[] values) throws ReflectiveOperationException {
        Class<?>[] parameters = new Class<?>[fields.length];
        for (int i = 0; i < fields.length; i++) parameters[i] = type.getDeclaredField(fields[i]).getType();
        Object result;
        try {
            Constructor<?> constructor = type.getDeclaredConstructor(parameters);
            constructor.setAccessible(true);
            result = constructor.newInstance(values);
        } catch (NoSuchMethodException absent) {
            if (type.getDeclaredConstructors().length == 0 && type.getSuperclass() == Object.class) {
                // A17 DEX invokes Object.<init> directly on these two verified
                // capture-only classes; no declared constructor exists.
                for (java.lang.reflect.Field field : type.getDeclaredFields())
                    if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())
                            && !java.util.Arrays.asList(fields).contains(field.getName()))
                        throw new IllegalStateException("Unexpected native capture field " + field);
                Class<?> unsafeType = Class.forName("sun.misc.Unsafe");
                java.lang.reflect.Field singleton = unsafeType.getDeclaredField("theUnsafe");
                singleton.setAccessible(true);
                result = unsafeType.getMethod("allocateInstance", Class.class).invoke(singleton.get(null), type);
            } else {
                Constructor<?> constructor = type.getDeclaredConstructor();
                constructor.setAccessible(true);
                result = constructor.newInstance();
            }
            for (int i = 0; i < fields.length; i++) write(result, fields[i], values[i]);
        }
        for (int i = 0; i < fields.length; i++) if (read(result, fields[i]) != values[i])
            throw new IllegalStateException("Native captured owner mismatch: " + fields[i]);
        return result;
    }
    private static Object read(Object owner, String name) throws ReflectiveOperationException {
        for (Class<?> type = owner.getClass(); type != null; type = type.getSuperclass()) {
            try {
                java.lang.reflect.Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(owner);
            } catch (NoSuchFieldException absent) { }
        }
        throw new NoSuchFieldException(name);
    }
}
