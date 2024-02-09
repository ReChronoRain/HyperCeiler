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
package com.sevtinge.hyperceiler.module.hook.mms;

import android.content.Context;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit;

import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.MethodDataList;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class DisableAd extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {

        findAndHookMethod("com.miui.smsextra.ui.BottomMenu", "allowMenuMode",
            Context.class, new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    param.setResult(false);
                }
            });

        try {
            MethodData methodData1 = DexKit.INSTANCE.getDexKitBridge().findMethod(FindMethod.create()
                .matcher(MethodMatcher.create()
                    .declaredClass(ClassMatcher.create()
                        .usingStrings("Unknown type of the message: "))
                    .usingNumbers(3, 4)
                    .returnType(boolean.class)
                    .paramCount(0)
                )
            ).singleOrThrow(() -> new IllegalStateException("DisableAd: Cannot found Method addAdButton()"));
            Method method1 = methodData1.getMethodInstance(lpparam.classLoader);
            logD(TAG, lpparam.packageName, "addAdButton() method is " + method1);
            hookMethod(method1, new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    param.setResult(false);
                }
            });
        } catch (Exception e) {
            logE(TAG, lpparam.packageName, "find addAdButton() error", e);

        }

        try {
            MethodDataList methodDataList = DexKit.INSTANCE.getDexKitBridge().findMethod(FindMethod.create()
                .matcher(MethodMatcher.create()
                    .name("setHideButton")
                )
            );
            if (methodDataList == null)
                throw new IllegalStateException("DisableAd: Cannot found Method setHideButton()");
            for (MethodData methodData : methodDataList) {
                Method method2 = methodData.getMethodInstance(lpparam.classLoader);
                if (!Modifier.isAbstract(method2.getModifiers())) {
                    logD(TAG, lpparam.packageName, "Current hooking setHideButton() method is " + method2);
                    hookMethod(method2, new MethodHook() {
                        @Override
                        protected void before(MethodHookParam param) throws Throwable {
                            param.args[0] = true;
                        }
                    });
                }
            }
        } catch (Exception e) {
            logE(TAG, lpparam.packageName, "find setHideButton() error", e);

        }
    }
}
