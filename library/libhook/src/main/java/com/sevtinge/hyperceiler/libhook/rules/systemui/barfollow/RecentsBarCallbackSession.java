/*
 * Derived from MiuiBackGestureHook status-bar-follow implementation.
 * Source: https://github.com/wxxsfxyzm/MiuiBackGestureHook
 * Reference commit: 004bc059e5821775917b771d89aa1d8c18a1e669 (includes b6e582f and later fixes)
 * Copyright (C) 2026 MiuiBackGestureHook contributors.
 * Licensed under the Apache License, Version 2.0; modified for HyperCeiler.
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow;

/** Reference identities only; callers serialize access. No wall-clock admission window. */
final class RecentsBarCallbackSession {
    final Object owner;
    Object token;
    Object visualRequest;
    Object controller;
    private Object callback;
    private int uid = -1;
    private boolean ended;

    RecentsBarCallbackSession(Object owner) { this.owner = owner; }

    boolean accept(Object senderOwner, Object senderCallback, int senderUid,
                   boolean authenticated, int state) {
        if (ended || !authenticated || senderOwner != owner || senderCallback == null) return false;
        // 14 is wallpaper-blur first-frame delivery, not gesture ownership.
        // 6 is taskSnapshot, not termination. Its Dart/FRB delivery may precede
        // prepare and has no per-gesture identity in this Binder contract. Until
        // that identity is bridged, neither replay it nor let it close this lease.
        if (state == 6 || state == 14) return false;
        if (callback == null) {
            if (state != 12) return false;
            callback = senderCallback;
            uid = senderUid;
        }
        if (callback != senderCallback || uid != senderUid) return false;
        if (state == 0 || state == 1 || state == 2 || state == 5
                || state == 7 || state == 9 || state == 10) ended = true;
        return true;
    }

    void close() { ended = true; }
}
