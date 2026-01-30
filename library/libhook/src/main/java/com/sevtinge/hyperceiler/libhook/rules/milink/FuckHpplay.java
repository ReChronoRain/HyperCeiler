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

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.milink;

import android.os.Environment;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import java.io.File;
import java.io.FileNotFoundException;

import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

;

public class FuckHpplay extends BaseHook {
    private static final String TARGET_PACKAGE = "com.milink.service";
    private static final String TARGET_PATH = new File(Environment.getExternalStorageDirectory(), "com.milink.service").getAbsolutePath();

    @Override
    public void init() {
        if (!TARGET_PACKAGE.equals(getPackageName())) return;
        XposedLog.d(TAG, getPackageName(), "Target path = " + TARGET_PATH);
        findAndHookMethod("com.hpplay.common.utils.ContextPath", "makeDir", String[].class, String.class, new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                final boolean isExternalStorage = ((String) param.getArgs()[1]).startsWith(TARGET_PATH);
                if (isExternalStorage) {
                    XposedLog.d(TAG, getPackageName(), "blocked");
                    param.setThrowable(new FileNotFoundException("blocked"));
                }
            }
        });

        findAndHookMethod("com.xiaomi.aivsbluetoothsdk.utils.FileUtil", "splicingFilePath",
            String.class, String.class, String.class, String.class,
            new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    if (((String) param.getArgs()[0]).startsWith("com.milink.service")) {
                        XposedLog.d(TAG, FuckHpplay.this.getPackageName(), "reDirect");
                        param.getArgs()[0] = "MIUI" + File.separator + param.getArgs()[0];
                    }
                }
            });
    }
}
