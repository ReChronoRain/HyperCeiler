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
package com.sevtinge.hyperceiler.libhook.rules.calendar;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.IDexKit;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.base.BaseData;

import java.lang.reflect.Method;
import java.util.Objects;

import io.github.kyuubiran.ezxhelper.xposed.common.HookParam;

public class UnlockSubscription extends BaseHook {

    Method targetMethod;

    @Override
    protected boolean useDexKit() {
        return true;
    }

    @Override
    protected boolean initDexKit() {
        targetMethod = requiredMember("CalendarApplication", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                        .declaredClass(ClassMatcher.create()
                            .usingStrings("Cal:D:CalendarApplicationDelegate"))
                        .usingStrings("key_subscription_display", "key_import_todo", "key_chinese_almanac_pref", "key_weather_display", "key_ai_time_parse")
                        .paramCount(0)
                    )).singleOrNull();
                return methodData;
            }
        });
        return true;
    }

    @Override
    public void init() {
        hookMethod(targetMethod, new IMethodHook() {
            @Override
            public void before(HookParam param) {
                XposedLog.d(TAG, getPackageName(), "1");
                try {
                    findAndHookMethod(findClass("android.app.SharedPreferencesImpl$EditorImpl"), "putBoolean", String.class, boolean.class, new IMethodHook() {
                        @Override
                        public void before(HookParam param) {
                            String param0 = (String) param.getArgs()[0];
                            if (Objects.equals(param0, "key_subscription_display") ||
                                    Objects.equals(param0, "key_import_todo") ||
                                    Objects.equals(param0, "key_chinese_almanac_pref") ||
                                    Objects.equals(param0, "key_weather_display") ||
                                    Objects.equals(param0, "key_ai_time_parse")) param.getArgs()[1] = true;
                        }
                    });
                } catch (Exception e) {
                    XposedLog.e(TAG, getPackageName(), "Cannot hook android.app.SharedPreferencesImpl$EditorImpl.putBoolean(String, boolean)", e);
                }
            }
        });
    }
}
