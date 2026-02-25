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

import static com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsUtils.mPrefsMap;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.libhook.app.CorePatch.CorePatch;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.FlagSecure;
import com.sevtinge.hyperceiler.libhook.safecrash.CrashMonitor;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;
import com.sevtinge.hyperceiler.libhook.utils.log.AndroidLog;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import io.github.kyuubiran.ezxhelper.xposed.EzXposed;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.service.RemotePreferences;

/**
 * Xposed 模块入口基类
 *
 * @author HyperCeiler
 */
public class XposedInitEntry extends XposedModule {

    private static final String TAG = "HyperCeiler";

    private volatile boolean mPrefsListenerRegistered = false;

    protected String processName;
    protected SharedPreferences remotePrefs;
    protected SharedPreferences.OnSharedPreferenceChangeListener mListener;

    public XposedInitEntry(@NonNull XposedInterface base, @NonNull ModuleLoadedParam param) {
        super(base, param);
        processName = param.getProcessName();

        XposedLog.init(base);
        BaseLoad.init(base);
        EzXposed.initXposedModule(base);
    }

    @Override
    public void onSystemServerLoaded(@NonNull final SystemServerLoadedParam lpparam) {
        // load preferences
        initPrefs();

        // set xposed module
        EzxHelpUtils.setXposedModule(this);

        // load CrashHook
        try {
            new CrashMonitor(lpparam);
        } catch (Exception e) {
            AndroidLog.e(TAG, "system", "Crash Hook load failed, " + e);
        }

        // load Third Hook
        if (PrefsBridge.getBoolean("system_framework_core_patch_enable")) {
            new CorePatch().onLoad(lpparam);
            AndroidLog.d(TAG, "system", "CorePatch loaded");
        }
        if (PrefsBridge.getBoolean("system_other_flag_secure")) {
            new FlagSecure().onLoad(lpparam);
            AndroidLog.d(TAG, "system", "FlagSecure loaded");
        }

        // load Hook
        invokeInit(lpparam);
    }

    @Override
    public void onPackageLoaded(@NonNull final PackageLoadedParam lpparam) {
        super.onPackageLoaded(lpparam);
        if (!lpparam.isFirstPackage()) return;
        // load preferences
        initPrefs();
        // load EzXposed
        EzXposed.initOnPackageLoaded(lpparam);
        // invoke module
        invokeInit(lpparam);
        // Sync preferences changes
        //loadPreferenceChange();
    }

    protected void invokeInit(PackageLoadedParam lpparam) {
        invokeInitInternal(lpparam.getPackageName(), module -> module.onLoad(lpparam));
    }

    protected void invokeInit(SystemServerLoadedParam lpparam) {
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
        String remoteName = PrefsBridge.PREFS_NAME + "_remote";
        SharedPreferences remote = getRemotePreferences(remoteName);
        // 直接塞给 Bridge，以后 PrefsBridge.getBoolean 就会直接读它
        PrefsBridge.initForHook(remote);
    }

}
