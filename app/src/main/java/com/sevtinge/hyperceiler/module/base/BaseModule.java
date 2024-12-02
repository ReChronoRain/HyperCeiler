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

 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.module.base;

import com.hchen.hooktool.BaseHC;
import com.hchen.hooktool.HCInit;
import com.sevtinge.hyperceiler.XposedInit;
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit;
import com.sevtinge.hyperceiler.safe.CrashData;
import com.sevtinge.hyperceiler.utils.ContextUtils;
import com.sevtinge.hyperceiler.utils.Helpers;
import com.sevtinge.hyperceiler.utils.api.ProjectApi;
import com.sevtinge.hyperceiler.utils.log.XposedLogUtils;
import com.sevtinge.hyperceiler.utils.prefs.PrefsMap;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;

import java.util.HashMap;

import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public abstract class BaseModule {
    public LoadPackageParam mLoadPackageParam = null;
    public String TAG = getClass().getSimpleName();
    public final PrefsMap<String, Object> mPrefsMap = PrefsUtils.mPrefsMap;
    private static HashMap<String, String> swappedMap = CrashData.swappedData();

    public abstract void handleLoadPackage();

    public void initZygote() {
    }

    public void init(LoadPackageParam lpparam) {
        if (swappedMap.isEmpty()) {
            swappedMap = CrashData.swappedData();
        }

        if (CrashData.toPkgList(lpparam.packageName)) {
            XposedLogUtils.logI(TAG, "Entry safe mode: " + lpparam.packageName);
            return;
        }
        HCInit.initLoadPackageParam(lpparam);
        // 把模块资源加载到目标应用
        try {
            if (!ProjectApi.mAppModulePkg.equals(lpparam.packageName)) {
                ContextUtils.getWaitContext(context -> {
                    if (context != null) {
                        XposedInit.mResHook.loadModuleRes(context);
                    }
                }, "android".equals(lpparam.packageName));
            }
        } catch (Throwable e) {
            XposedLogUtils.logE(TAG, "get context failed!" + e);
        }

        mLoadPackageParam = lpparam;

        DexKit dexKit = new DexKit(lpparam, TAG);
        initZygote();
        handleLoadPackage();

        if (dexKit.isInit) {
            dexKit.close();
        }
    }

    /*public void initHook(BaseHook baseHook) {
        if (baseHook.isLoad()) {
            baseHook.onCreate(mLoadPackageParam);
        }
    }*/

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
            int code = Helpers.getPackageVersionCode(mLoadPackageParam);
            String name = Helpers.getPackageVersionName(mLoadPackageParam);
            if (code == versionCode)
                onCreate(hook);
            if (name.equals(versionName))
                onCreate(hook);
        }
    }

    private void onCreate(Object hook) {
        if (hook instanceof BaseHook baseHook) baseHook.onCreate(mLoadPackageParam);
        else if (hook instanceof BaseHC baseHC) baseHC.onLoadPackage();
        else throw new RuntimeException("Unknown hook!");
    }
}
