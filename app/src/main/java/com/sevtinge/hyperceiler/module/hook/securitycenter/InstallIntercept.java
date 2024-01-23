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
package com.sevtinge.hyperceiler.module.hook.securitycenter;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.DexKit;

import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Method;

public class InstallIntercept extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        MethodData methodData = DexKit.INSTANCE.getDexKitBridge().findMethod(
            FindMethod.create()
                .matcher(MethodMatcher.create()
                    .usingStrings("permcenter_install_intercept_enabled")
                    .returnType(boolean.class)
                )
        ).singleOrThrow(() -> new IllegalStateException("Find permcenter_install_intercept_enabled E"));

        // logE(TAG, "find: " + methodData.getMethodInstance(lpparam.classLoader));

        Method method = methodData.getMethodInstance(lpparam.classLoader);

        hookMethod(method,
            new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    param.setResult(false);
                }
            }
        );
    }
}
