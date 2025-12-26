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
package com.sevtinge.hyperceiler.hook.module.rules.systemframework.corepatch;

import java.lang.reflect.InvocationTargetException;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class CorePatchForV extends CorePatchForU {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws InvocationTargetException, IllegalAccessException, InstantiationException {
        super.handleLoadPackage(loadPackageParam);

        var checkDowngradeAlt = XposedHelpers.findMethodExactIfExists("com.android.server.pm.PackageManagerServiceUtils",
            loadPackageParam.classLoader, "checkDowngrade", "com.android.server.pm.PackageSetting",
            "android.content.pm.PackageInfoLite");
        if (checkDowngradeAlt != null) {
            XposedBridge.hookMethod(checkDowngradeAlt, new ReturnConstant(prefs, "prefs_key_system_framework_core_patch_downgr", null));
        }
    }

    @Override
    Class<?> getParsedPackage(ClassLoader classLoader) {
        return XposedHelpers.findClassIfExists("com.android.internal.pm.parsing.pkg.ParsedPackage", classLoader);
    }
}
