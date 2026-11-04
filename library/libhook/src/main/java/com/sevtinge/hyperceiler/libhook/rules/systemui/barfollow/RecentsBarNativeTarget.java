/*
 * Derived from MiuiBackGestureHook status-bar-follow implementation.
 * Source: https://github.com/wxxsfxyzm/MiuiBackGestureHook
 * Reference commit: 004bc059e5821775917b771d89aa1d8c18a1e669 (includes b6e582f and later fixes)
 * Copyright (C) 2026 MiuiBackGestureHook contributors.
 * Licensed under the Apache License, Version 2.0; modified for HyperCeiler.
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow;

import android.graphics.Rect;
import android.os.IBinder;
import android.os.IInterface;
import android.view.SurfaceControl;
import android.window.TransitionInfo;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow.AppBarContentPreparation.call;

/** Immutable task/Surface receipt from Shell's native accepted-target delivery. Never animates. */
final class RecentsBarNativeTarget {
    final Object task, token, listener;
    final SurfaceControl leash, surface;
    final int taskId, mergeId;
    private final Rect bounds;

    private RecentsBarNativeTarget(Object task, Object token, Object listener, SurfaceControl leash,
            SurfaceControl surface, int taskId, int mergeId, Rect bounds) {
        this.task = task; this.token = token; this.listener = listener;
        this.leash = leash; this.surface = surface; this.taskId = taskId; this.mergeId = mergeId;
        this.bounds = new Rect(bounds);
    }

    static RecentsBarNativeTarget capture(RecentsBarReuseSession session, Object controller,
            List<?> appeared, TransitionInfo merged) throws ReflectiveOperationException {
        if (appeared == null || merged == null || read(controller, "mInfo") != session.info
                || read(controller, "mMergedTransitionInfo") != merged
                || !Integer.valueOf(merged.getDebugId()).equals(read(controller, "mLastNotifiedMergeInfoId"))
                || (merged.getType() != 1 && merged.getType() != 3 && merged.getType() != 6)
                || merged.getRootCount() != 1 || merged.getRoot(0).getDisplayId() != 0
                || !valid(merged.getRoot(0).getLeash())) return null;
        TransitionInfo root = (TransitionInfo) session.info;
        Rect full = null;
        for (TransitionInfo.Change change : root.getChanges()) {
            if (change.getTaskInfo() == null || change.getTaskInfo().taskId != session.taskId) continue;
            if (full != null || !read(session.task, "mToken").equals(call(change, "getContainer"))
                    || change.getStartAbsBounds().isEmpty()
                    || !change.getStartAbsBounds().equals(change.getEndAbsBounds())) return null;
            full = new Rect(change.getEndAbsBounds());
        }
        if (full == null || full.left != 0 || full.top != 0) return null;
        Object current = currentTask(controller, root, full);
        if (current == null) return null;
        Object info = read(current, "mTaskInfo");
        int id = (Integer) read(info, "taskId");
        Object delivered = null;
        for (Object target : appeared) {
            Object config = read(target, "windowConfiguration");
            if (!Integer.valueOf(1).equals(call(config, "getActivityType"))) continue;
            // Closing targets are not the current app destination. A second
            // opening app makes the native update ambiguous for this adapter.
            if (!Integer.valueOf(0).equals(read(target, "mode"))) continue;
            if (delivered != null || !Integer.valueOf(id).equals(read(target, "taskId"))) return null;
            delivered = target;
        }
        Object listener = read(controller, "mListener");
        if (!alive(listener)) return null;
        if (delivered == null) {
            // Home-only target updates retain a proven, unchanged app. This is
            // important when a subsequent Home preview is cancelled back to B.
            RecentsBarReuseSession.Target previous = session.previousTarget();
            if (previous != null && previous.proof instanceof RecentsBarNativeTarget) {
                RecentsBarNativeTarget old = (RecentsBarNativeTarget) previous.proof;
                return old.matchesCurrent(controller, root) ? old : null;
            }
            if (current != session.task) return null;
            return fromTask(current, listener, id, merged.getDebugId(), full);
        }
        SurfaceControl nativeLeash = (SurfaceControl) read(current, "mLeash");
        SurfaceControl nativeSurface = (SurfaceControl) read(current, "mTaskSurface");
        if (!valid(nativeLeash) || !valid(nativeSurface)
                || !same(nativeLeash, (SurfaceControl) read(delivered, "leash"))) return null;
        Object taskToken = read(current, "mToken");
        TransitionInfo.Change selected = null;
        for (TransitionInfo.Change change : merged.getChanges()) {
            if (change.getTaskInfo() == null || change.getTaskInfo().taskId != id) continue;
            if (selected != null || !taskToken.equals(call(change, "getContainer"))
                    || !same(nativeSurface, change.getLeash()) || !fullscreen(change, full)
                    || (change.getMode() != 1 && change.getMode() != 3 && change.getMode() != 6)) return null;
            selected = change;
        }
        return selected == null ? null : fromTask(current, listener, id, merged.getDebugId(), full);
    }

    private static RecentsBarNativeTarget fromTask(Object task, Object listener, int id, int mergeId,
            Rect bounds) throws ReflectiveOperationException {
        SurfaceControl leash = (SurfaceControl) read(task, "mLeash");
        SurfaceControl surface = (SurfaceControl) read(task, "mTaskSurface");
        return valid(leash) && valid(surface) ? new RecentsBarNativeTarget(task, read(task, "mToken"),
                listener, leash, surface, id, mergeId, bounds) : null;
    }

    boolean matchesCurrent(Object controller, TransitionInfo root) throws ReflectiveOperationException {
        return read(controller, "mInfo") == root && read(controller, "mListener") == listener && alive(listener)
                && currentTask(controller, root, bounds) == task
                && Integer.valueOf(taskId).equals(read(read(task, "mTaskInfo"), "taskId"))
                && token.equals(read(task, "mToken")) && valid(leash) && valid(surface)
                && same(leash, (SurfaceControl) read(task, "mLeash"))
                && same(surface, (SurfaceControl) read(task, "mTaskSurface"));
    }

    private static Object currentTask(Object controller, TransitionInfo root, Rect bounds)
            throws ReflectiveOperationException {
        Object opening = null, pausing = null;
        int pausedApps = 0, apps = 0, homes = 0;
        Set<Integer> ids = new HashSet<>();
        for (String group : new String[]{"mOpeningTasks", "mPausingTasks", "mClosingTasks"}) {
            Object value = read(controller, group);
            if (!(value instanceof List)) return null;
            for (Object candidate : (List<?>) value) {
                Object info = read(candidate, "mTaskInfo");
                if (info == null || !Integer.valueOf(0).equals(read(info, "displayId"))
                        || !Integer.valueOf(1).equals(call(info, "getWindowingMode"))) return null;
                int id = (Integer) read(info, "taskId");
                if (id < 0 || !ids.add(id)) return null;
                if (Integer.valueOf(2).equals(call(info, "getActivityType"))) {
                    if (++homes > 1 || !knownHome(root, candidate, id, bounds)) return null;
                    continue;
                }
                if (!Integer.valueOf(1).equals(call(info, "getActivityType")) || ++apps > 2
                        || !Boolean.FALSE.equals(read(candidate, "mIsTranslucent"))) return null;
                Object configuration = read(info, "configuration");
                if (!bounds.equals(call(read(configuration, "windowConfiguration"), "getBounds"))) return null;
                if (group.equals("mOpeningTasks")) {
                    if (opening != null) return null;
                    opening = candidate;
                } else if (group.equals("mPausingTasks")) {
                    pausedApps++;
                    pausing = candidate;
                }
            }
        }
        return opening != null ? opening : pausedApps == 1 ? pausing : null;
    }

    private static boolean knownHome(TransitionInfo root, Object task, int id, Rect bounds)
            throws ReflectiveOperationException {
        int matches = 0;
        for (TransitionInfo.Change change : root.getChanges()) {
            Object info = change.getTaskInfo();
            if (info == null || !Integer.valueOf(id).equals(read(info, "taskId"))) continue;
            if (!Integer.valueOf(2).equals(call(info, "getActivityType")) || !fullscreen(change, bounds)
                    || !read(task, "mToken").equals(call(change, "getContainer"))) return false;
            matches++;
        }
        return matches == 1;
    }

    private static boolean fullscreen(TransitionInfo.Change change, Rect bounds)
            throws ReflectiveOperationException {
        return change.getEndDisplayId() == 0
                && Integer.valueOf(1).equals(call(change, "getStartWindowMode"))
                && Integer.valueOf(1).equals(call(change, "getEndWindowMode"))
                && Integer.valueOf(0).equals(call(change, "getStartRotation"))
                && Integer.valueOf(0).equals(call(change, "getEndRotation"))
                && call(change, "getParent") == null && call(change, "getLastParent") == null
                && bounds.equals(change.getStartAbsBounds()) && bounds.equals(change.getEndAbsBounds());
    }

    private static boolean alive(Object value) {
        if (!(value instanceof IInterface)) return false;
        IBinder binder = ((IInterface) value).asBinder();
        return binder != null && binder.isBinderAlive();
    }

    private static boolean valid(SurfaceControl surface) { return surface != null && surface.isValid(); }
    private static Object read(Object owner, String name) throws ReflectiveOperationException {
        for (Class<?> type = owner.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(owner);
            } catch (NoSuchFieldException absent) { }
        }
        throw new NoSuchFieldException(owner.getClass().getName() + "." + name);
    }
    private static boolean same(SurfaceControl first, SurfaceControl second) throws ReflectiveOperationException {
        return valid(first) && valid(second) && (first == second || Boolean.TRUE.equals(call(first,
                "isSameSurface", new Class<?>[]{SurfaceControl.class}, second)));
    }
}
