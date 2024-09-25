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
package com.sevtinge.hyperceiler.module.skip;

import android.os.Build;

import com.github.kyuubiran.ezxhelper.EzXHelper;
import com.sevtinge.hyperceiler.module.hook.systemframework.corepatch.CorePatchForR;
import com.sevtinge.hyperceiler.module.hook.systemframework.corepatch.CorePatchForS;
import com.sevtinge.hyperceiler.module.hook.systemframework.corepatch.CorePatchForT;
import com.sevtinge.hyperceiler.module.hook.systemframework.corepatch.CorePatchForU;
import com.sevtinge.hyperceiler.module.hook.systemframework.corepatch.CorePatchForV;
import com.sevtinge.hyperceiler.utils.log.XposedLogUtils;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class SystemFrameworkForCorePatch implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    public static final String TAG = "CorePatch";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (("android".equals(lpparam.packageName)) && (lpparam.processName.equals("android"))) {
            EzXHelper.initHandleLoadPackage(lpparam);
            // EzXHelper.setLogTag(TAG);
            // EzXHelper.setToastTag(TAG);
            switch (Build.VERSION.SDK_INT) {
                case Build.VERSION_CODES.VANILLA_ICE_CREAM -> // 35
                    new CorePatchForV().handleLoadPackage(lpparam);
                case Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> // 34
                        new CorePatchForU().handleLoadPackage(lpparam);
                case Build.VERSION_CODES.TIRAMISU -> // 33
                        new CorePatchForT().handleLoadPackage(lpparam);
                case Build.VERSION_CODES.S_V2 -> // 32
                        new CorePatchForS().handleLoadPackage(lpparam);
                case Build.VERSION_CODES.S -> // 31
                        new CorePatchForS().handleLoadPackage(lpparam);
                case Build.VERSION_CODES.R -> // 30
                        new CorePatchForR().handleLoadPackage(lpparam);
                default ->
                        XposedLogUtils.logW("CorePatch", "android", "Unsupported Version of Android " + Build.VERSION.SDK_INT);
            }
        }
    }

    @Override
    public void initZygote(StartupParam startupParam) {
        if (startupParam.startsSystemServer) {
            switch (Build.VERSION.SDK_INT) {
                case Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> // 34
                        new CorePatchForU().initZygote(startupParam);
                case Build.VERSION_CODES.TIRAMISU -> // 33
                        new CorePatchForT().initZygote(startupParam);
                case Build.VERSION_CODES.S_V2 -> // 32
                        new CorePatchForS().initZygote(startupParam);
                case Build.VERSION_CODES.S -> // 31
                        new CorePatchForS().initZygote(startupParam);
                case Build.VERSION_CODES.R -> // 30
                        new CorePatchForR().initZygote(startupParam);
                default ->
                        XposedLogUtils.logW("CorePatch", "android", "Unsupported Version of Android " + Build.VERSION.SDK_INT);
            }
        }
    }
}
