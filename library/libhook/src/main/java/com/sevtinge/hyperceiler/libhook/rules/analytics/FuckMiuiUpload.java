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

package com.sevtinge.hyperceiler.libhook.rules.analytics;

import android.content.Context;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

;

// from https://github.com/gykkuo/MIUI_NoGuard/blob/master/app/src/main/java/cn/fyyr/noguardpls/MainHook.java
// Todo: 使用 DexKit 重写此 Hook
public class FuckMiuiUpload extends BaseHook {
    @Override
    public void init() {
        int needmod = 0;
        Class<?> classs = null;
        // com.miui.analytics.onetrack.p.u.w
        try {
            classs = findClass("com.miui.analytics.onetrack.p.u");
            findAndHookMethod(classs, "w", long.class, long.class, new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    param.setResult(null);
                }
            });

            needmod++;
        } catch (NoSuchMethodError err) {
            XposedLog.e(TAG, getPackageName(), err.toString());
        }
        // com.miui.analytics.c.f.k.y
        try {
            classs = findClass("com.miui.analytics.c.f.k");
            findAndHookMethod(classs, "y", long.class, long.class, new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    // XposedBridge.log("com.miui.analytics:执行拦截！ --- 2");
                    param.setResult(null);
                }
            });
            needmod++;
        } catch (NoSuchMethodError err) {
            XposedLog.e(TAG, getPackageName(), err.toString());
        }
        try {
            classs = findClass("com.miui.analytics.onetrack.q.c");
            // com.miui.analytics.onetrack.q.c.a
            try {
                findAndHookMethod(classs, "a", Context.class, new IMethodHook() {
                    @Override
                    public void before(BeforeHookParam param) {
                        // XposedBridge.log("com.miui.analytics:执行拦截！ --- 3");
                        param.setResult(0);
                    }
                });
                needmod++;
            } catch (NoSuchMethodError err) {
                XposedLog.e(TAG, getPackageName(), err.toString());
            }
            // com.miui.analytics.onetrack.q.c.b
            try {
                findAndHookMethod(classs, "b", Context.class, new IMethodHook() {
                    @Override
                    public void before(BeforeHookParam param) {
                        // XposedBridge.log("com.miui.analytics:执行拦截！ --- 4");
                        param.setResult("NONE");
                    }
                });
                needmod++;
            } catch (NoSuchMethodError err) {
                XposedLog.e(TAG, getPackageName(), err.toString());
            }
            // com.miui.analytics.onetrack.q.c.c
            try {
                findAndHookMethod(classs, "c", new IMethodHook() {
                    @Override
                    public void before(BeforeHookParam param) {
                        // XposedBridge.log("com.miui.analytics:执行拦截！ --- 5");
                        param.setResult(false);
                    }
                });
                needmod++;
            } catch (NoSuchMethodError err) {
                XposedLog.e(TAG, getPackageName(), err.toString());
            }
        } catch (NoSuchMethodError err) {
            XposedLog.e(TAG, getPackageName(), err.toString());
        }
    }
}

