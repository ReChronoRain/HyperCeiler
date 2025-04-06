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
package com.sevtinge.hyperceiler.hook.module.hook.browser;

import static com.sevtinge.hyperceiler.hook.module.base.tool.AppsTool.getPackageVersionCode;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;
import com.sevtinge.hyperceiler.hook.module.base.dexkit.DexKit;
import com.sevtinge.hyperceiler.hook.module.base.dexkit.IDexKit;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.base.BaseData;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;

public class EnableDebugEnvironment extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        Method method = DexKit.findMember("DebugMode", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .usingStrings("pref_key_debug_mode_" + getPackageVersionCode(lpparam))
                                .name("getDebugMode")
                                .returnType(boolean.class)
                        )).singleOrNull();
                return methodData;
            }
        });
        hookMethod(method, new MethodHook() {
            @Override
            protected void before(XC_MethodHook.MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}
