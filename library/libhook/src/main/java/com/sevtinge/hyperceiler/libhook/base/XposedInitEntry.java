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

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.common.log.LogStatusManager;
import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.app.CorePatch.CorePatch;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.FlagSecure;
import com.sevtinge.hyperceiler.libhook.safecrash.CrashMonitor;
import com.sevtinge.hyperceiler.libhook.utils.api.ContextUtils;

import java.util.HashMap;
import java.util.Objects;

import io.github.kyuubiran.ezxhelper.xposed.EzXposed;
import io.github.libxposed.api.XposedModule;

/**
 * Xposed 模块入口基类
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

    protected String processName;
    private final Object prefsInitLock = new Object();
    private volatile boolean prefsInited = false;

    @Override
    public void onModuleLoaded(@NonNull ModuleLoadedParam param) {
        processName = param.getProcessName();

        // load prefs
        try {
            initPrefs();
        } catch (Throwable t) {
            XposedLog.w(TAG, processName, "Failed to initialize prefs during module bootstrap, will retry later.", t);
        }

        EzXposed.initOnModuleLoaded(this, param);
        BaseLoad.init(this);
    }

    @Override
    public void onSystemServerStarting(@NonNull SystemServerStartingParam lpparam) {
        if (prepareHookLoad("system")) {
            return;
        }
        attachHookLogLevelObserver(true);

        // load ezx lpparam
        EzXposed.initOnSystemServerStarting(lpparam);

        // load CrashHook
        try {
            new CrashMonitor(lpparam);
        } catch (Exception e) {
            XposedLog.e(TAG, "system", "Crash Hook load failed, " + e);
        }

        // load Third Hook
        if (PrefsBridge.getBoolean("system_framework_core_patch_enable")) {
            new CorePatch().onLoad(lpparam);
            XposedLog.d(TAG, "system", "CorePatch loaded");
        }
        if (PrefsBridge.getBoolean("system_other_flag_secure")) {
            new FlagSecure().onLoad(lpparam);
            XposedLog.d(TAG, "system", "FlagSecure loaded");
        }

        // load Hook
        invokeInit(lpparam);
    }

    @Override
    public void onPackageReady(@NonNull PackageReadyParam lpparam) {
        super.onPackageReady(lpparam);
        if (!lpparam.isFirstPackage()) return;
        String packageName = lpparam.getPackageName();
        if (prepareHookLoad(packageName)) {
            return;
        }

        EzXposed.initOnPackageReady(lpparam);
        attachHookLogLevelObserver(false);
        // invoke module
        invokeInit(lpparam);
    }

    protected void invokeInit(PackageReadyParam lpparam) {
        invokeInitInternal(lpparam.getPackageName(), module -> module.onLoad(lpparam));
    }

    protected void invokeInit(SystemServerStartingParam lpparam) {
        invokeInitInternal(BaseLoad.SYSTEM_SERVER, module -> module.onLoad(lpparam));
    }

    private void invokeInitInternal(String packageName, ModuleLoader loader) {
        HashMap<String, DataBase> dataMap = DataBase.get();

        ClassLoader classLoader = getClass().getClassLoader();
        if (classLoader == null) {
            XposedLog.e(TAG, "ClassLoader is null, skip loading modules for: " + packageName);
            return;
        }

        // 构建匹配上下文
        ModuleMatcher.MatchContext context = buildMatchContext(packageName, dataMap);
        ModuleMatcher matcher = new ModuleMatcher(context);

        // 遍历并加载匹配的模块
        dataMap.forEach((className, data) -> {
            if (!matcher.shouldLoad(data, packageName)) return;

            try {
                Class<?> clazz = classLoader.loadClass(className);
                BaseLoad module = (BaseLoad) clazz.getDeclaredConstructor().newInstance();
                loader.load(module);
            } catch (ReflectiveOperationException e) {
                XposedLog.e(TAG, "Failed to load module: " + className, e);
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
