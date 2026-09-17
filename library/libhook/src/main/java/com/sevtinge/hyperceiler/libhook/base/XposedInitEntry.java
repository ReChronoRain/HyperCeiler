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
import android.content.pm.ApplicationInfo;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.common.log.LogStatusManager;
import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.app.CorePatch.CorePatch;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.FlagSecure;
import com.sevtinge.hyperceiler.libhook.safecrash.CrashMonitor;
import com.sevtinge.hyperceiler.libhook.utils.api.ContextUtils;
import com.sevtinge.hyperceiler.libhook.utils.api.ThreadPoolManager;

import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.github.libxposed.api.XposedModule;
import io.github.lingqiqi5211.ezhooktool.xposed.AutomaticHotReloadResult;
import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed;

/**
 * Xposed 模块入口（libxposed API 102）。
 *
 * @author HyperCeiler
 */
public class XposedInitEntry extends XposedModule {

    private static final String TAG = "HyperCeiler";
    private static final String PREF_ALLOW_HOOK = "allow_hook";
    private static final String PREF_FRAMEWORK_ALLOW_HOOK = "framework_api_allow_hook";
    private static final String PREF_FRAMEWORK_REASON = "framework_check_reason";
    private static final String PREF_FRAMEWORK_NAME = "framework_check_name";
    private static final String PREF_FRAMEWORK_VERSION = "framework_check_version";
    private static final String PREF_FRAMEWORK_VERSION_CODE = "framework_check_version_code";

    /**
     * EzHookTool snapshot 之外、本项目需要跨代恢复的宿主状态。
     *
     * <p>热重载语义是「旧代码写、新代码读」，槽位布局一旦变化就会被跨版本误读，
     * 因此和 EzHookTool 的 snapshot 一样带 MAGIC + VERSION 头：布局变更时必须递增
     * {@link #EXTRA_VERSION}，恢复端校验不通过就整体拒绝，不做部分降级。</p>
     */
    private static final String EXTRA_MAGIC = "HyperCeiler.HotReloadExtras";
    private static final int EXTRA_VERSION = 1;
    private static final int EXTRA_INDEX_MAGIC = 0;
    private static final int EXTRA_INDEX_VERSION = 1;
    private static final int EXTRA_INDEX_APP_INFO = 2;
    private static final int EXTRA_INDEX_APP_CONTEXT = 3;
    private static final int EXTRA_INDEX_RUNTIME_STATE = 4;
    private static final int EXTRA_LENGTH = 5;

    protected String processName;
    private final Object prefsInitLock = new Object();
    private volatile boolean prefsInited = false;
    private volatile boolean runtimeInitialized = false;

    @Nullable
    private volatile PackageTarget mTarget;

    @Override
    public void onModuleLoaded(@NonNull ModuleLoadedParam param) {
        initModule(param);
    }

    private void initModule(@NonNull ModuleLoadedParam param) {
        initializeRuntime(param, true);
    }

    /**
     * 初始化当前 generation 的项目运行时。
     *
     * <p>状态 snapshot、默认自动 hook ID 分配与旧 handle 收尾交给 EzHookTool 的 API 102 热重载入口。
     * 所有同步规则初始化统一注册为 {@code onTargetReady} 回调，因此初次加载与热重载使用同一条路径；
     * 个别需要稳定语义 ID 的规则仍可自行声明 {@code reloadKey}。</p>
     */
    private void initializeRuntime(@NonNull ModuleLoadedParam param, boolean initializeEzXposed) {
        processName = param.getProcessName();
        try {
            initPrefs();
        } catch (Throwable t) {
            XposedLog.w(TAG, processName, "Failed to initialize prefs during module bootstrap, will retry later.", t);
        }
        if (initializeEzXposed) {
            EzXposed.initOnModuleLoaded(this, param);
        }
        BaseLoad.init(this);
        if (!runtimeInitialized) {
            EzXposed.onTargetReady(this::installCurrentTargetHooks);
            runtimeInitialized = true;
        }
    }

    @Override
    public void onSystemServerStarting(@NonNull SystemServerStartingParam lpparam) {
        mTarget = PackageTarget.ofSystemServer(lpparam);
        EzXposed.initOnSystemServerStarting(lpparam);
    }

    @Override
    public void onPackageReady(@NonNull PackageReadyParam lpparam) {
        super.onPackageReady(lpparam);
        if (!lpparam.isFirstPackage()) return;
        mTarget = PackageTarget.ofPackageReady(lpparam);
        EzXposed.initOnPackageReady(lpparam);
    }

    @Override
    public boolean onHotReloading(@NonNull HotReloadingParam param) {
        String initializationBlockReason = BaseLoad.getHotReloadBlockReason();
        if (initializationBlockReason != null) {
            XposedLog.w(TAG, processName, "Hot reload rejected: " + initializationBlockReason);
            return false;
        }

        Object[] extras;
        try {
            extras = buildHotReloadExtras();
        } catch (Throwable t) {
            XposedLog.e(TAG, processName, "Hot reload rejected: failed to capture process state", t);
            return false;
        }
        if (extras == null) {
            XposedLog.w(TAG, processName, "Hot reload rejected: process state is incomplete.");
            return false;
        }
        try {
            // 复用 EzHookTool 的 target snapshot；extras 只承载项目自己的宿主状态。
            if (!EzXposed.handleHotReloading(param, extras)) {
                XposedLog.w(TAG, processName,
                    "Hot reload rejected by EzXposed; see the preceding EzXposed log for details.");
                return false;
            }
        } catch (Throwable t) {
            XposedLog.e(TAG, processName, "Hot reload rejected: failed to save state", t);
            return false;
        }

        if (!prepareForHotReload()) {
            XposedLog.w(TAG, processName,
                "Hot reload rejected: old generation cleanup did not complete safely.");
            return false;
        }
        XposedLog.i(TAG, processName, "Hot reload accepted.");
        return true;
    }

    @Override
    public void onHotReloaded(@NonNull HotReloadedParam param) {
        // API 102 不会为热重载自动重放 onModuleLoaded；这里重建新 generation 的运行时并注册
        // onTargetReady。重复调用保持幂等，避免重复注册回调。
        initializeRuntime(param, true);
        BaseLoad.beginHotReloadVerification();
        try {
            AutomaticHotReloadResult result = EzXposed.restoreHotReloadedAutomatically(
                this,
                param,
                extras -> {
                    HotReloadExtras restored = restoreHotReloadExtras(extras);
                    if (restored == null) {
                        throw new IllegalStateException("Hot reload state is missing or incompatible");
                    }
                    restoreEzXposedRuntime(restored);
                }
            );
            BaseLoad.verifyHotReloadInitialization();
            XposedLog.i(TAG, processName,
                "Hot reload hook transition completed: " + result.getInstalledHookCount()
                    + " logical hook(s), " + result.getAtomicallyReplacedHookCount()
                    + " old physical hook(s) atomically replaced, " + result.getRemovedOldHookCount()
                    + " obsolete physical hook(s) removed.");
        } catch (Throwable t) {
            XposedLog.e(TAG, processName, "Hot reload re-init failed", t);
            if (t instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (t instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("Hot reload re-init failed", t);
        } finally {
            BaseLoad.endHotReloadVerification();
        }
    }

    private void restoreEzXposedRuntime(@NonNull HotReloadExtras extras) {
        processName = EzXposed.getProcessName();
        if (extras.appContext() != null) {
            EzXposed.initAppContext(extras.appContext(), false, true);
        }
        BaseHook.restoreHotReloadRuntimeState(extras.runtimeState());

        // 包名与 ClassLoader 由 EzHookTool 的 snapshot 恢复；ApplicationInfo 不在其公开
        // API 内，因此仍由本项目的 extras 承载（DexKit 需要 dataDir / sourceDir）。
        if (EzXposed.isSystemServer()) {
            mTarget = PackageTarget.restored(BaseLoad.SYSTEM_SERVER, null,
                EzXposed.getClassLoader(), true);
            return;
        }

        String packageName = EzXposed.getPackageName();
        if (TextUtils.isEmpty(packageName) || extras.applicationInfo() == null) {
            throw new IllegalStateException("Restored package state is incomplete");
        }
        mTarget = PackageTarget.restored(packageName, extras.applicationInfo(),
            EzXposed.getClassLoader(), false);
    }

    /** 初次加载与自动热重载共同使用的同步规则初始化入口。 */
    private void installCurrentTargetHooks() {
        PackageTarget target = mTarget;
        if (target == null) {
            throw new IllegalStateException("Target state is unavailable before hook initialization");
        }
        BaseLoad.beginHookInitialization();
        if (!prepareHookLoad(target.getPackageName())) {
            attachHookLogLevelObserver(target.isSystemServer());
            if (target.isSystemServer()) {
                loadSystemEntryHooks(target);
            }
            invokeInit(target);
        }
        // 热重载时个别规则会自行捕获初始化异常并写入 BaseLoad；必须在 EzHookTool 批次
        // 发布任何新物理 hook 前把它们转回失败，不能等旧 hook 已收尾后才发现。
        BaseLoad.verifyHotReloadInitialization();
    }

    @Nullable
    private Object[] buildHotReloadExtras() {
        PackageTarget target = mTarget;
        if (target == null) return null;
        Context appContext = null;
        try {
            appContext = EzXposed.getAppContextOrNull();
        } catch (Throwable t) {
            XposedLog.d(TAG, processName, "Application context is unavailable during hot reload snapshot.");
        }
        ApplicationInfo appInfo = target.getApplicationInfo();
        // 普通应用进程必须带上 ApplicationInfo，否则热重载后 DexKit 无法重建缓存路径。
        if (!target.isSystemServer() && appInfo == null) return null;
        Object[] extras = new Object[EXTRA_LENGTH];
        extras[EXTRA_INDEX_MAGIC] = EXTRA_MAGIC;
        extras[EXTRA_INDEX_VERSION] = EXTRA_VERSION;
        extras[EXTRA_INDEX_APP_INFO] = appInfo;
        extras[EXTRA_INDEX_APP_CONTEXT] = appContext;
        extras[EXTRA_INDEX_RUNTIME_STATE] = BaseHook.snapshotHotReloadRuntimeState();
        return extras;
    }

    private boolean prepareForHotReload() {
        try {
            // 先确认模块线程已经结束，避免随后清理引用时旧代码仍在并发执行。
            if (!ThreadPoolManager.shutdownAndAwait(500, TimeUnit.MILLISECONDS)) {
                XposedLog.w(TAG, processName,
                    "Failed to stop all module background tasks before hot reload.");
                return false;
            }
        } catch (Throwable t) {
            XposedLog.w(TAG, processName, "Failed to stop background tasks before hot reload", t);
            return false;
        }
        try {
            BaseLoad.prepareHotReload();
            LogStatusManager.detachHookLogLevelObserver();
            return true;
        } catch (Throwable t) {
            XposedLog.w(TAG, processName,
                "Failed to release module runtime state before hot reload", t);
            return false;
        }
    }

    /**
     * 校验并还原本项目的跨代状态。
     *
     * <p>布局不符时返回 {@code null}，调用方会把本次热重载判为失败并要求重启目标进程，
     * 而不是带着部分错位的状态继续运行。</p>
     */
    @Nullable
    private HotReloadExtras restoreHotReloadExtras(@Nullable Object[] extras) {
        if (extras == null || extras.length != EXTRA_LENGTH) {
            return null;
        }
        if (!EXTRA_MAGIC.equals(extras[EXTRA_INDEX_MAGIC])
            || !Integer.valueOf(EXTRA_VERSION).equals(extras[EXTRA_INDEX_VERSION])) {
            return null;
        }
        ApplicationInfo appInfo = extras[EXTRA_INDEX_APP_INFO] instanceof ApplicationInfo info ? info : null;
        Context appContext = extras[EXTRA_INDEX_APP_CONTEXT] instanceof Context context ? context : null;
        return new HotReloadExtras(appInfo, appContext, extras[EXTRA_INDEX_RUNTIME_STATE]);
    }

    private record HotReloadExtras(
        @Nullable ApplicationInfo applicationInfo,
        @Nullable Context appContext,
        @Nullable Object runtimeState
    ) {
    }

    protected void invokeInit(PackageTarget target) {
        invokeInitInternal(target.getPackageName(), module -> module.onLoad(target));
    }

    private void loadSystemEntryHooks(PackageTarget target) {
        try {
            new CrashMonitor(target);
        } catch (Exception e) {
            XposedLog.e(TAG, "system", "Crash Hook load failed, " + e);
            BaseLoad.recordHookInitializationFailure("CrashMonitor", e);
            BaseLoad.recordHotReloadInitializationFailure("CrashMonitor", e);
        }

        if (PrefsBridge.getBoolean("system_framework_core_patch_enable")) {
            new CorePatch().onLoad(target);
            XposedLog.d(TAG, "system", "CorePatch loaded");
        }
        if (PrefsBridge.getBoolean("system_other_flag_secure")) {
            new FlagSecure().onLoad(target);
            XposedLog.d(TAG, "system", "FlagSecure loaded");
        }
    }

    private void invokeInitInternal(String packageName, ModuleLoader loader) {
        HashMap<String, DataBase> dataMap = DataBase.get();

        ClassLoader classLoader = getClass().getClassLoader();
        if (classLoader == null) {
            XposedLog.e(TAG, "ClassLoader is null, skip loading modules for: " + packageName);
            return;
        }

        ModuleMatcher.MatchContext context = buildMatchContext(packageName, dataMap);
        ModuleMatcher matcher = new ModuleMatcher(context);

        dataMap.forEach((className, data) -> {
            if (!matcher.shouldLoad(data, packageName)) return;
            try {
                Class<?> clazz = classLoader.loadClass(className);
                BaseLoad module = (BaseLoad) clazz.getDeclaredConstructor().newInstance();
                loader.load(module);
            } catch (ReflectiveOperationException e) {
                XposedLog.e(TAG, "Failed to load module: " + className, e);
                BaseLoad.recordHookInitializationFailure(className, e);
                BaseLoad.recordHotReloadInitializationFailure(className, e);
            }
        });
    }

    private ModuleMatcher.MatchContext buildMatchContext(String packageName, HashMap<String, DataBase> dataMap) {
        boolean isSystemServer = BaseLoad.SYSTEM_SERVER.equals(packageName);
        boolean hasExactMatch = dataMap.values().stream()
            .anyMatch(data -> packageName.equals(data.targetPackage));

        return ModuleMatcher.MatchContext.builder()
            .systemServer(isSystemServer)
            .exactMatch(hasExactMatch)
            .debugMode(PrefsBridge.getBoolean("development_debug_mode"))
            .build();
    }

    @FunctionalInterface
    private interface ModuleLoader {
        void load(BaseLoad module);
    }

    protected void initPrefs() {
        if (prefsInited) {
            return;
        }
        synchronized (prefsInitLock) {
            if (prefsInited) {
                return;
            }
            PrefsBridge.initForHook(getRemotePreferences(PrefsBridge.REMOTE_PREFS_GROUP));
            LogStatusManager.syncLogLevelFromPrefs();
            prefsInited = true;
        }
    }

    private boolean prepareHookLoad(String packageName) {
        if (!isHookEnabled()) {
            XposedLog.w(TAG, packageName, "Skip loading hooks because hook loading is disabled by app state.");
            return true;
        }
        return false;
    }

    private void attachHookLogLevelObserver(boolean isSystem) {
        ContextUtils.getWaitContext(context -> {
            if (context != null) {
                LogStatusManager.attachHookLogLevelObserver(context);
            }
        }, isSystem);
    }

    private boolean isHookEnabled() {
        return PrefsBridge.getBoolean(PREF_ALLOW_HOOK, false)
            && isFrameworkAllowedForCurrentRuntime();
    }

    private boolean isFrameworkAllowedForCurrentRuntime() {
        boolean allowHook = PrefsBridge.getBoolean(PREF_FRAMEWORK_ALLOW_HOOK, true);
        if (allowHook) {
            return true;
        }

        String storedReason = PrefsBridge.getString(PREF_FRAMEWORK_REASON, null);
        if (TextUtils.isEmpty(storedReason)) {
            return false;
        }

        String currentFrameworkName;
        String currentFrameworkVersion;
        long currentFrameworkVersionCode;
        try {
            currentFrameworkName = normalizeFrameworkText(getFrameworkName());
            currentFrameworkVersion = normalizeFrameworkText(getFrameworkVersion());
            currentFrameworkVersionCode = getFrameworkVersionCode();
        } catch (Throwable t) {
            return false;
        }

        String checkedFrameworkName = normalizeFrameworkText(PrefsBridge.getString(PREF_FRAMEWORK_NAME, null));
        String checkedFrameworkVersion = normalizeFrameworkText(PrefsBridge.getString(PREF_FRAMEWORK_VERSION, null));
        long checkedFrameworkVersionCode = PrefsBridge.getLong(PREF_FRAMEWORK_VERSION_CODE, Long.MIN_VALUE);

        boolean matched = Objects.equals(currentFrameworkName, checkedFrameworkName)
            && Objects.equals(currentFrameworkVersion, checkedFrameworkVersion)
            && currentFrameworkVersionCode == checkedFrameworkVersionCode;
        if (matched) {
            return false;
        }

        XposedLog.i(
            TAG,
            processName,
            "Allow loading hooks temporarily because the running framework differs from the last framework blocked by XposedService."
        );
        return true;
    }

    private static String normalizeFrameworkText(String value) {
        if (value == null) {
            return null;
        }
        String trimValue = value.trim();
        return trimValue.isEmpty() ? null : trimValue;
    }
}
