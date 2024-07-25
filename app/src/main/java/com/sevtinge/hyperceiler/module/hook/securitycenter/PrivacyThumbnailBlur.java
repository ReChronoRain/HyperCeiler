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
package com.sevtinge.hyperceiler.module.hook.securitycenter;

import android.content.Context;

import com.hchen.hooktool.BaseHC;
import com.hchen.hooktool.callback.IAction;
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit;
import com.sevtinge.hyperceiler.module.base.dexkit.IDexKit;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Arrays;

public class PrivacyThumbnailBlur extends BaseHC {
    @Override
    public void init() {
        Method method = (Method) DexKit.getDexKitBridge("ptb", new IDexKit() {
            @Override
            public AnnotatedElement dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                return bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .declaredClass(ClassMatcher.create()
                                        .usingStrings("miui_recents_privacy_thumbnail_blur")
                                )
                                .paramTypes(Context.class, String.class, boolean.class)
                        )
                ).singleOrNull().getMethodInstance(lpparam.classLoader);
            }
        });

        hook(method, new IAction() {
            @Override
            public void before() throws Throwable {
                StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
                if (Arrays.stream(stackTraceElements).noneMatch(stackTraceElement ->
                        stackTraceElement.getClassName().equals("PrivacyThumbnailBlurSettings")
                )) {
                    setResult(null);
                }
            }
        });
    }
}
