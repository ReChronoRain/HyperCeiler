/*
 * Derived from MiuiBackGestureHook status-bar-follow implementation.
 * Source: https://github.com/wxxsfxyzm/MiuiBackGestureHook
 * Reference commit: 004bc059e5821775917b771d89aa1d8c18a1e669 (includes b6e582f and later fixes)
 * Copyright (C) 2026 MiuiBackGestureHook contributors.
 * Licensed under the Apache License, Version 2.0; modified for HyperCeiler.
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow;

import android.os.Looper;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Collection;

/** Four independent native Folme properties. No substitute integrator or stop threshold. */
final class XiaomiBarFolme implements AutoCloseable {
    static final String[] NAMES = {"homeStatusAlpha", "appStatusAlpha", "navBarAlpha", "fakeNavBarAlpha"};
    interface Sink {
        void update(float[] values, double[] velocities);
        void terminal(boolean cancelled);
    }
    private final BarFolmeCommands commands = new BarFolmeCommands();
    private final Object target = new Object();
    private final Object style;
    private final Object[] properties = new Object[4];
    private final Object hide;
    private final Object show;
    private final Class<?> configClass;
    private final Class<?> listenerClass;
    private final Method setTo;
    private final Method to;
    private final Method clean;
    private final Method special;
    private final Method addListeners;
    private final Method floatValue;
    private final java.lang.reflect.Field updateProperty;
    private final java.lang.reflect.Field updateVelocity;
    private final Sink sink;
    private final Looper owner = Looper.myLooper();
    private final float[] values = new float[4];
    private final double[] velocities = new double[4];
    private boolean closed;

    XiaomiBarFolme(ClassLoader loader, Sink sink) throws ReflectiveOperationException {
        if (owner == null) throw new IllegalStateException("Folme requires its owner Looper");
        this.sink = sink;
        Class<?> folme = Class.forName("miuix.animation.Folme", false, loader);
        Class<?> contract = Class.forName("miuix.animation.FolmeStyle", false, loader);
        Class<?> property = Class.forName("miuix.animation.property.FloatProperty", false, loader);
        Class<?> valueProperty = Class.forName("miuix.animation.property.ValueProperty", false, loader);
        Class<?> ease = Class.forName("miuix.animation.utils.EaseManager$EaseStyle", false, loader);
        Class<?> update = Class.forName("miuix.animation.listener.UpdateInfo", false, loader);
        configClass = Class.forName("miuix.animation.base.AnimConfig", false, loader);
        listenerClass = Class.forName("miuix.animation.listener.TransitionListener", false, loader);
        setTo = contract.getMethod("setTo", Object[].class);
        to = contract.getMethod("to", Object[].class);
        clean = folme.getMethod("clean", Object[].class);
        special = configClass.getMethod("setSpecial", property, ease, float[].class);
        addListeners = configClass.getMethod("addListeners", Array.newInstance(listenerClass, 0).getClass());
        floatValue = update.getMethod("getFloatValue");
        updateProperty = update.getField("property");
        updateVelocity = update.getField("velocity");
        for (int i = 0; i < 4; i++) properties[i] = valueProperty.getConstructor(String.class).newInstance(NAMES[i]);
        hide = ease.getConstructor(int.class, float[].class).newInstance(-2, new float[]{1f, .3f});
        show = ease.getConstructor(int.class, float[].class).newInstance(-2, new float[]{1f, .6f});
        style = folme.getMethod("useValue", Object[].class).invoke(null, (Object) new Object[]{target});
        if (style == null) throw new IllegalStateException("Native Folme rejected owned target");
    }

    // Caller supplies separate status/nav decisions, including native explicit setTo resets.
    void set(float[] initial) throws ReflectiveOperationException {
        checkOwner();
        Object[] args = pairs(initial, 0, 4, false);
        commands.reset(0, 4);
        setTo.invoke(style, (Object) args);
        System.arraycopy(initial, 0, values, 0, 4);
        java.util.Arrays.fill(velocities, 0);
    }

    void retarget(float[] targets) throws ReflectiveOperationException {
        retarget(targets, 0, 4);
    }

    void retargetStatus(float home, float app) throws ReflectiveOperationException {
        retarget(new float[]{home, app}, 0, 2);
    }

    void retargetNavigation(float home, float app) throws ReflectiveOperationException {
        retarget(new float[]{home, app}, 2, 2);
    }

    // Native navigation's explicit-start overload uses setTo before to; preserve
    // that reset only for navigation, without resetting either status property.
    void resetNavigation(float home, float app) throws ReflectiveOperationException {
        checkOwner();
        Object[] args = pairs(new float[]{home, app}, 2, 2, false);
        commands.reset(2, 2);
        setTo.invoke(style, (Object) args);
        values[2] = home;
        values[3] = app;
        velocities[2] = velocities[3] = 0;
    }

    private void retarget(float[] targets, int offset, int count)
            throws ReflectiveOperationException {
        checkOwner();
        Object config = configClass.getConstructor().newInstance();
        Object[] args = java.util.Arrays.copyOf(pairs(targets, offset, count, true), count * 2 + 1);
        for (int i = 0; i < count; i++)
            special.invoke(config, properties[offset + i], targets[i] == 0f ? hide : show, new float[0]);
        // Each native command owns only its requested properties. A status
        // retarget cannot cancel navigation or accept stale status updates from
        // the preceding four-property command, even during synchronous to().
        Object listener = listenerClass.getConstructor().newInstance();
        Object listeners = Array.newInstance(listenerClass, 1);
        Array.set(listeners, 0, listener);
        addListeners.invoke(config, listeners);
        args[count * 2] = config;
        commands.replace(listener, offset, count);
        to.invoke(style, (Object) args);
    }

    boolean accepts(Object listener) { return !closed && commands.contains(listener); }

    void onUpdate(Object listener, Collection<?> updates) throws ReflectiveOperationException {
        checkOwner();
        if (!commands.contains(listener)) return;
        boolean changed = false;
        for (Object update : updates) {
            Object property = updateProperty.get(update);
            for (int i = 0; i < 4; i++) if (commands.owns(listener, i) && properties[i].equals(property)) {
                values[i] = ((Number) floatValue.invoke(update)).floatValue();
                velocities[i] = updateVelocity.getDouble(update);
                changed = true;
                break;
            }
        }
        if (changed) sink.update(values.clone(), velocities.clone());
    }

    void onTerminal(Object listener, boolean cancelled) {
        if (closed) return;
        checkOwner();
        int result = commands.terminal(listener, cancelled);
        if (result != 0) sink.terminal(result == 2);
    }

    private Object[] pairs(float[] input, int offset, int count, boolean visibilityTargets) {
        if (input.length != count || offset < 0 || offset + count > 4)
            throw new IllegalArgumentException("Invalid bar channel range");
        Object[] args = new Object[count * 2];
        for (int i = 0; i < count; i++) {
            if (!Float.isFinite(input[i]) || input[i] < 0 || input[i] > 1
                    || (visibilityTargets && input[i] != 0f && input[i] != 1f))
                throw new IllegalArgumentException("Invalid native alpha or visibility target");
            args[2 * i] = properties[offset + i]; args[2 * i + 1] = input[i];
        }
        return args;
    }

    private void checkOwner() {
        if (closed || Looper.myLooper() != owner) throw new IllegalStateException("Stale/wrong Folme owner");
    }

    @Override public void close() throws ReflectiveOperationException {
        if (closed) return;
        checkOwner();
        closed = true;
        clean.invoke(null, (Object) new Object[]{target});
    }
}
