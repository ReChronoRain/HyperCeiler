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

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.common.utils.api.ProjectApi;
import com.sevtinge.hyperceiler.libhook.utils.api.ContextUtils;
import com.sevtinge.hyperceiler.libhook.utils.api.ThreadPoolManager;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.DexKit;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.ResourcesTool;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.BooleanSupplier;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam;
import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed;

/**
 * 应用模块基类
 * <p>
 * 每个目标应用对应一个 BaseLoad 子类，负责管理该应用下的所有 Hook 规则。
 * 通过静态方法暴露 ClassLoader、PackageName 等资源供具体 Hook 使用。
 *
 * @author HyperCeiler
 */
public abstract class BaseLoad {
    public static final String SYSTEM_SERVER = "system";
    private static final Object sLock = new Object();
    private static volatile ClassLoader sClassLoader;
    private static volatile String sPackageName;
    private static volatile PackageReadyParam sLpparam;
    private static volatile SystemServerStartingParam sSystemServerParam;
    private static volatile XposedInterface sXposed;
    private static volatile String sCurrentHookTag = "BaseLoad";
    private static final List<String> sGenerationInitializationFailures =
        Collections.synchronizedList(new ArrayList<>());
    private static final List<String> sHotReloadInitializationFailures =
        Collections.synchronizedList(new ArrayList<>());
    private static volatile boolean sVerifyingHotReload = false;
    public static ResourcesTool mResHook;
    private boolean mDexKitSessionPrepared = false;
    private final List<BaseHook> mPendingDexKitHooks = new ArrayList<>();

    private record DexKitInitResult(BaseHook hook, boolean shouldInit, Throwable error) {
    }

    /**
     * 初始化 Xposed 运行时入口。
     * 统一分发给 BaseLoad、日志、EzXposed 和内部 Hook API。
     */
    public static void init(XposedInterface xposed) {
        sXposed = xposed;
        XposedLog.init(xposed);
    }

    /**
     * 在一个目标进程的完整规则初始化前清空本 generation 的失败记录。
     *
     * <p>初次加载不会因为单条规则失败而中断宿主进程，但该 generation 不能再被当作可安全原子切换的
     * 热重载起点。入口会在下一次 {@code onHotReloading} 前读取此记录并拒绝本次请求。</p>
     */
    public static void beginHookInitialization() {
        synchronized (sGenerationInitializationFailures) {
            sGenerationInitializationFailures.clear();
        }
    }

    /** 记录当前 generation 的规则初始化失败；同时供普通加载和热重载严格校验使用。 */
    public static void recordHookInitializationFailure(String source, Throwable error) {
        String detail = source + " (" + error.getClass().getSimpleName() + ")";
        sGenerationInitializationFailures.add(detail);
    }

    /** 当前 generation 若存在未初始化成功的规则，则不能安全作为热重载的旧 generation。 */
    public static String getHotReloadBlockReason() {
        List<String> failures;
        synchronized (sGenerationInitializationFailures) {
            if (sGenerationInitializationFailures.isEmpty()) return null;
            failures = new ArrayList<>(sGenerationInitializationFailures);
        }
        return "Hook initialization failed in the current generation: " + String.join("; ", failures);
    }

    /** 在新 generation 初始化期间收集规则失败，以便把真实状态回传给框架。 */
    public static void beginHotReloadVerification() {
        synchronized (sHotReloadInitializationFailures) {
            sHotReloadInitializationFailures.clear();
            sVerifyingHotReload = true;
        }
    }

    public static void verifyHotReloadInitialization() {
        List<String> failures;
        synchronized (sHotReloadInitializationFailures) {
            if (sHotReloadInitializationFailures.isEmpty()) return;
            failures = new ArrayList<>(sHotReloadInitializationFailures);
        }
        throw new IllegalStateException(
            "One or more hook rules failed to initialize during hot reload: "
                + String.join("; ", failures)
        );
    }

    public static void endHotReloadVerification() {
        sVerifyingHotReload = false;
        synchronized (sHotReloadInitializationFailures) {
            sHotReloadInitializationFailures.clear();
        }
    }

    public static void recordHotReloadInitializationFailure(String source, Throwable error) {
        if (!sVerifyingHotReload) return;
        String detail = source + " (" + error.getClass().getSimpleName() + ")";
        sHotReloadInitializationFailures.add(detail);
    }

    public static void prepareHotReload() {
        // 新 generation 会用同 ID 原子替换 Resources 方法 hook；这里仅移除旧 APK 的
        // ResourcesLoader，确保之后的资源查询不会继续命中上一版模块资源。
        ResourcesTool.prepareHotReload();
        BaseHook.prepareHotReload();
        synchronized (sLock) {
            sClassLoader = null;
            sPackageName = null;
            sLpparam = null;
            sSystemServerParam = null;
            sCurrentHookTag = "BaseLoad";
            mResHook = null;
        }
    }

    public static XposedInterface getXposed() {
        return sXposed;
    }

    public static ClassLoader getClassLoader() {
        synchronized (sLock) {
            return sClassLoader;
        }
    }

    public static String getPackageName() {
        synchronized (sLock) {
            return sPackageName;
        }
    }

    public static PackageReadyParam getLpparam() {
        synchronized (sLock) {
            return sLpparam;
        }
    }

    public static SystemServerStartingParam getSystemServerParam() {
        synchronized (sLock) {
            return sSystemServerParam;
        }
    }

    public static boolean isSystemServer() {
        synchronized (sLock) {
            return SYSTEM_SERVER.equals(sPackageName) && sSystemServerParam != null;
        }
    }

    public static String getTag() {
        synchronized (sLock) {
            return sCurrentHookTag;
        }
    }

    public abstract void onPackageLoaded();

    /**
     * 加载普通应用 Hook
     */
    public void onLoad(PackageReadyParam lpparam) {
        if (lpparam == null) return;

        synchronized (sLock) {
            sClassLoader = lpparam.getClassLoader();
            sPackageName = lpparam.getPackageName();
            sLpparam = lpparam;
            sSystemServerParam = null;
            sCurrentHookTag = this.getClass().getSimpleName();
            mResHook = ResourcesTool.getInstance(getXposed().getModuleApplicationInfo().sourceDir);
            mDexKitSessionPrepared = false;
            mPendingDexKitHooks.clear();
        }

        loadModuleResources();
        executeHook();
    }

    /**
     * 加载 SystemServer Hook
     */
    public void onLoad(SystemServerStartingParam lpparam) {
        if (lpparam == null) return;

        synchronized (sLock) {
            sClassLoader = lpparam.getClassLoader();
            sPackageName = SYSTEM_SERVER;
            sLpparam = null;
            sSystemServerParam = lpparam;
            sCurrentHookTag = this.getClass().getSimpleName();
            mResHook = ResourcesTool.getInstance(getXposed().getModuleApplicationInfo().sourceDir);
            mDexKitSessionPrepared = false;
            mPendingDexKitHooks.clear();
        }

        loadModuleResources();
        executeHook();
    }

    private void loadModuleResources() {
        try {
            String pkgName = getPackageName();
            if (!Objects.equals(ProjectApi.mAppModulePkg, pkgName)) {
                // 热重载已携带 application context 时直接重挂新 APK 的 loader，避免等后台轮询；
                // 这对 SystemUI 这类长生命周期进程尤为重要。
                Context activeContext = EzXposed.getAppContextOrNull();
                if (activeContext != null) {
                    mResHook.loadModuleRes(activeContext);
                    return;
                }
                boolean isAndroid = SYSTEM_SERVER.equals(pkgName);
                ContextUtils.getWaitContext(context -> {
                    if (context != null) {
                        mResHook.loadModuleRes(context);
                    }
                }, isAndroid);
            }
        } catch (Throwable e) {
            XposedLog.e(getTag(), "get context failed! " + e);
        }
    }

    private void executeHook() {
        try {
            onPackageLoaded();
        } finally {
            try {
                flushPendingDexKitHooks();
            } finally {
                closeDexKitSession();
            }
        }
    }

    private void prepareDexKitSession(String tag) {
        if (mDexKitSessionPrepared || isSystemServer()) return;
        PackageReadyParam param = getLpparam();
        if (param != null) {
            DexKit.ready(param, tag);
            mDexKitSessionPrepared = true;
        }
    }

    private void closeDexKitSession() {
        if (!mDexKitSessionPrepared || isSystemServer()) return;
        DexKit.close();
        mDexKitSessionPrepared = false;
    }

    protected void initHook(BaseHook hook) {
        try {
            initHookInternal(hook, true);
        } catch (Throwable t) {
            logHookFailure(hook, t);
        }
    }

    protected void initHook(BaseHook hook, boolean isInit) {
        try {
            initHookInternal(hook, isInit);
        } catch (Throwable t) {
            logHookFailure(hook, t);
        }
    }

    protected void initHook(BaseHook hook, BooleanSupplier condition) {
        if (hook == null) return;

        try {
            if (!mPendingDexKitHooks.isEmpty()) {
                flushPendingDexKitHooks();
            }
            initHookInternal(hook, condition.getAsBoolean());
        } catch (Throwable t) {
            logHookFailure(hook, t);
        }
    }

    private void initHookInternal(BaseHook hook, boolean shouldInit) {
        if (hook == null || !shouldInit) return;
        if (hook.useDexKit() && !isSystemServer()) {
            prepareDexKitSession(hook.TAG);
            mPendingDexKitHooks.add(hook);
            return;
        }
        flushPendingDexKitHooks();
        runHookInit(hook);
    }

    private void flushPendingDexKitHooks() {
        if (mPendingDexKitHooks.isEmpty()) return;

        List<BaseHook> pendingHooks = new ArrayList<>(mPendingDexKitHooks);
        mPendingDexKitHooks.clear();

        List<Future<DexKitInitResult>> futures = new ArrayList<>(pendingHooks.size());
        for (BaseHook hook : pendingHooks) {
            futures.add(ThreadPoolManager.getInstance().submit(() -> runDexKitInit(hook)));
        }

        List<DexKitInitResult> results = new ArrayList<>(pendingHooks.size());
        for (int i = 0; i < futures.size(); i++) {
            BaseHook hook = pendingHooks.get(i);
            try {
                results.add(futures.get(i).get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                results.add(new DexKitInitResult(hook, false, e));
            } catch (ExecutionException e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                results.add(new DexKitInitResult(hook, false, cause));
            }
        }

        for (DexKitInitResult result : results) {
            if (result.error != null) {
                XposedLog.e(result.hook.TAG, getPackageName(), "Skip hook because initDexKit failed", result.error);
                recordHookInitializationFailure(result.hook.TAG, result.error);
                recordHotReloadInitializationFailure(result.hook.TAG, result.error);
                continue;
            }
            if (!result.shouldInit) {
                XposedLog.w(result.hook.TAG, getPackageName(), "Skip hook because initDexKit returned false");
                continue;
            }
            try {
                runHookInit(result.hook);
            } catch (Throwable t) {
                logHookFailure(result.hook, t);
            }
        }
    }

    private DexKitInitResult runDexKitInit(BaseHook hook) {
        hook.setDexKitInitInProgress(true);
        try {
            return new DexKitInitResult(hook, hook.initDexKit(), null);
        } catch (Throwable t) {
            return new DexKitInitResult(hook, false, t);
        } finally {
            hook.setDexKitInitInProgress(false);
        }
    }

    private void runHookInit(BaseHook hook) {
        hook.init();
        XposedLog.i(hook.TAG, getPackageName(), "Hook Success");
    }

    private void logHookFailure(BaseHook hook, Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        XposedLog.e(hook.TAG, getPackageName(), "Hook Failed: " + sw);
        recordHookInitializationFailure(hook.TAG, t);
        recordHotReloadInitializationFailure(hook.TAG, t);
    }
}
