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

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit;

import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.MethodDataList;

public class BaiduClipboard extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        MethodData methodData = null;
        MethodData methodData1 = null;
        MethodDataList methodDataList = new MethodDataList();
        if ("com.baidu.input".equals(lpparam.packageName)) {
            methodData = DexKit.getDexKitBridge().findMethod(
                FindMethod.create()
                    .matcher(
                        MethodMatcher.create()
                            .declaredClass(
                                ClassMatcher.create()
                                    .usingStrings("begin to checkMaxQueryCount")
                            )
                            .returnType(int.class)
                            .usingStrings("clipboard.config.max_query_count")
                    )
            ).singleOrNull();

            methodData1 = DexKit.getDexKitBridge().findMethod(
                FindMethod.create()
                    .matcher(
                        MethodMatcher.create()
                            .declaredClass(
                                ClassMatcher.create()
                                    .usingStrings("begin to checkMaxQueryCount")
                            )
                            .name("getMaxCount")
                    )
            ).singleOrNull();
        } else if ("com.baidu.input_mi".equals(lpparam.packageName)) {
            methodDataList = DexKit.getDexKitBridge().findMethod(
                FindMethod.create()
                    .matcher(
                        MethodMatcher.create()
                            .declaredClass(
                                ClassMatcher.create()
                                    .usingStrings("clipboard.config.max_query_count")
                            )
                            .returnType(int.class)
                    )
            );
        }

        if (methodData == null && methodData1 == null) {
            if (methodDataList.isEmpty()) {
                logE(TAG, "list is empty!");
                return;
            }
            if (methodDataList.size() == 1) {
                logW(TAG, "list size only one!");
            }
            if (methodDataList.size() == 1 || methodDataList.size() == 2) {
                for (MethodData method : methodDataList) {
                    // logE(TAG, "find method: " + method.getMethodInstance(lpparam.classLoader));
                    hookMethod(method.getMethodInstance(lpparam.classLoader),
                        new MethodHook() {
                            @Override
                            protected void before(MethodHookParam param) {
                                param.setResult(Integer.MAX_VALUE);
                            }
                        }
                    );
                }
                return;
            }
            logE(TAG, "list size to more!");
            return;
        }

        if (methodData != null) {
            hookMethod(methodData.getMethodInstance(lpparam.classLoader),
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        param.setResult(Integer.MAX_VALUE);
                    }
                }
            );
        } else {
            logE(TAG, "no find method 1");
        }

        if (methodData1 != null) {
            hookMethod(methodData1.getMethodInstance(lpparam.classLoader),
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        param.setResult(Integer.MAX_VALUE);
                    }
                }
            );
        } else {
            logE(TAG, "no find method 2");
        }
    }
}
