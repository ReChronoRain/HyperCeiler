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
package com.sevtinge.hyperceiler.module.hook.clipboard;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit;
import com.sevtinge.hyperceiler.module.base.dexkit.IDexKit;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.base.BaseData;

import java.lang.reflect.Method;

public class SoGouClipboard extends BaseHook {
    public boolean clipboard;

    @Override
    public void init() {
        long stime = System.currentTimeMillis();
        Method method = DexKit.findMember("sogou", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .declaredClass(ClassMatcher.create()
                                        .usingStrings("sogou_clipboard_tmp"))
                                .usingNumbers("com.sohu.inputmethod.sogou.xiaomi".equals(lpparam.packageName) ? 150 : 80064)
                        )).singleOrNull();
                return methodData;
            }
        });
        long etime = System.currentTimeMillis();
        //logE(TAG, "代码执行时间（毫秒）: " + (etime - stime));
        // logE("find class: " + lpparam.packageName);
        // logE(TAG, "method: " + method);
        hookMethod(method, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                clipboard = true;
            }

            @Override
            protected void after(MethodHookParam param) throws Throwable {
                clipboard = false;
            }
        });
        findAndHookMethod("org.greenrobot.greendao.query.QueryBuilder",
                "list", new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        if (clipboard) {
                            param.setResult(null);

                        }
                    }
                }
        );
    }
}
