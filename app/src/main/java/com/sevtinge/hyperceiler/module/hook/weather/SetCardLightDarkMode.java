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
package com.sevtinge.hyperceiler.module.hook.weather;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit;
import com.sevtinge.hyperceiler.module.base.dexkit.IDexKit;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindField;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.FieldMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.FieldData;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.robv.android.xposed.XposedHelpers;

public class SetCardLightDarkMode extends BaseHook {
    private static final String METHOD_NAME = "judgeCurrentColor() mLightDarkMode : ";
    private static final MethodMatcher METHOD_MATCHER = MethodMatcher.create().usingStrings(METHOD_NAME);

    @Override
    public void init() {
        Method method = (Method) DexKit.findMember("LightDarkMode", new IDexKit() {
            @Override
            public AnnotatedElement dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(METHOD_MATCHER)).singleOrNull();
                return methodData.getMethodInstance(lpparam.classLoader);
            }
        });
        Field field = (Field) DexKit.findMember("LightDarkModeField", new IDexKit() {
            @Override
            public AnnotatedElement dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                FieldData fieldData = bridge.findField(FindField.create()
                        .matcher(FieldMatcher.create()
                                .declaredClass(ClassMatcher.create()
                                        .usingStrings(METHOD_NAME))
                                .addWriteMethod(METHOD_MATCHER)
                                .addReadMethod(METHOD_MATCHER)
                                .type(int.class)
                        )).singleOrNull();
                return fieldData.getFieldInstance(lpparam.classLoader);
            }
        });
        hookMethod(method, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) {
                XposedHelpers.setIntField(param.thisObject, field.getName(), mPrefsMap.getStringAsInt("weather_card_display_type", 0));
            }
        });
    }
}

