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

package com.sevtinge.hyperceiler.libhook.rules.personalassistant;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.IDexKit;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;

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

import io.github.kyuubiran.ezxhelper.xposed.common.HookParam;

;

public class DisableLiteVersion extends BaseHook {
    private Method mGetDeviceLevelMethod;
    private Field mCameraColorField;

    @Override
    protected boolean useDexKit() {
        return true;
    }

    @Override
    protected boolean initDexKit() {
        mGetDeviceLevelMethod = requiredMember("GetDeviceLevel", new IDexKit() {
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
        mCameraColorField = requiredMember("CameraColor", new IDexKit() {
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
                        .declaredClass(mGetDeviceLevelMethod.getDeclaringClass())
                        .type(boolean.class)
                        .modifiers(Modifier.PUBLIC)
                    )).singleOrNull();
                return fieldData;
            }
        });
        return true;
    }

    @Override
    public void init() {
        EzxHelpUtils.setStaticBooleanField(mCameraColorField.getDeclaringClass(), mCameraColorField.getName(), false);
        findAndHookMethod("com.miui.personalassistant.PAApplication", "onCreate", new IMethodHook() {
            @Override
            public void after(HookParam param) {
                EzxHelpUtils.setStaticBooleanField(mCameraColorField.getDeclaringClass(), mCameraColorField.getName(), false);
            }
        });
    }
}
