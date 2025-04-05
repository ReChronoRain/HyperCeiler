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
package com.sevtinge.hyperceiler.module.hook.milink;

import android.os.Environment;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import java.io.File;
import java.io.FileNotFoundException;

import de.robv.android.xposed.XC_MethodHook;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

public class FuckHpplay extends BaseHook {
    private static final String TARGET_PACKAGE = "com.milink.service";
    private static final String TARGET_PATH = new File(Environment.getExternalStorageDirectory(), "com.milink.service").getAbsolutePath();

    @Override
    public void init() {
        if (!TARGET_PACKAGE.equals(lpparam.packageName)) return;
        logI(TAG, this.lpparam.packageName, "Target path = " + TARGET_PATH);
        findAndHookMethod("com.hpplay.common.utils.ContextPath", lpparam.classLoader, "makeDir", String[].class, String.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                final boolean isExternalStorage = ((String) param.args[1]).startsWith(TARGET_PATH);
                if (isExternalStorage) {
                    logI(TAG, FuckHpplay.this.lpparam.packageName, "blocked");
                    param.setThrowable(new FileNotFoundException("blocked"));
                }
            }
        });

        if (!isMoreHyperOSVersion(2f)) return;

        findAndHookMethod("com.xiaomi.aivsbluetoothsdk.utils.FileUtil", lpparam.classLoader,
                "splicingFilePath",
                String.class, String.class, String.class, String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (((String)param.args[0]).startsWith("com.milink.service")) {
                            logI(TAG, FuckHpplay.this.lpparam.packageName, "reDirect");
                            param.args[0] = "MIUI" + File.separator + param.args[0];
                        }
                    }
                });
    }
}
