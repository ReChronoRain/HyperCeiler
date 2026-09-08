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

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.DexKit;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.IDexKit;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.IDexKitList;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.ResourcesTool;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import io.github.kyuubiran.ezxhelper.xposed.EzXposed;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam;

/**
 * Hook 基类
 * <p>
 * 提供 Java 常用的 Hook 工具方法
 * Kotlin 建议直接使用 ezxhelper API
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
            XposedLog.w(TAG, getPackageName(), "Optional DexKit member failed: " + key, t);
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
            XposedLog.w(TAG, getPackageName(), "Optional DexKit member list failed: " + key, t);
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

    public Class<?> findClass(String className) {
        return EzxHelpUtils.findClass(className, getClassLoader());
    }

    public Class<?> findClass(String className, ClassLoader classLoader) {
        return EzxHelpUtils.findClass(className, classLoader);
    }

    public Class<?> findClassIfExists(String className) {
        return EzxHelpUtils.findClassIfExists(className, getClassLoader());
    }

    public Class<?> findClassIfExists(String className, ClassLoader classLoader) {
        return EzxHelpUtils.findClassIfExists(className, classLoader);
    }

    // ==================== 字段操作 ====================

    /**
     * 获取对象字段值
     */
    public static Object getObjectField(Object obj, String fieldName) {
        return EzxHelpUtils.getObjectField(obj, fieldName);
    }

    /**
     * 设置对象字段值
     */
    public static void setObjectField(Object obj, String fieldName, Object value) {
        EzxHelpUtils.setObjectField(obj, fieldName, value);
    }

    /**
     * 获取静态字段值
     */
    public static Object getStaticObjectField(Class<?> clazz, String fieldName) {
        return EzxHelpUtils.getStaticObjectField(clazz, fieldName);
    }

    /**
     * 设置静态字段值
     */
    public static void setStaticObjectField(Class<?> clazz, String fieldName, Object value) {
        EzxHelpUtils.setStaticObjectField(clazz, fieldName, value);
    }

    // ==================== 方法调用 ====================

    /**
     * 调用对象方法
     */
    public static Object callMethod(Object obj, String methodName, Object... args) {
        return EzxHelpUtils.callMethod(obj, methodName, args);
    }

    /**
     * 调用静态方法
     */
    public static Object callStaticMethod(Class<?> clazz, String methodName, Object... args) {
        return EzxHelpUtils.callStaticMethod(clazz, methodName, args);
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
        EzxHelpUtils.registerApplicationHook(new EzxHelpUtils.IApplicationHook() {
            @Override
            public void onApplicationAttachBefore(@NonNull Context context) {
                BaseHook.this.onApplicationAttachBefore(context);
            }

            @Override
            public void onApplicationAttachAfter(@NonNull Context context) {
                BaseHook.this.onApplicationAttachAfter(context);
            }
        });
    }

    /**
     * 注册 Application attach 之后的回调
     */
    protected void runOnApplicationAttach(EzxHelpUtils.ContextConsumer callback) {
        EzxHelpUtils.runOnApplicationAttach(callback);
    }

    /**
     * 注册 Application 生命周期回调
     */
    protected void registerApplicationHook(
        @Nullable EzxHelpUtils.ContextConsumer before,
        @Nullable EzxHelpUtils.ContextConsumer after
    ) {
        EzxHelpUtils.registerApplicationHook(before, after);
    }

    // ==================== Hook 方法 ====================

    /**
     * Hook 方法
     *
     * @param method   要 Hook 的方法
     * @param callback Hook 回调
     * @return HookHandle 对象
     */
    public XposedInterface.HookHandle hookMethod(Method method, IMethodHook callback) {
        return EzxHelpUtils.hookMethod(method, callback);
    }

    public XposedInterface.HookHandle chain(Method method, XposedInterface.Hooker hooker) {
        return EzxHelpUtils.chain(method, hooker);
    }

    public XposedInterface.HookHandle chain(
        Method method,
        int priority,
        XposedInterface.ExceptionMode exceptionMode,
        XposedInterface.Hooker hooker
    ) {
        return EzxHelpUtils.chain(method, priority, exceptionMode, hooker);
    }

    public XposedInterface.HookHandle chain(Constructor<?> constructor, XposedInterface.Hooker hooker) {
        return EzxHelpUtils.chain(constructor, hooker);
    }

    public XposedInterface.HookHandle chain(
        Constructor<?> constructor,
        int priority,
        XposedInterface.ExceptionMode exceptionMode,
        XposedInterface.Hooker hooker
    ) {
        return EzxHelpUtils.chain(constructor, priority, exceptionMode, hooker);
    }

    /**
     * 查找并 Chain Hook 方法
     * <p>
     * 最后一个参数必须是 {@link XposedInterface.Hooker}，前面的参数是参数类型
     */
    public XposedInterface.HookHandle findAndChainMethod(Class<?> clazz, String methodName, Object... args) {
        return EzxHelpUtils.findAndChainMethod(clazz, methodName, args);
    }

    public XposedInterface.HookHandle findAndChainMethod(
        Class<?> clazz,
        String methodName,
        int priority,
        XposedInterface.ExceptionMode exceptionMode,
        Object... args
    ) {
        return EzxHelpUtils.findAndChainMethod(clazz, methodName, priority, exceptionMode, args);
    }

    public XposedInterface.HookHandle findAndChainMethod(
        Class<?> clazz,
        String methodName,
        XposedInterface.Hooker hooker,
        Object... args
    ) {
        Object[] argsAndHook = Arrays.copyOf(args, args.length + 1);
        argsAndHook[args.length] = hooker;
        return EzxHelpUtils.findAndChainMethod(clazz, methodName, argsAndHook);
    }

    public XposedInterface.HookHandle findAndChainMethod(
        Class<?> clazz,
        String methodName,
        int priority,
        XposedInterface.ExceptionMode exceptionMode,
        XposedInterface.Hooker hooker,
        Object... args
    ) {
        Object[] argsAndHook = Arrays.copyOf(args, args.length + 1);
        argsAndHook[args.length] = hooker;
        return EzxHelpUtils.findAndChainMethod(clazz, methodName, priority, exceptionMode, argsAndHook);
    }

    public XposedInterface.HookHandle findAndChainMethod(String className, String methodName, Object... args) {
        Class<?> clazz = findClassIfExists(className);
        if (clazz == null) {
            XposedLog.w(TAG, "findAndChainMethod: class not found: " + className);
            return null;
        }
        return EzxHelpUtils.findAndChainMethod(clazz, methodName, args);
    }

    public XposedInterface.HookHandle findAndChainMethod(
        String className,
        String methodName,
        int priority,
        XposedInterface.ExceptionMode exceptionMode,
        Object... args
    ) {
        Class<?> clazz = findClassIfExists(className);
        if (clazz == null) {
            XposedLog.w(TAG, "findAndChainMethod: class not found: " + className);
            return null;
        }
        return EzxHelpUtils.findAndChainMethod(clazz, methodName, priority, exceptionMode, args);
    }

    public XposedInterface.HookHandle findAndChainMethod(
        String className,
        String methodName,
        XposedInterface.Hooker hooker,
        Object... args
    ) {
        Class<?> clazz = findClassIfExists(className);
        if (clazz == null) {
            XposedLog.w(TAG, "findAndChainMethod: class not found: " + className);
            return null;
        }
        Object[] argsAndHook = Arrays.copyOf(args, args.length + 1);
        argsAndHook[args.length] = hooker;
        return EzxHelpUtils.findAndChainMethod(clazz, methodName, argsAndHook);
    }

    public XposedInterface.HookHandle findAndChainMethod(
        String className,
        String methodName,
        int priority,
        XposedInterface.ExceptionMode exceptionMode,
        XposedInterface.Hooker hooker,
        Object... args
    ) {
        Class<?> clazz = findClassIfExists(className);
        if (clazz == null) {
            XposedLog.w(TAG, "findAndChainMethod: class not found: " + className);
            return null;
        }
        Object[] argsAndHook = Arrays.copyOf(args, args.length + 1);
        argsAndHook[args.length] = hooker;
        return EzxHelpUtils.findAndChainMethod(clazz, methodName, priority, exceptionMode, argsAndHook);
    }

    /**
     * 查找并 Chain Hook 构造器
     * <p>
     * 最后一个参数必须是 {@link XposedInterface.Hooker}，前面的参数是参数类型
     */
    public XposedInterface.HookHandle findAndChainConstructor(Class<?> clazz, Object... args) {
        return EzxHelpUtils.findAndChainConstructor(clazz, args);
    }

    public XposedInterface.HookHandle findAndChainConstructor(
        Class<?> clazz,
        int priority,
        XposedInterface.ExceptionMode exceptionMode,
        Object... args
    ) {
        return EzxHelpUtils.findAndChainConstructor(clazz, priority, exceptionMode, args);
    }

    public XposedInterface.HookHandle findAndChainConstructor(
        Class<?> clazz,
        XposedInterface.Hooker hooker,
        Object... args
    ) {
        Object[] argsAndHook = Arrays.copyOf(args, args.length + 1);
        argsAndHook[args.length] = hooker;
        return EzxHelpUtils.findAndChainConstructor(clazz, argsAndHook);
    }

    public XposedInterface.HookHandle findAndChainConstructor(
        Class<?> clazz,
        int priority,
        XposedInterface.ExceptionMode exceptionMode,
        XposedInterface.Hooker hooker,
        Object... args
    ) {
        Object[] argsAndHook = Arrays.copyOf(args, args.length + 1);
        argsAndHook[args.length] = hooker;
        return EzxHelpUtils.findAndChainConstructor(clazz, priority, exceptionMode, argsAndHook);
    }

    public XposedInterface.HookHandle findAndChainConstructor(String className, Object... args) {
        Class<?> clazz = findClassIfExists(className);
        if (clazz == null) {
            XposedLog.w(TAG, "findAndChainConstructor: class not found: " + className);
            return null;
        }
        return EzxHelpUtils.findAndChainConstructor(clazz, args);
    }

    public XposedInterface.HookHandle findAndChainConstructor(
        String className,
        int priority,
        XposedInterface.ExceptionMode exceptionMode,
        Object... args
    ) {
        Class<?> clazz = findClassIfExists(className);
        if (clazz == null) {
            XposedLog.w(TAG, "findAndChainConstructor: class not found: " + className);
            return null;
        }
        return EzxHelpUtils.findAndChainConstructor(clazz, priority, exceptionMode, args);
    }

    public XposedInterface.HookHandle findAndChainConstructor(
        String className,
        XposedInterface.Hooker hooker,
        Object... args
    ) {
        Class<?> clazz = findClassIfExists(className);
        if (clazz == null) {
            XposedLog.w(TAG, "findAndChainConstructor: class not found: " + className);
            return null;
        }
        Object[] argsAndHook = Arrays.copyOf(args, args.length + 1);
        argsAndHook[args.length] = hooker;
        return EzxHelpUtils.findAndChainConstructor(clazz, argsAndHook);
    }

    public XposedInterface.HookHandle findAndChainConstructor(
        String className,
        int priority,
        XposedInterface.ExceptionMode exceptionMode,
        XposedInterface.Hooker hooker,
        Object... args
    ) {
        Class<?> clazz = findClassIfExists(className);
        if (clazz == null) {
            XposedLog.w(TAG, "findAndChainConstructor: class not found: " + className);
            return null;
        }
        Object[] argsAndHook = Arrays.copyOf(args, args.length + 1);
        argsAndHook[args.length] = hooker;
        return EzxHelpUtils.findAndChainConstructor(clazz, priority, exceptionMode, argsAndHook);
    }

    public Set<XposedInterface.HookHandle> chainAllMethods(Class<?> clazz, String methodName, XposedInterface.Hooker hooker) {
        return EzxHelpUtils.chainAllMethods(clazz, methodName, hooker);
    }

    public Set<XposedInterface.HookHandle> chainAllMethods(String className, String methodName, XposedInterface.Hooker hooker) {
        Class<?> clazz = findClassIfExists(className);
        if (clazz == null) {
            XposedLog.w(TAG, "chainAllMethods: class not found: " + className);
            return java.util.Collections.emptySet();
        }
        return EzxHelpUtils.chainAllMethods(clazz, methodName, hooker);
    }

    public Set<XposedInterface.HookHandle> chainAllConstructors(Class<?> clazz, XposedInterface.Hooker hooker) {
        return EzxHelpUtils.chainAllConstructors(clazz, hooker);
    }

    public Set<XposedInterface.HookHandle> chainAllConstructors(String className, XposedInterface.Hooker hooker) {
        Class<?> clazz = findClassIfExists(className);
        if (clazz == null) {
            XposedLog.w(TAG, "chainAllConstructors: class not found: " + className);
            return java.util.Collections.emptySet();
        }
        return EzxHelpUtils.chainAllConstructors(clazz, hooker);
    }

    /**
     * 查找并 Hook 方法
     * <p>
     * 最后一个参数必须是 IMethodHook，前面的参数是参数类型
     *
     * @param clazz      目标类
     * @param methodName 方法名
     * @param args       参数类型 + IMethodHook 回调
     * @return HookHandle 对象
     */
    public XposedInterface.HookHandle findAndHookMethod(Class<?> clazz, String methodName, Object... args) {
        return EzxHelpUtils.findAndHookMethod(clazz, methodName, args);
    }

    /**
     * 查找并 Hook 方法
     * <p>
     * 最后一个参数必须是 IMethodHook，前面的参数是参数类型
     *
     * @param className  类名
     * @param methodName 方法名
     * @param args       参数类型 + IMethodHook 回调
     * @return HookHandle 对象
     */
    public XposedInterface.HookHandle findAndHookMethod(String className, String methodName, Object... args) {
        Class<?> clazz = findClassIfExists(className);
        if (clazz == null) {
            XposedLog.w(TAG, "findAndHookMethod: class not found: " + className);
            return null;
        }
        return EzxHelpUtils.findAndHookMethod(clazz, methodName, args);
    }

    /**
     * 查找并 Hook 方法
     * <p>
     * 最后一个参数必须是 IMethodHook，前面的参数是参数类型
     *
     * @param clazz      目标类
     * @param methodName 方法名
     * @param args       参数类型 + IMethodHook 回调
     * @return HookHandle 对象
     */
    public XposedInterface.HookHandle findAndReplaceMethod(Class<?> clazz, String methodName, Object... args) {
        return EzxHelpUtils.findAndHookMethodReplace(clazz, methodName, args);
    }

    /**
     * 查找并 Hook 方法
     * <p>
     * 最后一个参数必须是 IMethodHook，前面的参数是参数类型
     *
     * @param className  类名
     * @param methodName 方法名
     * @param args       参数类型 + IMethodHook 回调
     * @return HookHandle 对象
     */
    public XposedInterface.HookHandle findAndReplaceMethod(String className, String methodName, Object... args) {
        Class<?> clazz = findClassIfExists(className);
        if (clazz == null) {
            XposedLog.w(TAG, "findAndReplaceMethod: class not found: " + className);
            return null;
        }
        return EzxHelpUtils.findAndHookMethodReplace(clazz, methodName, args);
    }

    /**
     * Hook 类中所有指定名称的方法
     *
     * @param clazz      目标类
     * @param methodName 方法名
     * @param callback   Hook 回调
     * @return HookHandle 对象列表
     */
    public List<XposedInterface.HookHandle> hookAllMethods(Class<?> clazz, String methodName, IMethodHook callback) {
        return EzxHelpUtils.hookAllMethods(clazz, methodName, callback);
    }

    /**
     * Hook 类中所有指定名称的方法（通过类名）
     *
     * @param className  类名
     * @param methodName 方法名
     * @param callback   Hook 回调
     * @return HookHandle 对象列表
     */
    public List<XposedInterface.HookHandle> hookAllMethods(String className, String methodName, IMethodHook callback) {
        Class<?> clazz = findClassIfExists(className);
        if (clazz == null) {
            XposedLog.w(TAG, "hookAllMethods: class not found: " + className);
            return java.util.Collections.emptyList();
        }
        return EzxHelpUtils.hookAllMethods(clazz, methodName, callback);
    }

    /**
     * Hook 类中所有构造器
     *
     * @param clazz    目标类
     * @param callback Hook 回调
     * @return HookHandle 对象列表
     */
    public List<XposedInterface.HookHandle> hookAllConstructors(Class<?> clazz, IMethodHook callback) {
        return EzxHelpUtils.hookAllConstructors(clazz, callback);
    }

    /**
     * Hook 类中所有构造器
     *
     * @param className    类名
     * @param callback Hook 回调
     * @return HookHandle 对象列表
     */
    public List<XposedInterface.HookHandle> hookAllConstructors(String className, IMethodHook callback) {
        Class<?> clazz = findClassIfExists(className);
        if (clazz == null) {
            XposedLog.w(TAG, "hookAllConstructors: class not found: " + className);
            return java.util.Collections.emptyList();
        }
        return EzxHelpUtils.hookAllConstructors(clazz, callback);
    }

    // ==================== 便捷 Hook 工具 ====================

    /**
     * 创建一个返回常量值的 Hook 回调
     */
    public IMethodHook returnConstant(Object result) {
        return EzxHelpUtils.returnConstant(result);
    }

    /**
     * 获取阻止原方法执行的 Hook（返回 null）
     */
    public IMethodHook doNothing() {
        return EzxHelpUtils.DO_NOTHING;
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
        XposedLog.w(TAG, getPackageName(), message, t);
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
