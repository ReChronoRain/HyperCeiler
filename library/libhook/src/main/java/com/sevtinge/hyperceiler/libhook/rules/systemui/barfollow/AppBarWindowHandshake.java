/*
 * Derived from MiuiBackGestureHook status-bar-follow implementation.
 * Source: https://github.com/wxxsfxyzm/MiuiBackGestureHook
 * Reference commit: 004bc059e5821775917b771d89aa1d8c18a1e669 (includes b6e582f and later fixes)
 * Copyright (C) 2026 MiuiBackGestureHook contributors.
 * Licensed under the Apache License, Version 2.0; modified for HyperCeiler.
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow;

import android.app.BroadcastOptions;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

/** Authenticated window lease; release waits for the server's committed restoration. */
final class AppBarWindowHandshake implements AutoCloseable {
    private static final String ACTION = "dev.codex.miuibackgesturehook.APP_BAR_WINDOW_PROOF";
    private final Handler main = new Handler(Looper.getMainLooper());
    private final String generation = UUID.randomUUID().toString();
    private final AtomicBoolean finished = new AtomicBoolean();
    private final BiConsumer<Bundle, Throwable> completion;
    private final IBinder window;
    private IBinder lease;
    private Bundle prepared;
    private boolean retiring;
    private final Runnable armed;
    private final Runnable active;
    private Throwable activationFailure;
    private final Runnable timeout = () -> finish(null, new IllegalStateException("WMS window proof timed out"));

    AppBarWindowHandshake(Context context, IBinder window, Runnable armed, Runnable active,
            BiConsumer<Bundle, Throwable> completion) {
        this.armed = armed;
        this.active = active;
        this.completion = completion;
        this.window = window;
        if (Looper.myLooper() != Looper.getMainLooper() || window == null)
            throw new IllegalStateException("Window proof requires an attached Main-owned root");
        Binder reply = new Binder() {
            @Override protected boolean onTransact(int code, Parcel data, Parcel unused, int flags) {
                if (code != IBinder.FIRST_CALL_TRANSACTION || Binder.getCallingUid() != 1000) return false;
                data.enforceInterface(ACTION + ".Reply");
                String received = data.readString();
                IBinder client = data.readStrongBinder();
                Bundle proof = data.readBundle();
                data.enforceNoDataAvail();
                if (!generation.equals(received) || !window.equals(client) || proof == null) return false;
                main.post(() -> receive(proof));
                return true;
            }
        };
        Bundle extras = new Bundle();
        extras.putBinder("window", window);
        extras.putBinder("reply", reply);
        extras.putString("generation", generation);
        main.postDelayed(timeout, 3000); // Diagnostic timeout only; never marks animation ready.
        try {
            context.sendBroadcast(new Intent(ACTION).setPackage("android").putExtras(extras), null,
                    BroadcastOptions.makeBasic().setShareIdentityEnabled(true).toBundle());
        } catch (RuntimeException failure) { close(); throw failure; }
    }

    private void receive(Bundle proof) {
        if (finished.get()) return;
        String phase = proof.getString("phase");
        if ("offered".equals(phase)) {
            if (lease != null || !proof.getBoolean("windowOwned") || !proof.getBoolean("drawn")) return;
            lease = proof.getBinder("lease");
            if (lease == null) { finish(null, new IllegalStateException("Missing WMS lease")); return; }
            prepared = proof;
            main.removeCallbacks(timeout);
            sendLeaseCommand(IBinder.FIRST_CALL_TRANSACTION + 1);
        } else if ("prepared".equals(phase) && lease != null) {
            if (retiring) return;
            sendLeaseCommand(IBinder.FIRST_CALL_TRANSACTION + 2);
        } else if ("armed".equals(phase) && lease != null) {
            if (retiring) return;
            prepared.putBoolean("nativeRestorerReady", proof.getBoolean("nativeRestorerReady"));
            try {
                if (!proof.getBoolean("nativeRestorerReady")) throw new IllegalStateException("Native restorer not ready");
                armed.run();
                sendLeaseCommand(IBinder.FIRST_CALL_TRANSACTION + 3);
            } catch (RuntimeException failure) {
                activationFailure = failure;
                requestRetirement();
            }
        } else if ("active".equals(phase) && lease != null && !retiring) {
            active.run();
        } else if ("retired".equals(phase) && lease != null && proof.getBoolean("retirementCommitted")) {
            prepared.putBoolean("retirementCommitted", true);
            prepared.remove("lease");
            finish(prepared, activationFailure);
        } else if ("rejected".equals(phase) || lease == null) {
            finish(null, new IllegalStateException("WMS rejected prepared-window lease: " + proof));
        }
    }

    void requestRetirement() {
        if (lease == null || retiring || finished.get()) return;
        sendLeaseCommand(IBinder.FIRST_CALL_TRANSACTION);
        retiring = true;
    }

    boolean awaitingRetirement() { return retiring && !finished.get(); }

    private void sendLeaseCommand(int code) {
        Parcel data = Parcel.obtain();
        try {
            data.writeInterfaceToken(ACTION + ".Reply.Lease");
            data.writeString(generation);
            data.writeStrongBinder(window);
            if (!lease.transact(code, data, null, IBinder.FLAG_ONEWAY))
                throw new IllegalStateException("WMS lease rejected retirement");
        } catch (android.os.RemoteException failure) {
            // Keep the window until a proven retirement; timeout cannot stand in for it.
            throw new IllegalStateException("WMS retirement unavailable", failure);
        } finally { data.recycle(); }
    }

    private void finish(Bundle result, Throwable failure) {
        if (!finished.compareAndSet(false, true)) return;
        main.removeCallbacks(timeout);
        completion.accept(result, failure);
    }
    @Override public void close() {
        if (lease != null && !finished.get()) {
            requestRetirement();
            throw new IllegalStateException("Awaiting WMS committed window retirement");
        }
        finished.set(true);
        main.removeCallbacks(timeout);
    }
}
