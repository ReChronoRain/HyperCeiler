/*
 * Derived from MiuiBackGestureHook status-bar-follow implementation.
 * Source: https://github.com/wxxsfxyzm/MiuiBackGestureHook
 * Reference commit: 004bc059e5821775917b771d89aa1d8c18a1e669
 * Copyright (C) 2026 MiuiBackGestureHook contributors.
 * Licensed under the Apache License, Version 2.0; modified for HyperCeiler.
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.statusbarfollow;

/** Shell owns publication; input only reads revocable, per-direction receipts. */
public final class RecentsOpenSession {
    public final Object owner, controller, transition, info, task, taskToken, topActivity;
    public final int taskId;
    public final boolean launchFromHome;
    private boolean closed;
    private Attempt attempt;

    public RecentsOpenSession(Object owner, Object controller, Object transition,
            Object info, Object task, int taskId, Object taskToken, Object topActivity,
            boolean launchFromHome) {
        this.owner = owner;
        this.controller = controller;
        this.transition = transition;
        this.info = info;
        this.task = task;
        this.taskId = taskId;
        this.taskToken = taskToken;
        this.topActivity = topActivity;
        this.launchFromHome = launchFromHome;
    }

    public Attempt toApp() {
        if (closed) return null;
        if (attempt == null) attempt = new Attempt(this);
        return attempt;
    }

    public void toHome() {
        if (attempt != null) attempt.active = false;
        attempt = null;
    }

    public void close() { closed = true; toHome(); }

    public static final class Attempt {
        public final RecentsOpenSession session;
        private volatile boolean active = true;
        private volatile boolean completedToApp;
        private Object pendingFinishHandler, pendingFinishToken;

        private Attempt(RecentsOpenSession session) { this.session = session; }
        public boolean isActive() { return active; }
        public boolean isCompletedToApp() { return completedToApp; }

        /** Only the successful, matching native finish transaction may publish this. */
        public void completedToApp() { active = false; completedToApp = true; }

        public boolean bindPendingFinish(Object handler, Object token) {
            if (!active || handler == null || token == null) return false;
            if (pendingFinishToken != null
                    && (pendingFinishHandler != handler || pendingFinishToken != token)) return false;
            pendingFinishHandler = handler;
            pendingFinishToken = token;
            return true;
        }

        public boolean matchesPendingFinish(Object handler, Object token) {
            return active && token != null && pendingFinishHandler == handler && pendingFinishToken == token;
        }

        public boolean hasPendingFinish(Object token) {
            return active && token != null && pendingFinishToken == token;
        }

        public boolean matches(Object owner, Object controller, Object transition,
                Object info, Object task, int taskId) {
            return active && session.owner == owner && session.controller == controller
                    && session.transition == transition && session.info == info
                    && session.task == task && session.taskId == taskId;
        }
    }
}
