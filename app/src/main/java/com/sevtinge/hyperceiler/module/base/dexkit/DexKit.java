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

import org.jetbrains.annotations.NotNull;
import org.luckypray.dexkit.DexKitBridge;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class DexKit {
    public boolean isInit = false;
    private final String TAG;
    private String hostDir = null;
    private XC_LoadPackage.LoadPackageParam loadPackageParam;
    private static DexKit dexKit = null;
    private static DexKitBridge privateDexKitBridge = null;

    public DexKit(XC_LoadPackage.LoadPackageParam param, String tag) {
        loadPackageParam = param;
        TAG = tag;
        dexKit = this;
    }

    private void init() {
        if (privateDexKitBridge == null) {
            if (hostDir == null) {
                if (loadPackageParam == null) {
                    throw new RuntimeException(TAG != null ? TAG : "InitDexKit" + ": lpparam is null");
                }
                hostDir = loadPackageParam.appInfo.sourceDir;
            }
            System.loadLibrary("dexkit");
            privateDexKitBridge = DexKitBridge.create(hostDir);
        }
        isInit = true;
    }

    @NotNull
    public static DexKitBridge getDexKitBridge() {
        if (privateDexKitBridge == null) {
            if (dexKit == null) {
                throw new RuntimeException("InitDexKit is null!!");
            } else {
                // new DexKitCache(dexKit.loadPackageParam).create();
                dexKit.init();
            }
        }
        return privateDexKitBridge;
    }

    /**
     * 请勿手动调用。
     */
    public void close() {
        if (privateDexKitBridge != null) {
            privateDexKitBridge.close();
            privateDexKitBridge = null;
        }
        loadPackageParam = null;
        dexKit = null;
        hostDir = null;
        isInit = false;
    }
}
