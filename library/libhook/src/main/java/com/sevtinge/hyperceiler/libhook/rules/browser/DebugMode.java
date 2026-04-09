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

import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;

import java.lang.reflect.Method;

import io.github.kyuubiran.ezxhelper.xposed.common.HookParam;

public class DebugMode extends BaseHook {
    private Method mEnvironmentFlagMethod;
    private Method mDebugModeMethod1;
    private Method mDebugModeMethod2;
    private Method mDebugModeMethod3;

    @Override
    protected boolean useDexKit() {
        return true;
    }

    @Override
    protected boolean initDexKit() {
        mEnvironmentFlagMethod = requiredMember("EnvironmentFlag", bridge -> bridge.findMethod(FindMethod.create()
            .matcher(MethodMatcher.create()
                .usingStrings("environment_flag")
                .returnType(String.class)
            )).singleOrNull());
        mDebugModeMethod1 = requiredMember("DebugMode1", bridge -> bridge.findMethod(FindMethod.create()
            .matcher(MethodMatcher.create()
                .usingStrings("pref_key_debug_mode_new")
                .returnType(boolean.class)
            )).singleOrNull());
        mDebugModeMethod2 = requiredMember("DebugMode2", bridge -> bridge.findMethod(FindMethod.create()
            .matcher(MethodMatcher.create()
                .usingStrings("pref_key_debug_mode")
                .returnType(boolean.class)
            )).singleOrNull());
        mDebugModeMethod3 = requiredMember("DebugMode3", bridge -> bridge.findMethod(FindMethod.create()
            .matcher(MethodMatcher.create()
                .usingStrings("pref_key_debug_mode_" + getPackageVersionCode(getLpparam()))
                .returnType(boolean.class)
            )).singleOrNull());
        return true;
    }

    @Override
    public void init() {
        hookMethod(mEnvironmentFlagMethod, new IMethodHook() {
            @Override
            public void before(HookParam param) {
                param.setResult("1");
            }
        });

        hookMethod(mDebugModeMethod1, new IMethodHook() {
            @Override
            public void before(HookParam param) {
                param.setResult(true);
            }
        });

        hookMethod(mDebugModeMethod2, new IMethodHook() {
            @Override
            public void before(HookParam param) {
                param.setResult(true);
            }
        });

        hookMethod(mDebugModeMethod3, new IMethodHook() {
            @Override
            public void before(HookParam param) {
                param.setResult(true);
            }
        });
    }
}
