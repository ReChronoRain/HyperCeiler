/*
 * Derived from MiuiBackGestureHook status-bar-follow implementation.
 * Source: https://github.com/wxxsfxyzm/MiuiBackGestureHook
 * Reference commit: 004bc059e5821775917b771d89aa1d8c18a1e669 (includes b6e582f and later fixes)
 * Copyright (C) 2026 MiuiBackGestureHook contributors.
 * Licensed under the Apache License, Version 2.0; modified for HyperCeiler.
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import java.lang.reflect.Constructor;
import static com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow.AppBarContentPreparation.*;

/** Stock StatusBarRoot chip composition with a private View and explicit disposal. */
final class AppBarChipBinding implements AutoCloseable {
    private final ClassLoader loader;
    private final View legacy;
    private final int originalVisibility;
    private final ViewGroup container;
    private final View compose;
    private boolean closed;

    static AppBarChipBinding create(ClassLoader loader, View home, View app, Object model, Object icons)
            throws ReflectiveOperationException {
        AppBarChipBinding result = new AppBarChipBinding(loader, app);
        try { result.bind(home, model, icons); return result; }
        catch (ReflectiveOperationException | RuntimeException | Error failure) {
            cleanupCreation(result, failure);
            throw failure;
        }
    }

    private AppBarChipBinding(ClassLoader loader, View app) throws ReflectiveOperationException {
        this.loader = loader;
        legacy = app.requireViewById(app.getResources().getIdentifier("ongoing_activity_chip_primary", "id", "com.android.systemui"));
        container = app.requireViewById(app.getResources().getIdentifier("phone_status_bar_left_container", "id", "com.android.systemui"));
        if (!(container instanceof LinearLayout) || legacy.getParent() != container)
            throw new IllegalStateException("Native chip layout changed");
        originalVisibility = legacy.getVisibility();
        Constructor<?> constructor = type("androidx.compose.ui.platform.ComposeView").getConstructor(
                Context.class, android.util.AttributeSet.class, int.class, int.class,
                type("kotlin.jvm.internal.DefaultConstructorMarker"));
        compose = (View) constructor.newInstance(app.getContext(), null, 0, 6, null);
    }

    private void bind(View home, Object model, Object icons) throws ReflectiveOperationException {
        // Reuse the already-active Home composition's services, but own and
        // dispose this composition. Never activate/destroy the Home lifecycle.
        Object lifecycle = type("androidx.lifecycle.ViewTreeLifecycleOwner").getMethod("get", View.class).invoke(null, home);
        Object savedState = type("androidx.savedstate.ViewTreeSavedStateRegistryOwner").getMethod("get", View.class).invoke(null, home);
        Object parent = null;
        for (View node = home; node != null; node = node.getParent() instanceof View ? (View) node.getParent() : null) {
            parent = type("androidx.compose.ui.platform.WindowRecomposer_androidKt")
                    .getMethod("getCompositionContext", View.class).invoke(null, node);
            if (parent != null) break;
        }
        if (lifecycle == null || savedState == null || parent == null)
            throw new IllegalStateException("Home composition services unavailable");
        // A17 inlines the two ViewTree setters; their getters retain the
        // exact resource-tag contract. Publish only on our new ComposeView.
        int lifecycleId = compose.getResources().getIdentifier("view_tree_lifecycle_owner", "id", "com.android.systemui");
        int savedStateId = compose.getResources().getIdentifier("view_tree_saved_state_registry_owner", "id", "com.android.systemui");
        if (lifecycleId == 0 || savedStateId == 0) throw new IllegalStateException("Native ViewTree tags missing");
        compose.setTag(lifecycleId, lifecycle);
        compose.setTag(savedStateId, savedState);
        call(compose, "setParentCompositionContext", new Class<?>[]{type("androidx.compose.runtime.CompositionContext")}, parent);
        Class<?> lambda = type("com.android.systemui.statusbar.pipeline.shared.ui.composable.StatusBarRootKt$$ExternalSyntheticLambda6");
        Object content = AppBarTouchBinding.captured(lambda, new String[]{"f$0", "f$1"}, new Object[]{model, icons});
        Object composable = type("androidx.compose.runtime.internal.ComposableLambdaKt")
                .getMethod("composableLambdaInstance", int.class, boolean.class, Object.class)
                .invoke(null, -1214456444, true, content);
        call(compose, "setContent", new Class<?>[]{type("kotlin.jvm.functions.Function2")}, composable);
        compose.setLayoutParams(new LinearLayout.LayoutParams(-2, -1));
        // Exact native replacement: the legacy XML placeholder is hidden,
        // while real chip visibility remains owned by the native ViewModel.
        legacy.setVisibility(View.GONE);
        container.addView(compose, container.indexOfChild(legacy) + 1);
    }

    String verify() throws ReflectiveOperationException {
        if (closed || legacy.getVisibility() != View.GONE || compose.getParent() != container
                || !compose.isAttachedToWindow() || !(Boolean) call(compose, "getHasComposition"))
            throw new IllegalStateException("Native chip composition not attached");
        return "legacyChip=gone, nativeChipComposition=active";
    }

    @Override public void close() throws ReflectiveOperationException {
        if (closed) return;
        call(compose, "disposeComposition");
        if (compose.getParent() == container) container.removeView(compose);
        legacy.setVisibility(originalVisibility);
        closed = true;
    }
    private Class<?> type(String name) throws ClassNotFoundException { return Class.forName(name, false, loader); }
}
