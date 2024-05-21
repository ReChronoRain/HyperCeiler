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

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.module.hook.mms;

import static com.sevtinge.hyperceiler.module.base.tool.OtherTool.getPackageVersionCode;

import android.content.Context;
import android.util.SparseArray;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit;
import com.sevtinge.hyperceiler.module.base.dexkit.IDexKit;
import com.sevtinge.hyperceiler.module.base.dexkit.IDexKitList;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.AnnotationMatcher;
import org.luckypray.dexkit.query.matchers.AnnotationsMatcher;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.MethodDataList;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import de.robv.android.xposed.XC_MethodHook;

public class DisableAd extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {

        findAndHookMethod("com.miui.smsextra.ui.BottomMenu", "allowMenuMode",
            Context.class, new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    param.setResult(false);
                }
            });

        Method method1 = (Method) DexKit.getDexKitBridge("MessageType", new IDexKit() {
            @Override
            public AnnotatedElement dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .declaredClass(ClassMatcher.create()
                                        .usingStrings("Unknown type of the message: "))
                                .usingNumbers(3, 4)
                                .returnType(boolean.class)
                                .paramCount(0)
                        )).singleOrNull();
                return methodData.getMethodInstance(lpparam.classLoader);
            }
        });
        hookMethod(method1, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(false);
            }
        });

        ArrayList<Method> methods = DexKit.getDexKitBridge("HideButton", new IDexKitList() {
            @Override
            public ArrayList<AnnotatedElement> dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodDataList methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .name("setHideButton")
                        )
                );
                return DexKit.toElementList(methodData, lpparam.classLoader);
            }
        }).toMethodList();
        for (Method method2 : methods) {
            hookMethod(method2, new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    if (!Modifier.isAbstract(method2.getModifiers())) {
                        hookMethod(method2, new MethodHook() {
                            @Override
                            protected void before(MethodHookParam param) throws Throwable {
                                param.args[0] = true;
                            }
                        });
                    }
                }
            });
        }
    }
}
