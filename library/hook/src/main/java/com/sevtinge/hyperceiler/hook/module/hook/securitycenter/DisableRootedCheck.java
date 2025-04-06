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
 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.hook.module.hook.securitycenter;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;
import com.sevtinge.hyperceiler.hook.module.base.dexkit.DexKit;
import com.sevtinge.hyperceiler.hook.module.base.dexkit.IDexKit;
import com.sevtinge.hyperceiler.hook.module.base.dexkit.IDexKitList;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.FindField;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.FieldMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.BaseDataList;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.result.FieldData;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.MethodDataList;
import org.luckypray.dexkit.result.base.BaseData;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import de.robv.android.xposed.XposedHelpers;

public class DisableRootedCheck extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        Method returnEnvironment = DexKit.findMember("ReturnEnvironment", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                        .usingStrings("Invalid Task , reason:  [canceled, ")
                    )).singleOrNull();
                return methodData;
            }
        });
        Class<?> clientApiRequest = DexKit.findMember("ClientApiRequest", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                ClassData classData = bridge.findClass(FindClass.create()
                    .matcher(ClassMatcher.create()
                        .usingStrings("ClientApiRequest")
                    )).singleOrNull();
                return classData;
            }
        });
        Method environmentPut = DexKit.findMember("EnvironmentPut", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                        .declaredClass(clientApiRequest)
                        .returnType(boolean.class)
                    )).singleOrNull();
                return methodData;
            }
        });
        Class<?> riskAppClass = DexKit.findMember("RiskAppClass", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                ClassData classData = bridge.findClass(FindClass.create()
                    .matcher(ClassMatcher.create()
                        .usingStrings("RiskAppManager")
                    )).singleOrNull();
                return classData;
            }
        });
        Field riskApp = DexKit.findMember("RiskApp", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                FieldData fieldData = bridge.findField(FindField.create()
                    .matcher(FieldMatcher.create()
                        .type(riskAppClass)
                        .modifiers(Modifier.PRIVATE)
                    )).singleOrNull();
                return fieldData;
            }
        });
        Method unsetEnvironment = DexKit.findMember("UnsetEnvironment", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                        .usingStrings("RiskAppManager")
                    )).singleOrNull();
                return methodData;
            }
        });
        List<Method> setEnvironment = DexKit.findMemberList("SetEnvironment", new IDexKitList() {
            @Override
            public BaseDataList<MethodData> dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodDataList methodData = bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                        .returnType(void.class)
                        .paramTypes(clientApiRequest)
                        .declaredClass(riskAppClass)
                    )
                );
                return methodData;
            }
        });

        hookMethod(returnEnvironment, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                Object obj = param.args[0];
                if (clientApiRequest.isInstance(obj)) {
                    XposedHelpers.callMethod(obj, environmentPut.getName());
                    Object thisObj = param.thisObject;
                    Object cField = XposedHelpers.getObjectField(thisObj, riskApp.getName());
                    for (Method method : setEnvironment) {
                        if (method != unsetEnvironment) {
                            XposedHelpers.callMethod(cField, method.getName(), obj);
                        }
                    }
                }
            }
        });

        List<Method> methods = DexKit.findMemberList("CheckRoot", new IDexKitList() {
            @Override
            public BaseDataList<MethodData> dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodDataList methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .usingStrings("/system/bin/")
                                .returnType(boolean.class)
                        )
                );
                return methodData;
            }
        });
        for (Method method : methods) {
            // Method method = methodData.getMethodInstance(lpparam.classLoader);
            logD(TAG, lpparam.packageName, "Current hooking method is " + method);
            hookMethod(method, new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    param.setResult(false);
                }
            });
        }
    }
}
