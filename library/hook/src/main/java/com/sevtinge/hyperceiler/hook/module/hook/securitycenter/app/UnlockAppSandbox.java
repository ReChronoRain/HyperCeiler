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
 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.hook.module.hook.securitycenter.app;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;
import com.sevtinge.hyperceiler.hook.module.base.dexkit.DexKit;
import com.sevtinge.hyperceiler.hook.module.base.dexkit.IDexKit;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.base.BaseData;

import java.lang.reflect.Method;

public class UnlockAppSandbox extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        Method method1 = DexKit.findMember("IsSupportAppSandboxByDevice", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .declaredClass(ClassMatcher.create()
                                        .usingStrings("PrivacyFeature"))
                                .usingStrings("rothko")
                        )).singleOrNull();
                return methodData;
            }
        });
        Method method2 = DexKit.findMember("IsUseSandbox", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .declaredClass(ClassMatcher.create()
                                        .usingStrings("SandboxManager"))
                                .usingStrings("android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE")
                        )).singleOrNull();
                return methodData;
            }
        });
        Method method3 = DexKit.findMember("IsSupportAppSandboxByProp", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .declaredClass(ClassMatcher.create()
                                        .usingStrings("PrivacyFeature"))
                                .usingStrings("persist.sys.mi_isolated")
                        )).singleOrNull();
                return methodData;
            }
        });
        Method method4 = DexKit.findMember("GetCloudDataBoolean", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .declaredClass(ClassMatcher.create()
                                        .usingStrings("content://com.android.settings.cloud.CloudSettings/cloud_all_data"))
                                .usingStrings("getCloudDataBoolean")
                        )).singleOrNull();
                return methodData;
            }
        });
        hookMethod(method1, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
        hookMethod(method2, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                hookMethod(method3, new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        param.setResult(true);
                    }
                });
            }
        });
        hookMethod(method4, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                if (param.args[2] == "HyperIsolateEnable") param.setResult(true);
            }
        });
    }
}
