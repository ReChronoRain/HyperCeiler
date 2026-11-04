/*
 * Derived from MiuiBackGestureHook status-bar-follow implementation.
 * Source: https://github.com/wxxsfxyzm/MiuiBackGestureHook
 * Reference commit: 004bc059e5821775917b771d89aa1d8c18a1e669 (includes b6e582f and later fixes)
 * Copyright (C) 2026 MiuiBackGestureHook contributors.
 * Licensed under the Apache License, Version 2.0; modified for HyperCeiler.
 */
package com.sevtinge.hyperceiler.libhook.rules.systemframework.statusbarfollow;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.view.SurfaceControl;
import android.view.WindowManager;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.BiConsumer;

/** Read-only WMS ownership proof. This endpoint does not grant animation ownership. */
final class ServerBarWindowBridge implements AutoCloseable {
    static final String ACTION = "dev.codex.miuibackgesturehook.APP_BAR_WINDOW_PROOF";
    static final String DESCRIPTOR = ACTION + ".Reply";
    private final Context context;
    private final Object impl;
    private final BiConsumer<String, Throwable> log;
    private volatile boolean closed;
    private final java.util.concurrent.CountDownLatch closeCompleted = new java.util.concurrent.CountDownLatch(1);
    private Runnable afterClose;
    private final Handler owner;
    private final ServerBarFollowLifecycle lifecycle;
    private volatile ServerBarPreparedWindow prepared;
    boolean animationEnabled() {
        ServerBarPreparedWindow lease = prepared;
        return !closed && lease != null && lease.enabled && lease.client.isBinderAlive();
    }

    void recordNative(Object controller, Integer state) {
        ServerBarPreparedWindow lease = prepared;
        if (lease == null || lease.controller != controller || !animationEnabled()) return;
        int frames = state == null ? lease.frames.incrementAndGet() : lease.frames.get();
        if (state == null && frames > 4) return;
        try {
            log.accept("App bar native evidence: generation=" + lease.generation + ", state=" + state
                    + ", frames=" + frames + ", homeAlpha=" + read(controller, "mCurrentStatusBarAlpha")
                    + ", appAlpha=" + read(controller, "mCurrentFakeStatusBarAlpha")
                    + ", appParentCache=" + surfaceIdentity(read(controller, "mAppSurface")), null);
        } catch (ReflectiveOperationException failure) { log.accept("App bar evidence unreadable", failure); }
    }

    void recordDecision(Object controller, String operation, String phase, java.util.List<?> arguments) {
        // Observe only native decision boundaries, never the per-frame Surface writes.
        if (!operation.equals("startStatusBarAnimation") && !operation.equals("animateStatusBarTo")
                && !operation.equals("stopFolmeAnimate") && !operation.equals("resetStatusBarState")
                && !operation.equals("bindAssociatedTask")
                && !operation.equals("reparentStausBarToAppTask")
                && !operation.equals("reparentAppBarToBirthParent")) return;
        ServerBarPreparedWindow lease = prepared;
        if (lease == null || lease.controller != controller || !animationEnabled()) return;
        try {
            String targets = operation.equals("animateStatusBarTo") || operation.equals("stopFolmeAnimate")
                    ? ", targets=" + arguments : "";
            // Read on the intercepted native caller, without posting Task objects to another Looper.
            log.accept("BarDecision generation=" + lease.generation + ", operation=" + operation
                    + ", phase=" + phase + targets
                    + ", state=" + read(controller, "mStatusState")
                    + ", lastState=" + read(controller, "mLastStatusState")
                    + ", homeAlpha=" + read(controller, "mCurrentStatusBarAlpha")
                    + ", appAlpha=" + read(controller, "mCurrentFakeStatusBarAlpha")
                    + ", homeTargetVisible=" + read(controller, "mLastHomeBarVisible")
                    + ", appTargetVisible=" + read(controller, "mLastAppBarVisible")
                    + ", associated=" + identity(read(controller, "mAssociatedTask"))
                    + ", top=" + identity(read(controller, "mCurrentTopTask"))
                    + ", associatedVisible=" + read(controller, "mAssociatedTaskisVisible")
                    + ", topVisible=" + read(controller, "mCurrentTopTaskisVisible")
                    + ", homeVisible=" + read(controller, "mHomeTaskisVisible")
                    + ", appTaskId=" + read(controller, "mAppTaskId")
                    + ", appParentCache=" + surfaceIdentity(read(controller, "mAppSurface"))
                    + ", appContainer=" + surfaceIdentity(read(read(controller, "mTaskBarData"),
                            "mAppBarContainerSurface")), null);
        } catch (ReflectiveOperationException | RuntimeException failure) {
            log.accept("App bar decision evidence unreadable", failure);
        }
    }

    private static String identity(Object value) {
        return value == null ? "null" : value.getClass().getSimpleName() + "@"
                + Integer.toHexString(System.identityHashCode(value));
    }

    private static String surfaceIdentity(Object value) {
        if (!(value instanceof SurfaceControl)) return "null";
        SurfaceControl surface = (SurfaceControl) value;
        // Surface identity only. The compositor graph, not this cache, proves ancestry.
        return surface + "/valid=" + surface.isValid();
    }
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context ignored, Intent intent) {
            if (closed || !ACTION.equals(intent.getAction()) || !"android".equals(intent.getPackage())) return;
            int uid = getSentFromUid();
            if (!"com.android.systemui".equals(getSentFromPackage())) return;
            String[] packages = context.getPackageManager().getPackagesForUid(uid);
            if (packages == null || !Arrays.asList(packages).contains("com.android.systemui")) return;
            try {
                Bundle request = intent.getExtras();
                if (request == null) return;
                IBinder client = request.getBinder("window");
                IBinder reply = request.getBinder("reply");
                String generation = request.getString("generation");
                if (client == null || reply == null || generation == null || generation.length() != 36) return;
                Bundle result;
                Object wms = read(impl, "mWmService");
                synchronized (read(wms, "mGlobalLock")) {
                    if (closed) return;
                    result = inspect(uid, client);
                    if (result.getBoolean("windowOwned") && result.getBoolean("drawn")
                            && result.getBoolean("tokenSurfacesValid") && (prepared == null || !prepared.begun)) {
                        Object controller = read(impl, "mStatusBarController");
                        ServerBarPreparedWindow lease = new ServerBarPreparedWindow(controller,
                                call(controller, "getAppTaskBar"), call(controller, "getHomeTaskBar"),
                                read(wms, "mGlobalLock"), owner, client, generation, uid, lifecycle, () -> {
                            Bundle retired = new Bundle();
                            retired.putString("phase", "retired");
                            retired.putBoolean("retirementCommitted", true);
                            sendReply(reply, generation, client, retired);
                            prepared = null;
                            completeCloseIfIdle();
                        });
                        prepared = lease;
                        result.putString("phase", "offered");
                        result.putBinder("lease", new Binder() {
                            @Override protected boolean onTransact(int code, Parcel data, Parcel out, int flags) {
                                if ((code < FIRST_CALL_TRANSACTION || code > FIRST_CALL_TRANSACTION + 3)
                                        || Binder.getCallingUid() != uid) return false;
                                data.enforceInterface(DESCRIPTOR + ".Lease");
                                if (!generation.equals(data.readString()) || !client.equals(data.readStrongBinder())) return false;
                                data.enforceNoDataAvail();
                                owner.post(() -> {
                                    if (prepared != lease) return;
                                    if (code == FIRST_CALL_TRANSACTION) { lease.retire(); return; }
                                    if (code == FIRST_CALL_TRANSACTION + 3) {
                                        if (closed) { lease.retire(); return; }
                                        try {
                                            lease.activate();
                                            Bundle active = new Bundle();
                                            active.putString("phase", "active");
                                            if (!sendReply(reply, generation, client, active)) lease.retire();
                                            log.accept("App bar native animation lease active", null);
                                        } catch (Throwable failure) {
                                            log.accept("App bar activation rejected; retiring", failure);
                                            lease.retire();
                                        }
                                        return;
                                    }
                                    if (code == FIRST_CALL_TRANSACTION + 2) {
                                        if (closed) { lease.retire(); return; }
                                        try {
                                            lease.arm(() -> {
                                                Bundle armed = new Bundle();
                                                armed.putString("phase", "armed");
                                                armed.putBoolean("nativeRestorerReady", true);
                                                sendReply(reply, generation, client, armed);
                                            });
                                        } catch (Throwable failure) {
                                            log.accept("App bar native arming failed; retiring", failure);
                                            lease.retire();
                                        }
                                        return;
                                    }
                                    if (lease.begun) return;
                                    if (closed) {
                                        Bundle rejected = new Bundle();
                                        rejected.putString("phase", "rejected");
                                        sendReply(reply, generation, client, rejected);
                                        return;
                                    }
                                    try {
                                        lease.verifyCurrent();
                                        lease.begin();
                                        Bundle accepted = new Bundle();
                                        accepted.putString("phase", "prepared");
                                        if (!sendReply(reply, generation, client, accepted)) lease.retire();
                                    } catch (Throwable failure) {
                                        log.accept("App bar lease confirmation rejected", failure);
                                        Bundle rejected = new Bundle();
                                        rejected.putString("phase", "rejected");
                                        sendReply(reply, generation, client, rejected);
                                        if (!lease.begun) prepared = null;
                                    }
                                });
                                return true;
                            }
                        });
                    }
                }
                // Never make a remote call while holding WMS's global lock.
                if (closed) return;
                if (!sendReply(reply, generation, client, result) && prepared != null) prepared.retire();
                log.accept("App bar WMS window proof: " + result, null);
            } catch (Throwable failure) { log.accept("App bar WMS window proof failed", failure); }
        }
    };

    ServerBarWindowBridge(Object impl, ServerBarFollowLifecycle lifecycle, BiConsumer<String, Throwable> log) throws ReflectiveOperationException {
        this.impl = impl;
        this.log = log;
        context = (Context) read(impl, "mContext");
        this.lifecycle = lifecycle;
        owner = (Handler) read(impl, "mHandler");
        if (context == null || owner == null || read(impl, "mWmService") == null)
            throw new IllegalStateException("Native bar server is not ready");
        context.registerReceiver(receiver, new IntentFilter(ACTION), null, owner, Context.RECEIVER_EXPORTED);
    }

    private Bundle inspect(int uid, IBinder client) throws ReflectiveOperationException {
        Bundle proof = new Bundle();
        proof.putBoolean("windowOwned", false);
        Object controller = read(impl, "mStatusBarController");
        Object home = call(controller, "getHomeTaskBar");
        Object app = call(controller, "getAppTaskBar");
        if (app == null || home == null || app == home) return proof;
        WindowManager.LayoutParams attrs = (WindowManager.LayoutParams) read(app, "mAttrs");
        if (((Number) read(app, "mOwnerUid")).intValue() != uid
                || ((Number) read(home, "mOwnerUid")).intValue() != uid
                || !"com.android.systemui".equals(attrs.packageName)
                || attrs.type != 2041 || !"StatusBar1".contentEquals(attrs.getTitle())
                || !client.equals(((IInterface) read(app, "mClient")).asBinder())
                || (Boolean) read(app, "mRemoved") || (Boolean) read(home, "mRemoved")
                || ((Number) call(app, "getDisplayId")).intValue() != 0
                || ((Number) call(home, "getDisplayId")).intValue() != 0) return proof;
        Object appToken = read(app, "mToken");
        Object homeToken = read(home, "mToken");
        if (appToken == null || homeToken == null || appToken == homeToken) return proof;
        proof.putBoolean("windowOwned", true);
        proof.putBoolean("tokenSurfacesValid", valid(call(appToken, "getSurfaceControl"))
                && valid(call(homeToken, "getSurfaceControl")));
        proof.putBoolean("windowSurfaceValid", valid(call(app, "getSurfaceControl")));
        proof.putBoolean("drawn", (Boolean) call(app, "isDrawn"));
        proof.putInt("display", 0);
        proof.putInt("type", attrs.type);
        // Verify the native prerequisites separately from WMS window readiness.
        // No initialization, reparent or appearance update is performed here.
        Object cache = read(controller, "mTaskBarData");
        proof.putBoolean("nativeWindowCacheMatches", cache != null
                && read(cache, "mHomelBarContainer") == homeToken && read(cache, "mAppBarContainer") == appToken
                && valid(read(cache, "mHomeBarContainerSurface")) && valid(read(cache, "mAppBarContainerSurface")));
        Object homeTask = read(controller, "mHomeTask");
        Object appTask = read(controller, "mCurrentTopTask");
        proof.putBoolean("nativeHomeParentValid", homeTask != null && valid(call(homeTask, "getStatusBarParent")));
        proof.putBoolean("nativeAppParentValid", appTask != null && valid(call(appTask, "getStatusBarParent")));
        Object callback = read(impl, "mBarFollowExecuteCallBack");
        proof.putBoolean("nativeCallbackAlive", callback instanceof IInterface
                && ((IInterface) callback).asBinder().isBinderAlive());
        proof.putBoolean("nativeSplitScreen", (Boolean) call(controller, "isInSplitScreen"));
        proof.putBoolean("nativeFullGestureProcessing", (Boolean) read(impl, "mFullGestureProcessing"));
        return proof;
    }

    private static boolean valid(Object surface) { return surface instanceof SurfaceControl && ((SurfaceControl) surface).isValid(); }
    static Object call(Object owner, String name) throws ReflectiveOperationException {
        for (Class<?> type = owner.getClass(); type != null; type = type.getSuperclass()) {
            try { Method method = type.getDeclaredMethod(name); method.setAccessible(true); return method.invoke(owner); }
            catch (NoSuchMethodException absent) { }
        }
        throw new NoSuchMethodException(name);
    }
    static Object read(Object owner, String name) throws ReflectiveOperationException {
        for (Class<?> type = owner.getClass(); type != null; type = type.getSuperclass()) {
            try { Field field = type.getDeclaredField(name); field.setAccessible(true); return field.get(owner); }
            catch (NoSuchFieldException absent) { }
        }
        throw new NoSuchFieldException(name);
    }
    private boolean sendReply(IBinder reply, String generation, IBinder client, Bundle result) {
        Parcel data = Parcel.obtain();
        try {
            data.writeInterfaceToken(DESCRIPTOR);
            data.writeString(generation);
            data.writeStrongBinder(client);
            data.writeBundle(result);
            return reply.transact(IBinder.FIRST_CALL_TRANSACTION, data, null, IBinder.FLAG_ONEWAY);
        } catch (android.os.RemoteException failure) {
            log.accept("App bar lease reply failed", failure);
            return false;
        } finally { data.recycle(); }
    }
    @Override public void close() {
        if (!closed) {
            closed = true;
            context.unregisterReceiver(receiver);
            owner.post(() -> {
                if (prepared != null) {
                    if (prepared.begun) prepared.retire();
                    else prepared = null; // Offered only: no native ownership or death recipient.
                }
                completeCloseIfIdle();
            });
        }
    }

    boolean canAwaitClose() throws ReflectiveOperationException {
        return android.os.Looper.myLooper() != owner.getLooper()
                && !Thread.holdsLock(read(read(impl, "mWmService"), "mGlobalLock"));
    }

    boolean awaitClose(long timeoutMillis) throws InterruptedException {
        return closeCompleted.await(timeoutMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    void recoverAfterRejectedReload(Runnable released) {
        Runnable recover = () -> {
            released.run();
            // Recovery is owned by the original runtime, after its exact lease
            // unlinked and acknowledged retirement. Never adopt an active lease.
        };
        synchronized (this) {
            if (closeCompleted.getCount() != 0) { afterClose = recover; return; }
        }
        owner.post(recover);
    }

    Object nativeOwner() { return impl; }

    private void completeCloseIfIdle() {
        if (!closed || prepared != null) return;
        Runnable recover;
        synchronized (this) {
            closeCompleted.countDown();
            recover = afterClose;
            afterClose = null;
        }
        if (recover != null) recover.run();
    }
}
