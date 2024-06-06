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
package com.sevtinge.hyperceiler.module.hook.calendar;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit;
import com.sevtinge.hyperceiler.module.base.dexkit.IDexKit;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;

public class UnlockSubscription extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        Method method = (Method) DexKit.getDexKitBridge("CalendarApplication", new IDexKit() {
            @Override
            public AnnotatedElement dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .declaredClass(ClassMatcher.create()
                                        .usingStrings("Cal:D:CalendarApplicationDelegate"))
                                .usingStrings("key_subscription_display", "key_import_todo", "key_chinese_almanac_pref", "key_weather_display", "key_ai_time_parse")
                                .paramCount(0)
                        )).singleOrNull();
                return methodData.getMethodInstance(lpparam.classLoader);
            }
        });
        hookMethod(method, new MethodHook() {
            @Override
            protected void before(XC_MethodHook.MethodHookParam param) throws Throwable {
                try {
                    findAndHookMethod(findClass("android.app.SharedPreferencesImpl$EditorImpl"), "putBoolean", String.class, boolean.class, new MethodHook() {
                        @Override
                        protected void before(MethodHookParam param) throws Throwable {
                            if (param.args[0] == "key_subscription_display" ||
                                    param.args[0] == "key_import_todo" ||
                                    param.args[0] == "key_chinese_almanac_pref" ||
                                    param.args[0] == "key_weather_display" ||
                                    param.args[0] == "key_ai_time_parse") param.args[1] = true;
                        }
                    });
                } catch (Exception e) {
                    logE(TAG, lpparam.packageName, "Cannot hook android.app.SharedPreferencesImpl$EditorImpl.putBoolean(String, boolean)", e);
                }
            }
        });
    }
}
