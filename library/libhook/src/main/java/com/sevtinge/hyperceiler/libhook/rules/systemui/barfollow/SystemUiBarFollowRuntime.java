/*
 * Derived from MiuiBackGestureHook status-bar-follow implementation.
 * Source: https://github.com/wxxsfxyzm/MiuiBackGestureHook
 * Reference commit: 004bc059e5821775917b771d89aa1d8c18a1e669 (includes b6e582f and later fixes)
 * Copyright (C) 2026 MiuiBackGestureHook contributors.
 * Licensed under the Apache License, Version 2.0; modified for HyperCeiler.
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow;

import android.os.Build;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.view.SurfaceControl;
import android.window.TransitionInfo;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.Executor;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.FieldUsingType;

import io.github.libxposed.api.XposedInterface;
import com.sevtinge.hyperceiler.libhook.rules.systemui.statusbarfollow.StatusBarFollowGate;
import com.sevtinge.hyperceiler.libhook.rules.systemui.statusbarfollow.StatusBarFollowPreferences;

import com.sevtinge.hyperceiler.libhook.rules.systemui.statusbarfollow.StatusBarFollowRuntime;
import com.sevtinge.hyperceiler.libhook.rules.systemui.statusbarfollow.RecentsOpenSession;

/** OS4 Recents lifecycle and optional bar adaptation. Never enables native multi-bar mode. */
public abstract class SystemUiBarFollowRuntime extends StatusBarFollowRuntime {
    protected final java.util.concurrent.atomic.AtomicInteger systemUiDexResolutionInFlight =
            new java.util.concurrent.atomic.AtomicInteger();
    private static final String RECENTS = "com.android.wm.shell.recents.RecentsTransitionHandler";
    protected static final String BAR_PREPARE = "systemui_recents_bar_follow_prepare_common";
    protected static final String BAR_START = "systemui_recents_bar_follow_start";
    protected static final String BAR_REUSE_BIND = "systemui_recents_bar_reuse_bind";
    protected static final String BAR_REUSE_DIRECTION = "systemui_recents_bar_reuse_direction";
    protected static final String BAR_REUSE_TARGETS = "systemui_recents_bar_reuse_appeared_targets";
    protected static final String RECENTS_OPEN_FINISH = "systemui_recents_open_native_finish";
    protected static final String RECENTS_OPEN_FINISH_START = "systemui_recents_open_native_finish_start";
    protected static final String RECENTS_OPEN_FINISH_CONSUMED = "systemui_recents_open_native_finish_consumed";
    protected static final String BAR_FOLME_UPDATE = "systemui_bar_folme_update";
    protected static final String BAR_FOLME_COMPLETE = "systemui_bar_folme_complete";
    protected static final String BAR_FOLME_CANCEL = "systemui_bar_folme_cancel";
    protected static final String BAR_TASK_PENDING = "systemui_bar_task_pending";
    protected static final String BAR_TASK_STARTING = "systemui_bar_task_starting";
    protected static final String BAR_TASK_CANCELLED = "systemui_bar_task_cancelled";
    protected static final String BAR_TASK_FINISHED = "systemui_bar_task_finished";
    protected static final String BAR_VIEW_INIT = "systemui_app_bar_view_init";
    protected static final String BAR_VIEW_DRIP = "systemui_app_bar_view_drip";
    protected static final String BAR_VIEW_FACTORY = "systemui_app_bar_view_factory";
    protected static final String BAR_HOME_BOUND = "systemui_app_bar_home_bound";
    protected static final String BAR_DARK_STATE = "systemui_app_bar_dark_state";
    protected static final String BAR_APP_APPEARANCE = "systemui_app_bar_window_appearance";
    protected static final String BAR_APP_TINT_ANIMATION = "systemui_app_bar_tint_animation";
    protected static final String BAR_PRIVACY_ROLE = "systemui_app_bar_privacy_role";
    protected static final String BAR_PRIVACY_PROMPT = "systemui_app_bar_privacy_prompt";
    protected static final String BAR_NOTIFICATION_ROLE = "systemui_bar_notification_animation_role";
    protected static final String BAR_ISLAND_ROLE = "systemui_bar_island_animation_role";
    protected static final String BAR_SHOW_ROLE = "systemui_app_bar_show_role";
    protected static final String BAR_HIDE_ROLE = "systemui_app_bar_hide_role";
    protected static final String BAR_CLOCK_SHOW_ROLE = "systemui_app_bar_clock_show_role";
    protected static final String BAR_CLOCK_HIDE_ROLE = "systemui_app_bar_clock_hide_role";
    private boolean appViewRoleHooksReady;
    private volatile XiaomiShadeBarEvents shadeBarEvents;
    private volatile XiaomiBarFolme barFolmeProbe;
    private volatile boolean barFolmeProbePending;
    private volatile AppBarContentPreparation pendingAppContentCleanup;
    private volatile SharedPreferences barFollowPreferences;
    private boolean barFollowPreferenceFailureLogged;
    private final AppBarFeatureState barFeatureState = new AppBarFeatureState();
    private final android.os.Handler barPreferenceHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final AppBarStartupRecovery barStartupRecovery =
            new AppBarStartupRecovery(barFeatureState, barPreferenceHandler::post);
    private ClassLoader barFollowClassLoader;
    private boolean barFollowRestartPending;
    private boolean appBarRetiring;
    private volatile java.util.concurrent.CountDownLatch barReloadCompletion;
    private final SharedPreferences.OnSharedPreferenceChangeListener barFollowPreferenceListener = (preferences, key) -> {
        if (key == null || StatusBarFollowPreferences.KEY_RESTORE_STATUS_BAR_FOLLOW.equals(key))
            barPreferenceHandler.post(this::reconcileBarFollowPreference);
    };
    protected static final String BAR_STATE = "systemui_recents_bar_follow_state";
    protected static final String BAR_CLEANUP = "systemui_recents_bar_follow_cleanup";
    // Diagnostic IDs only: never use identityHashCode as an acceptance token.
    private final java.util.concurrent.atomic.AtomicLong barTraceSequence =
            new java.util.concurrent.atomic.AtomicLong();
    private final Object barCallbackLock = new Object();
    private RecentsBarCallbackSession barCallbackSession;
    private volatile boolean barHooksReady;
    private final Object barReuseLock = new Object();
    private RecentsBarReuseSession barReuseSession;
    // Independent of bar preferences/callback delivery. Shell owns these task identities.
    private volatile RecentsOpenSession recentsOpenSession;
    private RecentsOpenSession recentsOpenGeneration;
    private RecentsOpenSession.Attempt pendingRecentsOpenFinish;
    private Object emptyRecentsFinishChange;
    private Object doNotPipRecentsFinishChange;
    private java.lang.reflect.Field[] recentsFinishChangeFields;
    private final Object recentsOpenLock = new Object();
    private boolean recentsOpenHooksActive;
    private volatile boolean nativeBarWindowActive;
    private final RecentsBarFollowController recentsBars = new RecentsBarFollowController(
            (message, failure) -> {
                if (failure == null) moduleLog(LOG_INFO, TAG, "RecentsBarFollow: " + message);
                else moduleLog(LOG_WARN, TAG, "RecentsBarFollow: " + message, failure);
            });

    private void validateSystemUiStatusbarContracts(ClassLoader loader) throws ReflectiveOperationException {
        Class.forName("com.android.systemui.statusbar.data.repository.StatusBarModePerDisplayRepositoryImpl", false, loader);
        Class.forName("com.android.systemui.statusbar.data.repository.StatusBarModePerDisplayRepositoryImpl$statusBarAppearance$1", false, loader);
        Class.forName("com.android.wm.shell.transition.DefaultTransitionHandler", false, loader);
        Class.forName("com.android.wm.shell.transition.Transitions$TransitionHandler", false, loader);
        Class.forName("com.android.wm.shell.recents.RecentsTransitionHandler", false, loader);
        Class<?> flow = Class.forName("kotlinx.coroutines.flow.Flow", false, loader);
        Class<?> adapter = Class.forName("com.android.systemui.util.kotlin.JavaAdapterKt", false, loader);
        adapter.getMethod("collectFlow$default", android.view.View.class, flow, java.util.function.Consumer.class,
            kotlin.coroutines.CoroutineContext.class, int.class);
    }

    protected void hookRecentsBarFollow(ClassLoader loader, Set<String> present) {
        if (Build.VERSION.SDK_INT != 37) return;
        try {
            Class<?> handler = Class.forName(RECENTS, false, loader);
            Class<?> controller = Class.forName(RECENTS + "$RecentsController", false, loader);
            Method prepare = requireExactDeclaredMethod(handler, "startRecentsTransition",
                    IBinder.class.getName(), "android.app.PendingIntent", "android.content.Intent",
                    "android.os.Bundle", "android.window.WindowContainerTransaction",
                    "android.app.IApplicationThread", "com.android.wm.shell.recents.IRecentsAnimationRunner");
            Method start = requireExactDeclaredMethod(handler, "startAnimation", "boolean",
                    "android.os.IBinder", "android.window.TransitionInfo",
                    "android.view.SurfaceControl$Transaction", "android.view.SurfaceControl$Transaction",
                    "com.android.wm.shell.transition.Transitions$TransitionFinishCallback");
            Method state = null;
            // Anonymous classes are absent from Class.getDeclaredClasses(). The OS4
            // source names this wrapper $1; require its complete Binder/owner contract.
            for (Class<?> candidate : new Class<?>[]{Class.forName(RECENTS + "$1", false, loader)}) {
                if (!android.os.IBinder.class.isAssignableFrom(candidate)) continue;
                try {
                    Method method = candidate.getDeclaredMethod("onGestureStateUpdated", int.class);
                    if (method.getReturnType() != void.class
                            || candidate.getDeclaredField("this$0").getType() != handler) continue;
                    if (state != null) throw new IllegalStateException("Ambiguous bar callback");
                    state = method;
                } catch (NoSuchMethodException | NoSuchFieldException ignored) { }
            }
            if (state == null) throw new NoSuchMethodException("Owned gesture state callback");
            state.setAccessible(true);
            Method cleanup = controller.getDeclaredMethod("cleanUp");
            cleanup.setAccessible(true);
            // Resolve the whole contract before installing any lifecycle key.
            handler.getDeclaredField("mExecutor");
            controller.getDeclaredField("mTransition");
            if (!present.contains(BAR_PREPARE)) recordHookHandle(hook(prepare).setId(BAR_PREPARE)
                    .intercept(this::prepareRecentsBarFollow));
            if (!present.contains(BAR_START)) recordHookHandle(hook(start).setId(BAR_START)
                    .intercept(this::startRecentsBarFollow));
            if (!present.contains(BAR_CLEANUP)) recordHookHandle(hook(cleanup).setId(BAR_CLEANUP)
                    .intercept(this::cleanupRecentsBarFollow));
            if (!present.contains(BAR_STATE)) recordHookHandle(hook(state).setId(BAR_STATE)
                    .intercept(this::stateRecentsBarFollow));
            try { hookRecentsBarReuse(loader, controller, present); }
            catch (Throwable failure) {
                moduleLog(LOG_WARN, TAG, "Recents bar reuse handoff unavailable", failure);
            }
            try { hookShadeBarEvents(loader, present); }
            catch (ReflectiveOperationException | RuntimeException failure) {
                moduleLog(LOG_WARN, TAG, "App bar task event hooks unavailable", failure);
            }
            hookAppBarViewRole(loader, present);
            try { hookHomeBarReady(loader, present); }
            catch (Throwable failure) {
                moduleLog(LOG_WARN, TAG, "Home status-bar startup recovery unavailable; retaining existing activation", failure);
            }
            hookBarFolmeProbe(loader, present);
            inspectNativeBarContentContracts(loader);
            barHooksReady = true;
            ensureBarFollowPreferenceListener(loader);
            moduleLog(LOG_INFO, TAG, "OS4 recents bar visual hooks ready; activation follows preference");
        } catch (Throwable failure) {
            moduleLog(LOG_WARN, TAG, "Recents bar hooks unavailable; preserving native bars", failure);
        }
    }

    private void hookRecentsBarReuse(ClassLoader loader, Class<?> controller,
            Set<String> present) throws Throwable {
        Method direction = null;
        for (Method candidate : controller.getDeclaredMethods()) {
            if (!candidate.getName().startsWith("lambda$setWillFinishToHome$")
                    || candidate.getReturnType() != void.class
                    || java.lang.reflect.Modifier.isStatic(candidate.getModifiers())
                    || !java.util.Arrays.equals(candidate.getParameterTypes(), new Class<?>[]{boolean.class})) continue;
            if (direction != null) throw new NoSuchMethodException("Ambiguous Recents direction callback");
            direction = candidate;
        }
        if (direction == null) throw new NoSuchMethodException("Native Recents direction callback");
        systemUiDexResolutionInFlight.incrementAndGet();
        try {
            ensureDexKitLibraryLoaded();
            try (DexKitBridge dex = DexKitBridge.create(resolveBarFollowDexContainer(controller))) {
                java.util.List<MethodData> matches = dex.findMethod(FindMethod.create().matcher(
                        MethodMatcher.create().declaredClass(controller.getName()).name(direction.getName()).paramCount(1)));
                if (matches.size() != 1) throw new NoSuchMethodException("Recents direction DEX identity");
                MethodData target = matches.get(0);
                if (target.getUsingFields().size() != 1
                        || target.getUsingFields().get(0).getUsingType() != FieldUsingType.Write
                        || !target.getUsingFields().get(0).getField().getFieldInstance(loader)
                        .equals(controller.getDeclaredField("mWillFinishToHome")))
                    throw new NoSuchMethodException("Recents direction no longer owns its native field");
                java.util.List<MethodData> callers = target.getCallers();
                if (callers.size() != 1) throw new NoSuchMethodException("Ambiguous Recents direction bridge");
                MethodData bridgeData = callers.get(0);
                Method bridge = bridgeData.getMethodInstance(loader);
                if (bridge.getDeclaringClass() != controller || bridge.getReturnType() != void.class
                        || !java.lang.reflect.Modifier.isStatic(bridge.getModifiers())
                        || !java.util.Arrays.equals(bridge.getParameterTypes(), new Class<?>[]{controller, boolean.class}))
                    throw new NoSuchMethodException("Recents direction bridge contract");
                java.util.List<MethodData> dispatchers = bridgeData.getCallers();
                if (dispatchers.size() != 1) throw new NoSuchMethodException("Ambiguous Recents direction dispatcher");
                Method run = dispatchers.get(0).getMethodInstance(loader);
                Class<?> runnable = run.getDeclaringClass();
                if (!Runnable.class.isAssignableFrom(runnable) || !run.getName().equals("run")
                        || run.getParameterCount() != 0 || run.getReturnType() != void.class
                        || runnable.getDeclaredField("f$0").getType() != controller
                        || runnable.getDeclaredField("f$1").getType() != boolean.class
                        || runnable.getDeclaredField("$r8$classId").getType() != int.class)
                    throw new NoSuchMethodException("Recents direction Runnable ownership");
                if (!deoptimize(bridge) || !deoptimize(run))
                    throw new IllegalStateException("Recents direction callers could not be deoptimized");
            }
        } finally { systemUiDexResolutionInFlight.decrementAndGet(); }
        direction.setAccessible(true);
        if (!present.contains(BAR_REUSE_DIRECTION)) recordHookHandle(hook(direction).setId(BAR_REUSE_DIRECTION)
                .intercept(this::handoffRecentsBarReuse));
        hookRecentsOpenFinish(loader, controller, present);
        synchronized (recentsOpenLock) { recentsOpenHooksActive = true; }
        try { hookRecentsBarTargets(loader, controller, present); }
        catch (Throwable failure) {
            moduleLog(LOG_WARN, TAG, "Recents accepted-target observation unavailable; preserving original task path", failure);
        }
    }

    private void hookRecentsBarTargets(ClassLoader loader, Class<?> controller, Set<String> present)
            throws Throwable {
        systemUiDexResolutionInFlight.incrementAndGet();
        try (DexKitBridge dex = DexKitBridge.create(resolveBarFollowDexContainer(controller))) {
            Class<?> runner = Class.forName("com.android.wm.shell.recents.IRecentsAnimationRunner", false, loader);
            Class<?> targetArray = java.lang.reflect.Array.newInstance(Class.forName(
                    "android.view.RemoteAnimationTarget", false, loader), 0).getClass();
            Method nativeAppeared = runner.getDeclaredMethod("onTasksAppeared",
                    targetArray, TransitionInfo.class);
            MethodData selected = null;
            for (MethodData candidate : dex.findMethod(FindMethod.create().matcher(
                    MethodMatcher.create().declaredClass(controller.getName()).paramCount(2)))) {
                if (!candidate.isMethod()) continue;
                Method method = candidate.getMethodInstance(loader);
                if (method.getReturnType() != void.class || java.lang.reflect.Modifier.isStatic(method.getModifiers())
                        || !java.util.Arrays.equals(method.getParameterTypes(),
                        new Class<?>[]{java.util.ArrayList.class, TransitionInfo.class})) continue;
                boolean invokesNative = false;
                for (MethodData invoked : candidate.getInvokes())
                    if (invoked.getDeclaredClassName().equals(runner.getName())
                            && invoked.getMethodInstance(loader).equals(nativeAppeared)) invokesNative = true;
                if (!invokesNative) continue;
                if (selected != null) throw new NoSuchMethodException("Ambiguous native appeared-target dispatch");
                selected = candidate;
            }
            if (selected == null) throw new NoSuchMethodException("Native appeared-target dispatch");
            java.util.List<MethodData> bridges = selected.getCallers();
            if (bridges.size() != 1) throw new NoSuchMethodException("Ambiguous appeared-target bridge");
            Method bridge = bridges.get(0).getMethodInstance(loader);
            if (bridge.getDeclaringClass() != controller || bridge.getReturnType() != void.class
                    || !java.lang.reflect.Modifier.isStatic(bridge.getModifiers())
                    || !java.util.Arrays.equals(bridge.getParameterTypes(),
                    new Class<?>[]{controller, java.util.ArrayList.class, TransitionInfo.class}))
                throw new NoSuchMethodException("Native appeared-target bridge contract");
            java.util.List<MethodData> dispatchers = bridges.get(0).getCallers();
            if (dispatchers.size() != 1) throw new NoSuchMethodException("Ambiguous appeared-target Runnable");
            Method run = dispatchers.get(0).getMethodInstance(loader);
            Class<?> runnable = run.getDeclaringClass();
            if (!Runnable.class.isAssignableFrom(runnable) || !run.getName().equals("run")
                    || run.getReturnType() != void.class || run.getParameterCount() != 0
                    || runnable.getDeclaredField("f$0").getType() != controller
                    || runnable.getDeclaredField("f$1").getType() != Object.class
                    || runnable.getDeclaredField("f$2").getType() != Object.class
                    || runnable.getDeclaredField("$r8$classId").getType() != int.class
                    || !deoptimize(bridge) || !deoptimize(run))
                throw new IllegalStateException("Native appeared-target caller could not be fenced");
            Method method = selected.getMethodInstance(loader);
            method.setAccessible(true);
            if (!present.contains(BAR_REUSE_TARGETS)) recordHookHandle(hook(method).setId(BAR_REUSE_TARGETS)
                    .intercept(this::observeRecentsBarTargets));
        } finally { systemUiDexResolutionInFlight.decrementAndGet(); }
    }

    private void hookRecentsOpenFinish(ClassLoader loader, Class<?> controller, Set<String> present)
            throws Throwable {
        Class<?> transitions = Class.forName("com.android.wm.shell.transition.Transitions", false, loader);
        Class<?> pending = Class.forName(controller.getName() + "$PendingFinishTransitionHandler", false, loader);
        if (pending.getDeclaredField("this$1").getType() != controller)
            throw new NoSuchFieldException("Owned Recents finish handler");
        Method start = requireExactDeclaredMethod(transitions, "startTransition", "android.os.IBinder",
                "int", "android.window.WindowContainerTransaction",
                "com.android.wm.shell.transition.Transitions$TransitionHandler");
        Method consumed = requireExactDeclaredMethod(pending, "onTransitionConsumed", "void",
                "android.os.IBinder", "boolean", "android.view.SurfaceControl$Transaction");
        Method finish = requireExactDeclaredMethod(controller, "onFinishInner", "void",
                "android.window.WindowContainerTransaction");
        systemUiDexResolutionInFlight.incrementAndGet();
        try (DexKitBridge dex = DexKitBridge.create(resolveBarFollowDexContainer(controller))) {
            java.util.List<MethodData> matches = dex.findMethod(FindMethod.create().matcher(
                    MethodMatcher.create().declaredClass(controller.getName()).name(finish.getName()).paramCount(1)));
            if (matches.size() != 1) throw new NoSuchMethodException("Recents finish DEX identity");
            MethodData target = matches.get(0);
            boolean cleanup = false, callback = false;
            for (MethodData invoked : target.getInvokes()) {
                if (invoked.getDeclaredClassName().equals(controller.getName())
                        && invoked.getName().equals("cleanUp")) cleanup = true;
                if (invoked.getDeclaredClassName().equals(
                        "com.android.wm.shell.transition.Transitions$TransitionFinishCallback")
                        && invoked.getName().equals("onTransitionFinished")) callback = true;
            }
            if (!cleanup || !callback) throw new NoSuchMethodException("Recents finish no longer owns cleanup/callback");
            for (MethodData caller : target.getCallers()) {
                if (!deoptimize(caller.getMethodInstance(loader)))
                    throw new IllegalStateException("Recents finish caller not deoptimized");
                for (MethodData dispatcher : caller.getCallers())
                    if (!deoptimize(dispatcher.getMethodInstance(loader)))
                        throw new IllegalStateException("Recents finish dispatcher not deoptimized");
            }
        } finally { systemUiDexResolutionInFlight.decrementAndGet(); }
        if (!present.contains(RECENTS_OPEN_FINISH)) recordHookHandle(hook(finish).setId(RECENTS_OPEN_FINISH)
                .intercept(this::observeRecentsOpenFinish));
        if (!present.contains(RECENTS_OPEN_FINISH_START)) recordHookHandle(hook(start).setId(RECENTS_OPEN_FINISH_START)
                .intercept(this::observeRecentsOpenFinishStart));
        if (!present.contains(RECENTS_OPEN_FINISH_CONSUMED)) recordHookHandle(hook(consumed).setId(RECENTS_OPEN_FINISH_CONSUMED)
                .intercept(this::observeRecentsOpenFinishConsumed));
    }

    protected abstract String resolveBarFollowDexContainer(Class<?> target) throws Throwable;

    // All controller/Task reads here execute on Shell's own executor.
    private Object singleBarReuseTask(Object controller) throws Throwable {
        Object value = readField(controller, "mPausingTasks");
        Object opening = readField(controller, "mOpeningTasks");
        if (!(value instanceof java.util.List) || !(opening instanceof java.util.List)) return null;
        java.util.List<?> paused = (java.util.List<?>) value;
        java.util.List<?> opened = (java.util.List<?>) opening;
        Object nativeType = readField(controller, "mRemoteToRecentType");
        boolean fromHome = nativeType instanceof Enum
                && "LAUNCH_FROM_HOME".equals(((Enum<?>) nativeType).name());
        Object task = null;
        boolean taskOpening = false;
        int homeCount = 0;
        // Native launcher OPEN can contain Home as the pausing participant and
        // the application as opening. It is still exactly one application.
        for (java.util.List<?> group : new java.util.List<?>[]{paused, opened}) {
            for (Object candidate : group) {
                Object candidateInfo = readField(candidate, "mTaskInfo");
                if (candidateInfo == null) return null;
                if (Integer.valueOf(2).equals(invokeAnyMethod(candidateInfo, "getActivityType", new Object[0]))) {
                    if (!fromHome || ++homeCount > 1 || group != paused
                            || !Integer.valueOf(0).equals(readField(candidateInfo, "displayId"))
                            || !Integer.valueOf(1).equals(invokeAnyMethod(candidateInfo, "getWindowingMode", new Object[0]))
                            || !matchesClosingHomeChange(controller, candidate, candidateInfo)) return null;
                    continue;
                }
                if (task != null) return null;
                task = candidate;
                taskOpening = group == opened;
            }
        }
        if (task == null || (taskOpening && !fromHome
                && !Boolean.TRUE.equals(readField(task, "mOpenFromPausingOrClosing")))) return null;
        android.app.ActivityManager.RunningTaskInfo info =
                (android.app.ActivityManager.RunningTaskInfo) readField(task, "mTaskInfo");
        if (info == null || info.taskId < 0 || !Integer.valueOf(0).equals(readField(info, "displayId"))
                || !Integer.valueOf(1).equals(invokeAnyMethod(info, "getWindowingMode", new Object[0]))
                || !Integer.valueOf(1).equals(invokeAnyMethod(info, "getActivityType", new Object[0]))
                || !Boolean.FALSE.equals(readField(task, "mIsTranslucent"))) return null;
        for (String name : new String[]{"mLeash", "mTaskSurface"}) {
            Object surface = readField(task, name);
            if (!(surface instanceof SurfaceControl) || !((SurfaceControl) surface).isValid()) return null;
        }
        TransitionInfo transitionInfo = (TransitionInfo) readField(controller, "mInfo");
        if (transitionInfo == null || transitionInfo.getRootCount() != 1
                || transitionInfo.getRoot(0).getDisplayId() != 0) return null;
        int matches = 0;
        for (TransitionInfo.Change change : transitionInfo.getChanges()) {
            if (change.getTaskInfo() == null || change.getTaskInfo().taskId != info.taskId) continue;
            if (!invokeAnyMethod(change, "getContainer", new Object[0]).equals(readField(task, "mToken"))
                    || change.getLeash() != readField(task, "mTaskSurface")
                    || change.getStartAbsBounds().isEmpty() || change.getEndAbsBounds().isEmpty()) return null;
            matches++;
        }
        if (matches != 1) return null;
        return task;
    }

    private boolean matchesClosingHomeChange(Object controller, Object task, Object taskInfo) throws Throwable {
        TransitionInfo info = (TransitionInfo) readField(controller, "mInfo");
        if (info == null) return false;
        Object token = readField(task, "mToken");
        Object surface = readField(task, "mTaskSurface");
        if (token == null || !(surface instanceof SurfaceControl) || !((SurfaceControl) surface).isValid()) return false;
        int matches = 0;
        for (TransitionInfo.Change change : info.getChanges()) {
            if (change.getTaskInfo() == null || !token.equals(invokeAnyMethod(change, "getContainer", new Object[0]))) continue;
            if (!Integer.valueOf(change.getTaskInfo().taskId).equals(readField(taskInfo, "taskId"))
                    || change.getLeash() != surface || (change.getMode() != 2 && change.getMode() != 4)
                    || !Integer.valueOf(2).equals(invokeAnyMethod(change.getTaskInfo(), "getActivityType", new Object[0]))
                    || change.getStartAbsBounds().isEmpty() || change.getEndAbsBounds().isEmpty()) return false;
            matches++;
        }
        return matches == 1;
    }

    private boolean liveBarReuseController(Object owner, Object controller, Object token) throws Throwable {
        if (!liveBarReuseOwner(owner, controller, token)) return false;
        Object tasks = readField(controller, "mClosingTasks");
        return tasks instanceof java.util.List && ((java.util.List<?>) tasks).isEmpty();
    }

    private boolean liveBarReuseOwner(Object owner, Object controller, Object token) throws Throwable {
        return nativeBarWindowActive && liveRecentsOpenOwner(owner, controller, token);
    }

    private boolean liveRecentsOpenOwner(Object owner, Object controller, Object token) throws Throwable {
        return liveRecentsOpenOwner(owner, controller, token, false);
    }

    private boolean liveRecentsOpenOwner(Object owner, Object controller, Object token,
            boolean finishing) throws Throwable {
        if (token == null
                || invokeAnyMethod(owner, "findController", new Object[]{token}) != controller
                || !Integer.valueOf(0).equals(readField(controller, "mDisplayId"))
                || readField(controller, "mFinishCB") == null || readField(controller, "mListener") == null
                || !Boolean.FALSE.equals(readField(controller, "mAwaitingCancelCompletion"))
                || !Boolean.FALSE.equals(readField(controller, "mKeyguardLocked"))
                || readField(controller, "mTakeoverHandler") != null
                || (!finishing && readField(controller, "mPendingFinishTransition") != null)) return false;
        return true;
    }

    private void bindRecentsOpen(Object owner, Object token, Object info) {
        synchronized (recentsOpenLock) {
            invalidateRecentsOpen();
            if (!recentsOpenHooksActive) return;
            try {
                invokeAnyMethod(readField(owner, "mExecutor"), "assertCurrentThread", new Object[0]);
                Object controller = invokeAnyMethod(owner, "findController", new Object[]{token});
                if (controller == null || readField(controller, "mInfo") != info
                        || !liveRecentsOpenOwner(owner, controller, token)) return;
                Object task = singleBarReuseTask(controller);
                if (task == null) return;
                android.app.ActivityManager.RunningTaskInfo taskInfo =
                        (android.app.ActivityManager.RunningTaskInfo) readField(task, "mTaskInfo");
                Object taskToken = readField(task, "mToken");
                if (taskToken == null || taskInfo.topActivity == null) return;
                Object type = readField(controller, "mRemoteToRecentType");
                Object opening = readField(controller, "mOpeningTasks");
                boolean launchFromHome = type instanceof Enum
                        && "LAUNCH_FROM_HOME".equals(((Enum<?>) type).name())
                        && ((TransitionInfo) info).getType() == 1
                        && opening instanceof java.util.List && ((java.util.List<?>) opening).contains(task);
                recentsOpenSession = new RecentsOpenSession(owner, controller, token, info, task,
                        taskInfo.taskId, taskToken, taskInfo.topActivity, launchFromHome);
                recentsOpenGeneration = recentsOpenSession;
                if (launchFromHome && Boolean.FALSE.equals(readField(controller, "mWillFinishToHome")))
                    observeRecentsOpenDirection(controller, false);
            } catch (Throwable failure) {
                moduleLog(LOG_WARN, TAG, "Native Recents OPEN binding unavailable", failure);
            }
        }
    }

    private void invalidateRecentsOpen() {
        synchronized (recentsOpenLock) {
            RecentsOpenSession session = recentsOpenSession;
            if (session != null) session.close();
            recentsOpenAttempt = null;
            recentsOpenSession = null;
            recentsOpenGeneration = null;
            pendingRecentsOpenFinish = null;
        }
    }

    @Override
    protected boolean isRecentsOpenCompletionCurrentOnOwner(RecentsOpenSession.Attempt attempt)
            throws Throwable {
        synchronized (recentsOpenLock) {
            invokeAnyMethod(readField(attempt.session.owner, "mExecutor"), "assertCurrentThread", new Object[0]);
            return recentsOpenHooksActive && attempt.isCompletedToApp()
                    && recentsOpenGeneration == attempt.session;
        }
    }

    protected Object observeRecentsOpenFinish(XposedInterface.Chain chain) throws Throwable {
        RecentsOpenSession.Attempt attempt = null;
        synchronized (recentsOpenLock) {
            RecentsOpenSession.Attempt current = recentsOpenAttempt;
            try {
                if (current != null && current.session.controller == chain.getThisObject()
                        && isRecentsOpenAttemptCurrentOnOwner(current, true)
                        && (finishesRecentsTaskToTop(chain.getArg(0), current.session.taskToken)
                        || (chain.getArg(0) == null && current.hasPendingFinish(
                        readField(chain.getThisObject(), "mPendingFinishTransition")))
                        || (current.session.launchFromHome
                        && readField(chain.getThisObject(), "mPendingFinishTransition") == null
                        && (chain.getArg(0) == null || Boolean.TRUE.equals(
                        invokeAnyMethod(chain.getArg(0), "isEmpty", new Object[0]))
                        || finishesRecentsRestoringOnlyHome(chain.getArg(0), current.session))))) {
                    attempt = current;
                    pendingRecentsOpenFinish = current;
                }
            } catch (Throwable failure) {
                moduleLog(LOG_WARN, TAG, "Native Recents completion proof unavailable", failure);
            }
        }
        Object result;
        try { result = chain.proceed(); } // Native owns WCT submission, surfaces and cleanup.
        catch (Throwable failure) {
            synchronized (recentsOpenLock) {
                if (attempt != null && recentsOpenGeneration == attempt.session) invalidateRecentsOpen();
            }
            throw failure;
        }
        RecentsOpenSession.Attempt completed = null;
        synchronized (recentsOpenLock) {
            if (attempt != null && pendingRecentsOpenFinish == attempt
                    && recentsOpenGeneration == attempt.session && recentsOpenHooksActive) {
                attempt.completedToApp();
                completed = attempt;
                pendingRecentsOpenFinish = null;
                moduleLog(LOG_INFO, TAG, "Native Recents OPEN completed to its original task"
                        + ", task=" + attempt.session.taskId);
            }
        }
        if (completed != null) {
            RecentsOpenSession.Attempt receipt = completed;
            try {
                // Finish observers run inside transition consumption. Wait for Shell's
                // real idle boundary before starting the next navigation; never poll.
                Object transitions = readField(receipt.session.owner, "mTransitions");
                invokeAnyMethod(transitions, "runOnIdle", new Object[]{(Runnable) () -> {
                    try {
                        if (isRecentsOpenCompletionCurrentOnOwner(receipt)) notifyRecentsOpenCompleted(receipt);
                    } catch (Throwable failure) {
                        moduleLog(LOG_WARN, TAG, "Recents OPEN completion notification retired", failure);
                    }
                }});
            } catch (Throwable failure) {
                moduleLog(LOG_WARN, TAG, "Recents OPEN idle progress handoff unavailable", failure);
            }
        }
        return result;
    }

    private boolean finishesRecentsTaskToTop(Object transaction, Object token) throws Throwable {
        if (transaction == null) return false;
        Object operations = invokeAnyMethod(transaction, "getHierarchyOps", new Object[0]);
        if (!(operations instanceof java.util.List)) return false;
        Object binder = invokeAnyMethod(token, "asBinder", new Object[0]);
        int matches = 0;
        for (Object op : (java.util.List<?>) operations) {
            Object reorder = readStaticField(op.getClass(), "HIERARCHY_OP_TYPE_REORDER");
            if (!reorder.equals(invokeAnyMethod(op, "getType", new Object[0]))) continue;
            Object container = invokeAnyMethod(op, "getContainer", new Object[0]);
            boolean toTop = Boolean.TRUE.equals(invokeAnyMethod(op, "getToTop", new Object[0]));
            if (binder.equals(container)) {
                if (!toTop || ++matches != 1) return false;
            } else if (toTop) return false;
        }
        return matches == 1;
    }

    private boolean finishesRecentsRestoringOnlyHome(Object transaction, RecentsOpenSession session)
            throws Throwable {
        Object changes = invokeAnyMethod(transaction, "getChanges", new Object[0]);
        Object operations = invokeAnyMethod(transaction, "getHierarchyOps", new Object[0]);
        if (!(changes instanceof java.util.Map) || !(operations instanceof java.util.List)) return false;
        java.util.Map<?, ?> changeMap = (java.util.Map<?, ?>) changes;
        java.util.List<?> ops = (java.util.List<?>) operations;
        if (ops.size() != 1) return false;
        Object op = ops.get(0);
        Object restore = readStaticField(op.getClass(), "HIERARCHY_OP_TYPE_RESTORE_TRANSIENT_ORDER");
        if (!restore.equals(invokeAnyMethod(op, "getType", new Object[0]))) return false;
        Object container = invokeAnyMethod(op, "getContainer", new Object[0]);
        Object paused = readField(session.controller, "mPausingTasks");
        if (!(paused instanceof java.util.List) || ((java.util.List<?>) paused).size() != 1) return false;
        Object home = ((java.util.List<?>) paused).get(0);
        Object homeInfo = readField(home, "mTaskInfo");
        Object homeToken = readField(home, "mToken");
        if (homeInfo == null || homeToken == null
                || !Integer.valueOf(2).equals(invokeAnyMethod(homeInfo, "getActivityType", new Object[0]))
                || !Integer.valueOf(0).equals(readField(homeInfo, "displayId"))
                || !Integer.valueOf(1).equals(invokeAnyMethod(homeInfo, "getWindowingMode", new Object[0]))
                || !matchesClosingHomeChange(session.controller, home, homeInfo)
                || !invokeAnyMethod(homeToken, "asBinder", new Object[0]).equals(container)) return false;
        if (!changeMap.isEmpty() && (changeMap.size() != 1
                || !changeMap.containsKey(container)
                || !isExpectedRecentsHomeFinishChange(changeMap.get(container), transaction, homeToken))) return false;
        moduleLog(LOG_INFO, TAG, "Verified native OPEN finish restoring only its closing Home"
                + ", task=" + session.taskId + ", transition=" + shortObject(session.transition));
        return true;
    }

    private boolean isExpectedRecentsHomeFinishChange(Object change, Object transaction, Object homeToken)
            throws Throwable {
        if (change == null || !change.getClass().getName().equals(
                "android.window.WindowContainerTransaction$Change")) return false;
        if (emptyRecentsFinishChange == null || emptyRecentsFinishChange.getClass() != change.getClass()) {
            // Current framework's constructor creates only an inert default Change /
            // Configuration. Compare every instance field, including Xiaomi additions,
            // rather than trusting toString() or a subset of modification masks.
            java.lang.reflect.Constructor<?> constructor = change.getClass().getDeclaredConstructor();
            constructor.setAccessible(true);
            Object baseline = constructor.newInstance();
            java.util.List<java.lang.reflect.Field> fields = new java.util.ArrayList<>();
            for (Class<?> type = change.getClass(); type != Object.class; type = type.getSuperclass()) {
                for (java.lang.reflect.Field field : type.getDeclaredFields()) {
                    if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
                    field.setAccessible(true);
                    fields.add(field);
                }
            }
            recentsFinishChangeFields = fields.toArray(new java.lang.reflect.Field[0]);
            emptyRecentsFinishChange = baseline;
            doNotPipRecentsFinishChange = null;
        }
        if (matchesRecentsFinishChange(change, emptyRecentsFinishChange)) return true;
        if (doNotPipRecentsFinishChange == null) {
            // Shell cleanUpPausingOrClosingTask marks its closing leaf Home do-not-PiP.
            // Generate that exact shape through the current framework API, without
            // submitting the reference transaction or mutating the native transaction.
            java.lang.reflect.Constructor<?> constructor = transaction.getClass().getDeclaredConstructor();
            constructor.setAccessible(true);
            Object reference = constructor.newInstance();
            invokeAnyMethod(reference, "setDoNotPip", new Object[]{homeToken});
            Object changes = invokeAnyMethod(reference, "getChanges", new Object[0]);
            if (!(changes instanceof java.util.Map)) return false;
            Object expected = ((java.util.Map<?, ?>) changes).get(
                    invokeAnyMethod(homeToken, "asBinder", new Object[0]));
            if (expected == null || expected.getClass() != change.getClass()) return false;
            doNotPipRecentsFinishChange = expected;
        }
        if (matchesRecentsFinishChange(change, doNotPipRecentsFinishChange)) {
            moduleLog(LOG_INFO, TAG, "Verified native closing Home do-not-PiP finish change");
            return true;
        }
        moduleLog(LOG_INFO, TAG, "Rejected unexpected native Home finish change");
        return false;
    }

    private boolean matchesRecentsFinishChange(Object change, Object expected) throws IllegalAccessException {
        for (java.lang.reflect.Field field : recentsFinishChangeFields) {
            if (!java.util.Objects.deepEquals(field.get(change), field.get(expected))) return false;
        }
        return true;
    }

    protected Object observeRecentsOpenFinishStart(XposedInterface.Chain chain) throws Throwable {
        RecentsOpenSession.Attempt attempt = null;
        Object handler = chain.getArg(2);
        synchronized (recentsOpenLock) {
            RecentsOpenSession.Attempt current = recentsOpenAttempt;
            try {
                if (current != null && handler != null
                        && handler.getClass().getName().equals(
                        current.session.controller.getClass().getName() + "$PendingFinishTransitionHandler")
                        && readField(handler, "this$1") == current.session.controller
                        && readField(current.session.owner, "mTransitions") == chain.getThisObject()
                        && isRecentsOpenAttemptCurrentOnOwner(current)
                        && finishesRecentsTaskToTop(chain.getArg(1), current.session.taskToken)) attempt = current;
            } catch (Throwable failure) {
                moduleLog(LOG_WARN, TAG, "Native Recents pending finish proof unavailable", failure);
            }
        }
        Object result = chain.proceed();
        synchronized (recentsOpenLock) {
            if (attempt != null && attempt == recentsOpenAttempt
                    && attempt.session == recentsOpenGeneration && result instanceof IBinder
                    && attempt.bindPendingFinish(handler, result))
                moduleLog(LOG_INFO, TAG, "Bound native Recents OPEN finish transaction"
                        + ", task=" + attempt.session.taskId + ", token=" + shortObject(result));
        }
        return result;
    }

    protected Object observeRecentsOpenFinishConsumed(XposedInterface.Chain chain) throws Throwable {
        synchronized (recentsOpenLock) {
            RecentsOpenSession.Attempt current = recentsOpenAttempt;
            try {
                if (current != null && readField(chain.getThisObject(), "this$1") == current.session.controller
                        && (!current.matchesPendingFinish(chain.getThisObject(), chain.getArg(0))
                        || Boolean.TRUE.equals(chain.getArg(1)))) invalidateRecentsOpen();
            } catch (Throwable failure) {
                invalidateRecentsOpen();
                moduleLog(LOG_WARN, TAG, "Native Recents finish consumption identity unavailable", failure);
            }
        }
        return chain.proceed();
    }

    @Override
    protected boolean isRecentsOpenAttemptCurrentOnOwner(RecentsOpenSession.Attempt attempt)
            throws Throwable {
        return isRecentsOpenAttemptCurrentOnOwner(attempt, false);
    }

    private boolean isRecentsOpenAttemptCurrentOnOwner(RecentsOpenSession.Attempt attempt,
            boolean finishing) throws Throwable {
        synchronized (recentsOpenLock) {
            RecentsOpenSession session = attempt.session;
            invokeAnyMethod(readField(session.owner, "mExecutor"), "assertCurrentThread", new Object[0]);
            if (!recentsOpenHooksActive || session != recentsOpenSession || attempt != recentsOpenAttempt
                    || !attempt.isActive()
                    || !liveRecentsOpenOwner(session.owner, session.controller, session.transition, finishing)
                    || !Boolean.FALSE.equals(readField(session.controller, "mWillFinishToHome"))) return false;
            Object closing = readField(session.controller, "mClosingTasks");
            if (!(closing instanceof java.util.List) || !((java.util.List<?>) closing).isEmpty()) return false;
            Object task = singleBarReuseTask(session.controller);
            if (task == null) return false;
            int id = ((android.app.ActivityManager.RunningTaskInfo) readField(task, "mTaskInfo")).taskId;
            return attempt.matches(session.owner, session.controller,
                    readField(session.controller, "mTransition"), readField(session.controller, "mInfo"), task, id);
        }
    }

    private void observeRecentsOpenDirection(Object controller, boolean toHome) {
        synchronized (recentsOpenLock) {
            RecentsOpenSession session = recentsOpenSession;
            if (!recentsOpenHooksActive || session == null || session.controller != controller) return;
            try {
                invokeAnyMethod(readField(session.owner, "mExecutor"), "assertCurrentThread", new Object[0]);
                if (toHome) {
                    session.toHome();
                    recentsOpenAttempt = null;
                    return;
                }
                RecentsOpenSession.Attempt attempt = session.toApp();
                recentsOpenAttempt = attempt;
                if (attempt == null || !isRecentsOpenAttemptCurrentOnOwner(attempt)) {
                    invalidateRecentsOpen();
                    return;
                }
                moduleLog(LOG_INFO, TAG, "Native Recents task-reuse OPEN active"
                        + ", task=" + session.taskId + ", transition=" + shortObject(session.transition));
            } catch (Throwable failure) {
                invalidateRecentsOpen();
                moduleLog(LOG_WARN, TAG, "Native Recents OPEN direction unavailable", failure);
            }
        }
    }

    protected Object observeRecentsBarTargets(XposedInterface.Chain chain) throws Throwable {
        Object controller = chain.getThisObject();
        if (recentsOpenSession != null && recentsOpenSession.controller == controller)
            invalidateRecentsOpen();
        RecentsBarReuseSession session;
        Object update = null;
        RecentsBarNativeTarget captured = null;
        synchronized (barReuseLock) {
            session = barReuseSession;
            if (session != null && session.controller == controller && barFeatureState.enabled()) {
                update = session.beginTargetUpdate();
                try {
                    invokeAnyMethod(readField(session.owner, "mExecutor"), "assertCurrentThread", new Object[0]);
                    if (liveBarReuseOwner(session.owner, controller, readField(controller, "mTransition")))
                        captured = RecentsBarNativeTarget.capture(session, controller,
                                (java.util.List<?>) chain.getArg(0), (TransitionInfo) chain.getArg(1));
                } catch (Throwable failure) {
                    moduleLog(LOG_WARN, TAG, "RecentsBarTarget: native target capture unavailable", failure);
                }
            }
        }
        Object result;
        try { result = chain.proceed(); } // Always preserve native target delivery.
        catch (Throwable failure) {
            synchronized (barReuseLock) { if (session != null && update != null) session.acceptTarget(update, null); }
            throw failure;
        }
        synchronized (barReuseLock) {
            if (session == null || session != barReuseSession || update == null) return result;
            try {
                if (captured != null && !captured.matchesCurrent(controller, (TransitionInfo) session.info)) captured = null;
                RecentsBarReuseSession.Target target = captured == null ? null
                        : new RecentsBarReuseSession.Target(captured.task, captured.taskId, captured);
                boolean accepted = session.acceptTarget(update, target);
                moduleLog(LOG_INFO, TAG, "RecentsBarTarget: accepted=" + accepted
                        + " task=" + (captured == null ? -1 : captured.taskId)
                        + " merge=" + ((TransitionInfo) chain.getArg(1)).getDebugId()
                        + (accepted ? "" : " " + describeBarReuseTasks(controller)));
                // The direction-to-app callback can precede onTasksAppeared.
                // Complete that same native decision after its target is known.
                if (accepted && session.awaitingAppShow()) deliverAcceptedAppBar(session, "targets");
            } catch (Throwable failure) {
                session.acceptTarget(update, null);
                moduleLog(LOG_WARN, TAG, "RecentsBarTarget: native target publication unavailable", failure);
            }
        }
        return result;
    }

    private boolean deliverAcceptedAppBar(RecentsBarReuseSession session, String source) throws Throwable {
        RecentsBarReuseSession.Target target = session.currentTarget();
        if (target == null || !(target.proof instanceof RecentsBarNativeTarget) || session.callback == null) return false;
        Object controller = session.controller;
        Object token = readField(controller, "mTransition");
        RecentsBarNativeTarget proof = (RecentsBarNativeTarget) target.proof;
        boolean live = barFeatureState.enabled() && liveBarReuseOwner(session.owner, controller, token)
                && readField(controller, "mInfo") == session.info
                && proof.matchesCurrent(controller, (TransitionInfo) session.info)
                && barCallbackBinder(readField(session.callback, "val$original")) == session.endpoint
                && ((IBinder) session.endpoint).isBinderAlive();
        if (!session.consumeTarget(controller, token, readField(controller, "mInfo"), session.callback,
                target, proof.task, !Boolean.FALSE.equals(readField(controller, "mWillFinishToHome")), live)) return false;
        invokeAnyMethod(session.original, "onGestureStateUpdated", new Object[]{7});
        moduleLog(LOG_INFO, TAG, "RecentsBarTarget: App show task=" + target.taskId
                + " merge=" + proof.mergeId + " source=" + source);
        return true;
    }

    private void bindStartedRecentsBarReuse(Object owner, Object token, Object suppliedInfo) {
        synchronized (barReuseLock) {
            if (barReuseSession != null) barReuseSession.close();
            barReuseSession = null;
            try {
                invokeAnyMethod(readField(owner, "mExecutor"), "assertCurrentThread", new Object[0]);
                Object controller = invokeAnyMethod(owner, "findController", new Object[]{token});
                if (controller == null
                        || suppliedInfo != readField(controller, "mInfo")) return;
                Object task = singleBarReuseTask(controller);
                boolean live = liveBarReuseController(owner, controller, token);
                if (task == null || !live) {
                    moduleLog(LOG_INFO, TAG, "RecentsBarReuse: start ineligible task=" + (task != null)
                            + " live=" + live + " token=" + token + " " + describeBarReuseTasks(controller));
                    return;
                }
                int taskId = ((android.app.ActivityManager.RunningTaskInfo) readField(task, "mTaskInfo")).taskId;
                barReuseSession = new RecentsBarReuseSession(owner, controller, token, suppliedInfo,
                        null, null, null, task, taskId);
                moduleLog(LOG_INFO, TAG, "RecentsBarReuse: awaiting authenticated start task=" + taskId + " token=" + token);
            } catch (Throwable failure) {
                moduleLog(LOG_WARN, TAG, "RecentsBarReuse: start binding unavailable", failure);
            }
        }
    }

    private static IBinder barCallbackBinder(Object callback) {
        return callback instanceof android.os.IInterface
                ? ((android.os.IInterface) callback).asBinder() : null;
    }

    private String describeBarReuseTasks(Object controller) throws Throwable {
        StringBuilder result = new StringBuilder("type=").append(readField(controller, "mRemoteToRecentType"));
        for (String name : new String[]{"mPausingTasks", "mOpeningTasks", "mClosingTasks"}) {
            result.append(' ').append(name).append('=');
            Object tasks = readField(controller, name);
            if (!(tasks instanceof java.util.List)) { result.append("unavailable"); continue; }
            result.append('[');
            for (Object task : (java.util.List<?>) tasks) {
                Object info = readField(task, "mTaskInfo");
                result.append("{id=").append(info == null ? "null" : readField(info, "taskId"))
                        .append(",type=").append(info == null ? "null" : invokeAnyMethod(info, "getActivityType", new Object[0]))
                        .append(",mode=").append(info == null ? "null" : invokeAnyMethod(info, "getWindowingMode", new Object[0]))
                        .append(",reopened=").append(readField(task, "mOpenFromPausingOrClosing"))
                        .append(",translucent=").append(readField(task, "mIsTranslucent"))
                        .append(",leash=").append(readField(task, "mLeash")).append('}');
            }
            result.append(']');
        }
        return result.toString();
    }

    protected Object handoffRecentsBarReuse(XposedInterface.Chain chain) throws Throwable {
        Object result = chain.proceed(); // Xiaomi owns the direction; never override it.
        observeRecentsOpenDirection(chain.getThisObject(), (Boolean) chain.getArg(0));
        synchronized (barReuseLock) {
            RecentsBarReuseSession session = barReuseSession;
            try {
                Object controller = chain.getThisObject();
                boolean toHome = (Boolean) chain.getArg(0);
                boolean delivered = false;
                if (session != null && session.controller == controller && session.callback != null) {
                    invokeAnyMethod(readField(session.owner, "mExecutor"), "assertCurrentThread", new Object[0]);
                    if (!toHome && session.awaitingAppShow()) delivered = deliverAcceptedAppBar(session, "direction");
                    if (session.currentTarget() == null) {
                        Object token = readField(controller, "mTransition");
                        boolean live = liveBarReuseController(session.owner, controller, token)
                                && ((android.app.ActivityManager.RunningTaskInfo) readField(session.task, "mTaskInfo")).taskId == session.taskId
                                // WMS allocates a callback per ready transition; Shell's
                                // latest publication does not retire the delivered one.
                                // Use exactly the endpoint that carried this Home target.
                                && barCallbackBinder(readField(session.callback, "val$original")) == session.endpoint
                                && ((IBinder) session.endpoint).isBinderAlive()
                                && Boolean.valueOf(toHome).equals(readField(controller, "mWillFinishToHome"));
                        Object currentTask = singleBarReuseTask(controller);
                        if (!delivered && session.consume(controller, token, readField(controller, "mInfo"), session.callback,
                                currentTask, toHome, live)) {
                            // A16 animToFullScreen -> noStartActivityAppOpen (7). Preserve
                            // WMS's existing Folme velocity, task parenting and cleanup.
                            invokeAnyMethod(session.original, "onGestureStateUpdated", new Object[]{7});
                            delivered = true;
                        } else if (!delivered && !toHome) {
                            moduleLog(LOG_INFO, TAG, "RecentsBarReuse: rejected live=" + live
                                    + " taskPresent=" + (currentTask != null)
                                    + " sameTask=" + (currentTask == session.task)
                                    + " sameTransition=" + (token == session.transition)
                                    + " sameInfo=" + (readField(controller, "mInfo") == session.info)
                                    + ((!live || currentTask != session.task) ? " " + describeBarReuseTasks(controller) : ""));
                        }
                    }
                }
                moduleLog(LOG_INFO, TAG, "RecentsBarReuse: direction toHome=" + toHome
                        + " appOpenDelivered=" + delivered + " bound=" + (session != null)
                        + " task=" + (session == null ? -1 : session.taskId));
            } catch (Throwable failure) {
                if (session != null) session.close();
                moduleLog(LOG_WARN, TAG, "RecentsBarReuse: preserving native direction without handoff", failure);
            }
        }
        return result;
    }

    private void hookShadeBarEvents(ClassLoader loader, Set<String> present) throws ReflectiveOperationException {
        Class<?> queue = Class.forName("com.android.systemui.statusbar.CommandQueue", false, loader);
        Method[] methods = {
                appTransitionEntry(queue, "appTransitionPending", new Class<?>[]{int.class, boolean.class}, new Class<?>[]{int.class}),
                appTransitionEntry(queue, "appTransitionStarting", new Class<?>[]{int.class, long.class, long.class, boolean.class},
                        new Class<?>[]{int.class, long.class, long.class}),
                queue.getDeclaredMethod("appTransitionCancelled", int.class),
                queue.getDeclaredMethod("appTransitionFinished", int.class)};
        String[] ids = {BAR_TASK_PENDING, BAR_TASK_STARTING, BAR_TASK_CANCELLED, BAR_TASK_FINISHED};
        for (Method method : methods) if (method.getReturnType() != void.class)
            throw new NoSuchMethodException("Unexpected CommandQueue app-transition contract");
        for (int i = 0; i < methods.length; i++) if (!present.contains(ids[i])) {
            methods[i].setAccessible(true);
            recordHookHandle(hook(methods[i]).setId(ids[i]).intercept(this::observeShadeBarEvent));
        }
    }

    private void hookHomeBarReady(ClassLoader loader, Set<String> present) throws Throwable {
        Class<?> binder = Class.forName(
                "com.android.systemui.statusbar.pipeline.shared.ui.binder.HomeStatusBarViewBinderImpl",
                false, loader);
        Method bind = binder.getDeclaredMethod("bind",
                Class.forName("com.android.systemui.statusbar.phone.PhoneStatusBarView", false, loader),
                Class.forName("com.android.systemui.statusbar.pipeline.shared.ui.viewmodel.HomeStatusBarViewModel", false, loader),
                Class.forName("kotlin.jvm.functions.Function1", false, loader),
                Class.forName("kotlin.jvm.functions.Function1", false, loader));
        if (bind.getReturnType() != void.class || !binder.getDeclaredField("mInjector").getType()
                .getName().equals("com.android.systemui.statusbar.pipeline.shared.ui.binder.HomeStatusBarViewBinderInjector"))
            throw new NoSuchMethodException("Native Home status-bar bind contract changed");
        systemUiDexResolutionInFlight.incrementAndGet();
        try {
            ensureDexKitLibraryLoaded();
            try (DexKitBridge dex = DexKitBridge.create(resolveBarFollowDexContainer(binder))) {
                java.util.List<MethodData> matches = dex.findMethod(FindMethod.create().matcher(
                        MethodMatcher.create().declaredClass(binder.getName()).name("bind").paramCount(4)));
                if (matches.size() != 1 || !matches.get(0).getMethodInstance(loader).equals(bind))
                    throw new NoSuchMethodException("Native Home bind DEX identity changed");
                java.util.List<MethodData> callers = matches.get(0).getCallers();
                if (callers.isEmpty()) throw new NoSuchMethodException("Native Home bind caller missing");
                for (MethodData caller : callers) {
                    if (!deoptimize(caller.getMethodInstance(loader)))
                        throw new IllegalStateException("Native Home bind caller could not be deoptimized");
                }
            }
        } finally { systemUiDexResolutionInFlight.decrementAndGet(); }
        if (!present.contains(BAR_HOME_BOUND)) recordHookHandle(hook(bind).setId(BAR_HOME_BOUND)
                .intercept(this::homeBarBound));
        moduleLog(LOG_INFO, TAG, "Native Home status-bar bind readiness hook ready");
    }

    private void hookAppBarViewRole(ClassLoader loader, Set<String> present) throws ReflectiveOperationException {
        Class<?> light = Class.forName("com.android.systemui.statusbar.phone.LightBarControllerImpl", false, loader);
        Class<?> regionArray = java.lang.reflect.Array.newInstance(Class.forName(
                "com.android.internal.view.AppearanceRegion", false, loader), 0).getClass();
        Method update = light.getDeclaredMethod("updateStatus", regionArray);
        if (update.getReturnType() != void.class) throw new NoSuchMethodException("App appearance contract changed");
        for (Method caller : light.getDeclaredMethods()) {
            if (!java.lang.reflect.Modifier.isNative(caller.getModifiers()) && !deoptimize(caller))
                throw new IllegalStateException("App appearance caller could not be deoptimized");
        }
        if (!present.contains(BAR_APP_APPEARANCE)) recordHookHandle(hook(update).setId(BAR_APP_APPEARANCE)
                .intercept(this::applyAppWindowAppearance));
        Method animateTint = light.getDeclaredMethod("animateChange");
        if (animateTint.getReturnType() != boolean.class)
            throw new NoSuchMethodException("App tint animation contract changed");
        if (!present.contains(BAR_APP_TINT_ANIMATION)) recordHookHandle(hook(animateTint)
                .setId(BAR_APP_TINT_ANIMATION).intercept(this::gateAppBarTintAnimation));
        Class<?> view = Class.forName("com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView", false, loader);
        Class<?> dep = Class.forName("com.android.systemui.statusbar.phone.MiuiPhoneStatusBarViewDep", false, loader);
        Method init = view.getDeclaredMethod("initDependence", dep, String.class);
        Method drip = view.getDeclaredMethod("setDripEnd", boolean.class);
        if (init.getReturnType() != void.class || drip.getReturnType() != void.class)
            throw new NoSuchMethodException("App View role contract changed");
        if (!present.contains(BAR_VIEW_INIT))
            recordHookHandle(hook(init).setId(BAR_VIEW_INIT).intercept(this::initializeAppBarView));
        if (!present.contains(BAR_VIEW_DRIP))
            recordHookHandle(hook(drip).setId(BAR_VIEW_DRIP).intercept(this::updateAppBarDrip));
        Class<?> component = Class.forName("com.android.systemui.dagger.DaggerReferenceGlobalRootComponent$HomeStatusBarComponentImpl", false, loader);
        Method factory = component.getDeclaredMethod("phoneStatusBarViewControllerFactory");
        if (!factory.getReturnType().getName().equals("com.android.systemui.statusbar.phone.PhoneStatusBarViewController$Factory"))
            throw new NoSuchMethodException("Native App controller factory changed");
        if (!present.contains(BAR_VIEW_FACTORY))
            recordHookHandle(hook(factory).setId(BAR_VIEW_FACTORY).intercept(this::configureAppBarViewFactory));
        Method darkState = Class.forName("com.android.systemui.statusbar.phone.data.repository.DarkIconRepositoryImpl", false, loader)
                .getDeclaredMethod("darkState", int.class);
        if (!darkState.getReturnType().getName().equals("kotlinx.coroutines.flow.StateFlow"))
            throw new NoSuchMethodException("Native dark-state contract changed");
        if (!present.contains(BAR_DARK_STATE))
            recordHookHandle(hook(darkState).setId(BAR_DARK_STATE).intercept(this::provideAppBarDarkState));
        Class<?> injector = Class.forName("com.android.systemui.statusbar.pipeline.shared.ui.binder.HomeStatusBarViewBinderInjector", false, loader);
        Method[] animationMethods = {
                injector.getDeclaredMethod("animateShow", android.view.View.class, boolean.class),
                injector.getDeclaredMethod("animateHide", android.view.View.class, boolean.class),
                injector.getDeclaredMethod("showClock", boolean.class),
                injector.getDeclaredMethod("hideClock", boolean.class)};
        String[] animationIds = {BAR_SHOW_ROLE, BAR_HIDE_ROLE, BAR_CLOCK_SHOW_ROLE, BAR_CLOCK_HIDE_ROLE};
        for (int i = 0; i < animationMethods.length; i++) {
            if (animationMethods[i].getReturnType() != void.class)
                throw new NoSuchMethodException("Native App animation role contract changed");
            if (!present.contains(animationIds[i])) recordHookHandle(hook(animationMethods[i])
                    .setId(animationIds[i]).intercept(this::gateAppBarAnimation));
        }
        Class<?> privacy = Class.forName("com.android.systemui.statusbar.phone.MiuiHomePrivacyController", false, loader);
        Method privacyUpdate;
        String privacyId;
        try {
            privacyUpdate = privacy.getDeclaredMethod("updateStatusBarHomePrivacyVisibility");
            privacyId = BAR_PRIVACY_ROLE;
        } catch (NoSuchMethodException inlined) {
            privacyUpdate = privacy.getDeclaredMethod("onPromptInfoChanged",
                    Class.forName("com.android.systemui.plugins.miui.statusbar.MiuiPrivacyController$MiuiPromptInfo", false, loader),
                    Class.forName("com.android.systemui.plugins.miui.statusbar.MiuiPrivacyController$AndroidPromptInfo", false, loader));
            privacy.getDeclaredField("mShowCapsuleAnim");
            privacy.getDeclaredField("mControlPanelExpand");
            privacyId = BAR_PRIVACY_PROMPT;
        }
        if (privacyUpdate.getReturnType() != void.class)
            throw new NoSuchMethodException("Native App privacy role contract changed");
        if (!present.contains(privacyId)) recordHookHandle(hook(privacyUpdate)
                .setId(privacyId).intercept(BAR_PRIVACY_PROMPT.equals(privacyId)
                        ? this::gateAppBarPrivacyPrompt : this::gateAppBarPrivacy));
        Class<?> notificationInject = Class.forName("com.android.systemui.statusbar.phone.NotificationIconContainerInject", false, loader);
        Method notificationAllowed = notificationInject.getDeclaredMethod("animationAllowed");
        if (notificationAllowed.getReturnType() != boolean.class)
            throw new NoSuchMethodException("Native notification eligibility contract changed");
        if (!present.contains(BAR_NOTIFICATION_ROLE)) recordHookHandle(hook(notificationAllowed)
                .setId(BAR_NOTIFICATION_ROLE).intercept(this::gateBarNotificationAnimation));
        Class<?> islandListener = Class.forName(injector.getName() + "$islandListener$1", false, loader);
        Method islandChanged = islandListener.getDeclaredMethod("onIslandStatusChanged", boolean.class, boolean.class, boolean.class);
        if (islandChanged.getReturnType() != void.class || islandListener.getDeclaredField("this$0").getType() != injector)
            throw new NoSuchMethodException("Native island role contract changed");
        if (!present.contains(BAR_ISLAND_ROLE)) recordHookHandle(hook(islandChanged)
                .setId(BAR_ISLAND_ROLE).intercept(this::gateBarIslandAnimation));
        // These exact native callers contain the unguarded shared-target writes
        // when ART inlines initDependence/setDripEnd. Fail preparation if callers
        // cannot be deoptimized; do not discover a missed hook by changing Home.
        boolean ready = deoptimize(view.getDeclaredMethod("setDependence", dep));
        ready &= deoptimize(Class.forName("com.android.systemui.statusbar.phone.NotificationIconContainer$IconState", false, loader)
                .getDeclaredMethod("applyToView", android.view.View.class));
        ready &= deoptimize(Class.forName("com.android.systemui.statusbar.phone.NotificationIconContainer", false, loader)
                .getDeclaredMethod("onViewRemoved", android.view.View.class));
        for (Method caller : injector.getDeclaredMethods())
            if (!java.lang.reflect.Modifier.isAbstract(caller.getModifiers())
                    && !java.lang.reflect.Modifier.isNative(caller.getModifiers())) ready &= deoptimize(caller);
        for (Method caller : privacy.getDeclaredMethods())
            if (!java.lang.reflect.Modifier.isAbstract(caller.getModifiers())
                    && !java.lang.reflect.Modifier.isNative(caller.getModifiers())) ready &= deoptimize(caller);
        ready &= deoptimize(view.getDeclaredMethod("onAttachedToWindow"));
        ready &= deoptimize(view.getDeclaredMethod("updateCutoutLocation"));
        ready &= deoptimize(view.getDeclaredMethod("setDarkIconManager",
                Class.forName("com.android.systemui.statusbar.phone.ui.DarkIconManager", false, loader)));
        ready &= deoptimize(Class.forName(component.getName() + "$SwitchingProvider", false, loader)
                .getDeclaredMethod("get"));
        ready &= deoptimize(Class.forName("com.android.systemui.statusbar.notification.icon.ui.viewbinder."
                + "NotificationIconContainerViewBinder$bind$2$1", false, loader).getDeclaredMethod("invokeSuspend", Object.class));
        for (java.lang.reflect.Constructor<?> constructor : Class.forName(
                "com.android.systemui.statusbar.pipeline.shared.ui.viewmodel.HomeStatusBarViewModelImpl", false, loader)
                .getDeclaredConstructors()) ready &= deoptimize(constructor);
        appViewRoleHooksReady = ready;
    }

    protected Object applyAppWindowAppearance(XposedInterface.Chain chain) throws Throwable {
        // Drain a color queued while the preference was off before the native
        // update computes and immediately applies the latest color. Keep every
        // native region, night-mode and sampling decision in updateStatus.
        if (AppBarModeBinding.ownsLightController(chain.getThisObject())
                && barFeatureState.enabled()) {
            try { AppBarModeBinding.settlePendingTintBeforeUpdate(chain.getThisObject()); }
            catch (ReflectiveOperationException failure) {
                moduleLog(LOG_WARN, TAG, "App pending tint reconciliation unavailable", failure);
            }
        }
        Object result = chain.proceed();
        try {
            String diagnostic = AppBarModeBinding.nativeAppearanceDiagnostic(chain.getThisObject());
            if (diagnostic != null) moduleLog(LOG_INFO, TAG, "BarAppearance native App update " + diagnostic);
        } catch (ReflectiveOperationException | RuntimeException failure) {
            moduleLog(LOG_WARN, TAG, "App native appearance diagnostic unavailable", failure);
        }
        return result;
    }

    protected Object gateAppBarTintAnimation(XposedInterface.Chain chain) throws Throwable {
        // Native animateChange consumes its transient-mode flag. Preserve that
        // bookkeeping even when this private App owner uses immediate tint.
        Object original = chain.proceed();
        return AppBarModeBinding.ownsLightController(chain.getThisObject())
                && barFeatureState.enabled() ? false : original;
    }

    private boolean readBarFollowEnabled() {
        try {
            SharedPreferences preferences = barFollowPreferences;
            if (preferences == null) return false;
            boolean enabled = preferences.getBoolean(
                    StatusBarFollowPreferences.KEY_RESTORE_STATUS_BAR_FOLLOW,
                    StatusBarFollowPreferences.DEFAULT_RESTORE_STATUS_BAR_FOLLOW);
            barFollowPreferenceFailureLogged = false;
            return enabled && StatusBarFollowGate.enabled();
        } catch (Throwable failure) {
            if (!barFollowPreferenceFailureLogged) {
                barFollowPreferenceFailureLogged = true;
                moduleLog(LOG_WARN, TAG, "Status-bar following preference unavailable; preserving native bars", failure);
            }
            return false;
        }
    }

    private void ensureBarFollowPreferenceListener(ClassLoader loader) {
        try {
            if (barFollowPreferences == null) {
                SharedPreferences preferences = getRemotePreferences(StatusBarFollowPreferences.GROUP);
                preferences.registerOnSharedPreferenceChangeListener(barFollowPreferenceListener);
                barFollowPreferences = preferences;
            }
            barFollowClassLoader = loader;
            recentsBars.onIdle(() -> {
                if (barFollowRestartPending) barPreferenceHandler.post(this::reconcileBarFollowPreference);
            });
            barPreferenceHandler.post(this::reconcileBarFollowPreference);
        } catch (Throwable failure) {
            moduleLog(LOG_WARN, TAG, "Status-bar following preference listener unavailable", failure);
        }
    }

    private void reconcileBarFollowPreference() {
        if (barFollowPreferences == null || barFollowClassLoader == null) return;
        boolean previous = barFeatureState.enabled();
        barFeatureState.setEnabled(readBarFollowEnabled());
        if (!barFeatureState.enabled()) {
            barFollowRestartPending = false;
            recentsBars.cancelForPreference();
            synchronized (barCallbackLock) {
                if (barCallbackSession != null) barCallbackSession.close();
                barCallbackSession = null;
            }
            synchronized (barReuseLock) {
                if (barReuseSession != null) barReuseSession.close();
                barReuseSession = null;
            }
            if (barFolmeProbe != null) finishBarFolmeProbe("preference disabled", null);
            if (pendingAppContentCleanup != null) closeAppWindowProbe(pendingAppContentCleanup);
            if (previous) moduleLog(LOG_INFO, TAG, "Status-bar following disabled; retiring exact App owner");
            return;
        }
        if (pendingAppContentCleanup != null) {
            barFollowRestartPending = appBarRetiring;
            return;
        }
        if (barFolmeProbePending || !recentsBars.idle()) {
            barFollowRestartPending = true;
            return;
        }
        barFollowRestartPending = false;
        barFolmeProbePending = true;
        long generation = barFeatureState.generation();
        ClassLoader loader = barFollowClassLoader;
        barPreferenceHandler.post(() -> runBarFolmeProbe(loader, generation));
    }

    protected Object homeBarBound(XposedInterface.Chain chain) throws Throwable {
        Object result = chain.proceed();
        // Native bind publishes mInjector and initializes its children before returning.
        // Queue recovery after that call stack, never from an App/successor binder.
        if (!barFeatureState.enabled() || !barPreferenceHandler.getLooper().isCurrentThread()) return result;
        ClassLoader loader = barFollowClassLoader;
        if (loader == null) return result;
        try {
            Object binder = chain.getThisObject();
            Object injector = readField(binder, "mInjector");
            Object view = chain.getArg(0);
            if (AppBarContentPreparation.isCurrentHomeBinding(loader, binder, injector, view)) {
                barStartupRecovery.homeReady(() -> {
                    if (barFollowClassLoader != loader) return;
                    try {
                        if (!AppBarContentPreparation.isCurrentHomeBinding(loader, binder, injector, view)) return;
                        moduleLog(LOG_INFO, TAG, "Home status bar bound; reconciling status-bar following");
                        reconcileBarFollowPreference();
                    } catch (Throwable failure) {
                        moduleLog(LOG_WARN, TAG, "Home status-bar readiness unavailable; preserving native bars", failure);
                    }
                });
            }
        } catch (Throwable failure) {
            moduleLog(LOG_WARN, TAG, "Home status-bar readiness unavailable; preserving native bars", failure);
        }
        return result;
    }

    protected Object initializeAppBarView(XposedInterface.Chain chain) throws Throwable {
        AppBarContentPreparation owner = pendingAppContentCleanup;
        if (owner == null || !owner.ownsView(chain.getThisObject())) return chain.proceed();
        owner.initializeViewDependency(chain.getArg(0));
        return null;
    }

    protected Object updateAppBarDrip(XposedInterface.Chain chain) throws Throwable {
        AppBarContentPreparation owner = pendingAppContentCleanup;
        if (owner == null || !owner.ownsView(chain.getThisObject())) return chain.proceed();
        owner.updateDrip((Boolean) chain.getArg(0));
        return null;
    }

    protected Object configureAppBarViewFactory(XposedInterface.Chain chain) throws Throwable {
        Object factory = chain.proceed();
        AppBarContentPreparation owner = pendingAppContentCleanup;
        if (owner != null && owner.ownsComponent(chain.getThisObject())) owner.configureViewFactory(factory);
        return factory;
    }

    protected Object provideAppBarDarkState(XposedInterface.Chain chain) throws Throwable {
        AppBarContentPreparation owner = pendingAppContentCleanup;
        Object state = owner == null ? null : owner.darkStateFor(chain.getThisObject(), (Integer) chain.getArg(0));
        return state != null ? state : chain.proceed();
    }

    protected Object gateAppBarAnimation(XposedInterface.Chain chain) throws Throwable {
        AppBarContentPreparation owner = pendingAppContentCleanup;
        Boolean allowed = owner == null ? null : owner.injectorAnimationAllowed(chain.getThisObject());
        if (!Boolean.FALSE.equals(allowed)) return chain.proceed();
        Object[] args = chain.getArgs().toArray();
        args[args.length - 1] = false;
        return chain.proceed(args);
    }

    protected Object gateAppBarPrivacy(XposedInterface.Chain chain) throws Throwable {
        AppBarContentPreparation owner = pendingAppContentCleanup;
        return owner != null && owner.handleAppPrivacy(chain.getThisObject()) ? null : chain.proceed();
    }

    protected Object gateAppBarPrivacyPrompt(XposedInterface.Chain chain) throws Throwable {
        AppBarContentPreparation owner = pendingAppContentCleanup;
        Boolean allowed = owner == null ? null : owner.privacyAnimationAllowed(chain.getThisObject());
        if (!Boolean.FALSE.equals(allowed)) return chain.proceed();
        // New SystemUI inlines the visibility decision into the prompt callback.
        // Use its own immediate-animation branch while preserving all prompt parsing,
        // capsule/dot selection, battery-island handling and terminal bookkeeping.
        Object controller = chain.getThisObject();
        java.lang.reflect.Field expanded = controller.getClass().getDeclaredField("mControlPanelExpand");
        expanded.setAccessible(true);
        boolean original = expanded.getBoolean(controller);
        if (original) return chain.proceed();
        expanded.setBoolean(controller, true);
        try { return chain.proceed(); }
        finally { expanded.setBoolean(controller, false); }
    }

    protected Object gateBarNotificationAnimation(XposedInterface.Chain chain) throws Throwable {
        Object original = chain.proceed();
        if (!Boolean.TRUE.equals(original)) return original;
        AppBarContentPreparation owner = pendingAppContentCleanup;
        Boolean allowed = owner == null ? null : owner.notificationAnimationAllowed(chain.getThisObject());
        return allowed == null ? original : allowed;
    }

    protected Object gateBarIslandAnimation(XposedInterface.Chain chain) throws Throwable {
        AppBarContentPreparation owner = pendingAppContentCleanup;
        if (owner == null || !Boolean.TRUE.equals(chain.getArg(1))) return chain.proceed();
        Object injector = AppBarContentPreparation.read(chain.getThisObject(), "this$0");
        if (!Boolean.FALSE.equals(owner.injectorAnimationAllowed(injector))) return chain.proceed();
        Object[] args = chain.getArgs().toArray();
        args[1] = false;
        return chain.proceed(args);
    }

    private static Method appTransitionEntry(Class<?> queue, String name, Class<?>[] implementation,
                                              Class<?>[] binderEntry) throws NoSuchMethodException {
        try { return queue.getDeclaredMethod(name, implementation); }
        catch (NoSuchMethodException absent) {
            // R8 may inline the boolean overload into the standard IStatusBar
            // entry. Hook one boundary only; otherwise delegated calls double emit.
            return queue.getDeclaredMethod(name, binderEntry);
        }
    }

    protected Object observeShadeBarEvent(XposedInterface.Chain chain) throws Throwable {
        AppBarContentPreparation content = pendingAppContentCleanup;
        String event = chain.getExecutable().getName();
        if (content != null) new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            if (pendingAppContentCleanup != content) return;
            try { moduleLog(LOG_INFO, TAG, "BarAppearance transition=" + event + " " + content.appearanceDiagnostic()); }
            catch (Throwable failure) { moduleLog(LOG_WARN, TAG, "BarAppearance diagnostic failed", failure); }
        });
        XiaomiShadeBarEvents events = shadeBarEvents;
        if (events != null) {
            try { events.event(chain.getThisObject(), (Integer) chain.getArg(0)); }
            catch (RuntimeException failure) { moduleLog(LOG_WARN, TAG, "App bar task event unavailable", failure); }
        }
        // A16 notifies before the original CommandQueue message is enqueued.
        return chain.proceed();
    }

    private void hookBarFolmeProbe(ClassLoader loader, Set<String> present) {
        try {
            Class<?> listener = Class.forName("miuix.animation.listener.TransitionListener", false, loader);
            Method update = listener.getMethod("onUpdate", Object.class, java.util.Collection.class);
            Method complete = listener.getMethod("onComplete", Object.class);
            Method cancel = listener.getMethod("onCancel", Object.class);
            if (!present.contains(BAR_FOLME_UPDATE)) recordHookHandle(hook(update).setId(BAR_FOLME_UPDATE)
                    .intercept(this::observeBarFolmeUpdate));
            if (!present.contains(BAR_FOLME_COMPLETE)) recordHookHandle(hook(complete).setId(BAR_FOLME_COMPLETE)
                    .intercept(this::observeBarFolmeComplete));
            if (!present.contains(BAR_FOLME_CANCEL)) recordHookHandle(hook(cancel).setId(BAR_FOLME_CANCEL)
                    .intercept(this::observeBarFolmeCancel));
            recentsBars.configureFolme(loader);
        } catch (Throwable failure) {
            moduleLog(LOG_WARN, TAG, "Native bar Folme unavailable; parity engine not activated", failure);
        }
    }

    // Inspect without class initialization or constructing another system window.
    // A16's content graph must exist before an A17 window can be safely attached.
    private void inspectNativeBarContentContracts(ClassLoader loader) {
        String[] names = {
                "com.miui.systemui.statusbar.StatusBarToken",
                "com.miui.systemui.statusbar.data.repository.StatusBarTokenRepository",
                "com.miui.systemui.statusbar.core.MultiStatusBarStarter",
                "com.miui.systemui.statusbar.core.MultiStatusBarInitializerStoreImpl",
                "com.miui.systemui.statusbar.core.MultiStatusBarOrchestratorStore",
                "com.miui.systemui.statusbar.window.MultiStatusBarWindowControllerStoreImpl",
                "com.miui.systemui.statusbar.data.repository.MultiStatusBarDarkIconDispatcherStoreImpl",
                "com.miui.systemui.statusbar.data.repository.MultiStatusBarModeRepositoryStoreImpl",
                "com.miui.systemui.statusbar.data.repository.MultiStatusBarLightBarControllerStoreImpl",
                "com.android.systemui.statusbar.core.StatusBarInitializerImpl",
                "com.android.systemui.statusbar.core.StatusBarOrchestrator",
                "com.android.systemui.statusbar.window.StatusBarWindowControllerImpl",
                "com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment",
                "com.miui.systemui.statusbar.phone.MiuiCollapsedStatusBarFragmentInjector"
        };
        for (String name : names) {
            try {
                Class<?> type = Class.forName(name, false, loader);
                java.util.ArrayList<String> fields = new java.util.ArrayList<>();
                for (java.lang.reflect.Field field : type.getDeclaredFields()) {
                    if (!java.lang.reflect.Modifier.isStatic(field.getModifiers()))
                        fields.add(field.getName() + ":" + field.getType().getName());
                }
                moduleLog(LOG_INFO, TAG, "BarContentContract: " + name + " fields=" + fields
                        + " constructors=" + java.util.Arrays.toString(type.getDeclaredConstructors()));
            } catch (ClassNotFoundException absent) {
                moduleLog(LOG_INFO, TAG, "BarContentContract: absent=" + name);
            } catch (Throwable failure) {
                moduleLog(LOG_WARN, TAG, "BarContentContract: unreadable=" + name, failure);
            }
        }
    }

    private void runBarFolmeProbe(ClassLoader loader, long generation) {
        if (!barFeatureState.accepts(generation)) {
            barFolmeProbePending = false;
            if (barFollowRestartPending) barPreferenceHandler.post(this::reconcileBarFollowPreference);
            return;
        }
        try {
            if (shadeBarEvents == null) shadeBarEvents = XiaomiShadeBarEvents.create(loader, new XiaomiShadeBarEvents.Sink() {
                public void selected(int role) {
                    AppBarContentPreparation content = pendingAppContentCleanup;
                    try { if (content != null) content.selectLiveRole(role); }
                    catch (Throwable failure) {
                        moduleLog(LOG_WARN, TAG, "App bar role selection failed; retiring exact owner", failure);
                        closeAppWindowProbe(content);
                    }
                }
                public void failed(Throwable failure) {
                    moduleLog(LOG_WARN, TAG, "A16 bar task selection unavailable; retaining previous selection", failure);
                }
            });
        } catch (Throwable failure) { moduleLog(LOG_WARN, TAG, "App bar task event bridge unavailable", failure); }
        // Keep the exact content owner reachable until cleanup really succeeds.
        // Reload must not discard a graph with outstanding native registrations.
        try {
            if (!appViewRoleHooksReady) throw new IllegalStateException("App View role guards are not ready");
            if (pendingAppContentCleanup != null) {
                pendingAppContentCleanup.close();
                pendingAppContentCleanup = null;
            }
            for (int cycle = 0; cycle < 2; cycle++) {
                try (AppBarContentPreparation prepared = AppBarContentPreparation.create(loader,
                        owner -> pendingAppContentCleanup = owner, this::appBarModeFailed)) {
                    prepared.verifyPrimaryUnchanged();
                    moduleLog(LOG_INFO, TAG, "App bar independent notification Views: "
                            + prepared.verifyNotificationViews());
                }
                pendingAppContentCleanup = null;
            }
            moduleLog(LOG_INFO, TAG, "App bar native content preparation verified: fresh root/binder,"
                    + " detached XML View/injector, fragment, independent notification Views/dark, shared island/animator;"
                    + " native 2041 window layout with owned injector Job (not attached);"
                    + " independent privacy controller bound to App battery/chip/dot with owned flow;"
                    + " native View dependency and selected shade targets resolved (detached App retains Home);"
                    + " App island listener bound/unbound without writing Home layout providers;"
                    + " native system-icon manager and owned-scope ViewModel with stock block-list lifecycle;"
                    + " two create/close cycles restored native registrations and main owners");
            AppBarContentPreparation attached = AppBarContentPreparation.create(loader,
                    owner -> pendingAppContentCleanup = owner, this::appBarModeFailed);
            attached.observeAppearance((message, failure) -> moduleLog(
                    failure == null ? LOG_INFO : LOG_WARN, TAG, message, failure));
            attached.attachWindowProbe(() -> {
                if (pendingAppContentCleanup != attached) return;
                if (!barFeatureState.accepts(generation)) { closeAppWindowProbe(attached); return; }
                try {
                    moduleLog(LOG_INFO, TAG, "App bar real-window attachment verified: type=2041, display=0,"
                            + " alpha=0, native children attached; " + attached.verifyAttachedWindowProbe());
                    attached.queryWindowProof(() -> {
                        try {
                            if (!barFeatureState.accepts(generation) || pendingAppContentCleanup != attached)
                                throw new IllegalStateException("App status-bar preference generation retired");
                            attached.verifyAttachedWindowProbe();
                            if (shadeBarEvents == null) throw new IllegalStateException("Task role bridge missing");
                            recentsBars.useNativeStatus(true);
                            attached.activateWindow(shadeBarEvents.current(false, true));
                        } catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
                    }, () -> {
                        if (!barFeatureState.accepts(generation) || pendingAppContentCleanup != attached) {
                            closeAppWindowProbe(attached);
                            return;
                        }
                        nativeBarWindowActive = true;
                        moduleLog(LOG_INFO, TAG, "App bar real window active; native WMS owns status animation");
                    },
                    (proof, failure) -> {
                        if (pendingAppContentCleanup != attached) return;
                        if (failure != null) moduleLog(LOG_WARN, TAG, "App bar server window proof unavailable", failure);
                        else {
                            proof.size(); // Decode before logging; Bundle's lazy string hides actual readiness.
                            moduleLog(LOG_INFO, TAG, "App bar authenticated server window proof: " + proof);
                        }
                        closeAppWindowProbe(attached);
                    });
                } catch (Throwable failure) {
                    moduleLog(LOG_WARN, TAG, "App bar real-window attachment verification failed", failure);
                    closeAppWindowProbe(attached);
                }
            });
        } catch (Throwable failure) {
            if (pendingAppContentCleanup != null && pendingAppContentCleanup.isClosed())
                pendingAppContentCleanup = null;
            moduleLog(LOG_WARN, TAG, "App bar native content preparation unavailable; no second root bound", failure);
        }
        try {
            final int[] frames = {0};
            final boolean[] reversed = {false};
            XiaomiBarFolme engine = new XiaomiBarFolme(loader, new XiaomiBarFolme.Sink() {
                public void update(float[] values, double[] velocities) {
                    frames[0]++;
                    if (!reversed[0] && values[1] < .8f && values[1] > .2f) {
                        reversed[0] = true;
                        moduleLog(LOG_INFO, TAG, "Native bar Folme retarget sample values="
                                + java.util.Arrays.toString(values) + " velocities="
                                + java.util.Arrays.toString(velocities));
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                            try {
                                XiaomiBarFolme active = barFolmeProbe;
                                if (active != null) {
                                    active.retargetStatus(0, 1);
                                    active.resetNavigation(0, 1);
                                    active.retargetNavigation(1, 0);
                                    moduleLog(LOG_INFO, TAG, "Native bar Folme independent status retarget"
                                            + " and navigation setTo/to issued");
                                }
                            } catch (Throwable failure) { finishBarFolmeProbe("retarget failed", failure); }
                        });
                    }
                }
                public void terminal(boolean cancelled) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                            finishBarFolmeProbe("terminal cancelled=" + cancelled + " updates="
                                    + frames[0] + " reversed=" + reversed[0], null));
                }
            });
            barFolmeProbe = engine;
            engine.set(new float[]{0, 1, 1, 0});
            engine.retarget(new float[]{1, 0, 0, 1});
            moduleLog(LOG_INFO, TAG, "Native bar Folme four-property probe started; no surfaces attached");
            // Diagnostic watchdog only, not an animation duration or completion proof.
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (barFolmeProbe == engine) finishBarFolmeProbe("probe watchdog: no terminal proof", null);
            }, 5000);
        } catch (Throwable failure) { finishBarFolmeProbe("probe failed", failure); }
    }

    private void appBarModeFailed(AppBarContentPreparation owner, RuntimeException failure) {
        moduleLog(LOG_WARN, TAG, "App bar Flow consumer failed; retiring exact owner", failure);
        // Let the current native collector return before cancelling its Job or
        // retiring the window. An old owner must never close its replacement.
        barPreferenceHandler.post(() -> {
            if (pendingAppContentCleanup == owner) closeAppWindowProbe(owner);
        });
    }

    private void closeAppWindowProbe(AppBarContentPreparation attached) {
        appBarRetiring = true;
        nativeBarWindowActive = false;
        try {
            attached.close();
            recentsBars.useNativeStatus(false);
            if (pendingAppContentCleanup == attached) pendingAppContentCleanup = null;
            appBarRetiring = false;
            java.util.concurrent.CountDownLatch reload = barReloadCompletion;
            if (reload != null) reload.countDown();
            if (barFollowRestartPending) barPreferenceHandler.post(this::reconcileBarFollowPreference);
            moduleLog(LOG_INFO, TAG, "App bar real-window detached; Home owners restored");
        } catch (Throwable failure) {
            if (attached.awaitingWindowRetirement()) {
                moduleLog(LOG_INFO, TAG, "App bar cleanup awaiting WMS committed retirement");
                return;
            }
            if (attached.resumeAfterNativeCleanup(() -> closeAppWindowProbe(attached))) {
                moduleLog(LOG_INFO, TAG, "App bar cleanup awaiting native notification coroutine completion");
                return;
            }
            moduleLog(LOG_WARN, TAG, "App bar real-window cleanup pending; retaining owner", failure);
        }
    }

    private void finishBarFolmeProbe(String reason, Throwable failure) {
        XiaomiBarFolme engine = barFolmeProbe;
        barFolmeProbe = null;
        try { if (engine != null) engine.close(); }
        catch (Throwable cleanup) { moduleLog(LOG_WARN, TAG, "Native Folme cleanup failed", cleanup); }
        barFolmeProbePending = false;
        if (barFollowRestartPending) barPreferenceHandler.post(this::reconcileBarFollowPreference);
        moduleLog(failure == null ? LOG_INFO : LOG_WARN, TAG, "Native bar Folme " + reason, failure);
    }

    protected Object observeBarFolmeUpdate(XposedInterface.Chain chain) throws Throwable {
        Object result = chain.proceed();
        recentsBars.dispatchFolme(chain.getThisObject(), (java.util.Collection<?>) chain.getArg(1), null);
        XiaomiBarFolme engine = barFolmeProbe;
        if (engine != null && engine.accepts(chain.getThisObject())) {
            try { engine.onUpdate(chain.getThisObject(), (java.util.Collection<?>) chain.getArg(1)); }
            catch (Throwable failure) { finishBarFolmeProbe("update failed", failure); }
        }
        return result;
    }
    protected Object observeBarFolmeComplete(XposedInterface.Chain chain) throws Throwable {
        Object result = chain.proceed();
        recentsBars.dispatchFolme(chain.getThisObject(), null, false);
        XiaomiBarFolme engine = barFolmeProbe;
        if (engine != null && engine.accepts(chain.getThisObject())) engine.onTerminal(chain.getThisObject(), false);
        return result;
    }
    protected Object observeBarFolmeCancel(XposedInterface.Chain chain) throws Throwable {
        Object result = chain.proceed();
        recentsBars.dispatchFolme(chain.getThisObject(), null, true);
        XiaomiBarFolme engine = barFolmeProbe;
        if (engine != null && engine.accepts(chain.getThisObject())) engine.onTerminal(chain.getThisObject(), true);
        return result;
    }

    protected Object prepareRecentsBarFollow(XposedInterface.Chain chain) throws Throwable {
        if (!barHooksReady || !barFeatureState.enabled() || !nativeBarWindowActive) return chain.proceed();
        synchronized (barReuseLock) {
            if (barReuseSession != null) barReuseSession.close();
            barReuseSession = null;
        }
        RecentsBarCallbackSession binding = new RecentsBarCallbackSession(chain.getThisObject());
        RecentsBarFollowController.Request request = null;
        synchronized (barCallbackLock) {
            if (barCallbackSession != null) barCallbackSession.close();
            barCallbackSession = binding;
            try {
                Object executor = readField(chain.getThisObject(), "mExecutor");
                if (executor instanceof Executor)
                    request = recentsBars.prepare(chain.getThisObject(), (Executor) executor);
            } catch (Throwable failure) {
                moduleLog(LOG_WARN, TAG, "Unable to prepare recents bars", failure);
            }
            binding.visualRequest = request;
        }
        try {
            Object token = chain.proceed();
            synchronized (barCallbackLock) {
                binding.token = token;
                if (token == null) binding.close();
                recentsBars.bind(request, token instanceof IBinder ? (IBinder) token : null);
            }
            return token;
        } catch (Throwable failure) {
            synchronized (barCallbackLock) { binding.close(); recentsBars.bind(request, null); }
            throw failure;
        }
    }

    protected Object startRecentsBarFollow(XposedInterface.Chain chain) throws Throwable {
        traceBarIdentity("start", barTraceSequence.incrementAndGet(),
                chain.getThisObject(), null, chain.getArg(0));
        RecentsBarFollowController.Session session = null;
        try {
            IBinder token = (IBinder) chain.getArg(0);
            Object controller = invokeAnyMethod(chain.getThisObject(), "findController",
                    new Object[]{token});
            if (controller != null) {
                synchronized (barCallbackLock) {
                    if (barCallbackSession != null && barCallbackSession.owner == chain.getThisObject()
                            && barCallbackSession.token == token) barCallbackSession.controller = controller;
                }
                session = recentsBars.stage(chain.getThisObject(), controller, token,
                        (TransitionInfo) chain.getArg(1),
                        (SurfaceControl.Transaction) chain.getArg(2));
            }
        } catch (Throwable failure) {
            moduleLog(LOG_WARN, TAG, "Unable to stage recents bar visuals", failure);
        }
        try {
            Object result = chain.proceed();
            recentsBars.started(session, Boolean.TRUE.equals(result));
            if (Boolean.TRUE.equals(result)) bindRecentsOpen(chain.getThisObject(),
                    chain.getArg(0), chain.getArg(1));
            if (Boolean.TRUE.equals(result)) bindStartedRecentsBarReuse(chain.getThisObject(),
                    chain.getArg(0), chain.getArg(1));
            return result;
        } catch (Throwable failure) {
            recentsBars.started(session, false);
            throw failure;
        }
    }

    protected Object cleanupRecentsBarFollow(XposedInterface.Chain chain) throws Throwable {
        synchronized (recentsOpenLock) {
            RecentsOpenSession opening = recentsOpenSession;
            if (opening != null && opening.controller == chain.getThisObject()) {
                if (pendingRecentsOpenFinish != null && pendingRecentsOpenFinish.session == opening) {
                    opening.close();
                    recentsOpenSession = null;
                    recentsOpenAttempt = null;
                } else invalidateRecentsOpen();
            }
        }
        synchronized (barReuseLock) {
            if (barReuseSession != null && barReuseSession.controller == chain.getThisObject()) {
                barReuseSession.close();
                barReuseSession = null;
            }
        }
        try {
            Object controller = chain.getThisObject();
            synchronized (barCallbackLock) {
                if (barCallbackSession != null && barCallbackSession.controller == controller) {
                    barCallbackSession.close();
                    barCallbackSession = null;
                }
            }
            traceBarIdentity("cleanup", barTraceSequence.incrementAndGet(),
                    readField(controller, "this$0"), null, readField(controller, "mTransition"));
        } catch (Throwable failure) {
            moduleLog(LOG_WARN, TAG, "RecentsBarTrace: cleanup identity unreadable", failure);
        }
        // cleanUp runs before Shell releases its runner leashes. Never retain their children.
        try { recentsBars.cleanup(chain.getThisObject()); }
        catch (Throwable failure) { moduleLog(LOG_WARN, TAG, "Bar cleanup failed", failure); }
        return chain.proceed();
    }

    protected Object stateRecentsBarFollow(XposedInterface.Chain chain) throws Throwable {
        long event = barTraceSequence.incrementAndGet();
        int state = (Integer) chain.getArg(0);
        int callerUid = android.os.Binder.getCallingUid();
        Object callback = chain.getThisObject();
        RecentsBarReuseSession reuseTicket;
        synchronized (barReuseLock) { reuseTicket = barReuseSession; }
        Object owner = null;
        try { owner = readField(callback, "this$0"); }
        catch (Throwable failure) {
            moduleLog(LOG_WARN, TAG, "RecentsBarTrace: callback owner unreadable", failure);
        }
        RecentsBarCallbackSession ticket;
        synchronized (barCallbackLock) { ticket = barCallbackSession; }
        boolean authenticated = authenticBarCaller(callerUid);
        traceBarIdentity("callback.enter state=" + state + " uid=" + callerUid,
                event, owner, callback, null);
        Object result;
        try {
            synchronized (barReuseLock) {
                result = chain.proceed();
                try {
                    if (reuseTicket != null && reuseTicket == barReuseSession) {
                        Object original = readField(callback, "val$original");
                        IBinder endpoint = barCallbackBinder(original);
                        boolean wasUnbound = reuseTicket.callback == null;
                        if (reuseTicket.adopt(owner, callback, original, endpoint,
                                authenticated && endpoint != null && endpoint.isBinderAlive(), state)) {
                            reuseTicket.delivered(callback, authenticated, state);
                            if (wasUnbound) moduleLog(LOG_INFO, TAG, "RecentsBarReuse: acknowledged callback="
                                    + barTraceId(callback) + " task=" + reuseTicket.taskId);
                        }
                    }
                } catch (Throwable failure) {
                    if (reuseTicket != null) reuseTicket.close();
                    moduleLog(LOG_WARN, TAG, "RecentsBarReuse: acknowledgement unavailable", failure);
                }
            }
        } catch (Throwable failure) {
            traceBarIdentity("callback.throw state=" + state + " failure="
                    + failure.getClass().getName(), event, owner, callback, null);
            throw failure; // Preserve the original Binder failure without manufacturing acceptance.
        }
        traceBarIdentity("callback.return state=" + state, event, owner, callback, null);
        try {
            boolean accepted;
            synchronized (barCallbackLock) {
                accepted = ticket != null && ticket == barCallbackSession
                        && ticket.accept(owner, callback, callerUid, authenticated, state);
                // Publish on the same serialized boundary as acceptance. Binder
                // threads must not enqueue an earlier HIDE after terminal SHOW.
                if (accepted) recentsBars.gestureState(
                        (RecentsBarFollowController.Request) ticket.visualRequest, state);
            }
            moduleLog(LOG_INFO, TAG, "RecentsBarTrace: session state=" + state
                    + " accepted=" + accepted + " authenticated=" + authenticated
                    + " event=" + event);
        } catch (Throwable failure) {
            moduleLog(LOG_WARN, TAG, "Unable to observe bar gesture state", failure);
        }
        return result;
    }

    private static String barTraceId(Object value) {
        return value == null ? "null" : value.getClass().getName() + "@"
                + Integer.toHexString(System.identityHashCode(value));
    }

    private void traceBarIdentity(String phase, long event, Object owner,
                                  Object callback, Object token) {
        try {
            Object latest = owner == null ? null : readField(owner, "mGestureStateUpdateCallback");
            Object original = callback == null ? null : readField(callback, "val$original");
            moduleLog(LOG_INFO, TAG, "RecentsBarTrace: " + phase + " event=" + event
                    + " owner=" + barTraceId(owner) + " callback=" + barTraceId(callback)
                    + " latest=" + barTraceId(latest) + " matchesLatest="
                    + (callback != null && callback == latest)
                    + " original=" + barTraceId(original) + " token=" + barTraceId(token));
        } catch (Throwable failure) {
            moduleLog(LOG_WARN, TAG, "RecentsBarTrace: identity unavailable phase=" + phase
                    + " event=" + event, failure);
        }
    }

    private boolean authenticBarCaller(int uid) {
        try {
            android.app.Application app = (android.app.Application) Class.forName("android.app.ActivityThread")
                    .getMethod("currentApplication").invoke(null);
            if (app == null) return false;
            String[] packages = app.getPackageManager().getPackagesForUid(uid);
            if (packages != null) for (String name : packages)
                if ("com.miui.home".equals(name)) return true;
        } catch (Throwable ignored) { }
        return false;
    }

    protected boolean reserveRecentsBarReload() {
        if (barFolmeProbePending) return false;
        AppBarContentPreparation pending = pendingAppContentCleanup;
        // Never wait on the View owner, and leave a rejected active session
        // intact. reserveReload atomically fences a racing prepare/stage.
        if (pending != null && android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) return false;
        if (!recentsBars.reserveReload()) return false;
        barFeatureState.suspend();
        barFollowRestartPending = false;
        if (pending != null) {
            // The API callback is a worker/Binder call. Wait for the real WMS
            // retirement reply and notification Job completion, while Main and
            // the native owner keep running. false would cancel this install's
            // reload permanently; it is not an asynchronous continuation API.
            java.util.concurrent.CountDownLatch completed = new java.util.concurrent.CountDownLatch(1);
            barReloadCompletion = completed;
            boolean ready = false;
            try {
                if (barPreferenceHandler.post(() -> closeAppWindowProbe(pending)))
                    ready = completed.await(5L, java.util.concurrent.TimeUnit.SECONDS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            } finally { barReloadCompletion = null; }
            if (!ready || pendingAppContentCleanup != null) {
                moduleLog(LOG_WARN, TAG, "Status-bar retirement did not complete; rejecting reload and recovering old generation");
                barPreferenceHandler.post(() -> {
                    recentsBars.resumeAfterRejectedReload();
                    barFeatureState.resumeAfterRejectedReload();
                    reconcileBarFollowPreference();
                });
                return false;
            }
            moduleLog(LOG_INFO, TAG, "Status-bar retirement completed within this hot-reload request");
        }
        synchronized (barCallbackLock) {
            if (barCallbackSession != null) barCallbackSession.close();
            barCallbackSession = null;
        }
        return true;
    }

    protected final void closeBarFollowRuntime() {
        try {
            SharedPreferences preferences = barFollowPreferences;
            if (preferences != null) preferences.unregisterOnSharedPreferenceChangeListener(barFollowPreferenceListener);
            barPreferenceHandler.removeCallbacksAndMessages(null);
            detachRecentsBarFollow();
        } catch (Throwable ignored) {
            // Cleanup is best effort; the next generation owns fresh registrations.
        }
    }

    protected void detachRecentsBarFollow() {
        synchronized (recentsOpenLock) {
            recentsOpenHooksActive = false;
            invalidateRecentsOpen();
        }
        barFeatureState.suspend();
        barFollowRestartPending = false;
        barFollowClassLoader = null;
        barPreferenceHandler.removeCallbacksAndMessages(null);
        SharedPreferences preferences = barFollowPreferences;
        barFollowPreferences = null;
        if (preferences != null) try {
            preferences.unregisterOnSharedPreferenceChangeListener(barFollowPreferenceListener);
        } catch (Throwable failure) { moduleLog(LOG_WARN, TAG, "Status-bar preference listener cleanup failed", failure); }
        recentsBars.onIdle(null);
        nativeBarWindowActive = false;
        synchronized (barReuseLock) {
            if (barReuseSession != null) barReuseSession.close();
            barReuseSession = null;
        }
        XiaomiShadeBarEvents events = shadeBarEvents;
        shadeBarEvents = null;
        if (events != null) events.close();
        recentsBars.detach();
    }
}
