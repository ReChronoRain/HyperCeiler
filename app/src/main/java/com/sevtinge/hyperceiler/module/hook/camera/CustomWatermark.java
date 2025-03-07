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
package com.sevtinge.hyperceiler.module.hook.camera;

import android.util.SparseArray;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit;
import com.sevtinge.hyperceiler.module.base.dexkit.IDexKitList;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.AnnotationMatcher;
import org.luckypray.dexkit.query.matchers.AnnotationsMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.BaseDataList;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.MethodDataList;

import java.lang.reflect.Method;
import java.util.List;

public class CustomWatermark extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        List<Method> methods = DexKit.findMemberList("Watermark", new IDexKitList() {
            @Override
            public BaseDataList<MethodData> dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodDataList methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .returnType(SparseArray.class)
                                .paramCount(0)
                                .annotations(AnnotationsMatcher.create()
                                        .add(AnnotationMatcher.create()
                                                .usingStrings("Ljava/lang/String;")
                                        )
                                )
                        )
                );
                return methodData;
            }
        });
        for (Method method : methods) {
            // Method method = methodData.getMethodInstance(lpparam.classLoader);
            logD(TAG, lpparam.packageName, "Current hooking method is " + method);
            try {
                hookMethod(method, new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        SparseArray<String[]> sparseArray = new SparseArray<>(1);
                        sparseArray.put(0, new String[]{mPrefsMap.getString("camera_custom_watermark_manufacturer", "XIAOMI"), mPrefsMap.getString("camera_custom_watermark_device", "MI PHONE")});
                        param.setResult(sparseArray);
                    }
                });
            } catch (Exception e) {
                logE(TAG, lpparam.packageName, e);
            }
        }
    }
}
