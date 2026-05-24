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

package com.sevtinge.hyperceiler.libhook.rules.thememanager;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.IDexKit;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.MethodDataList;
import org.luckypray.dexkit.result.base.BaseData;

import java.lang.reflect.Method;

import io.github.kyuubiran.ezxhelper.xposed.common.HookParam;

public class AllowDownloadMore extends BaseHook {
    private Class<?> mDownloadCounterClass;
    private Method mDownloadListMethod;
    private Method mDownloadListSizeMethod;

    @Override
    protected boolean useDexKit() {
        return true;
    }

    @Override
    protected boolean initDexKit() {
        mDownloadCounterClass = requiredMember("DownloadCounter", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                ClassData clazzData = bridge.findClass(FindClass.create()
                        .matcher(ClassMatcher.create()
                                .usingStrings("anonymous_use_resources")
                        )).singleOrNull();
                return clazzData;
            }
        });
        mDownloadListMethod = requiredMember("DownloadList", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .declaredClass(mDownloadCounterClass)
                                .returnType(mDownloadCounterClass)
                        )).singleOrNull();
                return methodData;
            }
        });
        mDownloadListSizeMethod = requiredMember("DownloadListSize", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData1 = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .declaredClass(mDownloadCounterClass)
                                .returnType(int.class)
                                .usingNumbers(0)
                        )).singleOrNull();
                MethodDataList methodData2 = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .declaredClass(mDownloadCounterClass)
                                .returnType(int.class)
                        )
                );
                methodData2.remove(methodData1);
                if (methodData2.size() == 1) {
                    for (MethodData method : methodData2) {
                        return method;
                    }
                }
                return null;
            }
        });
        return true;
    }

    @Override
    public void init() {
        hookMethod(mDownloadListMethod, new IMethodHook() {
            @Override
            public void after(HookParam param) {
                hookMethod(mDownloadListSizeMethod, new IMethodHook() {
                    @Override
                    public void after(HookParam param) {
                        param.setResult(1);
                    }
                });
            }
        });
    }
}
