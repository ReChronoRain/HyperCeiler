/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.provider;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.os.SystemProperties;
import android.view.Display;
import android.view.SurfaceControl;
import android.view.SurfaceControlViewHost;
import android.view.WindowManager;

import com.sevtinge.hyperceiler.common.log.ModuleLog;
import com.sevtinge.hyperceiler.libhook.rules.home.dock.XiaoheiziCapsuleView;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Host for the Xiaoheizi performance layer, kept strictly separate from {@link DockGlassHost}.
 *
 * <p>Same process boundary as the Dock glass: the HWUI View lives here, in the module's own
 * process, and the caller only receives a {@code SurfacePackage} to reparent. Nothing in this class
 * runs inside system_server, and nothing here touches the glass path, so a failure in the
 * performance layer cannot degrade an already shipped Dock feature.
 *
 * <p>The layer is deliberately short-lived: it exists only while one performance runs and is
 * released immediately afterwards. A missing or refused host is a no-op, never an error the caller
 * has to handle.
 */
final class XiaoheiziLayerHost {
    private static final String TAG = "HyperCeiler.Xiaoheizi";
    /** Generous but bounded: a stuck main thread must not pin a 5 s provider call. */
    private static final long CALL_TIMEOUT_MS = 5_000L;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final HashMap<String, Entry> entries = new HashMap<>();
    private final IBinder lifetime = new Binder();

    private static final class Entry {
        final SurfaceControlViewHost host;
        final XiaoheiziCapsuleView view;
        final IBinder owner;
        final IBinder.DeathRecipient death;
        final int capsuleHeight;
        SurfaceControlViewHost.SurfacePackage parcel;
        Entry(SurfaceControlViewHost host, XiaoheiziCapsuleView view, IBinder owner,
              IBinder.DeathRecipient death, int capsuleHeight) {
            this.host = host;
            this.view = view;
            this.owner = owner;
            this.death = death;
            this.capsuleHeight = capsuleHeight;
        }
    }

    Bundle call(Context context, String method, String id, Bundle args) {
        checkCaller(context,method,id);
        CompletableFuture<Bundle> result = new CompletableFuture<>();
        main.post(() -> dispatch(context, method, id, args == null ? Bundle.EMPTY : args, result));
        try {
            return result.get(CALL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            main.post(() -> release(id));
            return failure(error);
        } catch (ExecutionException | TimeoutException error) {
            main.post(() -> release(id));
            return failure(error);
        }
    }

    private static void checkCaller(Context context, String method, String id) {
        int uid = Binder.getCallingUid();
        boolean nativeRequest = method.startsWith("xiaoheizi_native_");
        if (!nativeRequest) {
            if (id != null && !id.startsWith("xc-native-")
                    && (uid == Process.SYSTEM_UID || uid == Process.myUid())) return;
            throw new SecurityException("System hook required");
        }
        if (!method.equals("xiaoheizi_native_release")
                && !"app-capsule-probe".equals(SystemProperties.get("debug.hyperceiler.home_glass_backend")))
            throw new SecurityException("Capsule probe is disabled");
        String prefix = "xc-native-" + Binder.getCallingPid() + "-";
        try {
            if (uid == context.getPackageManager().getPackageUid("com.miui.home", 0)
                    && id != null && id.startsWith(prefix) && id.length() <= prefix.length() + 20
                    && id.length() > prefix.length()
                    && id.substring(prefix.length()).chars().allMatch(c -> c >= '0' && c <= '9')) return;
        } catch (PackageManager.NameNotFoundException unavailable) {
            throw new SecurityException("Launcher unavailable", unavailable);
        }
        throw new SecurityException("Launcher capsule owner required");
    }

    private static Bundle failure(Throwable error) {
        Throwable cause = error.getCause() == null ? error : error.getCause();
        Bundle bundle = new Bundle();
        bundle.putString("error", cause.getClass().getSimpleName() + ": " + cause.getMessage());
        return bundle;
    }

    private void dispatch(Context context, String method, String id, Bundle args,
                          CompletableFuture<Bundle> result) {
        if (result.isDone()) return;
        try {
            switch (method) {
                case "xiaoheizi_create", "xiaoheizi_native_create" -> create(context, id, args, result);
                case "xiaoheizi_play", "xiaoheizi_native_play" -> result.complete(play(id));
                case "xiaoheizi_release", "xiaoheizi_native_release" -> { release(id); result.complete(Bundle.EMPTY); }
                default -> throw new IllegalArgumentException("Unknown performance operation");
            }
        } catch (Throwable error) {
            release(id);
            ModuleLog.w(TAG, "Performance layer unavailable", error);
            result.complete(failure(error));
        }
    }

    private void create(Context context, String id, Bundle args, CompletableFuture<Bundle> result)
            throws Exception {
        int width = args.getInt("width");
        int capsuleHeight = args.getInt("capsuleHeight");
        IBinder owner = args.getBinder("owner");
        if (width < 1 || width > 4096 || capsuleHeight < 1 || capsuleHeight > 1024) {
            throw new IllegalArgumentException("Invalid performance layer size");
        }
        if (owner == null || !owner.isBinderAlive()) {
            throw new IllegalArgumentException("Invalid host owner");
        }
        release(id);
        if (entries.size() >= 2) throw new IllegalStateException("Performance host limit reached");

        Display display = context.getSystemService(DisplayManager.class)
                .getDisplay(Display.DEFAULT_DISPLAY);
        if (display == null) throw new IllegalStateException("Default display unavailable");
        Context displayContext = context.createDisplayContext(display);

        int height = XiaoheiziCapsuleView.hostHeight(capsuleHeight);
        XiaoheiziCapsuleView view = new XiaoheiziCapsuleView(displayContext, args.getBoolean("rage") && !id.startsWith("xc-native-"));
        SurfaceControlViewHost host = new SurfaceControlViewHost(displayContext, display, new Binder());
        IBinder.DeathRecipient death = () -> main.post(() -> release(id));
        Entry entry = new Entry(host, view, owner, death, capsuleHeight);
        entries.put(id, entry);
        owner.linkToDeath(death, 0);

        WindowManager.LayoutParams layout = new WindowManager.LayoutParams(width, height,
                WindowManager.LayoutParams.TYPE_APPLICATION,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT);
        layout.setTitle("HyperCeiler Xiaoheizi layer");
        org.lsposed.hiddenapibypass.HiddenApiBypass.invoke(
                SurfaceControlViewHost.class, host, "setView", view, layout);
        view.post(() -> commitFrame(id, entry, result, 0));
    }

    /**
     * Wait for a drawable, hardware-accelerated ViewRoot before handing out the SurfacePackage.
     *
     * <p>{@code setView} attaches asynchronously, so the first post can legitimately run before the
     * ViewRoot exists. A bounded retry covers that window; the caller's own Binder timeout is the
     * final backstop, so this can never spin forever.
     */
    private void commitFrame(String id, Entry entry, CompletableFuture<Bundle> result, int attempt) {
        if (result.isDone() || !entries.containsKey(id)) return;
        try {
            if (!entry.view.isAttachedToWindow() || !entry.view.isHardwareAccelerated()) {
                if (attempt >= 8) {
                    throw new UnsupportedOperationException("No hardware-accelerated ViewRoot");
                }
                entry.view.postDelayed(() -> commitFrame(id, entry, result, attempt + 1), 16L);
                return;
            }
            SurfaceControlViewHost.SurfacePackage surface = entry.host.getSurfacePackage();
            if (surface == null) throw new IllegalStateException("No surface package");
            entry.parcel = surface;
            Bundle response = new Bundle();
            if (id.startsWith("xc-native-")) {
                Object control = org.lsposed.hiddenapibypass.HiddenApiBypass.invoke(
                        SurfaceControlViewHost.SurfacePackage.class, surface, "getSurfaceControl");
                if (!(control instanceof SurfaceControl nativeSurface) || !nativeSurface.isValid())
                    throw new IllegalStateException("No valid capsule SurfaceControl");
                response.putParcelable("surfaceControl", nativeSurface);
                response.putBoolean("surfaceReady", true);
            } else response.putParcelable("surface", surface);
            response.putBinder("lifetime", lifetime);
            response.putInt("capsuleInset",
                    XiaoheiziCapsuleView.capsuleTopInset(entry.capsuleHeight));
            if (!result.complete(response)) {
                release(id);
                return;
            }
            ModuleLog.i(TAG, "Performance layer surface ready for host " + id);
        } catch (Throwable error) {
            release(id);
            result.completeExceptionally(error);
            ModuleLog.w(TAG, "Performance layer configuration failed", error);
        }
    }

    private Bundle play(String id) {
        Entry entry = entries.get(id);
        if (entry == null) throw new IllegalStateException("Performance layer is not attached");
        // The layer is only meaningful while it is drawing; the caller hides it on completion.
        entry.view.play(() -> ModuleLog.i(TAG, "Capsule animation finished for host " + id));
        Bundle bundle = new Bundle();
        bundle.putBoolean("playing", true);
        if (entry.view.isRage()) bundle.putBoolean("continuous", true);
        else bundle.putInt("durationMs", XiaoheiziCapsuleView.PLAYBACK_DURATION_MS);
        return bundle;
    }

    private void release(String id) {
        Entry entry = entries.remove(id);
        if (entry == null) return;
        entry.view.stop();
        try { entry.owner.unlinkToDeath(entry.death, 0); } catch (RuntimeException ignored) {}
        entry.host.release();
        if (entry.parcel != null) entry.parcel.release();
    }

    void close() {
        main.post(() -> {
            for (String id : new java.util.ArrayList<>(entries.keySet())) {
                try { release(id); }
                catch (RuntimeException error) { ModuleLog.w(TAG, "Performance retirement failed", error); }
            }
        });
    }
}
