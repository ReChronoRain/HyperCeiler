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

import com.github.kyuubiran.ezxhelper.EzXHelper;
import com.sevtinge.hyperceiler.XposedInit;
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit;
import com.sevtinge.hyperceiler.module.base.dexkit.InitDexKit;
import com.sevtinge.hyperceiler.safe.CrashData;
import com.sevtinge.hyperceiler.utils.ContextUtils;
import com.sevtinge.hyperceiler.utils.api.ProjectApi;
import com.sevtinge.hyperceiler.utils.log.XposedLogUtils;
import com.sevtinge.hyperceiler.utils.prefs.PrefsMap;

import java.util.HashMap;

import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public abstract class BaseModule implements IXposedHook {

    public LoadPackageParam mLoadPackageParam = null;
    public String TAG = getClass().getSimpleName();
    public final PrefsMap<String, Object> mPrefsMap = XposedInit.mPrefsMap;
    private static HashMap<String, String> swappedMap = CrashData.swappedData();

    public void init(LoadPackageParam lpparam) {
        if (swappedMap.isEmpty()) swappedMap = CrashData.swappedData();
        if (CrashData.needIntercept(lpparam.packageName)) {
            XposedLogUtils.logI(TAG, "进入安全模式: " + lpparam.packageName);
            return;
        }
        EzXHelper.initHandleLoadPackage(lpparam);
        EzXHelper.setLogTag(TAG);
        EzXHelper.setToastTag(TAG);
        // 把模块资源加载到目标应用
        try {
            if (!ProjectApi.mAppModulePkg.equals(lpparam.packageName)) {
                ContextUtils.getWaitContext(
                        context -> {
                            if (context != null) {
                                // try {
                                //     Handler handler = new Handler(context.getMainLooper());
                                //     BaseXposedInit.mResHook.putHandler(handler);
                                // } catch (Throwable e) {
                                // }
                                BaseXposedInit.mResHook.loadModuleRes(context);
                                // mResHook.loadModuleRes(context);
                            }
                        }, "android".equals(lpparam.packageName));
            }
        } catch (Throwable e) {
            XposedLogUtils.logE(TAG, "get context failed!" + e);
        }
        mLoadPackageParam = lpparam;
        InitDexKit kit = new InitDexKit(lpparam, TAG);
        DexKit.INSTANCE.setInitDexKit(kit);
        initZygote();
        handleLoadPackage();
        if (kit.isInit) {
            try {
                kit.closeHostDir();
                // XposedLogUtils.logE(TAG, "close dexkit s: " + lpparam.packageName);
            } catch (Exception e) {
                XposedLogUtils.logE(TAG, "close dexkit failed!" + e);
            }
        }
    }

    @Override
    public void initZygote() {
    }

    public void initHook(BaseHook baseHook) {
        if (baseHook.isLoad()) {
            baseHook.onCreate(mLoadPackageParam);
        }
    }

    public void initHook(BaseHook baseHook, boolean isInit) {
        if (isInit) {
            baseHook.onCreate(mLoadPackageParam);
        }
    }
}
