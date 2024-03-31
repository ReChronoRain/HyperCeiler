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
package com.sevtinge.hyperceiler.module.base.dexkit;

import org.luckypray.dexkit.DexKitBridge;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class InitDexKit {
    public String mTAG;
    public XC_LoadPackage.LoadPackageParam mLpparam;
    public DexKitBridge mDexKitBridge = null;
    public boolean isInit = false;
    private String hostDir = null;

    public InitDexKit(XC_LoadPackage.LoadPackageParam param, String tag) {
        mLpparam = param;
        mTAG = tag;
    }

    public DexKitBridge init() throws Exception {
        if (mDexKitBridge == null) {
            if (hostDir == null) {
                if (mLpparam == null) {
                    throw new Exception(mTAG != null ? mTAG : "InitDexKit" + ": lpparam is null");
                }
                hostDir = mLpparam.appInfo.sourceDir;
            }
            System.loadLibrary("dexkit");
            // XposedLogUtils.logE(mTAG, "dexkit: " + hostDir);
            mDexKitBridge = DexKitBridge.create(hostDir);
        }
        isInit = true;
        return mDexKitBridge;
    }

    public void closeHostDir() throws Exception {
        if (mLpparam != null) {
            mDexKitBridge.close();
            mDexKitBridge = null;
            // DexKit.INSTANCE.setDexKitBridge(null);
            isInit = false;
        } else {
            throw new Exception(mTAG + ": lpparam is null");
        }
    }
}
