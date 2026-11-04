/*
 * Derived from MiuiBackGestureHook status-bar-follow implementation.
 * Source: https://github.com/wxxsfxyzm/MiuiBackGestureHook
 * Reference commit: 004bc059e5821775917b771d89aa1d8c18a1e669 (includes b6e582f and later fixes)
 * Copyright (C) 2026 MiuiBackGestureHook contributors.
 * Licensed under the Apache License, Version 2.0; modified for HyperCeiler.
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow;

import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Allocates the private portion of an A17 App-bar content graph through native
 * factories. This is preparation only: no View is attached; native child binders
 * wait for attachment. The remaining native
 * global dependencies must be isolated before this graph can render a second bar.
 */
final class AppBarContentPreparation implements AutoCloseable {
    private static final String DAGGER = "com.android.systemui.dagger.DaggerReferenceGlobalRootComponent$";
    private static final String CONTENT = "com.android.systemui.statusbar.pipeline.shared.ui.";
    private final Object primaryRoot;
    private final Object primaryBinder;
    private final Object primaryInjector;
    private final Object primaryIsland;
    private final Object[] primaryIslandProviders;
    private Object root;
    private Object binder;
    private Object fragment;
    private Object notificationStore;
    private Object animator;
    private AppBarAppearancePreparation appearance;
    private AppBarNotificationIcons notificationIcons;
    private View appView;
    private Object appInjector;
    private AppBarWindow window;
    private AppBarWindowContext windowContext;
    private AppBarPrivacyBinding privacyBinding;
    private AppBarShadeTargets shadeTargets;
    private Object appViewDependency;
    private AppBarIslandBinding islandBinding;
    private AppBarSystemIcons systemIcons;
    private Object viewModel;
    private AppBarVisibilityBinding visibilityBinding;
    private AppBarClockBatteryBinding clockBatteryBinding;
    private AppBarStateBinding stateBinding;
    private AppBarViewLayoutBinding layoutBinding;
    private AppBarStatusIconIslandBinding statusIconIslandBinding;
    private AppBarNotificationIslandBinding notificationIslandBinding;
    private AppBarAuxiliaryBinding auxiliaryBinding;
    private AutoCloseable incompleteChild;
    private boolean dependencyPrepared;
    private boolean dependencyInitialized;
    private boolean closing;
    private boolean appTokenReady;
    private AppBarWindowHandshake windowHandshake;
    private int selectedRole = XiaomiShadeBarSelection.HOME;
    private Object nativeComponent;
    private Object nativeViewController;
    private AppBarModeBinding modeBinding;
    private AppBarTouchBinding touchBinding;
    private AppBarChipBinding chipBinding;
    private AppBarIconAnimationBinding appIconAnimation;
    private AppBarIconAnimationBinding homeIconAnimation;
    private Object appNotificationInject;
    private Object homeNotificationInject;
    private boolean closed;

    static AppBarContentPreparation create(ClassLoader loader,
            java.util.function.Consumer<AppBarContentPreparation> retain,
            java.util.function.BiConsumer<AppBarContentPreparation, RuntimeException> onFailure)
            throws ReflectiveOperationException {
        requireMain();
        java.util.Objects.requireNonNull(retain, "App content requires a runtime owner");
        java.util.Objects.requireNonNull(onFailure, "App content requires a failure owner");
        if (Class.forName("com.miui.utils.configs.MiuiConfigs", false, loader).getField("IS_PAD").getBoolean(null))
            throw new IllegalStateException("App View role initialization currently requires the native phone branch");
        PrimaryHome home = new PrimaryHome(loader);
        Object component = home.component;
        Object display = home.display;
        Object root = home.root;
        AppBarContentPreparation prepared = new AppBarContentPreparation(loader, component, display, root);
        // Publish ownership before any registration. A failed cleanup must stay
        // reachable by the runtime rather than disappear with a failed factory.
        retain.accept(prepared);
        try {
            prepared.appearance = AppBarAppearancePreparation.create(loader, component,
                    prepared.primaryInjector);
            write(prepared.root, "darkIconDispatcher", prepared.appearance.dark());
            write(prepared.fragment, "darkIconDispatcher", prepared.appearance.dark());
            write(prepared.fragment, "islandController", prepared.primaryIsland);
            prepared.notificationIcons = AppBarNotificationIcons.create(loader, display,
                    prepared.root, prepared.primaryRoot, prepared.notificationStore);
            prepared.windowContext = AppBarWindowContext.create(loader,
                    ((View) read(prepared.primaryInjector, "rootView")).getContext());
            prepared.prepareDetachedViews(loader);
            prepared.window = AppBarWindow.create(loader, component, display, prepared.windowContext.get());
            prepared.notificationIcons.prepareDarkModel(loader, component, display,
                    prepared.appearance.dark(), prepared.appearance.dump());
            prepared.privacyBinding = AppBarPrivacyBinding.create(loader, component,
                    read(prepared.primaryInjector, "rootView"), prepared.appView, prepared.window.scope());
            write(prepared.fragment, "miuiHomePrivacyController", prepared.privacyBinding.controller());
            prepared.prepareViewDependency();
            prepared.shadeTargets = new AppBarShadeTargets(prepared.primaryBinder, prepared.appView);
            // A detached App graph must retain the real Home target even when
            // task classification says App. Live token readiness is separate.
            prepared.shadeTargets.select(XiaomiShadeBarSelection.APP, false);
            prepared.islandBinding = AppBarIslandBinding.bind(loader, prepared.appInjector,
                    prepared::isAppSelected);
            prepared.viewModel = AppBarViewModel.create(loader, component, display, prepared.window.scope(),
                    prepared.notificationIcons.darkInteractor());
            prepared.systemIcons = AppBarSystemIcons.create(loader, prepared.root,
                    prepared.appView, prepared.appearance.dark(), prepared.viewModel);
            prepared.statusIconIslandBinding = AppBarStatusIconIslandBinding.create(loader,
                    (View) read(prepared.appView, "mStatusBarStatusIcons"), prepared.primaryIsland,
                    prepared.animator);
            prepared.visibilityBinding = AppBarVisibilityBinding.create(loader, prepared.appInjector,
                    prepared.viewModel, call(read(display, "systemStatusEventAnimationInteractorProvider"), "get"));
            prepared.clockBatteryBinding = AppBarClockBatteryBinding.create(loader, prepared.appView,
                    prepared.appearance.dark());
            prepared.auxiliaryBinding = AppBarAuxiliaryBinding.create(loader, prepared.appInjector);
            prepared.stateBinding = AppBarStateBinding.create(loader, prepared.appInjector);
            prepared.layoutBinding = AppBarViewLayoutBinding.create(loader, prepared.appView,
                    prepared.fragment, prepared.appViewDependency);
            View notificationContainer = prepared.appView.requireViewById(resource(prepared.appView, "id", "notificationIcons"));
            prepared.notificationIslandBinding = AppBarNotificationIslandBinding.create(loader,
                    notificationContainer, prepared.primaryIsland, prepared.animator);
            prepared.appNotificationInject = call(notificationContainer, "getInject");
            View homeView = (View) read(prepared.primaryInjector, "rootView");
            prepared.homeNotificationInject = call(homeView.requireViewById(resource(homeView, "id", "notificationIcons")), "getInject");
            prepared.appIconAnimation = AppBarIconAnimationBinding.create(loader,
                    (View) read(prepared.appView, "mStatusBarStatusIcons"), () -> prepared.iconAnimationAllowed(true));
            prepared.homeIconAnimation = AppBarIconAnimationBinding.create(loader,
                    (View) read(homeView, "mStatusBarStatusIcons"), () -> prepared.iconAnimationAllowed(false));
            prepared.notificationIcons.bindView(loader, notificationContainer);
            prepared.dependencyPrepared = true;
            call(prepared.appView, "setDarkIconDispatcher", new Class<?>[]{Class.forName(
                    "com.android.systemui.plugins.DarkIconDispatcher", false, loader)}, prepared.appearance.dark());
            // Native setDependence still owns the phone branch and monitor
            // assignment. The exact instance's initDependence is role-gated.
            call(prepared.appView, "setDependence", new Class<?>[]{prepared.appViewDependency.getClass()},
                    prepared.appViewDependency);
            if (!prepared.dependencyInitialized || !(Boolean) read(prepared.appView, "mInited"))
                throw new IllegalStateException("App View initialization guard did not execute");
            prepared.window.bindContent(prepared.appView);
            prepared.prepareNativeController(loader, component, display);
            prepared.touchBinding = new AppBarTouchBinding(loader, homeView, prepared.appView);
            prepared.chipBinding = AppBarChipBinding.create(loader, homeView, prepared.appView,
                    prepared.viewModel, prepared.notificationStore);
            prepared.modeBinding = AppBarModeBinding.create(loader, component, display,
                    prepared.window.scope(), prepared.appearance.dark(), prepared.appearance.dump(),
                    prepared.nativeComponent, prepared.appView,
                    failure -> onFailure.accept(prepared, failure));
            prepared.verifyPrimaryUnchanged();
            return prepared;
        } catch (ReflectiveOperationException | RuntimeException | Error failure) {
            if (failure instanceof IncompleteCreation)
                prepared.incompleteChild = ((IncompleteCreation) failure).owner;
            try { prepared.close(); }
            catch (Throwable cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }

    String verifyNotificationViews() throws ReflectiveOperationException {
        clockBatteryBinding.verifyTintReceivers();
        stateBinding.verifyWindowStateBinding();
        return notificationIcons.verifyIndependentViews() + ", system=" + systemIcons.verifyPopulation()
                + ", visibilityCollectorsPrepared=" + visibilityBinding.collectorCount() + ", clockBatteryTint=verified"
                + ", stateCallbacks=" + stateBinding.registrationCount()
                + ", controlCenterPrepared=" + stateBinding.controlCenterPrepared()
                + ", nativeLayoutBound=" + layoutBinding.initialized() + ", physicalWindowConsumer=verified";
    }

    boolean ownsView(Object candidate) { return candidate != null && candidate == appView; }

    boolean ownsInjector(Object candidate) { return candidate != null && candidate == appInjector; }

    Boolean injectorAnimationAllowed(Object candidate) throws ReflectiveOperationException {
        requireMain();
        if (ownsInjector(candidate)) return appAnimationAllowed();
        if (candidate == primaryInjector && appTokenReady && !closing
                && read(primaryBinder, "mInjector") == primaryInjector) return iconAnimationAllowed(false);
        return null;
    }

    private boolean iconAnimationAllowed(boolean app) {
        try {
            if (app) return appAnimationAllowed();
            if (!appTokenReady || closing) return true;
            return read(primaryBinder, "mInjector") == primaryInjector
                    && selectedRole == XiaomiShadeBarSelection.HOME
                    && !(Boolean) read(primaryInjector, "mIsTempWindowHidden");
        } catch (ReflectiveOperationException failure) {
            return false;
        }
    }

    Boolean notificationAnimationAllowed(Object candidate) {
        requireMain();
        if (candidate != null && candidate == appNotificationInject) return iconAnimationAllowed(true);
        if (candidate != null && candidate == homeNotificationInject) return iconAnimationAllowed(false);
        return null;
    }

    private boolean isAppSelected() {
        return !closing && appTokenReady && selectedRole == XiaomiShadeBarSelection.APP
                && appView != null && appView.isAttachedToWindow();
    }

    boolean appAnimationAllowed() throws ReflectiveOperationException {
        requireMain();
        return isAppSelected() && !(Boolean) read(appInjector, "mIsTempWindowHidden");
    }

    boolean handleAppPrivacy(Object candidate) throws ReflectiveOperationException {
        requireMain();
        if (privacyBinding != null && candidate == privacyBinding.controller()) {
            if (closing) return true;
            return !appAnimationAllowed() && privacyBinding.applyWithoutAnimation();
        }
        if (!closing && appTokenReady && read(primaryBinder, "mInjector") == primaryInjector) {
            Object dependency = read(read(primaryInjector, "rootView"), "mDependence");
            if (dependency != null && candidate == read(dependency, "miuiHomePrivacy"))
                return !iconAnimationAllowed(false) && AppBarPrivacyBinding.applyWithoutAnimation(candidate);
        }
        return false;
    }

    Boolean privacyAnimationAllowed(Object candidate) throws ReflectiveOperationException {
        requireMain();
        if (privacyBinding != null && candidate == privacyBinding.controller())
            return !closing && appAnimationAllowed();
        if (!closing && appTokenReady && read(primaryBinder, "mInjector") == primaryInjector) {
            Object dependency = read(read(primaryInjector, "rootView"), "mDependence");
            if (dependency != null && candidate == read(dependency, "miuiHomePrivacy"))
                return iconAnimationAllowed(false);
        }
        return null;
    }

    boolean ownsComponent(Object candidate) { return candidate != null && candidate == nativeComponent; }

    Object darkStateFor(Object repository, int displayId) {
        return notificationIcons == null ? null : notificationIcons.darkStateFor(repository, displayId);
    }

    void configureViewFactory(Object factory) throws ReflectiveOperationException {
        requireMain();
        if (closing || !dependencyInitialized) throw new IllegalStateException("App controller factory owner unavailable");
        write(factory, "miuiPhoneStatusBarViewDep", appViewDependency);
        write(factory, "darkIconDispatcher", appearance.dark());
        // This implementation admits only the phone strategy. Do not toggle
        // the shared Home unfold lease from another native controller.
        write(factory, "progressProvider", java.util.Optional.empty());
    }

    private void prepareNativeController(ClassLoader loader, Object component, Object display)
            throws ReflectiveOperationException {
        Object global = read(component, "referenceGlobalRootComponentImpl");
        Class<?> type = Class.forName(DAGGER + "HomeStatusBarComponentImpl", false, loader);
        Constructor<?> constructor = type.getDeclaredConstructor(global.getClass(), component.getClass(), display.getClass(),
                Class.forName("com.android.systemui.statusbar.phone.PhoneStatusBarView", false, loader),
                Class.forName("com.android.systemui.statusbar.window.StatusBarWindowController", false, loader));
        constructor.setAccessible(true);
        nativeComponent = constructor.newInstance(global, component, display, appView, window.controller());
        // Never component.init(): it also installs Home-owned heads-up and
        // orchestrator consumers. Obtain only this View's native controller.
        nativeViewController = call(nativeComponent, "getPhoneStatusBarViewController");
        if (read(nativeViewController, "darkIconDispatcher") != appearance.dark()
                || read(appView, "mDependence") != appViewDependency)
            throw new IllegalStateException("Native App controller did not adopt private dependencies");
        clockBatteryBinding.delegateToNativeController();
        call(nativeViewController, "init");
    }

    void initializeViewDependency(Object dependency) throws ReflectiveOperationException {
        requireMain();
        if (closing) return;
        if (!dependencyPrepared || dependency != appViewDependency || read(appView, "mDependence") != dependency)
            throw new IllegalStateException("App native View dependency is not ready");
        if ((Boolean) read(appView, "mInited")) return;
        if (dependencyInitialized)
            throw new IllegalStateException("A detached App content lifetime must be recreated before reattachment");
        if (read(appView, "mHoverHelper") != null || !layoutBinding.initialized()
                || read(dependency, "miuiHomePrivacy") != privacyBinding.controller()
                || read(read(appView, "mStatusBarStatusIcons"), "_islandMonitor") == null)
            throw new IllegalStateException("App native initialization prerequisites are incomplete");
        // Privacy containers, native Folme initialization and the split-screen
        // listener have already completed under explicit ownership. The only
        // remaining phone init operations are selected shade publication and
        // local drip layout; they must not publish an unselected App View.
        privacyBinding.verifyPrimary();
        shadeTargets.select(XiaomiShadeBarSelection.APP, false);
        updateDrip((Boolean) read(appView, "mDripEnd"));
        write(appView, "mInited", true);
        dependencyInitialized = true;
    }

    void updateDrip(boolean drip) throws ReflectiveOperationException {
        requireMain();
        if (closing) return;
        write(appView, "mDripEnd", drip);
        Object manager = read(appView, "mDarkIconManager");
        if (manager != null) call(manager, "setDripEnd", new Class<?>[]{boolean.class}, drip);
        // A16 setDripEnd explicitly publishes Control Center drip only for
        // isHomeBar, even when App is the selected shade target.
    }

    private void prepareViewDependency() throws ReflectiveOperationException {
        Object primaryView = read(primaryInjector, "rootView");
        Object primaryDependency = read(primaryView, "mDependence");
        String[] fields = {"miuiPrivacy", "clockController", "miuiHomePrivacy", "ccFakeViewController",
                "multiTaskFactory", "ccControllerImpl", "shadeController", "statusBarClickTool", "islandController"};
        Class<?> type = primaryDependency.getClass();
        requireType(primaryDependency, "com.android.systemui.statusbar.phone.MiuiPhoneStatusBarViewDep");
        Class<?>[] contracts = new Class<?>[fields.length];
        Object[] values = new Object[fields.length];
        for (int i = 0; i < fields.length; i++) {
            contracts[i] = type.getDeclaredField(fields[i]).getType();
            values[i] = i == 2 ? privacyBinding.controller() : read(primaryDependency, fields[i]);
        }
        appViewDependency = type.getConstructor(contracts).newInstance(values);
        if (appViewDependency == primaryDependency || read(appViewDependency, "miuiHomePrivacy")
                == read(primaryDependency, "miuiHomePrivacy"))
            throw new IllegalStateException("App native View dependency reused its privacy owner");
        // Keep central clock/CC and island producers shared exactly as A16.
        // setDependence cannot run yet: A17's inlined initDependence publishes
        // these shared targets without checking the selected token.
        for (int i = 0; i < fields.length; i++) if (i != 2
                && read(appViewDependency, fields[i]) != read(primaryDependency, fields[i]))
            throw new IllegalStateException("App native View dependency lost shared producer " + fields[i]);
    }

    private void prepareDetachedViews(ClassLoader loader) throws ReflectiveOperationException {
        View primaryView = (View) read(primaryInjector, "rootView");
        LayoutInflater inflater = LayoutInflater.from(primaryView.getContext()).cloneInContext(windowContext.get());
        // Exact XML path from StatusBarRootKt's AndroidView factory. Keep this
        // hierarchy detached until View dependencies, Compose and lifecycle are
        // all owned. No main StatusBarInitializer callback may see this View.
        appView = inflater.inflate(resource(primaryView, "layout", "status_bar"),
                new FrameLayout(windowContext.get()), false);
        requireType(appView, primaryView.getClass().getName());
        if (appView == primaryView || appView.isAttachedToWindow() || appView.getParent() != null
                || read(appView, "mDependence") != null || (Boolean) read(appView, "mInited"))
            throw new IllegalStateException("App status-bar View unexpectedly initialized");
        ViewGroup notificationArea = appView.requireViewById(resource(primaryView, "id", "notification_icon_area"));
        inflater.inflate(resource(primaryView, "layout", "notification_icon_area"), notificationArea, true);
        for (String name : new String[]{"notificationIcons", "clock", "battery", "statusIcons"}) {
            int id = resource(primaryView, "id", name);
            View original = primaryView.requireViewById(id);
            View own = appView.requireViewById(id);
            if (own == original || own.getClass() != original.getClass())
                throw new IllegalStateException("App layout does not match native child: " + name);
        }

        // The inspected A17 native factory only allocates the injector's local
        // flows/listeners; its ViewModel argument is unused. This is not bind():
        // no null ViewModel is ever passed into an attached native coroutine.
        Object factory = read(binder, "injectorFactory");
        appInjector = call(factory, "create", new Class<?>[]{binder.getClass(), View.class,
                Class.forName(CONTENT + "viewmodel.HomeStatusBarViewModel", false, loader)},
                binder, appView, null);
        requireType(appInjector, primaryInjector.getClass().getName());
        if (appInjector == primaryInjector || read(appInjector, "rootView") != appView)
            throw new IllegalStateException("App injector reused a primary owner");
        write(appInjector, "fragmentDep", fragment);
        write(appInjector, "animatorController", animator);
        write(appInjector, "darkIconDispatcher", appearance.dark());
        write(appInjector, "islandController", primaryIsland);
        for (String field : new String[]{"regionCallback", "islandListener", "configurationListener",
                "clockSpaceProvider", "batterySpaceProvider", "statusBarStateFlow", "islandShowingFlow"}) {
            if (read(appInjector, field) == read(primaryInjector, field))
                throw new IllegalStateException("App injector shares local state: " + field);
        }
        // Do not assign binder.mInjector: native bind() would call onUnbind on
        // this never-bound object before constructing another global injector.
        if (read(binder, "mInjector") != null)
            throw new IllegalStateException("Detached App binder unexpectedly bound");
    }

    private static int resource(View primaryView, String type, String name) {
        // JADX's generated R classes are absent after native R8 optimization.
        // Resolve the installed SystemUI resource table; never freeze numeric IDs.
        int id = primaryView.getResources().getIdentifier(name, type, "com.android.systemui");
        if (id == 0) throw new IllegalStateException("Missing native SystemUI resource: " + type + "/" + name);
        return id;
    }

    /** A bind return is relevant only while its exact Home binder, injector and View remain current. */
    static boolean isCurrentHomeBinding(ClassLoader loader, Object binder, Object injector,
                                        Object view) throws ReflectiveOperationException {
        requireMain();
        if (binder == null || injector == null || view == null) return false;
        PrimaryHome home = new PrimaryHome(loader);
        return read(home.root, "homeStatusBarViewBinder") == binder
                && read(binder, "mInjector") == injector
                && injector.getClass().getName().equals(CONTENT + "binder.HomeStatusBarViewBinderInjector")
                && read(injector, "rootView") == view;
    }

    private static final class PrimaryHome {
        final Object component;
        final Object display;
        final Object root;

        PrimaryHome(ClassLoader loader) throws ReflectiveOperationException {
            // Same native component route used by the existing A17 BackPanel factory.
            Object dependency = Class.forName("com.android.systemui.Dependency", false, loader)
                    .getField("sDependency").get(null);
            Object navigation = call(read(dependency, "mNavigationBarController"), "get");
            component = read(read(navigation, "mNavigationBarComponentFactory"),
                    "referenceSysUIComponentImpl");
            requireType(component, DAGGER + "ReferenceSysUIComponentImpl");
            Object repository = call(read(component, "provideDisplayComponentRepositoryProvider"), "get");
            display = call(repository, "get", new Class<?>[]{int.class}, 0);
            requireType(display, DAGGER + "ReferenceSysUIDisplaySubcomponentImpl");
            root = call(display, "getStatusBarRootFactory");
            requireType(root, CONTENT + "composable.StatusBarRootFactory");
        }
    }

    private AppBarContentPreparation(ClassLoader loader, Object component, Object display,
                                     Object primaryRoot) throws ReflectiveOperationException {
        this.primaryRoot = primaryRoot;
        primaryBinder = read(primaryRoot, "homeStatusBarViewBinder");
        requireType(primaryBinder, CONTENT + "binder.HomeStatusBarViewBinderImpl");
        primaryInjector = read(primaryBinder, "mInjector");
        requireType(primaryInjector, CONTENT + "binder.HomeStatusBarViewBinderInjector");
        primaryIsland = read(primaryInjector, "islandController");
        primaryIslandProviders = islandProviders(primaryIsland);

        // Native SwitchingProvider constructor and cases49/82 verified in JADX.
        // Calling a NEW provider avoids changing DoubleCheck's cached main graph.
        Object global = read(component, "referenceGlobalRootComponentImpl");
        Class<?> provider = Class.forName(DAGGER
                + "ReferenceSysUIDisplaySubcomponentImpl$SwitchingProvider", false, loader);
        Constructor<?> constructor = provider.getDeclaredConstructor(global.getClass(),
                component.getClass(), display.getClass(), int.class);
        constructor.setAccessible(true);
        root = call(constructor.newInstance(global, component, display, 82), "get");
        binder = call(constructor.newInstance(global, component, display, 49), "get");
        requireType(root, primaryRoot.getClass().getName());
        requireType(binder, primaryBinder.getClass().getName());
        if (root == primaryRoot || binder == primaryBinder || read(binder, "mInjector") != null)
            throw new IllegalStateException("Native App content allocation reused a bound owner");
        if (read(binder, "injectorFactory") != read(primaryBinder, "injectorFactory"))
            throw new IllegalStateException("Unexpected native injector factory");
        write(root, "homeStatusBarViewBinder", binder);

        fragment = call(display, "miuiStatusBarFragmentDep");
        requireType(fragment, "com.android.systemui.statusbar.MiuiStatusBarFragmentDep");
        if (fragment == read(primaryInjector, "fragmentDep"))
            throw new IllegalStateException("Native fragment dependency was not fresh");

        // A16's token selects which fragment may animate; both fragments use
        // the same animator and island state producers. A fresh controller
        // would start empty and could not reproduce that ownership contract.
        // Binding must install the App-role gates before either shared owner
        // can be touched; this detached graph does not publish layout providers.
        animator = read(primaryInjector, "animatorController");
        write(fragment, "animatorController", animator);

        // This native store allocates new notification Views; its constructor
        // does not register listeners. AppBarNotificationIcons owns registration
        // and the independent bundle cache after this allocation returns.
        Object factory = read(root, "iconViewStoreFactory");
        notificationStore = call(factory, "create", new Class<?>[]{int.class}, 0);
        requireType(notificationStore, "com.android.systemui.statusbar.notification.icon.ui.viewbinder."
                + "ConnectedDisplaysStatusBarNotificationIconViewStore");
        if (((Number) read(notificationStore, "displayId")).intValue() != 0
                || !((java.util.Map<?, ?>) read(notificationStore, "cachedIcons")).isEmpty())
            throw new IllegalStateException("Unexpected new notification-store state");
        verifyPrimaryUnchanged();
    }

    void verifyPrimaryUnchanged() throws ReflectiveOperationException {
        verifyPrimaryOwners();
        if (window != null) window.verifyDetached();
    }

    void attachWindowProbe(Runnable attached) throws ReflectiveOperationException {
        verifyPrimaryUnchanged();
        if (closing || closed || appTokenReady) throw new IllegalStateException("App probe owner unavailable");
        window.attachProbe(attached);
    }

    String verifyAttachedWindowProbe() throws ReflectiveOperationException {
        verifyPrimaryOwners();
        window.verifyProbeAttached();
        if (!appView.isAttachedToWindow() || appTokenReady)
            throw new IllegalStateException("App content attachment identity changed");
        stateBinding.verifyLiveWindowState();
        View notifications = appView.requireViewById(resource(appView, "id", "notificationIcons"));
        View icons = (View) read(appView, "mStatusBarStatusIcons");
        if (!notifications.isAttachedToWindow() || !icons.isAttachedToWindow())
            throw new IllegalStateException("App icon containers did not attach");
        // Detached verification temporarily reparents icons and registers tint
        // receivers. Never run those mutation probes on live native children.
        return "notificationContainerAttached=true, statusIconsAttached=true, stateCallbacks="
                + stateBinding.registrationCount() + ", HomeOwners=unchanged, " + touchBinding.verify(nativeViewController)
                + ", " + chipBinding.verify();
    }

    void selectLiveRole(int role) throws ReflectiveOperationException {
        requireMain();
        if (closing || closed || !appTokenReady) return;
        shadeTargets.select(role, true);
        selectedRole = role;
    }

    void activateWindow(int role) throws ReflectiveOperationException {
        requireMain();
        if (closing || closed || appTokenReady) throw new IllegalStateException("App owner cannot activate");
        window.promote();
        appTokenReady = true;
        selectLiveRole(role);
    }

    void queryWindowProof(Runnable armed, Runnable active,
            java.util.function.BiConsumer<android.os.Bundle, Throwable> result)
            throws ReflectiveOperationException {
        window.verifyProbeAttached();
        if (windowHandshake != null || closing || closed) throw new IllegalStateException("Window query already owned");
        windowHandshake = new AppBarWindowHandshake(window.root().getContext(), window.root().getWindowToken(), armed, active, result);
    }

    private void verifyPrimaryOwners() throws ReflectiveOperationException {
        requireMain();
        if (read(primaryRoot, "homeStatusBarViewBinder") != primaryBinder
                || read(primaryBinder, "mInjector") != primaryInjector
                || read(primaryInjector, "islandController") != primaryIsland
                || read(primaryInjector, "animatorController") != animator)
            throw new IllegalStateException("Main status-bar owner changed during preparation");
        Object[] current = islandProviders(primaryIsland);
        for (int i = 0; i < current.length; i++) if (current[i] != primaryIslandProviders[i])
            throw new IllegalStateException("Main island layout provider changed during preparation");
        if (privacyBinding != null) privacyBinding.verifyPrimary();
        if (shadeTargets != null) shadeTargets.verifyHome();
        if (islandBinding != null) islandBinding.verifyHomeProviders();
    }

    @Override public void close() throws ReflectiveOperationException {
        requireMain();
        if (closed) return;
        if (windowHandshake != null) { windowHandshake.close(); windowHandshake = null; }
        closing = true;
        appTokenReady = false;
        selectedRole = XiaomiShadeBarSelection.HOME;
        // Move shared shade targets back before detaching their current View.
        // A failed prerequisite retains the entire dependent graph for retry.
        if (shadeTargets != null) { shadeTargets.close(); shadeTargets = null; }
        if (window != null) { window.close(); window = null; }
        if (touchBinding != null) { touchBinding.close(); touchBinding = null; }
        if (chipBinding != null) { chipBinding.close(); chipBinding = null; }
        if (nativeViewController != null) {
            call(nativeViewController, "destroy");
            nativeViewController = null;
        }
        if (incompleteChild != null) {
            try { incompleteChild.close(); }
            catch (Exception failure) { throw new ReflectiveOperationException("Partial App child still owned", failure); }
            incompleteChild = null;
        }

        // Detach/cancel the owner scope before destroying child stores. Attempt
        // every independent consumer, preserving each failed handle and cause.
        java.util.List<Throwable> failures = new java.util.ArrayList<>();
        release(failures, () -> { if (stateBinding != null) { stateBinding.close(); stateBinding = null; } });
        release(failures, () -> { if (modeBinding != null) { modeBinding.close(); modeBinding = null; } });
        release(failures, () -> { if (visibilityBinding != null) { visibilityBinding.close(); visibilityBinding = null; } });
        release(failures, () -> { if (appIconAnimation != null) { appIconAnimation.close(); appIconAnimation = null; } });
        release(failures, () -> { if (homeIconAnimation != null) { homeIconAnimation.close(); homeIconAnimation = null; } });
        release(failures, () -> { if (auxiliaryBinding != null) { auxiliaryBinding.close(); auxiliaryBinding = null; } });
        release(failures, () -> { if (clockBatteryBinding != null) { clockBatteryBinding.close(); clockBatteryBinding = null; } });
        release(failures, () -> { if (systemIcons != null) { systemIcons.close(); systemIcons = null; } });
        release(failures, () -> { if (notificationIcons != null) { notificationIcons.close(); notificationIcons = null; } });
        release(failures, () -> { if (notificationIslandBinding != null) { notificationIslandBinding.close(); notificationIslandBinding = null; } });
        release(failures, () -> { if (statusIconIslandBinding != null) { statusIconIslandBinding.close(); statusIconIslandBinding = null; } });
        release(failures, () -> { if (islandBinding != null) { islandBinding.close(); islandBinding = null; } });
        throwIfIncomplete(failures);

        // These producers/containers must outlive every consumer above.
        if (layoutBinding != null) { layoutBinding.close(); layoutBinding = null; }
        if (privacyBinding != null) { privacyBinding.close(); privacyBinding = null; }
        if (appearance != null) { appearance.close(); appearance = null; }
        if (windowContext != null) { windowContext.close(); windowContext = null; }
        if (appView != null) {
            write(appView, "mInited", false);
            write(appView, "mDependence", null);
            write(appView, "mDarkIconDispatcher", null);
        }
        if (appView != null && appView.isAttachedToWindow())
            throw new IllegalStateException("App View survived content release");
        verifyPrimaryOwners();
        appView = null;
        appInjector = appViewDependency = viewModel = null;
        root = binder = fragment = notificationStore = animator = null;
        nativeComponent = null;
        appNotificationInject = homeNotificationInject = null;
        closed = true;
    }

    boolean isClosed() { return closed; }

    void observeAppearance(java.util.function.BiConsumer<String, Throwable> log) throws ReflectiveOperationException {
        modeBinding.observeAppearance(log);
    }

    String appearanceDiagnostic() throws ReflectiveOperationException {
        if (closing || closed || modeBinding == null) return "ownerClosing";
        View clock = appView.requireViewById(resource(appView, "id", "clock"));
        View icons = appView.requireViewById(resource(appView, "id", "system_icons"));
        return modeBinding.diagnostic() + ", role=" + selectedRole
                + ", clock=" + clock.getVisibility() + "/" + clock.getAlpha()
                + ", icons=" + icons.getVisibility() + "/" + icons.getAlpha();
    }

    boolean awaitingWindowRetirement() {
        return windowHandshake != null && windowHandshake.awaitingRetirement();
    }

    boolean resumeAfterNativeCleanup(Runnable callback) {
        requireMain();
        return notificationIcons != null && notificationIcons.awaitCompletion(callback);
    }

    static void cleanupCreation(AutoCloseable owner, Throwable failure) throws ReflectiveOperationException {
        try { owner.close(); }
        catch (Throwable cleanup) {
            IncompleteCreation retained = new IncompleteCreation(owner, failure);
            retained.addSuppressed(cleanup);
            throw retained;
        }
    }

    private static final class IncompleteCreation extends ReflectiveOperationException {
        final AutoCloseable owner;
        IncompleteCreation(AutoCloseable owner, Throwable cause) {
            super("App child factory failed with outstanding registrations", cause);
            this.owner = owner;
        }
    }

    private interface Release { void run() throws ReflectiveOperationException; }
    private static void release(java.util.List<Throwable> failures, Release action) {
        try { action.run(); }
        catch (ReflectiveOperationException | RuntimeException failure) { failures.add(failure); }
    }
    private static void throwIfIncomplete(java.util.List<Throwable> failures) throws ReflectiveOperationException {
        if (failures.isEmpty()) return;
        ReflectiveOperationException failure = new ReflectiveOperationException("App content cleanup incomplete; owner retained");
        for (Throwable cause : failures) failure.addSuppressed(cause);
        throw failure;
    }

    private static Object[] islandProviders(Object island) throws ReflectiveOperationException {
        return new Object[]{read(island, "clockSpaceProvider"), read(island, "batterySpaceProvider"),
                read(island, "padClockLeftProvider")};
    }

    private static void requireType(Object object, String name) {
        if (object == null || !object.getClass().getName().equals(name))
            throw new IllegalStateException("Missing exact native content type: " + name);
    }

    static Object read(Object owner, String name) throws ReflectiveOperationException {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }

    static void write(Object owner, String name, Object value) throws ReflectiveOperationException {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(owner, value);
    }

    static Object call(Object owner, String name) throws ReflectiveOperationException {
        return call(owner, name, new Class<?>[0]);
    }

    static Object call(Object owner, String name, Class<?>[] types, Object... args)
            throws ReflectiveOperationException {
        Method method = owner.getClass().getMethod(name, types);
        method.setAccessible(true);
        return method.invoke(owner, args);
    }

    private static void requireMain() {
        if (Looper.myLooper() != Looper.getMainLooper())
            throw new IllegalStateException("Status-bar content requires main Looper");
    }
}
