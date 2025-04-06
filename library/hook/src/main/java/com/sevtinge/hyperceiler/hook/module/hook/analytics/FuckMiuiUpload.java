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

package com.sevtinge.hyperceiler.hook.module.hook.analytics;

import android.content.Context;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

// from https://github.com/gykkuo/MIUI_NoGuard/blob/master/app/src/main/java/cn/fyyr/noguardpls/MainHook.java
public class FuckMiuiUpload extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        int needmod = 0;
        Class<?> classs = null;
        // com.miui.analytics.onetrack.p.u.w
        try {
            classs = XposedHelpers.findClass("com.miui.analytics.onetrack.p.u", lpparam.classLoader);
            XposedHelpers.findAndHookMethod(classs, "w", long.class, long.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    // XposedBridge.log("com.miui.analytics:执行拦截！ --- 1");
                    param.setResult(null);
                }
            });

            needmod++;
        } catch (XposedHelpers.ClassNotFoundError | NoSuchMethodError err) {
            logD(TAG, lpparam.packageName, err.toString());
        }
        // com.miui.analytics.c.f.k.y
        try {
            classs = XposedHelpers.findClass("com.miui.analytics.c.f.k", lpparam.classLoader);
            XposedHelpers.findAndHookMethod(classs, "y", long.class, long.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    // XposedBridge.log("com.miui.analytics:执行拦截！ --- 2");
                    param.setResult(null);
                }
            });
            needmod++;
        } catch (XposedHelpers.ClassNotFoundError | NoSuchMethodError err) {
            logD(TAG, lpparam.packageName, err.toString());
        }
        try {
            classs = XposedHelpers.findClass("com.miui.analytics.onetrack.q.c", lpparam.classLoader);
            // com.miui.analytics.onetrack.q.c.a
            try {
                XposedHelpers.findAndHookMethod(classs, "a", Context.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        // XposedBridge.log("com.miui.analytics:执行拦截！ --- 3");
                        param.setResult(0);
                    }
                });
                needmod++;
            } catch (XposedHelpers.ClassNotFoundError | NoSuchMethodError err) {
                logD(TAG, lpparam.packageName, err.toString());
            }
            // com.miui.analytics.onetrack.q.c.b
            try {
                XposedHelpers.findAndHookMethod(classs, "b", Context.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        // XposedBridge.log("com.miui.analytics:执行拦截！ --- 4");
                        param.setResult("NONE");
                    }
                });
                needmod++;
            } catch (XposedHelpers.ClassNotFoundError | NoSuchMethodError err) {
                logD(TAG, lpparam.packageName, err.toString());
            }
            // com.miui.analytics.onetrack.q.c.c
            try {
                XposedHelpers.findAndHookMethod(classs, "c", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        // XposedBridge.log("com.miui.analytics:执行拦截！ --- 5");
                        param.setResult(false);
                    }
                });
                needmod++;
            } catch (XposedHelpers.ClassNotFoundError | NoSuchMethodError err) {
                logD(TAG, lpparam.packageName, err.toString());
            }
        } catch (XposedHelpers.ClassNotFoundError | NoSuchMethodError err) {
            logD(TAG, lpparam.packageName, err.toString());
        }
    }
}

