/*
 * Derived from MiuiBackGestureHook status-bar-follow implementation.
 * Source: https://github.com/wxxsfxyzm/MiuiBackGestureHook
 * Reference commit: 004bc059e5821775917b771d89aa1d8c18a1e669 (includes b6e582f and later fixes)
 * Copyright (C) 2026 MiuiBackGestureHook contributors.
 * Licensed under the Apache License, Version 2.0; modified for HyperCeiler.
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow;

/** Caller serializes callback delivery and Shell handoff; identities are never time based. */
final class RecentsBarReuseSession {
    static final class Target {
        final Object task, proof;
        final int taskId;

        Target(Object task, int taskId, Object proof) {
            this.task = task; this.taskId = taskId; this.proof = proof;
        }
    }

    final Object owner, controller, transition, info, task;
    Object callback, original, endpoint;
    final int taskId;
    private boolean homeTarget;
    private boolean closed;
    private Object targetUpdate;
    private Target target;

    RecentsBarReuseSession(Object owner, Object controller, Object transition, Object info,
            Object callback, Object original, Object endpoint, Object task, int taskId) {
        this.owner = owner; this.controller = controller; this.transition = transition;
        this.info = info; this.callback = callback; this.original = original; this.endpoint = endpoint;
        this.task = task; this.taskId = taskId;
    }

    void delivered(Object source, boolean authenticated, int state) {
        if (closed || source != callback || !authenticated) return;
        // Snapshot/blur delivery is not a new transition or a new alpha target.
        // Native state 3 updates gesture bookkeeping only. It can arrive after
        // state 1 and must not erase the still-active Home alpha target.
        if (state == 3 || state == 6 || state == 14) return;
        homeTarget = state == 1;
    }

    boolean adopt(Object sourceOwner, Object source, Object sourceOriginal, Object sourceEndpoint,
            boolean authenticated, int state) {
        if (closed || !authenticated || sourceOwner != owner || source == null
                || sourceOriginal == null || sourceEndpoint == null) return false;
        // Start acknowledgement binds the actual retained Launcher callback. Never
        // adopt a terminal state or replace it with Shell's newest publication.
        if (callback == null) {
            if (state != 12) return false;
            callback = source; original = sourceOriginal; endpoint = sourceEndpoint;
        }
        return callback == source && endpoint == sourceEndpoint;
    }

    boolean consume(Object currentController, Object currentTransition, Object currentInfo,
            Object currentCallback, Object currentTask, boolean finishToHome, boolean live) {
        // Once native delivery selected B, the original A is no longer a
        // fallback merely because its task remains on the controller.
        if (targetUpdate != null || target != null) return false;
        return consumeOwner(currentController, currentTransition, currentInfo, currentCallback,
                currentTask == task, finishToHome, live);
    }

    Object beginTargetUpdate() {
        if (closed) return null;
        return targetUpdate = new Object();
    }

    boolean acceptTarget(Object update, Target next) {
        if (closed || update == null || targetUpdate != update) return false;
        targetUpdate = null;
        target = next != null && next.task != null && next.taskId >= 0 && next.proof != null ? next : null;
        return target != null;
    }

    Target currentTarget() { return closed || targetUpdate != null ? null : target; }
    Target previousTarget() { return closed ? null : target; }
    boolean awaitingAppShow() { return !closed && homeTarget; }

    boolean consumeTarget(Object currentController, Object currentTransition, Object currentInfo,
            Object currentCallback, Target receipt, Object currentTask, boolean finishToHome, boolean live) {
        return consumeOwner(currentController, currentTransition, currentInfo, currentCallback,
                targetUpdate == null && receipt != null && target == receipt && currentTask == receipt.task,
                finishToHome, live);
    }

    private boolean consumeOwner(Object currentController, Object currentTransition, Object currentInfo,
            Object currentCallback, boolean matchesTask, boolean finishToHome, boolean live) {
        if (closed || !homeTarget || !live || finishToHome
                || currentController != controller || currentTransition != transition
                || currentInfo != info || currentCallback != callback || !matchesTask) return false;
        homeTarget = false;
        return true;
    }

    void close() { closed = true; homeTarget = false; targetUpdate = null; target = null; }

}
