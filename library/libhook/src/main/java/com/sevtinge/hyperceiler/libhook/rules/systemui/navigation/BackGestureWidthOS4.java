package com.sevtinge.hyperceiler.libhook.rules.systemui.navigation;

import android.content.SharedPreferences;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;
import android.os.Handler;
import android.os.Looper;
import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;

/** SystemUI rejects downs beyond its resource edge even when Home's native window accepts them. */
public final class BackGestureWidthOS4 extends BaseHook {
    private final Map<Object, BackGestureWidthState> states = new WeakHashMap<>();
    private Field left, right;
    private volatile int percent = 100;
    private volatile boolean closed;
    private Context context;
    private final AtomicLong request = new AtomicLong();
    private static final Uri ROOT = Uri.parse("content://com.sevtinge.hyperceiler.provider.sharedprefs");
    private static final Uri WIDTH = Uri.withAppendedPath(ROOT, "integer/prefs_key_home_navigation_back_area_width/100");
    private final Handler main = new Handler(Looper.getMainLooper());
    @Override public void init() {
        Class<?> type = findClassIfExists("com.android.systemui.navigationbar.gestural.EdgeBackGestureHandler");
        if (type == null) return;
        final Method resources;
        try {
            left = type.getDeclaredField("mEdgeWidthLeft");
            right = type.getDeclaredField("mEdgeWidthRight");
            resources = type.getDeclaredMethod("updateCurrentUserResources");
            if (left.getType() != int.class || right.getType() != int.class
                || resources.getReturnType() != void.class) return;
            left.setAccessible(true); right.setAccessible(true);
        } catch (ReflectiveOperationException error) {
            XposedLog.w(TAG, "edge resource shape not validated: " + error); return;
        }
        percent = readPercent();
        hookMethod(resources, new IMethodHook() {
            @Override public void after(HookParam p) { refresh(p.getThisObject(), true); }
        });
        hookAllConstructors(type, new IMethodHook() {
            @Override public void after(HookParam p) { refresh(p.getThisObject(), false); }
        });
        SharedPreferences prefs = PrefsBridge.getSharedPreferences();
        if (prefs != null) {
            SharedPreferences.OnSharedPreferenceChangeListener listener = (p, key) -> {
                if (key == null || key.equals("home_navigation_back_area_width")
                    || key.equals("prefs_key_home_navigation_back_area_width")) {
                    main.post(() -> { if (closed) return; requestProvider(); });
                }
            };
            prefs.registerOnSharedPreferenceChangeListener(listener);
            registerHotReloadCleanup(() -> { closed = true; prefs.unregisterOnSharedPreferenceChangeListener(listener); });
        }
        runOnApplicationAttach(c -> attach(c));
        registerHotReloadCleanup(() -> { closed = true; request.incrementAndGet(); restoreAll(); });
        XposedLog.i(TAG, "validated original edge widths consumer; initial=" + percent);
    }
    private void attach(Context c) {
        if (closed || context != null) return;
        context = c.getApplicationContext();
        if (context == null) context = c;
        ContentObserver observer = new ContentObserver(main) {
            @Override public void onChange(boolean selfChange, Uri uri) {
                if (!closed && (uri == null || uri.toString().contains("home_navigation_back_area_width"))) requestProvider();
            }
        };
        context.getContentResolver().registerContentObserver(ROOT, true, observer);
        registerContentObserverHotReloadCleanup(context.getContentResolver(), observer);
        requestProvider();
    }
    // Provider is the page's committed truth; LSPosed's startup snapshot may be stale.
    // Query only on attach/pref events, off UI thread, never on MotionEvent.
    private void requestProvider() {
        if (closed) return;
        Context c = context;
        if (c == null) { percent = readPercent(); refreshAll(); return; }
        long version = request.incrementAndGet();
        AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
            int value;
            try (Cursor cursor = c.getContentResolver().query(WIDTH, null, null, null, null)) {
                if (cursor == null || !cursor.moveToFirst()) return;
                value = BackGestureWidthState.normalize(cursor.getInt(0));
            } catch (Throwable ignored) { return; } // retain last successful value
            main.post(() -> {
                if (closed || request.get() != version) return;
                percent = value; refreshAll();
            });
        });
    }
    private int readPercent() {
        try { return BackGestureWidthState.normalize(PrefsBridge.getInt("home_navigation_back_area_width", 100)); }
        catch (Throwable ignored) { return 100; }
    }
    private synchronized void refresh(Object target, boolean resourceReset) {
        if (closed || target == null) return;
        try {
            int l = left.getInt(target), r = right.getInt(target);
            BackGestureWidthState state = states.get(target);
            if (state == null) { state = new BackGestureWidthState(l, r); states.put(target, state); }
            else if (resourceReset) state.reset(l, r);
            state.apply(percent);
            left.setInt(target, state.appliedLeft); right.setInt(target, state.appliedRight);
        } catch (ReflectiveOperationException error) { XposedLog.w(TAG, "edge fields retained: " + error); }
    }
    private synchronized void refreshAll() { for (Object target : new ArrayList<>(states.keySet())) refresh(target, false); }
    private synchronized void restoreAll() {
        for (Map.Entry<Object, BackGestureWidthState> e : states.entrySet()) {
            try {
                BackGestureWidthState s = e.getValue();
                // Do not overwrite a later resource/other module change on cleanup.
                if (left.getInt(e.getKey()) == s.appliedLeft) left.setInt(e.getKey(), s.left);
                if (right.getInt(e.getKey()) == s.appliedRight) right.setInt(e.getKey(), s.right);
            } catch (ReflectiveOperationException ignored) { }
        }
        states.clear();
    }
}
