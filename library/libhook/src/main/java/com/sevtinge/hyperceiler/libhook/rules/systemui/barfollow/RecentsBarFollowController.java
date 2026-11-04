/*
 * Derived from MiuiBackGestureHook status-bar-follow implementation.
 * Source: https://github.com/wxxsfxyzm/MiuiBackGestureHook
 * Reference commit: 004bc059e5821775917b771d89aa1d8c18a1e669 (includes b6e582f and later fixes)
 * Copyright (C) 2026 MiuiBackGestureHook contributors.
 * Licensed under the Apache License, Version 2.0; modified for HyperCeiler.
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.view.AttachedSurfaceControl;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.View;
import android.view.WindowManager;
import android.window.TransitionInfo;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

/**
 * A short-lived rendering of SystemUI's own bars, parented below the departing Task.
 * No app pixels are captured, no input window is added, and no native leash is transformed.
 * Main owns Views/Animator; the Shell executor owns only our SurfaceControl children.
 */
final class RecentsBarFollowController {
    private static final long CAPTURE_MAX_AGE_MS = 500;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final BiConsumer<String, Throwable> log;
    private final Object lock = new Object();
    private Request pending;
    private Session active;
    private long epoch;
    private boolean detached;
    private volatile ClassLoader folmeLoader;
    private volatile Runnable idleCallback;

    void onIdle(Runnable callback) {
        idleCallback = callback;
    }

    boolean idle() {
        synchronized (lock) {
            return active == null && pending == null;
        }
    }

    void cancelForPreference() {
        Session session;
        synchronized (lock) {
            pending = null;
            ++epoch;
            session = active;
        }
        if (session != null) cancelFromMain(session);
    }

    private void released(Session session) {
        synchronized (lock) {
            if (active == session) active = null;
        }
        Runnable callback = idleCallback;
        if (callback != null) callback.run();
    }

    void configureFolme(ClassLoader loader) {
        folmeLoader = loader;
    }

    RecentsBarFollowController(BiConsumer<String, Throwable> log) {
        this.log = log;
    }

    static final class Request {
        final Object owner;
        final Executor shell;
        final long epoch;
        final long time = SystemClock.uptimeMillis();
        IBinder token;
        Capture capture;
        int gestureState = -1;

        Request(Object owner, Executor shell, long epoch) {
            this.owner = owner;
            this.shell = shell;
            this.epoch = epoch;
        }
    }

    static final class Session {
        final Request request;
        final Object controller;
        final ArrayList<SurfaceControl> layers = new ArrayList<>();
        final ArrayList<Bar> bars;
        volatile boolean ended;
        volatile boolean finishing;
        boolean started; // Main only.
        XiaomiBarFolme folme; // Main only; callbacks are identity-bound to this session.
        boolean fadeComplete;
        volatile boolean returningToApp;
        int nativeFrames;
        volatile SurfaceControl.Transaction returnParents;
        final ArrayList<AttachedSurfaceControl> returnRoots = new ArrayList<>();

        Session(Request request, Object controller) {
            this.request = request;
            this.controller = controller;
            bars = request.capture.bars;
        }
    }

    private static final class Capture {
        final ArrayList<Bar> bars;
        final Rect display;

        Capture(ArrayList<Bar> bars, Rect display) {
            this.bars = bars;
            this.display = display;
        }
    }

    private static final class Bar {
        final WeakReference<View> view;
        final Bitmap bitmap;
        final Rect bounds;
        final int type;
        float writtenAlpha = 1f; // Main only. Restore only while we still own this value.
        boolean changed;

        Bar(View view, Bitmap bitmap, Rect bounds, int type) {
            this.view = new WeakReference<>(view);
            this.bitmap = bitmap;
            this.bounds = bounds;
            this.type = type;
        }
    }

    private volatile boolean nativeStatus;

    void useNativeStatus(boolean enabled) {
        synchronized (lock) {
            if (enabled && (pending != null || active != null || detached))
                throw new IllegalStateException("Screenshot bar session still owns status pixels");
            nativeStatus = enabled;
        }
    }

    Request prepare(Object owner, Executor shell) {
        Request request;
        synchronized (lock) {
            if (detached || active != null) {
                log.accept("Skipped prepare: detached=" + detached + ", active=" + (active != null), null);
                return null;
            }
            request = new Request(owner, shell, ++epoch);
            pending = request;
        }
        log.accept("Prepared recents bars epoch=" + request.epoch, null);
        main.post(() -> capture(request));
        return request;
    }

    void bind(Request request, IBinder token) {
        if (request == null) return;
        synchronized (lock) {
            if (pending != request) return;
            if (token == null) pending = null;
            else request.token = token;
        }
    }

    private void capture(Request request) {
        synchronized (lock) {
            if (detached || pending != request) return;
        }
        try {
            // Read SystemUI roots on their owner Looper. No hooks on global View/MotionEvent.
            Class<?> globalClass = Class.forName("android.view.WindowManagerGlobal");
            Object global = globalClass.getDeclaredMethod("getInstance").invoke(null);
            Object roots = field(global, "mViews");
            if (!(roots instanceof List<?>)) return;
            ArrayList<Bar> bars = new ArrayList<>();
            Rect display = null;
            boolean statusFound = false;
            for (Object entry : new ArrayList<>((List<?>) roots)) {
                if (!(entry instanceof View)) continue;
                View view = (View) entry;
                if (!(view.getLayoutParams() instanceof WindowManager.LayoutParams)
                        || view.getDisplay() == null || view.getDisplay().getDisplayId() != 0
                        || view.getDisplay().getRotation() != 0 || !view.isAttachedToWindow()
                        || !view.isShown() || view.getAlpha() != 1f) continue;
                WindowManager.LayoutParams lp = (WindowManager.LayoutParams) view.getLayoutParams();
                if ((lp.flags & WindowManager.LayoutParams.FLAG_SECURE) != 0) continue;
                boolean status = lp.type == 2000 && view.getClass().getName().equals(
                        "com.android.systemui.statusbar.window.StatusBarWindowView");
                boolean nav = lp.type == 2019 && view.getClass().getName().equals(
                        "com.android.systemui.navigationbar.views.NavigationBarFrame");
                if (status && nativeStatus) continue;
                if (!status && !nav) continue;
                // A visible native handle proves gestural navigation; do not copy 3-button UI.
                if (nav && !hasVisibleHandle(view)) continue;
                int width = view.getWidth(), height = view.getHeight();
                if (width <= 0 || height <= 0 || height > 256 || width > 4096) continue;
                Rect windowBounds = view.getContext().getSystemService(WindowManager.class)
                        .getMaximumWindowMetrics().getBounds();
                if (windowBounds.left != 0 || windowBounds.top != 0
                        || width != windowBounds.width()) continue;
                int[] xy = new int[2];
                view.getLocationOnScreen(xy);
                Rect bounds = new Rect(xy[0], xy[1], xy[0] + width, xy[1] + height);
                if (!windowBounds.contains(bounds) || bounds.left != 0
                        || (status && bounds.top != 0)
                        || (nav && bounds.bottom != windowBounds.bottom)) continue;
                if (display != null && !display.equals(windowBounds)) return;
                if (status && statusFound) return;
                if (status) {
                    logBatteryState(view, "before");
                    refreshBatteryDrawingCache(view);
                }
                Bitmap pixels = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                view.draw(new Canvas(pixels));
                if (status) logBatteryState(view, "after");
                bars.add(new Bar(view, pixels, bounds, lp.type));
                display = new Rect(windowBounds);
                statusFound |= status;
            }
            if ((!statusFound && !nativeStatus) || display == null || bars.size() > 2) return;
            synchronized (lock) {
                if (!detached && pending == request) request.capture = new Capture(bars, display);
            }
        } catch (Throwable failure) {
            log.accept("Bar capture unavailable; keeping native bars", failure);
        }
    }

    private void logBatteryState(View root, String phase) {
        try {
            int id = root.getResources().getIdentifier("battery", "id", "com.android.systemui");
            View battery = root.findViewById(id);
            if (battery == null || !battery.getClass().getName().equals(
                    "com.android.systemui.statusbar.views.MiuiBatteryMeterView")) return;
            Object icon = field(battery, "mBatteryIconView");
            Object hollow = field(battery, "mHollowBatteryIconView");
            log.accept("Bar battery capture " + phase + ": owner=" + System.identityHashCode(battery)
                    + ", level=" + field(battery, "mLevel") + ", charging=" + field(battery, "mCharging")
                    + ", shown=" + battery.isShown() + ", iconLevel=" + field(icon, "mLevel")
                    + ", hollowLevel=" + field(hollow, "level"), null);
        } catch (Throwable failure) {
            log.accept("Bar battery capture diagnostics unavailable", failure);
        }
    }

    @SuppressWarnings("deprecation")
    private void refreshBatteryDrawingCache(View root) {
        int id = root.getResources().getIdentifier("battery", "id", "com.android.systemui");
        View battery = id == 0 ? null : root.findViewById(id);
        if (battery == null || !battery.getClass().getName().equals(
                "com.android.systemui.statusbar.views.MiuiBatteryMeterView")) return;
        // View.draw on a software Canvas falls back to build/getDrawingCache
        // even for LAYER_TYPE_HARDWARE. HWUI's current display list does not
        // prove that an old software cache represents the current battery data.
        // Drop only this battery subtree's caches and ancestor caches containing
        // it; the stock draw rebuilds the same isolated layers synchronously.
        clearDrawingCaches(battery);
        View parent = battery;
        while (parent != root && parent.getParent() instanceof View) {
            parent = (View) parent.getParent();
            parent.destroyDrawingCache();
        }
        log.accept("Refreshed battery software drawing caches before bar capture", null);
    }

    @SuppressWarnings("deprecation")
    private static void clearDrawingCaches(View view) {
        view.destroyDrawingCache();
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) clearDrawingCaches(group.getChildAt(i));
        }
    }

    Session stage(Object owner, Object controller, IBinder token, TransitionInfo info,
                  SurfaceControl.Transaction start) {
        Request request;
        synchronized (lock) {
            request = pending;
            if (detached || active != null || request == null || request.owner != owner
                    || request.token != token) return null;
            pending = null; // Consume once. Never late-adopt a stale capture.
        }
        if (request.capture == null || SystemClock.uptimeMillis() - request.time > CAPTURE_MAX_AGE_MS) {
            log.accept("Skipped recents: no fresh bar capture", null);
            return null;
        }
        Session session = new Session(request, controller);
        try {
            TransitionInfo.Change app = selectApp(info, request.capture.display);
            if (app == null || app.getLeash() == null || !app.getLeash().isValid()) {
                log.accept("Skipped recents: unsupported transition " + info, null);
                return null;
            }
            synchronized (lock) {
                if (detached || request.epoch != epoch) return null;
                active = session;
            }
            for (Bar bar : session.bars) {
                SurfaceControl surface = createBufferSurface(bar, app.getLeash());
                session.layers.add(surface);
            }
            for (int i = 0; i < session.bars.size(); i++) {
                Bar bar = session.bars.get(i);
                SurfaceControl surface = session.layers.get(i);
                start.setPosition(surface, bar.bounds.left, bar.bounds.top)
                        .setLayer(surface, Integer.MAX_VALUE - (bar.type == 2000 ? 1 : 2))
                        .setAlpha(surface, 1f);
                SurfaceControl.Transaction.class.getMethod("show", SurfaceControl.class)
                        .invoke(start, surface);
            }
            log.accept("Staged bars for recents task=" + app.getTaskInfo().taskId
                    + ", count=" + session.layers.size(), null);
            return session;
        } catch (Throwable failure) {
            endOnShell(session);
            log.accept("Cannot stage bars; native transition preserved", failure);
            return null;
        }
    }

    private static TransitionInfo.Change selectApp(TransitionInfo info, Rect display) throws Exception {
        if (info == null || info.getRootCount() != 1 || info.getRoot(0).getDisplayId() != 0
                || !info.getRoot(0).getOffset().equals(0, 0)
                || (info.getFlags() & 128) == 0 || (info.getFlags() & (2 | 16 | 256 | 512)) != 0
                || (info.getType() != 3 && info.getType() != 1021)) return null;
        TransitionInfo.Change app = null;
        boolean home = false;
        for (TransitionInfo.Change change : info.getChanges()) {
            ActivityManager.RunningTaskInfo task = change.getTaskInfo();
            if (task == null) {
                if (!change.hasFlags(2)) return null; // Only wallpaper may accompany two Tasks.
                continue;
            }
            int type = (Integer) task.getClass().getMethod("getActivityType").invoke(task);
            int mode = (Integer) task.getClass().getMethod("getWindowingMode").invoke(task);
            int startRotation = (Integer) change.getClass().getMethod("getStartRotation").invoke(change);
            int endRotation = (Integer) change.getClass().getMethod("getEndRotation").invoke(change);
            if (task.getClass().getField("displayId").getInt(task) != 0 || mode != 1 || startRotation != 0 || endRotation != 0
                    || change.getParent() != null || change.getLastParent() != null
                    || !display.equals(change.getStartAbsBounds())
                    || !display.equals(change.getEndAbsBounds())) return null;
            if (type == 2 && (change.getMode() == 1 || change.getMode() == 3)) {
                if (home || task.topActivity == null
                        || !"com.miui.home".equals(task.topActivity.getPackageName())) return null;
                home = true;
            } else if (type == 1 && (change.getMode() == 4 || change.getMode() == 6)) {
                if (app != null) return null;
                app = change;
            } else return null;
        }
        return home ? app : null;
    }

    // Executed in LSPosed-injected SystemUI with framework hidden-API access.
    @SuppressLint({"BlockedPrivateApi", "DiscouragedPrivateApi"})
    private static SurfaceControl createBufferSurface(Bar bar, SurfaceControl parent) throws Exception {
        SurfaceControl.Builder builder = new SurfaceControl.Builder()
                .setName("MiuiBackGestureHook-RecentsBar-" + bar.type)
                .setBufferSize(bar.bitmap.getWidth(), bar.bitmap.getHeight())
                .setFormat(PixelFormat.TRANSLUCENT).setParent(parent);
        SurfaceControl.Builder.class.getDeclaredMethod("setHidden", boolean.class).invoke(builder, true);
        SurfaceControl layer = builder.build();
        Surface producer = null;
        try {
            producer = Surface.class.getDeclaredConstructor().newInstance();
            Surface.class.getDeclaredMethod("copyFrom", SurfaceControl.class).invoke(producer, layer);
            Canvas canvas = producer.lockCanvas(null);
            try {
                canvas.drawColor(0, PorterDuff.Mode.CLEAR);
                canvas.drawBitmap(bar.bitmap, 0, 0, null);
            } finally {
                producer.unlockCanvasAndPost(canvas);
            }
            return layer;
        } catch (Throwable failure) {
            try (SurfaceControl.Transaction t = new SurfaceControl.Transaction()) {
                remove(t, layer);
                t.apply();
            }
            layer.release();
            throw failure;
        } finally {
            if (producer != null) producer.release();
        }
    }

    void started(Session session, boolean accepted) {
        if (session == null) return;
        if (!accepted) {
            endOnShell(session);
            return;
        }
        main.post(() -> {
            if (!current(session)) return;
            for (Bar bar : session.bars) {
                if (!validSource(bar)) {
                    log.accept("Source changed before/during fade type=" + bar.type
                            + ", view=" + bar.view.get(), null);
                    cancelFromMain(session);
                    return;
                }
            }
            try {
                SurfaceControl.Transaction parents = new SurfaceControl.Transaction();
                try {
                    for (int i = 0; i < session.bars.size(); i++) {
                        View view = session.bars.get(i).view.get();
                        AttachedSurfaceControl root = view.getRootSurfaceControl();
                        if (root == null)
                            throw new IllegalStateException("Missing bar root for return handoff");
                        // buildReparentTransaction eagerly caches mBoundsLayer, but its show()
                        // remains in the returned transaction. Discarding that transaction on
                        // a Home finish leaves a permanently hidden parent for later cancels.
                        // Our captured roots have zero surface insets, so their buffer surface
                        // is the exact return space. Reparent only our child; never show/change
                        // the framework-owned bounds layer (including one left by an old build).
                        if (!(view.getLayoutParams() instanceof WindowManager.LayoutParams)
                                || !((Rect) WindowManager.LayoutParams.class.getField("surfaceInsets")
                                .get(view.getLayoutParams())).equals(new Rect())
                                || !root.getClass().getName().equals("android.view.ViewRootImpl"))
                            throw new IllegalStateException("Bar root has unsupported surface insets");
                        SurfaceControl parent = (SurfaceControl) root.getClass()
                                .getMethod("getSurfaceControl").invoke(root);
                        if (parent == null || !parent.isValid())
                            throw new IllegalStateException("Bar return buffer surface is invalid");
                        parents.reparent(session.layers.get(i), parent)
                                .setPosition(session.layers.get(i), 0, 0);
                        log.accept("Bar return parent epoch=" + session.request.epoch
                                + ", type=" + session.bars.get(i).type + ", parent=" + parent, null);
                        session.returnRoots.add(root);
                    }
                    session.returnParents = parents;
                } catch (Throwable failure) {
                    parents.close();
                    throw failure;
                }
            } catch (Throwable failure) {
                session.returnRoots.clear();
                log.accept("Bar return frame handoff unavailable", failure);
            }
            for (Bar bar : session.bars) {
                View view = bar.view.get();
                if (view != null) {
                    view.setAlpha(0f);
                    bar.writtenAlpha = 0f;
                    bar.changed = true;
                }
            }
            session.started = true;
            log.accept("Bar sources hidden; awaiting bound native swipe state", null);
            int state;
            synchronized (lock) {
                state = session.request.gestureState;
            }
            applyGestureState(session, state);


        });
    }

    void gestureState(Request request, int state) {
        if (request == null) return;
        Session session;
        synchronized (lock) {
            if (detached) return;
            session = active;
            if (pending != request && (session == null || session.request != request
                    || session.ended || session.finishing)) return;
            request.gestureState = state;
            if (session == null || session.request != request) return;
        }
        log.accept("Bound native state=" + state + ", epoch=" + request.epoch, null);
        main.post(() -> applyGestureState(session, state));
    }

    private void applyGestureState(Session session, int state) {
        if (!current(session) || !session.started) return;
        if (session.returningToApp) return;
        // State 3 starts while the upward drag is held; begin fading before
        // the terminal cancel (9) or Overview (0) state.
        if (state == 3 || state == 0 || state == 1) {
            if (session.folme == null && !session.fadeComplete) fade(session);
        } else if (state == 9) {
            // Native TO_CURRENT_APP is a SHOW command while the same task card
            // animates back to fullscreen. Keep both the child surfaces and the
            // existing Folme target until the owning Shell controller cleans up.
            session.returningToApp = true;
            if (session.folme != null) {
                try {
                    session.fadeComplete = false;
                    session.folme.retarget(new float[]{0, 1, 0, 1});
                    log.accept("Native Folme reversing to app epoch=" + session.request.epoch
                            + ", framesBeforeReverse=" + session.nativeFrames, null);
                } catch (Throwable failure) {
                    log.accept("Cannot restore app bar alpha through Folme", failure);
                    cancelFromMain(session);
                }
            }
        } else if (state == 2 || state == 5 || state == 7
                || state == 10) {
            cancelFromMain(session);
        }
    }

    private void fade(Session session) {
        try {
            ClassLoader loader = folmeLoader;
            if (loader == null) throw new IllegalStateException("Native Folme hooks unavailable");
            session.folme = new XiaomiBarFolme(loader, new XiaomiBarFolme.Sink() {
                public void update(float[] values, double[] velocities) {
                    if (!current(session)) return;
                    session.nativeFrames++;
                    for (Bar bar : session.bars) {
                        if (!validSource(bar)) {
                            cancelFromMain(session);
                            return;
                        }
                    }
                    applyNativeAlpha(session, values);
                }

                public void terminal(boolean cancelled) {
                    if (!current(session)) return;
                    if (cancelled) {
                        cancelFromMain(session);
                        return;
                    }
                    session.fadeComplete = true;
                    applyNativeAlpha(session, session.returningToApp
                            ? new float[]{0, 1, 0, 1} : new float[4]);
                    log.accept("Native Folme reached " + (session.returningToApp ? "app SHOW" : "HIDE")
                            + " epoch=" + session.request.epoch + ", frames=" + session.nativeFrames
                            + "; retaining surfaces until Shell cleanup", null);
                }
            });
            session.folme.set(new float[]{0, 1, 0, 1});
            session.folme.retarget(new float[4]);
            log.accept("Native Folme driving card layers epoch=" + session.request.epoch
                    + "; gesture policy still uses compatibility state3 trigger", null);
        } catch (Throwable failure) {
            log.accept("Native card Folme failed", failure);
            cancelFromMain(session);
        }
    }

    private void applyNativeAlpha(Session session, float[] values) {
        session.request.shell.execute(() -> {
            if (!current(session)) return;
            try (SurfaceControl.Transaction t = new SurfaceControl.Transaction()) {
                for (int i = 0; i < session.layers.size(); i++) {
                    SurfaceControl layer = session.layers.get(i);
                    if (!layer.isValid()) {
                        endOnShell(session);
                        return;
                    }
                    int channel = session.bars.get(i).type == 2000 ? 1 : 3;
                    t.setAlpha(layer, Math.max(0f, Math.min(1f, values[channel])));
                }
                t.apply();
            } catch (Throwable failure) {
                log.accept("Native bar alpha transaction failed", failure);
                endOnShell(session);
            }
        });
    }

    void dispatchFolme(Object listener, java.util.Collection<?> updates, Boolean cancelled) {
        Session session;
        synchronized (lock) {
            session = active;
        }
        if (session == null || !current(session) || Looper.myLooper() != main.getLooper()) return;
        XiaomiBarFolme engine = session.folme;
        if (engine == null || !engine.accepts(listener)) return;
        try {
            if (cancelled == null) engine.onUpdate(listener, updates);
            else engine.onTerminal(listener, cancelled);
        } catch (Throwable failure) {
            log.accept("Native bar Folme callback failed", failure);
            cancelFromMain(session);
        }
    }

    private void closeFolme(Session session) {
        XiaomiBarFolme engine = session.folme;
        session.folme = null;
        try {
            if (engine != null) engine.close();
        } catch (Throwable failure) {
            log.accept("Native bar Folme cleanup failed", failure);
        }
    }

    void cleanup(Object controller) {
        Session session;
        synchronized (lock) {
            session = active;
        }
        if (session != null && session.controller == controller) endOnShell(session);
    }

    private boolean current(Session session) {
        synchronized (lock) {
            return !detached && active == session && !session.ended && !session.finishing;
        }
    }

    private static boolean validSource(Bar bar) {
        View view = bar.view.get();
        if (view == null || !view.isAttachedToWindow() || view.getVisibility() != View.VISIBLE
                || view.getDisplay() == null || view.getDisplay().getRotation() != 0
                || view.getAlpha() != bar.writtenAlpha || view.getWidth() != bar.bounds.width()
                || view.getHeight() != bar.bounds.height()) return false;
        int[] xy = new int[2];
        view.getLocationOnScreen(xy);
        return xy[0] == bar.bounds.left && xy[1] == bar.bounds.top;
    }

    private void cancelFromMain(Session session) {
        if (session.finishing || session.ended) return;
        session.finishing = true;
        session.request.shell.execute(() -> endOnShell(session));
    }

    private void endOnShell(Session session) {
        synchronized (lock) {
            if (session.ended) return;
            session.ended = true;
        }
        SurfaceControl.Transaction parents = session.returnParents;
        session.returnParents = null;
        if (parents != null && !session.returningToApp) {
            parents.close();
            parents = null;
        }
        if (parents != null) {
            try (SurfaceControl.Transaction owned = parents) {
                // Leave Shell's dying task leash before its original cleanup runs.
                owned.apply();
                main.post(() -> restoreOnDraw(session));
                return;
            } catch (Throwable failure) {
                log.accept("Bar return reparent failed", failure);
            }
        }
        log.accept("Removing recents bar layers without frame handoff", null);
        removeSurfaces(session);
        main.post(() -> {
            closeFolme(session);
            restoreViews(session);
            released(session);
            log.accept("Recents bar session released epoch=" + session.request.epoch, null);
        });
    }

    private void restoreOnDraw(Session session) {
        closeFolme(session);
        restoreViews(session);
        java.util.concurrent.atomic.AtomicInteger remaining = new java.util.concurrent.atomic.AtomicInteger(session.layers.size());
        Runnable committed = () -> {
            if (remaining.decrementAndGet() != 0) return;
            session.request.shell.execute(() -> {
                for (SurfaceControl layer : session.layers) layer.release();
                session.layers.clear();
                main.post(() -> {
                    released(session);
                    log.accept("Bar return frame committed; session released epoch=" + session.request.epoch, null);
                });
            });
        };
        for (int i = 0; i < session.layers.size(); i++) {
            SurfaceControl layer = session.layers.get(i);
            View view = session.bars.get(i).view.get();
            java.util.concurrent.atomic.AtomicBoolean delivered = new java.util.concurrent.atomic.AtomicBoolean();
            Runnable once = () -> {
                if (delivered.compareAndSet(false, true)) committed.run();
            };
            try (SurfaceControl.Transaction transaction = new SurfaceControl.Transaction()) {
                AttachedSurfaceControl root = session.returnRoots.get(i);
                boolean sameRoot = view != null && view.isAttachedToWindow()
                        && view.getRootSurfaceControl() == root;
                log.accept("Bar return source epoch=" + session.request.epoch
                        + ", type=" + session.bars.get(i).type + ", sameRoot=" + sameRoot
                        + ", hardware=" + (view != null && view.isHardwareAccelerated()), null);
                if (layer.isValid()) remove(transaction, layer);
                transaction.addTransactionCommittedListener(main::post, once::run);
                if (sameRoot && view.isHardwareAccelerated()) {
                    if (!root.applyTransactionOnDraw(transaction)) transaction.apply();
                    view.invalidate();
                } else transaction.apply();
            } catch (Throwable failure) {
                log.accept("Bar return draw transaction failed", failure);
                // Remove only this owned layer on failure; do not leave a stale copy.
                try (SurfaceControl.Transaction fallback = new SurfaceControl.Transaction()) {
                    if (layer.isValid()) remove(fallback, layer);
                    fallback.apply();
                } catch (Throwable cleanup) {
                    log.accept("Bar return fallback failed", cleanup);
                }
                once.run();
            }
        }
    }

    private static void restoreViews(Session session) {
        for (Bar bar : session.bars) {
            View view = bar.view.get();
            if (bar.changed && view != null && view.getAlpha() == bar.writtenAlpha)
                view.setAlpha(1f);
            bar.changed = false;
        }
    }

    private void removeSurfaces(Session session) {
        try (SurfaceControl.Transaction t = new SurfaceControl.Transaction()) {
            for (SurfaceControl layer : session.layers) if (layer.isValid()) remove(t, layer);
            t.apply();
        } catch (Throwable failure) {
            log.accept("Failed to remove bar surfaces", failure);
        } finally {
            for (SurfaceControl layer : session.layers) layer.release();
            session.layers.clear();
        }
    }

    // Reserve reload atomically against a new prepare/stage. Active includes View cleanup.
    boolean reserveReload() {
        synchronized (lock) {
            if (active != null) return false;
            detached = true;
            ++epoch;
            pending = null;
            return true;
        }
    }

    void detach() {
        reserveReload();
    }

    void resumeAfterRejectedReload() {
        synchronized (lock) {
            if (active != null)
                throw new IllegalStateException("Active bar session cannot resume reload");
            detached = false;
            ++epoch;
        }
    }

    private static void remove(SurfaceControl.Transaction transaction, SurfaceControl layer)
            throws Exception {
        SurfaceControl.Transaction.class.getMethod("remove", SurfaceControl.class)
                .invoke(transaction, layer);
    }

    private static boolean hasVisibleHandle(View root) {
        if (root.getClass().getName().equals("com.android.systemui.navigationbar.gestural.NavigationHandle")) {
            return root.isShown() && root.getAlpha() == 1f;
        }
        if (root instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++)
                if (hasVisibleHandle(group.getChildAt(i))) return true;
        }
        return false;
    }

    private static Object field(Object owner, String name) throws Exception {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }
}
