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
package com.sevtinge.hyperceiler.libhook.rules.weather;

import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;

import org.luckypray.dexkit.query.FindField;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.FieldMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import io.github.kyuubiran.ezxhelper.xposed.common.HookParam;

public class SetCardLightDarkMode extends BaseHook {
    private static final String METHOD_NAME = "judgeCurrentColor() mLightDarkMode : ";
    private static final MethodMatcher METHOD_MATCHER = MethodMatcher.create().usingStrings(METHOD_NAME);
    private Method mLightDarkModeMethod;
    private Field mLightDarkModeField;

    @Override
    protected boolean useDexKit() {
        return true;
    }

    @Override
    protected boolean initDexKit() {
        mLightDarkModeMethod = requiredMember("LightDarkMode", bridge -> bridge.findMethod(FindMethod.create()
            .matcher(METHOD_MATCHER)).singleOrNull());
        mLightDarkModeField = requiredMember("LightDarkModeField", bridge -> bridge.findField(FindField.create()
            .matcher(FieldMatcher.create()
                .declaredClass(ClassMatcher.create()
                    .usingStrings(METHOD_NAME))
                .addWriteMethod(METHOD_MATCHER)
                .addReadMethod(METHOD_MATCHER)
                .type(int.class)
            )).singleOrNull());
        return true;
    }

    @Override
    public void init() {
        hookMethod(mLightDarkModeMethod, new IMethodHook() {
            @Override
            public void after(HookParam param) {
                EzxHelpUtils.setIntField(param.getThisObject(), mLightDarkModeField.getName(), PrefsBridge.getStringAsInt("weather_card_display_type", 0));
            }
        });
    }
}
