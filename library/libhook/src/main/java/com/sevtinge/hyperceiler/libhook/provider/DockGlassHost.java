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

import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.GradientDrawable;
import android.hardware.display.DisplayManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;
import android.view.Choreographer;
import android.view.Display;
import android.view.SurfaceControlViewHost;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.sevtinge.hyperceiler.libhook.rules.home.dock.DockGlassGeometry;
import com.sevtinge.hyperceiler.libhook.rules.home.dock.DockGlassPreset;
import com.sevtinge.hyperceiler.libhook.rules.home.dock.DockUnlockReveal;

import org.lsposed.hiddenapibypass.HiddenApiBypass;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Own-process HWUI only. Never execute vendor RenderThread code in system_server. */
final class DockGlassHost {
    private static final String TAG = "HyperCeiler.DockGlass";
    /** Bounds how long a returning Dock may wait for a post-return capture frame. */
    private static final long FRESH_TIMEOUT_NS = 300_000_000L;
    /** One 60 Hz frame of slack between the mark and the next producer timestamp. */
    private static final long FRESH_TOLERANCE_NS = 16_000_000L;
    private final Handler main = new Handler(Looper.getMainLooper());
    // Accessed on the app main thread only. Two slots allow resize handover.
    private final HashMap<String, Entry> entries = new HashMap<>();
    private final IBinder lifetime = new Binder();
    private int dockCreates;
    // Brightness bit the live material was built from (1 = light wallpaper, 0 = dark, -1 = none).
    // Process-wide because the applied wallpaper is; see probe(). Main-thread writes, volatile for
    // the readiness call that may read it from the provider dispatch thread.
    private volatile int appliedBrightnessBit = -1;
    private String lastDockStatus = "no Dock request in this app process";
    private String lastDockRelease = "none";
    // Main-thread only. The Choreographer belongs to this process, never to system_server.
    private Choreographer revealChoreographer;
    private final Choreographer.FrameCallback revealFrame = this::onRevealFrame;

    private static final class Entry {
        final SurfaceControlViewHost host;
        final View view;
        final View backdrop;
        final IBinder owner;
        final IBinder.DeathRecipient death;
        // UI night mode from the launcher. It no longer selects the glass token (that follows
        // the wallpaper brightness now); it is kept for diagnostics and the fallback tint.
        final boolean dark;
        final int radiusPx;
        // Absolute unlock epoch from system_server; -1 when no reveal is in flight.
        long revealStartedAt = -1L;
        boolean revealActive;
        /** Sampling is paused without unregistering the texture or clearing its last frame. */
        boolean capturePaused;
        /**
         * Nano-time before which every captured frame belongs to the previous foreground. A
         * returning Dock must not present the launcher's pass-blur texture while it still holds
         * the closed app's content, which is exactly one frame of visibly wrong colour.
         */
        long freshAfterNanos;
        int captureEpoch;
        DockUnlockReveal.Style revealStyle = DockUnlockReveal.Style.DAYBREAK;
        int height;
        SurfaceControlViewHost.SurfacePackage parcel;
        Entry(SurfaceControlViewHost host, View view, View backdrop, IBinder owner,
              IBinder.DeathRecipient death, boolean dark, int radiusPx) {
            this.host = host; this.view = view; this.backdrop = backdrop;
            this.owner = owner; this.death = death; this.dark = dark;
            this.radiusPx = radiusPx;
        }
    }

    Bundle call(Context context, String method, String id, Bundle args) {
        if ("dock_glass_native_record".equals(method)) {
            checkLauncherRecorder(context);
            return DockDiagnosticJournal.access(context, args);
        }
        checkCaller(method);
        return switch (method) {
            case "dock_glass_history" -> DockDiagnosticJournal.access(context, null);
            case "dock_glass_record" -> DockDiagnosticJournal.access(context, args);
            case "dock_glass_self_test" -> selfTest(context);
            default -> callHost(context, method, id, args);
        };
    }

    private static void checkLauncherRecorder(Context context) {
        int uid = Binder.getCallingUid();
        try {
            if (uid != context.getPackageManager().getPackageUid("com.miui.home", 0)) {
                throw new SecurityException("Only the system launcher may record native status");
            }
        } catch (android.content.pm.PackageManager.NameNotFoundException error) {
            throw new SecurityException("System launcher identity is unavailable", error);
        }
    }

    private static void checkCaller(String method) {
        int uid = Binder.getCallingUid();
        boolean selfTest = "dock_glass_self_test".equals(method);
        boolean diagnostics = "dock_glass_diagnostics".equals(method);
        boolean history = "dock_glass_history".equals(method);
        if (uid != Process.SYSTEM_UID && uid != Process.myUid()
                && !((selfTest || diagnostics || history) && uid == 2000)) {
            throw new SecurityException("Only the system Dock hook may manage glass hosts");
        }
    }

    private Bundle callHost(Context context, String method, String id, Bundle args) {
        if (!"dock_glass_diagnostics".equals(method) && (id == null || id.length() > 100)) {
            throw new IllegalArgumentException("Invalid host id");
        }
        CompletableFuture<Bundle> result = new CompletableFuture<>();
        main.post(() -> dispatch(context, method, id, args, result));
        try {
            return result.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            return cancelRequest(id, result, error);
        } catch (ExecutionException | TimeoutException error) {
            return cancelRequest(id, result, error);
        }
    }

    private void dispatch(Context context, String method, String id, Bundle args, CompletableFuture<Bundle> result) {
        if (result.isDone()) return;
        try {
            switch (method) {
                case "dock_glass_diagnostics" -> result.complete(diagnostics());
                case "dock_glass_create" -> create(context, id, args, result);
                case "dock_glass_status" -> result.complete(status(id));
                case "dock_glass_probe" -> result.complete(probe(id));
                case "dock_glass_pause_capture" -> result.complete(pauseCapture(id));
                case "dock_glass_resume_capture" -> result.complete(resumeCapture(id));
                case "dock_glass_sync_geometry" -> result.complete(syncGeometry(id));
                case "dock_glass_fresh_capture" -> { markFresh(id); result.complete(Bundle.EMPTY); }
                case "dock_glass_refresh" -> result.complete(refresh(id));
                case "dock_glass_release" -> { release(id); result.complete(Bundle.EMPTY); }
                case "dock_glass_unlock" -> { startReveal(id, args); result.complete(Bundle.EMPTY); }
                default -> throw new IllegalArgumentException("Unknown glass operation");
            }
        } catch (Throwable error) {
            release(id);
            record(id, "failed: " + error.getClass().getSimpleName() + ": " + error.getMessage());
            Log.w(TAG, "Glass host unavailable; retain compositor fallback", error);
            result.complete(failure(error));
        }
    }

    private Bundle cancelRequest(String id, CompletableFuture<Bundle> result, Exception error) {
        result.cancel(false);
        main.post(() -> release(id));
        return failure(error);
    }

    private static Bundle failure(Throwable error) {
        Bundle failure = new Bundle();
        Throwable cause = error.getCause() == null ? error : error.getCause();
        failure.putString("error", cause.getClass().getSimpleName() + ": " + cause.getMessage());
        return failure;
    }

    private void create(Context context, String id, Bundle args, CompletableFuture<Bundle> result)
            throws Exception {
        if (args == null) throw new IllegalArgumentException("Missing host configuration");
        if (!id.startsWith("self-test-")) { dockCreates++; record(id, "creating"); }
        int width = args.getInt("width");
        int height = args.getInt("height");
        float radius = args.getFloat("radius");
        IBinder owner = args.getBinder("owner");
        validateHost(width, height, radius, owner);
        requireGlassSupport();
        release(id);
        if (entries.size() >= 2) throw new IllegalStateException("Dock host limit reached");
        Display display = context.getSystemService(DisplayManager.class).getDisplay(Display.DEFAULT_DISPLAY);
        if (display == null) throw new IllegalStateException("Default display unavailable");
        Context displayContext = context.createDisplayContext(display);
        View view = new View(displayContext);
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        view.setFocusable(false);
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(Color.TRANSPARENT);
        shape.setCornerRadius(radius);
        view.setBackground(shape);
        view.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline outline) {
                outline.setRoundRect(0, 0, width, height, radius);
            }
        });
        view.setClipToOutline(true);
        // The source container and glass element have different HWUI roles. A
        // single View with both container mode 1 and element mode 1 can render
        // its container blur instead of the child's refractive material.
        FrameLayout backdrop = new FrameLayout(displayContext);
        backdrop.setBackgroundColor(Color.TRANSPARENT);
        backdrop.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        backdrop.addView(view, new FrameLayout.LayoutParams(width, height));
        SurfaceControlViewHost host = new SurfaceControlViewHost(displayContext, display, new Binder());
        IBinder.DeathRecipient death = () -> main.post(() -> release(id));
        boolean dark = args.getBoolean("dark");
        Entry entry = new Entry(host, view, backdrop, owner, death, dark, (int) radius);
        entry.height = height;
        entries.put(id, entry);
        owner.linkToDeath(death, 0);
        WindowManager.LayoutParams layout = new WindowManager.LayoutParams(width, height,
                WindowManager.LayoutParams.TYPE_APPLICATION,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT);
        layout.setTitle("HyperCeiler Dock glass");
        HiddenApiBypass.invoke(SurfaceControlViewHost.class, host, "setView", backdrop, layout);
        // setView schedules attachment; configure only after the View has a ViewRootImpl.
        view.post(() -> configure(id, entry, result));
    }

    private static void validateHost(int width, int height, float radius, IBinder owner) {
        if (width < 1 || height < 1 || width > 4096 || height > 4096) {
            throw new IllegalArgumentException("Invalid host dimensions");
        }
        if (!Float.isFinite(radius) || radius < 0 || radius > Math.min(width, height) / 2f) {
            throw new IllegalArgumentException("Invalid host radius");
        }
        if (owner == null || !owner.isBinderAlive()) throw new IllegalArgumentException("Invalid host owner");
    }

    private static void requireGlassSupport() throws Exception {
        Class<?> rootClass = Class.forName("android.view.ViewRootImpl");
        if (!Boolean.TRUE.equals(HiddenApiBypass.invoke(rootClass, null, "getSupportedBionicMaterial"))
                || !Boolean.TRUE.equals(HiddenApiBypass.invoke(rootClass, null, "getSupportedMiBlur"))) {
            throw new UnsupportedOperationException("Native glass or cross-window blur is unsupported");
        }
    }

    private void configure(String id, Entry entry, CompletableFuture<Bundle> result) {
        View view = entry.view;
        View backdrop = entry.backdrop;
        if (result.isDone() || !entries.containsKey(id)) return;
        try {
            if (!view.isAttachedToWindow() || !view.isHardwareAccelerated()) {
                throw new UnsupportedOperationException("No hardware-accelerated ViewRoot");
            }
            applyMaterial(entry);
            view.getViewTreeObserver().registerFrameCommitCallback(() -> main.post(() -> commitFrame(id, entry, result)));
            view.invalidate();
        } catch (Throwable error) {
            release(id);
            record(id, "configuration failed: " + error.getClass().getSimpleName() + ": " + error.getMessage());
            result.completeExceptionally(error);
            Log.w(TAG, "Native glass configuration failed", error);
        }
    }

    /**
     * Start the 3D part of the unlock reveal for one host.
     *
     * <p>The projection runs here, in the module's own process, against this process's
     * Choreographer: a child SurfaceControl can only carry an affine matrix, so the
     * perspective has to be applied to the glass View that we own. Only the absolute
     * start time comes from system_server, so the two sides cannot drift, and a failure
     * here can never take system_server with it.
     */
    private void startReveal(String id, Bundle args) {
        Entry entry = entries.get(id);
        if (entry == null || args == null) return;
        long startedAt = args.getLong("startedAtMs");
        if (startedAt <= 0L) return;
        entry.revealStartedAt = startedAt;
        entry.revealStyle = DockUnlockReveal.Style.of(args.getString("style"));
        applyRevealPose(entry, SystemClock.uptimeMillis());
        if (entry.revealActive) choreographer().postFrameCallback(revealFrame);
        record(id, "unlock reveal 3d style="
                + entry.revealStyle.name().toLowerCase(java.util.Locale.ROOT)
                + " startedAtMs=" + startedAt);
    }

    private void onRevealFrame(long frameTimeNanos) {
        long now = SystemClock.uptimeMillis();
        boolean again = false;
        for (Entry entry : entries.values()) {
            if (!entry.revealActive) continue;
            applyRevealPose(entry, now);
            if (entry.revealActive) again = true;
        }
        if (again) choreographer().postFrameCallback(revealFrame);
    }

    private void applyRevealPose(Entry entry, long now) {
        DockUnlockReveal.Pose3D pose = DockUnlockReveal.pose3D(entry.revealStyle, entry.revealStartedAt, now);
        View view = entry.view;
        if (pose.active) {
            // A finite camera distance is what turns the rotation into a perspective:
            // the near edge grows and the far edge shrinks, like the icon row's depth step.
            view.setCameraDistance(entry.height > 0
                    ? entry.height * DockUnlockReveal.cameraHeights(entry.revealStyle)
                    : view.getResources().getDisplayMetrics().density * 1280f);
            view.setRotationX(pose.rotationX);
            view.setRotationY(pose.rotationY);
            view.setRotation(pose.rotationZ);
            view.setScaleX(pose.scaleX);
            view.setScaleY(pose.scaleY);
            view.setTranslationZ(entry.height * pose.depthHeights);
        } else {
            restoreRevealPose(view);
        }
        entry.revealActive = pose.active;
    }

    /** Reset every RenderNode property touched by a reveal before re-use or teardown. */
    private static void restoreRevealPose(View view) {
        view.setRotationX(0f);
        view.setRotationY(0f);
        view.setRotation(0f);
        view.setScaleX(1f);
        view.setScaleY(1f);
        view.setTranslationX(0f);
        view.setTranslationY(0f);
        view.setTranslationZ(0f);
        view.setAlpha(1f);
        view.setCameraDistance(view.getResources().getDisplayMetrics().density * 1280f);
    }

    private void stopReveal(Entry entry) {
        entry.revealActive = false;
        entry.revealStartedAt = -1L;
        restoreRevealPose(entry.view);
    }

    private Choreographer choreographer() {
        if (revealChoreographer == null) revealChoreographer = Choreographer.getInstance();
        return revealChoreographer;
    }

    private void commitFrame(String id, Entry entry, CompletableFuture<Bundle> result) {
        if (result.isDone() || !entries.containsKey(id)) return;
        SurfaceControlViewHost.SurfacePackage surface = entry.host.getSurfacePackage();
        if (surface == null) {
            result.completeExceptionally(new IllegalStateException("No surface package"));
            release(id);
            return;
        }
        Bundle response = new Bundle();
        response.putParcelable("surface", surface);
        response.putBinder("lifetime", lifetime);
        response.putInt("rendererPid", Process.myPid());
        if (!result.complete(response)) { surface.release(); release(id); return; }
        entry.parcel = surface;
        // ContentProvider serializes the package after call() returns; do not release it here.
        Log.i(TAG, "Native glass frame committed for Dock host " + id);
        record(id, "frame committed; awaiting background texture; pipeline=container+glassChild");
    }

    private Bundle status(String id) throws Exception {
        Bundle result = new Bundle();
        Entry entry = entries.get(id);
        long timestamp = backgroundTimestamp(entry);
        boolean active = producerActive(entry);
        boolean paused = entry != null && entry.capturePaused;
        // A windowless host never receives a relayout, so a display rotation leaves its blur
        // geometry behind. Recompute it here instead of only reporting it: the caller then
        // gets a usable answer on the same round trip.
        healGeometry(id, entry);
        boolean geometryValid = captureGeometryValid(entry);
        boolean fresh = captureFresh(entry);
        // A captured frame mapped through the rotated transform, or one that still shows the
        // previous foreground, is not a visible source either: it paints the wrong wallpaper.
        // Require matching geometry and a post-return frame, exactly like producer liveness and
        // the texture timestamp.
        boolean ready = (active || paused) && timestamp > 0 && geometryValid && fresh;
        result.putBoolean("backgroundReady", ready);
        result.putBoolean("producerActive", active);
        result.putBoolean("capturePaused", paused);
        result.putBoolean("geometryValid", entry != null && geometryValid);
        result.putBoolean("captureFresh", entry != null && fresh);
        result.putLong("textureTimestamp", timestamp);
        result.putString("captureGeometry", captureGeometry(entry));
        record(id, "backgroundReady=" + ready + ", producerActive=" + active + ", capturePaused=" + paused
                + ", textureTimestamp=" + timestamp);
        return result;
    }

    private Bundle probe(String id) throws Exception {
        Entry entry = entries.get(id);
        if (entry == null || !entry.view.isAttachedToWindow()) {
            throw new IllegalStateException("Dock glass host is not attached");
        }
        // A static wallpaper can legitimately retain the same texture timestamp.
        // Inspect the vendor producer state instead of requiring a new frame.
        long timestamp = backgroundTimestamp(entry);
        boolean active = producerActive(entry);
        // The folder token follows the wallpaper brightness, and a wallpaper change leaves the
        // producer perfectly healthy. Report "not ready" once so the launcher routes us through
        // refresh(), which re-resolves the material; otherwise the Dock keeps the old glass.
        boolean brightnessChanged =
                (lightWallpaper(entry.view.getContext()) ? 1 : 0) != appliedBrightnessBit;
        // Self-heal before reporting. The client may already be showing a ready panel, so this
        // probe is the last chance to replace a rotated-transform sample with a correct one
        // before the next frame is composited.
        healGeometry(id, entry);
        boolean geometryValid = captureGeometryValid(entry);
        boolean fresh = captureFresh(entry);
        Bundle result = new Bundle();
        result.putBoolean("backgroundReady",
                active && timestamp > 0 && !brightnessChanged && geometryValid && fresh);
        result.putBoolean("producerActive", active);
        result.putBoolean("geometryValid", geometryValid);
        result.putBoolean("captureFresh", fresh);
        result.putLong("textureTimestamp", timestamp);
        result.putString("captureGeometry", captureGeometry(entry));
        return result;
    }

    /**
     * True when the vendor's blur geometry matches the display it is sampling.
     *
     * <p>Right after the display rotates back, {@code displayRot} is already portrait while
     * {@code mConfigRot} still carries the rotated geometry, so every frame sampled in that window
     * is mapped through the wrong transform - the "sampling position changed" artefact. Reporting
     * it lets the client fall back for exactly those frames instead of a fixed timer, and show the
     * native glass again the moment the geometry catches up.
     *
     * <p>Fails open: an unknown geometry must not block a working glass.
     */
    private static boolean captureGeometryValid(Entry entry) {
        if (entry == null) return false;
        try {
            Object root = invoke(entry.backdrop, "getViewRootImpl");
            if (root == null) return true;
            Field configRotation = ownField(root.getClass(), "mConfigRot");
            if (configRotation == null) return true;
            return DockGlassGeometry.matches(
                    expectedConfigRotation(entry), configRotation.getInt(root));
        } catch (Exception unavailable) {
            return true;
        }
    }

    /**
     * The {@code mConfigRot} the Dock's blur capture needs.
     *
     * <p>The Dock layer is a fixed-size windowless surface under the launcher window: its content
     * is always drawn in the host's natural orientation, no matter how the display is rotated. The
     * vendor's own {@code checkConfigRot()} agrees ({@code wmRot: 0}), but a host that was created
     * while the display was rotated inherits {@code wmRot: 1} and - because a windowless root never
     * receives a relayout - keeps it forever, transposing the wallpaper sample even after the
     * launcher is portrait again. This returns the natural rotation so the host can correct that.
     */
    private static int expectedConfigRotation(Entry entry) {
        try {
            android.view.Display display = entry.backdrop.getDisplay();
            if (display == null) return -1;
            int installOrientation = 0;
            try {
                Object install = HiddenApiBypass.invoke(Display.class, display, "getInstallOrientation");
                if (install instanceof Number) installOrientation = ((Number) install).intValue();
            } catch (Throwable unavailable) {
                // Optional on non-Xiaomi builds: natural orientation is the safe assumption.
                installOrientation = 0;
            }
            return DockGlassGeometry.expectedConfigRotation(0, installOrientation);
        } catch (Exception unavailable) {
            return -1;
        }
    }

    /**
     * Make the vendor blur geometry match the Dock window's natural orientation, from inside the
     * host.
     *
     * <p>A windowless {@code SurfaceControlViewHost} receives no relayout, so the vendor keeps
     * whatever {@code mConfigRot} its merged configuration produced at creation time. On-device
     * logging shows a host created during a rotation starts at {@code current blurRot: 1} with
     * {@code wmRot: 1} and - for a hidden launcher - never returns to portrait on its own, so the
     * sample stays mapped through the rotated transform: the "the colour moved" artefact. The field
     * is pinned to the natural rotation and the capture buffer is sized the same way the vendor's
     * {@code checkSurTexSize()} would for it.
     *
     * <p>The vendor's own {@code checkConfigRot()} must NOT be invoked here: it recomputes the same
     * stale {@code wmRot} and would undo the correction on every probe.
     */
    private boolean healGeometry(String id, Entry entry) {
        if (entry == null || !entry.view.isAttachedToWindow()) return captureGeometryValid(entry);
        try {
            Object root = invoke(entry.backdrop, "getViewRootImpl");
            if (root == null) return true;
            Field configRotation = ownField(root.getClass(), "mConfigRot");
            if (configRotation == null) return true;
            int expected = expectedConfigRotation(entry);
            if (DockGlassGeometry.matches(expected, configRotation.getInt(root))) return true;

            int before = configRotation.getInt(root);
            configRotation.setInt(root, expected);
            resizeCapture(entry, root, expected);
            // Tell the renderer immediately; waiting for the next texture frame would present one
            // more frame mapped through the rotated transform.
            HiddenApiBypass.invoke(View.class, entry.backdrop, "setTextureAvailable",
                    true, expected, readFloatField(root, "mTexScale", 1f));
            entry.backdrop.postInvalidateOnAnimation();
            boolean valid = captureGeometryValid(entry);
            Log.i(TAG, "glass geometry pinned naturalRot=" + expected + " before=" + before
                    + " valid=" + valid + " " + captureGeometry(entry));
            record(id, "glass geometry pinned naturalRot=" + expected + " before=" + before
                    + " valid=" + valid + " " + captureGeometry(entry));
            return valid;
        } catch (Exception unavailable) {
            record(id, "glass geometry pin unavailable=" + unavailable.getClass().getSimpleName());
            return captureGeometryValid(entry);
        }
    }

    /**
     * Require the next presented frame to have been captured after this call.
     *
     * <p>The pass-blur texture keeps the foreground app's content until the compositor produces a
     * new one, so the very first returning frame shows the closed app's colours. The launcher asks
     * for this at a rotation return and keeps the compositor fallback until the producer timestamp
     * passes the mark (bounded by {@link #FRESH_TIMEOUT_NS}, so a static background can never strand
     * the panel on the fallback).
     */
    private void markFresh(String id) {
        Entry entry = entries.get(id);
        if (entry == null) return;
        entry.freshAfterNanos = System.nanoTime() - FRESH_TOLERANCE_NS;
        Log.i(TAG, "glass capture freshness required id=" + id);
        record(id, "glass capture freshness required");
    }

    /** True when no post-return frame is outstanding, or one has already arrived. */
    private static boolean captureFresh(Entry entry) {
        if (entry == null || entry.freshAfterNanos <= 0L) return true;
        long now = System.nanoTime();
        if (now - entry.freshAfterNanos > FRESH_TIMEOUT_NS) return true;
        try {
            return backgroundTimestamp(entry) >= entry.freshAfterNanos;
        } catch (Exception unavailable) {
            return true;
        }
    }

    /**
     * Re-derive the capture buffer geometry for a rotation, mirroring {@code checkSurTexSize()}
     * without letting it run {@code checkConfigRot()} again.
     */
    private static void resizeCapture(Entry entry, Object root, int configRotation) {
        try {
            Field dispRectField = ownField(root.getClass(), "mDispRect");
            Field surfaceSizeField = ownField(root.getClass(), "mSurfaceSize");
            Field textureField = ownField(root.getClass(), "mSurTex");
            if (dispRectField == null || surfaceSizeField == null || textureField == null) return;
            Object surfaceSize = surfaceSizeField.get(root);
            if (!(surfaceSize instanceof Point)) return;
            Point size = (Point) surfaceSize;
            int width = size.x;
            int height = size.y;
            if (configRotation == 1 || configRotation == 3) {
                width = size.y;
                height = size.x;
            }
            float scale = readFloatField(root, "mTexScale", 1f);
            int bufferWidth = (int) Math.ceil(width * scale);
            int bufferHeight = (int) Math.ceil(height * scale);
            Object dispRect = dispRectField.get(root);
            if (dispRect instanceof Rect) ((Rect) dispRect).set(0, 0, bufferWidth, bufferHeight);
            Object texture = textureField.get(root);
            if (texture instanceof SurfaceTexture) {
                ((SurfaceTexture) texture).setDefaultBufferSize(bufferWidth, bufferHeight);
            }
        } catch (Exception unavailable) {
            // Metadata-only correction: the pinned rotation still applies to the next frame.
        }
    }

    private static float readFloatField(Object target, String name, float fallback) {
        try {
            Field field = ownField(target.getClass(), name);
            return field == null ? fallback : field.getFloat(target);
        } catch (Exception unavailable) {
            return fallback;
        }
    }

    /** Metadata only: distinguish a region/rotation change from a material restart. */
    private static String captureGeometry(Entry entry) {
        if (entry == null) return "detached";
        try {
            Object root = invoke(entry.backdrop, "getViewRootImpl");
            if (root == null) return "no-root";
            StringBuilder value = new StringBuilder("displayRot=")
                    .append(entry.backdrop.getDisplay().getRotation());
            for (String name : new String[]{"mConfigRot", "mDispRect", "mSurfaceSize", "mTexScale"}) {
                Field field = ownField(root.getClass(), name);
                value.append(' ').append(name).append('=').append(field == null ? "unknown" : field.get(root));
            }
            return value.toString();
        } catch (Exception unavailable) {
            return "unavailable:" + unavailable.getClass().getSimpleName();
        }
    }

    private static long backgroundTimestamp(Entry entry) throws Exception {
        if (entry == null || !entry.view.isAttachedToWindow()) return 0;
        Object root = invoke(entry.view, "getViewRootImpl");
        Field field = ownField(root.getClass(), "mSurTex");
        if (field == null) return 0;
        Object texture = field.get(root);
        // Only a timestamp, never copy, retain or expose background pixels.
        return texture instanceof SurfaceTexture ? ((SurfaceTexture) texture).getTimestamp() : 0;
    }

    private static boolean producerActive(Entry entry) throws Exception {
        if (entry == null || !entry.view.isAttachedToWindow()) return false;
        Object root = invoke(entry.view, "getViewRootImpl");
        if (root == null) return false;
        Field textureField = ownField(root.getClass(), "mSurTex");
        Field stateField = ownField(root.getClass(), "mLastSfState");
        Field visibleField = ownField(root.getClass(), "mTextureVis");
        if (textureField == null || stateField == null || visibleField == null) return false;
        Object texture = textureField.get(root);
        return texture instanceof SurfaceTexture
                && !((SurfaceTexture) texture).isReleased()
                && stateField.getInt(root) == 1
                && visibleField.getBoolean(root);
    }

    private Bundle refresh(String id) throws Exception {
        Entry entry = entries.get(id);
        if (entry == null || !entry.view.isAttachedToWindow()) {
            throw new IllegalStateException("Dock glass host is not attached");
        }
        // A hidden launcher parent can leave the vendor texture flag set while its
        // producer is stopped. Restart only OUR windowless backdrop, then reapply the
        // native material because the vendor ViewRoot can discard it with the texture.
        invoke(entry.backdrop, "setPassWindowBlurEnabled", false);
        applyMaterial(entry);
        invalidateMaterial(entry);
        main.postDelayed(() -> {
            if (entries.get(id) == entry) invalidateMaterial(entry);
        }, 120);
        Bundle result = new Bundle();
        result.putBoolean("refreshed", true);
        return result;
    }

    private Bundle pauseCapture(String id) throws Exception {
        Entry entry = entries.get(id);
        Bundle result = new Bundle();
        if (entry == null || !entry.view.isAttachedToWindow()) return result;
        // Freeze the capture while KEEPING the host drawable: the vendor's updateTextureState(false)
        // selects SF's "undraw" state, which leaves the panel with nothing to composite and forces
        // the compositor fallback for the whole frozen window. setPassWindowBlurEnabled(false) is
        // worse still - it releases mSurTex (and the retained frame with it).
        try {
            boolean drawn = setCapturePaused(entry, true);
            boolean retained = backgroundTimestamp(entry) > 0;
            result.putBoolean("capturePaused", true);
            result.putBoolean("frozenDrawn", drawn);
            result.putBoolean("retained", retained);
            result.putString("captureGeometry", captureGeometry(entry));
            Log.i(TAG, "glass capture frozen id=" + id + " drawn=" + drawn + " retained=" + retained);
            record(id, "capture frozen; drawn=" + drawn + " retained=" + retained);
        } catch (Exception unsupported) {
            // An optional pause API must not destroy a working host. Rotation will rebuild
            // normally if the client cannot confirm that a ready texture was retained.
            result.putString("pauseError", unsupported.getClass().getSimpleName());
        }
        return result;
    }

    private Bundle resumeCapture(String id) throws Exception {
        Entry entry = entries.get(id);
        if (entry == null || !entry.view.isAttachedToWindow()) {
            throw new IllegalStateException("Dock glass host is not attached");
        }
        setCapturePaused(entry, false);
        invalidateMaterial(entry);
        int epoch = entry.captureEpoch;
        // Bounded metadata observations after the first returning frames. A hide or another
        // resume invalidates them; there is no background polling or pixel capture.
        for (long delay : new long[]{80L, 240L}) main.postDelayed(() -> {
            if (entries.get(id) != entry || entry.capturePaused || entry.captureEpoch != epoch) return;
            try {
                Log.i(TAG, "glass capture sample id=" + id + " afterMs=" + delay
                        + " timestamp=" + backgroundTimestamp(entry) + " " + captureGeometry(entry));
            } catch (Exception unavailable) {
                Log.i(TAG, "glass capture sample metadata unavailable=" + unavailable.getClass().getSimpleName());
            }
        }, delay);
        return probe(id);
    }

    /**
     * Explicit geometry sync requested by the launcher side.
     *
     * <p>Same work as the self-heal that {@link #status} and {@link #probe} already perform, kept
     * as its own operation so the client can drive the recompute for the exact frame it is about to
     * present instead of waiting for its next readiness poll.
     */
    private Bundle syncGeometry(String id) throws Exception {
        Entry entry = entries.get(id);
        Bundle result = new Bundle();
        if (entry == null || !entry.view.isAttachedToWindow()) return result;
        result.putBoolean("geometryValid", healGeometry(id, entry));
        result.putString("captureGeometry", captureGeometry(entry));
        return result;
    }

    /**
     * Freeze or thaw the pass-blur capture without selecting the vendor's undraw state.
     *
     * <p>{@code updateTextureState(view, false)} sets the host to SF's "undraw" state (2): the whole
     * window stops being composited, so a frozen Dock would have to fall back to the compositor
     * blur. The vendor's own state machine separates the two concerns - the draw/undraw state is
     * {@code mLastSfState} and "keep feeding the pass-blur surface" is the {@code mTextureVis} flag
     * passed to {@code setUpdateTextureFlag}. This stops the feeding while leaving the last home
     * sample composited, which is what a returning Dock needs.
     *
     * @return true when the drawable freeze was applied, false when the vendor pause was used
     */
    private static boolean setCapturePaused(Entry entry, boolean paused) throws Exception {
        Object root = invoke(entry.backdrop, "getViewRootImpl");
        if (root == null) throw new IllegalStateException("No glass ViewRoot");
        Field visibleField = ownField(root.getClass(), "mTextureVis");
        Field stateField = ownField(root.getClass(), "mLastSfState");
        boolean drawn = visibleField != null && stateField != null;
        if (drawn) {
            visibleField.setBoolean(root, !paused);
            // Force sendSfState to write the flag even though the draw state does not change.
            stateField.setInt(root, 2);
            HiddenApiBypass.invoke(root.getClass(), root, "sendSfState", 1);
        } else {
            HiddenApiBypass.invoke(root.getClass(), root, "updateTextureState", entry.backdrop, !paused);
        }
        entry.capturePaused = paused;
        entry.captureEpoch++;
        return drawn;
    }

    private void applyMaterial(Entry entry) throws Exception {
        View view = entry.view;
        View backdrop = entry.backdrop;
        if (!enableOwnBackground(backdrop)) {
            throw new UnsupportedOperationException("Cross-window background was rejected");
        }
        invoke(backdrop, "setMiBackgroundBlurMode", 1);
        invoke(backdrop, "setMiBackgroundBlurRadius", 120);
        invoke(backdrop, "setMiViewBlurMode", 0);
        invoke(view, "setMiBackgroundBlurMode", 0);
        invoke(view, "setMiViewBlurMode", 1);
        invoke(backdrop, "setMiGlassBlurRadius", DockGlassPreset.SMALL_BLUR_RADIUS,
                DockGlassPreset.BIG_BLUR_RADIUS);
        try {
            invoke(view, "setMiBackgroundBlurEnhanceFlag", DockGlassPreset.GLASS_ENHANCE_FLAG,
                    DockGlassPreset.GLASS_ENHANCE_FLAG);
            view.setClipToOutline(false);
        } catch (Exception unsupported) {
            Log.i(TAG, "Glass clip enhancement unavailable; rounded outline retained");
        }
        invoke(view, "setMiViewMaterialType", DockGlassPreset.MATERIAL_TYPE);
        // The launcher keys its folder-glass token off the wallpaper, not off the UI night
        // mode, so the material is resolved here on every (re)apply instead of being frozen
        // into the Entry at creation time. `entry.dark` still drives the compositor fallback.
        boolean light = lightWallpaper(view.getContext());
        appliedBrightnessBit = light ? 1 : 0;
        invoke(view, "setMiGlass", (Object) DockGlassPreset.parameters(light));
        // Reapplying the material must not leave a frozen capture behind: thaw it through the same
        // primitive so the vendor's update flag and this entry stay in sync.
        if (entry.capturePaused) setCapturePaused(entry, false);
        else entry.captureEpoch++;
    }

    /**
     * FolderBlurUtils.buildFolderGlass picks Medium_Thin_High when the applied wallpaper
     * supports dark text and Medium_Thin_Low otherwise. Mirror that exact rule so the Dock
     * keeps the native folder icon's glass instead of the control-center card token.
     *
     * The hint is read from the system wallpaper on the host's own main thread; it is never
     * cached, so a later refresh() (fired by the launcher's wallpaper command) re-resolves it.
     */
    private static boolean lightWallpaper(Context context) {
        try {
            WallpaperManager manager = context.getSystemService(WallpaperManager.class);
            if (manager == null) return false;
            WallpaperColors colors = manager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM);
            // Low bit of the colour hints == HINT_SUPPORTS_DARK_TEXT, the launcher's own test.
            return colors != null && (colors.getColorHints() & 1) != 0;
        } catch (Throwable error) {
            // No colour hints (or no permission): fall back to the low-light folder glass.
            Log.i(TAG, "Wallpaper colour hints unavailable; keeping the low-light folder glass");
            return false;
        }
    }

    private static void invalidateMaterial(Entry entry) {
        entry.backdrop.postInvalidateOnAnimation();
        entry.view.postInvalidateOnAnimation();
    }

    private Bundle diagnostics() throws Exception {
        Bundle result = new Bundle();
        result.putInt("pid", Process.myPid());
        result.putInt("dockCreateRequests", dockCreates);
        result.putInt("activeHosts", entries.size());
        result.putString("lastDockRelease", lastDockRelease);
        for (String id : entries.keySet()) {
            if (!id.startsWith("self-test-")) result.putBundle("host_" + id, status(id));
        }
        result.putString("lastDockStatus", lastDockStatus);
        result.putString("pipeline", "container+glassChild");
        return result;
    }

    private void record(String id, String event) {
        if (id != null && !id.startsWith("self-test-")) lastDockStatus = id + ": " + event;
    }

    /** Fixed, unparented test surface: no screenshot, overlay, host window or caller-supplied target. */
    private Bundle selfTest(Context context) {
        String id = "self-test-" + java.util.UUID.randomUUID();
        Bundle config = new Bundle();
        config.putInt("width", 320); config.putInt("height", 120);
        config.putFloat("radius", 30); config.putBinder("owner", new Binder());
        CompletableFuture<Bundle> result = new CompletableFuture<>();
        main.post(() -> {
            if (result.isDone()) return;
            try { create(context, id, config, result); }
            catch (Throwable error) { result.completeExceptionally(error); }
        });
        Bundle response = new Bundle();
        try {
            Bundle rendered = result.get(5, TimeUnit.SECONDS);
            response.putBoolean("frameCommitted", rendered.containsKey("surface"));
            response.putString("note", "Unparented HWUI smoke test only; desktop background not verified");
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            result.cancel(false);
            response.putAll(failure(error));
        } catch (ExecutionException | TimeoutException error) {
            result.cancel(false);
            response.putAll(failure(error));
        } finally {
            main.post(() -> release(id));
        }
        return response;
    }

    private static Object invoke(View view, String name, Object... args) throws Exception {
        return HiddenApiBypass.invoke(View.class, view, name, args);
    }

    private static boolean enableOwnBackground(View view) throws Exception {
        if (Boolean.TRUE.equals(invoke(view, "setPassWindowBlurEnabled", true))) return true;
        // A false return can also mean the requested state is already set.
        Field enabledField = ownField(View.class, "mNeedPassWindowBlur");
        if (enabledField != null && enabledField.getBoolean(view)) return true;
        Object root = invoke(view, "getViewRootImpl");
        if (root == null) return false;
        return allowOwnBackground(view, root);
    }

    private static boolean allowOwnBackground(View view, Object root) throws Exception {
        Field field = ownField(root.getClass(), "mPassWindowBlurFilterData");
        if (field != null) {
            Object original = field.get(root);
            if (!(original instanceof String)) return false;
            String ownPackage = view.getContext().getPackageName();
            if (!"com.sevtinge.hyperceiler".equals(ownPackage)) return false;
            // Instance field of OUR windowless ViewRoot only. Never alter static flags,
            // global settings, system properties, or another window's package/filter.
            field.set(root, original + "," + ownPackage);
            boolean enabled = Boolean.TRUE.equals(invoke(view, "setPassWindowBlurEnabled", true));
            if (!enabled) field.set(root, original);
            return enabled;
        }
        return false;
    }

    // Vendor-only instance fields on HyperCeiler's own View/ViewRoot. Public API
    // has no texture-readiness/filter accessor; do not apply this to host windows.
    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    private static Field ownField(Class<?> type, String name) {
        for (Field field : HiddenApiBypass.getInstanceFields(type)) {
            if (field.getName().equals(name)) {
                field.setAccessible(true);
                return field;
            }
        }
        return null;
    }

    private void release(String id) {
        Entry entry = entries.remove(id);
        if (entry == null) return;
        // A host torn down mid-reveal must not keep its tilt: the frame loop stops as soon
        // as no entry is active, and a recreated host starts from a flat view.
        stopReveal(entry);
        if (!id.startsWith("self-test-")) lastDockRelease = id;
        try { entry.owner.unlinkToDeath(entry.death, 0); } catch (RuntimeException ignored) {}
        try { invoke(entry.backdrop, "setPassWindowBlurEnabled", false); }
        catch (Throwable ignored) { /* Releasing the root also clears its texture. */ }
        entry.host.release();
        if (entry.parcel != null) entry.parcel.release();
    }
}
