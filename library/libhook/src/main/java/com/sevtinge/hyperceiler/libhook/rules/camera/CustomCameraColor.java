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

package com.sevtinge.hyperceiler.libhook.rules.camera;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.DexKit;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.IDexKit;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindField;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.FieldMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.FieldData;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.base.BaseData;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class CustomCameraColor extends BaseHook {
    @Override
    public void init() {
        Method method = DexKit.findMember("CameraColorGetter", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .addCaller(MethodMatcher.create().declaredClass("com.android.camera.fragment.FragmentMasterFilter"))
                                .addCaller(MethodMatcher.create().declaredClass("com.android.camera.customization.BGTintTextView"))
                                .addCaller(MethodMatcher.create().declaredClass("com.android.camera.fragment.FragmentBottomPopupTips"))
                                .addCaller(MethodMatcher.create().declaredClass("com.android.camera.fragment.aiwatermark.holder.WatermarkHolder"))
                                .addCaller(MethodMatcher.create().declaredClass("com.android.camera2.compat.theme.common.MiThemeOperationBottom"))
                                .modifiers(Modifier.STATIC)
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
                                .declaredClass(method.getDeclaringClass())
                                .type(int.class)
                        )).singleOrNull();
                return fieldData;
            }
        });
        EzxHelpUtils.setStaticIntField(field.getDeclaringClass(), field.getName(), PrefsBridge.getInt("camera_custom_theme_color_picker", -2025677));
    }
}
