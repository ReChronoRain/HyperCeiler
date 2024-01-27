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

import com.sevtinge.hyperceiler.XposedInit;
import com.sevtinge.hyperceiler.utils.InitDexKit;
import com.sevtinge.hyperceiler.utils.PrefsMap;
import com.sevtinge.hyperceiler.utils.log.XposedLogUtils;

import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public abstract class BaseModule implements IXposedHook {

    public LoadPackageParam mLoadPackageParam = null;
    public String TAG = getClass().getSimpleName();
    public static ILoadDexKit loadDexKit;
    public final PrefsMap<String, Object> mPrefsMap = XposedInit.mPrefsMap;

    public interface ILoadDexKit {
        void createDexKit(LoadPackageParam lpparam, String TAG);
    }

    public static void setLoadDexKit(ILoadDexKit iLoadDexKit) {
        loadDexKit = iLoadDexKit;
    }

    public void init(LoadPackageParam lpparam) {
        mLoadPackageParam = lpparam;
        initZygote();
        DexKitHelper helper = new DexKitHelper();
        InitDexKit kit = new InitDexKit();
        loadDexKit.createDexKit(mLoadPackageParam, TAG);
        handleLoadPackage();
        if (helper.useDexKit) {
            try {
                kit.closeHostDir();
                // XposedLogUtils.logE(TAG, "close dexkit s: " + lpparam.packageName);
            } catch (Exception e) {
                XposedLogUtils.logE(TAG, "close dexkit e: " + e);
            }
        }
    }

    @Override
    public void initZygote() {
    }

    public void initHook(BaseHook baseHook) {
        initHook(baseHook, true);
    }

    public void initHook(BaseHook baseHook, boolean isInit) {
        if (isInit) {
            baseHook.onCreate(mLoadPackageParam);
        }
    }

    private static class DexKitHelper implements InitDexKit.IUseDexKit {
        public boolean useDexKit = false;

        public DexKitHelper() {
            InitDexKit.setUseDexKit(this);
        }

        @Override
        public void useDexKit(boolean use) {
            useDexKit = use;
        }
    }
}
