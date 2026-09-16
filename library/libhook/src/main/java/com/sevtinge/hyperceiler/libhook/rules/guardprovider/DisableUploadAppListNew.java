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
package com.sevtinge.hyperceiler.libhook.rules.guardprovider;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IReplaceHook;

import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;

import java.lang.reflect.Method;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;

public class DisableUploadAppListNew extends BaseHook {
    private Method mAntiDefraudAppManagerMethod;

    @Override
    protected boolean useDexKit() {
        return true;
    }

    @Override
    protected boolean initDexKit() {
        // Both anchor strings ("AntiDefraudAppManager" and
        // "https://flash.sec.miui.com/detect/app") are completely absent from the global
        // guardprovider on HyperOS 3.3 -- the app-list upload code is not in this package
        // any more, so there is nothing to hook. Use optionalMember instead of
        // requiredMember so a miss is skipped quietly rather than throwing and leaving
        // "Skip hook because initDexKit failed" behind.
        mAntiDefraudAppManagerMethod = optionalMember("AntiDefraudAppManager", bridge -> bridge.findMethod(FindMethod.create()
            .matcher(MethodMatcher.create()
                .usingStrings("AntiDefraudAppManager", "https://flash.sec.miui.com/detect/app")
            )).singleOrNull());
        return true;
    }

    @Override
    public void init() {
        if (mAntiDefraudAppManagerMethod == null) {
            return;
        }
        com.sevtinge.hyperceiler.libhook.base.BaseHook.hookMethod(mAntiDefraudAppManagerMethod, new IReplaceHook() {
            @Override
            public Object replace(HookParam param) {
                return null;
            }
        });
    }
}
