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
package com.sevtinge.hyperceiler.module.hook.thememanager;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit;

import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Method;

import miui.drm.DrmManager;

public class AllowThirdTheme extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        MethodData methodData = DexKit.getDexKitBridge().findMethod(FindMethod.create()
                .matcher(MethodMatcher.create()
                        .usingStrings("theme", "ThemeManagerTag", "/system", "check rights isLegal: ")
                )
        ).singleOrThrow(() -> new IllegalStateException("AllowThirdTheme: Cannot found MethodData"));
        Method method = methodData.getMethodInstance(lpparam.classLoader);
        logD(TAG, lpparam.packageName, "isLegal() method is " + method);
        hookMethod(method, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(DrmManager.DrmResult.DRM_SUCCESS);
            }
        });
    }
}
