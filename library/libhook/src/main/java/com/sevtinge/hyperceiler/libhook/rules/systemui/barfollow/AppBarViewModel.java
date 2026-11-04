/*
 * Derived from MiuiBackGestureHook status-bar-follow implementation.
 * Source: https://github.com/wxxsfxyzm/MiuiBackGestureHook
 * Reference commit: 004bc059e5821775917b771d89aa1d8c18a1e669 (includes b6e582f and later fixes)
 * Copyright (C) 2026 MiuiBackGestureHook contributors.
 * Licensed under the Apache License, Version 2.0; modified for HyperCeiler.
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow;

import java.lang.reflect.Constructor;

import static com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow.AppBarContentPreparation.call;
import static com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow.AppBarContentPreparation.read;

/** Native A17 ViewModel allocation with constructor subscriptions owned by the App window. */
final class AppBarViewModel {
    private AppBarViewModel() { }

    static Object create(ClassLoader loader, Object component, Object display, Object ownedScope, Object ownedDarkInteractor)
            throws ReflectiveOperationException {
        // Exact 25-argument native Dagger create() contract. Preserve physical
        // display/resources and shared business producers; replace only the
        // constructor scope so stateIn subscriptions end with the App window.
        Class<?> type = Class.forName("com.android.systemui.statusbar.pipeline.shared.ui.viewmodel.HomeStatusBarViewModelImpl", false, loader);
        Constructor<?> contract = null;
        for (Constructor<?> candidate : type.getDeclaredConstructors()) {
            Class<?>[] p = candidate.getParameterTypes();
            if (p.length != 25 || p[0] != int.class
                    || !p[21].getName().equals("kotlinx.coroutines.CoroutineScope")
                    || !p[22].getName().equals("kotlinx.coroutines.CoroutineDispatcher")) continue;
            if (contract != null) throw new NoSuchMethodException("Ambiguous native App ViewModel contract");
            contract = candidate;
        }
        if (contract == null) throw new NoSuchMethodException("Native App ViewModel contract");
        Class<?>[] nativeParameters = contract.getParameterTypes();
        Object[] args = {
                0,
                AppBarNativeContracts.provider(display, nativeParameters[1]),
                AppBarNativeContracts.provider(display, nativeParameters[2]),
                provider(component, "logBufferFactoryImplProvider"),
                provider(component, "tableLogBufferFactoryImplProvider"),
                provider(display, "provideStatusBarWindowResourcesProvider"),
                provider(display, "homeStatusBarInteractorProvider"),
                provider(component, "homeStatusBarIconBlockListInteractorProvider"),
                provider(component, "lightsOutInteractorProvider"),
                provider(component, "activeNotificationsInteractorProvider"),
                provider(component, "desktopInteractorProvider"),
                ownedDarkInteractor,
                provider(component, "keyguardTransitionInteractorProvider"),
                call(display, "statusBarNotificationIconsInteractor"),
                provider(display, "statusBarVisibilityInteractorProvider"),
                provider(component, "shadeInteractorImplProvider"),
                provider(component, "shareToAppChipViewModelProvider"),
                provider(display, "ongoingActivityChipsViewModelProvider"),
                AppBarNativeContracts.provider(display, nativeParameters[18]),
                provider(display, "systemStatusEventAnimationInteractorProvider"),
                provider(component, "contentInsetsViewModelProvider"),
                ownedScope,
                provider(component, "bgDispatcherProvider"),
                provider(component, "statusBarChipsUiEventLoggerProvider"),
                provider(component, "deviceProvisioningInteractorProvider")};
        Constructor<?> selected = null;
        for (Constructor<?> candidate : type.getDeclaredConstructors()) {
            Class<?>[] parameters = candidate.getParameterTypes();
            if (parameters.length != args.length || parameters[0] != int.class
                    || !parameters[21].getName().equals("kotlinx.coroutines.CoroutineScope")
                    || !parameters[22].getName().equals("kotlinx.coroutines.CoroutineDispatcher")) continue;
            boolean matches = true;
            for (int i = 1; i < parameters.length; i++) matches &= parameters[i].isInstance(args[i]);
            if (!matches) continue;
            if (selected != null) throw new IllegalStateException("Ambiguous App ViewModel constructor");
            selected = candidate;
        }
        if (selected == null) throw new NoSuchMethodException("Native App ViewModel constructor");
        selected.setAccessible(true);
        Object result = selected.newInstance(args);
        if (((Number) read(result, "thisDisplayId")).intValue() != 0
                || read(result, "iconBlockList") == null)
            throw new IllegalStateException("Invalid App ViewModel display/flow");
        return result;
    }

    private static Object provider(Object owner, String field) throws ReflectiveOperationException {
        return call(read(owner, field), "get");
    }
}
