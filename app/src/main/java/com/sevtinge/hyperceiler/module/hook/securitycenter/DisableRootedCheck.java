/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.module.hook.securitycenter;

import android.content.Intent;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit;
import com.sevtinge.hyperceiler.module.base.dexkit.IDexKitList;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.BaseDataList;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.MethodDataList;

import java.lang.reflect.Method;
import java.util.List;

public class DisableRootedCheck extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("com.xiaomi.security.xsof.MiSafetyDetectService", "onCreate", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(null);
            }
        });
        findAndHookMethod("com.xiaomi.security.xsof.MiSafetyDetectService", "onDestroy", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(null);
            }
        });
        /*findAndHookMethod("com.xiaomi.security.xsof.MiSafetyDetectService", "onBind", Intent.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(null);
            }
        });
        findAndHookMethod("com.xiaomi.security.xsof.MiSafetyDetectService", "onUnbind", Intent.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(null);
            }
        });*/
        List<Method> methods = DexKit.findMemberList("CheckRoot", new IDexKitList() {
            @Override
            public BaseDataList<MethodData> dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodDataList methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .usingStrings("/system/bin/")
                                .returnType(boolean.class)
                        )
                );
                return methodData;
            }
        });
        for (Method method : methods) {
            // Method method = methodData.getMethodInstance(lpparam.classLoader);
            logD(TAG, lpparam.packageName, "Current hooking method is " + method);
            hookMethod(method, new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    param.setResult(false);
                }
            });
        }
    }
}
