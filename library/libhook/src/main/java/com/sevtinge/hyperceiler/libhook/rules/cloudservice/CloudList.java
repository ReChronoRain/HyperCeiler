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
package com.sevtinge.hyperceiler.libhook.rules.cloudservice;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Method;

import io.github.kyuubiran.ezxhelper.xposed.common.HookParam;

public class CloudList extends BaseHook {
    private Method mDebugModeMethod;

    @Override
    protected boolean useDexKit() {
        return true;
    }

    @Override
    protected boolean initDexKit() {
        mDebugModeMethod = requiredMember("DebugMode", bridge -> {
            MethodData methodData = bridge.findMethod(FindMethod.create()
                .matcher(MethodMatcher.create()
                    .usingStrings("support_google_csp_sync")
                )).singleOrNull();
            methodData.toDexMethod().serialize();
            return methodData;
        });
        return true;
    }

    @Override
    public void init() {
        hookMethod(mDebugModeMethod, new IMethodHook() {
            @Override
            public void before(HookParam param) {
                param.setResult(null);
            }
        });
    }
}
