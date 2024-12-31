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
package com.sevtinge.hyperceiler.module.hook.securitycenter.battery;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit;
import com.sevtinge.hyperceiler.module.base.dexkit.IDexKitList;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.BaseDataList;
import org.luckypray.dexkit.result.ClassDataList;
import org.luckypray.dexkit.result.MethodDataList;

import java.lang.reflect.Method;
import java.util.List;

public class PowerConsumptionRanking extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        List<Class<?>> clazzs = DexKit.findMemberList("Matcher1Clazz", new IDexKitList() {
            @Override
            public BaseDataList dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                ClassDataList clazzData = bridge.findClass(
                        FindClass.create()
                                .matcher(ClassMatcher.create()
                                        .usingStrings("%d %s %d %s")
                                )
                );
                return clazzData;
            }
        });
        List<Method> methods = DexKit.findMemberList("MiuiVersionCode", new IDexKitList() {
            @Override
            public BaseDataList dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodDataList methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .declaredClass(ClassMatcher.create()
                                        .usingStrings("ro.miui.ui.version.code"))
                                .usingNumbers(9)
                                .returnType(boolean.class)
                        )
                );
                return methodData;
            }
        });
        
        /*ClassDataList data = bridge.findClass(
            FindClass.create()
                .matcher(ClassMatcher.create()
                    .usingStrings("%d %s %d %s")
                )
        );
        MethodDataList methodDataList = bridge.findMethod(
            FindMethod.create()
                .matcher(MethodMatcher.create()
                    .declaredClass(ClassMatcher.create()
                        .usingStrings("ro.miui.ui.version.code"))
                    .usingNumbers(9)
                    .returnType(boolean.class)
                )
        );*/
        for (Class<?> clazz : clazzs) {
            logI(TAG, lpparam.packageName, "Current hooking clazz is " + clazz);
            try {
                hookAllConstructors(clazz, new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws NoSuchMethodException {
                        for (Method method : methods) {
                            logI(TAG, lpparam.packageName, "Current hooking method is " + method);
                            try {
                                hookMethod(method, new MethodHook() {
                                    @Override
                                    protected void before(MethodHookParam param) throws Throwable {
                                        param.setResult(false);
                                    }
                                });
                            } catch (Exception ignore) {
                            }
                        }
                    }
                });
            } catch (Exception ignored) {
            }
        }
    }
}
