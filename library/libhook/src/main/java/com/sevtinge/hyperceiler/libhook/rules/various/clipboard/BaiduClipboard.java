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
package com.sevtinge.hyperceiler.libhook.rules.various.clipboard;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.DexKit;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.IDexKit;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.IDexKitList;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.BaseDataList;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.result.MethodDataList;
import org.luckypray.dexkit.result.base.BaseData;

import java.lang.reflect.Method;
import java.util.Arrays;

;

/**
 * @author 焕晨HChen
 */
public class BaiduClipboard extends BaseHook {
    @Override
    public void init() {
        if ("com.baidu.input".equals(getPackageName())) {
            Class<?> ClipboardConfig = DexKit.findMember("NewGetMaxQueryCount", new IDexKit() {
                @Override
                public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                    ClassData classData = bridge.findClass(FindClass.create()
                            .matcher(ClassMatcher.create()
                                    .usingStrings("begin to checkMaxQueryCount")
                            )).singleOrNull();
                    return classData;
                }
            });

            if (ClipboardConfig != null) {
                Arrays.stream(ClipboardConfig.getDeclaredMethods()).forEach(method -> {
                    if (method.getReturnType().equals(Integer.class)) {
                        hookMethod(method, returnConstant(Integer.MAX_VALUE));
                    }
                });
            }
        } else if ("com.baidu.input_mi".equals(getPackageName())) {
            DexKit.findMemberList("GetMaxQueryCountList", new IDexKitList() {
                @Override
                public BaseDataList<?> dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                    MethodDataList methodDataList = bridge.findMethod(FindMethod.create()
                            .matcher(MethodMatcher.create()
                                    .declaredClass(ClassMatcher.create()
                                            .usingStrings("clipboard.config.max_query_count")
                                    )
                                    .returnType(int.class))
                    );
                    return methodDataList;
                }
            }).forEach(method -> hookMethod((Method) method, returnConstant(Integer.MAX_VALUE)));
        }
    }
}
