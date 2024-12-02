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
package com.sevtinge.hyperceiler.module.hook.clipboard;

import com.hchen.hooktool.BaseHC;
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit;
import com.sevtinge.hyperceiler.module.base.dexkit.IDexKit;
import com.sevtinge.hyperceiler.module.base.dexkit.IDexKitList;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.result.MethodDataList;

import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.List;

/**
 * @author 焕晨HChen
 */
public class BaiduClipboard extends BaseHC {
    @Override
    public void init() {
        if ("com.baidu.input".equals(lpparam.packageName)) {
            Class<?> ClipboardConfig = (Class<?>) DexKit.getDexKitBridge("NewGetMaxQueryCount", new IDexKit() {
                @Override
                public AnnotatedElement dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                    ClassData classData = bridge.findClass(FindClass.create()
                            .matcher(ClassMatcher.create()
                                    .usingStrings("begin to checkMaxQueryCount")
                            )).singleOrNull();
                    return classData.getInstance(lpparam.classLoader);
                }
            });

            if (ClipboardConfig != null) {
                Arrays.stream(ClipboardConfig.getDeclaredMethods()).forEach(method -> {
                    if (method.getReturnType().equals(Integer.class)) {
                        hook(method, returnResult(Integer.MAX_VALUE));
                    }
                });
            }
        } else if ("com.baidu.input_mi".equals(lpparam.packageName)) {
            DexKit.getDexKitBridgeList("GetMaxQueryCountList", new IDexKitList() {
                @Override
                public List<AnnotatedElement> dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                    MethodDataList methodDataList = bridge.findMethod(FindMethod.create()
                            .matcher(MethodMatcher.create()
                                    .declaredClass(ClassMatcher.create()
                                            .usingStrings("clipboard.config.max_query_count")
                                    )
                                    .returnType(int.class))
                    );
                    return DexKit.toElementList(methodDataList);
                }
            }).toMethodList().forEach(method -> hook(method, returnResult(Integer.MAX_VALUE)));
        }
    }
}
