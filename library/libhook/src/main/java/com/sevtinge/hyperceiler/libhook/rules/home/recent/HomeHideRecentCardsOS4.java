/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.home.recent;

import android.app.Application;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.IInterface;
import android.os.SystemClock;
import com.sevtinge.hyperceiler.common.log.ModuleLog;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.base.OriginalArgumentsHook;
import com.sevtinge.hyperceiler.libhook.provider.HomeLayoutPrefsSnapshot;
import com.sevtinge.hyperceiler.libhook.utils.api.ContextUtils;
import com.sevtinge.hyperceiler.libhook.utils.api.ThreadPoolManager;
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** Filter the live Shell AIDL reply, not an OEM-named controller or the system task store. */
public final class HomeHideRecentCardsOS4 extends BaseHook {
    private static final String TAG = "HyperCeiler.RecentsCards";
    private static final Uri ROOT = Uri.parse("content://com.sevtinge.hyperceiler.provider.sharedprefs");
    private record Selection(Set<String> packages, int launcherAppId) {
        Selection { packages = Set.copyOf(packages); }
        boolean owns(int uid) { return uid >= 10000 && launcherAppId >= 10000 && uid % 100000 == launcherAppId; }
    }
    private final Object publication = new Object();
    private final Object contextGate = new Object();
    private final Set<Class<?>> bound = new HashSet<>();
    private record Listener(int uid, Object target, Method changed) { }
    private final ConcurrentHashMap<Object, Listener> listeners = new ConcurrentHashMap<>();
    private final AtomicBoolean queued = new AtomicBoolean(), appliedReported = new AtomicBoolean();
    private final ThreadLocal<ArrayDeque<Selection>> calls = ThreadLocal.withInitial(ArrayDeque::new);
    private static final Selection STOCK = new Selection(Set.of(), -1);
    private volatile Selection selection;
    private volatile Context context;
    private volatile boolean closed;
    private volatile long retryAt;
    private long revision;
    private Set<String> lastPackages = Set.of();
    private String lastFailure = "";
    private ContentObserver observer;
    private Method query;
    private HomeRecentCardsFilter.Plan plan;

    private static void observeMethod(Method method, IMethodHook callback) {
        chain(method, OriginalArgumentsHook.of(callback));
    }
    @Override public void init() {
        if (!"com.android.systemui".equals(Application.getProcessName())) return;
        registerHotReloadCleanup(this::close);
        try {
            // Public Shell Binder contract. Concrete implementation is learned
            // from its actual Stub construction, including vendor subclasses.
            Class<?> contract = findClass("com.android.wm.shell.recents.IRecentTasks");
            Class<?> stub = findClass(contract.getName() + "$Stub");
            if (!contract.isInterface() || !contract.isAssignableFrom(stub) || !Binder.class.isAssignableFrom(stub))
                throw new IllegalStateException("recent-task Stub contract");
            // Optimized OEM jars can move interface methods onto abstract Stub
            // while leaving IRecentTasks as a marker. Its live Stub ABI is authoritative.
            query = HomeRecentCardsFilter.query(stub);
            plan = new HomeRecentCardsFilter.Plan(query.getReturnType(), Class.forName("android.app.TaskInfo"));
            hookAllConstructors(stub, new IMethodHook() {
                @Override public void after(HookParam p) {
                    if (closed || p.getThrowable() != null) return;
                    try { bind(p.getThisObject().getClass()); }
                    catch (Exception | LinkageError e) { failure("runtime implementation", e); }
                }
            });
            hookMethod(Application.class.getDeclaredMethod("attach", Context.class), new IMethodHook() {
                @Override public void after(HookParam p) {
                    if (p.getThrowable() == null && p.getThisObject() instanceof Context app) attach(app);
                }
            });
            Context current = ContextUtils.getContextNoError(ContextUtils.FLAG_CURRENT_APP);
            if (current != null) attach(current);
            ModuleLog.i(TAG, "IRecentTasks reply contract ready, waiting for runtime implementation, original task permissions retained");
        } catch (Exception | LinkageError e) { closed = true; failure("contract unavailable, stock retained", e); }
    }
    private void bind(Class<?> type) throws Exception {
        synchronized (bound) {
            if (closed || bound.contains(type)) return;
            if (bound.size() >= 16) throw new IllegalStateException("implementation budget");
            Method implementation = type.getMethod(query.getName(), query.getParameterTypes());
            if (Modifier.isAbstract(implementation.getModifiers()) || implementation.getReturnType() != query.getReturnType())
                throw new NoSuchMethodException("concrete AIDL reply");
            implementation.setAccessible(true);
            bound.add(type); // Do not install duplicate hooks after a partial backend failure.
            observeMethod(implementation, new IMethodHook() {
                @Override public void before(HookParam p) {
                    Selection current = selection;
                    if (!closed && current == null && SystemClock.elapsedRealtime() >= retryAt) requestRefresh();
                    calls.get().push(!closed && current != null && current.owns(Binder.getCallingUid()) ? current : STOCK);
                }
                @Override public void after(HookParam p) {
                    var stack = calls.get();
                    Selection captured = stack.isEmpty() ? STOCK : stack.pop();
                    if (stack.isEmpty()) calls.remove();
                    if (closed || captured == STOCK || captured != selection || captured.packages().isEmpty()
                        || p.getThrowable() != null) return;
                    try {
                        Object original = p.getResult(), filtered = plan.filter(original, captured.packages());
                        if (selection != captured || closed || filtered == original) return;
                        p.setResult(filtered);
                        if (appliedReported.compareAndSet(false, true)) ModuleLog.i(TAG,
                            "selected-app cards filtered from launcher reply, task store and processes unchanged");
                    } catch (Exception | LinkageError e) { failure("reply declined, stock retained", e); }
                }
            });
            try {
                bindListener(type, "registerRecentTasksListener", true);
                bindListener(type, "unregisterRecentTasksListener", false);
            } catch (ReflectiveOperationException unavailable) { failure("listener contract unavailable", unavailable); }
            try {
                // ExternalInterfaceBinder retires old Stub instances whenever
                // the launcher reconnects. Do not retain their remote listeners.
                Method invalidate = type.getMethod("invalidate");
                if (invalidate.getReturnType() == void.class && !Modifier.isStatic(invalidate.getModifiers())) {
                    invalidate.setAccessible(true);
                    hookMethod(invalidate, new IMethodHook() {
                        @Override public void after(HookParam p) {
                            if (p.getThrowable() == null) listeners.remove(p.getThisObject());
                        }
                    });
                }
            } catch (NoSuchMethodException absent) { /* Older Shell variants can unregister explicitly. */ }
            ModuleLog.i(TAG, "runtime recent-task implementation bound " + type.getName());
        }
    }
    private void bindListener(Class<?> type, String name, boolean register) throws Exception {
        Method selected = null;
        for (Method method : type.getMethods()) if (method.getName().equals(name) && method.getParameterCount() == 1
            && method.getReturnType() == void.class && IInterface.class.isAssignableFrom(method.getParameterTypes()[0])) {
            if (selected != null) throw new NoSuchMethodException("listener contract");
            selected = method;
        }
        if (selected == null) return; // Filtering still applies to every requested reply.
        selected.setAccessible(true);
        hookMethod(selected, new IMethodHook() {
            @Override public void after(HookParam p) {
                if (closed || p.getThrowable() != null) return;
                Object listener = p.getArgs()[0];
                if (register && listener != null && listeners.size() < 8) {
                    try {
                        // The listener interface can be optimized too. Resolve
                        // its actual local Stub OR remote Proxy, not a Stub-only Method.
                        Method callback = listener.getClass().getMethod("onRecentTasksChanged");
                        if (callback.getReturnType() != void.class || Modifier.isStatic(callback.getModifiers()))
                            throw new NoSuchMethodException("listener callback");
                        callback.setAccessible(true);
                        listeners.put(p.getThisObject(), new Listener(Binder.getCallingUid(), listener, callback));
                    } catch (ReflectiveOperationException e) { failure("listener callback unavailable", e); }
                } else listeners.remove(p.getThisObject());
            }
        });
    }
    private void attach(Context app) {
        synchronized (contextGate) {
            if (closed || context != null || !"com.android.systemui".equals(app.getPackageName())) return;
            Context application = app.getApplicationContext(); context = application == null ? app : application;
            observer = new ContentObserver(null) {
                @Override public void onChange(boolean self, Uri uri) {
                    String key = uri == null ? "" : uri.getLastPathSegment();
                    if (key == null || key.isEmpty() || key.equals("home_layout")
                        || key.endsWith(HomeLayoutPrefsSnapshot.RECENT_CARDS_KEY)
                        || key.endsWith("allow_hook")) {
                        synchronized (publication) { ++revision; selection = null; retryAt = 0; }
                        requestRefresh();
                    }
                }
            };
            try {
                context.getContentResolver().registerContentObserver(ROOT, true, observer);
                requestRefresh();
            } catch (Exception e) {
                try { context.getContentResolver().unregisterContentObserver(observer); } catch (RuntimeException ignored) { }
                context = null; failure("preference observer", e);
            }
        }
    }
    private void requestRefresh() {
        if (closed || context == null || !queued.compareAndSet(false, true)) return;
        try { ThreadPoolManager.execute(this::refresh); }
        catch (RuntimeException rejected) { queued.set(false); retryAt = SystemClock.elapsedRealtime() + 2000; }
    }
    private void refresh() {
        long completed = -1;
        try {
            for (int attempt = 0; attempt < 4 && !closed; ++attempt) {
                long ticket; synchronized (publication) { ticket = revision; }
                Selection next = readSelection();
                boolean changed;
                synchronized (publication) {
                    if (closed) return;
                    if (revision != ticket) continue;
                    selection = next; completed = ticket;
                    retryAt = next == null ? SystemClock.elapsedRealtime() + 2000 : 0;
                    changed = next != null && !lastPackages.equals(next.packages());
                    if (next != null) lastPackages = next.packages();
                }
                if (changed) {
                    appliedReported.set(false);
                    ModuleLog.i(TAG, "physical selection published count=" + next.packages().size());
                    for (Listener listener : listeners.values()) try {
                        if (!closed && selection == next && next.owns(listener.uid()))
                            listener.changed().invoke(listener.target());
                    } catch (Exception e) { failure("listener invalidation", e); }
                }
                return;
            }
        } finally {
            queued.set(false);
            synchronized (publication) { if (!closed && revision != completed) requestRefresh(); }
        }
    }
    private Selection readSelection() {
        try (Cursor cursor = context.getContentResolver().query(Uri.withAppendedPath(ROOT, "home_layout"), null, null, null, null)) {
            if (cursor == null || !cursor.moveToFirst() || !"schema".equals(cursor.getColumnName(0))
                || cursor.getInt(0) != HomeLayoutPrefsSnapshot.SCHEMA) return null;
            var extras = cursor.getExtras();
            if (extras == null || extras.getInt(HomeLayoutPrefsSnapshot.RECENT_CARDS_VERSION, 0) != 1) return null;
            var packages = HomeRecentCardsFilter.packages(HomeLayoutPrefsSnapshot.decodeTitleRecords(
                extras.getString(HomeLayoutPrefsSnapshot.RECENT_CARDS_KEY)));
            for (String key : new String[]{"allow_hook", "framework_api_allow_hook"}) {
                int at = cursor.getColumnIndex(key);
                if (at >= 0 && !cursor.isNull(at) && cursor.getInt(at) != 1) packages = Set.of();
            }
            int uid = context.getPackageManager().getApplicationInfo("com.miui.home", 0).uid;
            return new Selection(packages, uid % 100000);
        } catch (Exception | LinkageError e) { failure("physical preferences unavailable", e); return null; }
    }
    private synchronized void failure(String stage, Throwable error) {
        String value = stage + ":" + error.getClass().getSimpleName();
        if (lastFailure.equals(value)) return;
        lastFailure = value; ModuleLog.w(TAG, value + ", original behavior retained");
    }
    private void close() {
        synchronized (publication) { closed = true; ++revision; selection = null; }
        synchronized (contextGate) {
            Context current = context;
            if (current != null && observer != null) try { current.getContentResolver().unregisterContentObserver(observer); }
            catch (RuntimeException ignored) { }
        }
        listeners.clear(); synchronized (bound) { bound.clear(); }
    }
}
