/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.home.other;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.Process;
import androidx.annotation.Keep;
import com.sevtinge.hyperceiler.common.log.ModuleLog;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.rules.home.dock.HomeLayoutNativeEndpointOS4;
import com.sevtinge.hyperceiler.libhook.rules.home.dock.HomeLayoutWire;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/** Process-owned configuration publisher. Native hooks reject APK hot reload until process restart. */
@Keep
public final class HomeLayoutLocalRuntime {
    private static final String TAG = "HyperCeiler.HomeLocal";
    private static final Uri PREFS = Uri.parse("content://com.sevtinge.hyperceiler.provider.sharedprefs");
    private static final long[] RETRY_MS = {500, 2_000, 10_000, 30_000};
    private static final AtomicBoolean registered = new AtomicBoolean();
    private static final AtomicBoolean attached = new AtomicBoolean();
    private static native void nativeBegin();
    private static native boolean nativePublish(Parcel snapshot, long revision);

    public static void start() {
        if (!registered.compareAndSet(false, true)) return;
        BaseHook.runOnApplicationAttach(context -> {
            if (!"com.miui.home".equals(context.getPackageName())
                || !"com.miui.home".equals(android.app.Application.getProcessName())
                || !attached.compareAndSet(false, true)) return;
            Context app = context.getApplicationContext();
            new HomeLayoutLocalRuntime(app == null ? context : app).startWorker();
        });
    }

    private final Context context;
    private final Handler worker;
    private final AtomicLong notifications = new AtomicLong();
    private final Runnable refresh = this::refresh;
    private volatile boolean interactive;
    private long sequence;
    private int retry;
    private boolean published;
    private String lastFailure;

    private HomeLayoutLocalRuntime(Context context) {
        this.context = context;
        HandlerThread thread = new HandlerThread("hc-home-local-config", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        worker = new Handler(thread.getLooper());
    }

    private void startWorker() {
        worker.post(() -> {
            try {
                PowerManager power = context.getSystemService(PowerManager.class);
                interactive = power == null || power.isInteractive();
                ContentObserver observer = new ContentObserver(null) {
                    @Override public void onChange(boolean selfChange, Uri uri) {
                        notifications.incrementAndGet();
                        worker.post(() -> schedule(50));
                    }
                };
                context.getContentResolver().registerContentObserver(PREFS, true, observer);
                IntentFilter filter = new IntentFilter();
                filter.addAction(Intent.ACTION_SCREEN_ON);
                filter.addAction(Intent.ACTION_SCREEN_OFF);
                filter.addAction(Intent.ACTION_USER_UNLOCKED);
                filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
                context.registerReceiver(new BroadcastReceiver() {
                    @Override public void onReceive(Context ignored, Intent intent) {
                        if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                            interactive = false;
                            worker.removeCallbacks(refresh);
                        } else {
                            if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) interactive = true;
                            notifications.incrementAndGet();
                            schedule(0);
                        }
                    }
                }, filter, null, worker, Context.RECEIVER_NOT_EXPORTED);
                // Claim ownership only after all event sources are ready, on this worker,
                // never from Application.attach or a Dart rendering callback.
                nativeBegin();
                schedule(0);
            } catch (RuntimeException | LinkageError failure) {
                report(failure);
            }
        });
    }

    private void schedule(long delay) {
        worker.removeCallbacks(refresh);
        if (interactive) worker.postDelayed(refresh, delay);
    }

    private void refresh() {
        if (!interactive) return;
        long observed = notifications.get();
        try {
            var snapshot = HomeLayoutNativeEndpointOS4.readLocalSnapshot(context);
            if (snapshot == null) throw new IllegalStateException("complete Provider snapshot unavailable");
            // A notification received during the query invalidates that result before JNI publication.
            if (observed != notifications.get()) { schedule(50); return; }
            Parcel packet = Parcel.obtain();
            try {
                HomeLayoutWire.write(packet, snapshot.layout(), snapshot.gestures(), Process.myPid());
                packet.setDataPosition(0);
                if (!nativePublish(packet, ++sequence))
                    throw new IllegalStateException("native complete-snapshot validation rejected publication");
            } finally {
                packet.recycle();
            }
            retry = 0;
            lastFailure = null;
            if (!published) ModuleLog.i(TAG, "launcher-local config active pid=" + Process.myPid());
            published = true;
        } catch (RuntimeException | LinkageError failure) {
            report(failure);
            long delay = RETRY_MS[retry];
            retry = Math.min(retry + 1, RETRY_MS.length - 1);
            schedule(delay);
        }
    }

    private void report(Throwable failure) {
        String reason = failure.getClass().getSimpleName() + ": " + failure.getMessage();
        if (!reason.equals(lastFailure)) {
            lastFailure = reason;
            ModuleLog.w(TAG, "local config unavailable, keeping complete cached values: " + reason);
        }
    }
}
