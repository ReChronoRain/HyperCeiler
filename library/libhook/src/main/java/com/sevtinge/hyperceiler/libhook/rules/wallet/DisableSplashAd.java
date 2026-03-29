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

package com.sevtinge.hyperceiler.libhook.rules.wallet;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import java.lang.reflect.Method;

import io.github.kyuubiran.ezxhelper.xposed.common.HookParam;

public class DisableSplashAd extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.xiaomi.jr.app.MiFinanceActivity", "onCreate", Bundle.class, new IMethodHook() {
            @Override
            public void after(HookParam param) {
                Activity activity = (Activity) param.getThisObject();
                int resId = activity.getResources().getIdentifier("splash_container", "id", activity.getPackageName());
                if (resId > 0) {
                    View splashContainer = activity.findViewById(resId);
                    if (splashContainer != null) {
                        splashContainer.setVisibility(View.GONE);
                    }
                }
            }
        });

        Class<?> adManagerClass = findClassIfExists("com.xiaomi.jr.ad.AdManager");
        if (adManagerClass != null) {
            for (Method m : adManagerClass.getDeclaredMethods()) {
                if (m.getParameterTypes().length == 2
                        && m.getParameterTypes()[0] == Context.class
                        && m.getParameterTypes()[1].getName().contains("OnGetAdDataListener")) {
                    
                    hookMethod(m, new IMethodHook() {
                        @Override
                        public void before(HookParam param) {
                            Object listener = param.getArgs()[1];
                            if (listener != null) {
                                callMethod(listener, "onGetAdData", new Object[]{null});
                            }
                            param.setResult(null);
                        }
                    });
                }
            }
        }

        findAndHookMethod("com.xiaomi.jr.app.splash.SplashFragment", "onResume", new IMethodHook() {
            @Override
            public void after(HookParam param) {
                Object activity = callMethod(param.getThisObject(), "getActivity");
                if (activity != null) {
                    try {
                        callMethod(activity, "finishSplash", false);
                    } catch (Throwable ignored) {}
                }
            }
        });
    }
}