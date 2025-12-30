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

package com.sevtinge.hyperceiler.hook.module.rules.various.clipboard;

import android.content.ClipData;
import android.content.Context;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;

import java.util.ArrayList;

import de.robv.android.xposed.XposedHelpers;


public class ClipboardLimit extends BaseHook {
    private static ClassLoader classLoader;

    @Override
    public void init() throws NoSuchMethodException {}

    public static void unlock(@NonNull ClassLoader classLoader) {
        ClipboardLimit.classLoader = classLoader;

        XposedHelpers.setStaticIntField(XposedHelpers.findClassIfExists("com.miui.inputmethod.MiuiClipboardManager", classLoader), "MAX_CLIP_CONTENT_SIZE", Integer.MAX_VALUE);
        XposedHelpers.findAndHookMethod("com.miui.inputmethod.MiuiClipboardManager", classLoader, "processSingleItemOfClipData", ClipData.class, String.class ,new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                XposedHelpers.findAndHookMethod("java.lang.String", param.getExtra().getClassLoader(), "substring", int.class, int.class, new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        String charSequence = (String) param.thisObject;
                        int maxLength = (int) param.args[1];
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

        XposedHelpers.findAndHookMethod("com.miui.inputmethod.MiuiClipboardManager", classLoader, "getNoExpiredClipboardData", Context.class, String.class, long.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
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
                //ArrayList<?> a = (ArrayList<?>) XposedHelpers.callStaticMethod(XposedHelpers.findClass(method.getClass().getName(), classLoader), method.getName(), param.args[1]);
                ArrayList<?> a = (ArrayList<?>) XposedHelpers.callStaticMethod(XposedHelpers.findClass("a2.e", classLoader), "a", param.args[1]);
                param.setResult(a);
            }
        });
    }
}
