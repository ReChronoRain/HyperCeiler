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
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow.AppBarContentPreparation.call;
import static com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow.AppBarContentPreparation.read;
import static com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow.AppBarContentPreparation.write;

/** Independent native system-icon group and stock attachment/block-list lifecycle. */
final class AppBarSystemIcons implements AutoCloseable {
    private static final String UI = "com.android.systemui.statusbar.phone.ui.";
    private final ClassLoader loader;
    private final View view;
    private final LinearLayout group;
    private final Object controller;
    private final Object[] originalGroups;
    private final List<View[]> originalChildren = new ArrayList<>();
    private Object manager;
    private Object binding;
    private boolean closed;

    static AppBarSystemIcons create(ClassLoader loader, Object root, View view,
                                    Object dark, Object viewModel) throws ReflectiveOperationException {
        requireMain();
        AppBarSystemIcons result = new AppBarSystemIcons(loader, root, view);
        try { result.initialize(root, dark, viewModel); return result; }
        catch (ReflectiveOperationException | RuntimeException | Error failure) {
            AppBarContentPreparation.cleanupCreation(result, failure);
            throw failure;
        }
    }

    private AppBarSystemIcons(ClassLoader loader, Object root, View view) throws ReflectiveOperationException {
        this.loader = loader;
        this.view = view;
        group = (LinearLayout) read(view, "mStatusBarStatusIcons");
        if (group.getChildCount() != 0 || read(view, "mDarkIconManager") != null)
            throw new IllegalStateException("App system-icon group already owned");
        controller = read(root, "iconController");
        originalGroups = groups().toArray();
        for (Object original : originalGroups) {
            ViewGroup originalGroup = (ViewGroup) field(original, "mGroup");
            if (originalGroup == group) throw new IllegalStateException("App group already registered");
            originalChildren.add(children(originalGroup));
        }
        controller.getClass().getMethod("removeIconGroup", type(UI + "IconManager"));
        type("kotlinx.coroutines.DisposableHandle").getMethod("dispose");
    }

    private void initialize(Object root, Object dark, Object viewModel) throws ReflectiveOperationException {
        Object factory = read(root, "darkIconManagerFactory");
        Class<?> location = type("com.android.systemui.statusbar.phone.StatusBarLocation");
        manager = call(factory, "create", new Class<?>[]{LinearLayout.class, location,
                type("com.android.systemui.plugins.DarkIconDispatcher")}, group, location.getField("HOME").get(null), dark);
        if (!manager.getClass().getName().equals(UI + "DarkIconManager") || field(manager, "mGroup") != group
                || read(manager, "mDarkIconDispatcher") != dark)
            throw new IllegalStateException("Native App icon manager identity mismatch");
        call(view, "setDarkIconManager", new Class<?>[]{manager.getClass()}, manager);

        Object blockList = read(viewModel, "iconBlockList");
        // Preserve ViewModel flowOn, stock STARTED collection and native
        // add/remove finally; do not create a second icon-list policy.
        Class<?> flow = type("kotlinx.coroutines.flow.Flow");
        Class<?> continuation = type("kotlin.coroutines.Continuation");
        Class<?> controllerContract = type(UI + "StatusBarIconController");
        Class<?> coroutine = type("com.android.systemui.statusbar.pipeline.shared.ui.binder.HomeStatusBarIconBlockListBinder$bind$1");
        Constructor<?> constructor = coroutine.getDeclaredConstructor(controllerContract, manager.getClass(), flow, continuation);
        constructor.setAccessible(true);
        Object block = constructor.newInstance(controller, manager, blockList, null);
        binding = type("com.android.systemui.lifecycle.RepeatWhenAttachedKt")
                .getMethod("repeatWhenAttached$default", View.class, type("kotlin.coroutines.CoroutineContext"),
                        type("kotlin.jvm.functions.Function3"), int.class)
                .invoke(null, group, null, block, 3);
    }

    int verifyPopulation() throws ReflectiveOperationException {
        requireMain();
        if (closed || group.isAttachedToWindow()) throw new IllegalStateException("Population check requires detached App group");
        int count;
        try {
            call(controller, "addIconGroup", new Class<?>[]{type(UI + "IconManager")}, manager);
            count = group.getChildCount();
            for (View[] originals : originalChildren) for (View original : originals)
                for (int i = 0; i < count; i++) if (group.getChildAt(i) == original)
                    throw new IllegalStateException("Native App manager stole an existing icon View");
        } finally {
            if (groups().contains(manager)) call(controller, "removeIconGroup",
                    new Class<?>[]{type(UI + "IconManager")}, manager);
        }
        verifyPrimary();
        return count;
    }

    private void verifyPrimary() throws ReflectiveOperationException {
        Object[] actual = groups().toArray();
        if (actual.length != originalGroups.length) throw new IllegalStateException("Native icon group count changed");
        for (int i = 0; i < actual.length; i++) {
            if (actual[i] != originalGroups[i]) throw new IllegalStateException("Native icon group owner changed");
            View[] after = children((ViewGroup) field(actual[i], "mGroup"));
            View[] before = originalChildren.get(i);
            if (after.length != before.length) throw new IllegalStateException("Primary icon child count changed");
            for (int j = 0; j < after.length; j++) if (after[j] != before[j])
                throw new IllegalStateException("Primary icon child identity changed");
        }
    }

    @Override public void close() throws ReflectiveOperationException {
        requireMain();
        if (closed) return;
        if (binding != null) {
            type("kotlinx.coroutines.DisposableHandle").getMethod("dispose").invoke(binding);
            binding = null;
        }
        // A still-running binder may add the manager again. Disposal must
        // succeed before destroying its target; failed disposal remains owned.
        if (manager != null) {
            if (groups().contains(manager)) call(controller, "removeIconGroup",
                    new Class<?>[]{type(UI + "IconManager")}, manager);
            else call(manager, "destroy");
            if (read(view, "mDarkIconManager") == manager) write(view, "mDarkIconManager", null);
        }
        for (Object current : groups()) if (current == manager || field(current, "mGroup") == group)
            throw new IllegalStateException("App system-icon group remains registered");
        if (group.getChildCount() != 0) throw new IllegalStateException("App system icons survived release");
        closed = true;
    }

    private List<?> groups() throws ReflectiveOperationException { return (List<?>) read(controller, "mIconGroups"); }
    private static View[] children(ViewGroup group) {
        View[] result = new View[group.getChildCount()];
        for (int i = 0; i < result.length; i++) result[i] = group.getChildAt(i);
        return result;
    }
    private static Object field(Object owner, String name) throws ReflectiveOperationException {
        for (Class<?> c = owner.getClass(); c != null; c = c.getSuperclass()) {
            try { Field field = c.getDeclaredField(name); field.setAccessible(true); return field.get(owner); }
            catch (NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(name);
    }
    private Class<?> type(String name) throws ClassNotFoundException { return Class.forName(name, false, loader); }
    private static void requireMain() {
        if (Looper.myLooper() != Looper.getMainLooper()) throw new IllegalStateException("System icons require main Looper");
    }
}
