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

import com.sevtinge.hyperceiler.callback.ITAG;
import com.sevtinge.hyperceiler.utils.FileUtils;
import com.sevtinge.hyperceiler.utils.log.XposedLogUtils;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class DexKitCache {
    private final XC_LoadPackage.LoadPackageParam param;
    private final String data;
    private final String dexPath;
    private final String dexFile;

    public DexKitCache(XC_LoadPackage.LoadPackageParam param) {
        this.param = param;
        data = param.appInfo.dataDir;
        dexPath = data + "/dexkit";
        dexFile = dexPath + "/cache";
        XposedLogUtils.logE(ITAG.TAG, "data: " + data + " dex: " + dexPath);
    }

    public DexKitCache create() {
        FileUtils.mkdirs(dexPath);
        FileUtils.touch(dexFile);
        FileUtils.setPermission(dexFile);
        return this;
    }
}
