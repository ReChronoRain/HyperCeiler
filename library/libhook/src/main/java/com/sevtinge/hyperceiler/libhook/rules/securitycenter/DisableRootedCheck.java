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

package com.sevtinge.hyperceiler.libhook.rules.securitycenter;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.IDexKit;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.IDexKitList;

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

import io.github.kyuubiran.ezxhelper.xposed.common.HookParam;

public class DisableRootedCheck extends BaseHook {
    private Method mReturnEnvironmentMethod;
    private Class<?> mClientApiRequestClass;
    private Method mEnvironmentPutMethod;
    private Class<?> mRiskAppClass;
    private Field mRiskAppField;
    private Method mUnsetEnvironmentMethod;
    private List<Method> mSetEnvironmentMethods;
    private List<Method> mCheckRootMethods;

    @Override
    protected boolean useDexKit() {
        return true;
    }

    @Override
    protected boolean initDexKit() {
        mReturnEnvironmentMethod = requiredMember("ReturnEnvironment", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                        .usingStrings("Invalid Task , reason:  [canceled, ")
                    )).singleOrNull();
                return methodData;
            }
        });
        mClientApiRequestClass = requiredMember("ClientApiRequest", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                ClassData classData = bridge.findClass(FindClass.create()
                    .matcher(ClassMatcher.create()
                        .usingStrings("ClientApiRequest")
                    )).singleOrNull();
                return classData;
            }
        });
        mEnvironmentPutMethod = requiredMember("EnvironmentPut", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                        .declaredClass(mClientApiRequestClass)
                        .returnType(boolean.class)
                    )).singleOrNull();
                return methodData;
            }
        });
        mRiskAppClass = requiredMember("RiskAppClass", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                ClassData classData = bridge.findClass(FindClass.create()
                    .matcher(ClassMatcher.create()
                        .usingStrings("RiskAppManager")
                    )).singleOrNull();
                return classData;
            }
        });
        mRiskAppField = requiredMember("RiskApp", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                FieldData fieldData = bridge.findField(FindField.create()
                    .matcher(FieldMatcher.create()
                        .type(mRiskAppClass)
                        .modifiers(Modifier.PRIVATE)
                    )).singleOrNull();
                return fieldData;
            }
        });
        mUnsetEnvironmentMethod = requiredMember("UnsetEnvironment", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                        .usingStrings("RiskAppManager")
                    )).singleOrNull();
                return methodData;
            }
        });
        mSetEnvironmentMethods = requiredMemberList("SetEnvironment", new IDexKitList() {
            @Override
            public BaseDataList<MethodData> dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodDataList methodData = bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                        .returnType(void.class)
                        .paramTypes(mClientApiRequestClass)
                        .declaredClass(mRiskAppClass)
                    )
                );
                return methodData;
            }
        });
        mCheckRootMethods = requiredMemberList("CheckRoot", new IDexKitList() {
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
        return true;
    }

    @Override
    public void init() {
        hookMethod(mReturnEnvironmentMethod, new IMethodHook() {
            @Override
            public void before(HookParam param) {
                Object obj = param.getArgs()[0];
                if (mClientApiRequestClass.isInstance(obj)) {
                    callMethod(obj, mEnvironmentPutMethod.getName());
                    Object thisObj = param.getThisObject();
                    Object cField = getObjectField(thisObj, mRiskAppField.getName());
                    for (Method method : mSetEnvironmentMethods) {
                        if (method != mUnsetEnvironmentMethod) {
                            callMethod(cField, method.getName(), obj);
                        }
                    }
                }
            }
        });
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
