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
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.DexKit;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.IDexKit;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.base.BaseData;

import java.lang.reflect.Method;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

;

public class SoGouClipboard extends BaseHook {
    public boolean clipboard;

    @Override
    public void init() {
        Method method = DexKit.findMember("sogou", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                return bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                        .declaredClass(ClassMatcher.create()
                            .usingStrings("sogou_clipboard_tmp"))
                        .usingNumbers("com.sohu.inputmethod.sogou.xiaomi".equals(getPackageName()) ? 150 : 80064)
                    )).single();
            }
        });

        hookMethod(method, new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                clipboard = true;
            }

            @Override
            public void after(AfterHookParam param) {
                clipboard = false;
            }
        });
        findAndHookMethod("org.greenrobot.greendao.query.QueryBuilder",
            "list", new IMethodHook()  {
                    @Override
                    public void before(BeforeHookParam param) {
                        if (clipboard) {
                            param.setResult(null);

                        }
                    }
                }
        );
    }
}
