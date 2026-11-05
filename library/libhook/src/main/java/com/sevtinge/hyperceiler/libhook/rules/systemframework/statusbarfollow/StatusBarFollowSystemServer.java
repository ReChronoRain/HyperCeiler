/* SPDX-License-Identifier: AGPL-3.0-or-later */
/*
 * Derived from MiuiBackGestureHook status-bar-follow implementation.
 * Source: https://github.com/wxxsfxyzm/MiuiBackGestureHook
 * Reference commit: 004bc059e5821775917b771d89aa1d8c18a1e669 (includes b6e582f and later fixes)
 * Copyright (C) 2026 MiuiBackGestureHook contributors.
 * Licensed under the Apache License, Version 2.0; modified for HyperCeiler.
 */
package com.sevtinge.hyperceiler.libhook.rules.systemframework.statusbarfollow;

import android.os.Build;

import com.sevtinge.hyperceiler.libhook.rules.systemui.statusbarfollow.BootFuseController;
import com.sevtinge.hyperceiler.libhook.rules.systemui.statusbarfollow.RecentsOpenSession;
import com.sevtinge.hyperceiler.libhook.rules.systemui.statusbarfollow.StatusBarBootFuse;
import com.sevtinge.hyperceiler.libhook.rules.systemui.statusbarfollow.StatusBarFollowGate;
import com.sevtinge.hyperceiler.libhook.rules.systemui.statusbarfollow.StatusBarFollowRuntime;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.libxposed.api.XposedInterface;
import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed;

/**
 * Default-off system_server adapter. The crash-loop safe mode has no system_server alias,
 * so every hook is additionally fenced by the feature gate and exact contract checks.
 */
public final class StatusBarFollowSystemServer extends StatusBarFollowRuntime {
    protected static final String SERVER_BAR_LISTENER_CREATE = "server_bar_follow_listener_create";
    protected static final String SERVER_BAR_LISTENER_UPDATE = "server_bar_follow_listener_update";
    protected static final String SERVER_BAR_START_CREATE = "server_bar_follow_start_create";
    protected static final String SERVER_BAR_START_RUN = "server_bar_follow_start_run";
    protected static final String SERVER_BAR_STATE_CREATE = "server_bar_follow_state_create";
    protected static final String SERVER_BAR_STATE_RUN = "server_bar_follow_state_run";
    protected static final String SERVER_BAR_STATE_PREFIX = "server_bar_follow_state_call_";
    protected static final String SERVER_BAR_CALLBACK_CREATE_PREFIX = "server_bar_follow_callback_create_";
    protected static final String SERVER_BAR_CALLBACK_RUN_PREFIX = "server_bar_follow_callback_run_";
    protected static final String SERVER_BAR_MUTATION_PREFIX = "server_bar_follow_mutation_";
    protected static final String SERVER_BAR_BRIDGE_READY = "server_bar_window_bridge_ready";
    protected static final String SERVER_BAR_FEATURE = "server_bar_owned_multi_status";
    protected static final String SERVER_BAR_APPEARANCE_OWNER = "server_bar_appearance_source_owner";

    private final ThreadLocal<Object> rejectedServerBarState = new ThreadLocal<>();
    private final ServerBarFollowLifecycle serverBarFollowLifecycle = new ServerBarFollowLifecycle();
    private boolean serverBarLifetimeHooksReady;
    private volatile ServerBarWindowBridge serverBarWindowBridge;
    private final AtomicInteger serverNavigationPolicyEpoch = new AtomicInteger();

    @Override public void init() {
        StatusBarBootFuse.Session fuse = null;
        try {
            if (!com.sevtinge.hyperceiler.common.BuildConfig.STATUS_BAR_FOLLOW_SERVER_AVAILABLE) return;
            if (!StatusBarFollowGate.enabled()) return;
            android.content.Context context = EzXposed.getAppContextOrNull();
            if (context == null)
                context = com.sevtinge.hyperceiler.libhook.utils.api.ContextUtils.getContextNoError(
                    com.sevtinge.hyperceiler.libhook.utils.api.ContextUtils.FlAG_ONLY_ANDROID);
            if (context == null) return;
            fuse = StatusBarBootFuse.open(context, "systemserver");
            if (fuse == null) return;
            BootFuseController.Decision decision = fuse.controller().beginInstall(
                com.sevtinge.hyperceiler.common.utils.PrefsBridge.getString(
                    "system_ui_status_bar_boot_fuse_reset_request", ""));
            if (decision != BootFuseController.Decision.ALLOW) {
                moduleLog(LOG_WARN, TAG, "Status-bar follow boot fuse denied system_server install state=" + decision);
                return;
            }
            final StatusBarBootFuse.Session session = fuse;
            if (!session.startBootCompletionObserver(context, () -> {
                try {
                    hookServerBarLifetime(EzXposed.getSafeClassLoader(), Collections.emptySet());
                    if (!serverBarLifetimeHooksReady) {
                        session.close();
                        return;
                    }
                    session.markInstalled();
                } catch (Throwable failure) {
                    rollbackStatusbarHooks();
                    moduleLog(LOG_WARN, TAG, "Deferred system_server status-bar installation failed", failure);
                    session.close();
                }
            })) return;
            registerHotReloadCleanup(() -> {
                rollbackStatusbarHooks();
                session.close();
            });
        } catch (Throwable ignored) {
            // Fail closed: no partial system_server ownership survives a contract failure.
        } finally {
            if (fuse != null && !fuse.hasStarted() && !fuse.hasInstalled()) fuse.close();
        }
    }

    @Override protected String resolveBarFollowDexContainer(Class<?> target) {
        return target.getProtectionDomain().getCodeSource().getLocation().getPath();
    }

    @Override protected boolean isRecentsOpenAttemptCurrentOnOwner(RecentsOpenSession.Attempt attempt) {
        return false;
    }

    @Override protected boolean isRecentsOpenCompletionCurrentOnOwner(RecentsOpenSession.Attempt attempt) {
        return false;
    }

    @Override protected void sendAuthenticatedMiuiHomeOpenBreakCommand(Object controller, boolean toHome) {
        throw new UnsupportedOperationException("system_server status-bar port does not own input handoff");
    }

    @Override protected void publishSystemUiReturnHomeFinish(Object controller, Object transaction) {
        throw new UnsupportedOperationException("system_server status-bar port does not own input handoff");
    }
    protected void hookServerBarLifetime(ClassLoader loader, Set<String> present) {
        if (!com.sevtinge.hyperceiler.common.BuildConfig.STATUS_BAR_FOLLOW_SERVER_AVAILABLE) return;
        if (!StatusBarFollowGate.enabled() || Build.VERSION.SDK_INT != 37) return;
        try {
            Class<?> controller = Class.forName("com.android.server.wm.MiuiStatusBarController", false, loader);
            Method feature = Class.forName("com.android.server.wm.BarFollowAnimationUtils", false, loader)
                    .getDeclaredMethod("MULTIS_STATUSBAR_ENABLED");
            if (feature.getReturnType() != boolean.class || !java.lang.reflect.Modifier.isStatic(feature.getModifiers()))
                throw new NoSuchMethodException("Multi-status feature shape changed");
            if (!present.contains(SERVER_BAR_FEATURE)) recordHookHandle(hook(feature)
                    .setId(SERVER_BAR_FEATURE).intercept(this::serverBarOwnedFeature));
            List<ServerBarCallbackProfile.Entry> callbacks = ServerBarCallbackProfile.resolve(loader);
            String[] writes = {"initFolme", "stopFolmeAnimate", "diableStatusBarVisibilityPolicy",
                    "enableStatusBarVisibility", "bindAssociatedTask", "updateHomeBarParent",
                    "reparentAppBarToBirthParent", "reparentHomeBarToOrigParent", "startStatusBarAnimation",
                    "updateBarAlpha", "updateAppBarAlphaUnCheck", "reparentStausBarToAppTask",
                    "onSystemWindowAdded", "onSystemWindowRemoved", "checkBarAlpha", "updateStatusBarAlpha",
                    "updateAppBarAlpha", "updateHomeBarAlpha", "animateStatusBarTo", "resetStatusBarState",
                    "setStatusBarUpdateState", "resetToInitState", "setAppBarContainer", "updateStatusBar"};
            java.util.ArrayList<Method> nativeWrites = new java.util.ArrayList<>();
            for (String name : writes) {
                Method resolved = null;
                for (Method method : controller.getDeclaredMethods())
                    if (method.getName().equals(name)) {
                        if (resolved != null || method.getReturnType() != void.class
                                || java.lang.reflect.Modifier.isStatic(method.getModifiers()))
                            throw new NoSuchMethodException("Ambiguous native bar mutation " + name);
                        resolved = method;
                    }
                if (resolved == null) throw new NoSuchMethodException(name);
                nativeWrites.add(resolved);
            }
            Class<?> listener = Class.forName(controller.getName() + "$1", false, loader);
            Class<?> impl = Class.forName("com.android.server.wm.BarFollowAnimationImpl", false, loader);
            Class<?> start = Class.forName(impl.getName() + "$9", false, loader);
            Class<?> info = Class.forName("android.window.TransitionInfo", false, loader);
            java.lang.reflect.Constructor<?> listenerConstructor = listener.getDeclaredConstructor(controller);
            java.lang.reflect.Constructor<?> startConstructor = start.getDeclaredConstructor(impl, info);
            Method update = listener.getDeclaredMethod("onUpdate", Object.class, java.util.Collection.class);
            Method run = start.getDeclaredMethod("run");
            Class<?> state = Class.forName(impl.getName() + "$8", false, loader);
            java.lang.reflect.Constructor<?> stateConstructor = state.getDeclaredConstructor(impl, int.class, boolean.class);
            Method stateRun = state.getDeclaredMethod("run");
            if (!Runnable.class.isAssignableFrom(state) || state.getDeclaredField("this$0").getType() != impl
                    || state.getDeclaredField("val$state").getType() != int.class
                    || state.getDeclaredField("val$homeTaskOnTop").getType() != boolean.class
                    || stateRun.getReturnType() != void.class)
                throw new NoSuchMethodException("Native mixed bar state contract changed");
            String[] stateMethods = {"setStatusBarState", "setLastStatusState", "updateStatusBarToRecents",
                    "updateStatusBarToHome", "updateStatusBarAnotherApp", "updateStatusBarCloseWindow",
                    "updateStatusBarTaskSnapshot", "updateStatusBarAppOpen", "updateStatusBarToCurrentApp"};
            Method[] stateCalls = new Method[stateMethods.length];
            for (int i = 0; i < stateCalls.length; i++) {
                stateCalls[i] = controller.getDeclaredMethod(stateMethods[i], i < 2 ? new Class<?>[]{int.class} : new Class<?>[0]);
                if (stateCalls[i].getReturnType() != void.class)
                    throw new NoSuchMethodException(stateMethods[i]);
            }
            if (!listener.getSuperclass().getName().equals("miuix.animation.listener.TransitionListener")
                    || listener.getDeclaredField("this$0").getType() != controller
                    || !Runnable.class.isAssignableFrom(start)
                    || start.getDeclaredField("this$0").getType() != impl
                    || start.getDeclaredField("val$info").getType() != info
                    || impl.getDeclaredField("mStatusBarController").getType() != controller
                    || update.getReturnType() != void.class || run.getReturnType() != void.class)
                throw new NoSuchMethodException("Native bar lifetime contract changed");
            if (!present.contains(SERVER_BAR_LISTENER_CREATE))
                recordHookHandle(hook(listenerConstructor)
                        .setId(SERVER_BAR_LISTENER_CREATE).intercept(this::createServerBarListener));
            if (!present.contains(SERVER_BAR_LISTENER_UPDATE)) recordHookHandle(hook(update)
                    .setId(SERVER_BAR_LISTENER_UPDATE).intercept(this::dispatchServerBarListener));
            if (!present.contains(SERVER_BAR_START_CREATE)) recordHookHandle(hook(startConstructor)
                    .setId(SERVER_BAR_START_CREATE).intercept(this::createServerBarStart));
            if (!present.contains(SERVER_BAR_START_RUN)) recordHookHandle(hook(run)
                    .setId(SERVER_BAR_START_RUN).intercept(this::dispatchServerBarStart));
            if (!present.contains(SERVER_BAR_STATE_CREATE)) recordHookHandle(hook(stateConstructor)
                    .setId(SERVER_BAR_STATE_CREATE).intercept(this::createServerBarStart));
            if (!present.contains(SERVER_BAR_STATE_RUN)) recordHookHandle(hook(stateRun)
                    .setId(SERVER_BAR_STATE_RUN).intercept(this::dispatchServerBarState));
            for (int i = 0; i < stateCalls.length; i++)
                if (!present.contains(SERVER_BAR_STATE_PREFIX + stateMethods[i]))
                    recordHookHandle(hook(stateCalls[i]).setId(SERVER_BAR_STATE_PREFIX + stateMethods[i])
                            .intercept(this::dispatchServerBarStateCall));
            // Only these verified allocation sites create the fenced objects.
            Class<?> ease = Class.forName("miuix.animation.utils.EaseManager$EaseStyle", false, loader);
            boolean ready = deoptimize(controller.getDeclaredMethod("animateStatusBarTo", float.class, float.class, ease, ease));
            ready &= deoptimize(impl.getDeclaredMethod("startStatusBarAnimation", info));
            ready &= deoptimize(impl.getDeclaredMethod("updateBarState", info, int.class));
            ready &= deoptimize(stateRun);
            for (ServerBarCallbackProfile.Entry callback : callbacks) {
                if (!present.contains(SERVER_BAR_CALLBACK_CREATE_PREFIX + callback.id))
                    recordHookHandle(hook(callback.constructor).setId(SERVER_BAR_CALLBACK_CREATE_PREFIX + callback.id)
                            .intercept(this::createServerBarSynthetic));
                if (!present.contains(SERVER_BAR_CALLBACK_RUN_PREFIX + callback.id))
                    recordHookHandle(hook(callback.run).setId(SERVER_BAR_CALLBACK_RUN_PREFIX + callback.id)
                            .intercept(this::dispatchServerBarSynthetic));
                ready &= deoptimize(callback.producer);
            }
            for (Method mutation : nativeWrites) {
                String id = SERVER_BAR_MUTATION_PREFIX + mutation.getName();
                if (!present.contains(id)) recordHookHandle(hook(mutation).setId(id)
                        .intercept(this::dispatchServerBarMutation));
            }
            // Native controller helpers are inlined into each other and the
            // verified bar implementation. Recompile neither while a lease owns writes.
            for (Class<?> owner : new Class<?>[]{controller, impl})
                for (Method method : owner.getDeclaredMethods())
                    if (!java.lang.reflect.Modifier.isAbstract(method.getModifiers())
                            && !java.lang.reflect.Modifier.isNative(method.getModifiers()))
                        ready &= deoptimize(method);
            serverBarLifetimeHooksReady = ready;
            Method systemReady = impl.getDeclaredMethod("systemReady",
                    Class.forName("com.android.server.wm.WindowManagerService", false, loader));
            if (!present.contains(SERVER_BAR_BRIDGE_READY)) recordHookHandle(hook(systemReady)
                    .setId(SERVER_BAR_BRIDGE_READY).intercept(this::serverBarBridgeReady));
            // Do not construct the platform singleton during bootstrap. Its constructor
            // initializes BarFollowAnimationUtils, caching the feature service's fallback
            // before HyperOSCustFeatureResolve is available. systemReady supplies the
            // actual native owner; only hot reload may recover an already-running owner.
            moduleLog(LOG_INFO, TAG, "Native bar lifetime guards installed; activation=" + false + ", ready=" + ready);
        } catch (Throwable failure) {
            serverBarLifetimeHooksReady = false;
            moduleLog(LOG_WARN, TAG, "Native bar lifetime guards unavailable; no real-window activation", failure);
        }
    }

    protected Object createServerBarListener(XposedInterface.Chain chain) throws Throwable {
        Object result = chain.proceed();
        serverBarFollowLifecycle.created(chain.getThisObject(), chain.getArg(0));
        return result;
    }

    protected Object dispatchServerBarListener(XposedInterface.Chain chain) throws Throwable {
        Object callback = chain.getThisObject();
        Object controller = readField(callback, "this$0");
        try (ServerBarFollowLifecycle.Permit permit = serverBarFollowLifecycle.enter(callback, controller)) {
            if (permit == null) return null;
            Object result = chain.proceed();
            ServerBarWindowBridge bridge = serverBarWindowBridge;
            if (bridge != null) bridge.recordNative(controller, null);
            return result;
        }
    }

    protected Object createServerBarStart(XposedInterface.Chain chain) throws Throwable {
        Object result = chain.proceed();
        serverBarFollowLifecycle.created(chain.getThisObject(), readField(chain.getArg(0), "mStatusBarController"));
        return result;
    }

    protected Object dispatchServerBarStart(XposedInterface.Chain chain) throws Throwable {
        Object callback = chain.getThisObject();
        Object controller = readField(readField(callback, "this$0"), "mStatusBarController");
        try (ServerBarFollowLifecycle.Permit permit = serverBarFollowLifecycle.enter(callback, controller)) {
            return permit == null ? null : chain.proceed();
        }
    }

    protected Object dispatchServerBarState(XposedInterface.Chain chain) throws Throwable {
        Object callback = chain.getThisObject();
        Object controller = readField(readField(callback, "this$0"), "mStatusBarController");
        Object previous = rejectedServerBarState.get();
        try (ServerBarFollowLifecycle.Permit permit = serverBarFollowLifecycle.enter(callback, controller)) {
            if (permit == null) rejectedServerBarState.set(controller);
            // This Runnable also owns native navigation-bar work. A stale
            // status lifetime must not discard navigation state or its finish.
            Object result = chain.proceed();
            ServerBarWindowBridge bridge = serverBarWindowBridge;
            if (permit != null && bridge != null)
                bridge.recordNative(controller, (Integer) readField(callback, "val$state"));
            return result;
        } finally {
            if (previous == null) rejectedServerBarState.remove();
            else rejectedServerBarState.set(previous);
        }
    }

    protected Object dispatchServerBarStateCall(XposedInterface.Chain chain) throws Throwable {
        return rejectedServerBarState.get() == chain.getThisObject() ? null : dispatchServerBarMutation(chain);
    }

    protected Object dispatchServerBarMutation(XposedInterface.Chain chain) throws Throwable {
        try (ServerBarFollowLifecycle.Permit permit = serverBarFollowLifecycle.enterMutation(chain.getThisObject())) {
            if (permit == null) return null;
            ServerBarWindowBridge bridge = serverBarWindowBridge;
            String operation = chain.getExecutable().getName();
            if (bridge != null)
                bridge.recordDecision(chain.getThisObject(), operation, "before", chain.getArgs());
            Object result = chain.proceed();
            if (bridge != null)
                bridge.recordDecision(chain.getThisObject(), operation, "after", chain.getArgs());
            return result;
        }
    }

    private Object syntheticBarController(Object owner) throws ReflectiveOperationException {
        return owner.getClass().getName().equals("com.android.server.wm.MiuiStatusBarController")
                ? owner : readField(owner, "mStatusBarController");
    }

    protected Object createServerBarSynthetic(XposedInterface.Chain chain) throws Throwable {
        Object result = chain.proceed();
        serverBarFollowLifecycle.created(chain.getThisObject(), syntheticBarController(chain.getArg(0)));
        return result;
    }

    protected Object dispatchServerBarSynthetic(XposedInterface.Chain chain) throws Throwable {
        Object callback = chain.getThisObject();
        Object controller = syntheticBarController(readField(callback, "f$0"));
        try (ServerBarFollowLifecycle.Permit permit = serverBarFollowLifecycle.enter(callback, controller)) {
            return permit == null ? null : chain.proceed();
        }
    }

    protected Object serverBarBridgeReady(XposedInterface.Chain chain) throws Throwable {
        Object result = chain.proceed();
        installServerBarWindowBridge(chain.getThisObject());
        repairServerNavigationPolicy(chain.getThisObject());
        return result;
    }

    protected void restoreServerBarOwnerAfterHotReload(ClassLoader loader) {
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.CINNAMON_BUN || !serverBarLifetimeHooksReady)
            return;
        try {
            Object instance = Class.forName("com.android.server.wm.BarFollowAnimationStub", false, loader)
                    .getMethod("getInstance").invoke(null);
            if (readField(instance, "mContext") == null || readField(instance, "mWmService") == null)
                return;
            installServerBarWindowBridge(instance);
            repairServerNavigationPolicy(instance);
        } catch (Throwable failure) {
            moduleLog(LOG_WARN, TAG, "Native bar owner restoration unavailable", failure);
        }
    }

    private void repairServerNavigationPolicy(Object impl) {
        try {
            Class<?> policy = Class.forName("com.android.server.wm.BarFollowAnimationUtils", false,
                    impl.getClass().getClassLoader());
            Field cachedDisabled = policy.getDeclaredField("disableMultiBars");
            cachedDisabled.setAccessible(true);
            if (cachedDisabled.getBoolean(null)) return;
            Class<?> features = Class.forName("miui.os.HyperOSCustFeatureResolve", false,
                    impl.getClass().getClassLoader());
            Method resolve = features.getMethod("getBoolean", String.class, boolean.class);
            String feature = "disable_multi_statusbar_navigationbar";
            // Different fallback answers mean the service is absent or unreadable.
            // Neither default alone proves native policy, and we never rewrite it.
            if (!Boolean.TRUE.equals(resolve.invoke(null, feature, false))
                    || !Boolean.TRUE.equals(resolve.invoke(null, feature, true))) return;
            Object navigation = readField(impl, "mNavigationBarController");
            Object wms = readField(impl, "mWmService");
            Object display = readField(impl, "mDisplayContent");
            if (navigation == null || wms == null || display == null
                    || !navigation.getClass().getName().equals("com.android.server.wm.MiuiNavigationBarController"))
                return;
            Object lock = readField(wms, "mGlobalLock");
            android.os.Handler handler = (android.os.Handler) readField(wms, "mAnimationHandler");
            Method retire = impl.getClass().getDeclaredMethod("destroyMiuiNavigationBarController",
                    Class.forName("com.android.server.wm.DisplayContent", false, impl.getClass().getClassLoader()));
            retire.setAccessible(true);
            Method traverse = wms.getClass().getDeclaredMethod("requestTraversal");
            traverse.setAccessible(true);
            int epoch = serverNavigationPolicyEpoch.incrementAndGet();
            handler.post(() -> {
                try {
                    if (serverNavigationPolicyEpoch.get() != epoch
                            || !Boolean.TRUE.equals(resolve.invoke(null, feature, false))
                            || !Boolean.TRUE.equals(resolve.invoke(null, feature, true))) return;
                    synchronized (lock) {
                        if (serverNavigationPolicyEpoch.get() != epoch
                                || readField(impl, "mWmService") != wms
                                || readField(impl, "mDisplayContent") != display
                                || readField(impl, "mNavigationBarController") != navigation)
                            return;
                        // Stock teardown restores the real NavigationBar in the WMS
                        // pending transaction and retires the obsolete controller on
                        // its own Handler. No module Surface/flag writes are needed.
                        retire.invoke(impl, display);
                        traverse.invoke(wms);
                    }
                    moduleLog(LOG_INFO, TAG, "Retired prematurely initialized native multi-navigation owner;"
                            + " authoritative HyperOS policy disables task navigation bars");
                } catch (Throwable failure) {
                    moduleLog(LOG_WARN, TAG, "Native navigation policy reconciliation failed", failure);
                }
            });
        } catch (Throwable failure) {
            moduleLog(LOG_WARN, TAG, "Native navigation policy proof unavailable; preserving owner", failure);
        }
    }

    protected Object serverBarOwnedFeature(XposedInterface.Chain chain) throws Throwable {
        ServerBarWindowBridge bridge = serverBarWindowBridge;
        return bridge != null && bridge.animationEnabled() ? true : chain.proceed();
    }


    private synchronized void installServerBarWindowBridge(Object impl) {
        if (!serverBarLifetimeHooksReady || serverBarWindowBridge != null) return;
        try {
            serverBarWindowBridge = new ServerBarWindowBridge(impl, serverBarFollowLifecycle, (message, failure) -> {
                if (failure == null) moduleLog(LOG_INFO, TAG, message);
                else moduleLog(LOG_WARN, TAG, message, failure);
            });
        } catch (Throwable failure) {
            moduleLog(LOG_WARN, TAG, "WMS bar window bridge unavailable", failure);
        }
    }

    protected boolean reserveServerBarReload() {
        ServerBarWindowBridge bridge = serverBarWindowBridge;
        try {
            if (bridge != null && !bridge.canAwaitClose()) return false;
        } catch (ReflectiveOperationException failure) {
            moduleLog(LOG_WARN, TAG, "Cannot prove safe native bar retirement wait", failure);
            return false;
        }
        serverNavigationPolicyEpoch.incrementAndGet();
        if (bridge != null) {
            bridge.close();
            serverBarWindowBridge = null;
        }
        boolean retired = serverBarFollowLifecycle.reserveReload();
        boolean ready = false;
        try {
            // Waiting happens outside the WMS lock and the native owner Looper.
            // Include the lease acknowledgement/death-recipient unlink turn,
            // which follows the Surface commit and native bookkeeping.
            long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
            retired = retired || serverBarFollowLifecycle.awaitReload(5000L);
            long remainingMillis = Math.max(0L, java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(
                    deadline - System.nanoTime()));
            ready = retired && (bridge == null || bridge.awaitClose(remainingMillis));
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
        if (!ready) {
            if (bridge != null) bridge.recoverAfterRejectedReload(() -> {
                serverBarFollowLifecycle.cancelReloadReservation();
                installServerBarWindowBridge(bridge.nativeOwner());
            });
            else serverBarFollowLifecycle.cancelReloadReservation();
            moduleLog(LOG_WARN, TAG, "Native bar retirement not committed; rejected reload keeps old runtime recovery");
        } else if (bridge != null) {
            moduleLog(LOG_INFO, TAG, "Native bar restoration and lease cleanup completed within this hot-reload request");
        }
        return ready;
    }

    protected Object[] saveServerBarRetiredOwners() {
        return new Object[]{serverBarFollowLifecycle.retiredControllers(), serverBarFollowLifecycle.retiredCallbacks()};
    }

    protected void restoreServerBarRetiredOwners(Object saved) {
        if (!(saved instanceof Object[])) return;
        Object[] state = (Object[]) saved;
        if (state.length <= 17 || !(state[17] instanceof Object[])) return;
        Object[] barState = (Object[]) state[17];
        if (barState.length != 2 || !(barState[0] instanceof Object[]) || !(barState[1] instanceof Object[][]))
            return;
        java.util.ArrayList<Object> owners = new java.util.ArrayList<>();
        for (Object candidate : (Object[]) barState[0])
            if (candidate != null
                    && candidate.getClass().getName().equals("com.android.server.wm.MiuiStatusBarController"))
                owners.add(candidate);
        serverBarFollowLifecycle.restoreRetiredControllers(owners.toArray());
        serverBarFollowLifecycle.restoreRetiredCallbacks((Object[][]) barState[1]);
    }


}
