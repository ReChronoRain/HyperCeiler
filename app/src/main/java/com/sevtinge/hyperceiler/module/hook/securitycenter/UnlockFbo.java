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
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit;

import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Method;

public class UnlockFbo extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        MethodData isFboStateOpenInCloud = DexKit.getDexKitBridge().findMethod(FindMethod.create()
            .matcher(MethodMatcher.create()
                .usingStrings("FBO_STATE_OPEN")
                .returnType(boolean.class)
                .paramCount(0)
            )
        ).singleOrThrow(() -> new IllegalStateException("UnlockFbo: Cannot found MethodData FBO_STATE_OPEN"));
        hookMethod(isFboStateOpenInCloud.getMethodInstance(lpparam.classLoader), MethodHook.returnConstant(true));

        MethodData methodData = DexKit.getDexKitBridge().findMethod(FindMethod.create()
            .matcher(MethodMatcher.create()
                .usingStrings("miui.fbo.FboManager")
                .returnType(boolean.class)
                .paramTypes(String.class)
            )
        ).singleOrThrow(() -> new IllegalStateException("UnlockFbo: Cannot found MethodData"));
        Method method = methodData.getMethodInstance(lpparam.classLoader);
        logD(TAG, lpparam.packageName, "Unlock FBO method is " + method);
        hookMethod(method, MethodHook.returnConstant(true));
    }
}
