/*
 * Derived from MiuiBackGestureHook status-bar-follow implementation.
 * Source: https://github.com/wxxsfxyzm/MiuiBackGestureHook
 * Reference commit: 004bc059e5821775917b771d89aa1d8c18a1e669 (includes b6e582f and later fixes)
 * Copyright (C) 2026 MiuiBackGestureHook contributors.
 * Licensed under the Apache License, Version 2.0; modified for HyperCeiler.
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow;

import android.content.Context;
import android.os.Looper;

import java.lang.reflect.Method;

/** A16's type-2041 resource/token context, owned until all child Views are released. */
final class AppBarWindowContext implements AutoCloseable {
    private Context context;
    private final Method release;

    static AppBarWindowContext create(ClassLoader loader, Context base)
            throws ReflectiveOperationException {
        requireMain();
        Class<?> type = Class.forName("android.window.WindowContext", false, loader);
        AppBarWindowContext result = new AppBarWindowContext(type.getMethod("release"));
        if (base.getDisplay() == null || base.getDisplay().getDisplayId() != 0)
            throw new IllegalStateException("App bar requires the physical default display");
        try {
            result.context = base.createWindowContext(base.getDisplay(), 2041, null);
            if (result.context.getClass() != type || result.context == base
                    || result.context.getDisplay().getDisplayId() != 0
                    || ((Number) type.getMethod("getWindowType").invoke(result.context)).intValue() != 2041)
                throw new IllegalStateException("Native App window context identity changed");
            // Resource entry names retain dots; R.style's Java field replaces them with underscores.
            int theme = result.context.getResources().getIdentifier("Theme.SystemUI", "style", "com.android.systemui");
            if (theme == 0) throw new IllegalStateException("Native SystemUI theme unavailable");
            result.context.setTheme(theme);
            return result;
        } catch (ReflectiveOperationException | RuntimeException | Error failure) {
            AppBarContentPreparation.cleanupCreation(result, failure);
            throw failure;
        }
    }

    private AppBarWindowContext(Method release) { this.release = release; }

    Context get() {
        requireMain();
        if (context == null) throw new IllegalStateException("App window context released");
        return context;
    }

    @Override public void close() throws ReflectiveOperationException {
        requireMain();
        if (context != null) {
            release.invoke(context);
            context = null;
        }
    }

    private static void requireMain() {
        if (Looper.myLooper() != Looper.getMainLooper())
            throw new IllegalStateException("App window context requires main Looper");
    }
}
