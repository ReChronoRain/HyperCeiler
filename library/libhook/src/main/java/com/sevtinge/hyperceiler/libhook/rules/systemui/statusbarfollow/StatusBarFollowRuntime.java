/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.systemui.statusbarfollow;

import android.content.SharedPreferences;
import android.view.SurfaceControl;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.github.libxposed.api.XposedInterface;
import io.github.lingqiqi5211.ezhooktool.xposed.java.Deoptimizers;

/**
 * Minimal adapter over HyperCeiler's hook framework for the copied status-bar follow contracts.
 * It intentionally does not import the reference project's general hook/hot-reload runtime.
 */
public abstract class StatusBarFollowRuntime extends BaseHook {
    protected static final String TAG = "HyperCeiler.StatusBarFollow";
    protected static final int LOG_DEBUG = 3;
    protected static final int LOG_INFO = 4;
    protected static final int LOG_WARN = 5;
    protected static final int LOG_ERROR = 6;
    protected volatile RecentsOpenSession.Attempt recentsOpenAttempt;
    private final List<XposedInterface.HookHandle> statusbarHookHandles = new CopyOnWriteArrayList<>();
    private volatile boolean dexKitLibraryLoaded;
    private final ThreadLocal<Boolean> statusbarHookActive = new ThreadLocal<>();
    private volatile boolean callbackTimeoutReported;

    protected abstract String resolveBarFollowDexContainer(Class<?> target) throws Throwable;

    protected abstract boolean isRecentsOpenAttemptCurrentOnOwner(RecentsOpenSession.Attempt attempt)
        throws Throwable;

    protected abstract boolean isRecentsOpenCompletionCurrentOnOwner(RecentsOpenSession.Attempt attempt)
        throws Throwable;

    protected void notifyRecentsOpenCompleted(RecentsOpenSession.Attempt opening) { }

    protected abstract void sendAuthenticatedMiuiHomeOpenBreakCommand(Object controller, boolean toHome)
        throws Throwable;

    protected abstract void publishSystemUiReturnHomeFinish(Object controller, Object transaction)
        throws Throwable;

    protected final class HookBuilder {
        private final Executable executable;
        HookBuilder(Executable executable) { this.executable = executable; }
        public HookBuilder setId(String value) { return this; }
        public XposedInterface.HookHandle intercept(XposedInterface.Hooker hooker) {
                        XposedInterface.Hooker gated = chain -> {
                if (!StatusBarFollowGate.enabled() || Boolean.TRUE.equals(statusbarHookActive.get()))
                    return chain.proceed();
                GuardedChain guarded = new GuardedChain(chain);
                statusbarHookActive.set(Boolean.TRUE);
                long started = System.nanoTime();
                try {
                    return hooker.intercept(guarded);
                } catch (Throwable failure) {
                    if (!guarded.proceeded) return chain.proceed();
                    if (guarded.hasResult()) return guarded.result;
                    moduleLog(LOG_WARN, TAG, "Status-bar callback failed after proceed", failure);
                    return null;
                } finally {
                    statusbarHookActive.remove();
                    long elapsed = System.nanoTime() - started;
                    if (elapsed > 50_000_000L && !callbackTimeoutReported) {
                        callbackTimeoutReported = true;
                        moduleLog(LOG_WARN, TAG, "Status-bar callback exceeded 50 ms budget");
                    }
                }
            };
            XposedInterface.HookHandle handle = executable instanceof Method method
                ? BaseHook.chain(method, gated)
                : BaseHook.chain((Constructor<?>) executable, gated);
            if (handle != null) statusbarHookHandles.add(handle);
            return handle;
        }
    }

    protected final HookBuilder hook(Method method) { return new HookBuilder(method); }
    protected final HookBuilder hook(Constructor<?> constructor) { return new HookBuilder(constructor); }

    protected final void rollbackStatusbarHooks() {
        for (int i = statusbarHookHandles.size() - 1; i >= 0; --i) {
            try { statusbarHookHandles.get(i).unhook(); }
            catch (Throwable ignored) { }
        }
        statusbarHookHandles.clear();
    }

    protected void recordHookHandle(XposedInterface.HookHandle handle) {
        if (handle != null) statusbarHookHandles.add(handle);
    }

    protected final void ensureDexKitLibraryLoaded() {
        if (dexKitLibraryLoaded) return;
        synchronized (this) {
            if (!dexKitLibraryLoaded) {
                System.loadLibrary("dexkit");
                dexKitLibraryLoaded = true;
            }
        }
    }

    protected final Method requireExactDeclaredMethod(
        Class<?> owner, String methodName, String returnTypeName, String... parameterTypeNames
    ) throws NoSuchMethodException {
        List<Method> matches = new ArrayList<>();
        for (Method method : owner.getDeclaredMethods()) {
            if (!method.getName().equals(methodName)
                || !method.getReturnType().getTypeName().equals(returnTypeName)
                || method.getParameterCount() != parameterTypeNames.length) continue;
            boolean same = true;
            Class<?>[] types = method.getParameterTypes();
            for (int i = 0; i < types.length; ++i) {
                if (!types[i].getTypeName().equals(parameterTypeNames[i])) { same = false; break; }
            }
            if (same) matches.add(method);
        }
        if (matches.size() != 1)
            throw new NoSuchMethodException(owner.getName() + "." + methodName + " matches=" + matches.size());
        matches.get(0).setAccessible(true);
        return matches.get(0);
    }

    protected final Object readField(Object target, String fieldName)
        throws NoSuchFieldException, IllegalAccessException {
        Field field = findStatusbarField(target.getClass(), fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    protected final Object readStaticField(Class<?> owner, String fieldName)
        throws NoSuchFieldException, IllegalAccessException {
        Field field = findStatusbarField(owner, fieldName);
        field.setAccessible(true);
        return field.get(null);
    }

    protected final Object readFieldOrNull(Object target, String fieldName) {
        try { return readField(target, fieldName); }
        catch (Throwable ignored) { return null; }
    }

    protected final void writeField(Object target, String fieldName, Object value)
        throws NoSuchFieldException, IllegalAccessException {
        Field field = findStatusbarField(target.getClass(), fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Field findStatusbarField(Class<?> type, String name) throws NoSuchFieldException {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try { return current.getDeclaredField(name); }
            catch (NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(type.getName() + "." + name);
    }

    protected final Object invokeAnyMethod(Object target, String methodName, Object[] args) throws Exception {
        Method best = findAnyMethod(target.getClass(), methodName, args == null ? 0 : args.length);
        if (best == null) throw new NoSuchMethodException(target.getClass().getName() + "." + methodName);
        best.setAccessible(true);
        return best.invoke(target, args == null ? new Object[0] : args);
    }

    private static Method findAnyMethod(Class<?> owner, String name, int argCount) {
        for (Class<?> current = owner; current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() == argCount) return method;
            }
        }
        return null;
    }

    protected final int readIntFieldOrDefault(Object target, String name, int fallback) {
        try {
            Object value = readField(target, name);
            return value instanceof Number ? ((Number) value).intValue() : fallback;
        } catch (Throwable ignored) { return fallback; }
    }

    protected final float readFloatFieldOrDefault(Object target, String name, float fallback) {
        try {
            Object value = readField(target, name);
            return value instanceof Number ? ((Number) value).floatValue() : fallback;
        } catch (Throwable ignored) { return fallback; }
    }

    protected final boolean surfacesAreSame(SurfaceControl first, SurfaceControl second) throws Exception {
        if (first == second) return true;
        if (first == null || second == null) return false;
        Object same = invokeAnyMethod(first, "isSameSurface", new Object[]{second});
        if (!(same instanceof Boolean)) throw new IllegalStateException("isSameSurface returned " + shortObject(same));
        return (Boolean) same;
    }

    protected static String shortObject(Object value) {
        if (value == null) return "null";
        String text = value.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(value));
        return text.length() > 96 ? text.substring(0, 93) + "..." : text;
    }

    protected final void moduleLog(int priority, String tag, String message) {
        if (priority <= 3) XposedLog.d(tag, message);
        else if (priority == 4) XposedLog.i(tag, message);
        else if (priority == 5) XposedLog.w(tag, message);
        else XposedLog.e(tag, message);
    }

    protected final void moduleLog(int priority, String tag, String message, Throwable failure) {
        if (priority <= 4) XposedLog.d(tag, message, failure);
        else if (priority == 5) XposedLog.w(tag, message, failure);
        else XposedLog.e(tag, message, failure);
    }

    private static final class GuardedChain implements XposedInterface.Chain {
        private final XposedInterface.Chain delegate;
        boolean proceeded;
        boolean completed;
        Object result;
        GuardedChain(XposedInterface.Chain delegate) { this.delegate = delegate; }
        @Override public Executable getExecutable() { return delegate.getExecutable(); }
        @Override public Object getThisObject() { return delegate.getThisObject(); }
        @Override public List<Object> getArgs() { return delegate.getArgs(); }
        @Override public Object getArg(int index) { return delegate.getArg(index); }
        @Override public Object proceed() throws Throwable { proceeded = true; return proceedWithResult(delegate.proceed()); }
        @Override public Object proceed(Object[] values) throws Throwable { proceeded = true; return proceedWithResult(delegate.proceed(values)); }
        @Override public Object proceedWith(Object target) throws Throwable { proceeded = true; return proceedWithResult(delegate.proceedWith(target)); }
        @Override public Object proceedWith(Object target, Object[] values) throws Throwable {
            proceeded = true;
            return proceedWithResult(delegate.proceedWith(target, values));
        }
        private Object proceedWithResult(Object value) {
            proceeded = true;
            completed = true;
            result = value;
            return value;
        }
        boolean hasResult() { return completed; }
    }

    protected static boolean deoptimize(Constructor<?> constructor) {
        return Deoptimizers.deoptimize(constructor);
    }

    protected final SharedPreferences getRemotePreferences(String group) {
        return PrefsBridge.getSharedPreferences();
    }

    protected final List<XposedInterface.HookHandle> statusbarHookHandles() {
        return new ArrayList<>(statusbarHookHandles);
    }
}
