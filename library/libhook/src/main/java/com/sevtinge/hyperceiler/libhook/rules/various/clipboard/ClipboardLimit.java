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

package com.sevtinge.hyperceiler.libhook.rules.various.clipboard;

import android.content.ClipData;
import android.content.Context;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;

import java.util.ArrayList;

import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

;


public class ClipboardLimit extends BaseHook {
    private static ClassLoader classLoader;

    @Override
    public void init() {}

    public static void unlock(@NonNull ClassLoader classLoader) {
        ClipboardLimit.classLoader = classLoader;

        EzxHelpUtils.setStaticIntField(EzxHelpUtils.findClassIfExists("com.miui.inputmethod.MiuiClipboardManager", classLoader), "MAX_CLIP_CONTENT_SIZE", Integer.MAX_VALUE);
        EzxHelpUtils.findAndHookMethod("com.miui.inputmethod.MiuiClipboardManager", classLoader, "processSingleItemOfClipData", ClipData.class, String.class ,new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) { // Todo: 这里原本是 param.getExtra().getClassLoader(), 不太清楚怎么拿
                EzxHelpUtils.findAndHookMethod("java.lang.String", param.getThisObject().getClass().getClassLoader(), "substring", int.class, int.class, new IMethodHook() {
                    @Override
                    public void before(BeforeHookParam param) {
                    String charSequence = (String) param.getThisObject();
                        int maxLength = (int) param.getArgs()[1];
                        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

                        for (StackTraceElement element : stackTrace) {
                            if ("com.miui.inputmethod.MiuiClipboardManager".equals(element.getClassName())
                                && "processSingleItemOfClipData".equals(element.getMethodName())) {
                                if (maxLength == 5000) param.setResult(charSequence);
                            }
                        }
                    }


                });
            }
        });

        EzxHelpUtils.findAndHookMethod("com.miui.inputmethod.MiuiClipboardManager", classLoader, "getNoExpiredClipboardData", Context.class, String.class, long.class, new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                /*Method method = DexKit.findMember("JsonToBean", new IDexKit() {
                    @Override
                    public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                        MethodData methodData = bridge.findMethod(FindMethod.create()
                            .matcher(MethodMatcher.create()
                                .usingStrings("jsonToBean jsonArray len =")
                            )).singleOrNull();
                        return methodData;
                    }
                });
*/
                //ArrayList<?> a = (ArrayList<?>) EzxHelpUtils.callStaticMethod(EzxHelpUtils.findClass(method.getClass().getName(), classLoader), method.getName(), param.args[1]);
                ArrayList<?> a = (ArrayList<?>) EzxHelpUtils.callStaticMethod(EzxHelpUtils.findClass("a2.e", classLoader), "a", param.getArgs()[1]);
                param.setResult(a);
            }
        });
    }
}
