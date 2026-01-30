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
package com.sevtinge.hyperceiler.libhook.rules.browser;

import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool.getPackageVersionCode;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.DexKit;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.IDexKit;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.base.BaseData;

import java.lang.reflect.Method;

import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

;

public class DebugMode extends BaseHook {
    @Override
    public void init() {
        Method method1 = DexKit.findMember("EnvironmentFlag", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .usingStrings("environment_flag")
                                .returnType(String.class)
                        )).singleOrNull();
                return methodData;
            }
        });
        hookMethod(method1, new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                param.setResult("1");
            }
        });

        Method method2 = DexKit.findMember("DebugMode1", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .usingStrings("pref_key_debug_mode_new")
                                .returnType(boolean.class)
                        )).singleOrNull();
                return methodData;
            }
        });
        hookMethod(method2, new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                param.setResult(true);
            }
        });

        Method method3 = DexKit.findMember("DebugMode2", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .usingStrings("pref_key_debug_mode")
                                .returnType(boolean.class)
                        )).singleOrNull();
                return methodData;
            }
        });
        hookMethod(method3, new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                param.setResult(true);
            }
        });

        Method method4 = DexKit.findMember("DebugMode3", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .usingStrings("pref_key_debug_mode_" + getPackageVersionCode(getLpparam()))
                                .returnType(boolean.class)
                        )).singleOrNull();
                return methodData;
            }
        });
        hookMethod(method4, new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                param.setResult(true);
            }
        });
    }
}
