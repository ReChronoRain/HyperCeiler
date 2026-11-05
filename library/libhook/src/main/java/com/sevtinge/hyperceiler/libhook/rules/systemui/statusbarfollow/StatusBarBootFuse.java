/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.systemui.statusbarfollow;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemProperties;

import com.sevtinge.hyperceiler.common.log.XposedLog;

import java.nio.file.Path;

/** Android adapter for the pure boot fuse. Marker files are process-private and bounded. */
public final class StatusBarBootFuse {
    private static final String TAG = "HyperCeiler.StatusBarFollow";
    private static final String DIR = "hyperceiler_statusbar_boot_fuse";

    private StatusBarBootFuse() { }

    public static Session open(Context context, String processKey) {
        if (context == null || processKey == null || processKey.isEmpty()) return null;
        try {
            Path directory = context.getDir(DIR, Context.MODE_PRIVATE).toPath();
            Path file = directory.resolve(processKey + ".state");
            BootFuseFileStore store = new BootFuseFileStore(file);
            BootFuseController controller = new BootFuseController(
                store,
                System::currentTimeMillis,
                () -> Thread.currentThread().getId());
            return new Session(controller, store);
        } catch (Throwable failure) {
            XposedLog.w(TAG, "boot fuse storage unavailable; status-bar follow remains disabled", failure);
            return null;
        }
    }

    public static final class Session implements AutoCloseable {
        private final BootFuseController controller;
        private final BootFuseFileStore store;
        private AutoCloseable observer;
        private boolean installed;
        private boolean observerStarted;

        private Session(BootFuseController controller, BootFuseFileStore store) {
            this.controller = controller;
            this.store = store;
        }

        public BootFuseController controller() {
            return controller;
        }

        /** Returns false when boot completion cannot be observed; callers must not install. */
        public boolean startBootCompletionObserver(Context context, Runnable onReady) {
            if (context == null) return false;
            try {
                if (isBootCompleted()) {
                    if (!controller.markBootCompleted()) return false;
                    observerStarted = true;
                    startReady(onReady);
                    return true;
                }
                BroadcastReceiver receiver = new BroadcastReceiver() {
                    @Override public void onReceive(Context receiverContext, Intent intent) {
                        if (intent != null && Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())
                            && controller.markBootCompleted()) {
                            startReady(onReady);
                        }
                    }
                };
                context.registerReceiver(receiver, new IntentFilter(Intent.ACTION_BOOT_COMPLETED));
                observer = () -> {
                    try { context.unregisterReceiver(receiver); }
                    catch (Throwable ignored) { }
                };
                observerStarted = true;
                return true;
            } catch (Throwable failure) {
                XposedLog.w(TAG, "boot completion observer unavailable; status-bar follow remains disabled", failure);
                return false;
            }
        }

        public boolean hasInstalled() {
            return installed;
        }

        public boolean hasStarted() {
            return observerStarted;
        }

        public void markInstalled() {
            installed = true;
        }

        private static void startReady(Runnable onReady) {
            if (onReady == null) return;
            Thread thread = new Thread(() -> {
                try { onReady.run(); }
                catch (Throwable ignored) { }
            }, "hc-statusbar-boot-ready");
            thread.setDaemon(true);
            thread.start();
        }

        @Override public void close() {
            if (observer != null) {
                try { observer.close(); }
                catch (Throwable ignored) { }
                observer = null;
            }
            store.close();
        }
    }

    private static boolean isBootCompleted() {
        try {
            return "1".equals(SystemProperties.get("sys.boot_completed", "0"));
        } catch (Throwable ignored) {
            return false;
        }
    }
}
