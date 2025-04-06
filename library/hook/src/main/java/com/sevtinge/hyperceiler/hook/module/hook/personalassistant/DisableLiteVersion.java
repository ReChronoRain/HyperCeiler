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
 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.hook.module.hook.personalassistant;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;
import com.sevtinge.hyperceiler.hook.module.base.dexkit.DexKit;
import com.sevtinge.hyperceiler.hook.module.base.dexkit.IDexKit;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindField;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.AnnotationMatcher;
import org.luckypray.dexkit.query.matchers.AnnotationsMatcher;
import org.luckypray.dexkit.query.matchers.FieldMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.FieldData;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.base.BaseData;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import de.robv.android.xposed.XposedHelpers;

public class DisableLiteVersion extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        Method method = DexKit.findMember("GetDeviceLevel", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .usingStrings("getDeviceLevel # physical-memory: ")
                                .returnType(int.class)
                                .paramCount(0)
                        )).singleOrNull();
                return methodData;
            }
        });
        Field field = DexKit.findMember("CameraColor", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                FieldData fieldData = bridge.findField(FindField.create()
                        .matcher(FieldMatcher.create()
                                .addReadMethod(MethodMatcher.create()
                                        .declaredClass(findClassIfExists("com.miui.personalassistant.database.oldsettings.SettingDBManager"))
                                        .returnType(Map.class)
                                        .paramCount(0)
                                        .annotations(AnnotationsMatcher.create()
                                                .add(AnnotationMatcher.create()
                                                        .usingStrings("Ljava/lang/String;")
                                                        .usingStrings("Ljava/lang/Boolean;")
                                                )
                                        )
                                )
                                .declaredClass(method.getDeclaringClass())
                                .type(boolean.class)
                                .modifiers(Modifier.PUBLIC)
                        )).singleOrNull();
                return fieldData;
            }
        });
        XposedHelpers.setStaticBooleanField(field.getDeclaringClass(), field.getName(), false);
        findAndHookMethod("com.miui.personalassistant.PAApplication", "onCreate", new MethodHook(){
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                XposedHelpers.setStaticBooleanField(field.getDeclaringClass(), field.getName(), false);
            }
        });
    }
}
