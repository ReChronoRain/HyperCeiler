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
package com.sevtinge.hyperceiler.module.hook.securityadd;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit;

import org.json.JSONObject;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Method;

public class DisableGameBoosterAds extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        MethodData methodData1 = DexKit.getDexKitBridge().findMethod(FindMethod.create()
                .matcher(MethodMatcher.create()
                        .usingStrings("is_international")
                )
        ).singleOrThrow(() -> new IllegalStateException("DisableGameBoosterAds: Cannot found MethodData"));
        Method method1 = methodData1.getMethodInstance(lpparam.classLoader);
        logD(TAG, lpparam.packageName, "getDeviceInfo() method is " + method1);
        hookMethod(method1, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                findAndHookMethod(JSONObject.class, "put", String.class, boolean.class, new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        param.args[1] = true;
                    }
                });
            }
        });

        MethodData methodData2 = DexKit.getDexKitBridge().findMethod(FindMethod.create()
                .matcher(MethodMatcher.create()
                        .usingStrings("gt_xunyou_net_privacy_alter_not_show")
                )
        ).singleOrThrow(() -> new IllegalStateException("DisableGameBoosterAds: Cannot found MethodData"));
        Method method2 = methodData2.getMethodInstance(lpparam.classLoader);
        logD(TAG, lpparam.packageName, "getShowXunyouGameBooster() method is " + method2);
        hookMethod(method2, MethodHook.returnConstant(null));
    }
}
