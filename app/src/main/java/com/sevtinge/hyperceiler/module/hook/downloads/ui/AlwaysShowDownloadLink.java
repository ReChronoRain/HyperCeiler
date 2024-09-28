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
package com.sevtinge.hyperceiler.module.hook.downloads.ui;

import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit;
import com.sevtinge.hyperceiler.module.base.dexkit.IDexKit;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.*;
import org.luckypray.dexkit.query.matchers.*;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

public class AlwaysShowDownloadLink extends BaseHook {

    @Override
    public void init() {

        Class<?> class1 = (Class<?>) DexKit.getDexKitBridge("DownloadInfo", new IDexKit() {
            @Override
            public AnnotatedElement dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                ClassData classData = bridge.findClass(FindClass.create()
                        .excludePackages("androidx")
                        .matcher(
                                ClassMatcher.create()
                                        .usingEqStrings("/s", " | ", "/")
                        )).singleOrNull();;
                return classData.getInstance(lpparam.classLoader);
            }
        });

        Method method1 = (Method) DexKit.getDexKitBridge("ShowTaskDetailMatcher", new IDexKit() {
            @Override
            public AnnotatedElement dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .declaredClass(ClassMatcher.create().usingStrings("onEventMainThread noScrollListView="))
                                .usingNumbers(4, 8, 0)
                                .paramTypes(class1)
                                .returnType(void.class)
                        )).singleOrNull();;
                return methodData.getMethodInstance(lpparam.classLoader);
            }
        });

        hookMethod(method1, new MethodHook() {
            public void before(MethodHookParam param) throws Throwable {
                // @TODO 显示来源应用和路径
                logD(TAG, lpparam.packageName, "source: " + getObjectField(param.args[0], "r") + "  path: " + getObjectField(param.args[0], "i"));
                setObjectField(param.args[0], "y", "");
            }
        });
        // findAndHookMethod("h1.h", "R", "i1.a",new MethodHook() {
        //     @Override
        //     public void before(MethodHookParam param) throws Throwable {
        //         logD(TAG, lpparam.packageName, "source: " + getObjectField(param.args[0], "r") + "  path: " + getObjectField(param.args[0], "i"));
        //         setObjectField(param.args[0], "y", "");
        //     }
        // });
    }
}
