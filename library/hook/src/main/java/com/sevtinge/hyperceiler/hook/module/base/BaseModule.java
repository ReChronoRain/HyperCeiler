/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.hook.module.base;

import static java.util.Arrays.asList;

import com.hchen.hooktool.HCBase;
import com.hchen.hooktool.HCInit;
import com.sevtinge.hyperceiler.hook.XposedInit;
import com.sevtinge.hyperceiler.hook.module.base.dexkit.DexKit;
import com.sevtinge.hyperceiler.hook.module.base.tool.AppsTool;
import com.sevtinge.hyperceiler.hook.safe.CrashData;
import com.sevtinge.hyperceiler.hook.utils.ContextUtils;
import com.sevtinge.hyperceiler.hook.utils.api.ProjectApi;
import com.sevtinge.hyperceiler.hook.utils.log.XposedLogUtils;
import com.sevtinge.hyperceiler.hook.utils.pkg.CheckModifyUtils;
import com.sevtinge.hyperceiler.hook.utils.pkg.DebugModeUtils;
import com.sevtinge.hyperceiler.hook.utils.prefs.PrefsMap;
import com.sevtinge.hyperceiler.hook.utils.prefs.PrefsUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public abstract class BaseModule {
    public LoadPackageParam mLoadPackageParam = null;
    public String TAG = getClass().getSimpleName();
    public final PrefsMap<String, Object> mPrefsMap = PrefsUtils.mPrefsMap;
    private static HashMap<String, String> swappedMap = CrashData.swappedData();
    private final ArrayList<String> checkList = new ArrayList<>(asList(
        "com.miui.securitycenter",
        "com.android.camera",
        "com.miui.home"
    ));

    public abstract void handleLoadPackage();

    public void init(LoadPackageParam lpparam) {
        if (lpparam == null || !lpparam.isFirstApplication) return;

        if (swappedMap == null || swappedMap.isEmpty()) {
            swappedMap = CrashData.swappedData();
        }

        PrefsMap<String, Object> prefs = PrefsUtils.mPrefsMap;
        if (!prefs.getBoolean("module_settings_reshook_new")) {
            // 把模块资源加载到目标应用
            try {
                if (!Objects.equals(ProjectApi.mAppModulePkg, lpparam.packageName)) {
                    boolean isAndroid = "android".equals(lpparam.packageName);
                    ContextUtils.getWaitContext(context -> {
                        if (context != null) {
                            XposedInit.mResHook.loadModuleRes(context);
                        }
                    }, isAndroid);
                }
            } catch (Throwable e) {
                XposedLogUtils.logE(TAG, "get context failed! " + e);
            }
        }

        mLoadPackageParam = lpparam;
        DexKit.ready(lpparam, TAG);
        HCInit.initLoadPackageParam(lpparam);

        try {
            boolean isDebug = mPrefsMap.getBoolean("development_debug_mode");
            for (String pkg : checkList) {
                if (Objects.equals(lpparam.packageName, pkg)) {
                    boolean check = CheckModifyUtils.INSTANCE.getCheckResult(lpparam.packageName);
                    boolean isVersion = DebugModeUtils.INSTANCE.getChooseResult(lpparam.packageName) == 0;
                    if (check && !isDebug && isVersion) return;
                    break;
                }
            }
            handleLoadPackage();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            DexKit.close();
        }
    }

    public void initHook(Object hook) {
        initHook(hook, true);
    }

    public void initHook(Object hook, boolean isInit) {
        initHook(hook, isInit, null, -1);
    }

    public void initHook(Object hook, boolean isInit, String versionName) {
        initHook(hook, isInit, versionName, -1);
    }

    public void initHook(Object hook, boolean isInit, String versionName, int... versionCodes) {
        for (int code : versionCodes) {
            initHook(hook, isInit, versionName, code);
        }
    }

    public void initHook(Object hook, boolean isInit, int... versionCodes) {
        for (int code : versionCodes) {
            initHook(hook, isInit, null, code);
        }
    }

    public void initHook(Object hook, boolean isInit, String versionName, int versionCode) {
        if (isInit) {
            if (versionCode == -1 && versionName == null) {
                onCreate(hook);
                return;
            }
            int code = AppsTool.getPackageVersionCode(mLoadPackageParam);
            String name = AppsTool.getPackageVersionName(mLoadPackageParam);
            if (code == versionCode)
                onCreate(hook);
            if (name.equals(versionName))
                onCreate(hook);
        }
    }

    private void onCreate(Object hook) {
        if (hook instanceof BaseHook baseHook) baseHook.onCreate(mLoadPackageParam);
        else if (hook instanceof HCBase HCBase) HCBase.onLoadPackage();
        else throw new RuntimeException("Unknown hook!");
    }
}
