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
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.libhook.rules.guardprovider;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;

import java.lang.reflect.Method;
import java.util.List;

import io.github.kyuubiran.ezxhelper.xposed.common.HookParam;

public class DisableRootedCheck extends BaseHook {
    private List<Method> mCheckRootMethods;

    @Override
    protected boolean useDexKit() {
        return true;
    }

    @Override
    protected boolean initDexKit() {
        mCheckRootMethods = requiredMemberList("CheckRoot", bridge -> bridge.findMethod(FindMethod.create()
            .matcher(MethodMatcher.create()
                .usingStrings("/system/bin/")
                .returnType(boolean.class)
            )));
        return true;
    }

    @Override
    public void init() {
        for (Method method : mCheckRootMethods) {
            // Method method = methodData.getMethodInstance(lpparam.classLoader);
            XposedLog.d(TAG, getPackageName(), "Current hooking method is " + method);
            hookMethod(method, new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    param.setResult(false);
                }
            });
        }
    }
}
