/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.base;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.DexKit;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.IDexKit;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.IDexKitList;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.ResourcesTool;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam;
import io.github.lingqiqi5211.ezhooktool.core.BestMatchUtils;
import io.github.lingqiqi5211.ezhooktool.core.ClassUtils;
import io.github.lingqiqi5211.ezhooktool.core.java.Constructors;
import io.github.lingqiqi5211.ezhooktool.core.java.Fields;
import io.github.lingqiqi5211.ezhooktool.core.java.Methods;
import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed;
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.Deoptimizers;
import io.github.lingqiqi5211.ezhooktool.xposed.java.ExtraFields;
import io.github.lingqiqi5211.ezhooktool.xposed.java.Hooks;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IReplaceHook;

/**
 * Hook 基类
 * <p>
 * 提供 Java 常用的 Hook 工具方法
 * Kotlin 建议直接使用 EzHookTool DSL
 *
 * @author HyperCeiler
 */
public abstract class BaseHook {
    public static String ACTION_PREFIX = "com.sevtinge.hyperceiler.module.action.";
    public final String TAG = getClass().getSimpleName();
    private volatile boolean mInDexKitInit = false;

    @FunctionalInterface
    protected interface ThrowableRunnable {
        void run() throws Throwable;
    }

    @FunctionalInterface
    protected interface ThrowableSupplier<T> {
        T get() throws Throwable;
    }

    @FunctionalInterface
    public interface ContextConsumer {
        void accept(@NonNull Context context);
    }

    private interface ApplicationHook {
        void onApplicationAttachBefore(@NonNull Context context);

        void onApplicationAttachAfter(@NonNull Context context);
    }

    private static final CopyOnWriteArrayList<ApplicationHook> APPLICATION_HOOKS = new CopyOnWriteArrayList<>();
    private static final ConcurrentLinkedDeque<Runnable> HOT_RELOAD_CLEANUPS = new ConcurrentLinkedDeque<>();
    /** 仅保存宿主/系统 classloader 创建的对象，供下一 generation 重建外部注册。 */
    private static final ConcurrentHashMap<String, Object> HOT_RELOAD_RUNTIME_STATE =
        new ConcurrentHashMap<>();
    private static volatile boolean sApplicationHookInstalled = false;

    public static void registerHotReloadCleanup(@NonNull Runnable cleanup) {
        HOT_RELOAD_CLEANUPS.addLast(cleanup);
    }

    /** 为已成功注册的 BroadcastReceiver 添加幂等的热重载注销操作。 */
    public static void registerReceiverHotReloadCleanup(@NonNull Context context,
                                                        @NonNull BroadcastReceiver receiver) {
        registerHotReloadCleanup(() -> {
            try {
                context.unregisterReceiver(receiver);
            } catch (IllegalArgumentException ignored) {
                // 宿主已主动注销时无需将本次热重载判为失败。
            }
        });
    }

    /** 为已成功注册的 ContentObserver 添加幂等的热重载注销操作。 */
    public static void registerContentObserverHotReloadCleanup(@NonNull ContentResolver resolver,
                                                               @NonNull ContentObserver observer) {
        registerHotReloadCleanup(() -> {
            try {
                resolver.unregisterContentObserver(observer);
            } catch (IllegalArgumentException ignored) {
                // 同上。
            }
        });
    }

    /** 为已成功注册的 NetworkCallback 添加幂等的热重载注销操作。 */
    public static void registerNetworkCallbackHotReloadCleanup(
        @NonNull ConnectivityManager manager,
        @NonNull ConnectivityManager.NetworkCallback callback
    ) {
        registerHotReloadCleanup(() -> {
            try {
                manager.unregisterNetworkCallback(callback);
            } catch (IllegalArgumentException ignored) {
                // 同上。
            }
        });
    }

    /** 为已成功注册的 Application 生命周期回调添加幂等的热重载注销操作。 */
    public static void registerActivityLifecycleHotReloadCleanup(
        @NonNull Application application,
        @NonNull Application.ActivityLifecycleCallbacks callback
    ) {
        registerHotReloadCleanup(() -> application.unregisterActivityLifecycleCallbacks(callback));
    }

    /** 为已成功注册的 SensorEventListener 添加热重载注销操作。 */
    public static void registerSensorListenerHotReloadCleanup(
        @NonNull SensorManager manager,
        @NonNull SensorEventListener listener
    ) {
        registerHotReloadCleanup(() -> manager.unregisterListener(listener));
    }

    /** 移除当前模块通过指定 Handler 投递的所有延迟任务。 */
    public static void registerHandlerHotReloadCleanup(@NonNull Handler handler) {
        registerHotReloadCleanup(() -> handler.removeCallbacksAndMessages(null));
    }

    /**
     * 保存下一 generation 可安全复用的宿主状态（例如 Context、View 或系统服务对象）。
     * 不能保存由模块 classloader 创建的对象、模块 lambda 或匿名回调。
     */
    public static void putHotReloadRuntimeState(@NonNull String key, @Nullable Object value) {
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Hot reload runtime state key must not be empty");
        }
        if (value == null) {
            HOT_RELOAD_RUNTIME_STATE.remove(key);
            return;
        }
        ClassLoader moduleClassLoader = BaseHook.class.getClassLoader();
        if (moduleClassLoader != null && value.getClass().getClassLoader() == moduleClassLoader) {
            throw new IllegalArgumentException(
                "Hot reload runtime state must not retain a module-classloader object: "
                    + value.getClass().getName()
            );
        }
        HOT_RELOAD_RUNTIME_STATE.put(key, value);
    }

    @Nullable
    public static <T> T getHotReloadRuntimeState(@NonNull String key, @NonNull Class<T> type) {
        Object value = HOT_RELOAD_RUNTIME_STATE.get(key);
        return type.isInstance(value) ? type.cast(value) : null;
    }

    @NonNull
    static Map<String, Object> snapshotHotReloadRuntimeState() {
        // HashMap 来自 boot classloader；其中内容仍由 libxposed 在 setSavedInstanceState 时校验。
        return new HashMap<>(HOT_RELOAD_RUNTIME_STATE);
    }

    static void restoreHotReloadRuntimeState(@Nullable Object rawState) {
        HOT_RELOAD_RUNTIME_STATE.clear();
        if (!(rawState instanceof Map<?, ?> states)) {
            return;
        }
        for (Map.Entry<?, ?> entry : states.entrySet()) {
            if (!(entry.getKey() instanceof String key) || entry.getValue() == null) {
                continue;
            }
            putHotReloadRuntimeState(key, entry.getValue());
        }
    }

    static void prepareHotReload() {
        Throwable firstFailure = null;
        Runnable cleanup;
        while ((cleanup = HOT_RELOAD_CLEANUPS.pollLast()) != null) {
            try {
                cleanup.run();
            } catch (Throwable t) {
                if (firstFailure == null) {
                    firstFailure = t;
                }
                XposedLog.w(BaseLoad.getTag(), BaseLoad.getPackageName(),
                    "Failed to release a host callback before hot reload", t);
            }
        }
        APPLICATION_HOOKS.clear();
        sApplicationHookInstalled = false;
        HOT_RELOAD_RUNTIME_STATE.clear();
        if (firstFailure != null) {
            throw new IllegalStateException(
                "One or more module-owned host callbacks could not be released", firstFailure
            );
        }
    }

    private static void ensureApplicationHookInstalled() {
        if (sApplicationHookInstalled) return;

        synchronized (BaseHook.class) {
            if (sApplicationHookInstalled) return;
            Hooks.findAndHookMethod(Application.class, "attach", Context.class, new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    Context context = (Context) param.getArgs()[0];
                    for (ApplicationHook hook : APPLICATION_HOOKS) {
                        hook.onApplicationAttachBefore(context);
                    }
                }

                @Override
                public void after(HookParam param) {
                    Context context = (Context) param.getArgs()[0];
                    EzXposed.initAppContext(context, false);
                    for (ApplicationHook hook : APPLICATION_HOOKS) {
                        hook.onApplicationAttachAfter(context);
                    }
                }
            });
            sApplicationHookInstalled = true;
        }
    }

    /**
     * 初始化 Hook，子类实现此方法编写具体 Hook 逻辑
     */
    public abstract void init();

    /**
     * 当前 Hook 是否需要 DexKit。
     * <p>
     * 返回 true 后，框架会在当前包加载期间为该 Hook 准备 DexKit 会话，
     * 并先调用 {@link #initDexKit()}，再调用 {@link #init()}。
     */
    protected boolean useDexKit() {
        return false;
    }

    /**
     * DexKit 初始化阶段。
     * <p>
     * 约定：
     * 1. 只在这里解析 DexKit 成员，不要在 hook 回调里首次触发 DexKit。
     * 2. {@link #init()} 只消费这里解析好的 Method/Field/Class/List。
     * <p>
     * Kotlin 示例：
     * <pre>{@code
     * private lateinit var targetMethod: Method
     *
     * override fun useDexKit() = true
     *
     * override fun initDexKit(): Boolean {
     *     targetMethod = requiredMember("targetMethod") { bridge ->
     *         bridge.findMethod { matcher { name = "target" } }.single()
     *     }
     *     return true
     * }
     * }</pre>
     * Java 示例：
     * <pre>{@code
     * private Method targetMethod;
     *
     * @Override
     * protected boolean useDexKit() {
     *     return true;
     * }
     *
     * @Override
     * protected boolean initDexKit() {
     *     targetMethod = requiredMember("targetMethod",
     *         bridge -> bridge.findMethod(...).single());
     *     return true;
     * }
     * }</pre>
     * <p>
     * 框架会在 {@link BaseLoad} 中自动把一批 Hook 的 {@link #initDexKit()} 并行执行。
     *
     * @return true 表示继续执行 {@link #init()}；false 表示跳过当前 Hook。
     */
    protected boolean initDexKit() {
        return true;
    }

    final void setDexKitInitInProgress(boolean inProgress) {
        mInDexKitInit = inProgress;
    }

    private void ensureDexKitInitPhase(String apiName) {
        if (!mInDexKitInit) {
            throw new IllegalStateException(TAG + ": " + apiName + " can only be used in initDexKit()");
        }
    }

    private String namespacedDexKitKey(@NonNull String key) {
        return getClass().getSimpleName() + "#" + key;
    }

    protected final <T> T requiredMember(@NonNull String key, @NonNull IDexKit finder) {
        ensureDexKitInitPhase("requiredMember");
        T member = DexKit.findMember(namespacedDexKitKey(key), finder);
        if (member == null) {
            throw new IllegalStateException(TAG + ": required DexKit member not found: " + key);
        }
        return member;
    }

    protected final <T> List<T> requiredMemberList(@NonNull String key, @NonNull IDexKitList finder) {
        ensureDexKitInitPhase("requiredMemberList");
        List<T> members = DexKit.findMemberList(namespacedDexKitKey(key), finder);
        if (members == null || members.isEmpty()) {
            throw new IllegalStateException(TAG + ": required DexKit member list not found: " + key);
        }
        return members;
    }

    @Nullable
    protected final <T> T optionalMember(@NonNull String key, @NonNull IDexKit finder) {
        ensureDexKitInitPhase("optionalMember");
        try {
            return DexKit.findMember(namespacedDexKitKey(key), finder);
        } catch (Throwable t) {
            XposedLog.w(BaseLoad.getTag(), getPackageName(), "Optional DexKit member failed: " + key, t);
            return null;
        }
    }

    @NonNull
    protected final <T> List<T> optionalMemberList(@NonNull String key, @NonNull IDexKitList finder) {
        ensureDexKitInitPhase("optionalMemberList");
        try {
            List<T> members = DexKit.findMemberList(namespacedDexKitKey(key), finder);
            return members != null ? members : Collections.emptyList();
        } catch (Throwable t) {
            XposedLog.w(BaseLoad.getTag(), getPackageName(), "Optional DexKit member list failed: " + key, t);
            return Collections.emptyList();
        }
    }

    /**
     * 获取类加载器
     */
    public ClassLoader getClassLoader() {
        if (BaseLoad.getClassLoader() != null) {
            return BaseLoad.getClassLoader();
        }
        return EzXposed.getAppContext().getClassLoader();
    }

    /**
     * 获取包名
     */
    public String getPackageName() {
        return BaseLoad.getPackageName();
    }

    /**
     * 获取包加载参数 (应用)
     */
    public PackageReadyParam getLpparam() {
        return BaseLoad.getLpparam();
    }

    /**
     * 获取包加载参数 (系统框架)
     */
    public SystemServerStartingParam getSystemParam() {
        return BaseLoad.getSystemServerParam();
    }

    /**
     * 获取 Xposed 接口
     */
    public XposedInterface xposed() {
        return BaseLoad.getXposed();
    }

    // ==================== 类查找 ====================

    public static Class<?> findClass(String className) {
        return ClassUtils.loadClass(className, EzXposed.getSafeClassLoader());
    }

    public static Class<?> findClass(String className, ClassLoader classLoader) {
        return ClassUtils.loadClass(className, classLoader != null ? classLoader : EzXposed.getSafeClassLoader());
    }

    public static Class<?> findClassIfExists(String className) {
        return ClassUtils.loadClassOrNull(className, EzXposed.getSafeClassLoader());
    }

    public static Class<?> findClassIfExists(String className, ClassLoader classLoader) {
        return ClassUtils.loadClassOrNull(className, classLoader != null ? classLoader : EzXposed.getSafeClassLoader());
    }

    // ==================== 字段操作 ====================

    /**
     * 获取对象字段值
     */
    public static Object getObjectField(Object obj, String fieldName) {
        return Fields.getObjectField(obj, fieldName);
    }

    /**
     * 设置对象字段值
     */
    public static void setObjectField(Object obj, String fieldName, Object value) {
        Fields.setObjectField(obj, fieldName, value);
    }

    /**
     * 获取静态字段值
     */
    public static Object getStaticObjectField(Class<?> clazz, String fieldName) {
        return Fields.getStaticObjectField(clazz, fieldName);
    }

    /**
     * 设置静态字段值
     */
    public static void setStaticObjectField(Class<?> clazz, String fieldName, Object value) {
        Fields.setStaticObjectField(clazz, fieldName, value);
    }

    public static boolean getBooleanField(Object obj, String fieldName) {
        return Fields.getBooleanField(obj, fieldName);
    }

    public static void setBooleanField(Object obj, String fieldName, boolean value) {
        Fields.setBooleanField(obj, fieldName, value);
    }

    public static int getIntField(Object obj, String fieldName) {
        return Fields.getIntField(obj, fieldName);
    }

    public static void setIntField(Object obj, String fieldName, int value) {
        Fields.setIntField(obj, fieldName, value);
    }

    public static long getLongField(Object obj, String fieldName) {
        return Fields.getLongField(obj, fieldName);
    }

    public static void setLongField(Object obj, String fieldName, long value) {
        Fields.setLongField(obj, fieldName, value);
    }

    public static float getFloatField(Object obj, String fieldName) {
        return Fields.getFloatField(obj, fieldName);
    }

    public static void setFloatField(Object obj, String fieldName, float value) {
        Fields.setFloatField(obj, fieldName, value);
    }

    public static boolean getStaticBooleanField(Class<?> clazz, String fieldName) {
        return Fields.getStaticBooleanField(clazz, fieldName);
    }

    public static void setStaticBooleanField(Class<?> clazz, String fieldName, boolean value) {
        Fields.setStaticBooleanField(clazz, fieldName, value);
    }

    public static int getStaticIntField(Class<?> clazz, String fieldName) {
        return Fields.getStaticIntField(clazz, fieldName);
    }

    public static void setStaticIntField(Class<?> clazz, String fieldName, int value) {
        Fields.setStaticIntField(clazz, fieldName, value);
    }

    public static float getStaticFloatField(Class<?> clazz, String fieldName) {
        return Fields.getStaticFloatField(clazz, fieldName);
    }

    public static Field findField(Class<?> clazz, String fieldName) {
        return Fields.find(clazz).filterByName(fieldName).first();
    }

    @Nullable
    public static Field findFieldIfExists(Class<?> clazz, String fieldName) {
        return Fields.find(clazz).filterByName(fieldName).firstOrNull();
    }

    public static Field findFirstFieldByExactType(Class<?> clazz, Class<?> type) {
        return Fields.find(clazz).filterByType(type).first();
    }

    public static Object setAdditionalInstanceField(Object obj, String key, Object value) {
        return ExtraFields.setInstanceField(obj, key, value);
    }

    public static Object getAdditionalInstanceField(Object obj, String key) {
        return ExtraFields.getInstanceField(obj, key);
    }

    public static Object removeAdditionalInstanceField(Object obj, String key) {
        return ExtraFields.removeInstanceField(obj, key);
    }

    public static Object getSurroundingThis(Object obj) {
        return getObjectField(obj, "this$0");
    }

    // ==================== 方法调用 ====================

    /**
     * 调用对象方法
     */
    public static Object callMethod(Object obj, String methodName, Object... args) {
        return Methods.callMethod(obj, methodName, args);
    }

    /**
     * 调用静态方法
     */
    public static Object callStaticMethod(Class<?> clazz, String methodName, Object... args) {
        return Methods.callStaticMethod(clazz, methodName, args);
    }

    public static Object newInstance(Class<?> clazz, Object... args) {
        return Constructors.newInstance(clazz, args);
    }

    public static Method findMethodBestMatch(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        return BestMatchUtils.findMethodBestMatch(clazz, methodName, parameterTypes);
    }

    public static Method findMethodBestMatch(Class<?> clazz, String methodName, Object... args) {
        return BestMatchUtils.findMethodBestMatch(clazz, methodName, args);
    }

    @Nullable
    public static Method findMethodExactIfExists(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        return Methods.find(clazz).filterByName(methodName).filterByParamTypes(parameterTypes).firstOrNull();
    }

    @Nullable
    public static Method findMethodExactIfExists(Class<?> clazz, String methodName, Object... parameterTypes) {
        return findMethodExactIfExists(clazz, methodName, resolveTypes(clazz, parameterTypes));
    }

    @Nullable
    public static Method findMethodExactIfExists(
        String className,
        ClassLoader classLoader,
        String methodName,
        Object... parameterTypes
    ) {
        Class<?> clazz = ClassUtils.loadClassOrNull(className, classLoader);
        if (clazz == null) return null;
        return findMethodExactIfExists(clazz, methodName, resolveTypes(clazz, parameterTypes));
    }

    public static Method[] findMethodsByExactParameters(
        Class<?> clazz,
        @Nullable Class<?> returnType,
        Class<?>... parameterTypes
    ) {
        List<Method> methods = Methods.find(clazz)
            .filterByParamTypes(parameterTypes)
            .filter(method -> returnType == null || method.getReturnType() == returnType)
            .toList();
        return methods.toArray(new Method[0]);
    }

    public static Constructor<?> findConstructorExact(Class<?> clazz, Class<?>... parameterTypes) {
        try {
            Constructor<?> constructor = clazz.getDeclaredConstructor(parameterTypes);
            constructor.setAccessible(true);
            return constructor;
        } catch (NoSuchMethodException e) {
            throw new NoSuchMethodError(e.getMessage());
        }
    }

    public static Object invokeOriginalMethod(Method method, Object thisObject, Object... args) {
        try {
            method.setAccessible(true);
            return method.invoke(thisObject, args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Object invokeSuperMethod(String methodName, Object thisObject, Object... args) {
        Class<?> superClass = thisObject.getClass().getSuperclass();
        if (superClass == null) {
            throw new NoSuchMethodError(methodName);
        }
        try {
            Method method = BestMatchUtils.findMethodBestMatch(superClass, methodName, args);
            method.setAccessible(true);
            return method.invoke(thisObject, args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    public static boolean deoptimize(Method method) {
        return Deoptimizers.deoptimize(method);
    }

    public static void deoptimizeMethods(Class<?> clazz, String... names) {
        var list = Arrays.asList(names);
        Arrays.stream(clazz.getDeclaredMethods())
            .filter(method -> list.contains(method.getName()))
            .forEach(BaseHook::deoptimize);
    }

    private static Class<?>[] resolveTypes(Class<?> owner, Object[] types) {
        Class<?>[] resolved = new Class<?>[types.length];
        ClassLoader classLoader = owner.getClassLoader() != null ? owner.getClassLoader() : EzXposed.getSafeClassLoader();
        for (int i = 0; i < types.length; i++) {
            Object type = types[i];
            if (type instanceof Class<?> clazz) {
                resolved[i] = clazz;
            } else if (type instanceof String className) {
                resolved[i] = ClassUtils.loadClass(className, classLoader);
            } else {
                throw new IllegalArgumentException("Parameter type must be Class or class name String: " + type);
            }
        }
        return resolved;
    }

    // ==================== Application 生命周期 ====================

    /**
     * Application.attach(Context) 之前调用
     * 子类可重写此方法
     */
    protected void onApplicationAttachBefore(@NonNull Context context) {
    }

    /**
     * Application.attach(Context) 之后调用
     * 子类可重写此方法
     */
    protected void onApplicationAttachAfter(@NonNull Context context) {
    }

    /**
     * 注册当前 Hook 的 Application 生命周期回调
     */
    protected void registerApplicationHook() {
        registerApplicationHook(new ContextConsumer() {
            @Override
            public void accept(@NonNull Context context) {
                BaseHook.this.onApplicationAttachBefore(context);
            }
        }, new ContextConsumer() {
            @Override
            public void accept(@NonNull Context context) {
                BaseHook.this.onApplicationAttachAfter(context);
            }
        });
    }

    /**
     * 注册 Application attach 之后的回调
     */
    public static void runOnApplicationAttach(ContextConsumer callback) {
        registerApplicationHook(null, callback);
    }

    /**
     * 注册 Application 生命周期回调
     */
    public static void registerApplicationHook(
        @Nullable ContextConsumer before,
        @Nullable ContextConsumer after
    ) {
        APPLICATION_HOOKS.add(new ApplicationHook() {
            @Override
            public void onApplicationAttachBefore(@NonNull Context context) {
                if (before != null) before.accept(context);
            }

            @Override
            public void onApplicationAttachAfter(@NonNull Context context) {
                if (after != null) after.accept(context);
            }
        });
        ensureApplicationHookInstalled();

        Context context = EzXposed.getAppContextOrNull();
        if (context != null && after != null) {
            after.accept(context);
        }
    }

    // ==================== Hook 方法 ====================

    /**
     * Hook 方法
     *
     * @param method   要 Hook 的方法
     * @param callback Hook 回调
     * @return HookHandle 对象
     */
    public static XposedInterface.HookHandle hookMethod(Method method, IMethodHook callback) {
        return Hooks.createHook(method, callback);
    }

    public static XposedInterface.HookHandle hookMethod(Method method, IReplaceHook callback) {
        return Hooks.createHook(method, callback);
    }

    public static XposedInterface.HookHandle chain(Method method, XposedInterface.Hooker hooker) {
        return Hooks.intercept(method, hooker);
    }

    public static XposedInterface.HookHandle chain(
        Method method,
        int priority,
        XposedInterface.ExceptionMode exceptionMode,
        XposedInterface.Hooker hooker
    ) {
        return Hooks.intercept(method, hooker);
    }

    public static XposedInterface.HookHandle chain(Constructor<?> constructor, XposedInterface.Hooker hooker) {
        return Hooks.intercept(constructor, hooker);
    }

    public static XposedInterface.HookHandle chain(
        Constructor<?> constructor,
        int priority,
        XposedInterface.ExceptionMode exceptionMode,
        XposedInterface.Hooker hooker
    ) {
        return Hooks.intercept(constructor, hooker);
    }

    /**
     * 查找并 Chain Hook 方法
     * <p>
     * 最后一个参数必须是 {@link XposedInterface.Hooker}，前面的参数是参数类型
     */
    public static XposedInterface.HookHandle findAndChainMethod(Class<?> clazz, String methodName, Object... args) {
        return Hooks.findAndHookMethod(clazz, methodName, args);
    }

    public static XposedInterface.HookHandle findAndChainMethod(
        Class<?> clazz,
        String methodName,
        int priority,
        XposedInterface.ExceptionMode exceptionMode,
        Object... args
    ) {
        return Hooks.findAndHookMethod(clazz, methodName, args);
    }

    public static XposedInterface.HookHandle findAndChainMethod(
        Class<?> clazz,
        String methodName,
        XposedInterface.Hooker hooker,
        Object... args
    ) {
        Object[] argsAndHook = Arrays.copyOf(args, args.length + 1);
        argsAndHook[args.length] = hooker;
        return Hooks.findAndHookMethod(clazz, methodName, argsAndHook);
    }

    public static XposedInterface.HookHandle findAndChainMethod(
        Class<?> clazz,
        String methodName,
        int priority,
        XposedInterface.ExceptionMode exceptionMode,
        XposedInterface.Hooker hooker,
        Object... args
    ) {
        Object[] argsAndHook = Arrays.copyOf(args, args.length + 1);
        argsAndHook[args.length] = hooker;
        return Hooks.findAndHookMethod(clazz, methodName, argsAndHook);
    }

    public static XposedInterface.HookHandle findAndChainMethod(String className, String methodName, Object... args) {
        Class<?> clazz = findClassIfExists(className);
        if (clazz == null) {
            XposedLog.w(BaseLoad.getTag(), "findAndChainMethod: class not found: " + className);
            return null;
        }
        return Hooks.findAndHookMethod(clazz, methodName, args);
    }

    public static XposedInterface.HookHandle findAndChainMethod(
        String className,
        String methodName,
        int priority,
        XposedInterface.ExceptionMode exceptionMode,
        Object... args
    ) {
        Class<?> clazz = findClassIfExists(className);
        if (clazz == null) {
            XposedLog.w(BaseLoad.getTag(), "findAndChainMethod: class not found: " + className);
            return null;
        }
        return Hooks.findAndHookMethod(clazz, methodName, args);
    }

    public static XposedInterface.HookHandle findAndChainMethod(
        String className,
        String methodName,
        XposedInterface.Hooker hooker,
        Object... args
    ) {
        Class<?> clazz = findClassIfExists(className);
        if (clazz == null) {
            XposedLog.w(BaseLoad.getTag(), "findAndChainMethod: class not found: " + className);
            return null;
        }
        Object[] argsAndHook = Arrays.copyOf(args, args.length + 1);
        argsAndHook[args.length] = hooker;
        return Hooks.findAndHookMethod(clazz, methodName, argsAndHook);
    }

    public static XposedInterface.HookHandle findAndChainMethod(
        String className,
        String methodName,
        int priority,
        XposedInterface.ExceptionMode exceptionMode,
        XposedInterface.Hooker hooker,
        Object... args
    ) {
        Class<?> clazz = findClassIfExists(className);
        if (clazz == null) {
            XposedLog.w(BaseLoad.getTag(), "findAndChainMethod: class not found: " + className);
            return null;
        }
        Object[] argsAndHook = Arrays.copyOf(args, args.length + 1);
        argsAndHook[args.length] = hooker;
        return Hooks.findAndHookMethod(clazz, methodName, argsAndHook);
    }

    /**
     * 查找并 Chain Hook 构造器
     * <p>
     * 最后一个参数必须是 {@link XposedInterface.Hooker}，前面的参数是参数类型
     */
    public static XposedInterface.HookHandle findAndChainConstructor(Class<?> clazz, Object... args) {
        return Hooks.findAndHookConstructor(clazz, args);
    }

    public static XposedInterface.HookHandle findAndChainConstructor(
        Class<?> clazz,
        int priority,
        XposedInterface.ExceptionMode exceptionMode,
        Object... args
    ) {
        return Hooks.findAndHookConstructor(clazz, args);
    }

    public static XposedInterface.HookHandle findAndChainConstructor(
        Class<?> clazz,
        XposedInterface.Hooker hooker,
        Object... args
    ) {
        Object[] argsAndHook = Arrays.copyOf(args, args.length + 1);
        argsAndHook[args.length] = hooker;
        return Hooks.findAndHookConstructor(clazz, argsAndHook);
    }

    public static XposedInterface.HookHandle findAndChainConstructor(
        Class<?> clazz,
        int priority,
        XposedInterface.ExceptionMode exceptionMode,
        XposedInterface.Hooker hooker,
        Object... args
    ) {
        Object[] argsAndHook = Arrays.copyOf(args, args.length + 1);
        argsAndHook[args.length] = hooker;
        return Hooks.findAndHookConstructor(clazz, argsAndHook);
    }

    public static XposedInterface.HookHandle findAndChainConstructor(String className, Object... args) {
        Class<?> clazz = findClassIfExists(className);
        if (clazz == null) {
            XposedLog.w(BaseLoad.getTag(), "findAndChainConstructor: class not found: " + className);
            return null;
        }
        return Hooks.findAndHookConstructor(clazz, args);
    }

    public static XposedInterface.HookHandle findAndChainConstructor(
        String className,
        int priority,
        XposedInterface.ExceptionMode exceptionMode,
        Object... args
    ) {
        Class<?> clazz = findClassIfExists(className);
        if (clazz == null) {
            XposedLog.w(BaseLoad.getTag(), "findAndChainConstructor: class not found: " + className);
            return null;
        }
        return Hooks.findAndHookConstructor(clazz, args);
    }

    public static XposedInterface.HookHandle findAndChainConstructor(
        String className,
        XposedInterface.Hooker hooker,
        Object... args
    ) {
        Class<?> clazz = findClassIfExists(className);
        if (clazz == null) {
            XposedLog.w(BaseLoad.getTag(), "findAndChainConstructor: class not found: " + className);
            return null;
        }
        Object[] argsAndHook = Arrays.copyOf(args, args.length + 1);
        argsAndHook[args.length] = hooker;
        return Hooks.findAndHookConstructor(clazz, argsAndHook);
    }

    public static XposedInterface.HookHandle findAndChainConstructor(
        String className,
        int priority,
        XposedInterface.ExceptionMode exceptionMode,
        XposedInterface.Hooker hooker,
        Object... args
    ) {
        Class<?> clazz = findClassIfExists(className);
        if (clazz == null) {
            XposedLog.w(BaseLoad.getTag(), "findAndChainConstructor: class not found: " + className);
            return null;
        }
        Object[] argsAndHook = Arrays.copyOf(args, args.length + 1);
        argsAndHook[args.length] = hooker;
        return Hooks.findAndHookConstructor(clazz, argsAndHook);
    }

    public static Set<XposedInterface.HookHandle> chainAllMethods(Class<?> clazz, String methodName, XposedInterface.Hooker hooker) {
        Set<XposedInterface.HookHandle> handles = new LinkedHashSet<>();
        for (Method method : Methods.find(clazz).filterByName(methodName).toList()) {
            handles.add(Hooks.intercept(method, hooker));
        }
        return handles;
    }

    public static Set<XposedInterface.HookHandle> chainAllMethods(String className, String methodName, XposedInterface.Hooker hooker) {
        Class<?> clazz = findClassIfExists(className);
        if (clazz == null) {
            XposedLog.w(BaseLoad.getTag(), "chainAllMethods: class not found: " + className);
            return java.util.Collections.emptySet();
        }
        return chainAllMethods(clazz, methodName, hooker);
    }

    public static Set<XposedInterface.HookHandle> chainAllConstructors(Class<?> clazz, XposedInterface.Hooker hooker) {
        Set<XposedInterface.HookHandle> handles = new LinkedHashSet<>();
        for (Constructor<?> constructor : Constructors.find(clazz).toList()) {
            handles.add(Hooks.intercept(constructor, hooker));
        }
        return handles;
    }

    public static Set<XposedInterface.HookHandle> chainAllConstructors(String className, XposedInterface.Hooker hooker) {
        Class<?> clazz = findClassIfExists(className);
        if (clazz == null) {
            XposedLog.w(BaseLoad.getTag(), "chainAllConstructors: class not found: " + className);
            return java.util.Collections.emptySet();
        }
        return chainAllConstructors(clazz, hooker);
    }

    /**
     * 查找并 Hook 方法
     * <p>
     * 最后一个参数必须是 MethodHook，前面的参数是参数类型
     *
     * @param clazz      目标类
     * @param methodName 方法名
     * @param args       参数类型 + MethodHook 回调
     * @return HookHandle 对象
     */
    public static XposedInterface.HookHandle findAndHookMethod(Class<?> clazz, String methodName, Object... args) {
        return Hooks.findAndHookMethod(clazz, methodName, args);
    }

    /**
     * 查找并 Hook 方法
     * <p>
     * 最后一个参数必须是 MethodHook，前面的参数是参数类型
     *
     * @param className  类名
     * @param methodName 方法名
     * @param args       参数类型 + MethodHook 回调
     * @return HookHandle 对象
     */
    public static XposedInterface.HookHandle findAndHookMethod(String className, String methodName, Object... args) {
        Class<?> clazz = findClassIfExists(className);
        if (clazz == null) {
            XposedLog.w(BaseLoad.getTag(), "findAndHookMethod: class not found: " + className);
            return null;
        }
        return Hooks.findAndHookMethod(clazz, methodName, args);
    }

    public static XposedInterface.HookHandle findAndHookMethod(
        String className,
        ClassLoader classLoader,
        String methodName,
        Object... args
    ) {
        Class<?> clazz = findClassIfExists(className, classLoader);
        if (clazz == null) {
            XposedLog.w(BaseLoad.getTag(), "findAndHookMethod: class not found: " + className);
            return null;
        }
        return Hooks.findAndHookMethod(clazz, methodName, args);
    }

    public static XposedInterface.HookHandle findAndHookConstructor(Class<?> clazz, Object... args) {
        return Hooks.findAndHookConstructor(clazz, args);
    }

    public static XposedInterface.HookHandle findAndHookConstructor(String className, Object... args) {
        Class<?> clazz = findClassIfExists(className);
        if (clazz == null) {
            XposedLog.w(BaseLoad.getTag(), "findAndHookConstructor: class not found: " + className);
            return null;
        }
        return Hooks.findAndHookConstructor(clazz, args);
    }

    public static XposedInterface.HookHandle findAndHookConstructor(
        String className,
        ClassLoader classLoader,
        Object... args
    ) {
        Class<?> clazz = findClassIfExists(className, classLoader);
        if (clazz == null) {
            XposedLog.w(BaseLoad.getTag(), "findAndHookConstructor: class not found: " + className);
            return null;
        }
        return Hooks.findAndHookConstructor(clazz, args);
    }

    /**
     * 查找并 Hook 方法
     * <p>
     * 最后一个参数必须是 MethodHook，前面的参数是参数类型
     *
     * @param clazz      目标类
     * @param methodName 方法名
     * @param args       参数类型 + MethodHook 回调
     * @return HookHandle 对象
     */
    public static XposedInterface.HookHandle findAndReplaceMethod(Class<?> clazz, String methodName, Object... args) {
        return Hooks.findAndHookMethod(clazz, methodName, args);
    }

    public static XposedInterface.HookHandle findAndHookMethodReplace(Class<?> clazz, String methodName, Object... args) {
        return findAndReplaceMethod(clazz, methodName, args);
    }

    /**
     * 查找并 Hook 方法
     * <p>
     * 最后一个参数必须是 MethodHook，前面的参数是参数类型
     *
     * @param className  类名
     * @param methodName 方法名
     * @param args       参数类型 + MethodHook 回调
     * @return HookHandle 对象
     */
    public static XposedInterface.HookHandle findAndReplaceMethod(String className, String methodName, Object... args) {
        Class<?> clazz = findClassIfExists(className);
        if (clazz == null) {
            XposedLog.w(BaseLoad.getTag(), "findAndReplaceMethod: class not found: " + className);
            return null;
        }
        return Hooks.findAndHookMethod(clazz, methodName, args);
    }

    public static XposedInterface.HookHandle findAndHookMethodReplace(String className, String methodName, Object... args) {
        return findAndReplaceMethod(className, methodName, args);
    }

    public static XposedInterface.HookHandle findAndHookMethodReplace(
        String className,
        ClassLoader classLoader,
        String methodName,
        Object... args
    ) {
        Class<?> clazz = findClassIfExists(className, classLoader);
        if (clazz == null) {
            XposedLog.w(BaseLoad.getTag(), "findAndReplaceMethod: class not found: " + className);
            return null;
        }
        return Hooks.findAndHookMethod(clazz, methodName, args);
    }

    /**
     * Hook 类中所有指定名称的方法
     *
     * @param clazz      目标类
     * @param methodName 方法名
     * @param callback   Hook 回调
     * @return HookHandle 对象列表
     */
    public static List<XposedInterface.HookHandle> hookAllMethods(Class<?> clazz, String methodName, IMethodHook callback) {
        return Hooks.createHooks(Methods.find(clazz).filterByName(methodName).toList(), callback);
    }

    /**
     * Hook 类中所有指定名称的方法（通过类名）
     *
     * @param className  类名
     * @param methodName 方法名
     * @param callback   Hook 回调
     * @return HookHandle 对象列表
     */
    public static List<XposedInterface.HookHandle> hookAllMethods(String className, String methodName, IMethodHook callback) {
        Class<?> clazz = findClassIfExists(className);
        if (clazz == null) {
            XposedLog.w(BaseLoad.getTag(), "hookAllMethods: class not found: " + className);
            return java.util.Collections.emptyList();
        }
        return hookAllMethods(clazz, methodName, callback);
    }

    public static List<XposedInterface.HookHandle> hookAllMethods(
        String className,
        @Nullable ClassLoader classLoader,
        String methodName,
        IMethodHook callback
    ) {
        Class<?> clazz = classLoader == null ? findClassIfExists(className) : findClassIfExists(className, classLoader);
        if (clazz == null) {
            XposedLog.w(BaseLoad.getTag(), "hookAllMethods: class not found: " + className);
            return java.util.Collections.emptyList();
        }
        return hookAllMethods(clazz, methodName, callback);
    }

    /**
     * Hook 类中所有构造器
     *
     * @param clazz    目标类
     * @param callback Hook 回调
     * @return HookHandle 对象列表
     */
    public static List<XposedInterface.HookHandle> hookAllConstructors(Class<?> clazz, IMethodHook callback) {
        return Hooks.createConstructorHooks(Constructors.find(clazz).toList(), callback);
    }

    /**
     * Hook 类中所有构造器
     *
     * @param className    类名
     * @param callback Hook 回调
     * @return HookHandle 对象列表
     */
    public static List<XposedInterface.HookHandle> hookAllConstructors(String className, IMethodHook callback) {
        Class<?> clazz = findClassIfExists(className);
        if (clazz == null) {
            XposedLog.w(BaseLoad.getTag(), "hookAllConstructors: class not found: " + className);
            return java.util.Collections.emptyList();
        }
        return hookAllConstructors(clazz, callback);
    }

    // ==================== 便捷 Hook 工具 ====================

    /**
     * 创建一个返回常量值的 Hook 回调
     */
    public static IMethodHook returnConstant(Object result) {
        return new IMethodHook() {
            @Override
            public void before(HookParam param) {
                param.setResult(result);
            }
        };
    }

    /**
     * 获取阻止原方法执行的 Hook（返回 null）
     */
    public static IMethodHook doNothing() {
        return returnConstant(null);
    }

    public Object proxySystemProperties(String method, String prop, int val, ClassLoader classLoader) {
        return callStaticMethod(findClassIfExists("android.os.SystemProperties", classLoader),
            method, prop, val);
    }

    /**
     * 仅供开发调试时使用的 callback 错误日志。
     * <p>
     * 使用 warn 级别，仅在详细日志模式下输出，避免常规使用时制造额外噪音。
     * <p>
     * 用法示例：
     * <pre>{@code
     * debugCallbackError("after updateClock", t);
     * }</pre>
     */
    protected final void debugCallbackError(String where, Throwable t) {
        String message = (where == null || where.isEmpty())
            ? "Debug callback error"
            : "Debug callback error at " + where;
        XposedLog.w(BaseLoad.getTag(), getPackageName(), message, t);
    }

    /**
     * 仅供开发调试时使用。
     * <p>
     * 记录 callback 异常后吞掉，适合 before/after 这类不希望影响主流程的场景。
     * <p>
     * 用法示例：
     * <pre>{@code
     * @Override
     * public void after(HookParam param) {
     *     debugProtect("after updateClock", () -> {
     *         Object result = param.getResult();
     *         param.setResult(result);
     *     });
     * }
     * }</pre>
     */
    protected final void debugProtect(String where, ThrowableRunnable action) {
        try {
            action.run();
        } catch (Throwable t) {
            debugCallbackError(where, t);
        }
    }

    /**
     * 仅供开发调试时使用。
     * <p>
     * 记录 callback 异常后继续抛出，适合 replace 或其他需要保留原始异常语义的场景。
     * <p>
     * 用法示例：
     * <pre>{@code
     * @Override
     * public void before(HookParam param) throws Throwable {
     *     debugRethrow("before verifySignature", () -> {
     *         throw new IllegalStateException("debug");
     *     });
     * }
     * }</pre>
     */
    protected final void debugRethrow(String where, ThrowableRunnable action) throws Throwable {
        try {
            action.run();
        } catch (Throwable t) {
            debugCallbackError(where, t);
            throw t;
        }
    }

    /**
     * 仅供开发调试时使用。
     * <p>
     * 记录 callback 异常后继续抛出，并返回回调结果。
     * <p>
     * 用法示例：
     * <pre>{@code
     * @Override
     * public Object replace(HookParam param) throws Throwable {
     *     return debugRethrow("replace buildIntent", () -> {
     *         return param.getResult();
     *     });
     * }
     * }</pre>
     */
    protected final <T> T debugRethrow(String where, ThrowableSupplier<T> action) throws Throwable {
        try {
            return action.get();
        } catch (Throwable t) {
            debugCallbackError(where, t);
            throw t;
        }
    }

    // ==================== 资源 Hook ====================

    /**
     * 获取虚拟资源 ID
     */
    public static int getFakeResId(String resourceName) {
        return ResourcesTool.getFakeResId(resourceName);
    }

    /**
     * 设置资源替换
     */
    public static void setResReplacement(String pkg, String type, String name, int replacementResId) {
        ResourcesTool resTool = ResourcesTool.getInstance();
        if (resTool != null) {
            resTool.setResReplacement(pkg, type, name, replacementResId);
        }
    }

    /**
     * 设置密度资源替换
     */
    public static void setDensityReplacement(String pkg, String type, String name, float replacementResValue) {
        ResourcesTool resTool = ResourcesTool.getInstance();
        if (resTool != null) {
            resTool.setDensityReplacement(pkg, type, name, replacementResValue);
        }
    }

    /**
     * 设置对象资源替换
     */
    public static void setObjectReplacement(String pkg, String type, String name, Object replacementResValue) {
        ResourcesTool resTool = ResourcesTool.getInstance();
        if (resTool != null) {
            resTool.setObjectReplacement(pkg, type, name, replacementResValue);
        }
    }

}
