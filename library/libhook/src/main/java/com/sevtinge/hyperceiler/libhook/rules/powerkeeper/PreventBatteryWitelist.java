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
package com.sevtinge.hyperceiler.libhook.rules.powerkeeper;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.IDexKit;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.base.BaseData;

import java.lang.reflect.Method;
import java.util.function.Consumer;

import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory;

public class PreventBatteryWitelist extends BaseHook {
    private Method mFucSwitchMethod;

    @Override
    protected boolean useDexKit() {
        return true;
    }

    @Override
    protected boolean initDexKit() {
        mFucSwitchMethod = requiredMember("FucSwitch", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .declaredClass(ClassMatcher.create()
                                        .usingStrings("addPowerSaveWhitelistApps: "))
                                .usingStrings("addPowerSaveWhitelistApps: ")
                        )).singleOrNull();
                return methodData;
            }
        });
        return true;
    }

    @Override
    public void init() {
        // hookAllMethods("com.miui.powerkeeper.utils.CommonAdapter", lpparam.classLoader, "addPowerSaveWhitelistApps", new MethodHook(20000) {
        //     @Override
        //     protected void before(MethodHookParam param) throws Throwable {
        //         param.setResult(null);
        //     }
        // });

        HookFactory.createMethodHook(mFucSwitchMethod, new Consumer<>() {
            @Override
            public void accept(HookFactory hookFactory) {
                hookFactory.before(param -> {
                    String[] strArr = (String[]) param.getArgs()[0];
                    if (strArr.length > 1) {
                        param.setResult(null);
                    }
                });
            }
        });
    }
}
