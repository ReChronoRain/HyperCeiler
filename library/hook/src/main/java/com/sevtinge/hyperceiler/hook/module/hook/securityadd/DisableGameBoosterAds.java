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
package com.sevtinge.hyperceiler.hook.module.hook.securityadd;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;
import com.sevtinge.hyperceiler.hook.module.base.dexkit.DexKit;
import com.sevtinge.hyperceiler.hook.module.base.dexkit.IDexKit;

import org.json.JSONObject;
import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.base.BaseData;

import java.lang.reflect.Method;

public class DisableGameBoosterAds extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        Method method1 = DexKit.findMember("IsInternational", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .usingStrings("is_international")
                        )).singleOrNull();
                return methodData;
            }
        });
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

        Method method2 = DexKit.findMember("Xunyou", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .usingStrings("gt_xunyou_net_privacy_alter_not_show")
                        )).singleOrNull();
                return methodData;
            }
        });
        hookMethod(method2, new replaceHookedMethod() {
            @Override
            protected Object replace(MethodHookParam param) throws Throwable {
                return null;
            }
        });

    }
}
