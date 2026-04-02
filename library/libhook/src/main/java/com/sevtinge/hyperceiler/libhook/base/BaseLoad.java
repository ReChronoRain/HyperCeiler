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

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.common.utils.api.ProjectApi;
import com.sevtinge.hyperceiler.libhook.utils.api.ContextUtils;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.DexKit;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.ResourcesTool;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;
import java.util.function.BooleanSupplier;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam;

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
    public static ResourcesTool mResHook;
    private boolean mDexKitSessionPrepared = false;

    /**
     * 初始化 Xposed 运行时入口。
     * 统一分发给 BaseLoad、日志、EzXposed 和内部 Hook API。
     */
    public static void init(XposedModule xposedModule) {
        sXposed = xposedModule;
        XposedLog.init(xposedModule);
        EzxHelpUtils.setXposedModule(xposedModule);
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
        }

        loadModuleResources();
        executeHook();
    }

    private void loadModuleResources() {
        try {
            String pkgName = getPackageName();
            if (!Objects.equals(ProjectApi.mAppModulePkg, pkgName)) {
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
            closeDexKitSession();
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
        initHook(hook, () -> true);
    }

    protected void initHook(BaseHook hook, boolean isInit) {
        initHook(hook, () -> isInit);
    }

    protected void initHook(BaseHook hook, BooleanSupplier condition) {
        if (hook == null) return;

        try {
            if (condition.getAsBoolean()) {
                if (hook.useDexKit() && !isSystemServer()) {
                    prepareDexKitSession(hook.TAG);
                    hook.setDexKitInitInProgress(true);
                    try {
                        if (!hook.initDexKit()) {
                            XposedLog.w(hook.TAG, getPackageName(), "Skip hook because initDexKit returned false");
                            return;
                        }
                    } catch (Throwable t) {
                        XposedLog.e(hook.TAG, getPackageName(), "Skip hook because initDexKit failed", t);
                        return;
                    } finally {
                        hook.setDexKitInitInProgress(false);
                    }
                }
                hook.init();
                XposedLog.i(hook.TAG, getPackageName(), "Hook Success");
            }
        } catch (Throwable t) {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            XposedLog.e(hook.TAG, getPackageName(), "Hook Failed: " + sw);
        }
    }
}
