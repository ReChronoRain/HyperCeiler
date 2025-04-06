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
package com.sevtinge.hyperceiler.hook.module.hook.screenrecorder;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;
import com.sevtinge.hyperceiler.hook.module.base.dexkit.DexKit;
import com.sevtinge.hyperceiler.hook.module.base.dexkit.IDexKit;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.base.BaseData;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

public class ScreenRecorderConfig extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        Method method1 = DexKit.findMember("Frame", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .usingStrings("Error when set frame value, maxValue = ")
                        )).singleOrNull();
                return methodData;
            }
        });
        hookMethod(method1, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.args[0] = 1200;
                param.args[1] = 1;

                Field[] fields = param.method.getDeclaringClass().getDeclaredFields();
                for (Field field : fields) {
                    field.setAccessible(true);
                    if (Modifier.isFinal(field.getModifiers())) {
                        Object value = field.get(null);
                        if (value instanceof int[] intArray) {
                            if (Arrays.equals(intArray, new int[]{15, 24, 30, 48, 60, 90})) {
                                field.set(null, new int[]{15, 24, 30, 48, 60, 90, 120, 144});
                                break;
                            }
                        }
                    }
                }
            }
        });

        Method method2 = DexKit.findMember("BitRate", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .usingStrings("defaultBitRate = ")
                        )).singleOrNull();
                return methodData;
            }
        });
        hookMethod(method2, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.args[0] = 1200;
                param.args[1] = 1;

                Field[] fields = param.method.getDeclaringClass().getDeclaredFields();
                for (Field field : fields) {
                    field.setAccessible(true);
                    if (Modifier.isFinal(field.getModifiers())) {
                        Object value = field.get(null);
                        if (value instanceof int[] intArray) {
                            if (Arrays.equals(intArray, new int[]{200, 100, 50, 32, 24, 16, 8, 6, 4, 1})) {
                                field.set(null, new int[]{1200, 800, 400, 200, 100, 50, 32, 24, 16, 8, 6, 4, 1});
                                break;
                            }
                        }
                    }
                }
            }
        });
    }
}
